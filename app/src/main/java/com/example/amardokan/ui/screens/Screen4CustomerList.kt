package com.example.amardokan.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.data.Customer
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen4CustomerList(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit,
    onOpenAddCustomerDialog: () -> Unit,
    onEditCustomer: (Customer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val balances by viewModel.customerBalances.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(0) } // 0 = সব, 1 = বাকি আছে, 2 = পরিশোধিত

    val filteredCustomers = remember(customers, balances, searchQuery, filterMode) {
        customers.filter { cust ->
            val matchesSearch = cust.name.contains(searchQuery, ignoreCase = true) ||
                    cust.phone.contains(searchQuery, ignoreCase = true)
            val due = balances[cust.id]?.outstandingBalance ?: 0.0

            val matchesFilter = when (filterMode) {
                1 -> due > 0.0
                2 -> due <= 0.0
                else -> true
            }

            matchesSearch && matchesFilter
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
                    Text(
                        text = "কাস্টমার তালিকা (${BanglaNumberUtil.toBangla(customers.size)})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddCustomerDialog,
                containerColor = AmarDokanColors.PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "নতুন কাস্টমার")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmarDokanColors.ContentBg)
                .testTag("screen_customer_list"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ---------------- SEARCH BAR ----------------
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("নাম বা ফোন নম্বর দিয়ে খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "পরিষ্কার")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            // ---------------- FILTER CHIPS ----------------
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
                        label = { Text("সব (${BanglaNumberUtil.toBangla(customers.size)})") }
                    )
                    FilterChip(
                        selected = filterMode == 1,
                        onClick = { filterMode = 1 },
                        label = { Text("🔴 বাকি আছে") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmarDokanColors.DueRedLight,
                            selectedLabelColor = AmarDokanColors.DueRed
                        )
                    )
                    FilterChip(
                        selected = filterMode == 2,
                        onClick = { filterMode = 2 },
                        label = { Text("🟢 পরিশোধিত") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmarDokanColors.CashGreenLight,
                            selectedLabelColor = AmarDokanColors.CashGreen
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // ---------------- CUSTOMER ITEMS ----------------
            if (filteredCustomers.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PersonOff, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "এই নামে কোনো কাস্টমার পাওয়া যায়নি" else "কোনো কাস্টমার যুক্ত নেই",
                                color = AmarDokanColors.TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredCustomers, key = { it.id }) { cust ->
                    val summary = balances[cust.id]
                    val due = summary?.outstandingBalance ?: 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .clickable { viewModel.openLedgerForCustomer(cust.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(AmarDokanColors.PrimaryBlue.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cust.name.take(1),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmarDokanColors.PrimaryBlue
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = cust.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmarDokanColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (cust.phone.isNotBlank()) {
                                            Text(
                                                text = cust.phone,
                                                fontSize = 12.sp,
                                                color = AmarDokanColors.TextSecondary
                                            )
                                        }
                                    }
                                }

                                // Outstanding Balance Badge
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (due > 0.0) "বাকি টাকা" else "হিসাব",
                                        fontSize = 10.sp,
                                        color = AmarDokanColors.TextSecondary
                                    )
                                    Surface(
                                        color = if (due > 0.0) AmarDokanColors.DueRedLight else AmarDokanColors.CashGreenLight,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (due > 0.0) BanglaNumberUtil.formatCurrencyBn(due) else "পরিশোধিত ₹০",
                                            color = if (due > 0.0) AmarDokanColors.DueRed else AmarDokanColors.CashGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = AmarDokanColors.BorderLight.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Action Strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Ledger button
                                TextButton(
                                    onClick = { viewModel.openLedgerForCustomer(cust.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("বাকি খাতা দেখুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Right Action Buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Pay button
                                    FilledTonalButton(
                                        onClick = { viewModel.openReceivePaymentForCustomer(cust.id) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("পেমেন্ট নিন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Call button if phone exists
                                    if (cust.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "ফোন করুন", tint = AmarDokanColors.CashGreen, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // Edit button
                                    IconButton(
                                        onClick = { onEditCustomer(cust) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "এডিট", tint = AmarDokanColors.TextSecondary, modifier = Modifier.size(18.dp))
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
