package com.example.workers

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

object BackupManager {
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

    fun initialize(context: Context) {
        val prefs = getPrefs(context)
        _lastBackupTime.value = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        _lastStatus.value = prefs.getString(KEY_LAST_STATUS, null)
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
        _isBackupRunning.value = true
        _lastStatus.value = "ক্লাউড ব্যাকআপ শুরু হচ্ছে..."

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
            ExistingWorkPolicy.REPLACE,
            manualWork
        )
    }

    fun getManualWorkInfo(context: Context): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(WORK_NAME_MANUAL)
    }

    fun setBackupRunning(running: Boolean) {
        _isBackupRunning.value = running
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
    fun getCashMemoFolderId(context: Context): String? = getPrefs(context).getString(KEY_CASH_MEMO_FOLDER_ID, null)
}
