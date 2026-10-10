package com.example.hisab.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.storage.GlobalSaveManager
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UpiPreferenceManager {
    private const val PREFS_NAME = "upi_biller_prefs"
    private const val KEY_UPI_ID = "pref_upi_id"
    private const val KEY_PAYEE_NAME = "pref_payee_name"

    fun getUpiId(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_UPI_ID, "") ?: ""
    }

    fun getPayeeName(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_PAYEE_NAME, "Digital Cyber Point") ?: "Digital Cyber Point"
    }

    fun saveConfig(context: Context, upiId: String, payeeName: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_UPI_ID, upiId.trim()).putString(KEY_PAYEE_NAME, payeeName.trim()).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickUpiQrScreen(
    initialAmount: Double = 0.0,
    initialNote: String = "",
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    var upiId by remember { mutableStateOf(UpiPreferenceManager.getUpiId(context)) }
    var payeeName by remember { mutableStateOf(UpiPreferenceManager.getPayeeName(context)) }

    var amountText by remember {
        mutableStateOf(if (initialAmount > 0) String.format(Locale.US, "%.0f", initialAmount) else "")
    }
    var transactionNote by remember { mutableStateOf(initialNote) }

    var showSettingsDialog by remember { mutableStateOf(upiId.isBlank()) }
    var isFullscreenQr by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    // Recompute QR Code whenever amount, UPI ID or payee name changes
    LaunchedEffect(upiId, payeeName, amountText, transactionNote) {
        if (upiId.isBlank()) {
            qrBitmap = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.Default) {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            val amtStr = if (amt > 0) String.format(Locale.US, "%.2f", amt) else ""
            val cleanName = URLEncoder.encode(payeeName.ifBlank { "Store" }, "UTF-8")
            val cleanNote = if (transactionNote.isNotBlank()) "&tn=${URLEncoder.encode(transactionNote.trim(), "UTF-8")}" else ""

            val upiPayload = if (amtStr.isNotBlank()) {
                "upi://pay?pa=$upiId&pn=$cleanName&am=$amtStr&cu=INR$cleanNote"
            } else {
                "upi://pay?pa=$upiId&pn=$cleanName&cu=INR$cleanNote"
            }

            val bmp = generateUpiQrBitmap(upiPayload, 600)
            qrBitmap = bmp
        }
    }

    BackHandler {
        if (isFullscreenQr) {
            isFullscreenQr = false
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("quick_upi_qr_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Quick UPI QR Biller",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (upiId.isNotBlank()) "$payeeName • $upiId" else "দোকানের UPI কোড সেট করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_upi_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("btn_upi_settings")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "UPI Settings", tint = MaterialTheme.colorScheme.primary)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // IF UPI ID IS NOT CONFIGURED, SHOW BIG PROMPT
            if (upiId.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonPrimary.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(48.dp))
                        Text(
                            text = "দোকানের UPI আইডি সেট করুন",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "কাস্টমার থেকে দ্রুত টাকা পেতে আপনার PhonePe, GPay বা Paytm UPI ID যুক্ত করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showSettingsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Text("UPI আইডি যোগ করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // AMOUNT INPUT SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "বিল অ্যামাউন্ট (টাকা)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Enter Amount") },
                        placeholder = { Text("0.00") },
                        prefix = {
                            Text(
                                text = "₹ ",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = ToolGreen
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        trailingIcon = {
                            if (amountText.isNotEmpty()) {
                                IconButton(onClick = { amountText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_upi_amount")
                    )

                    // QUICK AMOUNT CHIPS (+10, +20, +50, +100, +200, +500)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10, 20, 50, 100, 200).forEach { chipAdd ->
                            FilledTonalButton(
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    val next = current + chipAdd
                                    amountText = String.format(Locale.US, "%.0f", next)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+₹$chipAdd", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // QR DISPLAY CARD (High contrast for counter scan)
            if (qrBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_upi_qr_display"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Shop Name & Payee
                        Text(
                            text = payeeName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )

                        // Amount Badge
                        val currentAmt = amountText.toDoubleOrNull() ?: 0.0
                        if (currentAmt > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AndroidColor.parseColor("#E8F5E9").let { Color(it) }
                            ) {
                                Text(
                                    text = "Pay ₹${String.format(Locale.US, "%.2f", currentAmt)}",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = AndroidColor.parseColor("#1B5E20").let { Color(it) },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // HIGH CONTRAST QR IMAGE
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            qrBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Bharat UPI QR",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // UPI ID & SUPPORTED APPS HINT
                        Text(
                            text = "UPI: $upiId",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.DarkGray
                        )

                        Text(
                            text = "Scan with any UPI App • GPay • PhonePe • Paytm • BHIM",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // BOTTOM ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // COPY UPI ID
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(upiId))
                            onShowSnackbar("UPI ID কপি করা হয়েছে!")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy ID", fontSize = 12.sp)
                    }

                    // SHARE QR SLIP
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val bmp = qrBitmap ?: return@launch
                                    val slipBmp = createCustomerPaymentSlip(bmp, payeeName, upiId, amountText.toDoubleOrNull() ?: 0.0)
                                    val tempFile = File(context.cacheDir, "UPI_QR_Slip_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(tempFile).use { out ->
                                        slipBmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                        out.flush()
                                    }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, "Payment QR for $payeeName: ₹$amountText")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share UPI QR"))
                                } catch (e: Exception) {
                                    onShowSnackbar("Share error: ${e.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // SAVE QR IMAGE TO STORAGE
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val bmp = qrBitmap ?: return@launch
                                    val slipBmp = createCustomerPaymentSlip(bmp, payeeName, upiId, amountText.toDoubleOrNull() ?: 0.0)
                                    val time = System.currentTimeMillis()
                                    val tempFile = File(context.cacheDir, "UPI_QR_$time.jpg")
                                    FileOutputStream(tempFile).use { out ->
                                        slipBmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                        out.flush()
                                    }
                                    val saveResult = GlobalSaveManager.saveProcessedFile(
                                        context = context,
                                        tempSourceFile = tempFile,
                                        desiredFileName = "UPI_QR_${payeeName.replace("\\s+".toRegex(), "_")}_$time.jpg",
                                        mimeType = "image/jpeg",
                                        category = "photos",
                                        tag = "UPI QR"
                                    )
                                    onShowSnackbar("QR কোড সংরক্ষিত হয়েছে: ${saveResult.displayLocation}")
                                } catch (e: Exception) {
                                    onShowSnackbar("Save error: ${e.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // SETTINGS DIALOG FOR SHOP UPI ID & PAYEE NAME
    if (showSettingsDialog) {
        var tempUpi by remember { mutableStateOf(upiId) }
        var tempName by remember { mutableStateOf(payeeName) }

        AlertDialog(
            onDismissRequest = {
                if (upiId.isNotBlank()) showSettingsDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("দোকানের UPI তথ্য", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "একবার সেট করলে প্রতিবার সরাসরি কিউআর কোড তৈরি হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("দোকানের নাম / Payee Name") },
                        placeholder = { Text("e.g. Digital Cyber Point") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempUpi,
                        onValueChange = { tempUpi = it.trim() },
                        label = { Text("দোকানের UPI ID") },
                        placeholder = { Text("e.g. cybercafe@okhdfcbank") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempUpi.isNotBlank()) {
                            upiId = tempUpi
                            payeeName = tempName.ifBlank { "My Cyber Cafe" }
                            UpiPreferenceManager.saveConfig(context, upiId, payeeName)
                            showSettingsDialog = false
                            onShowSnackbar("UPI সেটিংস সফলভাবে সংরক্ষিত!")
                        } else {
                            onShowSnackbar("অনুগ্রহ করে একটি সঠিক UPI ID লিখুন")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (upiId.isNotBlank()) {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("বাতিল")
                    }
                }
            }
        )
    }
}

/**
 * Standard Bharat UPI QR bitmap generator using ZXing QRCodeWriter.
 */
private fun generateUpiQrBitmap(content: String, size: Int = 600): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1
    )
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
        }
    }
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, width, 0, 0, width, height)
    return bmp
}

/**
 * Composites a branded counter-top payment card with shop name, amount, and QR.
 */
private fun createCustomerPaymentSlip(qrBmp: Bitmap, shopName: String, upiId: String, amount: Double): Bitmap {
    val width = 720
    val height = 960
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    canvas.drawColor(AndroidColor.WHITE)

    // Top Header Banner
    val bannerPaint = Paint().apply {
        color = AndroidColor.parseColor("#B71C1C")
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), 150f, bannerPaint)

    val titlePaint = Paint().apply {
        color = AndroidColor.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText(shopName, width / 2f, 75f, titlePaint)

    val subTitlePaint = Paint().apply {
        color = AndroidColor.parseColor("#FFCDD2")
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("SCAN & PAY WITH ANY UPI APP", width / 2f, 115f, subTitlePaint)

    // Draw Amount Badge if amount > 0
    if (amount > 0) {
        val amtPaint = Paint().apply {
            color = AndroidColor.parseColor("#1B5E20")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 46f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Amount: ₹${String.format(Locale.US, "%.2f", amount)}", width / 2f, 215f, amtPaint)
    }

    // Draw QR Code centered
    val qrSize = 460
    val qrLeft = (width - qrSize) / 2f
    val qrTop = if (amount > 0) 250f else 200f
    val srcRect = Rect(0, 0, qrBmp.width, qrBmp.height)
    val destRect = Rect(qrLeft.toInt(), qrTop.toInt(), (qrLeft + qrSize).toInt(), (qrTop + qrSize).toInt())
    canvas.drawBitmap(qrBmp, srcRect, destRect, null)

    // Border around QR
    val borderPaint = Paint().apply {
        color = AndroidColor.BLACK
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    canvas.drawRect(destRect, borderPaint)

    // Draw Footer UPI details
    val upiPaint = Paint().apply {
        color = AndroidColor.DKGRAY
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val footerY = qrTop + qrSize + 55f
    canvas.drawText("UPI ID: $upiId", width / 2f, footerY, upiPaint)

    val appsPaint = Paint().apply {
        color = AndroidColor.GRAY
        textSize = 20f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Google Pay • PhonePe • Paytm • BHIM • Amazon Pay", width / 2f, footerY + 35f, appsPaint)

    return result
}
