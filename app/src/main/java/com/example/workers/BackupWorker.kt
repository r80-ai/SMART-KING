package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.amardokan.data.AmarDokanDao
import com.example.amardokan.data.SyncMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.amarDokanDao()
        val apiService = com.example.util.GoogleApiService(applicationContext)

        // 1. Fetch pending records from SyncMetadata
        val pendingSyncs = dao.getPendingSyncs()
        if (pendingSyncs.isEmpty()) {
            return@withContext Result.success()
        }

        try {
            val sheetsService = apiService.getSheetsService()
            // 2. Implementation: Upload records to Google Sheets
            // Logic: Create/update sheets per table
            // For each record, if successful, mark as synced in SyncMetadata
            
            // Simplified for now: Log pending count
            android.util.Log.d("BackupWorker", "Syncing ${pendingSyncs.size} records")
            
            // Mark as synced
            pendingSyncs.forEach { metadata ->
                dao.updateSyncMetadata(metadata.copy(isPending = false))
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("BackupWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
