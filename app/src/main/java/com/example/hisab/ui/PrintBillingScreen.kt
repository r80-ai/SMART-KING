package com.example.hisab.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.pdf.PdfEngine
import com.example.storage.GlobalSaveManager
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BillingItem(
    val id: String,
    var name: String,
    var quantity: Int,
    var unitRate: Double,
    val isRemovable: Boolean = false
) {
    val total: Double get() = quantity * unitRate
}

object BillingPreferenceManager {
    private const val PREFS_NAME = "cyber_billing_prefs"
    private const val KEY_SHOP_NAME = "pref_shop_name"
    private const val KEY_SHOP_PHONE = "pref_shop_phone"
    private const val KEY_SHOP_ADDRESS = "pref_shop_address"
    private const val KEY_RATE_BW = "rate_bw"
    private const val KEY_RATE_COLOR = "rate_color"
    private const val KEY_RATE_SCAN = "rate_scan"
    private const val KEY_RATE_LAM = "rate_lam"

    fun getShopName(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SHOP_NAME, "Digital Cyber Point & Studio") ?: "Digital Cyber Point & Studio"

    fun getShopPhone(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SHOP_PHONE, "9876543210") ?: "9876543210"

    fun getShopAddress(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SHOP_ADDRESS, "Station Road, Near Bus Stand") ?: "Station Road, Near Bus Stand"

