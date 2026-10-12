package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.util.GoogleApiService
import com.example.amardokan.data.Customer
import com.example.amardokan.data.ServiceItem
import com.example.amardokan.data.Bill
import com.example.amardokan.data.BillItem
import com.example.amardokan.data.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RestoreWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "RestoreWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Starting Restore process...")
            val apiService = GoogleApiService(applicationContext)
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.amarDokanDao()

            val rootFolderId = apiService.findOrCreateFolder("Telecom Cyber Cafe")
            BackupManager.saveRootFolderId(applicationContext, rootFolderId)

            val spreadsheetId = apiService.findOrCreateSpreadsheet("Billing_Data", rootFolderId)
            BackupManager.saveSpreadsheetId(applicationContext, spreadsheetId)

            // Restore Customers
            val customersData = apiService.readSheetRows(spreadsheetId, "ad_customers")
            if (customersData.size > 1) { // Skip header
                for (i in 1 until customersData.size) {
                    val row = customersData[i]
                    if (row.size >= 6) {
                        dao.insertCustomer(Customer(
                            id = row[0].toString().toLong(),
                            name = row[1].toString(),
                            phone = row[2].toString(),
                            address = row[3].toString(),
                            notes = row[4].toString(),
                            createdAt = row[5].toString().toLong()
                        ))
                    }
                }
            }

            // Restore Services
            val servicesData = apiService.readSheetRows(spreadsheetId, "ad_services")
            if (servicesData.size > 1) {
                for (i in 1 until servicesData.size) {
                    val row = servicesData[i]
                    if (row.size >= 6) {
                        dao.insertService(ServiceItem(
                            id = row[0].toString().toLong(),
                            nameBn = row[1].toString(),
                            nameEn = row[2].toString(),
                            defaultRate = row[3].toString().toDouble(),
                            unitBn = row[4].toString(),
                            isDefault = row[5].toString() == "YES"
                        ))
                    }
                }
            }

            // Restore Bills
            val billsData = apiService.readSheetRows(spreadsheetId, "ad_bills")
            if (billsData.size > 1) {
                for (i in 1 until billsData.size) {
                    val row = billsData[i]
                    if (row.size >= 15) {
                        dao.insertBill(Bill(
                            id = row[0].toString().toLong(),
                            billNumber = row[1].toString(),
                            dateFormatted = row[2].toString(),
                            dateMillis = row[3].toString().toLong(),
                            customerId = row[4].toString().toLongOrNull(),
                            customerName = row[5].toString(),
                            customerPhone = row[6].toString(),
                            paymentType = row[7].toString(),
                            subtotal = row[8].toString().toDouble(),
                            discount = row[9].toString().toDouble(),
                            grandTotal = row[10].toString().toDouble(),
                            cashReceived = row[11].toString().toDouble(),
                            notes = row[12].toString(),
                            language = row[13].toString(),
                            createdAt = row[14].toString().toLong()
                        ))
                    }
                }
            }
            // ... (similar for BillItems and Payments)

            Log.d(tag, "Restore completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Restore failed", e)
            Result.failure()
        }
    }
}
