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

    companion object {
        lateinit var instance: AmarDokanApp
            private set
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(this, workManagerConfiguration)
            }
        } catch (e: Exception) {
            // Already initialized or configured
        }

        // Initialize user management
        UserManager.init(this)
        
        // Initialize notification channel
        com.example.util.NotificationHelper.createNotificationChannel(this)
    }
}
