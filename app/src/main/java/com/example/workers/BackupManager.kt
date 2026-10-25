package com.example.workers

import android.content.Context
import com.example.amardokan.data.AmarDokanBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * BackupManager bridge for offline local storage backup.
 * Google Drive cloud backup has been permanently removed.
 */
object BackupManager {
    val lastBackupTime: StateFlow<Long> get() = AmarDokanBackupManager.lastBackupTime
    val lastStatus: StateFlow<String?> get() = AmarDokanBackupManager.lastStatus
    val isBackupRunning: StateFlow<Boolean> get() = AmarDokanBackupManager.isBackupRunning

    fun initialize(context: Context) {
        AmarDokanBackupManager.init(context)
    }

    fun schedulePeriodicBackup(context: Context) {
        // Permanently disabled: Google Drive cloud backup removed in favor of local storage
    }

    fun cancelPeriodicBackup(context: Context) {
        // No-op
    }

    fun triggerImmediateBackup(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            AmarDokanBackupManager.createLocalBackup(context)
        }
    }

    fun triggerImmediateRestore(context: Context, userKey: String = "") {
        // No-op for cloud restore
    }

    fun onUserAccountChanged(context: Context, account: Any?) {
        // No-op: Google accounts are no longer used for Amar Dokan backup
    }
}
