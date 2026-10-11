package com.example.amardokan.ui.memo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.BanglaNumberUtil
import com.example.storage.GlobalSaveManager
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-Featured Interactive Cash Memo Dialog.
 * Displays the authentic rendered Bengali Cash Memo with zoom/scroll,
 * and direct action buttons for:
 * - Print (Thermal/Normal Printer via Android PrintManager)
 * - Save PDF (Downloads/Storage via GlobalSaveManager)
 * - Share on WhatsApp / System Apps (Actual PDF File)
 * - Dynamic Scannable UPI QR Modal (Merchant: 7718489@ybl)
 */
@Composable
fun AmarDokanCashMemoDialog(
    memoData: AmarDokanCashMemoGenerator.MemoData,
    onDismiss: () -> Unit,
    onMarkAsPaid: (() -> Unit)? = null // Optional callback if user verifies payment
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var memoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSavingPdf by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }

    LaunchedEffect(memoData) {
        memoBitmap = withContext(Dispatchers.Default) {
            AmarDokanCashMemoGenerator.generateMemoBitmap(memoData)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .testTag("dialog_cash_memo"),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmarDokanColors.NavyDarkHeader)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ক্যাশ মেমো (Cash Memo)",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${memoData.billNumber} • ${memoData.customerName}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_close_memo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Scrollable Cash Memo Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (memoBitmap != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shadowElevation = 4.dp,
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White
                            ) {
                                Image(
                                    bitmap = memoBitmap!!.asImageBitmap(),
                                    contentDescription = "Bengali Cash Memo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1000f / 1450f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AmarDokanColors.PrimaryBlue)
                        }
                    }
                }

                // Bottom Action Buttons
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Print & WhatsApp Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Print Button
                            Button(
                                onClick = {
                                    AmarDokanCashMemoGenerator.printMemo(context, memoData)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_print_cash_memo"),
                                colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.PrimaryBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("প্রিন্ট (Print)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            // Share WhatsApp Button
                            Button(
                                onClick = {
                                    AmarDokanCashMemoGenerator.shareOnWhatsAppOrApps(context, memoData, asPdf = true)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_share_whatsapp_memo"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("হোয়াটসঅ্যাপ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Row 2: Save PDF & UPI Pay QR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Save PDF Button
                            OutlinedButton(
                                onClick = {
                                    if (isSavingPdf) return@OutlinedButton
                                    isSavingPdf = true
                                    coroutineScope.launch {
                                        try {
                                            val pdfFile = withContext(Dispatchers.IO) {
                                                AmarDokanCashMemoGenerator.generatePdfFile(context, memoData)
                                            }
                                            val sanitized = memoData.billNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                                            val fileName = "CashMemo_${sanitized}.pdf"
                                            val result = GlobalSaveManager.saveProcessedFile(
                                                context = context,
                                                tempSourceFile = pdfFile,
                                                desiredFileName = fileName,
                                                mimeType = "application/pdf",
                                                category = "created",
                                                tag = "AmarDokan"
                                            )
                                            Toast.makeText(context, "✅ সেভ হয়েছে: ${result.fileName}", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isSavingPdf = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_save_pdf_memo"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isSavingPdf) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AmarDokanColors.PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("সেভ PDF", fontSize = 13.sp, color = AmarDokanColors.PrimaryBlue, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // UPI QR Button
                            OutlinedButton(
                                onClick = { showUpiQrDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_upi_qr_memo"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = AmarDokanColors.NavyDarkHeader, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("UPI কিউআর", fontSize = 13.sp, color = AmarDokanColors.NavyDarkHeader, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal UPI QR Payment Dialog
    if (showUpiQrDialog) {
        UpiPaymentQrDialog(
            billNumber = memoData.billNumber,
            customerName = memoData.customerName,
            payableAmount = memoData.grandTotal,
            isCreditBill = memoData.paymentType == "CREDIT",
            onDismiss = { showUpiQrDialog = false },
            onConfirmPaid = onMarkAsPaid
        )
    }
}

/**
 * Dedicated Scannable UPI QR Modal.
 * Generates genuine scannable UPI QR code for merchant 7718489@ybl.
 */
@Composable
fun UpiPaymentQrDialog(
    billNumber: String,
    customerName: String,
    payableAmount: Double,
    isCreditBill: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmPaid: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val merchantUpi = AmarDokanCashMemoGenerator.MERCHANT_UPI_ID
    val upiAmountStr = String.format(java.util.Locale.ENGLISH, "%.2f", payableAmount)
    val upiUri = "upi://pay?pa=$merchantUpi&pn=Telecom%20Cyber%20Cafe&am=$upiAmountStr&cu=INR&tn=Bill%20$billNumber"

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(upiUri) {
        qrBitmap = withContext(Dispatchers.Default) {
            QrCodeGenerator.generateUpiQr(upiUri, 380)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "ইউপিআই পেমেন্ট কিউআর কোড",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AmarDokanColors.NavyDarkHeader
                )
                Text(
                    text = "$billNumber • গ্রাহক: $customerName",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // QR Box
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = AmarDokanColors.PrimaryBlue)
                    }
                }

                // Payable Amount Highlight
                Surface(
                    color = if (isCreditBill) AmarDokanColors.DueRedLight else AmarDokanColors.CashGreenLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isCreditBill) "বকেয়া পরিশোধের পরিমাণ:" else "প্রদেয় সর্বমোট টাকা:",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "₹ ${BanglaNumberUtil.toBangla(payableAmount.toInt().toString())}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCreditBill) AmarDokanColors.DueRed else AmarDokanColors.CashGreen
                        )
                    }
                }

                // Merchant UPI ID display with Copy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("UPI ID", merchantUpi)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "UPI ID কপি করা হয়েছে: $merchantUpi", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("মার্চেন্ট ইউপিআই আইডি (UPI ID):", fontSize = 11.sp, color = Color.Gray)
                        Text(merchantUpi, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmarDokanColors.NavyDarkHeader)
                    }
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AmarDokanColors.PrimaryBlue, modifier = Modifier.size(18.dp))
                }

                // Important Note about payment verification
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সতর্কতা: কিউআর স্ক্যান করলে টাকা সরাসরি অ্যাকাউন্টে ঢুকবে। ব্যাংকে টাকা নিশ্চিত হলে তবেই রসিদকে পেইড হিসেবে মার্ক করুন।",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (onConfirmPaid != null) {
                Button(
                    onClick = {
                        onConfirmPaid()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.CashGreen)
                ) {
                    Text("পেমেন্ট নিশ্চিত করুন (Mark Paid)")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("বন্ধ করুন")
                }
            }
        },
        dismissButton = {
            if (onConfirmPaid != null) {
                TextButton(onClick = onDismiss) {
                    Text("বাতিল")
                }
            }
        }
    )
}
