package com.example.amardokan.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen8MonthlyReport(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val monthlyStats by viewModel.monthlyReportStats.collectAsState()

    Scaffold(
        topBar = {
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
                        text = "মাসিক হিসাব রিপোর্ট (Monthly Report)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Share report button
                    IconButton(
                        onClick = {
                            val sb = StringBuilder()
                            sb.append("📊 *আমার দোকান - মাসিক রিপোর্ট*\n")
                            sb.append("মাস: ${monthlyStats.monthNameBn} ${BanglaNumberUtil.toBangla(monthlyStats.year.toString())}\n")
                            sb.append("----------------------------\n")
                            sb.append("মোট বিক্রি: ₹${monthlyStats.totalSales}\n")
                            sb.append("নগদ বিক্রি: ₹${monthlyStats.totalCashSales}\n")
                            sb.append("বাকি বিক্রি: ₹${monthlyStats.totalCreditSales}\n")
                            sb.append("সংগৃহীত পেমেন্ট জমা: ₹${monthlyStats.totalPaymentsCollected}\n")
                            sb.append("মোট বিল সংখ্যা: ${monthlyStats.totalBillsCount} টি\n")
                            sb.append("মোট খদ্দের সংখ্যা: ${monthlyStats.totalCustomersCount} জন\n")
                            sb.append("----------------------------\n")
                            sb.append("টেলিকম সাইবার ক্যাফে ও জেরক্স সেন্টার")

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, sb.toString())
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "মাসিক রিপোর্ট শেয়ার করুন"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "শেয়ার", tint = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmarDokanColors.ContentBg)
                .testTag("screen_monthly_report"),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ---------------- MONTH PICKER STEPPER ----------------
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
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.stepMonthlyReport(-1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "পূর্ববর্তী মাস")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AmarDokanColors.PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${monthlyStats.monthNameBn} ${BanglaNumberUtil.toBangla(monthlyStats.year.toString())}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmarDokanColors.TextPrimary
                            )
                        }

                        IconButton(onClick = { viewModel.stepMonthlyReport(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "পরবর্তী মাস")
                        }
                    }
                }
            }

            // ---------------- TOP METRICS GRID ----------------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MonthlyMetricCard(
                            title = "মাসের মোট বিক্রি",
                            amount = monthlyStats.totalSales,
                            color = AmarDokanColors.PrimaryBlue,
                            bgColor = Color(0xFFEFF6FF),
                            subtext = "${BanglaNumberUtil.toBangla(monthlyStats.totalBillsCount)} টি বিল",
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyMetricCard(
                            title = "মোট নগদ জমা কালেকশন",
                            amount = monthlyStats.totalCashSales + monthlyStats.totalPaymentsCollected,
                            color = AmarDokanColors.CashGreen,
                            bgColor = AmarDokanColors.CashGreenLight,
                            subtext = "নগদ বিক্রি + জমা",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MonthlyMetricCard(
                            title = "নগদ বিক্রি",
                            amount = monthlyStats.totalCashSales,
                            color = Color(0xFF047857),
                            bgColor = Color(0xFFECFDF5),
                            subtext = "ক্যাশ সেল",
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyMetricCard(
                            title = "বাকি বিক্রি (ধার)",
                            amount = monthlyStats.totalCreditSales,
                            color = AmarDokanColors.DueRed,
                            bgColor = AmarDokanColors.DueRedLight,
                            subtext = "বকেয়া সেল",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MonthlyMetricCard(
                            title = "সংগৃহীত বাকি পেমেন্ট",
                            amount = monthlyStats.totalPaymentsCollected,
                            color = Color(0xFF7C3AED),
                            bgColor = Color(0xFFEDE9FE),
                            subtext = "বাকি আদায়",
                            modifier = Modifier.weight(1f)
                        )
                        MonthlyMetricCard(
                            title = "খদ্দের সংখ্যা",
                            amount = monthlyStats.totalCustomersCount.toDouble(),
                            isCurrency = false,
                            color = Color(0xFF0284C7),
                            bgColor = Color(0xFFE0F2FE),
                            subtext = "লেনদেনকারী",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ---------------- DATE-WISE BREAKDOWN TABLE ----------------
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "তারিখভিত্তিক বিক্রির তালিকা (Date-wise Table)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AmarDokanColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            if (monthlyStats.dailyBreakdowns.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("এই মাসে এখনো কোনো বিক্রি বা জমা লেনদেন হয়নি", color = AmarDokanColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
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
                                    Text("তারিখ", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("বিল", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                    Text("বিক্রি (₹)", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                                    Text("নগদ (₹)", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, color = AmarDokanColors.CashGreen)
                                    Text("বাকি (₹)", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, color = AmarDokanColors.DueRed)
                                }
                            }

                            monthlyStats.dailyBreakdowns.forEachIndexed { idx, day ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 0) AmarDokanColors.TableRowEven else AmarDokanColors.TableRowOdd)
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = BanglaNumberUtil.toBangla(day.dateFormatted),
                                        modifier = Modifier.weight(1.5f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = BanglaNumberUtil.toBangla(day.billCount.toString()),
                                        modifier = Modifier.weight(0.7f),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = BanglaNumberUtil.toBangla(day.totalSales.toInt().toString()),
                                        modifier = Modifier.weight(1.3f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberUtil.toBangla(day.cashSales.toInt().toString()),
                                        modifier = Modifier.weight(1.2f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        color = AmarDokanColors.CashGreen
                                    )
                                    Text(
                                        text = BanglaNumberUtil.toBangla(day.creditSales.toInt().toString()),
                                        modifier = Modifier.weight(1.2f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        color = if (day.creditSales > 0.0) AmarDokanColors.DueRed else Color.LightGray
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
private fun MonthlyMetricCard(
    title: String,
    amount: Double,
    color: Color,
    bgColor: Color,
    subtext: String,
    isCurrency: Boolean = true,
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
                text = if (isCurrency) BanglaNumberUtil.formatCurrencyBn(amount) else "${BanglaNumberUtil.toBangla(amount.toInt().toString())} জন",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
        }
    }
}
