package com.example

import android.app.Application
import androidx.room.InvalidationTracker
import com.example.data.AppDatabase
import com.example.workers.BackupManager

class AmarDokanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize BackupManager
        BackupManager.initialize(this)
        
        // 2. Set up Auto-Sync Observer
        val db = AppDatabase.getDatabase(this)
        val observer = object : InvalidationTracker.Observer(
            "ad_customers", "ad_services", "ad_bills", "ad_bill_items", "ad_payments"
        ) {
            override fun onInvalidated(tables: Set<String>) {
                BackupManager.triggerImmediateBackup(this@AmarDokanApp)
            }
        }
        db.invalidationTracker.addObserver(observer)
        
        // 3. Optional: Restore mechanism on startup if backup folders not initialized
        // Note: Actual restore logic to be added in BackupManager
        BackupManager.checkAndRestoreIfNeeded(this)
    }
}
