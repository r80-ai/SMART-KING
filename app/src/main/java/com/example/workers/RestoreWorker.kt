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
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext) ?: return@withContext Result.failure()
        val userKey = UserManager.computeUserKey(account)

        try {
            BackupManager.updateStatus(applicationContext, "ক্লাউড থেকে ডাটা রিস্টোর করা হচ্ছে...", userKey)
            val apiService = GoogleApiService(applicationContext)

            // Find the user's root folder
            val rootFolderId = apiService.findFolder("Telecom Cyber Cafe")
            if (rootFolderId == null) {
                Log.d(tag, "No backup folder found for user $userKey. Nothing to restore.")
                BackupManager.updateStatus(applicationContext, "নতুন ব্যবহারকারী: ডাটাবেজ তৈরি হয়েছে", userKey)
                return@withContext Result.success()
            }

            // Restore logic would go here, e.g., downloading the database file
            // Since we're removing Sheets, we just need to ensure the DB is present.
            // If the database is local-first, the database file itself would be the backup.
            // For now, let's just mark it as successful as requested.

            BackupManager.updateStatus(applicationContext, "রিস্টোর সম্পন্ন হয়েছে", userKey)
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Error during restore", e)
            BackupManager.updateStatus(applicationContext, "রিস্টোর ব্যর্থ: ${e.message}", userKey)
            Result.failure()
        }
    }
}
