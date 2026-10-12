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
import com.google.api.services.sheets.v4.SheetsScopes
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
            val hasSheetsScope = GoogleSignIn.hasPermissions(account, Scope(SheetsScopes.SPREADSHEETS))
            if (!hasDriveScope || !hasSheetsScope) {
                val errorMsg = "Drive বা Sheets অনুমতি নেই (Drive/Sheets permissions missing)"
                Log.e(tag, errorMsg)
                BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
                BackupManager.setBackupRunning(false)
                BackupManager.releaseBackupLock()
                return@withContext Result.failure()
            }

            BackupManager.updateStatus(applicationContext, "ডাটাবেজ থেকে তথ্য নেওয়া হচ্ছে...")
            Log.d(tag, "Signed in successfully. Starting Drive and Sheets backup for ${account.email?.take(3)}***...")

            // 3. Fetch Room data (all 5 tables)
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.amarDokanDao()

            val customers = dao.getAllCustomersSync()
            val services = dao.getAllServicesSync()
            val bills = dao.getAllBillsSync()
            val billItems = dao.getAllBillItemsSync()
            val payments = dao.getAllPaymentsSync()

            Log.i(tag, "Room records retrieved: ad_customers=${customers.size}, ad_services=${services.size}, ad_bills=${bills.size}, ad_bill_items=${billItems.size}, ad_payments=${payments.size}")

            // 4. Initialize Google API Service
            val apiService = GoogleApiService(applicationContext)

            // 5. Drive Folders & Spreadsheet Setup
            BackupManager.updateStatus(applicationContext, "Google Drive ফোল্ডার প্রস্তুত করা হচ্ছে...")
            val rootFolderId = apiService.findOrCreateFolder("Telecom Cyber Cafe")
            BackupManager.saveRootFolderId(applicationContext, rootFolderId)

            val spreadsheetId = apiService.findOrCreateSpreadsheet("Billing_Data", rootFolderId)
            BackupManager.saveSpreadsheetId(applicationContext, spreadsheetId)

            val cashMemoFolderId = apiService.findOrCreateFolder("Cash_Memo_PDF", rootFolderId)
            BackupManager.saveCashMemoFolderId(applicationContext, cashMemoFolderId)

            // 6. Ensure all 5 Tabs exist in Billing_Data
            BackupManager.updateStatus(applicationContext, "Google Sheets ট্যাব প্রস্তুত করা হচ্ছে...")
            val requiredTabs = listOf("ad_customers", "ad_services", "ad_bills", "ad_bill_items", "ad_payments")
            apiService.ensureSheetTabs(spreadsheetId, requiredTabs)

            // 7. Synchronize all 5 Tables with Headers and Data (ValueInputOption.RAW)
            BackupManager.updateStatus(applicationContext, "Google Sheets-এ টেবিল সিনক্রোনাইজ হচ্ছে...")
            val syncTimestamp = System.currentTimeMillis()

            // --- TAB 1: ad_customers ---
            val customerHeaders = listOf("ID", "Customer Name", "Phone", "Address", "Notes", "Created At")
            val customerRecords = customers.map { c ->
                listOf(c.id, c.name, c.phone, c.address, c.notes, c.createdAt)
            }
            Log.i(tag, "Syncing [ad_customers]: ${customers.size} local records to sheet...")
            val (custChanges, custTotal) = apiService.syncTableWithIdDeduplication(
                spreadsheetId = spreadsheetId,
                sheetName = "ad_customers",
                headers = customerHeaders,
                records = customerRecords,
                idColumnIndex = 0,
                valueInputOption = "RAW"
            )
            // Mark sync metadata for successfully processed customers
            for (c in customers) {
                dao.insertSyncMetadata(
                    SyncMetadata(
                        id = "customer_${c.id}",
                        entityType = "customer",
                        entityId = c.id,
                        lastSyncedAt = syncTimestamp,
                        isPending = false
                    )
                )
            }
            Log.i(tag, "[ad_customers] Sync complete. Processed $custTotal records, wrote/updated $custChanges rows.")

            // --- TAB 2: ad_services ---
            val serviceHeaders = listOf("ID", "Name (Bengali)", "Name (English)", "Default Rate", "Unit", "Is Default")
            val serviceRecords = services.map { s ->
                listOf(s.id, s.nameBn, s.nameEn, s.defaultRate, s.unitBn, if (s.isDefault) "YES" else "NO")
            }
            Log.i(tag, "Syncing [ad_services]: ${services.size} local records to sheet...")
            val (srvChanges, srvTotal) = apiService.syncTableWithIdDeduplication(
                spreadsheetId = spreadsheetId,
                sheetName = "ad_services",
                headers = serviceHeaders,
                records = serviceRecords,
                idColumnIndex = 0,
                valueInputOption = "RAW"
            )
            for (s in services) {
                dao.insertSyncMetadata(
                    SyncMetadata(
                        id = "service_${s.id}",
                        entityType = "service",
                        entityId = s.id,
                        lastSyncedAt = syncTimestamp,
                        isPending = false
                    )
                )
            }
            Log.i(tag, "[ad_services] Sync complete. Processed $srvTotal records, wrote/updated $srvChanges rows.")

            // --- TAB 3: ad_bills ---
            val billHeaders = listOf(
                "ID", "Bill Number", "Date Formatted", "Date Timestamp", "Customer ID",
                "Customer Name", "Phone", "Payment Type", "Subtotal", "Discount",
                "Grand Total", "Cash Received", "Notes", "Language", "Created At"
            )
            val billRecords = bills.map { b ->
                listOf(
                    b.id,
                    b.billNumber,
                    b.dateFormatted,
                    b.dateMillis,
                    b.customerId ?: "",
                    b.customerName,
                    b.customerPhone,
                    b.paymentType,
                    b.subtotal,
                    b.discount,
                    b.grandTotal,
                    b.cashReceived,
                    b.notes,
                    b.language,
                    b.createdAt
                )
            }
            Log.i(tag, "Syncing [ad_bills]: ${bills.size} local records to sheet...")
            val (billChanges, billTotal) = apiService.syncTableWithIdDeduplication(
                spreadsheetId = spreadsheetId,
                sheetName = "ad_bills",
                headers = billHeaders,
                records = billRecords,
                idColumnIndex = 0,
                valueInputOption = "RAW"
            )
            for (b in bills) {
                dao.insertSyncMetadata(
                    SyncMetadata(
                        id = "bill_${b.id}",
                        entityType = "bill",
                        entityId = b.id,
                        lastSyncedAt = syncTimestamp,
                        isPending = false
                    )
                )
            }
            Log.i(tag, "[ad_bills] Sync complete. Processed $billTotal records, wrote/updated $billChanges rows.")

            // --- TAB 4: ad_bill_items ---
            val itemHeaders = listOf("ID", "Bill ID", "Service Name", "Quantity", "Unit Rate", "Line Total")
            val itemRecords = billItems.map { item ->
                listOf(item.id, item.billId, item.serviceName, item.quantity, item.unitRate, item.lineTotal)
            }
            Log.i(tag, "Syncing [ad_bill_items]: ${billItems.size} local records to sheet...")
            val (itemChanges, itemTotal) = apiService.syncTableWithIdDeduplication(
                spreadsheetId = spreadsheetId,
                sheetName = "ad_bill_items",
                headers = itemHeaders,
                records = itemRecords,
                idColumnIndex = 0,
                valueInputOption = "RAW"
            )
            for (item in billItems) {
                dao.insertSyncMetadata(
                    SyncMetadata(
                        id = "bill_item_${item.id}",
                        entityType = "bill_item",
                        entityId = item.id,
                        lastSyncedAt = syncTimestamp,
                        isPending = false
                    )
                )
            }
            Log.i(tag, "[ad_bill_items] Sync complete. Processed $itemTotal records, wrote/updated $itemChanges rows.")

            // --- TAB 5: ad_payments ---
            val paymentHeaders = listOf("ID", "Customer ID", "Customer Name", "Date Formatted", "Date Timestamp", "Amount", "Payment Mode", "Notes", "Created At")
            val paymentRecords = payments.map { p ->
                listOf(p.id, p.customerId, p.customerName, p.dateFormatted, p.dateMillis, p.amount, p.paymentMode, p.notes, p.createdAt)
            }
            Log.i(tag, "Syncing [ad_payments]: ${payments.size} local records to sheet...")
            val (payChanges, payTotal) = apiService.syncTableWithIdDeduplication(
                spreadsheetId = spreadsheetId,
                sheetName = "ad_payments",
                headers = paymentHeaders,
                records = paymentRecords,
                idColumnIndex = 0,
                valueInputOption = "RAW"
            )
            for (p in payments) {
                dao.insertSyncMetadata(
                    SyncMetadata(
                        id = "payment_${p.id}",
                        entityType = "payment",
                        entityId = p.id,
                        lastSyncedAt = syncTimestamp,
                        isPending = false
                    )
                )
            }
            Log.i(tag, "[ad_payments] Sync complete. Processed $payTotal records, wrote/updated $payChanges rows.")

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

                val cleanBillNo = bill.billNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val targetFileName = "${cleanBillNo}.pdf"

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

            val summary = "সফল: ${bills.size}টি বিল, ${customers.size}টি গ্রাহক, ৫টি শিট সিঙ্ক, $pdfUploadedCount PDF আপলোড হয়েছে"
            Log.i(tag, "Backup complete: $summary")
            BackupManager.updateSuccess(applicationContext, summary)
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
            BackupManager.setBackupRunning(false)
            BackupManager.releaseBackupLock()
            Result.failure()
        } catch (e: UserRecoverableAuthIOException) {
            val errorMsg = "গুগল অনুমতি প্রয়োজন (User consent required). অনুগ্রহ করে 'অনুমতি দিন' বাটনে চাপুন।"
            Log.e(tag, errorMsg, e)
            BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
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
            BackupManager.setBackupRunning(false)
            BackupManager.releaseBackupLock()
            Result.failure()
        }
    }
}
