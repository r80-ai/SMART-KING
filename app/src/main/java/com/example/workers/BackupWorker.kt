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

        try {
            // Check if user is signed in
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account == null) {
                android.util.Log.e("BackupWorker", "User not signed in, skipping sync")
                return@withContext Result.failure()
            }

            // 1. Fetch all records
            val customers = dao.getAllCustomersSync()
            val services = dao.getAllServicesSync()
            val bills = dao.getAllBillsSync()
            val billItems = dao.getAllBillItemsSync()
            val payments = dao.getAllPaymentsSync()

            android.util.Log.d("BackupWorker", "Fetched ${customers.size} customers, ${bills.size} bills")

            // 2. Implementation: Upload records to Google Sheets
            // (Sheets upload logic will go here)

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("BackupWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
