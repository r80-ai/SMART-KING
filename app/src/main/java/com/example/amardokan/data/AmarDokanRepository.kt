package com.example.amardokan.data

import android.content.Context
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AmarDokanRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.amarDokanDao()

    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val allServices: Flow<List<ServiceItem>> = dao.getAllServices()
    val allBills: Flow<List<Bill>> = dao.getAllBills()
    val allPayments: Flow<List<Payment>> = dao.getAllPayments()

    suspend fun initDefaultServicesIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countServices() == 0) {
            val defaults = listOf(
                ServiceItem(nameBn = "সাদা-কালো জেরক্স / প্রিন্ট", nameEn = "B&W Xerox / Print", defaultRate = 3.0, unitBn = "পাতা", isDefault = true),
                ServiceItem(nameBn = "কালার প্রিন্ট", nameEn = "Color Print", defaultRate = 10.0, unitBn = "পাতা", isDefault = true),
                ServiceItem(nameBn = "ডকুমেন্ট স্ক্যানিং", nameEn = "Document Scanning", defaultRate = 5.0, unitBn = "পাতা", isDefault = true),
                ServiceItem(nameBn = "লেমিনেশন", nameEn = "Lamination", defaultRate = 20.0, unitBn = "টি", isDefault = true),
                ServiceItem(nameBn = "অনলাইন ফর্ম / আবেদন ফি", nameEn = "Online Form / Apply Fee", defaultRate = 50.0, unitBn = "সার্ভিস", isDefault = true)
            )
            dao.insertServices(defaults)
        }
    }

    // ---------------- CUSTOMERS ----------------
    suspend fun addCustomer(name: String, phone: String, address: String = "", notes: String = ""): Long = withContext(Dispatchers.IO) {
        dao.insertCustomer(Customer(name = name.trim(), phone = phone.trim(), address = address.trim(), notes = notes.trim()))
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.deleteCustomer(customer)
    }

    suspend fun getCustomerById(customerId: Long): Customer? = withContext(Dispatchers.IO) {
        dao.getCustomerById(customerId)
    }

    // ---------------- SERVICES ----------------
    suspend fun addService(nameBn: String, nameEn: String, rate: Double, unitBn: String): Long = withContext(Dispatchers.IO) {
        dao.insertService(ServiceItem(nameBn = nameBn.trim(), nameEn = nameEn.trim(), defaultRate = rate, unitBn = unitBn.trim(), isDefault = false))
    }

    suspend fun updateService(service: ServiceItem) = withContext(Dispatchers.IO) {
        dao.updateService(service)
    }

    suspend fun deleteService(service: ServiceItem) = withContext(Dispatchers.IO) {
        dao.deleteService(service)
    }

    suspend fun resetDefaultServices() = withContext(Dispatchers.IO) {
        val defaults = listOf(
            ServiceItem(nameBn = "সাদা-কালো জেরক্স / প্রিন্ট", nameEn = "B&W Xerox / Print", defaultRate = 3.0, unitBn = "পাতা", isDefault = true),
            ServiceItem(nameBn = "কালার প্রিন্ট", nameEn = "Color Print", defaultRate = 10.0, unitBn = "পাতা", isDefault = true),
            ServiceItem(nameBn = "ডকুমেন্ট স্ক্যানিং", nameEn = "Document Scanning", defaultRate = 5.0, unitBn = "পাতা", isDefault = true),
            ServiceItem(nameBn = "লেমিনেশন", nameEn = "Lamination", defaultRate = 20.0, unitBn = "টি", isDefault = true),
            ServiceItem(nameBn = "অনলাইন ফর্ম / আবেদন ফি", nameEn = "Online Form / Apply Fee", defaultRate = 50.0, unitBn = "সার্ভিস", isDefault = true)
        )
        defaults.forEach { dao.insertService(it) }
    }

    // ---------------- BILLS ----------------
    suspend fun createBill(
        billNumber: String,
        dateMillis: Long,
        dateFormatted: String,
        customerId: Long?,
        customerName: String,
        customerPhone: String,
        paymentType: String, // "CASH" or "CREDIT"
        subtotal: Double,
        discount: Double,
        grandTotal: Double,
        notes: String,
        items: List<BillItem>
    ): Long = withContext(Dispatchers.IO) {
        val cashReceived = if (paymentType == "CASH") grandTotal else 0.0
        val bill = Bill(
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
            notes = notes
        )
        dao.saveBillWithItems(bill, items)
    }

    suspend fun deleteBill(bill: Bill) = withContext(Dispatchers.IO) {
        dao.deleteBillCompletely(bill)
    }

    suspend fun getBillItems(billId: Long): List<BillItem> = withContext(Dispatchers.IO) {
        dao.getBillItemsByBillIdSync(billId)
    }

    // ---------------- PAYMENTS ----------------
    suspend fun receivePayment(
        customerId: Long,
        customerName: String,
        dateMillis: Long,
        dateFormatted: String,
        amount: Double,
        paymentMode: String, // "CASH" or "UPI"
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        if (amount <= 0.0) return@withContext -1L
        dao.insertPayment(
            Payment(
                customerId = customerId,
                customerName = customerName,
                dateMillis = dateMillis,
                dateFormatted = dateFormatted,
                amount = amount,
                paymentMode = paymentMode,
                notes = notes
            )
        )
    }

    suspend fun deletePayment(payment: Payment) = withContext(Dispatchers.IO) {
        dao.deletePayment(payment)
    }

    // ---------------- ACCOUNTING CALCULATIONS ----------------
    suspend fun getCustomerBalance(customerId: Long): Double = withContext(Dispatchers.IO) {
        val totalCreditBills = dao.getTotalCreditBillsForCustomer(customerId)
        val totalPayments = dao.getTotalPaymentsForCustomer(customerId)
        val balance = totalCreditBills - totalPayments
        if (balance < 0.0) 0.0 else balance
    }

    suspend fun getCustomerLedger(customerId: Long): List<LedgerEntry> = withContext(Dispatchers.IO) {
        val bills = dao.getBillsByCustomerId(customerId).first()
        val payments = dao.getPaymentsByCustomerId(customerId).first()

        val rawEntries = mutableListOf<LedgerEntry>()

        for (bill in bills) {
            val isCredit = bill.paymentType == "CREDIT"
            val desc = if (isCredit) "বিলের কাজ (${bill.billNumber})" else "নগদ বিল (${bill.billNumber})"
            val billAmount = if (isCredit) bill.grandTotal else bill.grandTotal
            val payAmount = if (!isCredit) bill.grandTotal else 0.0

            rawEntries.add(
                LedgerEntry(
                    id = bill.id,
                    dateMillis = bill.dateMillis,
                    dateFormatted = bill.dateFormatted,
                    type = LedgerEntryType.BILL,
                    description = desc,
                    billAmount = if (isCredit) bill.grandTotal else 0.0,
                    paymentAmount = 0.0,
                    paymentMode = bill.paymentType
                )
            )
        }

        for (pay in payments) {
            rawEntries.add(
                LedgerEntry(
                    id = pay.id,
                    dateMillis = pay.dateMillis,
                    dateFormatted = pay.dateFormatted,
                    type = LedgerEntryType.PAYMENT,
                    description = if (pay.notes.isNotBlank()) "জমা: ${pay.notes}" else "পেমেন্ট জমা (${if (pay.paymentMode == "UPI") "UPI" else "নগদ"})",
                    billAmount = 0.0,
                    paymentAmount = pay.amount,
                    paymentMode = pay.paymentMode
                )
            )
        }

        // Sort chronologically ascending to calculate running balance
        rawEntries.sortBy { it.dateMillis }

        var runningBalance = 0.0
        val ledgerWithBalance = mutableListOf<LedgerEntry>()
        for (entry in rawEntries) {
            if (entry.type == LedgerEntryType.BILL) {
                runningBalance += entry.billAmount
            } else {
                runningBalance -= entry.paymentAmount
            }
            val displayBalance = if (runningBalance < 0.0) 0.0 else runningBalance
            ledgerWithBalance.add(entry.copy(runningBalance = displayBalance))
        }

        // Return latest first for user view
        ledgerWithBalance.reversed()
    }

    // ---------------- BACKUP & RESTORE ----------------
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val customers = dao.getAllCustomers().first()
        val services = dao.getAllServices().first()
        val bills = dao.getAllBills().first()
        val payments = dao.getAllPayments().first()

        val custArray = JSONArray()
        for (c in customers) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("address", c.address)
            obj.put("notes", c.notes)
            obj.put("createdAt", c.createdAt)
            custArray.put(obj)
        }
        root.put("customers", custArray)

        val srvArray = JSONArray()
        for (s in services) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("nameBn", s.nameBn)
            obj.put("nameEn", s.nameEn)
            obj.put("defaultRate", s.defaultRate)
            obj.put("unitBn", s.unitBn)
            obj.put("isDefault", s.isDefault)
            srvArray.put(obj)
        }
        root.put("services", srvArray)

        val billArray = JSONArray()
        for (b in bills) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("billNumber", b.billNumber)
            obj.put("dateMillis", b.dateMillis)
            obj.put("dateFormatted", b.dateFormatted)
            obj.put("customerId", b.customerId ?: -1L)
            obj.put("customerName", b.customerName)
            obj.put("customerPhone", b.customerPhone)
            obj.put("paymentType", b.paymentType)
            obj.put("subtotal", b.subtotal)
            obj.put("discount", b.discount)
            obj.put("grandTotal", b.grandTotal)
            obj.put("cashReceived", b.cashReceived)
            obj.put("notes", b.notes)
            obj.put("createdAt", b.createdAt)

            val items = dao.getBillItemsByBillIdSync(b.id)
            val itemsArray = JSONArray()
            for (it in items) {
                val itObj = JSONObject()
                itObj.put("serviceName", it.serviceName)
                itObj.put("quantity", it.quantity)
                itObj.put("unitRate", it.unitRate)
                itObj.put("lineTotal", it.lineTotal)
                itemsArray.put(itObj)
            }
            obj.put("items", itemsArray)
            billArray.put(obj)
        }
        root.put("bills", billArray)

        val payArray = JSONArray()
        for (p in payments) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("customerId", p.customerId)
            obj.put("customerName", p.customerName)
            obj.put("dateMillis", p.dateMillis)
            obj.put("dateFormatted", p.dateFormatted)
            obj.put("amount", p.amount)
            obj.put("paymentMode", p.paymentMode)
            obj.put("notes", p.notes)
            obj.put("createdAt", p.createdAt)
            payArray.put(obj)
        }
        root.put("payments", payArray)

        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("version", 1)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("customers")) {
                val custArray = root.getJSONArray("customers")
                for (i in 0 until custArray.length()) {
                    val obj = custArray.getJSONObject(i)
                    dao.insertCustomer(
                        Customer(
                            name = obj.getString("name"),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (root.has("services")) {
                val srvArray = root.getJSONArray("services")
                for (i in 0 until srvArray.length()) {
                    val obj = srvArray.getJSONObject(i)
                    dao.insertService(
                        ServiceItem(
                            nameBn = obj.getString("nameBn"),
                            nameEn = obj.optString("nameEn", ""),
                            defaultRate = obj.getDouble("defaultRate"),
                            unitBn = obj.optString("unitBn", "পাতা"),
                            isDefault = obj.optBoolean("isDefault", false)
                        )
                    )
                }
            }
            if (root.has("bills")) {
                val billArray = root.getJSONArray("bills")
                for (i in 0 until billArray.length()) {
                    val obj = billArray.getJSONObject(i)
                    val rawCustId = obj.optLong("customerId", -1L)
                    val custId = if (rawCustId <= 0L) null else rawCustId

                    val bill = Bill(
                        billNumber = obj.getString("billNumber"),
                        dateMillis = obj.getLong("dateMillis"),
                        dateFormatted = obj.getString("dateFormatted"),
                        customerId = custId,
                        customerName = obj.getString("customerName"),
                        customerPhone = obj.optString("customerPhone", ""),
                        paymentType = obj.getString("paymentType"),
                        subtotal = obj.getDouble("subtotal"),
                        discount = obj.optDouble("discount", 0.0),
                        grandTotal = obj.getDouble("grandTotal"),
                        cashReceived = obj.optDouble("cashReceived", 0.0),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )

                    val items = mutableListOf<BillItem>()
                    if (obj.has("items")) {
                        val itemsArray = obj.getJSONArray("items")
                        for (j in 0 until itemsArray.length()) {
                            val itObj = itemsArray.getJSONObject(j)
                            items.add(
                                BillItem(
                                    billId = 0L,
                                    serviceName = itObj.getString("serviceName"),
                                    quantity = itObj.getDouble("quantity"),
                                    unitRate = itObj.getDouble("unitRate"),
                                    lineTotal = itObj.getDouble("lineTotal")
                                )
                            )
                        }
                    }
                    dao.saveBillWithItems(bill, items)
                }
            }
            if (root.has("payments")) {
                val payArray = root.getJSONArray("payments")
                for (i in 0 until payArray.length()) {
                    val obj = payArray.getJSONObject(i)
                    dao.insertPayment(
                        Payment(
                            customerId = obj.getLong("customerId"),
                            customerName = obj.getString("customerName"),
                            dateMillis = obj.getLong("dateMillis"),
                            dateFormatted = obj.getString("dateFormatted"),
                            amount = obj.getDouble("amount"),
                            paymentMode = obj.optString("paymentMode", "CASH"),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
