package com.example.amardokan.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.data.BillItem
import com.example.amardokan.data.Customer
import com.example.amardokan.data.ServiceItem
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil
import com.example.amardokan.ui.memo.AmarDokanCashMemoDialog
import com.example.amardokan.ui.memo.AmarDokanCashMemoGenerator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DraftBillItem(
    val serviceName: String,
    val quantity: Double = 1.0,
    val unitRate: Double = 0.0,
    val unitName: String = "পাতা"
) {
    val lineTotal: Double get() = quantity * unitRate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen2NewBill(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit,
    onOpenAddCustomerDialog: () -> Unit,
    onPrintReceipt: (billNumber: String, customerName: String, dateStr: String, items: List<DraftBillItem>, subtotal: Double, discount: Double, grandTotal: Double, paymentType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val services by viewModel.services.collectAsState()

    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) } // null = Cash Counter
    var isCashCustomer by remember { mutableStateOf(true) }
    var paymentType by remember { mutableStateOf("CASH") } // "CASH" or "CREDIT"
    var discountInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    // Draft items list
    val draftItems = remember { mutableStateListOf<DraftBillItem>() }

    // Add custom item dialog state
    var showAddItemDialog by remember { mutableStateOf(false) }
    var customItemName by remember { mutableStateOf("") }
    var customItemQty by remember { mutableStateOf("1") }
    var customItemRate by remember { mutableStateOf("") }
    var customItemUnit by remember { mutableStateOf("পাতা") }

    // Customer selection dropdown expanded
    var customerMenuExpanded by remember { mutableStateOf(false) }

    // Cash memo preview dialog state
    var activeCashMemoData by remember { mutableStateOf<AmarDokanCashMemoGenerator.MemoData?>(null) }
    var shouldExitAfterMemoDismiss by remember { mutableStateOf(false) }

    // Calculations
    val subtotal = draftItems.sumOf { it.lineTotal }
    val discount = discountInput.toDoubleOrNull() ?: 0.0
    val grandTotal = (subtotal - discount).coerceAtLeast(0.0)

    val dateFormatted = remember(selectedDateMillis) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(selectedDateMillis))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AmarDokanColors.ContentBg)
            .testTag("screen_new_bill"),
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
                        text = "নতুন বিল তৈরি করুন (হিসাব)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---------------- DATE & CUSTOMER SELECTION ----------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Date picker row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বিলের তারিখ:",
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
                                        val picked = Calendar.getInstance().apply {
                                            set(year, month, day)
                                        }
                                        selectedDateMillis = picked.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = BanglaNumberUtil.toBangla(dateFormatted), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = AmarDokanColors.BorderLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Customer Selection
                    Text(
                        text = "কাস্টমার নির্বাচন:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AmarDokanColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
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
                                        text = if (isCashCustomer) "নগদ কাউন্টার খদ্দের" else (selectedCustomer?.name ?: "কাস্টমার বাছুন"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmarDokanColors.TextPrimary
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = customerMenuExpanded,
                                onDismissRequest = { customerMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("নগদ কাউন্টার খদ্দের (Cash Counter)") },
                                    onClick = {
                                        isCashCustomer = true
                                        selectedCustomer = null
                                        paymentType = "CASH"
                                        customerMenuExpanded = false
                                    }
                                )
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text("${cust.name} ${if (cust.phone.isNotBlank()) "(${cust.phone})" else ""}") },
                                        onClick = {
                                            isCashCustomer = false
                                            selectedCustomer = cust
                                            customerMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onOpenAddCustomerDialog,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmarDokanColors.PrimaryBlue.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "নতুন কাস্টমার",
                                tint = AmarDokanColors.PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Payment Type: Cash vs Credit (Due)
                    Text(
                        text = "পেমেন্টের ধরণ:",
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
                            selected = paymentType == "CASH",
                            onClick = { paymentType = "CASH" },
                            label = { Text("🟢 নগদ (Cash)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmarDokanColors.CashGreenLight,
                                selectedLabelColor = AmarDokanColors.CashGreen
                            )
                        )

                        FilterChip(
                            selected = paymentType == "CREDIT",
                            onClick = {
                                if (isCashCustomer) {
                                    // If credit selected, prompt to pick a real customer
                                    if (customers.isNotEmpty()) {
                                        isCashCustomer = false
                                        selectedCustomer = customers.first()
                                        paymentType = "CREDIT"
                                    } else {
                                        onOpenAddCustomerDialog()
                                    }
                                } else {
                                    paymentType = "CREDIT"
                                }
                            },
                            label = { Text("🔴 বাকি (Credit)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmarDokanColors.DueRedLight,
                                selectedLabelColor = AmarDokanColors.DueRed
                            )
                        )
                    }
                }
            }
        }

        // ---------------- QUICK ADD SERVICE CHIPS ----------------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "দ্রুত আইটেম যোগ করুন (ডিফল্ট রেট):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmarDokanColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    services.forEach { srv ->
                        SuggestionChip(
                            onClick = {
                                // Add or increment item
                                val existingIndex = draftItems.indexOfFirst { it.serviceName == srv.nameBn }
                                if (existingIndex >= 0) {
                                    val cur = draftItems[existingIndex]
                                    draftItems[existingIndex] = cur.copy(quantity = cur.quantity + 1)
                                } else {
                                    draftItems.add(
                                        DraftBillItem(
                                            serviceName = srv.nameBn,
                                            quantity = 1.0,
                                            unitRate = srv.defaultRate,
                                            unitName = srv.unitBn
                                        )
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = "+ ${srv.nameBn} (₹${srv.defaultRate.toInt()})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color.White
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = AmarDokanColors.PrimaryBlue.copy(alpha = 0.5f)
                            )
                        )
                    }

                    // Add Custom Item Chip
                    SuggestionChip(
                        onClick = {
                            customItemName = ""
                            customItemQty = "1"
                            customItemRate = ""
                            showAddItemDialog = true
                        },
                        label = {
                            Text(
                                text = "+ অন্য আইটেম",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmarDokanColors.PrimaryBlue
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = AmarDokanColors.PrimaryBlue.copy(alpha = 0.08f)
                        )
                    )
                }
            }
        }

        // ---------------- BILL ITEMS TABLE ----------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
            ) {
                Column {
                    // Table Header
                    Surface(color = Color(0xFFF1F5F9)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("আইটেমের নাম", modifier = Modifier.weight(2.2f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmarDokanColors.TextPrimary)
                            Text("পরিমাণ", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmarDokanColors.TextPrimary, textAlign = TextAlign.Center)
                            Text("রেট", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmarDokanColors.TextPrimary, textAlign = TextAlign.End)
                            Text("মোট", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmarDokanColors.TextPrimary, textAlign = TextAlign.End)
                            Spacer(modifier = Modifier.width(32.dp)) // delete icon spacer
                        }
                    }

                    if (draftItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "কোনো আইটেম যোগ করা হয়নি।\nউপরের তালিকা থেকে সার্ভিস যোগ করুন।",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        draftItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) AmarDokanColors.TableRowEven else AmarDokanColors.TableRowOdd)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Service name
                                Text(
                                    text = item.serviceName,
                                    modifier = Modifier.weight(2.2f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AmarDokanColors.TextPrimary
                                )

                                // Quantity Controls (- / +)
                                Row(
                                    modifier = Modifier.weight(1.5f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (item.quantity > 1.0) {
                                                draftItems[index] = item.copy(quantity = item.quantity - 1.0)
                                            } else {
                                                draftItems.removeAt(index)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "কম", modifier = Modifier.size(16.dp))
                                    }

                                    Text(
                                        text = BanglaNumberUtil.toBangla(item.quantity.toInt().toString()),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            draftItems[index] = item.copy(quantity = item.quantity + 1.0)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "বেশি", modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Unit Rate
                                Text(
                                    text = "₹" + BanglaNumberUtil.toBangla(item.unitRate.toInt().toString()),
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.End,
                                    color = AmarDokanColors.TextSecondary
                                )

                                // Line Total
                                Text(
                                    text = "₹" + BanglaNumberUtil.toBangla(item.lineTotal.toInt().toString()),
                                    modifier = Modifier.weight(1.2f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    color = AmarDokanColors.TextPrimary
                                )

                                // Delete button
                                IconButton(
                                    onClick = { draftItems.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "মুছুন",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = AmarDokanColors.BorderLight.copy(alpha = 0.5f))
                        }
                    }

                    // ---------------- TOTAL & DISCOUNT SUMMARY ----------------
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(14.dp)
                    ) {
                        // Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("মোট সাবটোটাল:", color = AmarDokanColors.TextSecondary, fontSize = 14.sp)
                            Text(BanglaNumberUtil.formatCurrencyBn(subtotal), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Discount Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ছাড় / ডিসকাউন্ট (₹):", color = AmarDokanColors.TextSecondary, fontSize = 14.sp)
                            OutlinedTextField(
                                value = discountInput,
                                onValueChange = { discountInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("০") },
                                singleLine = true,
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(48.dp),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 14.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = AmarDokanColors.BorderLight)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Grand Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "সর্বমোট প্রদেয় বিল:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmarDokanColors.TextPrimary
                            )
                            Text(
                                text = BanglaNumberUtil.formatCurrencyBn(grandTotal),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (paymentType == "CASH") AmarDokanColors.CashGreen else AmarDokanColors.DueRed
                            )
                        }
                    }
                }
            }
        }

        // ---------------- ACTION BUTTONS: SAVE & PRINT ----------------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save Bill Button
                Button(
                    onClick = {
                        if (draftItems.isEmpty()) {
                            viewModel.showMessage("❌ বিলে অন্তত একটি আইটেম যোগ করুন")
                            return@Button
                        }
                        if (paymentType == "CREDIT" && isCashCustomer) {
                            viewModel.showMessage("❌ বাকি বিলের জন্য একজন নির্দিষ্ট কাস্টমার নির্বাচন করুন")
                            return@Button
                        }

                        val custId = if (isCashCustomer) null else selectedCustomer?.id
                        val custName = if (isCashCustomer) "নগদ কাউন্টার খদ্দের" else (selectedCustomer?.name ?: "নগদ খদ্দের")
                        val custPhone = selectedCustomer?.phone ?: ""

                        val billItems = draftItems.map {
                            BillItem(
                                billId = 0L,
                                serviceName = it.serviceName,
                                quantity = it.quantity,
                                unitRate = it.unitRate,
                                lineTotal = it.lineTotal
                            )
                        }

                        val newBillCount = viewModel.bills.value.size + 1
                        val billNumber = "বিল-${newBillCount + 1000}"

                        viewModel.createBill(
                            dateMillis = selectedDateMillis,
                            customerId = custId,
                            customerName = custName,
                            customerPhone = custPhone,
                            paymentType = paymentType,
                            subtotal = subtotal,
                            discount = discount,
                            grandTotal = grandTotal,
                            notes = notesInput,
                            items = billItems
                        ) {
                            activeCashMemoData = AmarDokanCashMemoGenerator.MemoData.fromDraft(
                                billNumber = billNumber,
                                customerName = custName,
                                customerPhone = custPhone,
                                dateStr = dateFormatted,
                                items = draftItems.toList(),
                                subtotal = subtotal,
                                discount = discount,
                                grandTotal = grandTotal,
                                paymentType = paymentType
                            )
                            shouldExitAfterMemoDismiss = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_bill"),
                    colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "বিল সেভ করুন (Save Bill)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Print Receipt Button (Cash Memo preview)
                OutlinedButton(
                    onClick = {
                        if (draftItems.isEmpty()) {
                            viewModel.showMessage("❌ বিলে কোনো আইটেম নেই")
                            return@OutlinedButton
                        }
                        val custName = if (isCashCustomer) "নগদ কাউন্টার খদ্দের" else (selectedCustomer?.name ?: "নগদ খদ্দের")
                        val custPhone = selectedCustomer?.phone ?: ""
                        activeCashMemoData = AmarDokanCashMemoGenerator.MemoData.fromDraft(
                            billNumber = "নতুন বিল",
                            customerName = custName,
                            customerPhone = custPhone,
                            dateStr = dateFormatted,
                            items = draftItems.toList(),
                            subtotal = subtotal,
                            discount = discount,
                            grandTotal = grandTotal,
                            paymentType = paymentType
                        )
                        shouldExitAfterMemoDismiss = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_print_bill"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = AmarDokanColors.PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ক্যাশ মেমো দেখুন ও প্রিন্ট করুন",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmarDokanColors.PrimaryBlue
                    )
                }
            }
        }
    }

    // ---------------- CUSTOM ITEM DIALOG ----------------
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("অন্য আইটেম যোগ করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customItemName,
                        onValueChange = { customItemName = it },
                        label = { Text("আইটেম বা কাজের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customItemQty,
                            onValueChange = { customItemQty = it },
                            label = { Text("পরিমাণ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customItemRate,
                            onValueChange = { customItemRate = it },
                            label = { Text("দর (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = customItemUnit,
                        onValueChange = { customItemUnit = it },
                        label = { Text("একক (পাতা / পিস / টি)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = customItemName.trim()
                        val qty = customItemQty.toDoubleOrNull() ?: 1.0
                        val rate = customItemRate.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && rate > 0.0) {
                            draftItems.add(
                                DraftBillItem(
                                    serviceName = name,
                                    quantity = qty,
                                    unitRate = rate,
                                    unitName = customItemUnit.trim()
                                )
                            )
                            showAddItemDialog = false
                        }
                    }
                ) {
                    Text("যোগ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Interactive Full Cash Memo Dialog
    if (activeCashMemoData != null) {
        AmarDokanCashMemoDialog(
            memoData = activeCashMemoData!!,
            onDismiss = {
                val exit = shouldExitAfterMemoDismiss
                activeCashMemoData = null
                if (exit) {
                    onBack()
                }
            }
        )
    }
}
