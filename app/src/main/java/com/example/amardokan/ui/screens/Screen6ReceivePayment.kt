package com.example.amardokan.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.data.Customer
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil
import com.example.amardokan.ui.memo.UpiPaymentQrDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen6ReceivePayment(
    viewModel: AmarDokanViewModel,
    preselectedCustomerId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val balances by viewModel.customerBalances.collectAsState()

    var selectedCustomer by remember(customers, preselectedCustomerId) {
        mutableStateOf(customers.find { it.id == preselectedCustomerId } ?: customers.firstOrNull())
    }

    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var amountInput by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") } // "CASH" or "UPI"
    var notesInput by remember { mutableStateOf("") }
    var customerMenuExpanded by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }

    val dateFormatted = remember(selectedDateMillis) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(selectedDateMillis))
    }

    val currentDue = remember(selectedCustomer, balances) {
        if (selectedCustomer != null) {
            balances[selectedCustomer!!.id]?.outstandingBalance ?: 0.0
        } else 0.0
    }

    val payingAmount = amountInput.toDoubleOrNull() ?: 0.0
    val remainingDue = (currentDue - payingAmount).coerceAtLeast(0.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AmarDokanColors.ContentBg)
            .testTag("screen_receive_payment"),
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
                        text = "পেমেন্ট জমা নিন (Receive Payment)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---------------- CUSTOMER SELECTION & CURRENT DUE ----------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "কাস্টমার বাছুন:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AmarDokanColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { customerMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCustomer?.let { "${it.name} ${if (it.phone.isNotBlank()) "(${it.phone})" else ""}" } ?: "কাস্টমার নির্বাচন করুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AmarDokanColors.TextPrimary
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = customerMenuExpanded,
                            onDismissRequest = { customerMenuExpanded = false }
                        ) {
                            customers.forEach { cust ->
                                val due = balances[cust.id]?.outstandingBalance ?: 0.0
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(cust.name, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = if (due > 0.0) "বাকি: ₹${due.toInt()}" else "বাকি নেই",
                                                color = if (due > 0.0) AmarDokanColors.DueRed else AmarDokanColors.CashGreen,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomer = cust
                                        customerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Current Due Banner
                    Surface(
                        color = AmarDokanColors.DueRedLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.DueRed.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "এই গ্রাহকের বর্তমান বাকি:",
                                color = AmarDokanColors.DueRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = BanglaNumberUtil.formatCurrencyBn(currentDue),
                                color = AmarDokanColors.DueRed,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // ---------------- PAYMENT DETAILS INPUT ----------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Payment Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "জমার তারিখ:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = AmarDokanColors.TextPrimary
                        )

                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val picked = Calendar.getInstance().apply { set(year, month, day) }
                                        selectedDateMillis = picked.timeInMillis
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
                            Text(BanglaNumberUtil.toBangla(dateFormatted), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Amount Input
                    Text(
                        text = "জমা টাকার পরিমাণ (₹):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AmarDokanColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("যেমন: ৫০০") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Amount Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentDue > 0.0) {
                            SuggestionChip(
                                onClick = { amountInput = currentDue.toInt().toString() },
                                label = { Text("পুরো বাকি (₹${currentDue.toInt()})", fontWeight = FontWeight.Bold) }
                            )
                            if (currentDue >= 100.0) {
                                SuggestionChip(
                                    onClick = { amountInput = (currentDue / 2.0).toInt().toString() },
                                    label = { Text("অর্ধেক (₹${(currentDue / 2.0).toInt()})") }
                                )
                            }
                        }
                        listOf(50, 100, 200, 500, 1000).forEach { preset ->
                            SuggestionChip(
                                onClick = { amountInput = preset.toString() },
                                label = { Text("₹$preset") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Payment Mode: Cash vs UPI
                    Text(
                        text = "পেমেন্টের মাধ্যম:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AmarDokanColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = paymentMode == "CASH",
                            onClick = { paymentMode = "CASH" },
                            label = { Text("💵 নগদ ক্যাশ", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmarDokanColors.CashGreenLight,
                                selectedLabelColor = AmarDokanColors.CashGreen
                            )
                        )

                        FilterChip(
                            selected = paymentMode == "UPI",
                            onClick = { paymentMode = "UPI" },
                            label = { Text("📱 UPI (অনলাইন)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEDE9FE),
                                selectedLabelColor = Color(0xFF6D28D9)
                            )
                        )
                    }

                    // UPI QR Code Generator Button
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showUpiQrDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.NavyDarkHeader)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = AmarDokanColors.NavyDarkHeader, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ইউপিআই কিউআর কোড স্ক্যান করান (QR Code)", fontSize = 12.sp, color = AmarDokanColors.NavyDarkHeader, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Note / Memo Field
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("মন্তব্য / নোট (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: Google Pay থেকে পাঠিয়েছে") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ---------------- BALANCE CALCULATION PREVIEW ----------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "হিসাব পূর্বরূপ (Calculation Preview):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AmarDokanColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("পূর্বের বাকি:", fontSize = 13.sp, color = AmarDokanColors.TextPrimary)
                        Text(BanglaNumberUtil.formatCurrencyBn(currentDue), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("এখন জমা দিচ্ছেন:", fontSize = 13.sp, color = AmarDokanColors.CashGreen)
                        Text("- " + BanglaNumberUtil.formatCurrencyBn(payingAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmarDokanColors.CashGreen)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AmarDokanColors.BorderLight)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("নতুন অবশিষ্ট বাকি থাকবে:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmarDokanColors.TextPrimary)
                        Text(
                            text = BanglaNumberUtil.formatCurrencyBn(remainingDue),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (remainingDue > 0.0) AmarDokanColors.DueRed else AmarDokanColors.CashGreen
                        )
                    }
                }
            }
        }

        // ---------------- SAVE PAYMENT BUTTON ----------------
        item {
            Button(
                onClick = {
                    if (selectedCustomer == null) {
                        viewModel.showMessage("❌ কাস্টমার নির্বাচন করুন")
                        return@Button
                    }
                    if (payingAmount <= 0.0) {
                        viewModel.showMessage("❌ জমার পরিমাণ সঠিক নয়")
                        return@Button
                    }

                    viewModel.receivePayment(
                        customerId = selectedCustomer!!.id,
                        customerName = selectedCustomer!!.name,
                        dateMillis = selectedDateMillis,
                        amount = payingAmount,
                        paymentMode = paymentMode,
                        notes = notesInput
                    ) {
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .testTag("btn_save_payment"),
                colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.CashGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "পেমেন্ট জমা সেভ করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showUpiQrDialog && selectedCustomer != null) {
        val payAmount = if (payingAmount > 0.0) payingAmount else currentDue
        UpiPaymentQrDialog(
            billNumber = "বকেয়া পেমেন্ট",
            customerName = selectedCustomer!!.name,
            payableAmount = if (payAmount > 0.0) payAmount else 100.0,
            isCreditBill = true,
            onDismiss = { showUpiQrDialog = false }
        )
    }
}
