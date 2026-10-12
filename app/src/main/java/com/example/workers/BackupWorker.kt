package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.amardokan.ui.memo.AmarDokanCashMemoGenerator
import com.example.util.GoogleApiService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
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
        BackupManager.setBackupRunning(true)
        BackupManager.updateStatus(applicationContext, "ক্লাউড ব্যাকআপ চলছে...")

        try {
            // 1. Verify Google Sign-In and scopes
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account == null) {
                val errorMsg = "গুগল অ্যাকাউন্ট সংযুক্ত নেই (Google Account not connected)"
                Log.e(tag, errorMsg)
                BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
                BackupManager.setBackupRunning(false)
                return@withContext Result.failure()
            }

            val hasDriveScope = GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
            val hasSheetsScope = GoogleSignIn.hasPermissions(account, Scope(SheetsScopes.SPREADSHEETS))
            if (!hasDriveScope || !hasSheetsScope) {
                val errorMsg = "Drive বা Sheets অনুমতি নেই (Drive/Sheets permissions missing)"
                Log.e(tag, errorMsg)
                BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
                BackupManager.setBackupRunning(false)
                return@withContext Result.failure()
            }

            Log.d(tag, "Signed in successfully. Starting Drive and Sheets backup...")

            // 2. Fetch Room data
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.amarDokanDao()

            val customers = dao.getAllCustomersSync()
            val services = dao.getAllServicesSync()
            val bills = dao.getAllBillsSync()
            val billItems = dao.getAllBillItemsSync()
            val payments = dao.getAllPaymentsSync()

            Log.d(tag, "Database fetched: ${customers.size} customers, ${services.size} services, ${bills.size} bills, ${billItems.size} items, ${payments.size} payments")

            // 3. Google API Service
            val apiService = GoogleApiService(applicationContext)

            // 4. Drive Folders & Spreadsheet Setup
            val rootFolderId = apiService.findOrCreateFolder("Telecom Cyber Cafe")
            BackupManager.saveRootFolderId(applicationContext, rootFolderId)

            val spreadsheetId = apiService.findOrCreateSpreadsheet("Billing_Data", rootFolderId)
            BackupManager.saveSpreadsheetId(applicationContext, spreadsheetId)

            val cashMemoFolderId = apiService.findOrCreateFolder("Cash_Memo_PDF", rootFolderId)
            BackupManager.saveCashMemoFolderId(applicationContext, cashMemoFolderId)

            // 5. Google Sheets Data Sync (5 Tabs)
            val requiredTabs = listOf("ad_customers", "ad_services", "ad_bills", "ad_bill_items", "ad_payments")
            apiService.ensureSheetTabs(spreadsheetId, requiredTabs)

            // Tab 1: ad_customers
            val customerRows = mutableListOf<List<Any>>()
            customerRows.add(listOf("ID", "Customer Name", "Phone", "Address", "Notes", "Created At"))
            for (c in customers) {
                customerRows.add(listOf(c.id, c.name, c.phone, c.address, c.notes, c.createdAt))
            }
            apiService.writeSheetData(spreadsheetId, "ad_customers", customerRows)

            // Tab 2: ad_services
            val serviceRows = mutableListOf<List<Any>>()
            serviceRows.add(listOf("ID", "Name (Bengali)", "Name (English)", "Default Rate", "Unit", "Is Default"))
            for (s in services) {
                serviceRows.add(listOf(s.id, s.nameBn, s.nameEn, s.defaultRate, s.unitBn, if (s.isDefault) "YES" else "NO"))
            }
            apiService.writeSheetData(spreadsheetId, "ad_services", serviceRows)

            // Tab 3: ad_bills
            val billRows = mutableListOf<List<Any>>()
            billRows.add(listOf(
                "ID", "Bill Number", "Date Formatted", "Date Timestamp", "Customer ID",
                "Customer Name", "Phone", "Payment Type", "Subtotal", "Discount",
                "Grand Total", "Cash Received", "Notes", "Language", "Created At"
            ))
            for (b in bills) {
                billRows.add(listOf(
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
                ))
            }
            apiService.writeSheetData(spreadsheetId, "ad_bills", billRows)

            // Tab 4: ad_bill_items
            val itemRows = mutableListOf<List<Any>>()
            itemRows.add(listOf("ID", "Bill ID", "Service Name", "Quantity", "Unit Rate", "Line Total"))
            for (item in billItems) {
                itemRows.add(listOf(item.id, item.billId, item.serviceName, item.quantity, item.unitRate, item.lineTotal))
            }
            apiService.writeSheetData(spreadsheetId, "ad_bill_items", itemRows)

            // Tab 5: ad_payments
            val paymentRows = mutableListOf<List<Any>>()
            paymentRows.add(listOf("ID", "Customer ID", "Customer Name", "Date Formatted", "Date Timestamp", "Amount", "Payment Mode", "Notes", "Created At"))
            for (p in payments) {
                paymentRows.add(listOf(p.id, p.customerId, p.customerName, p.dateFormatted, p.dateMillis, p.amount, p.paymentMode, p.notes, p.createdAt))
            }
            apiService.writeSheetData(spreadsheetId, "ad_payments", paymentRows)

            // 6. Cash Memo PDF Uploads into YEAR / MONTH subfolders
            val calendar = Calendar.getInstance()
            var pdfUploadedCount = 0

            for (bill in bills) {
                val timestamp = if (bill.dateMillis > 0) bill.dateMillis else bill.createdAt
                calendar.timeInMillis = if (timestamp > 0) timestamp else System.currentTimeMillis()
                val yearStr = calendar.get(Calendar.YEAR).toString()
                val monthStr = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) ?: "General"

                // Create or find Year and Month folders
                val yearFolderId = apiService.findOrCreateFolder(yearStr, cashMemoFolderId)
                val monthFolderId = apiService.findOrCreateFolder(monthStr, yearFolderId)

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
                if (pdfFile == null || !pdfFile.exists()) {
                    val externalDir = applicationContext.getExternalFilesDir(null)
                    if (externalDir != null && externalDir.exists()) {
                        pdfFile = externalDir.walkTopDown().firstOrNull { file ->
                            file.name.contains(cleanBillNo) && file.name.endsWith(".pdf", ignoreCase = true)
                        }
                    }
                }

                // If not found, generate the authentic PDF using AmarDokanCashMemoGenerator
                if (pdfFile == null || !pdfFile.exists()) {
                    val items = dao.getBillItemsByBillIdSync(bill.id)
                    val memoData = AmarDokanCashMemoGenerator.MemoData.fromBill(bill, items)
                    pdfFile = AmarDokanCashMemoGenerator.generatePdfFile(applicationContext, memoData)
                }

                if (pdfFile != null && pdfFile.exists()) {
                    apiService.uploadFile(pdfFile, monthFolderId, targetFileName, "application/pdf")
                    pdfUploadedCount++
                }
            }

            val summary = "সফল: ${bills.size}টি বিল, ${customers.size}টি গ্রাহক, $pdfUploadedCount PDF আপলোড হয়েছে"
            Log.i(tag, "Backup complete: $summary")
            BackupManager.updateSuccess(applicationContext, summary)
            BackupManager.setBackupRunning(false)
            Result.success()

        } catch (e: Exception) {
            val errorMsg = e.message ?: "অপ্রত্যাশিত ত্রুটি (Unknown error)"
            Log.e(tag, "Backup encountered an error: $errorMsg", e)
            BackupManager.updateStatus(applicationContext, "ব্যর্থ: $errorMsg")
            BackupManager.setBackupRunning(false)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
