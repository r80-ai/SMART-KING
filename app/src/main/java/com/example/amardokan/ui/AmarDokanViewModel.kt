package com.example.amardokan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.example.amardokan.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AmarDokanScreenType {
    HOME_DASHBOARD,
    NEW_BILL,
    DAILY_REPORT,
    CUSTOMER_LIST,
    CUSTOMER_LEDGER,
    RECEIVE_PAYMENT,
    ITEM_SETTINGS,
    MONTHLY_REPORT
}

data class TodayStats(
    val totalSales: Double = 0.0,
    val cashCollection: Double = 0.0,
    val creditSales: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val billCount: Int = 0
)

class AmarDokanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AmarDokanRepository.getInstance(application)

    // Navigation state stack
    private val _screenStack = mutableListOf(AmarDokanScreenType.HOME_DASHBOARD)
    private val _currentScreen = MutableStateFlow(AmarDokanScreenType.HOME_DASHBOARD)
    val currentScreen: StateFlow<AmarDokanScreenType> = _currentScreen.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _selectedCustomerIdForLedger = MutableStateFlow<Long?>(null)
    val selectedCustomerIdForLedger: StateFlow<Long?> = _selectedCustomerIdForLedger.asStateFlow()

    private val _selectedBillLanguage = MutableStateFlow("BN")
    val selectedBillLanguage: StateFlow<String> = _selectedBillLanguage.asStateFlow()

    fun setNewBillLanguage(lang: String) {
        _selectedBillLanguage.value = lang
    }

    fun openReceivePaymentForCustomer(customerId: Long?) {
        _selectedCustomerIdForLedger.value = customerId
        navigateTo(AmarDokanScreenType.RECEIVE_PAYMENT)
    }

    // Core domain flows from repository
    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val services: StateFlow<List<ServiceItem>> = repository.allServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ServiceItem>> = services // Alias for code compatibility

    val bills: StateFlow<List<Bill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today Stats & Metrics
    val todayStats: StateFlow<TodayStats> = combine(bills, payments) { allBills, allPayments ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        val todayBills = allBills.filter { it.dateMillis >= startOfToday }
        val todayPayments = allPayments.filter { it.dateMillis >= startOfToday }

        val totalSales = todayBills.sumOf { it.grandTotal }
        val cashCollection = todayPayments.sumOf { it.amount }
        val creditSales = todayBills.filter { it.paymentType == "CREDIT" }.sumOf { it.grandTotal }

        // Total customer outstanding across all time
        val allCreditBillsTotal = allBills.filter { it.paymentType == "CREDIT" }.sumOf { it.grandTotal }
        val allPaymentsTotal = allPayments.sumOf { it.amount }
        val totalOutstanding = (allCreditBillsTotal - allPaymentsTotal).coerceAtLeast(0.0)

        TodayStats(
            totalSales = totalSales,
            cashCollection = cashCollection,
            creditSales = creditSales,
            totalOutstanding = totalOutstanding,
            billCount = todayBills.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayStats())

    val todaySales: StateFlow<Double> = todayStats.map { it.totalSales }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayCash: StateFlow<Double> = todayStats.map { it.cashCollection }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayDue: StateFlow<Double> = todayStats.map { it.creditSales }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPendingDue: StateFlow<Double> = todayStats.map { it.totalOutstanding }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayBillCount: StateFlow<Int> = todayStats.map { it.billCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            repository.initializeDefaultServicesIfEmpty()
        }
    }

    // Navigation
    fun navigateTo(screen: AmarDokanScreenType) {
        if (_screenStack.lastOrNull() != screen) {
            _screenStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_screenStack.size > 1) {
            _screenStack.removeAt(_screenStack.size - 1)
            _currentScreen.value = _screenStack.last()
            return true
        }
        return false
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun selectCustomerForLedger(customerId: Long?) {
        _selectedCustomerIdForLedger.value = customerId
        if (customerId != null) {
            navigateTo(AmarDokanScreenType.CUSTOMER_LEDGER)
        }
    }

    // Customer operations
    fun addCustomer(name: String, phone: String, address: String, notes: String) {
        if (name.isBlank()) {
            showMessage("কাস্টমারের নাম প্রদান করুন")
            return
        }
        viewModelScope.launch {
            val customer = Customer(
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                notes = notes.trim(),
                createdAt = System.currentTimeMillis()
            )
            repository.insertCustomer(customer)
            showMessage("কাস্টমার সফলভাবে যোগ করা হয়েছে")
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            showMessage("কাস্টমারের তথ্য আপডেট করা হয়েছে")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            showMessage("কাস্টমার মুছে ফেলা হয়েছে")
        }
    }

    // Bill operations
    fun createBill(
        dateMillis: Long,
        customerId: Long?,
        customerName: String,
        customerPhone: String,
        paymentType: String,
        subtotal: Double,
        discount: Double,
        grandTotal: Double,
        cashReceived: Double = if (paymentType == "CASH") grandTotal else 0.0,
        notes: String = "",
        items: List<BillItem>,
        language: String = "BN",
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val count = bills.value.size + 1
            val prefix = if (language.equals("EN", ignoreCase = true)) "BILL" else "বিল"
            val billNumber = "$prefix-${count + 1000}"

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateFormatted = sdf.format(Date(dateMillis))

            val bill = Bill(
                billNumber = billNumber,
                dateMillis = dateMillis,
                dateFormatted = dateFormatted,
                customerId = customerId,
                customerName = customerName.ifBlank { "নগদ কাস্টমার" },
                customerPhone = customerPhone,
                paymentType = paymentType,
                subtotal = subtotal,
                discount = discount,
                grandTotal = grandTotal,
                cashReceived = cashReceived,
                notes = notes,
                language = language,
                createdAt = System.currentTimeMillis()
            )

            repository.createBill(bill, items)
            showMessage("বিল তৈরি সম্পন্ন হয়েছে")
            onSuccess?.invoke()
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            showMessage("বিল মুছে ফেলা হয়েছে")
        }
    }

    fun deleteBill(billId: Long) {
        viewModelScope.launch {
            repository.deleteBillById(billId)
            showMessage("বিল মুছে ফেলা হয়েছে")
        }
    }

    // Payment operations
    fun recordPayment(
        customerId: Long,
        customerName: String,
        amount: Double,
        paymentMode: String = "CASH",
        notes: String = "",
        billId: Long? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        if (amount <= 0.0) {
            showMessage("সঠিক পরিমাণ এন্ট্রি করুন")
            return
        }
        viewModelScope.launch {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateFormatted = sdf.format(Date())

            val payment = Payment(
                customerId = customerId,
                customerName = customerName,
                billId = billId,
                dateMillis = System.currentTimeMillis(),
                dateFormatted = dateFormatted,
                amount = amount,
                paymentMode = paymentMode,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            repository.insertPayment(payment)
            showMessage("পেমেন্ট সফলভাবে জমা হয়েছে")
            onSuccess?.invoke()
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            showMessage("পেমেন্ট এন্ট্রি মুছে ফেলা হয়েছে")
        }
    }

    // Service Item operations
    fun addServiceItem(nameBn: String, nameEn: String, defaultRate: Double, unitBn: String) {
        if (nameBn.isBlank()) {
            showMessage("আইটেমের নাম প্রদান করুন")
            return
        }
        viewModelScope.launch {
            val item = ServiceItem(
                nameBn = nameBn.trim(),
                nameEn = nameEn.trim(),
                defaultRate = defaultRate,
                unitBn = unitBn.trim()
            )
            repository.insertService(item)
            showMessage("নতুন আইটেম যোগ করা হয়েছে")
        }
    }

    fun updateServiceItem(item: ServiceItem) {
        viewModelScope.launch {
            repository.updateService(item)
            showMessage("আইটেম আপডেট করা হয়েছে")
        }
    }

    fun deleteServiceItem(item: ServiceItem) {
        viewModelScope.launch {
            repository.deleteService(item)
            showMessage("আইটেম মুছে ফেলা হয়েছে")
        }
    }

    // Ledger flow for a given customer
    fun getCustomerLedger(customerId: Long): Flow<List<LedgerEntry>> {
        return repository.getCustomerLedger(customerId)
    }

    fun getCustomerBalance(customer: Customer): Flow<CustomerBalanceSummary> {
        return repository.getCustomerBalanceSummary(customer)
    }

    suspend fun getBillItems(billId: Long): List<BillItem> = withContext(Dispatchers.IO) {
        repository.getBillItemsSync(billId)
    }

    fun getBillItemsFlow(billId: Long): Flow<List<BillItem>> {
        return repository.getBillItems(billId)
    }

    suspend fun exportDatabaseBackup(): DatabaseBackup {
        return repository.exportBackup()
    }

    suspend fun restoreDatabase(backup: DatabaseBackup) {
        repository.restoreBackup(backup)
        showMessage("ব্যাকআপ সফলভাবে রিস্টোর করা হয়েছে")
    }

    suspend fun triggerLocalBackup(context: android.content.Context) {
        val result = AmarDokanBackupManager.createLocalBackup(context)
        if (result.isSuccess) {
            showMessage("✅ ব্যাকআপ সংরক্ষিত: ${result.getOrNull()?.name}")
        }
    }

    private val moshi = Moshi.Builder().build()
    private val backupAdapter = moshi.adapter(DatabaseBackup::class.java)

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val backup = exportDatabaseBackup()
        backupAdapter.toJson(backup)
    }


    suspend fun importBackupJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = backupAdapter.fromJson(json) ?: return@withContext false
            restoreDatabase(backup)
            true
        } catch (e: Exception) {
            showMessage("ব্যাকআপ ফাইল পড়তে সমস্যা হয়েছে: ${e.message}")
            false
        }
    }
}

