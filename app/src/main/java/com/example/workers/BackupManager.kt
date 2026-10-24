package com.example.workers

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import com.example.util.UserManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object BackupManager {
    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "amar_dokan_backup_prefs"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_LAST_STATUS = "last_status"
    private const val KEY_LAST_SUMMARY = "last_summary"
    private const val KEY_ROOT_FOLDER_ID = "drive_root_folder_id"
    private const val KEY_CASH_MEMO_FOLDER_ID = "drive_cash_memo_folder_id"

    const val WORK_NAME_PERIODIC = "amar_dokan_periodic_backup"
    const val WORK_NAME_MANUAL = "amar_dokan_manual_backup"

    private val _lastBackupTime = MutableStateFlow(0L)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _lastStatus = MutableStateFlow<String?>(null)
    val lastStatus: StateFlow<String?> = _lastStatus.asStateFlow()

    private val _isBackupRunning = MutableStateFlow(false)
    val isBackupRunning: StateFlow<Boolean> = _isBackupRunning.asStateFlow()

    // Atomic execution lock to prevent parallel or overlapping backup operations
    private val backupExecutionLock = AtomicBoolean(false)
    // Tracks if database was modified while a backup was already running
    private val hasPendingChanges = AtomicBoolean(false)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isObserverAttached = false

    fun acquireBackupLock(): Boolean {
        return backupExecutionLock.compareAndSet(false, true)
    }

    fun releaseBackupLock() {
        backupExecutionLock.set(false)
    }

    fun isBackupLocked(): Boolean {
        return backupExecutionLock.get()
    }

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val userKey = UserManager.getCurrentUserKey(appContext)
        val prefs = getPrefs(appContext, userKey)
        _lastBackupTime.value = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        _lastStatus.value = prefs.getString(KEY_LAST_STATUS, null)

        if (!isObserverAttached) {
            isObserverAttached = true
            scope.launch {
                try {
                    val workManager = WorkManager.getInstance(appContext)
                    workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME_MANUAL).collect { workInfoList ->
                        val workInfo = workInfoList.firstOrNull() ?: return@collect
                        when (workInfo.state) {
                            WorkInfo.State.RUNNING -> {
                                _isBackupRunning.value = true
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                _isBackupRunning.value = false
                                releaseBackupLock()
                                if (hasPendingChanges.compareAndSet(true, false)) {
                                    triggerImmediateBackup(appContext)
                                }
                            }
                            WorkInfo.State.FAILED -> {
                                _isBackupRunning.value = false
                                releaseBackupLock()
                                val currentStatus = _lastStatus.value
                                if (currentStatus == null || (!currentStatus.startsWith("ব্যর্থ") && !currentStatus.startsWith("সফল") && !currentStatus.startsWith("বাতিল"))) {
                                    updateStatus(appContext, "ব্যর্থ: ক্লাউড ব্যাকআপ সম্পন্ন হতে পারেনি (Backup failed)")
                                }
                            }
                            WorkInfo.State.CANCELLED -> {
                                _isBackupRunning.value = false
                                releaseBackupLock()
                                val stopReasonStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    when (workInfo.stopReason) {
                                        WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "নেটওয়ার্ক সংযোগ বিঘ্নিত হওয়ায় বাতিল হয়েছে"
                                        WorkInfo.STOP_REASON_TIMEOUT -> "সময় অতিক্রম করায় বাতিল হয়েছে"
                                        WorkInfo.STOP_REASON_CANCELLED_BY_APP -> "ক্যান্সেল করা হয়েছে"
                                        else -> "WorkManager বন্ধ করেছে (Stop reason: ${workInfo.stopReason})"
                                    }
                                } else {
                                    "ব্যাকআপ বাতিল হয়েছে (Backup cancelled)"
                                }
                                val currentStatus = _lastStatus.value
                                if (currentStatus == null || (!currentStatus.startsWith("ব্যর্থ") && !currentStatus.startsWith("সফল") && !currentStatus.startsWith("বাতিল"))) {
                                    updateStatus(appContext, "বাতিল: $stopReasonStr")
                                }
                            }
                            WorkInfo.State.ENQUEUED -> {
                                // Waiting in queue
                            }
                            WorkInfo.State.BLOCKED -> {
                                // Blocked waiting for dependencies
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing WorkManager manual backup flow", e)
                }
            }
        }
    }

    fun onUserAccountChanged(context: Context, account: GoogleSignInAccount?) {
        val userKey = UserManager.computeUserKey(account)
        val prefs = getPrefs(context, userKey)
        _lastBackupTime.value = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        _lastStatus.value = prefs.getString(KEY_LAST_STATUS, null)

        if (account != null) {
            checkAndRestoreIfNeeded(context, userKey)
            schedulePeriodicBackup(context)
        } else {
            cancelPeriodicBackup(context)
        }
    }

    private fun getPrefs(context: Context, userKey: String = UserManager.getCurrentUserKey(context)): SharedPreferences {
        return context.getSharedPreferences("${PREFS_NAME}_$userKey", Context.MODE_PRIVATE)
    }

    fun schedulePeriodicBackup(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.i(TAG, "No Google account connected. Skipping periodic backup scheduling.")
            cancelPeriodicBackup(context)
            return
        }

        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<BackupWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling periodic backup", e)
        }
    }

    fun cancelPeriodicBackup(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling periodic backup", e)
        }
    }

    fun triggerImmediateBackup(context: Context) {
        if (_isBackupRunning.value || backupExecutionLock.get()) {
            Log.w(TAG, "Backup is already running or locked. Marking pending changes.")
            hasPendingChanges.set(true)
            return
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            updateStatus(context, "ব্যর্থ: গুগল অ্যাকাউন্ট সংযুক্ত নেই (Google Account not connected)")
            return
        }

        _isBackupRunning.value = true
        updateStatus(context, "ক্লাউড ব্যাকআপ শুরু হচ্ছে...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val manualWork = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            WORK_NAME_MANUAL,
            ExistingWorkPolicy.KEEP,
            manualWork
        )
    }

    fun triggerImmediateRestore(context: Context, userKey: String = UserManager.getCurrentUserKey(context)) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            updateStatus(context, "ব্যর্থ: গুগল অ্যাকাউন্ট সংযুক্ত নেই", userKey)
            return
        }
        updateStatus(context, "ক্লাউড থেকে রিস্টোর শুরু হচ্ছে...", userKey)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val restoreWork = OneTimeWorkRequestBuilder<RestoreWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "amar_dokan_restore_$userKey",
            ExistingWorkPolicy.REPLACE,
            restoreWork
        )
    }

    fun getManualWorkInfo(context: Context): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(WORK_NAME_MANUAL)
    }

    fun setBackupRunning(running: Boolean) {
        _isBackupRunning.value = running
        if (!running) {
            releaseBackupLock()
        }
    }

    fun updateStatus(context: Context, status: String, userKey: String = UserManager.getCurrentUserKey(context)) {
        if (userKey == UserManager.getCurrentUserKey(context)) {
            _lastStatus.value = status
        }
        getPrefs(context, userKey).edit().putString(KEY_LAST_STATUS, status).apply()
    }

    fun updateSuccess(context: Context, summary: String, userKey: String = UserManager.getCurrentUserKey(context)) {
        val now = System.currentTimeMillis()
        if (userKey == UserManager.getCurrentUserKey(context)) {
            _lastBackupTime.value = now
            _lastStatus.value = summary
            _isBackupRunning.value = false
        }
        releaseBackupLock()
        getPrefs(context, userKey).edit()
            .putLong(KEY_LAST_BACKUP_TIME, now)
            .putString(KEY_LAST_STATUS, summary)
            .putString(KEY_LAST_SUMMARY, summary)
            .apply()
    }

    fun saveRootFolderId(context: Context, id: String, userKey: String = UserManager.getCurrentUserKey(context)) {
        getPrefs(context, userKey).edit().putString(KEY_ROOT_FOLDER_ID, id).apply()
    }

    fun saveCashMemoFolderId(context: Context, id: String, userKey: String = UserManager.getCurrentUserKey(context)) {
        getPrefs(context, userKey).edit().putString(KEY_CASH_MEMO_FOLDER_ID, id).apply()
    }

    fun getRootFolderId(context: Context, userKey: String = UserManager.getCurrentUserKey(context)): String? =
        getPrefs(context, userKey).getString(KEY_ROOT_FOLDER_ID, null)

    fun getCashMemoFolderId(context: Context, userKey: String = UserManager.getCurrentUserKey(context)): String? =
        getPrefs(context, userKey).getString(KEY_CASH_MEMO_FOLDER_ID, null)

    fun checkAndRestoreIfNeeded(context: Context, userKey: String = UserManager.getCurrentUserKey(context)) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return
        val prefs = getPrefs(context, userKey)
        val hasRestoredOrSynced = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L) > 0L

        // If this user has never restored or synced on this device, check Google Drive to restore historical data
        if (!hasRestoredOrSynced) {
            updateStatus(context, "ক্লাউড থেকে আগের ডাটা চেক করা হচ্ছে...", userKey)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val restoreWork = OneTimeWorkRequestBuilder<RestoreWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "amar_dokan_restore_$userKey",
                ExistingWorkPolicy.KEEP,
                restoreWork
            )
        }
    }
}
