package com.example

import android.app.Application
import androidx.room.InvalidationTracker
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.data.AppDatabase
import com.example.util.UserManager
import com.example.workers.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AmarDokanApp : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val attachedObservers = mutableSetOf<String>()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        try {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(this, workManagerConfiguration)
            }
        } catch (e: Exception) {
            // Already initialized or configured
        }

        // 1. Initialize user management
        UserManager.init(this)

        // 2. Initialize BackupManager
        BackupManager.initialize(this)

        // 3. Listen to active user changes to attach InvalidationTracker and check for restore
        applicationScope.launch {
            UserManager.currentUserKey.collect { userKey ->
                val db = AppDatabase.getDatabase(this@AmarDokanApp, userKey)
                if (!attachedObservers.contains(userKey)) {
                    attachedObservers.add(userKey)
                    val observer = object : InvalidationTracker.Observer(
                        "ad_customers", "ad_services", "ad_bills", "ad_bill_items", "ad_payments"
                    ) {
                        override fun onInvalidated(tables: Set<String>) {
                            BackupManager.triggerImmediateBackup(this@AmarDokanApp)
                        }
                    }
                    db.invalidationTracker.addObserver(observer)
                }

                // Auto-restore check if this account has existing backup on Drive
                BackupManager.checkAndRestoreIfNeeded(this@AmarDokanApp, userKey)
            }
        }
    }
}
