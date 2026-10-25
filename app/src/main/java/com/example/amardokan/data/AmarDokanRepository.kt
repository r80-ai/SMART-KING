package com.example.amardokan.data

import android.content.Context
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AmarDokanRepository(private val dao: AmarDokanDao) {

    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val allServices: Flow<List<ServiceItem>> = dao.getAllServices()
    val allBills: Flow<List<Bill>> = dao.getAllBills()
    val allPayments: Flow<List<Payment>> = dao.getAllPayments()

    suspend fun initializeDefaultServicesIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countServices() == 0) {
            val defaults = listOf(
                ServiceItem(nameBn = "জেরক্স", nameEn = "Xerox", defaultRate = 2.0, unitBn = "পাতা", isDefault = true),
                ServiceItem(nameBn = "প্রিন্টআউট (B/W)", nameEn = "Print B/W", defaultRate = 5.0, unitBn = "পাতা", isDefault = true),
                ServiceItem(nameBn = "রঙিন প্রিন্ট (Color)", nameEn = "Color Print", defaultRate = 10.0, unitBn = "পাতা", isDefault = true),
                ServiceItem(nameBn = "ছবি প্রিন্ট (Photo)", nameEn = "Photo Print", defaultRate = 30.0, unitBn = "কপি", isDefault = true),
                ServiceItem(nameBn = "অনলাইন ফর্ম ফিলআপ", nameEn = "Online Form", defaultRate = 50.0, unitBn = "টি", isDefault = true),
                ServiceItem(nameBn = "ল্যামিনেশন", nameEn = "Lamination", defaultRate = 20.0, unitBn = "টি", isDefault = true),
                ServiceItem(nameBn = "স্ক্যান ও ইমেইল", nameEn = "Scan & Email", defaultRate = 15.0, unitBn = "ফাইল", isDefault = true)
            )
            dao.insertServices(defaults)
        }
    }

    suspend fun insertCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        dao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.deleteCustomer(customer)
    }

    suspend fun insertService(service: ServiceItem): Long = withContext(Dispatchers.IO) {
        dao.insertService(service)
    }

    suspend fun updateService(service: ServiceItem) = withContext(Dispatchers.IO) {
        dao.updateService(service)
    }

    suspend fun deleteService(service: ServiceItem) = withContext(Dispatchers.IO) {
        dao.deleteService(service)
    }

    suspend fun createBill(bill: Bill, items: List<BillItem>): Long = withContext(Dispatchers.IO) {
        val billId = dao.insertBill(bill)
        val itemsWithBillId = items.map { it.copy(billId = billId) }
        dao.insertAllBillItems(itemsWithBillId)

        // If it's a cash bill with cashReceived, or if cashReceived > 0, we can also record payment or rely on bill status
        if (bill.paymentType == "CASH" && bill.cashReceived > 0) {
            val payment = Payment(
                customerId = bill.customerId ?: 0L,
                customerName = bill.customerName,
                billId = billId,
                dateMillis = bill.dateMillis,
                dateFormatted = bill.dateFormatted,
                amount = bill.cashReceived,
                paymentMode = "CASH",
                notes = "বিল #${bill.billNumber} নগদ পেমেন্ট"
            )
            dao.insertPayment(payment)
        }
        billId
    }

    suspend fun deleteBill(bill: Bill) = withContext(Dispatchers.IO) {
        dao.deleteBillItemsByBillId(bill.id)
        dao.deleteBill(bill)
    }

    suspend fun deleteBillById(billId: Long) = withContext(Dispatchers.IO) {
        dao.deleteBillItemsByBillId(billId)
        val all = dao.getAllBillsSync()
        all.find { it.id == billId }?.let { dao.deleteBill(it) }
    }

    fun getBillItems(billId: Long): Flow<List<BillItem>> = dao.getBillItemsByBillId(billId)

    fun getBillItemsSync(billId: Long): List<BillItem> = dao.getBillItemsByBillIdSync(billId)

    suspend fun insertPayment(payment: Payment): Long = withContext(Dispatchers.IO) {
        dao.insertPayment(payment)
    }

    suspend fun deletePayment(payment: Payment) = withContext(Dispatchers.IO) {
        dao.deletePayment(payment)
    }

    fun getBillsByCustomer(customerId: Long): Flow<List<Bill>> = dao.getBillsByCustomerId(customerId)

    fun getPaymentsByCustomer(customerId: Long): Flow<List<Payment>> = dao.getPaymentsByCustomerId(customerId)

    fun getCustomerLedger(customerId: Long): Flow<List<LedgerEntry>> {
        return combine(
            dao.getBillsByCustomerId(customerId),
            dao.getPaymentsByCustomerId(customerId)
        ) { bills, payments ->
            val entries = mutableListOf<LedgerEntry>()

            bills.forEach { b ->
                entries.add(
                    LedgerEntry(
                        id = b.id,
                        dateMillis = b.dateMillis,
                        dateFormatted = b.dateFormatted,
                        type = LedgerEntryType.BILL,
                        description = "বিল #${b.billNumber} (${if (b.paymentType == "CASH") "নগদ" else "বাকি"})",
                        billAmount = b.grandTotal,
                        paymentAmount = 0.0,
                        runningBalance = 0.0,
                        rawBillId = b.id
                    )
                )
            }

            payments.forEach { p ->
                entries.add(
                    LedgerEntry(
                        id = p.id,
                        dateMillis = p.dateMillis,
                        dateFormatted = p.dateFormatted,
                        type = LedgerEntryType.PAYMENT,
                        description = if (p.notes.isNotBlank()) p.notes else "জমা (${p.paymentMode})",
                        billAmount = 0.0,
                        paymentAmount = p.amount,
                        runningBalance = 0.0,
                        paymentMode = p.paymentMode
                    )
                )
            }

            // Sort chronologically
            val sorted = entries.sortedBy { it.dateMillis }
            var running = 0.0
            val computed = mutableListOf<LedgerEntry>()
            for (entry in sorted) {
                if (entry.type == LedgerEntryType.BILL) {
                    running += entry.billAmount
                } else {
                    running -= entry.paymentAmount
                }
                computed.add(entry.copy(runningBalance = running))
            }
            computed.reversed() // Show newest first
        }
    }

    fun getCustomerBalanceSummary(customer: Customer): Flow<CustomerBalanceSummary> {
        return combine(
            dao.getBillsByCustomerId(customer.id),
            dao.getPaymentsByCustomerId(customer.id)
        ) { bills, payments ->
            val totalBills = bills.sumOf { it.grandTotal }
            val totalPaid = payments.sumOf { it.amount }
            val outstanding = (totalBills - totalPaid).coerceAtLeast(0.0)
            CustomerBalanceSummary(
                customer = customer,
                totalWorkAmount = totalBills,
                totalPaidAmount = totalPaid,
                outstandingBalance = outstanding
            )
        }
    }

    suspend fun exportBackup(): DatabaseBackup = withContext(Dispatchers.IO) {
        DatabaseBackup(
            customers = dao.getAllCustomersSync(),
            services = dao.getAllServicesSync(),
            bills = dao.getAllBillsSync(),
            billItems = dao.getAllBillItemsSync(),
            payments = dao.getAllPaymentsSync()
        )
    }

    suspend fun restoreBackup(backup: DatabaseBackup) = withContext(Dispatchers.IO) {
        backup.customers.forEach { dao.insertCustomer(it) }
        if (backup.services.isNotEmpty()) {
            dao.insertServices(backup.services)
        }
        backup.bills.forEach { dao.insertBill(it) }
        if (backup.billItems.isNotEmpty()) {
            dao.insertAllBillItems(backup.billItems)
        }
        backup.payments.forEach { dao.insertPayment(it) }
    }

    companion object {
        @Volatile
        private var INSTANCE: AmarDokanRepository? = null

        fun getInstance(context: Context): AmarDokanRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context.applicationContext)
                AmarDokanRepository(db.amarDokanDao()).also { INSTANCE = it }
            }
        }
    }
}
