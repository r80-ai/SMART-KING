package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdf.PdfEngine
import com.example.storage.GlobalSaveManager
import com.example.ui.PdfMasterViewModel
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintBillingScreen(
    onBack: () -> Unit,
    viewModel: PdfMasterViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var language by remember { mutableStateOf("bn") } // "bn" or "en"
    val isBn = language == "bn"

    // Billing items
    var bwCount by remember { mutableStateOf(0) }
    var colorCount by remember { mutableStateOf(0) }
    var laminationCount by remember { mutableStateOf(0) }
    var onlineFormFees by remember { mutableStateOf(0) }
    var otherAmount by remember { mutableStateOf(0) }
    var discountAmount by remember { mutableStateOf(0) }

    // Rates (defaults)
    val bwRate = 2
    val colorRate = 10
    val laminationRate = 15

    val subTotal = (bwCount * bwRate) + (colorCount * colorRate) + (laminationCount * laminationRate) + onlineFormFees + otherAmount
    val totalAmount = (subTotal - discountAmount).coerceAtLeast(0)

    val upiId = "7718489@ybl"
    val upiUri = "upi://pay?pa=$upiId&pn=Telecom%20Cyber%20Cafe&am=$totalAmount&cu=INR"
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(totalAmount) {
        if (totalAmount > 0) {
            qrBitmap = withContext(Dispatchers.Default) {
                QrCodeGenerator.generateUpiQr(upiUri, 300)
            }
        } else {
            qrBitmap = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isBn) "জেরক্স ও প্রিন্ট বিলিং" else "Xerox & Print Billing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { language = if (isBn) "en" else "bn" }) {
                        Text(if (isBn) "English" else "বাংলা", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SHOP INFO HEADER
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isBn) "টেলিকম সাইবার ক্যাফ" else "Telecom Cyber Cafe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isBn) "চোরগেড়িয়া, সিহড়, কোতুলপুর, বাঁকুড়া" else "Chorgeriya, Sihar, Kotulpur, Bankura",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Phone: 7718489405",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // BILLING ITEMS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BillingItemRow("ব্ল্যাক অ্যান্ড হোয়াইট (২৳)", "B&W Xerox/Print (₹2)", isBn, bwCount) { bwCount = it }
                BillingItemRow("কালার প্রিন্ট (১০৳)", "Color Print (₹10)", isBn, colorCount) { colorCount = it }
                BillingItemRow("ল্যামিনেশন/স্ক্যান (১৫৳)", "Lamination/Scan (₹15)", isBn, laminationCount) { laminationCount = it }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = if (onlineFormFees == 0) "" else onlineFormFees.toString(),
                    onValueChange = { onlineFormFees = it.toIntOrNull() ?: 0 },
                    label = { Text(if (isBn) "অনলাইন ফর্ম ফিস (টাকা)" else "Online Form Fees (₹)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )

                OutlinedTextField(
                    value = if (otherAmount == 0) "" else otherAmount.toString(),
                    onValueChange = { otherAmount = it.toIntOrNull() ?: 0 },
                    label = { Text(if (isBn) "অন্যান্য খরচ (টাকা)" else "Other Charges (₹)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )

                OutlinedTextField(
                    value = if (discountAmount == 0) "" else discountAmount.toString(),
                    onValueChange = { discountAmount = it.toIntOrNull() ?: 0 },
                    label = { Text(if (isBn) "ছাড় / ডিসকাউন্ট (টাকা)" else "Discount (₹)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Discount, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
            }

            // TOTAL & UPI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBn) "সাবটোটাল: ₹$subTotal" else "Sub-total: ₹$subTotal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBn) "মোট টাকা (টোটাল):" else "Total Amount:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "₹$totalAmount",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                    
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "UPI QR",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            // ACTIONS - SINGLE CONSOLIDATED SAVE & PRINT INVOICE BUTTON
            Button(
                onClick = {
                    scope.launch {
                        generateAndSaveBill(context, viewModel, isBn, bwCount, colorCount, laminationCount, onlineFormFees, otherAmount, totalAmount, upiUri, "PDF")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBn) "Save & Print Invoice (সেভ ও প্রিন্ট করুন)" else "Save & Print Invoice", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Text(
                text = if (isBn) "টিপ: বিলটি সেভ করলে এতে অটোমেটিক QR কোড এবং দোকানের বিস্তারিত যুক্ত হবে।" else "Tip: Saving the bill will automatically include a QR code and shop details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BillingItemRow(labelBn: String, labelEn: String, isBn: Boolean, count: Int, onCountChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isBn) labelBn else labelEn,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onCountChange(0) }) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = { if (count > 0) onCountChange(count - 1) }) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = count.toString(),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { onCountChange(count + 1) }) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private suspend fun generateAndSaveBill(
    context: android.content.Context,
    viewModel: PdfMasterViewModel,
    isBn: Boolean,
    bwVal: Int,
    colorVal: Int,
    lamVal: Int,
    onlineVal: Int,
    otherVal: Int,
    totalVal: Int,
    upiUri: String,
    format: String
) = withContext(Dispatchers.IO) {
    // Generate high-res Bitmap for the bill
    val width = 800
    val height = 1200
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)
    
    val paint = Paint().apply { isAntiAlias = true }
    val titlePaint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }
    
    val bodyPaint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.BLACK
        textSize = 28f
    }
    
    val totalPaint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 36f
    }

    // DRAW HEADER
    canvas.drawText(if (isBn) "টেলিকম সাইবার ক্যাফ" else "Telecom Cyber Cafe", width / 2f, 80f, titlePaint)
    bodyPaint.textAlign = Paint.Align.CENTER
    canvas.drawText(if (isBn) "চোরগেড়িয়া, সিহড়, কোতুলপুর, বাঁকুড়া" else "Chorgeriya, Sihar, Kotulpur, Bankura", width / 2f, 120f, bodyPaint)
    canvas.drawText("Phone: 7718489405", width / 2f, 160f, bodyPaint)
    
    paint.color = AndroidColor.LTGRAY
    canvas.drawLine(50f, 190f, width - 50f, 190f, paint)
    
    // BILL DETAILS
    bodyPaint.textAlign = Paint.Align.LEFT
    val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
    canvas.drawText(if (isBn) "তারিখ: $dateStr" else "Date: $dateStr", 60f, 240f, bodyPaint)
    
    var yPos = 300f
    val lineH = 50f
    
    if (bwVal > 0) {
        canvas.drawText(if (isBn) "ব্ল্যাক অ্যান্ড হোয়াইট" else "B&W Xerox/Print", 60f, yPos, bodyPaint)
        canvas.drawText("$bwVal x ₹2 = ₹${bwVal * 2}", width - 250f, yPos, bodyPaint)
        yPos += lineH
    }
    
    if (colorVal > 0) {
        canvas.drawText(if (isBn) "কালার প্রিন্ট" else "Color Print", 60f, yPos, bodyPaint)
        canvas.drawText("$colorVal x ₹10 = ₹${colorVal * 10}", width - 250f, yPos, bodyPaint)
        yPos += lineH
    }
    
    if (lamVal > 0) {
        canvas.drawText(if (isBn) "ল্যামিনেশন/স্ক্যান" else "Lamination/Scan", 60f, yPos, bodyPaint)
        canvas.drawText("$lamVal x ₹15 = ₹${lamVal * 15}", width - 250f, yPos, bodyPaint)
        yPos += lineH
    }
    
    if (onlineVal > 0) {
        canvas.drawText(if (isBn) "অনলাইন ফর্ম ফিস" else "Online Form Fees", 60f, yPos, bodyPaint)
        canvas.drawText("₹$onlineVal", width - 250f, yPos, bodyPaint)
        yPos += lineH
    }
    
    if (otherVal > 0) {
        canvas.drawText(if (isBn) "অন্যান্য খরচ" else "Other Charges", 60f, yPos, bodyPaint)
        canvas.drawText("₹$otherVal", width - 250f, yPos, bodyPaint)
        yPos += lineH
    }
    
    canvas.drawLine(50f, yPos + 20f, width - 50f, yPos + 20f, paint)
    yPos += 80f
    
    canvas.drawText(if (isBn) "সর্বমোট টাকা:" else "Total Amount:", 60f, yPos, totalPaint)
    canvas.drawText("₹$totalVal", width - 250f, yPos, totalPaint)
    
    // QR CODE
    val qrSize = 300
    val qr = QrCodeGenerator.generateUpiQr(upiUri, qrSize)
    if (qr != null) {
        canvas.drawBitmap(qr, (width - qrSize) / 2f, height - qrSize - 150f, null)
        bodyPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isBn) "স্ক্যান করে পেমেন্ট করুন" else "Scan & Pay", width / 2f, height - 120f, bodyPaint)
    }
    
    bodyPaint.textSize = 22f
    canvas.drawText(if (isBn) "আমাদের আসার জন্য ধন্যবাদ" else "Thank you for visiting us", width / 2f, height - 50f, bodyPaint)

    // SAVE
    val time = System.currentTimeMillis()
    val name = "Bill_$time"
    
    try {
        if (format == "JPG") {
            val file = File(context.cacheDir, "$name.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            val result = GlobalSaveManager.saveProcessedFile(context, file, "$name.jpg", "image/jpeg", "created", "Billing")
            viewModel.setNotification("Saved: ${result.fileName}")
        } else {
            val pdfFile = PdfEngine.createPdfFromBitmaps(context, listOf(bitmap), "$name.pdf")
            val result = GlobalSaveManager.saveProcessedFile(context, pdfFile, "$name.pdf", "application/pdf", "created", "Billing")
            viewModel.setNotification("Saved: ${result.fileName}")
        }
    } catch (e: Exception) {
        viewModel.setNotification("Error: ${e.message}")
    }
}
