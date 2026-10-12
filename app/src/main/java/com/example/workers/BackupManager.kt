package com.example.workers

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
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
    private const val KEY_SPREADSHEET_ID = "drive_spreadsheet_id"
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
        val prefs = getPrefs(appContext)
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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun schedulePeriodicBackup(context: Context) {
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
    }

    fun triggerImmediateBackup(context: Context) {
        if (_isBackupRunning.value || backupExecutionLock.get()) {
            Log.w(TAG, "Backup is already running or locked. Ignoring duplicate trigger.")
            updateStatus(context, "ব্যাকআপ ইতিমধ্যে চলছে, অনুগ্রহ করে অপেক্ষা করুন...")
            return
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            updateStatus(context, "ব্যর্থ: গুগল অ্যাকাউন্ট সংযুক্ত নেই (Google Account not connected)")
            return
        }

        _isBackupRunning.value = true
        updateStatus(context, "ক্লাউড ব্যাকআপ শুরু হচ্ছে...")

        val manualWork = OneTimeWorkRequestBuilder<BackupWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val workManager = WorkManager.getInstance(context)
        // CRITICAL: Use ExistingWorkPolicy.KEEP so an in-flight manual backup is NEVER cancelled or replaced!
        workManager.enqueueUniqueWork(
            WORK_NAME_MANUAL,
            ExistingWorkPolicy.KEEP,
            manualWork
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

    fun updateStatus(context: Context, status: String) {
        _lastStatus.value = status
        getPrefs(context).edit().putString(KEY_LAST_STATUS, status).apply()
    }

    fun updateSuccess(context: Context, summary: String) {
        val now = System.currentTimeMillis()
        _lastBackupTime.value = now
        _lastStatus.value = summary
        _isBackupRunning.value = false
        releaseBackupLock()
        getPrefs(context).edit()
            .putLong(KEY_LAST_BACKUP_TIME, now)
            .putString(KEY_LAST_STATUS, summary)
            .putString(KEY_LAST_SUMMARY, summary)
            .apply()
    }

    fun saveRootFolderId(context: Context, id: String) {
        getPrefs(context).edit().putString(KEY_ROOT_FOLDER_ID, id).apply()
    }

    fun saveSpreadsheetId(context: Context, id: String) {
        getPrefs(context).edit().putString(KEY_SPREADSHEET_ID, id).apply()
    }

    fun saveCashMemoFolderId(context: Context, id: String) {
        getPrefs(context).edit().putString(KEY_CASH_MEMO_FOLDER_ID, id).apply()
    }

    fun getRootFolderId(context: Context): String? = getPrefs(context).getString(KEY_ROOT_FOLDER_ID, null)
    fun getSpreadsheetId(context: Context): String? = getPrefs(context).getString(KEY_SPREADSHEET_ID, null)
    fun checkAndRestoreIfNeeded(context: Context) {
        // Simple check: if no backup folders initialized, it's likely a new install
        if (getRootFolderId(context) == null) {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                // Trigger Restore check in background
                val restoreWork = OneTimeWorkRequestBuilder<RestoreWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork("amar_dokan_restore", ExistingWorkPolicy.KEEP, restoreWork)
            }
        }
    }
}
