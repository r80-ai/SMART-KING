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
import com.example.amardokan.data.Customer
import com.example.amardokan.data.LedgerEntry
import com.example.amardokan.data.LedgerEntryType
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen5CustomerLedger(
    viewModel: AmarDokanViewModel,
    customerId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val customers by viewModel.customers.collectAsState()
    val balances by viewModel.customerBalances.collectAsState()

    val currentCustomer = remember(customers, customerId) {
        customers.find { it.id == customerId }
    }

    val customerSummary = remember(balances, customerId) {
        balances[customerId]
    }

    var ledgerEntries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }
    var filterMode by remember { mutableStateOf(0) } // 0 = সব, 1 = শুধু কাজ/বিল, 2 = শুধু জমা/পেমেন্ট

    LaunchedEffect(customerId) {
        if (customerId != null) {
            ledgerEntries = viewModel.getCustomerLedger(customerId)
        }
    }

    val filteredEntries = remember(ledgerEntries, filterMode) {
        when (filterMode) {
            1 -> ledgerEntries.filter { it.type == LedgerEntryType.BILL }
            2 -> ledgerEntries.filter { it.type == LedgerEntryType.PAYMENT }
            else -> ledgerEntries
        }
    }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "কাস্টমার লেজার (বাকি খাতা)",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentCustomer?.name ?: "গ্রাহক তথ্য",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }

                    // Share Statement Icon
                    IconButton(
                        onClick = {
                            if (currentCustomer != null) {
                                val sb = StringBuilder()
                                sb.append("📋 *আমার দোকান - খতিয়ান বাকি খাতা*\n")
                                sb.append("গ্রাহক: ${currentCustomer.name}\n")
                                if (currentCustomer.phone.isNotBlank()) sb.append("ফোন: ${currentCustomer.phone}\n")
                                sb.append("----------------------------\n")
                                sb.append("মোট কাজ: ₹${customerSummary?.totalWorkAmount ?: 0.0}\n")
                                sb.append("মোট জমা: ₹${customerSummary?.totalPaidAmount ?: 0.0}\n")
                                sb.append("*বর্তমান বাকি বকেয়া: ₹${customerSummary?.outstandingBalance ?: 0.0}*\n")
                                sb.append("----------------------------\n")
                                sb.append("ধন্যবাদ! টেলিকম সাইবার ক্যাফে ও জেরক্স সেন্টার।")

                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "খাতা শেয়ার করুন"))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "শেয়ার", tint = Color.White)
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.openReceivePaymentForCustomer(customerId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.CashGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পেমেন্ট জমা নিন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.navigateTo(com.example.amardokan.ui.AmarDokanScreenType.NEW_BILL)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp), tint = AmarDokanColors.PrimaryBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("নতুন বিল করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmarDokanColors.PrimaryBlue)
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
                .testTag("screen_customer_ledger"),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // ---------------- 1. CUSTOMER INFO & BALANCE CARD ----------------
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentCustomer?.name ?: "গ্রাহকের নাম",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmarDokanColors.TextPrimary
                                )
                                if (!currentCustomer?.phone.isNullOrBlank()) {
                                    Text(
                                        text = "মোবাইল: ${currentCustomer?.phone}",
                                        fontSize = 13.sp,
                                        color = AmarDokanColors.TextSecondary
                                    )
                                }
                            }

                            // Big Outstanding Badge
                            Surface(
                                color = AmarDokanColors.DueRedLight,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.DueRed.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("বর্তমান বাকি", fontSize = 10.sp, color = AmarDokanColors.DueRed, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = BanglaNumberUtil.formatCurrencyBn(customerSummary?.outstandingBalance ?: 0.0),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AmarDokanColors.DueRed
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = AmarDokanColors.BorderLight)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("মোট কাজের বিল:", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                                Text(
                                    text = BanglaNumberUtil.formatCurrencyBn(customerSummary?.totalWorkAmount ?: 0.0),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmarDokanColors.TextPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("মোট জমা পেমেন্ট:", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                                Text(
                                    text = BanglaNumberUtil.formatCurrencyBn(customerSummary?.totalPaidAmount ?: 0.0),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmarDokanColors.CashGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("অবশিষ্ট দেনা:", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                                Text(
                                    text = BanglaNumberUtil.formatCurrencyBn(customerSummary?.outstandingBalance ?: 0.0),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmarDokanColors.DueRed
                                )
                            }
                        }
                    }
                }
            }

            // ---------------- 2. FILTER TABS ----------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterMode == 0,
                        onClick = { filterMode = 0 },
                        label = { Text("সব লেনদেন (${BanglaNumberUtil.toBangla(ledgerEntries.size)})") }
                    )
                    FilterChip(
                        selected = filterMode == 1,
                        onClick = { filterMode = 1 },
                        label = { Text("শুধু কাজ / বিল") }
                    )
                    FilterChip(
                        selected = filterMode == 2,
                        onClick = { filterMode = 2 },
                        label = { Text("শুধু জমা") }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // ---------------- 3. LEDGER TIMELINE TABLE ----------------
            if (filteredEntries.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "এই গ্রাহকের কোনো লেনদেন পাওয়া যায়নি",
                                color = AmarDokanColors.TextSecondary,
                                fontSize = 13.sp
                            )
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
                            // Table Header
                            Surface(color = Color(0xFFF1F5F9)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("তারিখ ও বিবরণ", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("কাজ (+)", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, color = AmarDokanColors.DueRed)
                                    Text("জমা (-)", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, color = AmarDokanColors.CashGreen)
                                    Text("অবশিষ্ট", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                                }
                            }

                            filteredEntries.forEachIndexed { idx, entry ->
                                val isBill = entry.type == LedgerEntryType.BILL
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 0) AmarDokanColors.TableRowEven else AmarDokanColors.TableRowOdd)
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2.5f)) {
                                        Text(
                                            text = entry.description,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = AmarDokanColors.TextPrimary
                                        )
                                        Text(
                                            text = entry.dateFormatted,
                                            fontSize = 10.sp,
                                            color = AmarDokanColors.TextSecondary
                                        )
                                    }

                                    // Bill Amount (Debit)
                                    Text(
                                        text = if (entry.billAmount > 0.0) "₹${BanglaNumberUtil.toBangla(entry.billAmount.toInt().toString())}" else "-",
                                        modifier = Modifier.weight(1.2f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        color = if (entry.billAmount > 0.0) AmarDokanColors.DueRed else Color.LightGray
                                    )

                                    // Payment Amount (Credit)
                                    Text(
                                        text = if (entry.paymentAmount > 0.0) "₹${BanglaNumberUtil.toBangla(entry.paymentAmount.toInt().toString())}" else "-",
                                        modifier = Modifier.weight(1.2f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        color = if (entry.paymentAmount > 0.0) AmarDokanColors.CashGreen else Color.LightGray
                                    )

                                    // Running Balance
                                    Text(
                                        text = "₹${BanglaNumberUtil.toBangla(entry.runningBalance.toInt().toString())}",
                                        modifier = Modifier.weight(1.3f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.End,
                                        color = if (entry.runningBalance > 0.0) AmarDokanColors.DueRed else AmarDokanColors.CashGreen
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
