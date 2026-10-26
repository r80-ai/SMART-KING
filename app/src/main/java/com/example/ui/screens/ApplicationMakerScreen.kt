package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.print.PrintAttributes
import android.print.PrintManager
import android.speech.RecognizerIntent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.components.SignatureUploadSection
import com.example.ui.components.toBase64Png
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// Application Categories and Letter Types are imported from ApplicationCategoriesData.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationMakerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isBengali by remember { mutableStateOf(true) }

    // Full 21 Categories and Letter Types Repository from ApplicationCategoriesData.kt
    val categories = remember { getApplicationCategories() }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val currentCategory = categories[selectedCategoryIndex]

    var selectedLetterTypeIndex by remember { mutableIntStateOf(0) }
    val currentLetterType = currentCategory.letterTypes.getOrElse(selectedLetterTypeIndex) { currentCategory.letterTypes.first() }

    // Dynamic Form States
    var applicantName by remember { mutableStateOf("") }
    val currentDate = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(currentDate) }
    var recipientTitle by remember(selectedCategoryIndex, isBengali) {
        mutableStateOf(if (isBengali) currentCategory.defaultRecipientBn else currentCategory.defaultRecipientEn)
    }
    var officeName by remember { mutableStateOf("") }
    var applicantAddress by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var dynamicField1 by remember { mutableStateOf("") }
    var dynamicField2 by remember { mutableStateOf("") }

    // Signature State
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Voice typing launcher
    var activeVoiceTarget by remember { mutableStateOf<String?>(null) }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                when (activeVoiceTarget) {
                    "name" -> applicantName = spokenText
                    "office" -> officeName = spokenText
                    "address" -> applicantAddress = spokenText
                    "place" -> place = spokenText
                    "field1" -> dynamicField1 = spokenText
                    "field2" -> dynamicField2 = spokenText
                }
            }
        }
    }

    fun startVoiceInput(target: String) {
        activeVoiceTarget = target
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isBengali) "bn-IN" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (isBengali) "বলুন, স্বয়ংক্রিয়ভাবে টাইপ হবে..." else "Speak now...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, if (isBengali) "ভয়েস ইনপুট সমর্থিত নয়" else "Voice input not supported", Toast.LENGTH_SHORT).show()
        }
    }

    // Dynamic Live Preview Generator
    val generatedLetter = remember(
        isBengali, currentCategory, currentLetterType,
        applicantName, date, recipientTitle, officeName, applicantAddress, place, dynamicField1, dynamicField2, signatureBitmap
    ) {
        val sigLine = if (signatureBitmap != null) "[স্বাক্ষর সংযুক্ত]" else "................................"
        if (isBengali) {
            val body = currentLetterType.bodyTemplateBn(
                applicantName, applicantAddress, officeName, dynamicField1, dynamicField2
            )
            """
প্রতি,
${recipientTitle.ifBlank { currentCategory.defaultRecipientBn }}
${officeName.ifBlank { "...................................." }}

বিষয়: ${currentLetterType.subjectBn}

মহাশয়/মহাশয়া,
$body

অতএব, মহাশয়ের নিকট বিনীত প্রার্থনা, উক্ত বিষয়টি বিবেচনা করে প্রয়োজনীয় ব্যবস্থা গ্রহণ করলে আমি বিশেষভাবে উপকৃত হব।

তারিখ: $date
স্থান: ${place.ifBlank { "................" }}

ধন্যবাদান্তে,
$sigLine
${applicantName.ifBlank { "................" }}
${if (applicantAddress.isNotBlank()) "ঠিকানা: $applicantAddress\n" else ""}
            """.trimIndent()
        } else {
            val body = currentLetterType.bodyTemplateEn(
                applicantName, applicantAddress, officeName, dynamicField1, dynamicField2
            )
            """
To,
${recipientTitle.ifBlank { currentCategory.defaultRecipientEn }}
${officeName.ifBlank { "...................................." }}

Subject: ${currentLetterType.subjectEn}

Respected Sir/Madam,
$body

Therefore, I request you to kindly take necessary action in this regard at your earliest convenience.

Date: $date
Place: ${place.ifBlank { "................" }}

Yours faithfully,
$sigLine
${applicantName.ifBlank { "................" }}
${if (applicantAddress.isNotBlank()) "Address: $applicantAddress\n" else ""}
            """.trimIndent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBengali) "দরখাস্ত তৈরি" else "Application Maker",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isBengali) "সহজেই তৈরি করুন অফিশিয়াল দরখাস্ত" else "Create formal applications instantly",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // 1. Language Switcher (বাংলা / English)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBengali) "ভাষা নির্বাচন করুন:" else "Select Language:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isBengali = true
                            recipientTitle = currentCategory.defaultRecipientBn
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBengali) Color(0xFF2563EB) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("বাংলা", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            isBengali = false
                            recipientTitle = currentCategory.defaultRecipientEn
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isBengali) Color(0xFF2563EB) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("English", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Category Selector (বিভাগ নির্বাচন)
            Text(
                text = if (isBengali) "১. বিভাগ বা দপ্তর নির্বাচন (Category):" else "1. Select Category:",
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            var categoryMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .clickable { categoryMenuExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) currentCategory.titleBn else currentCategory.titleEn,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text("▼", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xFF1E293B))
                ) {
                    categories.forEachIndexed { index, cat ->
                        DropdownMenuItem(
                            text = { Text(if (isBengali) cat.titleBn else cat.titleEn, color = Color.White) },
                            onClick = {
                                selectedCategoryIndex = index
                                selectedLetterTypeIndex = 0
                                recipientTitle = if (isBengali) cat.defaultRecipientBn else cat.defaultRecipientEn
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Letter Type Selector (দরখাস্তের ধরন)
            Text(
                text = if (isBengali) "২. দরখাস্তের ধরন নির্বাচন (Letter Type):" else "2. Select Letter Type:",
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            var typeMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .clickable { typeMenuExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) currentLetterType.titleBn else currentLetterType.titleEn,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text("▼", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                DropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xFF1E293B))
                ) {
                    currentCategory.letterTypes.forEachIndexed { index, lType ->
                        DropdownMenuItem(
                            text = { Text(if (isBengali) lType.titleBn else lType.titleEn, color = Color.White) },
                            onClick = {
                                selectedLetterTypeIndex = index
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Input Fields with Voice Typing
            Text(
                text = if (isBengali) "৩. প্রয়োজনীয় তথ্য পূরণ করুন:" else "3. Fill in Details:",
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Applicant Name
            OutlinedTextField(
                value = applicantName,
                onValueChange = { applicantName = it },
                label = { Text(if (isBengali) "আবেদনকারীর নাম (Applicant Name)" else "Applicant Name", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("name") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Date
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text(if (isBengali) "তারিখ (Date)" else "Date", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Recipient Title
            OutlinedTextField(
                value = recipientTitle,
                onValueChange = { recipientTitle = it },
                label = { Text(if (isBengali) "প্রাপকের পদ (Recipient Title)" else "Recipient Title", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Office / Branch Name
            OutlinedTextField(
                value = officeName,
                onValueChange = { officeName = it },
                label = { Text(if (isBengali) "দপ্তর বা শাখার নাম ও ঠিকানা (Office Name & Branch)" else "Office Name & Address", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("office") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Applicant Address
            OutlinedTextField(
                value = applicantAddress,
                onValueChange = { applicantAddress = it },
                label = { Text(if (isBengali) "আবেদনকারীর ঠিকানা (Applicant Address)" else "Applicant Address", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("address") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Place
            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text(if (isBengali) "স্থান (Place)" else "Place", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("place") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Dynamic Context Field 1
            if (currentLetterType.field1LabelBn.isNotBlank()) {
                OutlinedTextField(
                    value = dynamicField1,
                    onValueChange = { dynamicField1 = it },
                    label = { Text(if (isBengali) currentLetterType.field1LabelBn else currentLetterType.field1LabelEn, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("field1") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Dynamic Context Field 2
            if (currentLetterType.field2LabelBn.isNotBlank()) {
                OutlinedTextField(
                    value = dynamicField2,
                    onValueChange = { dynamicField2 = it },
                    label = { Text(if (isBengali) currentLetterType.field2LabelBn else currentLetterType.field2LabelEn, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("field2") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Signature Upload Section (Optional with ONNX AI BG Remove & Original/Manual)
            SignatureUploadSection(
                signatureBitmap = signatureBitmap,
                onSignatureChanged = { signatureBitmap = it },
                title = if (isBengali) "স্বাক্ষর আপলোড (Upload Signature - ঐচ্ছিক)" else "Upload Signature (Optional)",
                subtitle = if (isBengali) "স্বাক্ষর আপলোড করলে প্রিন্ট কপিতে সই বসে যাবে। না দিলে ডট-লাইনে (................) হাতে সই করতে পারবেন।" else "Uploaded signature will be placed in the printout. If empty, a dot-line will appear for manual signing."
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons: Print A4 / Save PDF, Copy, Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A4 Print / PDF
                Button(
                    onClick = {
                        val webView = WebView(context)
                        val bodyText = if (isBengali) {
                            currentLetterType.bodyTemplateBn(applicantName, applicantAddress, officeName, dynamicField1, dynamicField2)
                        } else {
                            currentLetterType.bodyTemplateEn(applicantName, applicantAddress, officeName, dynamicField1, dynamicField2)
                        }

                        val signatureHtml = if (signatureBitmap != null) {
                            """<img src="data:image/png;base64,${signatureBitmap!!.toBase64Png()}" class="sig-img" alt="Signature" />"""
                        } else {
                            """<div class="sig-dots">................................</div>"""
                        }

                        val htmlContent = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <style>
                                    @page {
                                        size: A4 portrait;
                                        margin: 24mm 20mm 20mm 24mm;
                                    }
                                    body {
                                        font-family: 'Noto Sans Bengali', sans-serif, 'Segoe UI', Arial, sans-serif;
                                        font-size: 12.5pt;
                                        line-height: 1.7;
                                        color: #111;
                                        padding: 0;
                                        margin: 0;
                                    }
                                    .recipient {
                                        margin-bottom: 16px;
                                    }
                                    .subject {
                                        font-weight: bold;
                                        margin: 16px 0;
                                        text-decoration: underline;
                                    }
                                    .salutation {
                                        margin-bottom: 8px;
                                        font-weight: bold;
                                    }
                                    .body-text {
                                        text-align: justify;
                                        margin-bottom: 16px;
                                    }
                                    .closing-text {
                                        text-align: justify;
                                        margin-bottom: 24px;
                                    }
                                    .footer-table {
                                        width: 100%;
                                        margin-top: 24px;
                                        border-collapse: collapse;
                                    }
                                    .footer-left {
                                        width: 50%;
                                        vertical-align: bottom;
                                        font-size: 11pt;
                                        line-height: 1.6;
                                    }
                                    .footer-right {
                                        width: 50%;
                                        vertical-align: bottom;
                                        text-align: right;
                                        font-size: 11.5pt;
                                    }
                                    .sig-container {
                                        display: inline-block;
                                        text-align: right;
                                        min-width: 140px;
                                        margin: 4px 0;
                                    }
                                    .sig-img {
                                        max-height: 50px;
                                        max-width: 150px;
                                        object-fit: contain;
                                        display: block;
                                        margin-left: auto;
                                        margin-right: 0;
                                    }
                                    .sig-dots {
                                        letter-spacing: 2px;
                                        color: #444;
                                        font-size: 11pt;
                                        margin: 8px 0 4px 0;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="recipient">
                                    <div>${if (isBengali) "প্রতি," else "To,"}</div>
                                    <div>${recipientTitle.ifBlank { if (isBengali) currentCategory.defaultRecipientBn else currentCategory.defaultRecipientEn }}</div>
                                    <div>${officeName.ifBlank { "...................................." }}</div>
                                </div>

                                <div class="subject">
                                    ${if (isBengali) "বিষয়: " + currentLetterType.subjectBn else "Subject: " + currentLetterType.subjectEn}
                                </div>

                                <div class="salutation">${if (isBengali) "মহাশয়/মহাশয়া," else "Respected Sir/Madam,"}</div>
                                
                                <div class="body-text">$bodyText</div>

                                <div class="closing-text">
                                    ${if (isBengali) "অতএব, মহাশয়ের নিকট বিনীত প্রার্থনা, উক্ত বিষয়টি বিবেচনা করে প্রয়োজনীয় ব্যবস্থা গ্রহণ করলে আমি বিশেষভাবে উপকৃত হব।" else "Therefore, I request you to kindly take necessary action in this regard at your earliest convenience."}
                                </div>

                                <table class="footer-table">
                                    <tr>
                                        <td class="footer-left">
                                            <div>${if (isBengali) "তারিখ: " + date else "Date: " + date}</div>
                                            <div>${if (isBengali) "স্থান: " + place.ifBlank { "................" } else "Place: " + place.ifBlank { "................" }}</div>
                                        </td>
                                        <td class="footer-right">
                                            <div>${if (isBengali) "ধন্যবাদান্তে," else "Yours faithfully,"}</div>
                                            <div class="sig-container">
                                                $signatureHtml
                                            </div>
                                            <div style="font-weight: bold; margin-top: 2px;">${applicantName.ifBlank { "................" }}</div>
                                            ${if (applicantAddress.isNotBlank()) "<div style='font-size: 10.5pt; color: #444; margin-top: 2px;'>" + (if (isBengali) "ঠিকানা: " else "Address: ") + applicantAddress + "</div>" else ""}
                                        </td>
                                    </tr>
                                </table>
                            </body>
                            </html>
                        """.trimIndent()

                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                val printAdapter = webView.createPrintDocumentAdapter("Application_Letter")
                                val printAttributes = PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .build()
                                printManager.print("Application_Letter", printAdapter, printAttributes)
                            }
                        }
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("A4 Print", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Copy Letter
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Application Letter", generatedLetter))
                        Toast.makeText(context, if (isBengali) "লেখাটি কপি করা হয়েছে" else "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Clear
                Button(
                    onClick = {
                        applicantName = ""
                        officeName = ""
                        applicantAddress = ""
                        place = ""
                        dynamicField1 = ""
                        dynamicField2 = ""
                        signatureBitmap = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // LIVE PREVIEW Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE PREVIEW (লাইভ প্রিভিউ)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (isBengali) "স্বয়ংক্রিয়ভাবে তৈরি দরখাস্ত" else "Real-time formatted letter",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    val bodyText = if (isBengali) {
                        currentLetterType.bodyTemplateBn(applicantName, applicantAddress, officeName, dynamicField1, dynamicField2)
                    } else {
                        currentLetterType.bodyTemplateEn(applicantName, applicantAddress, officeName, dynamicField1, dynamicField2)
                    }

                    Text(
                        text = if (isBengali) "প্রতি,\n${recipientTitle.ifBlank { currentCategory.defaultRecipientBn }}\n${officeName.ifBlank { "...................................." }}"
                               else "To,\n${recipientTitle.ifBlank { currentCategory.defaultRecipientEn }}\n${officeName.ifBlank { "...................................." }}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isBengali) "বিষয়: ${currentLetterType.subjectBn}" else "Subject: ${currentLetterType.subjectEn}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isBengali) "মহাশয়/মহাশয়া," else "Respected Sir/Madam,",
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = bodyText,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isBengali) "অতএব, মহাশয়ের নিকট বিনীত প্রার্থনা, উক্ত বিষয়টি বিবেচনা করে প্রয়োজনীয় ব্যবস্থা গ্রহণ করলে আমি বিশেষভাবে উপকৃত হব।"
                               else "Therefore, I request you to kindly take necessary action in this regard at your earliest convenience.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("তারিখ: $date", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                            Text("স্থান: ${place.ifBlank { "................" }}", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isBengali) "ধন্যবাদান্তে," else "Yours faithfully,",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (signatureBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Image(
                                        bitmap = signatureBitmap!!.asImageBitmap(),
                                        contentDescription = "Signature",
                                        modifier = Modifier
                                            .height(40.dp)
                                            .widthIn(max = 140.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "................................",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = applicantName.ifBlank { "................" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            if (applicantAddress.isNotBlank()) {
                                Text(
                                    text = applicantAddress,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
