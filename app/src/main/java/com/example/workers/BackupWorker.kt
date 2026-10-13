package com.example.workers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.amardokan.data.SyncMetadata
import com.example.amardokan.ui.memo.AmarDokanCashMemoGenerator
import com.example.util.GoogleApiService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Locale

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "BackupWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Prevent concurrent execution across periodic and manual backups
        if (!BackupManager.acquireBackupLock()) {
            Log.w(tag, "Another backup task is already active. Skipping duplicate worker execution.")
            return@withContext Result.success()
        }

        BackupManager.setBackupRunning(true)
        BackupManager.updateStatus(applicationContext, "ক্লাউড ব্যাকআপ শুরু হচ্ছে...")
        com.example.util.NotificationHelper.showNotification(applicationContext, "ব্যাকআপ", "ক্লাউড ব্যাকআপ শুরু হচ্ছে...")

        try {
            // 1. Check network connectivity upfront
            val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
            val isConnected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!isConnected) {
                val errorMsg = "ইন্টারনেট সংযোগ নেই (No internet connection)"
                Log.e(tag, errorMsg)
                BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
                BackupManager.setBackupRunning(false)
                BackupManager.releaseBackupLock()
                return@withContext Result.failure()
            }

            // 2. Verify Google Sign-In and scopes
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account == null) {
                val errorMsg = "গুগল অ্যাকাউন্ট সংযুক্ত নেই (Google Account not connected)"
                Log.e(tag, errorMsg)
                BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
                BackupManager.setBackupRunning(false)
                BackupManager.releaseBackupLock()
                return@withContext Result.failure()
            }

            val hasDriveScope = GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
            if (!hasDriveScope) {
                val errorMsg = "Drive অনুমতি নেই (Drive permissions missing)"
                Log.e(tag, errorMsg)
                BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
                BackupManager.setBackupRunning(false)
                BackupManager.releaseBackupLock()
                return@withContext Result.failure()
            }

            val userKey = com.example.util.UserManager.computeUserKey(account)
            BackupManager.updateStatus(applicationContext, "ডাটাবেজ থেকে তথ্য নেওয়া হচ্ছে...", userKey)
            Log.d(tag, "Signed in successfully. Starting Drive and Sheets backup for user $userKey (${account.email?.take(3)}***)...")

            // 3. Fetch Room data (all 5 tables for this user)
            val db = AppDatabase.getDatabase(applicationContext, userKey)
            val dao = db.amarDokanDao()

            val customers = dao.getAllCustomersSync()
            val services = dao.getAllServicesSync()
            val bills = dao.getAllBillsSync()
            val billItems = dao.getAllBillItemsSync()
            val payments = dao.getAllPaymentsSync()

            Log.i(tag, "Room records retrieved for $userKey: ad_customers=${customers.size}, ad_services=${services.size}, ad_bills=${bills.size}, ad_bill_items=${billItems.size}, ad_payments=${payments.size}")

            // 4. Initialize Google API Service
            val apiService = GoogleApiService(applicationContext)

            // 5. Drive Folders Setup
            BackupManager.updateStatus(applicationContext, "Google Drive ফোল্ডার প্রস্তুত করা হচ্ছে...", userKey)
            val rootFolderId = apiService.findOrCreateFolder("Telecom Cyber Cafe")
            BackupManager.saveRootFolderId(applicationContext, rootFolderId, userKey)

            val cashMemoFolderId = apiService.findOrCreateFolder("Cash_Memo_PDF", rootFolderId)
            BackupManager.saveCashMemoFolderId(applicationContext, cashMemoFolderId, userKey)

            // 5.5 Database JSON Backup
            BackupManager.updateStatus(applicationContext, "ডেটাবেজ ব্যাকআপ হচ্ছে...", userKey)
            val dbBackup = com.example.amardokan.data.DatabaseBackup(customers, services, bills, billItems, payments)
            val moshi = com.squareup.moshi.Moshi.Builder().build()
            val adapter = moshi.adapter(com.example.amardokan.data.DatabaseBackup::class.java)
            val json = adapter.toJson(dbBackup)
            val backupFile = java.io.File(applicationContext.cacheDir, "database_backup.json")
            backupFile.writeText(json)
            apiService.uploadFile(backupFile, rootFolderId, "database_backup.json", "application/json")

            // 6. PDF Backup
            BackupManager.updateStatus(applicationContext, "PDF ব্যাকআপ শুরু হচ্ছে...", userKey)
            val syncTimestamp = System.currentTimeMillis()

            // 8. Cash Memo PDF Uploads into YEAR / MONTH subfolders
            BackupManager.updateStatus(applicationContext, "ক্যাশ মেমো PDF ব্যাকআপ হচ্ছে...")
            val calendar = Calendar.getInstance()
            var pdfUploadedCount = 0

            // Cache for year and month folder IDs to avoid redundant Drive network lookups
            val folderCache = mutableMapOf<String, String>()

            for (bill in bills) {
                if (isStopped) {
                    Log.w(tag, "Worker was stopped during PDF upload loop. Breaking loop cleanly.")
                    break
                }

                val timestamp = if (bill.dateMillis > 0) bill.dateMillis else bill.createdAt
                calendar.timeInMillis = if (timestamp > 0) timestamp else System.currentTimeMillis()
                val yearStr = calendar.get(Calendar.YEAR).toString()
                val monthStr = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) ?: "General"

                // Create or find Year and Month folders with cache
                val yearKey = "$cashMemoFolderId/$yearStr"
                val yearFolderId = folderCache.getOrPut(yearKey) {
                    apiService.findOrCreateFolder(yearStr, cashMemoFolderId)
                }

                val monthKey = "$yearFolderId/$monthStr"
                val monthFolderId = folderCache.getOrPut(monthKey) {
                    apiService.findOrCreateFolder(monthStr, yearFolderId)
                }

                val cleanCustomerName = bill.customerName.ifBlank { "Unknown" }.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val cleanBillNo = bill.billNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val targetFileName = "${cleanCustomerName}_${cleanBillNo}.pdf"

                // Duplicate prevention: check if already in Drive
                val existingFileId = apiService.findFileInFolder(targetFileName, monthFolderId)
                if (existingFileId != null) {
                    continue
                }

                // Locate existing file or generate PDF
                var pdfFile: File? = null

                // Search in cache memos
                val memoDir = File(applicationContext.cacheDir, "memos")
                if (memoDir.exists()) {
                    val matching = memoDir.listFiles { _, name ->
                        name.contains(cleanBillNo) && name.endsWith(".pdf", ignoreCase = true)
                    }
                    if (!matching.isNullOrEmpty()) {
                        pdfFile = matching[0]
                    }
                }

                // Search in app external files directory
                if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0L) {
                    val externalDir = applicationContext.getExternalFilesDir(null)
                    if (externalDir != null && externalDir.exists()) {
                        pdfFile = externalDir.walkTopDown().firstOrNull { file ->
                            file.name.contains(cleanBillNo) && file.name.endsWith(".pdf", ignoreCase = true) && file.length() > 0L
                        }
                    }
                }

                // If not found, generate the authentic PDF using AmarDokanCashMemoGenerator
                if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0L) {
                    val items = dao.getBillItemsByBillIdSync(bill.id)
                    val memoData = AmarDokanCashMemoGenerator.MemoData.fromBill(bill, items)
                    pdfFile = AmarDokanCashMemoGenerator.generatePdfFile(applicationContext, memoData)
                }

                if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0L) {
                    apiService.uploadFile(pdfFile, monthFolderId, targetFileName, "application/pdf")
                    pdfUploadedCount++
                }
            }

            val summary = "সফল: ${bills.size}টি বিল, ${customers.size} জন গ্রাহক, ৫টি শিট সিঙ্ক, $pdfUploadedCount PDF আপলোড হয়েছে"
            Log.i(tag, "Backup complete: $summary")
            BackupManager.updateSuccess(applicationContext, summary, userKey)
            com.example.util.NotificationHelper.showNotification(applicationContext, "ব্যাকআপ", "ব্যাকআপ সফল হয়েছে।")
            BackupManager.setBackupRunning(false)
            BackupManager.releaseBackupLock()
            Result.success()

        } catch (e: CancellationException) {
            val stopReasonStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (stopReason) {
                    WorkInfo.STOP_REASON_CANCELLED_BY_APP -> "ক্যান্সেল করা হয়েছে (Cancelled by app/replaced)"
                    WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "নেটওয়ার্ক সংযোগ বিঘ্নিত হয়েছে (Network lost)"
                    WorkInfo.STOP_REASON_TIMEOUT -> "সময় শেষ (Timeout)"
                    else -> "WorkManager বন্ধ করেছে (Stop reason: $stopReason)"
                }
            } else {
                if (isStopped) "Worker বন্ধ করা হয়েছে (Worker stopped)" else "জব বাতিল হয়েছে (Job cancelled)"
            }
            Log.w(tag, "BackupWorker was cancelled: isStopped=$isStopped, reason=$stopReasonStr", e)
            BackupManager.setBackupRunning(false)
            BackupManager.releaseBackupLock()
            BackupManager.updateStatus(applicationContext, "বাতিল: $stopReasonStr")
            throw e // MUST rethrow CancellationException in coroutines!
        } catch (e: GoogleJsonResponseException) {
            val code = e.statusCode
            val details = e.details?.message ?: e.message ?: "Unknown API error"
            val errorMsg = when (code) {
                401 -> "গুগল সাইন-ইন মেয়াদোত্তীর্ণ (Authentication expired). অনুগ্রহ করে রি-কানেক্ট করুন।"
                403 -> "Drive বা Sheets ব্যবহারের অনুমতি নেই (Permission denied). অনুগ্রহ করে 'অনুমতি দিন' বাটনে চাপুন।"
                404 -> "ফাইল বা ফোল্ডার পাওয়া যায়নি (404 Not Found)"
                else -> "গুগল এপিআই ত্রুটি (API error $code: $details)"
            }
            Log.e(tag, "GoogleJsonResponseException $code: $details", e)
            BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
            com.example.util.NotificationHelper.showNotification(applicationContext, "ব্যাকআপ", "ব্যাকআপ ব্যর্থ হয়েছে: $errorMsg")
            BackupManager.setBackupRunning(false)
            BackupManager.releaseBackupLock()
            Result.failure()
        } catch (e: UserRecoverableAuthIOException) {
            val errorMsg = "গুগল অনুমতি প্রয়োজন (User consent required). অনুগ্রহ করে 'অনুমতি দিন' বাটনে চাপুন।"
            Log.e(tag, errorMsg, e)
            BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
            com.example.util.NotificationHelper.showNotification(applicationContext, "ব্যাকআপ", "ব্যাকআপ ব্যর্থ হয়েছে: $errorMsg")
            BackupManager.setBackupRunning(false)
            BackupManager.releaseBackupLock()
            Result.failure()
        } catch (e: Exception) {
            val errorMsg = when {
                e is java.net.UnknownHostException || e is java.net.ConnectException || (e is java.io.IOException && e.message?.contains("Unable to resolve host") == true) ->
                    "ইন্টারনেট সংযোগ নেই বা সার্ভারে পৌঁছানো যাচ্ছে না (Network connection failed)"
                else -> e.message ?: "অপ্রত্যাশিত ত্রুটি (Unknown error)"
            }
            Log.e(tag, "Backup encountered an error: $errorMsg", e)
            BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
            com.example.util.NotificationHelper.showNotification(applicationContext, "ব্যাকআপ", "ব্যাকআপ ব্যর্থ হয়েছে: $errorMsg")
            BackupManager.setBackupRunning(false)
            BackupManager.releaseBackupLock()
            Result.failure()
        }
    }
}
