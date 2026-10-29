package com.example.amardokan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen6ReceivePayment(
    viewModel: AmarDokanViewModel,
    preselectedCustomerId: Long? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    var selectedCustomerId by remember { mutableStateOf(preselectedCustomerId) }
    var amountText by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") } // "CASH" or "UPI"
    var notes by remember { mutableStateOf("") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCustomer = remember(customers, selectedCustomerId) {
        customers.find { it.id == selectedCustomerId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("টাকা জমা গ্রহণ (Payment)", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Selector
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "কাস্টমার নির্বাচন করুন *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AmarDokanColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "কাস্টমার নির্বাচন করুন",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                            textStyle = LocalTextStyle.current.copy(
                                color = if (selectedCustomer != null) Color.Black else Color(0xFF333333),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            colors = com.example.amardokan.ui.amarDokanTextFieldColors(containerColor = Color.White),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${cust.name} ${if (cust.phone.isNotBlank()) "(${cust.phone})" else ""}",
                                            color = Color.Black,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        selectedCustomerId = cust.id
                                        customerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Amount and Payment Mode
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "জমা টাকার পরিমাণ ও মোড",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("জমা টাকার পরিমাণ (₹) *", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                        placeholder = { Text("যেমন: 150", color = Color(0xFF444444), fontWeight = FontWeight.Medium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        colors = com.example.amardokan.ui.amarDokanTextFieldColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_receive_amount")
                    )

                    Text("পেমেন্ট মাধ্যম:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = paymentMode == "CASH",
                            onClick = { paymentMode = "CASH" },
                            label = {
                                Text(
                                    "🟢 নগদ (Cash)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (paymentMode == "CASH") Color(0xFF065F46) else Color(0xFF0F172A)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = Color(0xFF0F172A),
                                selectedContainerColor = AmarDokanColors.CashGreenLight,
                                selectedLabelColor = Color(0xFF065F46)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = paymentMode == "CASH",
                                borderColor = Color(0xFF64748B),
                                selectedBorderColor = AmarDokanColors.CashGreen,
                                borderWidth = 1.5.dp
                            )
                        )
                        FilterChip(
                            selected = paymentMode == "UPI",
                            onClick = { paymentMode = "UPI" },
                            label = {
                                Text(
                                    "💳 অনলাইন (UPI / QR)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (paymentMode == "UPI") Color(0xFF0D47A1) else Color(0xFF0F172A)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = Color(0xFF0F172A),
                                selectedContainerColor = AmarDokanColors.PrimaryContainer,
                                selectedLabelColor = Color(0xFF0D47A1)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = paymentMode == "UPI",
                                borderColor = Color(0xFF64748B),
                                selectedBorderColor = AmarDokanColors.PrimaryBlue,
                                borderWidth = 1.5.dp
                            )
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("মন্তব্য / নোট (ঐচ্ছিক)", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                        placeholder = { Text("যেমন: অনলাইন পেমেন্ট রেফারেন্স", color = Color(0xFF444444)) },
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        colors = com.example.amardokan.ui.amarDokanTextFieldColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val cust = selectedCustomer
                    if (cust == null) {
                        viewModel.showMessage("অনুগ্রহ করে কাস্টমার নির্বাচন করুন")
                        return@Button
                    }
                    if (amount <= 0.0) {
                        viewModel.showMessage("সঠিক টাকার পরিমাণ এন্ট্রি করুন")
                        return@Button
                    }
                    viewModel.recordPayment(
                        customerId = cust.id,
                        customerName = cust.name,
                        amount = amount,
                        paymentMode = paymentMode,
                        notes = notes,
                        onSuccess = { onBack() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_payment"),
                colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.CashGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("জমা নিশ্চিত করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
