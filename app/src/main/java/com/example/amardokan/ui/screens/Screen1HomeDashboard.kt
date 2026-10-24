package com.example.amardokan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanScreenType
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Screen1HomeDashboard(
    viewModel: AmarDokanViewModel,
    onOpenPdfTools: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val todayStats by viewModel.todayStats.collectAsState()
    val bills by viewModel.bills.collectAsState()

    val currentDateStr = remember {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AmarDokanColors.ContentBg)
            .testTag("screen_home_dashboard"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ---------------- 1. DARK NAVY BLUE HEADER ----------------
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(AmarDokanColors.NavyDarkHeader, AmarDokanColors.NavyDarkHeaderLight)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "আমার দোকান",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Xerox & Service Hisab • জেরক্স ও সার্ভিস হিসাব",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onOpenPdfTools != null) {
                                IconButton(
                                    onClick = onOpenPdfTools,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .testTag("btn_pdf_tools")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HomeRepairService,
                                        contentDescription = "PDF ও অন্যান্য টুলস",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFFE2E8F0),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentDateStr,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
                        )
                    )
                    .clickable {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://rofikulgayenrg.github.io/smartking-print/dashboard.html"))
                        context.startActivity(intent)
                    }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SMART KING - লাইভ প্রিন্ট ড্যাশবোর্ড", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("কাস্টমারের পাঠানো ফাইল দেখুন ও কন্ট্রোল করুন", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Icon(androidx.compose.material.icons.Icons.Rounded.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // ---------------- 2. TOP 4 METRIC CARDS ----------------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Today's Total Sales
                    StatSummaryCard(
                        title = "আজকের মোট বিক্রি",
                        amount = todayStats.totalSales,
                        badgeText = "${BanglaNumberUtil.toBangla(todayStats.billCount)} টি বিল",
                        badgeBg = Color(0xFFE0F2FE),
                        badgeTextColor = AmarDokanColors.PrimaryBlue,
                        borderColor = AmarDokanColors.PrimaryBlue.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f)
                    )

                    // Card 2: Today's Cash Collection (Green)
                    StatSummaryCard(
                        title = "আজকের নগদ জমা",
                        amount = todayStats.cashCollection,
                        badgeText = "নগদ ক্যাশ",
                        badgeBg = AmarDokanColors.CashGreenLight,
                        badgeTextColor = AmarDokanColors.CashGreen,
                        borderColor = AmarDokanColors.CashGreen.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 3: Today's Credit Sales (Amber)
                    StatSummaryCard(
                        title = "আজকের বাকি বিক্রি",
                        amount = todayStats.creditSales,
                        badgeText = "আজকের ধার",
                        badgeBg = AmarDokanColors.AmberOrangeLight,
                        badgeTextColor = AmarDokanColors.AmberOrange,
                        borderColor = AmarDokanColors.AmberOrange.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    )

                    // Card 4: Total Customer Outstanding (Red)
                    StatSummaryCard(
                        title = "মোট বকেয়া বাকি",
                        amount = todayStats.totalOutstanding,
                        badgeText = "বকেয়া ঋণ",
                        badgeBg = AmarDokanColors.DueRedLight,
                        badgeTextColor = AmarDokanColors.DueRed,
                        borderColor = AmarDokanColors.DueRed.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---------------- 3. MAIN ACTION GRID (6 CORE BUTTONS) ----------------
        item {
            Text(
                text = "প্রধান সেবাসমূহ ও হিসাব",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AmarDokanColors.TextPrimary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Grid 2x2 of other main features
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTileCard(
                        title = "কাস্টমার তালিকা",
                        subtitle = "গ্রাহক তথ্য ও ফোন",
                        icon = Icons.Default.People,
                        iconColor = Color(0xFF0284C7),
                        onClick = { viewModel.navigateTo(AmarDokanScreenType.CUSTOMER_LIST) },
                        modifier = Modifier.weight(1f)
                    )

                    ActionTileCard(
                        title = "বাকি খাতা",
                        subtitle = "খদ্দেরের লেনদেন লেজার",
                        icon = Icons.Default.MenuBook,
                        iconColor = AmarDokanColors.DueRed,
                        onClick = { viewModel.navigateTo(AmarDokanScreenType.CUSTOMER_LEDGER) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTileCard(
                        title = "দৈনিক রিপোর্ট",
                        subtitle = "প্রতিদিনের বিক্রি ও জমা",
                        icon = Icons.Default.Assessment,
                        iconColor = AmarDokanColors.CashGreen,
                        onClick = { viewModel.navigateTo(AmarDokanScreenType.DAILY_REPORT) },
                        modifier = Modifier.weight(1f)
                    )

                    ActionTileCard(
                        title = "পেমেন্ট নিন",
                        subtitle = "বাকি টাকা জমা এন্ট্রি",
                        icon = Icons.Default.Payments,
                        iconColor = Color(0xFF7C3AED),
                        onClick = { viewModel.openReceivePaymentForCustomer(null) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTileCard(
                        title = "আইটেম ও রেট সেটিংস",
                        subtitle = "জেরক্স ও প্রিন্ট রেট",
                        icon = Icons.Default.PriceChange,
                        iconColor = Color(0xFFD97706),
                        onClick = { viewModel.navigateTo(AmarDokanScreenType.ITEM_SETTINGS) },
                        modifier = Modifier.weight(1f)
                    )

                    ActionTileCard(
                        title = "মাসিক রিপোর্ট",
                        subtitle = "মাসের আয় ও বাকি হিসাব",
                        icon = Icons.Default.CalendarMonth,
                        iconColor = Color(0xFF0D9488),
                        onClick = { viewModel.navigateTo(AmarDokanScreenType.MONTHLY_REPORT) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---------------- 4. RECENT BILLS SECTION ----------------
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সাম্প্রতিক বিলসমূহ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmarDokanColors.TextPrimary
                )
                TextButton(onClick = { viewModel.navigateTo(AmarDokanScreenType.DAILY_REPORT) }) {
                    Text("সব দেখুন", color = AmarDokanColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (bills.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এখনো কোনো বিল তৈরি করা হয়নি",
                            color = AmarDokanColors.TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "উপরে 'নতুন বিল / হিসাব করুন' বাটনে ক্লিক করে বিল বানান",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            val recentBills = bills.take(5)
            items(recentBills.size) { index ->
                val bill = recentBills[index]
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bill.billNumber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AmarDokanColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreenLight else AmarDokanColors.DueRedLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (bill.paymentType == "CASH") "নগদ" else "বাকি",
                                        color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreen else AmarDokanColors.DueRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${bill.customerName} • ${bill.dateFormatted}",
                                fontSize = 12.sp,
                                color = AmarDokanColors.TextSecondary
                            )
                        }

                        Text(
                            text = BanglaNumberUtil.formatCurrencyBn(bill.grandTotal),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreen else AmarDokanColors.DueRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatSummaryCard(
    title: String,
    amount: Double,
    badgeText: String,
    badgeBg: Color,
    badgeTextColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = AmarDokanColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = BanglaNumberUtil.formatCurrencyBn(amount),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AmarDokanColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionTileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmarDokanColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = AmarDokanColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
