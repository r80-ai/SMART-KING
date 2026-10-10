package com.example.hisab.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storage.GlobalSaveManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintBillingScreen() {
    val context = LocalContext.current
    var isBangla by remember { mutableStateOf(true) }
    
    // Shop Details
    val shopName = if (isBangla) "টেলিকম সাইবার ক্যাফ" else "Telecom Cyber Cafe"
    val shopPhone = "7718489405"
    val shopAddress = if (isBangla) "চোরগেড়িয়া, সিহড়, কোতুলপুর, বাঁকুড়া, পশ্চিমবঙ্গ ৭২২১৬১" 
                      else "Chorgeriya, Sihar, Kotulpur, Bankura, West Bengal 722161"
    
    val upiId = "7718489@ybl"
    
    var customerName by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(mutableListOf<BillItem>()) }
    var newItemName by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }
    
    val totalAmount = items.sumOf { it.qty * it.price }
    
    var showSuccessDialog by remember { mutableStateOf(false) }
    var savedFilePath by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isBangla) "প্রিন্ট ও জেরক্স বিল" else "Print & Xerox Bill") },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    ) {
                        Button(
                            onClick = { isBangla = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBangla) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (isBangla) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.height(36.sp.value.dp * 1.5f),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("বাংলা")
                        }
                        Button(
                            onClick = { isBangla = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isBangla) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (!isBangla) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.height(36.sp.value.dp * 1.5f),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("English")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Customer Details
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text(if (isBangla) "গ্রাহকের নাম" else "Customer Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add Item Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            label = { Text(if (isBangla) "বিবরণ (উদাঃ প্রিন্ট)" else "Item (e.g. Print)") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newItemQty,
                            onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) newItemQty = it },
                            label = { Text(if (isBangla) "পরিমাণ" else "Qty") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newItemPrice,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) newItemPrice = it },
                            label = { Text(if (isBangla) "দর" else "Rate") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = {
                            if (newItemName.isNotBlank() && newItemQty.isNotBlank() && newItemPrice.isNotBlank()) {
                                items.add(BillItem(newItemName, newItemQty.toInt(), newItemPrice.toDouble()))
                                newItemName = ""; newItemQty = ""; newItemPrice = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(if (isBangla) "আইটেম যোগ করুন" else "Add Item")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items List
            Text(if (isBangla) "আইটেম তালিকা:" else "Items List:", fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(items) { index, item ->
                    ListItem(
                        headlineContent = { Text("${item.name} (${item.qty} x ${item.price})") },
                        trailingContent = {
                            Row {
                                Text("₹${item.qty * item.price}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { items.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }

            // Total and Save
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isBangla) "মোট টাকা:" else "Total Amount:", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("₹$totalAmount", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (items.isEmpty()) {
                                Toast.makeText(context, if (isBangla) "দয়া করে আইটেম যোগ করুন" else "Please add items", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Generate Receipt
                            val bitmap = generateReceiptBitmap(
                                context, shopName, shopPhone, shopAddress, 
                                customerName, items, totalAmount, upiId, isBangla
                            )
                            val fileName = "Bill_${System.currentTimeMillis()}.jpg"
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                            val result = GlobalSaveManager.saveFile(context, fileName, stream.toByteArray())
                            
                            if (result.success) {
                                savedFilePath = result.filePath ?: ""
                                showSuccessDialog = true
                            } else {
                                Toast.makeText(context, "Error: ${result.error}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBangla) "বিল সেভ করুন" else "Save Bill")
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(if (isBangla) "সফল!" else "Success!") },
            text = { 
                Column {
                    Text(if (isBangla) "বিলটি সফলভাবে সেভ করা হয়েছে।" else "Bill saved successfully.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(savedFilePath, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showSuccessDialog = false
                    // Simple logic to "view" file - usually would use Intent
                    Toast.makeText(context, "File at: $savedFilePath", Toast.LENGTH_LONG).show()
                }) {
                    Text(if (isBangla) "ফাইল দেখুন" else "View File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text(if (isBangla) "ঠিক আছে" else "OK")
                }
            }
        )
    }
}

data class BillItem(val name: String, val qty: Int, val price: Double)

fun generateReceiptBitmap(
    context: Context,
    shopName: String,
    shopPhone: String,
    shopAddress: String,
    customerName: String,
    items: List<BillItem>,
    totalAmount: Double,
    upiId: String,
    isBangla: Boolean
): Bitmap {
    val width = 500
    val headerHeight = 200
    val itemHeight = 40
    val footerHeight = 350
    val totalHeight = headerHeight + (items.size * itemHeight) + footerHeight
    
    val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    // Background
    canvas.drawColor(Color.WHITE)
    
    // Header
    paint.color = Color.BLACK
    paint.textAlign = android.graphics.Paint.Align.CENTER
    paint.textSize = 32f
    paint.isFakeBoldText = true
    canvas.drawText(shopName, (width / 2).toFloat(), 50f, paint)
    
    paint.textSize = 20f
    paint.isFakeBoldText = false
    canvas.drawText("${if (isBangla) "ফোন" else "Mob"}: $shopPhone", (width / 2).toFloat(), 85f, paint)
    
    paint.textSize = 16f
    // Simple word wrap for address
    val addressParts = shopAddress.chunked(40)
    var currentY = 110f
    addressParts.forEach {
        canvas.drawText(it, (width / 2).toFloat(), currentY, paint)
        currentY += 20f
    }
    
    canvas.drawLine(20f, currentY + 10, (width - 20).toFloat(), currentY + 10, paint)
    currentY += 40f
    
    // Customer Info
    paint.textAlign = android.graphics.Paint.Align.LEFT
    paint.textSize = 22f
    canvas.drawText("${if (isBangla) "গ্রাহক" else "Cust"}: $customerName", 30f, currentY, paint)
    
    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    paint.textAlign = android.graphics.Paint.Align.RIGHT
    paint.textSize = 18f
    canvas.drawText(date, (width - 30).toFloat(), currentY, paint)
    
    currentY += 40f
    canvas.drawLine(20f, currentY, (width - 20).toFloat(), currentY, paint)
    currentY += 30f
    
    // Table Header
    paint.textAlign = android.graphics.Paint.Align.LEFT
    paint.isFakeBoldText = true
    canvas.drawText(if (isBangla) "আইটেম" else "Item", 30f, currentY, paint)
    canvas.drawText(if (isBangla) "পরিমাণ" else "Qty", 250f, currentY, paint)
    canvas.drawText(if (isBangla) "মোট" else "Total", 400f, currentY, paint)
    
    currentY += 20f
    canvas.drawLine(20f, currentY, (width - 20).toFloat(), currentY, paint)
    currentY += 30f
    
    // Items
    paint.isFakeBoldText = false
    items.forEach { item ->
        canvas.drawText(item.name, 30f, currentY, paint)
        canvas.drawText(item.qty.toString(), 250f, currentY, paint)
        canvas.drawText("₹${item.qty * item.price}", 400f, currentY, paint)
        currentY += 40f
    }
    
    canvas.drawLine(20f, currentY, (width - 20).toFloat(), currentY, paint)
    currentY += 40f
    
    // Total
    paint.textSize = 28f
    paint.isFakeBoldText = true
    canvas.drawText(if (isBangla) "সর্বমোট:" else "Grand Total:", 30f, currentY, paint)
    canvas.drawText("₹$totalAmount", 350f, currentY, paint)
    
    currentY += 60f
    
    // UPI QR Code
    val upiPayload = "upi://pay?pa=$upiId&pn=${shopName.replace(" ", "%20")}&am=$totalAmount&cu=INR"
    val qrBitmap = generateQrCode(upiPayload, 200)
    if (qrBitmap != null) {
        canvas.drawBitmap(qrBitmap, 30f, currentY, paint)
        
        paint.textSize = 18f
        paint.isFakeBoldText = false
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText(if (isBangla) "স্ক্যান করে পেমেন্ট করুন" else "Scan to Pay", 250f, currentY + 80, paint)
        canvas.drawText(upiId, 250f, currentY + 110, paint)
    }
    
    currentY += 220f
    paint.textAlign = android.graphics.Paint.Align.CENTER
    paint.textSize = 20f
    canvas.drawText(if (isBangla) "--- ধন্যবাদ, আবার আসবেন ---" else "--- Thank You, Visit Again ---", (width / 2).toFloat(), currentY, paint)

    return bitmap
}

fun generateQrCode(text: String, size: Int): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
