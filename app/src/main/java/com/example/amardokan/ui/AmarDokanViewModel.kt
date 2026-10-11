package com.example.amardokan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    HOME_DASHBOARD,    // Screen 1
    NEW_BILL,          // Screen 2
    DAILY_REPORT,      // Screen 3
    CUSTOMER_LIST,     // Screen 4
    CUSTOMER_LEDGER,   // Screen 5
    RECEIVE_PAYMENT,   // Screen 6
    ITEM_SETTINGS,     // Screen 7
    MONTHLY_REPORT     // Screen 8
}

data class TodayStats(
    val totalSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val cashCollection: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val billCount: Int = 0
)

data class DailyReportStats(
    val selectedDateMillis: Long,
    val selectedDateFormatted: String,
    val totalSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val totalCashReceived: Double = 0.0, // cash sales + payments received on that day
    val todayOutstandingAdded: Double = 0.0,
    val bills: List<Bill> = emptyList(),
    val payments: List<Payment> = emptyList()
)

data class MonthDaySummary(
    val dayOfMonth: Int,
    val dateFormatted: String,
    val billCount: Int,
    val totalSales: Double,
    val cashSales: Double,
    val creditSales: Double,
    val cashCollected: Double
)

data class MonthlyReportStats(
    val year: Int,
    val month: Int, // 0-based: 0 = January, 9 = October
    val monthNameBn: String,
    val totalSales: Double = 0.0,
    val totalCashSales: Double = 0.0,
    val totalCreditSales: Double = 0.0,
    val totalPaymentsCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val totalCustomersCount: Int = 0,
    val totalBillsCount: Int = 0,
    val dailyBreakdowns: List<MonthDaySummary> = emptyList()
)

class AmarDokanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AmarDokanRepository(application)

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val services: StateFlow<List<ServiceItem>> = repository.allServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<Bill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(AmarDokanScreenType.HOME_DASHBOARD)
    val currentScreen: StateFlow<AmarDokanScreenType> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<AmarDokanScreenType>()

    private val _selectedCustomerIdForLedger = MutableStateFlow<Long?>(null)
    val selectedCustomerIdForLedger: StateFlow<Long?> = _selectedCustomerIdForLedger.asStateFlow()

    // Daily Report state
    private val _dailyReportDateMillis = MutableStateFlow(System.currentTimeMillis())
    val dailyReportDateMillis: StateFlow<Long> = _dailyReportDateMillis.asStateFlow()

    // Monthly Report state (Calendar.YEAR and Calendar.MONTH)
    private val _monthlyReportCalendar = MutableStateFlow(Calendar.getInstance())
    val monthlyReportCalendar: StateFlow<Calendar> = _monthlyReportCalendar.asStateFlow()

    // Notification message for snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Bill Template Language ("BN" or "EN")
    private val _selectedBillLanguage = MutableStateFlow("BN")
    val selectedBillLanguage: StateFlow<String> = _selectedBillLanguage.asStateFlow()

    fun setNewBillLanguage(lang: String) {
        _selectedBillLanguage.value = lang
    }

    init {
        viewModelScope.launch {
            repository.initDefaultServicesIfEmpty()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun navigateTo(screen: AmarDokanScreenType) {
        if (_currentScreen.value != screen) {
            _screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (_screenHistory.isNotEmpty()) {
            _currentScreen.value = _screenHistory.removeAt(_screenHistory.lastIndex)
            true
        } else if (_currentScreen.value != AmarDokanScreenType.HOME_DASHBOARD) {
            _currentScreen.value = AmarDokanScreenType.HOME_DASHBOARD
            true
        } else {
            false
        }
    }

    fun openLedgerForCustomer(customerId: Long) {
        _selectedCustomerIdForLedger.value = customerId
        navigateTo(AmarDokanScreenType.CUSTOMER_LEDGER)
    }

    fun openReceivePaymentForCustomer(customerId: Long? = null) {
        _selectedCustomerIdForLedger.value = customerId
        navigateTo(AmarDokanScreenType.RECEIVE_PAYMENT)
    }

    // ---------------- CUSTOMER BALANCE MAP ----------------
    val customerBalances: StateFlow<Map<Long, CustomerBalanceSummary>> = combine(
        customers, bills, payments
    ) { custList, billList, payList ->
        val map = mutableMapOf<Long, CustomerBalanceSummary>()
        for (c in custList) {
            val customerCreditBills = billList.filter { it.customerId == c.id && it.paymentType == "CREDIT" }
            val customerCashBills = billList.filter { it.customerId == c.id && it.paymentType == "CASH" }
            val customerPays = payList.filter { it.customerId == c.id }

            val totalWork = customerCreditBills.sumOf { it.grandTotal } + customerCashBills.sumOf { it.grandTotal }
            val totalCreditAmount = customerCreditBills.sumOf { it.grandTotal }
            val totalPaid = customerPays.sumOf { it.amount }

            val due = totalCreditAmount - totalPaid
            val finalDue = if (due < 0.0) 0.0 else due

            map[c.id] = CustomerBalanceSummary(
                customer = c,
                totalWorkAmount = totalWork,
                totalPaidAmount = totalPaid,
                outstandingBalance = finalDue
            )
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ---------------- TODAY'S STATS ----------------
    val todayStats: StateFlow<TodayStats> = combine(
        bills, payments, customerBalances
    ) { billList, payList, balanceMap ->
        val todayStart = getStartOfDayMillis(System.currentTimeMillis())
        val todayEnd = getEndOfDayMillis(System.currentTimeMillis())

        val todayBills = billList.filter { it.dateMillis in todayStart..todayEnd }
        val todayPays = payList.filter { it.dateMillis in todayStart..todayEnd }

        val cashSales = todayBills.filter { it.paymentType == "CASH" }.sumOf { it.grandTotal }
        val creditSales = todayBills.filter { it.paymentType == "CREDIT" }.sumOf { it.grandTotal }
        val totalSales = cashSales + creditSales

        val paymentsCollectedToday = todayPays.sumOf { it.amount }
        val totalCashCollection = cashSales + paymentsCollectedToday

        // Total outstanding across all customers
        val totalOutstanding = balanceMap.values.sumOf { it.outstandingBalance }

        TodayStats(
            totalSales = totalSales,
            cashSales = cashSales,
            creditSales = creditSales,
            cashCollection = totalCashCollection,
            totalOutstanding = totalOutstanding,
            billCount = todayBills.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayStats())

    // ---------------- DAILY REPORT STATS ----------------
    val dailyReportStats: StateFlow<DailyReportStats> = combine(
        _dailyReportDateMillis, bills, payments
    ) { targetMillis, billList, payList ->
        val dayStart = getStartOfDayMillis(targetMillis)
        val dayEnd = getEndOfDayMillis(targetMillis)

        val dayBills = billList.filter { it.dateMillis in dayStart..dayEnd }
        val dayPays = payList.filter { it.dateMillis in dayStart..dayEnd }

        val cashSales = dayBills.filter { it.paymentType == "CASH" }.sumOf { it.grandTotal }
        val creditSales = dayBills.filter { it.paymentType == "CREDIT" }.sumOf { it.grandTotal }
        val totalSales = cashSales + creditSales

        val dayPaymentsCollected = dayPays.sumOf { it.amount }
        val totalCashReceived = cashSales + dayPaymentsCollected

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        DailyReportStats(
            selectedDateMillis = targetMillis,
            selectedDateFormatted = sdf.format(Date(targetMillis)),
            totalSales = totalSales,
            cashSales = cashSales,
            creditSales = creditSales,
            totalCashReceived = totalCashReceived,
            todayOutstandingAdded = creditSales,
            bills = dayBills,
            payments = dayPays
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyReportStats(System.currentTimeMillis(), ""))

    fun setDailyReportDate(millis: Long) {
        _dailyReportDateMillis.value = millis
    }

    fun stepDailyReportDate(days: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = _dailyReportDateMillis.value
        cal.add(Calendar.DAY_OF_YEAR, days)
        _dailyReportDateMillis.value = cal.timeInMillis
    }

    // ---------------- MONTHLY REPORT STATS ----------------
    val monthlyReportStats: StateFlow<MonthlyReportStats> = combine(
        _monthlyReportCalendar, bills, payments, customerBalances
    ) { cal, billList, payList, balanceMap ->
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = startCal.timeInMillis

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val monthEnd = endCal.timeInMillis

        val monthBills = billList.filter { it.dateMillis in monthStart..monthEnd }
        val monthPays = payList.filter { it.dateMillis in monthStart..monthEnd }

        val totalCashSales = monthBills.filter { it.paymentType == "CASH" }.sumOf { it.grandTotal }
        val totalCreditSales = monthBills.filter { it.paymentType == "CREDIT" }.sumOf { it.grandTotal }
        val totalSales = totalCashSales + totalCreditSales

        val totalPaymentsCollected = monthPays.sumOf { it.amount }
        val totalOutstanding = balanceMap.values.sumOf { it.outstandingBalance }

        val distinctCustomerIds = (monthBills.mapNotNull { it.customerId } + monthPays.map { it.customerId }).toSet()

        // Daily breakdowns
        val breakdowns = mutableListOf<MonthDaySummary>()
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        for (d in 1..maxDays) {
            val dStartCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dEndCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val dBills = monthBills.filter { it.dateMillis in dStartCal.timeInMillis..dEndCal.timeInMillis }
            val dPays = monthPays.filter { it.dateMillis in dStartCal.timeInMillis..dEndCal.timeInMillis }

            val dCashSales = dBills.filter { it.paymentType == "CASH" }.sumOf { it.grandTotal }
            val dCreditSales = dBills.filter { it.paymentType == "CREDIT" }.sumOf { it.grandTotal }
            val dSales = dCashSales + dCreditSales
            val dCollected = dCashSales + dPays.sumOf { it.amount }

            if (dBills.isNotEmpty() || dPays.isNotEmpty()) {
                breakdowns.add(
                    MonthDaySummary(
                        dayOfMonth = d,
                        dateFormatted = daySdf.format(dStartCal.time),
                        billCount = dBills.size,
                        totalSales = dSales,
                        cashSales = dCashSales,
                        creditSales = dCreditSales,
                        cashCollected = dCollected
                    )
                )
            }
        }

        val banglaMonths = arrayOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val monthNameBn = if (month in 0..11) banglaMonths[month] else ""

        MonthlyReportStats(
            year = year,
            month = month,
            monthNameBn = monthNameBn,
            totalSales = totalSales,
            totalCashSales = totalCashSales,
            totalCreditSales = totalCreditSales,
            totalPaymentsCollected = totalPaymentsCollected,
            totalOutstanding = totalOutstanding,
            totalCustomersCount = distinctCustomerIds.size,
            totalBillsCount = monthBills.size,
            dailyBreakdowns = breakdowns
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyReportStats(2026, 9, "অক্টোবর"))

    fun stepMonthlyReport(monthDelta: Int) {
        val newCal = Calendar.getInstance().apply {
            timeInMillis = _monthlyReportCalendar.value.timeInMillis
            add(Calendar.MONTH, monthDelta)
        }
        _monthlyReportCalendar.value = newCal
    }

    fun setMonthlyReportMonthYear(year: Int, month: Int) {
        val newCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        _monthlyReportCalendar.value = newCal
    }

    // ---------------- ACTIONS ----------------
    fun createBill(
        dateMillis: Long,
        customerId: Long?,
        customerName: String,
        customerPhone: String,
        paymentType: String,
        subtotal: Double,
        discount: Double,
        grandTotal: Double,
        notes: String,
        items: List<BillItem>,
        language: String = _selectedBillLanguage.value,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateFormatted = sdf.format(Date(dateMillis))
            val billCount = bills.value.size + 1
            val prefix = if (language.uppercase() == "EN") "BILL" else "বিল"
            val billNumber = "$prefix-${billCount + 1000}"

            val billId = repository.createBill(
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
                notes = notes,
                items = items,
                language = language
            )
            val msg = if (language.uppercase() == "EN") {
                "✅ Bill saved successfully! ($billNumber)"
            } else {
                "✅ বিল সফলভাবে সেভ হয়েছে! ($billNumber)"
            }
            showMessage(msg)
            onSuccess(billId)
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            showMessage("বিল ডিলিট করা হয়েছে।")
        }
    }

    suspend fun getBillItems(billId: Long): List<BillItem> {
        return repository.getBillItems(billId)
    }

    fun receivePayment(
        customerId: Long,
        customerName: String,
        dateMillis: Long,
        amount: Double,
        paymentMode: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateFormatted = sdf.format(Date(dateMillis))
            val res = repository.receivePayment(
                customerId = customerId,
                customerName = customerName,
                dateMillis = dateMillis,
                dateFormatted = dateFormatted,
                amount = amount,
                paymentMode = paymentMode,
                notes = notes
            )
            if (res > 0) {
                showMessage("✅ ₹$amount জমা সেভ করা হয়েছে ($customerName)")
                onSuccess()
            } else {
                showMessage("❌ টাকার পরিমাণ সঠিক নয়")
            }
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            showMessage("পেমেন্ট এন্ট্রি মুছে ফেলা হয়েছে।")
        }
    }

    fun addCustomer(name: String, phone: String, address: String = "", notes: String = "", onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.addCustomer(name, phone, address, notes)
            showMessage("✅ নতুন কাস্টমার '$name' যুক্ত করা হয়েছে")
            onDone(id)
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

    fun addService(nameBn: String, nameEn: String, rate: Double, unitBn: String) {
        viewModelScope.launch {
            repository.addService(nameBn, nameEn, rate, unitBn)
            showMessage("✅ নতুন সার্ভিস রেট যোগ করা হয়েছে")
        }
    }

    fun updateService(service: ServiceItem) {
        viewModelScope.launch {
            repository.updateService(service)
            showMessage("সার্ভিস রেট আপডেট করা হয়েছে")
        }
    }

    fun deleteService(service: ServiceItem) {
        viewModelScope.launch {
            repository.deleteService(service)
            showMessage("সার্ভিস মুছে ফেলা হয়েছে")
        }
    }

    fun resetDefaultServices() {
        viewModelScope.launch {
            repository.resetDefaultServices()
            showMessage("ডিফল্ট জেরক্স ও প্রিন্ট রেট রিস্টোর করা হয়েছে")
        }
    }

    suspend fun getCustomerLedger(customerId: Long): List<LedgerEntry> {
        return repository.getCustomerLedger(customerId)
    }

    suspend fun exportBackupJson(): String {
        return repository.exportBackupJson()
    }

    suspend fun importBackupJson(json: String): Boolean {
        val ok = repository.importBackupJson(json)
        if (ok) {
            showMessage("✅ ব্যাকআপ সফলভাবে রিস্টোর হয়েছে!")
        } else {
            showMessage("❌ ব্যাকআপ ফাইল পার্স করতে সমস্যা হয়েছে")
        }
        return ok
    }

    private fun getStartOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getEndOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
