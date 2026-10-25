package com.example.amardokan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.data.LedgerEntry
import com.example.amardokan.data.LedgerEntryType
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanScreenType
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen5CustomerLedger(
    viewModel: AmarDokanViewModel,
    customerId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    val customer = remember(customers, customerId) {
        customers.find { it.id == customerId }
    }

    val ledgerEntries by remember(customerId) {
        if (customerId != null) viewModel.getCustomerLedger(customerId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val totalWork = remember(ledgerEntries) {
        ledgerEntries.sumOf { it.billAmount }
    }
    val totalPaid = remember(ledgerEntries) {
        ledgerEntries.sumOf { it.paymentAmount }
    }
    val outstanding = remember(totalWork, totalPaid) {
        (totalWork - totalPaid).coerceAtLeast(0.0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = customer?.name?.let { "$it এর খাতা" } ?: "কাস্টমার খাতা",
                        fontWeight = FontWeight.Bold
                    )
                },
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
        },
        floatingActionButton = {
            if (customer != null) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(AmarDokanScreenType.RECEIVE_PAYMENT) },
                    containerColor = AmarDokanColors.CashGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_receive_payment")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AddCard, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("টাকা জমা নিন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmarDokanColors.ContentBg)
        ) {
            // Customer Balance Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = customer?.name ?: "অজ্ঞাত",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = AmarDokanColors.NavyDarkHeader
                    )
                    if (!customer?.phone.isNullOrBlank()) {
                        Text(
                            text = "ফোন: ${customer?.phone}",
                            fontSize = 13.sp,
                            color = AmarDokanColors.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট কাজ", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                            Text(
                                text = BanglaNumberUtil.formatCurrency(totalWork),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmarDokanColors.PrimaryBlue
                            )
                        }
                        Column {
                            Text("মোট জমা", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                            Text(
                                text = BanglaNumberUtil.formatCurrency(totalPaid),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmarDokanColors.CashGreen
                            )
                        }
                        Column {
                            Text("অবশিষ্ট বকেয়া", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                            Text(
                                text = BanglaNumberUtil.formatCurrency(outstanding),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (outstanding > 0) AmarDokanColors.DueRed else AmarDokanColors.CashGreen
                            )
                        }
                    }
                }
            }

            Text(
                text = "লেনদেন হিস্ট্রি",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = AmarDokanColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (ledgerEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("এখনও কোনো লেনদেন রেকর্ড নেই", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ledgerEntries, key = { "${it.type}_${it.id}" }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.description,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AmarDokanColors.TextPrimary
                                    )
                                    Text(
                                        text = entry.dateFormatted,
                                        fontSize = 12.sp,
                                        color = AmarDokanColors.TextSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    if (entry.type == LedgerEntryType.BILL) {
                                        Text(
                                            text = "+ ${BanglaNumberUtil.formatCurrency(entry.billAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = AmarDokanColors.DueRed
                                        )
                                    } else {
                                        Text(
                                            text = "- ${BanglaNumberUtil.formatCurrency(entry.paymentAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = AmarDokanColors.CashGreen
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
}