    fun saveShopConfig(context: Context, name: String, phone: String, address: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SHOP_NAME, name.trim())
            .putString(KEY_SHOP_PHONE, phone.trim())
            .putString(KEY_SHOP_ADDRESS, address.trim())
            .apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintBillingScreen(
    onBack: () -> Unit,
    onOpenUpiBillerWithAmount: (Double, String) -> Unit,
    onShowSnackbar: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var shopName by remember { mutableStateOf(BillingPreferenceManager.getShopName(context)) }
    var shopPhone by remember { mutableStateOf(BillingPreferenceManager.getShopPhone(context)) }
    var shopAddress by remember { mutableStateOf(BillingPreferenceManager.getShopAddress(context)) }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var billNo by remember { mutableStateOf("#${(1000..9999).random()}") }

    // Standard service billing items
    val items = remember {
        mutableStateListOf(
            BillingItem("bw_print", "B&W Xerox / Print", 10, 3.0),
            BillingItem("color_print", "Color Print", 2, 10.0),
            BillingItem("scanning", "Document Scanning", 0, 5.0),
            BillingItem("lamination", "Lamination", 0, 20.0),
            BillingItem("online_form", "Online Form / Apply Fee", 0, 50.0)
        )
    }

    var discountAmountText by remember { mutableStateOf("0") }
    var showShopEditDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var isSavingPdf by remember { mutableStateOf(false) }

    val subtotal = items.sumOf { it.total }
    val discount = discountAmountText.toDoubleOrNull() ?: 0.0
    val grandTotal = (subtotal - discount).coerceAtLeast(0.0)

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("print_billing_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Print & Xerox Billing Memo",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$shopName • $billNo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_billing_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showShopEditDialog = true },
                        modifier = Modifier.testTag("btn_edit_shop_info")
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = "Shop Details", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CUSTOMER DETAILS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "কাস্টমার তথ্য (Optional)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name") },
                            placeholder = { Text("e.g. Amit Kumar") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("input_customer_name")
                        )

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Mobile") },
                            placeholder = { Text("98XXXXXXXX") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_customer_phone")
                        )
                    }
                }
            }

            // BILLING COUNTERS LIST
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "জেরক্স ও সার্ভিসের হিসাব",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TextButton(
                            onClick = { showAddItemDialog = true },
                            modifier = Modifier.testTag("btn_add_service_row")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("নতুন যোগ করুন", fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                        }
                    }

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("আইটেম", modifier = Modifier.weight(1.6f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Text("সংখ্যা", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Text("দর (₹)", modifier = Modifier.weight(1.0f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Text("মোট", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    // Rows
                    items.forEachIndexed { index, item ->
                        BillingItemRow(
                            item = item,
                            onQuantityChanged = { newQty ->
                                items[index] = item.copy(quantity = newQty)
                            },
                            onRateChanged = { newRate ->
                                items[index] = item.copy(unitRate = newRate)
                            },
                            onRemove = {
                                items.removeAt(index)
                            }
                        )
                        if (index < items.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                    }
                }
            }

            // SUMMARY & GRAND TOTAL CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("মোট মূল্য (Subtotal):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.US, "%.2f", subtotal)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ছাড় (Discount ₹):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = discountAmountText,
                            onValueChange = { discountAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            modifier = Modifier.width(110.dp).height(48.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সর্বমোট দেয় (Grand Total):",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", grandTotal)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = ToolGreen
                        )
                    }
                }
            }

            // ACTION 1: INSTANT BHARAT UPI QR BUTTON
            Button(
                onClick = {
                    onOpenUpiBillerWithAmount(grandTotal, "Bill $billNo: Xerox & Print")
                },
                colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_pay_via_upi_qr")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("UPI QR কোডে পেমেন্ট নিন (₹${String.format(Locale.US, "%.0f", grandTotal)})", fontWeight = FontWeight.Bold)
            }

            // ACTION 2 & 3: PRINT / SAVE PDF RECEIPT & SHARE VIA WHATSAPP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // PRINT & SAVE PDF RECEIPT
                Button(
                    onClick = {
                        if (isSavingPdf) return@Button
                        isSavingPdf = true
                        coroutineScope.launch {
                            try {
                                val activeItems = items.filter { it.quantity > 0 }
                                val pdfFile = generateThermalReceiptPdf(
                                    context = context,
                                    shopName = shopName,
                                    shopPhone = shopPhone,
                                    shopAddress = shopAddress,
                                    billNo = billNo,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    items = activeItems,
                                    subtotal = subtotal,
                                    discount = discount,
                                    grandTotal = grandTotal
                                )

                                val time = System.currentTimeMillis()
                                val saveResult = GlobalSaveManager.saveProcessedFile(
                                    context = context,
                                    tempSourceFile = pdfFile,
                                    desiredFileName = "Bill_Memo_${billNo.replace("#", "")}_$time.pdf",
                                    mimeType = "application/pdf",
                                    category = "created",
                                    tag = "Print Billing"
                                )
                                onShowSnackbar("রসিদ PDF সংরক্ষিত হয়েছে: ${saveResult.displayLocation}")
                            } catch (e: Exception) {
                                onShowSnackbar("PDF তৈরি ব্যর্থ: ${e.message}")
                            } finally {
                                isSavingPdf = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    enabled = !isSavingPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_save_pdf_receipt")
                ) {
                    if (isSavingPdf) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF Bill", fontWeight = FontWeight.Bold)
                    }
                }

                // SHARE VIA WHATSAPP / TEXT
                OutlinedButton(
                    onClick = {
                        val activeItems = items.filter { it.quantity > 0 }
                        val textSlip = StringBuilder().apply {
                            appendLine("🧾 *--- $shopName ---*")
                            if (shopAddress.isNotBlank()) appendLine("📍 $shopAddress")
                            if (shopPhone.isNotBlank()) appendLine("📞 Ph: $shopPhone")
                            appendLine("--------------------------------")
                            appendLine("Bill No: $billNo | Date: ${SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())}")
                            if (customerName.isNotBlank()) appendLine("Customer: $customerName")
                            if (customerPhone.isNotBlank()) appendLine("Mobile: $customerPhone")
                            appendLine("--------------------------------")
                            activeItems.forEach {
                                appendLine("• ${it.name}: ${it.quantity} × ₹${it.unitRate.toInt()} = ₹${String.format(Locale.US, "%.0f", it.total)}")
                            }
                            appendLine("--------------------------------")
                            appendLine("Subtotal: ₹${String.format(Locale.US, "%.2f", subtotal)}")
                            if (discount > 0) appendLine("Discount: -₹${String.format(Locale.US, "%.2f", discount)}")
                            appendLine("*GRAND TOTAL: ₹${String.format(Locale.US, "%.2f", grandTotal)}*")
                            appendLine("--------------------------------")
                            appendLine("🙏 Thank you for visiting!")
                        }.toString()

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, textSlip)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Bill via WhatsApp"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_share_bill_text")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp Share", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // DIALOG: EDIT SHOP DETAILS
    if (showShopEditDialog) {
        var tempName by remember { mutableStateOf(shopName) }
        var tempPhone by remember { mutableStateOf(shopPhone) }
        var tempAddress by remember { mutableStateOf(shopAddress) }

        AlertDialog(
            onDismissRequest = { showShopEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("দোকানের তথ্য পরিবর্তন")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("দোকানের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("ফোন নম্বর") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempAddress,
                        onValueChange = { tempAddress = it },
                        label = { Text("ঠিকানা") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shopName = tempName.ifBlank { "Cyber Cafe" }
                        shopPhone = tempPhone
                        shopAddress = tempAddress
                        BillingPreferenceManager.saveShopConfig(context, shopName, shopPhone, shopAddress)
                        showShopEditDialog = false
                        onShowSnackbar("দোকানের তথ্য আপডেট করা হয়েছে!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShopEditDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // DIALOG: ADD CUSTOM SERVICE ITEM
    if (showAddItemDialog) {
        var customName by remember { mutableStateOf("") }
        var customQty by remember { mutableStateOf("1") }
        var customRate by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("নতুন সার্ভিস আইটেম যোগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("সার্ভিসের নাম (e.g. PVC Card, Photo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customQty,
                            onValueChange = { customQty = it.filter { ch -> ch.isDigit() } },
                            label = { Text("সংখ্যা") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = customRate,
                            onValueChange = { customRate = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("দর (₹)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customName.isNotBlank()) {
                            val q = customQty.toIntOrNull() ?: 1
                            val r = customRate.toDoubleOrNull() ?: 10.0
                            items.add(BillingItem("custom_${System.currentTimeMillis()}", customName.trim(), q, r, isRemovable = true))
                            showAddItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
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
}

@Composable
private fun BillingItemRow(
    item: BillingItem,
    onQuantityChanged: (Int) -> Unit,
    onRateChanged: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item Name
        Column(modifier = Modifier.weight(1.6f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.isRemovable) {
                Text(
                    text = "মুছুন",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonPrimary,
                    modifier = Modifier.clickable { onRemove() }
                )
            }
        }

        // Quantity counter (- count +)
        Row(
            modifier = Modifier.weight(1.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { if (item.quantity > 0) onQuantityChanged(item.quantity - 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }
            Text(
                text = "${item.quantity}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = { onQuantityChanged(item.quantity + 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
        }

        // Unit Rate
        Text(
            text = "₹${String.format(Locale.US, "%.0f", item.unitRate)}",
            modifier = Modifier.weight(1.0f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Row Total
        Text(
            text = "₹${String.format(Locale.US, "%.0f", item.total)}",
            modifier = Modifier.weight(1.0f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Creates an authentic, thermal-receipt style PDF slip (58mm/80mm width standard).
 */
private suspend fun generateThermalReceiptPdf(
    context: Context,
    shopName: String,
    shopPhone: String,
    shopAddress: String,
    billNo: String,
    customerName: String,
    customerPhone: String,
    items: List<BillingItem>,
    subtotal: Double,
    discount: Double,
    grandTotal: Double
): File = withContext(Dispatchers.IO) {
    val pageWidth = 384 // 80mm thermal receipt width in points
    val estimatedHeight = 520 + (items.size * 30)
    val pageHeight = estimatedHeight.coerceAtLeast(600)

    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas

    canvas.drawColor(AndroidColor.WHITE)

    var currentY = 40f

    val centerPaint = Paint().apply {
        color = AndroidColor.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 20f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText(shopName, pageWidth / 2f, currentY, centerPaint)
    currentY += 22f

    if (shopAddress.isNotBlank()) {
        val addrPaint = Paint().apply {
            color = AndroidColor.DKGRAY
            textSize = 12f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(shopAddress, pageWidth / 2f, currentY, addrPaint)
        currentY += 18f
    }

    if (shopPhone.isNotBlank()) {
        val phonePaint = Paint().apply {
            color = AndroidColor.DKGRAY
            textSize = 12f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Phone: $shopPhone", pageWidth / 2f, currentY, phonePaint)
        currentY += 22f
    }

    // Divider Line
    val linePaint = Paint().apply {
        color = AndroidColor.BLACK
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }
    canvas.drawLine(20f, currentY, pageWidth - 20f, currentY, linePaint)
    currentY += 20f

    // Metadata: Bill No & Date
    val metaPaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 12f
        isAntiAlias = true
    }
    val rightMetaPaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 12f
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }

    canvas.drawText("Memo: $billNo", 20f, currentY, metaPaint)
    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    canvas.drawText(sdf.format(Date()), pageWidth - 20f, currentY, rightMetaPaint)
    currentY += 18f

    if (customerName.isNotBlank()) {
        canvas.drawText("Customer: $customerName", 20f, currentY, metaPaint)
        if (customerPhone.isNotBlank()) {
            canvas.drawText("Ph: $customerPhone", pageWidth - 20f, currentY, rightMetaPaint)
        }
        currentY += 18f
    }

    // Divider Line
    canvas.drawLine(20f, currentY, pageWidth - 20f, currentY, linePaint)
    currentY += 20f

    // Table Header
    val headerPaint = Paint().apply {
        color = AndroidColor.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
        isAntiAlias = true
    }
    canvas.drawText("ITEM DESCRIPTION", 20f, currentY, headerPaint)
    canvas.drawText("QTY", 210f, currentY, headerPaint)
    canvas.drawText("RATE", 270f, currentY, headerPaint)
    canvas.drawText("AMOUNT", pageWidth - 20f, currentY, Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT })
    currentY += 12f

    canvas.drawLine(20f, currentY, pageWidth - 20f, currentY, linePaint)
    currentY += 18f

    // Item Rows
    val rowPaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 11.5f
        isAntiAlias = true
    }
    val rightRowPaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 11.5f
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }

    items.forEach { item ->
        canvas.drawText(item.name.take(24), 20f, currentY, rowPaint)
        canvas.drawText("${item.quantity}", 215f, currentY, rowPaint)
        canvas.drawText("₹${item.unitRate.toInt()}", 270f, currentY, rowPaint)
        canvas.drawText("₹${String.format(Locale.US, "%.0f", item.total)}", pageWidth - 20f, currentY, rightRowPaint)
        currentY += 20f
    }

    // Divider
    currentY += 6f
    canvas.drawLine(20f, currentY, pageWidth - 20f, currentY, linePaint)
    currentY += 20f

    // Subtotal & Grand Total
    val totalLabelPaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 12f
        isAntiAlias = true
    }
    canvas.drawText("Subtotal:", 160f, currentY, totalLabelPaint)
    canvas.drawText("₹${String.format(Locale.US, "%.2f", subtotal)}", pageWidth - 20f, currentY, rightRowPaint)
    currentY += 18f

    if (discount > 0) {
        canvas.drawText("Discount:", 160f, currentY, totalLabelPaint)
        canvas.drawText("-₹${String.format(Locale.US, "%.2f", discount)}", pageWidth - 20f, currentY, rightRowPaint)
        currentY += 18f
    }

    val grandTotalPaint = Paint().apply {
        color = AndroidColor.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 16f
        isAntiAlias = true
    }
    val rightGrandPaint = Paint().apply {
        color = AndroidColor.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 16f
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }
    canvas.drawText("TOTAL PAID:", 160f, currentY + 4f, grandTotalPaint)
    canvas.drawText("₹${String.format(Locale.US, "%.2f", grandTotal)}", pageWidth - 20f, currentY + 4f, rightGrandPaint)
    currentY += 32f

    // Footer
    canvas.drawLine(20f, currentY, pageWidth - 20f, currentY, linePaint)
    currentY += 24f

    val footerPaint = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 12f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Thank you for visiting! Please visit again.", pageWidth / 2f, currentY, footerPaint)

    doc.finishPage(page)

    val tempFile = File(context.cacheDir, "thermal_receipt_${System.currentTimeMillis()}.pdf")
    FileOutputStream(tempFile).use { out ->
        doc.writeTo(out)
        out.flush()
    }
    doc.close()
    tempFile
}
