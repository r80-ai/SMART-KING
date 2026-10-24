package com.example.amardokan.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.data.Bill
import com.example.amardokan.data.BillItem
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen3DailyReport(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit,
    onOpenBillDetails: (Bill) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dailyStats by viewModel.dailyReportStats.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = সব বিল, 1 = নগদ পেমেন্ট জমা

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AmarDokanColors.ContentBg)
            .testTag("screen_daily_report"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ---------------- HEADER ----------------
        item {
            Surface(
                color = AmarDokanColors.NavyDarkHeader,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "দৈনিক রিপোর্ট (Daily Report)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---------------- DATE PICKER CONTROLS ----------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.stepDailyReportDate(-1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "পূর্ববর্তী দিন")
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = dailyStats.selectedDateMillis }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val picked = Calendar.getInstance().apply { set(year, month, day) }
                                    viewModel.setDailyReportDate(picked.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = BanglaNumberUtil.toBangla(dailyStats.selectedDateFormatted),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(onClick = { viewModel.stepDailyReportDate(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "পরবর্তী দিন")
                    }
                }
            }
        }

        // ---------------- SUMMARY METRICS ----------------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Total Sales & Cash Collection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportMetricCard(
                        title = "দিনের মোট বিক্রি",
                        amount = dailyStats.totalSales,
                        color = AmarDokanColors.PrimaryBlue,
                        bgColor = Color(0xFFEFF6FF),
                        modifier = Modifier.weight(1f)
                    )
                    ReportMetricCard(
                        title = "মোট নগদ জমা",
                        amount = dailyStats.totalCashReceived,
                        color = AmarDokanColors.CashGreen,
                        bgColor = AmarDokanColors.CashGreenLight,
                        note = "নগদ বিক্রি + জমা",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Cash Sales vs Credit Sales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportMetricCard(
                        title = "নগদ বিক্রি",
                        amount = dailyStats.cashSales,
                        color = Color(0xFF047857),
                        bgColor = Color(0xFFECFDF5),
                        modifier = Modifier.weight(1f)
                    )
                    ReportMetricCard(
                        title = "বাকি বিক্রি (ধার)",
                        amount = dailyStats.creditSales,
                        color = AmarDokanColors.DueRed,
                        bgColor = AmarDokanColors.DueRedLight,
                        note = "বকেয়া যোগ হয়েছে",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---------------- TAB TOGGLE (BILLS VS PAYMENTS) ----------------
        item {
            Spacer(modifier = Modifier.height(14.dp))
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.White,
                contentColor = AmarDokanColors.PrimaryBlue,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Text(
                            text = "বিলের তালিকা (${BanglaNumberUtil.toBangla(dailyStats.bills.size)})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Text(
                            text = "জমা পেমেন্ট (${BanglaNumberUtil.toBangla(dailyStats.payments.size)})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ---------------- TAB CONTENT ----------------
        if (activeTab == 0) {
            // BILLS TABLE
            if (dailyStats.bills.isEmpty()) {
                item {
                    EmptyReportNotice("এই তারিখে কোনো বিল করা হয়নি।")
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
                    ) {
                        Column {
                            // Table header
                            Surface(color = Color(0xFFF1F5F9)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("বিল নং ও খদ্দের", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("পেমেন্ট", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Text("মোট টাকা", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                                }
                            }

                            dailyStats.bills.forEachIndexed { idx, bill ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 0) AmarDokanColors.TableRowEven else AmarDokanColors.TableRowOdd)
                                        .clickable { onOpenBillDetails(bill) }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2.5f)) {
                                        Text(
                                            text = bill.customerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = AmarDokanColors.TextPrimary
                                        )
                                        Text(
                                            text = bill.billNumber,
                                            fontSize = 11.sp,
                                            color = AmarDokanColors.TextSecondary
                                        )
                                    }

                                    Surface(
                                        color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreenLight else AmarDokanColors.DueRedLight,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (bill.paymentType == "CASH") "নগদ" else "বাকি",
                                            color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreen else AmarDokanColors.DueRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = BanglaNumberUtil.formatCurrencyBn(bill.grandTotal),
                                        modifier = Modifier.weight(1.5f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        color = AmarDokanColors.TextPrimary
                                    )
                                }
                                HorizontalDivider(color = AmarDokanColors.BorderLight.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        } else {
            // PAYMENTS TABLE
            if (dailyStats.payments.isEmpty()) {
                item {
                    EmptyReportNotice("এই তারিখে কোনো বকেয়া পেমেন্ট জমা পড়েনি।")
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
                    ) {
                        Column {
                            Surface(color = Color(0xFFF1F5F9)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("কাস্টমারের নাম", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("মাধ্যম", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Text("জমা টাকা", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                                }
                            }

                            dailyStats.payments.forEachIndexed { idx, pay ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 0) AmarDokanColors.TableRowEven else AmarDokanColors.TableRowOdd)
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(
                                            text = pay.customerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = AmarDokanColors.TextPrimary
                                        )
                                        if (pay.notes.isNotBlank()) {
                                            Text(
                                                text = pay.notes,
                                                fontSize = 11.sp,
                                                color = AmarDokanColors.TextSecondary
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFFEDE9FE),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (pay.paymentMode == "UPI") "UPI" else "নগদ ক্যাশ",
                                            color = Color(0xFF6D28D9),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = "+ " + BanglaNumberUtil.formatCurrencyBn(pay.amount),
                                        modifier = Modifier.weight(1.5f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        color = AmarDokanColors.CashGreen
                                    )
                                }
                                HorizontalDivider(color = AmarDokanColors.BorderLight.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    amount: Double,
    color: Color,
    bgColor: Color,
    note: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = BanglaNumberUtil.formatCurrencyBn(amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            if (note != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = note, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun EmptyReportNotice(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, color = AmarDokanColors.TextSecondary, fontSize = 13.sp)
        }
    }
}
