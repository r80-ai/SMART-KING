package com.example.ui.screens

import android.content.ContentValues
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
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
import com.example.ui.components.SignatureUploadSection
import com.example.ui.components.toBase64Png
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnapurnaDeclarationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isSavingPdf by remember { mutableStateOf(false) }

    // PDF Save & Picker States
    var pendingPdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var savedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var savedPdfSizeKb by remember { mutableIntStateOf(0) }
    var savedPdfLocationText by remember { mutableStateOf("") }
    var showSaveSuccessDialog by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && pendingPdfBytes != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(pendingPdfBytes!!)
                        os.flush()
                    }
                    val sizeKb = pendingPdfBytes!!.size / 1024
                    val location = getHumanReadablePath(context, uri)
                    withContext(Dispatchers.Main) {
                        savedPdfUri = uri
                        savedPdfSizeKb = sizeKb
                        savedPdfLocationText = location
                        showSaveSuccessDialog = true
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error saving file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // 1. Form Inputs State
    val currentDate = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }
    var applicantName by remember { mutableStateOf("") }
    var guardianName by remember { mutableStateOf("") }
    var guardianType by remember { mutableStateOf("স্বামী") } // স্বামী / পিতা
    var address by remember { mutableStateOf("") }
    var postOffice by remember { mutableStateOf("") }
    var policeStation by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var panchayatArea by remember { mutableStateOf("") }

    // Family & Property Details
    var guardianProfession by remember { mutableStateOf("") }
    var monthlyIncome by remember { mutableStateOf("") }
    var houseDescription by remember { mutableStateOf("") }
    var landAmount by remember { mutableStateOf("") }

    // 9 Status Checkbox Options
    val statusOptions = remember {
        listOf(
            "For further verification",
            "Under Process",
            "For further verification - ASDD",
            "For further verification - Government Employee",
            "Not applying for AY",
            "For further verification – Pays income tax",
            "For further verification – More than 3 rooms in Urban Area",
            "Rejected & Maybe Further Verified",
            "Other (অন্যান্য কারণ)"
        )
    }
    var selectedOptionIndex by remember { mutableIntStateOf(0) }
    var customOptionText by remember { mutableStateOf("") }

    // Footer details
    var formDate by remember { mutableStateOf(currentDate) }
    var mobileNo by remember { mutableStateOf("") }
    var idNo by remember { mutableStateOf("") }
    var aadhaarNo by remember { mutableStateOf("") }
    var voterNo by remember { mutableStateOf("") }

    // Signature State
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Voice recognition target and launcher
    var activeVoiceTarget by remember { mutableStateOf("") }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                when (activeVoiceTarget) {
                    "applicantName" -> applicantName = spokenText
                    "guardianName" -> guardianName = spokenText
                    "address" -> address = spokenText
                    "postOffice" -> postOffice = spokenText
                    "policeStation" -> policeStation = spokenText
                    "pinCode" -> pinCode = spokenText
                    "panchayatArea" -> panchayatArea = spokenText
                    "guardianProfession" -> guardianProfession = spokenText
                    "monthlyIncome" -> monthlyIncome = spokenText
                    "houseDescription" -> houseDescription = spokenText
                    "landAmount" -> landAmount = spokenText
                    "customOptionText" -> {
                        customOptionText = spokenText
                        selectedOptionIndex = 8
                    }
                    "mobileNo" -> mobileNo = spokenText
                    "idNo" -> idNo = spokenText
                    "aadhaarNo" -> aadhaarNo = spokenText
                    "voterNo" -> voterNo = spokenText
                }
            }
        }
    }

    fun startVoiceInput(target: String) {
        activeVoiceTarget = target
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "বলুন, স্বয়ংক্রিয়ভাবে লেখা হবে...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ভয়েস ইনপুট সমর্থিত নয়", Toast.LENGTH_SHORT).show()
        }
    }

    // Plain text formatted letter for clipboard
    val plainTextDeclaration = remember(
        applicantName, guardianName, guardianType, address, postOffice, policeStation, pinCode, panchayatArea,
        guardianProfession, monthlyIncome, houseDescription, landAmount, selectedOptionIndex, customOptionText,
        formDate, mobileNo, idNo, aadhaarNo, voterNo, signatureBitmap
    ) {
        val selectedStatusStr = if (selectedOptionIndex == 8) {
            "9. ${customOptionText.ifBlank { "অন্যান্য কারণ" }}"
        } else {
            "${selectedOptionIndex + 1}. ${statusOptions[selectedOptionIndex]}"
        }

        """
স্ব-ঘোষণা পত্র (Self-Declaration)

মহাশয়,
আমি ${applicantName.ifBlank { "................" }} $guardianType ${guardianName.ifBlank { "................" }}
ঠিকানা ${address.ifBlank { "................" }} পোস্ট ${postOffice.ifBlank { "................" }} থানা- ${policeStation.ifBlank { "................" }}
পিন- ${pinCode.ifBlank { "................" }}
${panchayatArea.ifBlank { "................" }} পঞ্চায়েতের স্থানীয় বাসিন্দা।

আমার স্বামী/ পিতা-র কাজ/ পেশার বিবরণ: ${guardianProfession.ifBlank { "................" }}
মাসিক আয়: ${monthlyIncome.ifBlank { "................" }}
গৃহের বিবরণ: ${houseDescription.ifBlank { "................" }}
জমির পরিমান: ${landAmount.ifBlank { "................" }}

মহাশয়, আমি অন্নপূর্ণা টাকা পাওয়ার যোগ্য, কিন্তু আমার অন্নপূর্ণা স্ট্যাটাস নিম্নরূপ দেখাচ্ছে
সঠিক জায়গায় টিক দিন:
[✔] $selectedStatusStr

অতএব, মহাশয় আমি অন্নপূর্ণা টাকা পাওয়ার যোগ্য, তাই আপনার নিকট আমার বিনীত নিবেদন, এই যে, আমাকে অন্নপূর্ণা যোজনার অন্তর্গত সুবিধা টি প্রদান করলে খুব উপকৃত হব। উপরোক্ত তথ্য আমার জ্ঞান ও বিশ্বাস মতে সত্য।

তারিখ: $formDate
ফোন নম্বর: ${mobileNo.ifBlank { "................" }}
আইডি নম্বর: ${idNo.ifBlank { "................" }}
আধার নম্বর: ${aadhaarNo.ifBlank { "................" }}
ভোটার নম্বর: ${voterNo.ifBlank { "................" }}

বিনীত-
${if (signatureBitmap != null) "[স্বাক্ষর সংযুক্ত]" else "................................"}
        """.trimIndent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "অন্নপূর্ণা স্ব-ঘোষণা পত্র",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Self-Declaration Form Generator",
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
            // Form Title Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFD97706).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "স্ব-ঘোষণা পত্র (Self-Declaration)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "অন্নপূর্ণা যোজনার স্ট্যাটাস সংশোধন ও আবেদন ফর্ম",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: Personal & Guardian Details
            Text(
                text = "১. আবেদনকারী ও অভিভাবকের তথ্য:",
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Applicant Name
            OutlinedTextField(
                value = applicantName,
                onValueChange = { applicantName = it },
                label = { Text("আবেদনকারীর নাম (Applicant Name)", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .testTag("input_annapurna_name"),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("applicantName") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD97706),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Guardian Relation Toggle & Guardian Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    FilterChip(
                        selected = guardianType == "স্বামী",
                        onClick = { guardianType = "স্বামী" },
                        label = { Text("স্বামী") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = guardianType == "পিতা",
                        onClick = { guardianType = "পিতা" },
                        label = { Text("পিতা") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    label = { Text("$guardianType-র নাম (Guardian Name)", color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_annapurna_guardian"),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("guardianName") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Address Details
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("ঠিকানা / গ্রাম / পাড়া (Address)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("address") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD97706),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = postOffice,
                    onValueChange = { postOffice = it },
                    label = { Text("পোস্ট (P.O.)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("postOffice") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = policeStation,
                    onValueChange = { policeStation = it },
                    label = { Text("থানা (P.S.)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("policeStation") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = pinCode,
                    onValueChange = { pinCode = it },
                    label = { Text("পিন কোড (PIN Code)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("pinCode") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = panchayatArea,
                    onValueChange = { panchayatArea = it },
                    label = { Text("পঞ্চায়েত / এলাকা", color = Color.Gray) },
                    modifier = Modifier.weight(1.3f),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("panchayatArea") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2: Family & Property Details
            Text(
                text = "২. পরিবার ও সম্পত্তির বিবরণ:",
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = guardianProfession,
                onValueChange = { guardianProfession = it },
                label = { Text("স্বামী/পিতা-র কাজ / পেশার বিবরণ", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("guardianProfession") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD97706),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = monthlyIncome,
                    onValueChange = { monthlyIncome = it },
                    label = { Text("মাসিক আয় (টাকা)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("monthlyIncome") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = landAmount,
                    onValueChange = { landAmount = it },
                    label = { Text("জমির পরিমান", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("landAmount") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            OutlinedTextField(
                value = houseDescription,
                onValueChange = { houseDescription = it },
                label = { Text("গৃহের বিবরণ (যেমন: মাটির বাড়ি / খড়ের ছাউনি / টিন)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("houseDescription") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD97706),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 3: 9 Checkbox Options (Square Box with Green Tick Mark)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "৩. অন্নপূর্ণা স্ট্যাটাস (সঠিক জায়গায় টিক দিন):",
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "মহাশয়, আমি অন্নপূর্ণা টাকা পাওয়ার যোগ্য, কিন্তু আমার অন্নপূর্ণা স্ট্যাটাস নিম্নরূপ দেখাচ্ছে:",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Options 1 through 8
                statusOptions.take(8).forEachIndexed { index, optionText ->
                    val isChecked = selectedOptionIndex == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedOptionIndex = index }
                            .background(if (isChecked) Color(0xFF0F172A) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Square Checkbox with Green Check Icon
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isChecked) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .background(
                                    if (isChecked) Color(0xFF22C55E).copy(alpha = 0.2f) else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Checked",
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "${index + 1}. $optionText",
                            color = if (isChecked) Color.White else Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                // Option 9: Custom editable text
                val isChecked9 = selectedOptionIndex == 8
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedOptionIndex = 8 }
                        .background(if (isChecked9) Color(0xFF0F172A) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (isChecked9) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(
                                if (isChecked9) Color(0xFF22C55E).copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked9) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Checked",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "9.",
                        color = if (isChecked9) Color.White else Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = if (isChecked9) FontWeight.SemiBold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedTextField(
                        value = customOptionText,
                        onValueChange = {
                            customOptionText = it
                            selectedOptionIndex = 8
                        },
                        placeholder = { Text("অন্যান্য কারণ লিখুন (Custom Reason)...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = { startVoiceInput("customOptionText") }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF22C55E),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 4: Contact & Identification Details
            Text(
                text = "৪. যোগাযোগ ও পরিচয়পত্রের নম্বর:",
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formDate,
                    onValueChange = { formDate = it },
                    label = { Text("তারিখ (Date)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = mobileNo,
                    onValueChange = { mobileNo = it },
                    label = { Text("ফোন নম্বর (Mobile)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("mobileNo") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = idNo,
                    onValueChange = { idNo = it },
                    label = { Text("আইডি নম্বর (ID No)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("idNo") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = aadhaarNo,
                    onValueChange = { aadhaarNo = it },
                    label = { Text("আধার নম্বর (Aadhaar)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("aadhaarNo") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            OutlinedTextField(
                value = voterNo,
                onValueChange = { voterNo = it },
                label = { Text("ভোটার নম্বর (Voter ID No)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("voterNo") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD97706),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 5: Unified Reusable Signature Upload Section (AI BG Remove + Original/Manual Toggle)
            SignatureUploadSection(
                signatureBitmap = signatureBitmap,
                onSignatureChanged = { signatureBitmap = it },
                title = "৫. আবেদনকারীর স্বাক্ষর (Signature - ঐচ্ছিক):",
                subtitle = "স্বাক্ষর আপলোড করলে প্রিন্ট কপিতে সই বসে যাবে। না দিলে ডট-লাইনে (................) হাতে সই করতে পারবেন।"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ACTION BUTTONS: Strict 180-190 KB PDF Save, A4 Print, Copy, Clear
            // 1. Prominent Primary Button: Save PDF (180 KB - 190 KB)
            Button(
                onClick = {
                    if (isSavingPdf) return@Button
                    isSavingPdf = true
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val bitmap = buildAnnapurnaBitmap(
                                applicantName = applicantName,
                                guardianName = guardianName,
                                guardianType = guardianType,
                                address = address,
                                postOffice = postOffice,
                                policeStation = policeStation,
                                pinCode = pinCode,
                                panchayatArea = panchayatArea,
                                guardianProfession = guardianProfession,
                                monthlyIncome = monthlyIncome,
                                houseDescription = houseDescription,
                                landAmount = landAmount,
                                statusOptions = statusOptions,
                                selectedOptionIndex = selectedOptionIndex,
                                customOptionText = customOptionText,
                                formDate = formDate,
                                mobileNo = mobileNo,
                                idNo = idNo,
                                aadhaarNo = aadhaarNo,
                                voterNo = voterNo,
                                signatureBitmap = signatureBitmap
                            )
                            val pdfBytes = generateStrictSizedPdf(bitmap, targetKb = 185)
                            pendingPdfBytes = pdfBytes

                            withContext(Dispatchers.Main) {
                                isSavingPdf = false
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                createDocumentLauncher.launch("Annapurna_Declaration_$timestamp.pdf")
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isSavingPdf = false
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_annapurna_save_pdf"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(8.dp),
                enabled = !isSavingPdf
            ) {
                if (isSavingPdf) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF তৈরি হচ্ছে (Strict 180-190 KB)...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save PDF (180 KB - 190 KB)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Secondary Row: A4 Print, Copy, Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A4 Print
                Button(
                    onClick = {
                        val webView = WebView(context)
                        val htmlContent = buildAnnapurnaHtml(
                            applicantName = applicantName,
                            guardianName = guardianName,
                            guardianType = guardianType,
                            address = address,
                            postOffice = postOffice,
                            policeStation = policeStation,
                            pinCode = pinCode,
                            panchayatArea = panchayatArea,
                            guardianProfession = guardianProfession,
                            monthlyIncome = monthlyIncome,
                            houseDescription = houseDescription,
                            landAmount = landAmount,
                            statusOptions = statusOptions,
                            selectedOptionIndex = selectedOptionIndex,
                            customOptionText = customOptionText,
                            formDate = formDate,
                            mobileNo = mobileNo,
                            idNo = idNo,
                            aadhaarNo = aadhaarNo,
                            voterNo = voterNo,
                            signatureBitmap = signatureBitmap
                        )

                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                val printAdapter = webView.createPrintDocumentAdapter("Annapurna_Declaration")
                                val printAttributes = PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .build()
                                printManager.print("Annapurna_Declaration", printAdapter, printAttributes)
                            }
                        }
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("btn_annapurna_print"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("A4 Print", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Copy Text
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Annapurna Declaration", plainTextDeclaration))
                        Toast.makeText(context, "ফর্মের লেখা কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
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
                        guardianName = ""
                        address = ""
                        postOffice = ""
                        policeStation = ""
                        pinCode = ""
                        panchayatArea = ""
                        guardianProfession = ""
                        monthlyIncome = ""
                        houseDescription = ""
                        landAmount = ""
                        selectedOptionIndex = 0
                        customOptionText = ""
                        mobileNo = ""
                        idNo = ""
                        aadhaarNo = ""
                        voterNo = ""
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

            Spacer(modifier = Modifier.height(24.dp))

            // LIVE PREVIEW CARD
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
                    text = "A4 অফিশিয়াল ফর্ম প্রিভিউ",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // White Paper preview container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "স্ব-ঘোষণা পত্র (Self-Declaration)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("মহাশয়,", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "আমি ${applicantName.ifBlank { "................" }} $guardianType ${guardianName.ifBlank { "................" }}",
                        color = Color.Black,
                        fontSize = 12.5.sp,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "ঠিকানা ${address.ifBlank { "................" }} পোস্ট ${postOffice.ifBlank { "................" }} থানা- ${policeStation.ifBlank { "................" }}",
                        color = Color.Black,
                        fontSize = 12.5.sp,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "পিন- ${pinCode.ifBlank { "................" }}",
                        color = Color.Black,
                        fontSize = 12.5.sp,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "${panchayatArea.ifBlank { "................" }} পঞ্চায়েতের স্থানীয় বাসিন্দা।",
                        color = Color.Black,
                        fontSize = 12.5.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "আমার স্বামী/ পিতা-র কাজ/ পেশার বিবরণ: ${guardianProfession.ifBlank { "................" }}",
                        color = Color.Black,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "মাসিক আয়: ${monthlyIncome.ifBlank { "................" }}",
                        color = Color.Black,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "গৃহের বিবরণ: ${houseDescription.ifBlank { "................" }}",
                        color = Color.Black,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "জমির পরিমান: ${landAmount.ifBlank { "................" }}",
                        color = Color.Black,
                        fontSize = 12.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "মহাশয়, আমি অন্নপূর্ণা টাকা পাওয়ার যোগ্য, কিন্তু আমার অন্নপূর্ণা স্ট্যাটাস নিম্নরূপ দেখাচ্ছে",
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("সঠিক জায়গায় টিক দিন:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    statusOptions.forEachIndexed { idx, opt ->
                        val isChecked = selectedOptionIndex == idx
                        val label = if (idx == 8) {
                            "9. ${customOptionText.ifBlank { "................................................" }}"
                        } else {
                            "${idx + 1}. $opt"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 1.5.dp)
                        ) {
                            Text(
                                text = if (isChecked) "[✔] " else "[  ] ",
                                color = if (isChecked) Color(0xFF15803D) else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = label,
                                color = Color.Black,
                                fontSize = 11.5.sp,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "অতএব, মহাশয় আমি অন্নপূর্ণা টাকা পাওয়ার যোগ্য, তাই আপনার নিকট আমার বিনীত নিবেদন, এই যে, আমাকে অন্নপূর্ণা যোজনার অন্তর্গত সুবিধা টি প্রদান করলে খুব উপকৃত হব। উপরোক্ত তথ্য আমার জ্ঞান ও বিশ্বাস মতে সত্য।",
                        color = Color.Black,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text("তারিখ: $formDate", color = Color.Black, fontSize = 11.sp)
                            Text("ফোন নম্বর: ${mobileNo.ifBlank { "................" }}", color = Color.Black, fontSize = 11.sp)
                            Text("আইডি নম্বর: ${idNo.ifBlank { "................" }}", color = Color.Black, fontSize = 11.sp)
                            Text("আধার নম্বর: ${aadhaarNo.ifBlank { "................" }}", color = Color.Black, fontSize = 11.sp)
                            Text("ভোটার নম্বর: ${voterNo.ifBlank { "................" }}", color = Color.Black, fontSize = 11.sp)
                        }

                        Column(
                            modifier = Modifier.weight(0.9f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("বিনীত-", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            val sig = signatureBitmap
                            if (sig != null) {
                                Image(
                                    bitmap = sig.asImageBitmap(),
                                    contentDescription = "Signature",
                                    modifier = Modifier.height(40.dp).padding(2.dp)
                                )
                            } else {
                                Text("....................................", color = Color.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Save Confirmation Dialog with Exact Location & Open PDF
    if (showSaveSuccessDialog && savedPdfUri != null) {
        AlertDialog(
            onDismissRequest = { showSaveSuccessDialog = false },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFE2E8F0),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF22C55E).copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "File Saved Successfully! ($savedPdfSizeKb KB)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "অন্নপূর্ণা স্ব-ঘোষণা পত্রটি ১৮০ KB - ১৯০ KB সাইজ বজায় রেখে আপনার নির্বাচিত ফোল্ডারে সফলভাবে সেভ হয়েছে।",
                        fontSize = 12.5.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 18.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "সংরক্ষিত অবস্থান (Location):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF60A5FA)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = savedPdfLocationText,
                                fontSize = 12.sp,
                                color = Color(0xFFF1F5F9),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openPdf(context, savedPdfUri!!)
                        showSaveSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSaveSuccessDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done / OK")
                }
            }
        )
    }
}

// HTML Generator for pixel-perfect A4 Official Print
private fun buildAnnapurnaHtml(
    applicantName: String,
    guardianName: String,
    guardianType: String,
    address: String,
    postOffice: String,
    policeStation: String,
    pinCode: String,
    panchayatArea: String,
    guardianProfession: String,
    monthlyIncome: String,
    houseDescription: String,
    landAmount: String,
    statusOptions: List<String>,
    selectedOptionIndex: Int,
    customOptionText: String,
    formDate: String,
    mobileNo: String,
    idNo: String,
    aadhaarNo: String,
    voterNo: String,
    signatureBitmap: Bitmap?
): String {
    val signatureHtml = if (signatureBitmap != null) {
        """<img src="data:image/png;base64,${signatureBitmap.toBase64Png()}" class="sig-img" alt="Signature" />"""
    } else {
        """................................................"""
    }

    val optionsHtml = buildString {
        statusOptions.forEachIndexed { idx, opt ->
            val isChecked = selectedOptionIndex == idx
            val boxContent = if (isChecked) "✔" else "&nbsp;"
            val optText = if (idx == 8) {
                "9. ${customOptionText.ifBlank { "................................................" }}"
            } else {
                "${idx + 1}. $opt"
            }
            append("""
                <tr>
                    <td style="width: 28px; vertical-align: middle;">
                        <div class="box ${if (isChecked) "checked" else ""}">$boxContent</div>
                    </td>
                    <td style="vertical-align: middle; font-size: 10pt; ${if (isChecked) "font-weight: bold;" else ""}">
                        $optText
                    </td>
                </tr>
            """.trimIndent())
        }
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Annapurna Self-Declaration</title>
            <style>
                @page {
                    size: A4 portrait;
                    margin: 10mm 15mm 10mm 15mm;
                }
                body {
                    font-family: 'Noto Sans Bengali', sans-serif, 'Segoe UI', Arial, sans-serif;
                    font-size: 10.5pt;
                    line-height: 1.5;
                    color: #000;
                    margin: 0;
                    padding: 0;
                }
                .title {
                    text-align: center;
                    font-size: 14pt;
                    font-weight: bold;
                    text-decoration: underline;
                    margin-bottom: 10px;
                }
                .salutation {
                    font-weight: bold;
                    margin-bottom: 4px;
                }
                .line {
                    margin-bottom: 3px;
                    text-align: justify;
                }
                .bold-val {
                    font-weight: bold;
                    text-decoration: underline;
                }
                .box-tag {
                    display: inline-block;
                    border: 1.5px solid #000;
                    padding: 1px 7px;
                    font-weight: bold;
                    font-size: 9.5pt;
                    margin: 4px 0;
                }
                .options-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 3px 0 6px 0;
                }
                .options-table td {
                    padding: 1.8px 0;
                }
                .box {
                    width: 14px;
                    height: 14px;
                    border: 1.5px solid #000;
                    text-align: center;
                    line-height: 12px;
                    font-size: 10pt;
                    font-weight: bold;
                }
                .box.checked {
                    background-color: #f0fdf4;
                }
                .closing {
                    text-align: justify;
                    margin: 6px 0 10px 0;
                    font-size: 10pt;
                    line-height: 1.45;
                }
                .footer-table {
                    width: 100%;
                    margin-top: 8px;
                    border-collapse: collapse;
                }
                .footer-table td {
                    vertical-align: bottom;
                }
                .sig-img {
                    max-height: 44px;
                    max-width: 130px;
                    object-fit: contain;
                    display: block;
                    margin: 0 auto;
                }
            </style>
        </head>
        <body>
            <div class="title">স্ব-ঘোষণা পত্র (Self-Declaration)</div>
            
            <div class="salutation">মহাশয়,</div>
            
            <div class="line">
                আমি <span class="bold-val">${applicantName.ifBlank { "...................................." }}</span> 
                $guardianType <span class="bold-val">${guardianName.ifBlank { "...................................." }}</span>
            </div>
            
            <div class="line">
                ঠিকানা <span class="bold-val">${address.ifBlank { "...................................." }}</span> 
                পোস্ট <span class="bold-val">${postOffice.ifBlank { "...................." }}</span> 
                থানা- <span class="bold-val">${policeStation.ifBlank { "...................." }}</span>
            </div>
            
            <div class="line">
                পিন- <span class="bold-val">${pinCode.ifBlank { "............" }}</span>
            </div>
            
            <div class="line">
                <span class="bold-val">${panchayatArea.ifBlank { "...................................." }}</span> পঞ্চায়েতের স্থানীয় বাসিন্দা।
            </div>
            
            <div style="margin: 5px 0;">
                <div>আমার স্বামী/ পিতা-র কাজ/ পেশার বিবরণ: <span class="bold-val">${guardianProfession.ifBlank { "...................................." }}</span></div>
                <div>মাসিক আয়: <span class="bold-val">${monthlyIncome.ifBlank { "...................." }}</span></div>
                <div>গৃহের বিবরণ: <span class="bold-val">${houseDescription.ifBlank { "...................................." }}</span></div>
                <div>জমির পরিমান: <span class="bold-val">${landAmount.ifBlank { "...................." }}</span></div>
            </div>
            
            <div style="margin-top: 6px; font-weight: 500;">
                মহাশয়, আমি অন্নপূর্ণা টাকা পাওয়ার যোগ্য, কিন্তু আমার অন্নপূর্ণা স্ট্যাটাস নিম্নরূপ দেখাচ্ছে
            </div>
            
            <div class="box-tag">সঠিক জায়গায় টিক দিন:</div>
            
            <table class="options-table">
                $optionsHtml
            </table>
            
            <div class="closing">
                অতএব, মহাশয় আমি অন্নপূর্ণা টাকা পাওয়ার যোগ্য, তাই আপনার নিকট আমার বিনীত নিবেদন, এই যে, আমাকে অন্নপূর্ণা যোজনার অন্তর্গত সুবিধা টি প্রদান করলে খুব উপকৃত হব। উপরোক্ত তথ্য আমার জ্ঞান ও বিশ্বাস মতে সত্য।
            </div>
            
            <table class="footer-table">
                <tr>
                    <td style="width: 60%; font-size: 10pt; line-height: 1.55;">
                        <div>তারিখ: <span class="bold-val">$formDate</span></div>
                        <div>ফোন নম্বর: <span class="bold-val">${mobileNo.ifBlank { "...................." }}</span></div>
                        <div>আইডি নম্বর: <span class="bold-val">${idNo.ifBlank { "...................." }}</span></div>
                        <div>আধার নম্বর: <span class="bold-val">${aadhaarNo.ifBlank { "...................." }}</span></div>
                        <div>ভোটার নম্বর: <span class="bold-val">${voterNo.ifBlank { "...................." }}</span></div>
                    </td>
                    <td style="width: 40%; text-align: center; vertical-align: bottom;">
                        <div style="font-weight: bold; margin-bottom: 6px;">বিনীত-</div>
                        $signatureHtml
                    </td>
                </tr>
            </table>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Renders the Annapurna Self-Declaration form directly to a crisp A4 Bitmap (1240 x 1754 px)
 */
private fun buildAnnapurnaBitmap(
    applicantName: String,
    guardianName: String,
    guardianType: String,
    address: String,
    postOffice: String,
    policeStation: String,
    pinCode: String,
    panchayatArea: String,
    guardianProfession: String,
    monthlyIncome: String,
    houseDescription: String,
    landAmount: String,
    statusOptions: List<String>,
    selectedOptionIndex: Int,
    customOptionText: String,
    formDate: String,
    mobileNo: String,
    idNo: String,
    aadhaarNo: String,
    voterNo: String,
    signatureBitmap: Bitmap?
): Bitmap {
    val width = 1240
    val height = 1754
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val titlePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 33f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val regularPaint = TextPaint().apply {
        color = android.graphics.Color.BLACK
        textSize = 21f
        isAntiAlias = true
        typeface = Typeface.DEFAULT
    }
    val borderPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    val checkPaint = Paint().apply {
        color = android.graphics.Color.rgb(21, 128, 61)
        textSize = 20f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val marginX = 75f
    val contentWidth = (width - 150).toInt()
    var currentY = 75f

    // 1. Header Title
    canvas.drawText("স্ব-ঘোষণা পত্র (Self-Declaration)", width / 2f, currentY, titlePaint)
    canvas.drawLine(width / 2f - 240f, currentY + 8f, width / 2f + 240f, currentY + 8f, borderPaint)
    currentY += 50f

    // 2. Salutation
    val salutationPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 24f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("মহাশয়,", marginX, currentY, salutationPaint)
    currentY += 40f

    // 3. Line 1: আমি [নাম] স্বামী/পিতা [অভিভাবক]
    val line1Ssb = SpannableStringBuilder()
    line1Ssb.append("আমি ")
    appendBoldUnderline(line1Ssb, applicantName.ifBlank { "...................................." })
    line1Ssb.append(" $guardianType ")
    appendBoldUnderline(line1Ssb, guardianName.ifBlank { "...................................." })
    currentY = drawSpannableText(canvas, line1Ssb, marginX, currentY, contentWidth, regularPaint) + 12f

    // 4. Line 2: ঠিকানা ... পোস্ট ... থানা- ...
    val line2Ssb = SpannableStringBuilder()
    line2Ssb.append("ঠিকানা ")
    appendBoldUnderline(line2Ssb, address.ifBlank { "...................................." })
    line2Ssb.append(" পোস্ট ")
    appendBoldUnderline(line2Ssb, postOffice.ifBlank { "...................." })
    line2Ssb.append(" থানা- ")
    appendBoldUnderline(line2Ssb, policeStation.ifBlank { "...................." })
    currentY = drawSpannableText(canvas, line2Ssb, marginX, currentY, contentWidth, regularPaint) + 12f

    // 5. Line 3: পিন-
    val line3Ssb = SpannableStringBuilder()
    line3Ssb.append("পিন- ")
    appendBoldUnderline(line3Ssb, pinCode.ifBlank { "............" })
    currentY = drawSpannableText(canvas, line3Ssb, marginX, currentY, contentWidth, regularPaint) + 12f

    // 6. Line 4: [এলাকা] পঞ্চায়েতের স্থানীয় বাসিন্দা।
    val line4Ssb = SpannableStringBuilder()
    appendBoldUnderline(line4Ssb, panchayatArea.ifBlank { "...................................." })
    line4Ssb.append(" পঞ্চায়েতের স্থানীয় বাসিন্দা।")
    currentY = drawSpannableText(canvas, line4Ssb, marginX, currentY, contentWidth, regularPaint) + 20f

    // 7. Family & Property Details
    val fam1Ssb = SpannableStringBuilder().apply {
        append("আমার স্বামী/ পিতা-র কাজ/ পেশার বিবরণ: ")
        appendBoldUnderline(this, guardianProfession.ifBlank { "...................................." })
    }
    currentY = drawSpannableText(canvas, fam1Ssb, marginX, currentY, contentWidth, regularPaint) + 10f

    val fam2Ssb = SpannableStringBuilder().apply {
        append("মাসিক আয়: ")
        appendBoldUnderline(this, monthlyIncome.ifBlank { "...................." })
    }
    currentY = drawSpannableText(canvas, fam2Ssb, marginX, currentY, contentWidth, regularPaint) + 10f

    val fam3Ssb = SpannableStringBuilder().apply {
        append("গৃহের বিবরণ: ")
        appendBoldUnderline(this, houseDescription.ifBlank { "...................................." })
    }
    currentY = drawSpannableText(canvas, fam3Ssb, marginX, currentY, contentWidth, regularPaint) + 10f

    val fam4Ssb = SpannableStringBuilder().apply {
        append("জমির পরিমান: ")
        appendBoldUnderline(this, landAmount.ifBlank { "...................." })
    }
    currentY = drawSpannableText(canvas, fam4Ssb, marginX, currentY, contentWidth, regularPaint) + 24f

    // 8. Status intro
    val statusIntroPaint = TextPaint().apply {
        color = android.graphics.Color.BLACK
        textSize = 21f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val introSsb = SpannableStringBuilder("মহাশয়, আমি অন্নপূর্ণা টাকা পাওয়ার যোগ্য, কিন্তু আমার অন্নপূর্ণা স্ট্যাটাস নিম্নরূপ দেখাচ্ছে")
    currentY = drawSpannableText(canvas, introSsb, marginX, currentY, contentWidth, statusIntroPaint) + 14f

    // 9. Box Tag: "সঠিক জায়গায় টিক দিন:"
    val tagText = "সঠিক জায়গায় টিক দিন:"
    val tagPaint = TextPaint().apply {
        color = android.graphics.Color.BLACK
        textSize = 19f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val tagWidth = tagPaint.measureText(tagText) + 24f
    val tagHeight = 30f
    canvas.drawRect(marginX, currentY, marginX + tagWidth, currentY + tagHeight, borderPaint)
    canvas.drawText(tagText, marginX + 12f, currentY + 22f, tagPaint)
    currentY += tagHeight + 16f

    // 10. Status Options
    val optionPaint = TextPaint().apply {
        color = android.graphics.Color.BLACK
        textSize = 19f
        isAntiAlias = true
    }
    val optionBoldPaint = TextPaint().apply {
        color = android.graphics.Color.BLACK
        textSize = 19.5f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    statusOptions.forEachIndexed { idx, opt ->
        val isChecked = selectedOptionIndex == idx
        val boxSize = 22f
        val boxY = currentY - 17f
        canvas.drawRect(marginX, boxY, marginX + boxSize, boxY + boxSize, borderPaint)
        if (isChecked) {
            canvas.drawText("✔", marginX + boxSize / 2f, boxY + 17f, checkPaint)
        }

        val label = if (idx == 8) {
            "9. ${customOptionText.ifBlank { "................................................" }}"
        } else {
            "${idx + 1}. $opt"
        }
        val p = if (isChecked) optionBoldPaint else optionPaint
        canvas.drawText(label, marginX + boxSize + 14f, currentY, p)
        currentY += 34f
    }

    currentY += 12f

    // 11. Closing paragraph
    val closingSsb = SpannableStringBuilder("অতএব, মহাশয় আমি অন্নপূর্ণা টাকা পাওয়ার যোগ্য, তাই আপনার নিকট আমার বিনীত নিবেদন, এই যে, আমাকে অন্নপূর্ণা যোজনার অন্তর্গত সুবিধা টি প্রদান করলে খুব উপকৃত হব। উপরোক্ত তথ্য আমার জ্ঞান ও বিশ্বাস মতে সত্য।")
    currentY = drawSpannableText(canvas, closingSsb, marginX, currentY, contentWidth, regularPaint) + 30f

    // 12. Footer Section
    val footerPaint = TextPaint().apply {
        color = android.graphics.Color.BLACK
        textSize = 20f
        isAntiAlias = true
    }
    val leftFooterSsb = SpannableStringBuilder().apply {
        append("তারিখ: ")
        appendBoldUnderline(this, formDate)
        append("\nফোন নম্বর: ")
        appendBoldUnderline(this, mobileNo.ifBlank { "...................." })
        append("\nআইডি নম্বর: ")
        appendBoldUnderline(this, idNo.ifBlank { "...................." })
        append("\nআধার নম্বর: ")
        appendBoldUnderline(this, aadhaarNo.ifBlank { "...................." })
        append("\nভোটার নম্বর: ")
        appendBoldUnderline(this, voterNo.ifBlank { "...................." })
    }

    val leftFooterLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(leftFooterSsb, 0, leftFooterSsb.length, footerPaint, (contentWidth * 0.6f).toInt())
            .setLineSpacing(0f, 1.4f)
            .build()
    } else {
        @Suppress("DEPRECATION")
        StaticLayout(leftFooterSsb, footerPaint, (contentWidth * 0.6f).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.4f, 0f, false)
    }

    canvas.save()
    canvas.translate(marginX, currentY)
    leftFooterLayout.draw(canvas)
    canvas.restore()

    // Right Footer: বিনীত- and Signature
    val rightX = width - marginX - 220f
    val binitPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 22f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("বিনীত-", rightX + 110f, currentY + 22f, binitPaint)

    if (signatureBitmap != null) {
        val sigDest = RectF(rightX + 10f, currentY + 36f, rightX + 210f, currentY + 110f)
        canvas.drawBitmap(signatureBitmap, null, sigDest, null)
    } else {
        val dotsPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 19f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("....................................", rightX + 110f, currentY + 70f, dotsPaint)
    }

    return bitmap
}

private fun drawSpannableText(
    canvas: Canvas,
    ssb: CharSequence,
    x: Float,
    y: Float,
    width: Int,
    paint: TextPaint
): Float {
    val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(ssb, 0, ssb.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.35f)
            .build()
    } else {
        @Suppress("DEPRECATION")
        StaticLayout(ssb, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.35f, 0f, false)
    }
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
    return y + layout.height
}

private fun appendBoldUnderline(builder: SpannableStringBuilder, text: String) {
    val start = builder.length
    builder.append(text)
    builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    builder.setSpan(UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

/**
 * Strict PDF Compression & Clamping Engine:
 * Guarantees the PDF file size is strictly between 180 KB and 190 KB (Default target: 185 KB = 189,440 bytes)
 */
private fun generateStrictSizedPdf(bitmap: Bitmap, targetKb: Int = 185): ByteArray {
    val targetBytes = targetKb * 1024 // 189,440 bytes (Strictly in 180 KB - 190 KB range)
    var currentBitmap = bitmap
    var quality = 85
    var jpegBytes: ByteArray

    while (true) {
        val stream = ByteArrayOutputStream()
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        jpegBytes = stream.toByteArray()

        // Base PDF overhead is ~650 bytes.
        // If JPEG + overhead exceeds targetBytes, reduce quality
        if (jpegBytes.size + 700 <= targetBytes) {
            break
        }

        if (quality > 35) {
            quality -= 6
        } else {
            val newWidth = (currentBitmap.width * 0.9f).toInt()
            val newHeight = (currentBitmap.height * 0.9f).toInt()
            currentBitmap = Bitmap.createScaledBitmap(currentBitmap, newWidth, newHeight, true)
            quality = 70
        }
    }

    return buildPdfWithExactSize(jpegBytes, currentBitmap.width, currentBitmap.height, targetBytes)
}

/**
 * Constructs a fully compliant PDF 1.4 document containing the A4 page image
 * and precision metadata/comment padding block to clamp the file size to the target byte range.
 */
private fun buildPdfWithExactSize(
    jpegBytes: ByteArray,
    imgWidth: Int,
    imgHeight: Int,
    exactTotalBytes: Int
): ByteArray {
    val out = ByteArrayOutputStream()
    val offsets = LongArray(6)

    // PDF Header (Binary)
    out.write("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n".toByteArray(Charsets.ISO_8859_1))

    // Object 1: Catalog
    offsets[1] = out.size().toLong()
    out.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".toByteArray(Charsets.US_ASCII))

    // Object 2: Pages
    offsets[2] = out.size().toLong()
    out.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".toByteArray(Charsets.US_ASCII))

    // Object 3: Page (A4 MediaBox: 595.28 x 841.89 pt)
    offsets[3] = out.size().toLong()
    val pageObj = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595.28 841.89] /Resources << /ProcSet [/PDF /ImageC] /XObject << /Im1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n"
    out.write(pageObj.toByteArray(Charsets.US_ASCII))

    // Object 4: Image XObject with JPEG stream
    offsets[4] = out.size().toLong()
    val imgHeader = "4 0 obj\n<< /Type /XObject /Subtype /Image /Width $imgWidth /Height $imgHeight /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpegBytes.size} >>\nstream\n"
    out.write(imgHeader.toByteArray(Charsets.US_ASCII))
    out.write(jpegBytes)
    out.write("\nendstream\nendobj\n".toByteArray(Charsets.US_ASCII))

    // Object 5: Contents stream
    offsets[5] = out.size().toLong()
    val contentStream = "q\n595.28 0 0 841.89 0 0 cm\n/Im1 Do\nQ\n"
    val contentObj = "5 0 obj\n<< /Length ${contentStream.length} >>\nstream\n$contentStream\nendstream\nendobj\n"
    out.write(contentObj.toByteArray(Charsets.US_ASCII))

    // Calculate trailer length with an estimated 6-digit startXref (around exactTotalBytes - 170L)
    val estimatedStartXref = exactTotalBytes - 170L
    val sampleTrailer = buildTrailerString(offsets, estimatedStartXref).toByteArray(Charsets.US_ASCII)

    val currentTotal = out.size() + sampleTrailer.size
    val neededPadding = exactTotalBytes - currentTotal

    if (neededPadding > 0) {
        val commentHeader = "%PADDING_STREAM_"
        val padLength = neededPadding - commentHeader.length - 1 // for '\n'
        if (padLength >= 0) {
            val padChars = CharArray(padLength) { 'X' }
            val comment = commentHeader + String(padChars) + "\n"
            out.write(comment.toByteArray(Charsets.US_ASCII))
        } else {
            val padChars = CharArray(maxOf(0, neededPadding - 2)) { ' ' }
            val comment = "%" + String(padChars) + "\n"
            out.write(comment.toByteArray(Charsets.US_ASCII))
        }
    }

    // Real startXref byte offset
    val realStartXref = out.size().toLong()
    val finalTrailer = buildTrailerString(offsets, realStartXref)
    out.write(finalTrailer.toByteArray(Charsets.US_ASCII))

    var resultBytes = out.toByteArray()

    // Safety check to ensure size is strictly within 180 KB - 190 KB range
    if (resultBytes.size < 180 * 1024) {
        val extraNeeded = (185 * 1024) - resultBytes.size
        val extraComment = "\n%PAD_" + "X".repeat(maxOf(0, extraNeeded - 7)) + "\n"
        resultBytes = (resultBytes.toString(Charsets.ISO_8859_1) + extraComment).toByteArray(Charsets.ISO_8859_1)
    }

    return resultBytes
}

private fun buildTrailerString(offsets: LongArray, startXref: Long): String {
    val sb = StringBuilder()
    sb.append("xref\n")
    sb.append("0 6\n")
    sb.append("0000000000 65535 f \n")
    for (i in 1..5) {
        sb.append(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
    }
    sb.append("trailer\n")
    sb.append("<< /Size 6 /Root 1 0 R >>\n")
    sb.append("startxref\n")
    sb.append(startXref).append("\n")
    sb.append("%%EOF\n")
    return sb.toString()
}

/**
 * Direct file save to Downloads folder
 */
private fun savePdfToDownloads(context: Context, pdfBytes: ByteArray, fileName: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(pdfBytes)
                    os.flush()
                }
                true
            } else false
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { os ->
                os.write(pdfBytes)
                os.flush()
            }
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * Opens a PDF document with the system's default PDF viewer application
 */
private fun openPdf(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF viewer application found on device", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extracts a friendly human-readable location from the SAF Document Uri
 */
private fun getHumanReadablePath(context: Context, uri: Uri): String {
    try {
        val path = uri.path ?: ""
        if (path.contains("primary:")) {
            return "Internal Storage > " + path.substringAfter("primary:").replace("/", " > ")
        }
        if (path.contains("/document/")) {
            val docPart = path.substringAfter("/document/")
            if (docPart.contains(":")) {
                return docPart.substringBefore(":") + " > " + docPart.substringAfter(":").replace("/", " > ")
            }
            return docPart
        }
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                val name = cursor.getString(nameIndex)
                return "Selected Folder > $name"
            }
        }
    } catch (e: Exception) {
        // fallback
    }
    return uri.lastPathSegment ?: uri.toString()
}

