package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.util.GoogleApiService
import com.example.util.UserManager
import com.example.amardokan.data.Customer
import com.example.amardokan.data.ServiceItem
import com.example.amardokan.data.Bill
import com.example.amardokan.data.BillItem
import com.example.amardokan.data.Payment
import com.example.amardokan.data.SyncMetadata
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
        Log.i(tag, "Starting automated Google Drive restore check for $userKey (${account.email})...")
        BackupManager.updateStatus(applicationContext, "ক্লাউড থেকে আগের ডাটা চেক করা হচ্ছে...", userKey)

        try {
            val apiService = GoogleApiService(applicationContext)
            val db = AppDatabase.getDatabase(applicationContext, userKey)
            val dao = db.amarDokanDao()

            // 1. Locate root folder "Telecom Cyber Cafe" in Google Drive
            val rootFolderId = apiService.findFolder("Telecom Cyber Cafe")
            if (rootFolderId == null) {
                Log.i(tag, "No existing 'Telecom Cyber Cafe' folder in Google Drive for user $userKey.")
                BackupManager.updateStatus(applicationContext, "ক্লাউডে কোনো পূর্বের ব্যাকআপ নেই (নতুন অ্যাকাউন্ট)", userKey)
                return@withContext Result.success()
            }
            BackupManager.saveRootFolderId(applicationContext, rootFolderId, userKey)

            // 2. Locate spreadsheet "Billing_Data" inside root folder
            val spreadsheetId = apiService.findSpreadsheet("Billing_Data", rootFolderId)
            if (spreadsheetId == null) {
                Log.i(tag, "No 'Billing_Data' spreadsheet found in folder $rootFolderId for user $userKey.")
                BackupManager.updateStatus(applicationContext, "ক্লাউডে কোনো স্প্রেডশিট ডাটা পাওয়া যায়নি", userKey)
                return@withContext Result.success()
            }
            BackupManager.saveSpreadsheetId(applicationContext, spreadsheetId, userKey)

            val cashMemoFolderId = apiService.findFolder("Cash_Memo_PDF", rootFolderId)
            if (cashMemoFolderId != null) {
                BackupManager.saveCashMemoFolderId(applicationContext, cashMemoFolderId, userKey)
            }

            val syncTimestamp = System.currentTimeMillis()

            // 3. Restore Customers (ad_customers)
            BackupManager.updateStatus(applicationContext, "গ্রাহক তথ্য রিস্টোর হচ্ছে...", userKey)
            var restoredCustomers = 0
            val customersData = apiService.readSheetRows(spreadsheetId, "ad_customers")
            if (customersData.size > 1) { // Skip header row
                for (i in 1 until customersData.size) {
                    val row = customersData[i]
                    if (row.size >= 2) {
                        val id = row.getOrNull(0)?.toString()?.toLongOrNull() ?: 0L
                        val name = row.getOrNull(1)?.toString()?.trim() ?: ""
                        val phone = row.getOrNull(2)?.toString()?.trim() ?: ""
                        val address = row.getOrNull(3)?.toString()?.trim() ?: ""
                        val notes = row.getOrNull(4)?.toString()?.trim() ?: ""
                        val createdAt = row.getOrNull(5)?.toString()?.toLongOrNull() ?: syncTimestamp
                        if (name.isNotBlank()) {
                            val savedId = dao.insertCustomer(
                                Customer(
                                    id = id,
                                    name = name,
                                    phone = phone,
                                    address = address,
                                    notes = notes,
                                    createdAt = createdAt
                                )
                            )
                            dao.insertSyncMetadata(
                                SyncMetadata(
                                    id = "customer_$savedId",
                                    entityType = "customer",
                                    entityId = savedId,
                                    lastSyncedAt = syncTimestamp,
                                    isPending = false
                                )
                            )
                            restoredCustomers++
                        }
                    }
                }
            }

            // 4. Restore Services (ad_services)
            BackupManager.updateStatus(applicationContext, "সার্ভিস রেট রিস্টোর হচ্ছে...", userKey)
            var restoredServices = 0
            val servicesData = apiService.readSheetRows(spreadsheetId, "ad_services")
            if (servicesData.size > 1) {
                for (i in 1 until servicesData.size) {
                    val row = servicesData[i]
                    if (row.size >= 4) {
                        val id = row.getOrNull(0)?.toString()?.toLongOrNull() ?: 0L
                        val nameBn = row.getOrNull(1)?.toString()?.trim() ?: ""
                        val nameEn = row.getOrNull(2)?.toString()?.trim() ?: ""
                        val defaultRate = row.getOrNull(3)?.toString()?.toDoubleOrNull() ?: 0.0
                        val unitBn = row.getOrNull(4)?.toString()?.trim() ?: "পাতা"
                        val isDefault = row.getOrNull(5)?.toString()?.trim() == "YES"
                        if (nameBn.isNotBlank()) {
                            val savedId = dao.insertService(
                                ServiceItem(
                                    id = id,
                                    nameBn = nameBn,
                                    nameEn = nameEn,
                                    defaultRate = defaultRate,
                                    unitBn = unitBn,
                                    isDefault = isDefault
                                )
                            )
                            dao.insertSyncMetadata(
                                SyncMetadata(
                                    id = "service_$savedId",
                                    entityType = "service",
                                    entityId = savedId,
                                    lastSyncedAt = syncTimestamp,
                                    isPending = false
                                )
                            )
                            restoredServices++
                        }
                    }
                }
            }

            // 5. Restore Bills (ad_bills)
            BackupManager.updateStatus(applicationContext, "বিলের তথ্য রিস্টোর হচ্ছে...", userKey)
            var restoredBills = 0
            val billsData = apiService.readSheetRows(spreadsheetId, "ad_bills")
            if (billsData.size > 1) {
                for (i in 1 until billsData.size) {
                    val row = billsData[i]
                    if (row.size >= 11) {
                        val id = row.getOrNull(0)?.toString()?.toLongOrNull() ?: 0L
                        val billNumber = row.getOrNull(1)?.toString()?.trim() ?: ""
                        val dateFormatted = row.getOrNull(2)?.toString()?.trim() ?: ""
                        val dateMillis = row.getOrNull(3)?.toString()?.toLongOrNull() ?: syncTimestamp
                        val rawCustId = row.getOrNull(4)?.toString()?.trim()
                        val customerId = if (rawCustId.isNullOrEmpty() || rawCustId == "null") null else rawCustId.toLongOrNull()
                        val customerName = row.getOrNull(5)?.toString()?.trim() ?: ""
                        val customerPhone = row.getOrNull(6)?.toString()?.trim() ?: ""
                        val paymentType = row.getOrNull(7)?.toString()?.trim() ?: "CASH"
                        val subtotal = row.getOrNull(8)?.toString()?.toDoubleOrNull() ?: 0.0
                        val discount = row.getOrNull(9)?.toString()?.toDoubleOrNull() ?: 0.0
                        val grandTotal = row.getOrNull(10)?.toString()?.toDoubleOrNull() ?: 0.0
                        val cashReceived = row.getOrNull(11)?.toString()?.toDoubleOrNull() ?: (if (paymentType == "CASH") grandTotal else 0.0)
                        val notes = row.getOrNull(12)?.toString()?.trim() ?: ""
                        val language = row.getOrNull(13)?.toString()?.trim() ?: "BN"
                        val createdAt = row.getOrNull(14)?.toString()?.toLongOrNull() ?: dateMillis
                        if (billNumber.isNotBlank()) {
                            val savedId = dao.insertBill(
                                Bill(
                                    id = id,
                                    billNumber = billNumber,
                                    dateMillis = dateMillis,
                                    dateFormatted = dateFormatted,
                                    customerId = customerId,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    paymentType = paymentType,
                                    subtotal = subtotal,
                                    discount = discount,
                                    grandTotal = grandTotal,
                                    cashReceived = cashReceived,
                                    notes = notes,
                                    language = language,
                                    createdAt = createdAt
                                )
                            )
                            dao.insertSyncMetadata(
                                SyncMetadata(
                                    id = "bill_$savedId",
                                    entityType = "bill",
                                    entityId = savedId,
                                    lastSyncedAt = syncTimestamp,
                                    isPending = false
                                )
                            )
                            restoredBills++
                        }
                    }
                }
            }

            // 6. Restore Bill Items (ad_bill_items)
            BackupManager.updateStatus(applicationContext, "বিল আইটেম রিস্টোর হচ্ছে...", userKey)
            var restoredItems = 0
            val itemsData = apiService.readSheetRows(spreadsheetId, "ad_bill_items")
            if (itemsData.size > 1) {
                val itemsList = mutableListOf<BillItem>()
                for (i in 1 until itemsData.size) {
                    val row = itemsData[i]
                    if (row.size >= 6) {
                        val id = row.getOrNull(0)?.toString()?.toLongOrNull() ?: 0L
                        val billId = row.getOrNull(1)?.toString()?.toLongOrNull() ?: 0L
                        val serviceName = row.getOrNull(2)?.toString()?.trim() ?: ""
                        val quantity = row.getOrNull(3)?.toString()?.toDoubleOrNull() ?: 1.0
                        val unitRate = row.getOrNull(4)?.toString()?.toDoubleOrNull() ?: 0.0
                        val lineTotal = row.getOrNull(5)?.toString()?.toDoubleOrNull() ?: (quantity * unitRate)
                        if (billId > 0 && serviceName.isNotBlank()) {
                            itemsList.add(
                                BillItem(
                                    id = id,
                                    billId = billId,
                                    serviceName = serviceName,
                                    quantity = quantity,
                                    unitRate = unitRate,
                                    lineTotal = lineTotal
                                )
                            )
                        }
                    }
                }
                if (itemsList.isNotEmpty()) {
                    dao.insertBillItems(itemsList)
                    restoredItems = itemsList.size
                }
            }

            // 7. Restore Payments (ad_payments)
            BackupManager.updateStatus(applicationContext, "পেমেন্ট ও বাকি খাতার হিসাব রিস্টোর হচ্ছে...", userKey)
            var restoredPayments = 0
            val paymentsData = apiService.readSheetRows(spreadsheetId, "ad_payments")
            if (paymentsData.size > 1) {
                for (i in 1 until paymentsData.size) {
                    val row = paymentsData[i]
                    if (row.size >= 6) {
                        val id = row.getOrNull(0)?.toString()?.toLongOrNull() ?: 0L
                        val customerId = row.getOrNull(1)?.toString()?.toLongOrNull() ?: 0L
                        val customerName = row.getOrNull(2)?.toString()?.trim() ?: ""
                        val dateFormatted = row.getOrNull(3)?.toString()?.trim() ?: ""
                        val dateMillis = row.getOrNull(4)?.toString()?.toLongOrNull() ?: syncTimestamp
                        val amount = row.getOrNull(5)?.toString()?.toDoubleOrNull() ?: 0.0
                        val paymentMode = row.getOrNull(6)?.toString()?.trim() ?: "CASH"
                        val notes = row.getOrNull(7)?.toString()?.trim() ?: ""
                        val createdAt = row.getOrNull(8)?.toString()?.toLongOrNull() ?: dateMillis
                        if (amount > 0.0) {
                            val savedId = dao.insertPayment(
                                Payment(
                                    id = id,
                                    customerId = customerId,
                                    customerName = customerName,
                                    dateMillis = dateMillis,
                                    dateFormatted = dateFormatted,
                                    amount = amount,
                                    paymentMode = paymentMode,
                                    notes = notes,
                                    createdAt = createdAt
                                )
                            )
                            dao.insertSyncMetadata(
                                SyncMetadata(
                                    id = "payment_$savedId",
                                    entityType = "payment",
                                    entityId = savedId,
                                    lastSyncedAt = syncTimestamp,
                                    isPending = false
                                )
                            )
                            restoredPayments++
                        }
                    }
                }
            }

            val summary = "সফলভাবে রিস্টোর সম্পন্ন: $restoredBills টি বিল, $restoredCustomers জন গ্রাহক, $restoredPayments টি পেমেন্ট"
            Log.i(tag, "Restore finished: $summary (items=$restoredItems, services=$restoredServices)")
            BackupManager.updateSuccess(applicationContext, summary, userKey)
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Restore process failed for user $userKey", e)
            val errorSummary = "ব্যর্থ: রিস্টোর সম্পন্ন করা যায়নি (${e.message ?: "Error"})"
            BackupManager.updateStatus(applicationContext, errorSummary, userKey)
            Result.failure()
        }
    }
}
