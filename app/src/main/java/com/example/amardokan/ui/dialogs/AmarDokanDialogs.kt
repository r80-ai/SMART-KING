package com.example.amardokan.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.data.Bill
import com.example.amardokan.data.BillItem
import com.example.amardokan.data.Customer
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil
import com.example.amardokan.ui.memo.AmarDokanCashMemoDialog
import com.example.amardokan.ui.memo.AmarDokanCashMemoGenerator
import com.example.amardokan.data.DraftBillItem
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.amardokan.data.AmarDokanBackupManager
import com.example.amardokan.data.DatabaseBackup
import java.io.File

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch

@Composable
fun AddCustomerDialog(
    customerToEdit: Customer? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (name: String, phone: String, address: String, notes: String) -> Unit,
    viewModel: AmarDokanViewModel = viewModel()
) {
    var name by remember { mutableStateOf(customerToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(customerToEdit?.phone ?: "") }
    var address by remember { mutableStateOf(customerToEdit?.address ?: "") }
    var notes by remember { mutableStateOf(customerToEdit?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (customerToEdit == null) "নতুন কাস্টমার যোগ করুন" else "কাস্টমারের তথ্য এডিট করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("কাস্টমারের পুরো নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("মোবাইল নম্বর (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("বিশেষ নোট (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotBlank()) {
                        onSave(name.trim(), phone.trim(), address.trim(), notes.trim())
                    }
                }
            ) {
                Text(if (customerToEdit == null) "যোগ করুন" else "আপডেট করুন")
            }
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (customerToEdit != null) {
                    TextButton(
                        onClick = {
                            if (onDelete != null) {
                                onDelete()
                            } else {
                                viewModel.deleteCustomer(customerToEdit)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        modifier = Modifier.testTag("delete_customer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss) { Text("বাতিল") }
            }
        }
    )
}

@Composable
fun BillDetailDialog(
    bill: Bill,
    viewModel: AmarDokanViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var billItems by remember { mutableStateOf<List<BillItem>>(emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullCashMemo by remember { mutableStateOf(false) }

    LaunchedEffect(bill.id) {
        billItems = viewModel.getBillItems(bill.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "বিলের বিবরণ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "${bill.billNumber} • ${bill.dateFormatted}", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                }
                Surface(
                    color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreenLight else AmarDokanColors.DueRedLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (bill.paymentType == "CASH") "নগদ" else "বাকি",
                        color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreen else AmarDokanColors.DueRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "গ্রাহক: ${bill.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (bill.customerPhone.isNotBlank()) {
                    Text(text = "ফোন: ${bill.customerPhone}", fontSize = 12.sp, color = AmarDokanColors.TextSecondary)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = AmarDokanColors.BorderLight)
                Spacer(modifier = Modifier.height(10.dp))

                Text("আইটেম তালিকা:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                billItems.forEach { itm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = itm.serviceName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "${itm.quantity.toInt()} × ₹${itm.unitRate.toInt()}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(
                            text = "₹${BanglaNumberUtil.toBangla(itm.lineTotal.toInt().toString())}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = AmarDokanColors.BorderLight)
                Spacer(modifier = Modifier.height(10.dp))

                if (bill.discount > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("সাবটোটাল:", fontSize = 13.sp, color = Color.Gray)
                        Text(BanglaNumberUtil.formatCurrencyBn(bill.subtotal), fontSize = 13.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ছাড় (Discount):", fontSize = 13.sp, color = Color.Gray)
                        Text("- " + BanglaNumberUtil.formatCurrencyBn(bill.discount), fontSize = 13.sp, color = AmarDokanColors.CashGreen)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("সর্বমোট প্রদেয় বিল:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = BanglaNumberUtil.formatCurrencyBn(bill.grandTotal),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = if (bill.paymentType == "CASH") AmarDokanColors.CashGreen else AmarDokanColors.DueRed
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Action: Open Authentic Cash Memo (Print / PDF / Share / QR)
                Button(
                    onClick = { showFullCashMemo = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.NavyDarkHeader),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("অরিজিনাল ক্যাশ মেমো দেখুন ও প্রিন্ট করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Action buttons: Text Share & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sb = StringBuilder()
                            sb.append("🧾 *আমার দোকান - বিল রসিদ*\n")
                            sb.append("বিল নং: ${bill.billNumber}\n")
                            sb.append("তারিখ: ${bill.dateFormatted}\n")
                            sb.append("গ্রাহক: ${bill.customerName}\n")
                            sb.append("পেমেন্ট: ${if (bill.paymentType == "CASH") "নগদ (পরিশোধিত)" else "বাকি (বকেয়া)"}\n")
                            sb.append("----------------------------\n")
                            billItems.forEach {
                                sb.append("${it.serviceName}: ${it.quantity.toInt()} × ₹${it.unitRate.toInt()} = ₹${it.lineTotal.toInt()}\n")
                            }
                            if (bill.discount > 0.0) sb.append("ছাড়: -₹${bill.discount.toInt()}\n")
                            sb.append("----------------------------\n")
                            sb.append("*সর্বমোট বিল: ₹${bill.grandTotal.toInt()}*\n")
                            sb.append("ধন্যবাদ! আবার আসবেন।")

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, sb.toString())
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "বিল রসিদ শেয়ার করুন"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("টেক্সট রসিদ", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("বিল মুছুন", fontSize = 12.sp, color = Color.Red)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
        }
    )

    // Full Authentic Cash Memo Dialog
    if (showFullCashMemo) {
        val memoData = AmarDokanCashMemoGenerator.MemoData.fromBill(bill, billItems)
        AmarDokanCashMemoDialog(
            memoData = memoData,
            onDismiss = { showFullCashMemo = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("বিলটি মুছে ফেলবেন?") },
            text = { Text("বিলটি ডিলিট করলে এর সাথে জড়িত বাকি বা নগদ হিসাব স্বয়ংক্রিয়ভাবে সমন্বয় হয়ে যাবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(bill)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("হ্যাঁ, ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
fun BackupRestoreDialog(
    viewModel: AmarDokanViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var backupJson by remember { mutableStateOf("") }
    var inputRestoreJson by remember { mutableStateOf("") }
    var isRestoring by remember { mutableStateOf(false) }

    val isBackupRunning by AmarDokanBackupManager.isBackupRunning.collectAsState()
    val lastBackupTime by AmarDokanBackupManager.lastBackupTime.collectAsState()
    val lastBackupPath by AmarDokanBackupManager.lastBackupPath.collectAsState()

    var showRestoreList by remember { mutableStateOf(false) }
    var localBackupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var pendingRestoreData by remember { mutableStateOf<Pair<String, DatabaseBackup>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val inspectResult = AmarDokanBackupManager.inspectBackupUri(context, uri)
                inspectResult.onSuccess { pair ->
                    pendingRestoreData = pair
                }.onFailure { err ->
                    viewModel.showMessage("❌ ফাইল পড়তে ত্রুটি: ${err.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        backupJson = viewModel.exportBackupJson()
        AmarDokanBackupManager.init(context)
        localBackupFiles = AmarDokanBackupManager.listLocalBackupFiles()
    }

    // Confirmation dialog before restoring database
    if (pendingRestoreData != null) {
        val (fileName, backup) = pendingRestoreData!!
        AlertDialog(
            onDismissRequest = { if (!isRestoring) pendingRestoreData = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmarDokanColors.AmberOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ব্যাকআপ রিস্টোর নিশ্চিত করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ফাইল: $fileName", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    HorizontalDivider(color = AmarDokanColors.BorderLight)
                    Text("এই ব্যাকআপে প্রাপ্ত রেকর্ড সংখ্যা:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("• খদ্দের তালিকা: ${backup.customers.size} জন", fontSize = 12.sp)
                    Text("• তৈরি বিল: ${backup.bills.size} টি", fontSize = 12.sp)
                    Text("• গৃহীত পেমেন্ট: ${backup.payments.size} টি", fontSize = 12.sp)
                    Text("• আইটেম ও রেট: ${backup.services.size} টি", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ সতর্কতা: রিস্টোর করলে বর্তমান ডাটাবেজ এই ফাইলের ডাটা দিয়ে প্রতিস্থাপিত হবে।",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRestoring = true
                        coroutineScope.launch {
                            val res = AmarDokanBackupManager.restoreDatabase(context, backup)
                            isRestoring = false
                            pendingRestoreData = null
                            if (res.isSuccess) {
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.CashGreen)
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিস্টোর হচ্ছে...", fontSize = 12.sp)
                    } else {
                        Text("হ্যাঁ, রিস্টোর করুন", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingRestoreData = null },
                    enabled = !isRestoring
                ) {
                    Text("বাতিল", fontSize = 12.sp)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = AmarDokanColors.PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ডাটা ব্যাকআপ ও রিস্টোর", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "আপনার দোকানের সমস্ত খদ্দের, বিল, রেট ও লেনদেনের ডাটা ফোনের ইন্টারনাল মেমরিতে অফলাইনে সুরক্ষিত রাখুন।",
                    fontSize = 12.sp,
                    color = AmarDokanColors.TextSecondary
                )

                // ---------------- 1. LOCAL STORAGE BACKUP SECTION ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmarDokanColors.ContentBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = AmarDokanColors.PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("১. লোকাল স্টোরেজ ব্যাকআপ (Local Storage Backup)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Target Directory Display
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, AmarDokanColors.BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("সংরক্ষণ লোকেশন (Location):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmarDokanColors.TextSecondary)
                                Text(
                                    AmarDokanBackupManager.TARGET_FOLDER_PATH,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AmarDokanColors.PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Last backup info
                        val lastBackupFormatted = if (lastBackupTime > 0) {
                            SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(lastBackupTime))
                        } else {
                            "কখনো ব্যাকআপ নেওয়া হয়নি (Never)"
                        }
                        Text("সর্বশেষ ব্যাকআপ: $lastBackupFormatted", fontSize = 11.sp, color = AmarDokanColors.TextSecondary)

                        if (!lastBackupPath.isNullOrBlank()) {
                            Text(
                                text = "ফাইল: ${File(lastBackupPath!!).name}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AmarDokanColors.CashGreen
                            )
                        }

                        // Backup Now button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val result = AmarDokanBackupManager.createLocalBackup(context)
                                    if (result.isSuccess) {
                                        localBackupFiles = AmarDokanBackupManager.listLocalBackupFiles()
                                        viewModel.showMessage("✅ ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে!")
                                    }
                                }
                            },
                            enabled = !isBackupRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_amar_dokan_backup_now")
                        ) {
                            if (isBackupRunning) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ব্যাকআপ তৈরি হচ্ছে...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("এখনই ব্যাকআপ নিন (Backup Now)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Restore from Storage button
                        OutlinedButton(
                            onClick = {
                                localBackupFiles = AmarDokanBackupManager.listLocalBackupFiles()
                                showRestoreList = !showRestoreList
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_amar_dokan_restore_storage")
                        ) {
                            Icon(Icons.Default.RestorePage, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmarDokanColors.PrimaryBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (showRestoreList) "ব্যাকআপ ফাইল তালিকা লুকান" else "ব্যাকআপ থেকে রিস্টোর (Restore from Storage)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AmarDokanColors.PrimaryBlue
                            )
                        }

                        // Expanded list of local backups
                        if (showRestoreList) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "📁 সংরক্ষিত ব্যাকআপ ফাইল (${localBackupFiles.size}টি পাওয়া গেছে):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmarDokanColors.TextPrimary
                                    )

                                    if (localBackupFiles.isEmpty()) {
                                        Text(
                                            text = "এখনো কোনো ব্যাকআপ ফাইল নেই। উপরের 'এখনই ব্যাকআপ নিন' বাটনে চাপ দিয়ে ব্যাকআপ তৈরি করুন।",
                                            fontSize = 11.sp,
                                            color = AmarDokanColors.TextSecondary
                                        )
                                    } else {
                                        localBackupFiles.take(6).forEach { file ->
                                            val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
                                            val sizeKb = (file.length() / 1024).coerceAtLeast(1)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(AmarDokanColors.ContentBg)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(file.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                    Text("$dateStr • ${sizeKb} KB", fontSize = 10.sp, color = AmarDokanColors.TextSecondary)
                                                }
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val inspectRes = AmarDokanBackupManager.inspectBackupFile(file)
                                                            inspectRes.onSuccess { backup ->
                                                                pendingRestoreData = Pair(file.name, backup)
                                                            }.onFailure { err ->
                                                                viewModel.showMessage("❌ ফাইল পড়তে ত্রুটি: ${err.message}")
                                                            }
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.CashGreen),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("রিস্টোর", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }

                                    // Pick other file from storage
                                    OutlinedButton(
                                        onClick = {
                                            filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("অন্য ফোল্ডার থেকে ফাইল নির্বাচন করুন...", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = AmarDokanColors.BorderLight)

                // ---------------- 2. LOCAL JSON EXPORT / COPY ----------------
                Text("২. অফলাইন ব্যাকআপ কোড কপি / শেয়ার করুন:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Amar Dokan Backup", backupJson)
                            clipboard.setPrimaryClip(clip)
                            viewModel.showMessage("✅ ব্যাকআপ ডাটা ক্লিপবোর্ডে কপি হয়েছে!")
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কপি করুন", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, backupJson)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "ব্যাকআপ শেয়ার করুন"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("শেয়ার করুন", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = AmarDokanColors.BorderLight)

                // ---------------- 3. LOCAL RESTORE FROM TEXT ----------------
                Text("৩. ব্যাকআপ কোড পেস্ট করে রিস্টোর:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                OutlinedTextField(
                    value = inputRestoreJson,
                    onValueChange = { inputRestoreJson = it },
                    placeholder = { Text("এখানে ব্যাকআপ JSON কোড পেস্ট করুন...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )

                Button(
                    onClick = {
                        if (inputRestoreJson.trim().isNotBlank()) {
                            isRestoring = true
                            coroutineScope.launch {
                                val ok = viewModel.importBackupJson(inputRestoreJson.trim())
                                isRestoring = false
                                if (ok) onDismiss()
                            }
                        } else {
                            viewModel.showMessage("❌ অনুগ্রহ করে ব্যাকআপ টেকস্ট পেস্ট করুন")
                        }
                    },
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.CashGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ডাটা রিস্টোর করুন (Restore)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
        }
    )
}


/**
 * Printable Receipt Slip Generator (Thermal Print / Web Print view).
 */
fun printReceiptHtml(
    context: Context,
    billNumber: String,
    customerName: String,
    dateStr: String,
    items: List<DraftBillItem>,
    subtotal: Double,
    discount: Double,
    grandTotal: Double,
    paymentType: String
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return

    val html = StringBuilder().apply {
        append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
        append("<style>")
        append("body { font-family: monospace; width: 280px; margin: 0 auto; padding: 10px; color: #000; font-size: 12px; }")
        append(".center { text-align: center; }")
        append(".bold { font-weight: bold; }")
        append(".line { border-top: 1px dashed #000; margin: 6px 0; }")
        append(".row { display: flex; justify-content: space-between; margin: 3px 0; }")
        append(".title { font-size: 16px; font-weight: bold; }")
        append("</style></head><body>")
        append("<div class='center title'>আমার দোকান</div>")
        append("<div class='center'>টেলিকম সাইবার ক্যাফে ও জেরক্স</div>")
        append("<div class='center'>ফোন: 7718489405</div>")
        append("<div class='line'></div>")
        append("<div class='row'><span>বিল নং: $billNumber</span><span>তারিখ: $dateStr</span></div>")
        append("<div class='row'><span>গ্রাহক: $customerName</span></div>")
        append("<div class='row'><span>পেমেন্ট: ${if (paymentType == "CASH") "নগদ (Cash)" else "বাকি (Credit)"}</span></div>")
        append("<div class='line'></div>")
        append("<div class='row bold'><span>বিবরণ</span><span>পরিমাণ × দর</span><span>মোট</span></div>")
        append("<div class='line'></div>")
        items.forEach {
            append("<div class='row'>")
            append("<span>${it.serviceName}</span>")
            append("<span>${it.quantity.toInt()} × ₹${it.unitRate.toInt()}</span>")
            append("<span>₹${it.lineTotal.toInt()}</span>")
            append("</div>")
        }
        append("<div class='line'></div>")
        if (discount > 0.0) {
            append("<div class='row'><span>সাবটোটাল:</span><span>₹${subtotal.toInt()}</span></div>")
            append("<div class='row'><span>ছাড় (Discount):</span><span>-₹${discount.toInt()}</span></div>")
        }
        append("<div class='row bold' style='font-size: 14px;'><span>সর্বমোট বিল:</span><span>₹${grandTotal.toInt()}</span></div>")
        append("<div class='line'></div>")
        append("<div class='center'>ধন্যবাদ! আবার আসবেন।</div>")
        append("</body></html>")
    }.toString()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printAdapter = webView.createPrintDocumentAdapter("Amar_Dokan_$billNumber")
            printManager.print("Amar_Dokan_Receipt", printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
}
