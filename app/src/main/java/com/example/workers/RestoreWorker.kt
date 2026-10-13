package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.util.GoogleApiService
import com.example.util.UserManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RestoreWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "RestoreWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null) {
            Log.w(tag, "Restore skipped: Google Account not connected.")
            BackupManager.updateStatus(applicationContext, "ব্যর্থ: গুগল অ্যাকাউন্ট সংযুক্ত নেই")
            return@withContext Result.failure()
        }

        val userKey = UserManager.computeUserKey(account)
        Log.i(tag, "Starting automated Google Drive folder check for $userKey (${account.email})...")
        BackupManager.updateStatus(applicationContext, "ক্লাউড থেকে আগের ফোল্ডার স্ট্রাকচার চেক করা হচ্ছে...", userKey)
        com.example.util.NotificationHelper.showNotification(applicationContext, "রিস্টোর", "রিস্টোর শুরু হচ্ছে...")

        try {
            val apiService = GoogleApiService(applicationContext)

            // Locate root folder "Telecom Cyber Cafe" in Google Drive
            val rootFolderId = apiService.findFolder("Telecom Cyber Cafe")
            if (rootFolderId == null) {
                Log.i(tag, "No existing 'Telecom Cyber Cafe' folder in Google Drive for user $userKey.")
                BackupManager.updateStatus(applicationContext, "ক্লাউডে কোনো পূর্বের ব্যাকআপ ফোল্ডার নেই (নতুন অ্যাকাউন্ট)", userKey)
                return@withContext Result.success()
            }
            BackupManager.saveRootFolderId(applicationContext, rootFolderId, userKey)

            val cashMemoFolderId = apiService.findFolder("Cash_Memo_PDF", rootFolderId)
            if (cashMemoFolderId != null) {
                BackupManager.saveCashMemoFolderId(applicationContext, cashMemoFolderId, userKey)
            }

            // --- Database JSON Restore ---
            val backupFileId = apiService.findFileInFolder("database_backup.json", rootFolderId)
            if (backupFileId != null) {
                val backupFile = java.io.File(applicationContext.cacheDir, "database_backup.json")
                apiService.downloadFile(backupFileId, backupFile)
                
                val json = backupFile.readText()
                val moshi = com.squareup.moshi.Moshi.Builder().build()
                val adapter = moshi.adapter(com.example.amardokan.data.DatabaseBackup::class.java)
                val dbBackup = adapter.fromJson(json)
                
                if (dbBackup != null) {
                    val db = AppDatabase.getDatabase(applicationContext, userKey)
                    val dao = db.amarDokanDao()
                    
                    // Transaction is handled by @Transaction annotation in dao.restoreAllData
                    dao.restoreAllData(
                        dbBackup.customers,
                        dbBackup.services,
                        dbBackup.bills,
                        dbBackup.billItems,
                        dbBackup.payments
                    )
                    Log.i(tag, "Database successfully restored from Drive.")
                }
            }

            Log.i(tag, "Restore check completed successfully.")
            BackupManager.updateSuccess(applicationContext, "রিস্টোর চেক সম্পন্ন", userKey)
            com.example.util.NotificationHelper.showNotification(applicationContext, "রিস্টোর", "রিস্টোর সফল হয়েছে।")
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Restore check failed for user $userKey", e)
            val errorSummary = "ব্যর্থ: রিস্টোর চেক সম্পন্ন করা যায়নি (${e.message ?: "Error"})"
            BackupManager.updateStatus(applicationContext, errorSummary, userKey)
            com.example.util.NotificationHelper.showNotification(applicationContext, "রিস্টোর", "রিস্টোর ব্যর্থ হয়েছে: ${e.message ?: "Error"}")
            Result.failure()
        }
    }
}
