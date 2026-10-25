package com.example.amardokan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen8MonthlyReport(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bills by viewModel.bills.collectAsState()
    val payments by viewModel.payments.collectAsState()

    // Calculate current month's start and end timestamps
    val currentMonthName = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val cal = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val startOfMonth = remember { cal.timeInMillis }

    val monthBills = remember(bills, startOfMonth) {
        bills.filter { it.dateMillis >= startOfMonth }
    }
    val monthPayments = remember(payments, startOfMonth) {
        payments.filter { it.dateMillis >= startOfMonth }
    }

    val monthTotalSales = remember(monthBills) { monthBills.sumOf { it.grandTotal } }
    val monthCash = remember(monthPayments) { monthPayments.sumOf { it.amount } }
    val monthCredit = remember(monthBills) {
        monthBills.filter { it.paymentType == "CREDIT" }.sumOf { it.grandTotal }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("মাসিক রিপোর্ট ও লাভ-ক্ষতি", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmarDokanColors.NavyDarkHeader,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmarDokanColors.ContentBg)
        ) {
            // Month Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = AmarDokanColors.PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "চলতি মাস: $currentMonthName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmarDokanColors.NavyDarkHeader
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট বিক্রি", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                            Text(
                                text = BanglaNumberUtil.formatCurrency(monthTotalSales),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmarDokanColors.PrimaryBlue
                            )
                        }
                        Column {
                            Text("নগদ জমা", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                            Text(
                                text = BanglaNumberUtil.formatCurrency(monthCash),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmarDokanColors.CashGreen
                            )
                        }
                        Column {
                            Text("বাকি বিক্রি", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                            Text(
                                text = BanglaNumberUtil.formatCurrency(monthCredit),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmarDokanColors.AmberOrange
                            )
                        }
                    }
                }
            }

            Text(
                text = "চলতি মাসের বিলসমূহ (${BanglaNumberUtil.toBangla(monthBills.size)} টি)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = AmarDokanColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(monthBills, key = { it.id }) { bill ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monthly_bill_item_${bill.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bill.customerName.ifBlank { "নগদ কাস্টমার" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AmarDokanColors.TextPrimary
                                )
                                Text(
                                    text = "বিল #${bill.billNumber} • ${bill.dateFormatted}",
                                    fontSize = 12.sp,
                                    color = AmarDokanColors.TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = BanglaNumberUtil.formatCurrency(bill.grandTotal),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreen else AmarDokanColors.DueRed
                                )
                                Surface(
                                    color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreenLight else AmarDokanColors.DueRedLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (bill.paymentType == "CASH") "নগদ" else "বাকি",
                                        color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreen else AmarDokanColors.DueRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
