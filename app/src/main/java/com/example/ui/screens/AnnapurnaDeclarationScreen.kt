package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SignatureUploadSection
import com.example.ui.components.toBase64Png
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnapurnaDeclarationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
এর সাথে আমার পরিবারের সকল সদস্যর নথি সংযোগ করলাম
আধার কার্ড ও ভোটার স্লিপের জেরক্স।

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

            // ACTION BUTTONS: A4 Print / Save PDF, Copy Text, Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A4 Print / Save PDF
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
                        .weight(1.3f)
                        .testTag("btn_annapurna_print"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("A4 Print / PDF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Copy Text
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Annapurna Declaration", plainTextDeclaration))
                        Toast.makeText(context, "ফর্মের লেখা কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
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
                            Text("এর সাথে আমার পরিবারের সকল সদস্যর নথি সংযোগ করলাম", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text("আধার কার্ড ও ভোটার স্লিপের জেরক্স।", color = Color.Black, fontSize = 10.sp)
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
                            Text("(আবেদনকারীর স্বাক্ষর)", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
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
        """<img src="data:image/png;base64,${signatureBitmap.toBase64Png()}" class="sig-img" alt="Signature" /><br><span style="font-size: 9pt; color: #444;">(আবেদনকারীর স্বাক্ষর)</span>"""
    } else {
        """................................................<br><span style="font-size: 9pt; color: #444;">(আবেদনকারীর স্বাক্ষর)</span>"""
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
                    <td style="vertical-align: middle; font-size: 10.5pt; ${if (isChecked) "font-weight: bold;" else ""}">
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
                    margin: 14mm 16mm 14mm 16mm;
                }
                body {
                    font-family: 'Noto Sans Bengali', sans-serif, 'Segoe UI', Arial, sans-serif;
                    font-size: 11pt;
                    line-height: 1.55;
                    color: #000;
                    margin: 0;
                    padding: 0;
                }
                .title {
                    text-align: center;
                    font-size: 15pt;
                    font-weight: bold;
                    text-decoration: underline;
                    margin-bottom: 12px;
                }
                .salutation {
                    font-weight: bold;
                    margin-bottom: 6px;
                }
                .line {
                    margin-bottom: 4px;
                    text-align: justify;
                }
                .bold-val {
                    font-weight: bold;
                    text-decoration: underline;
                }
                .box-tag {
                    display: inline-block;
                    border: 1.5px solid #000;
                    padding: 1px 8px;
                    font-weight: bold;
                    font-size: 10pt;
                    margin: 6px 0;
                }
                .options-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 4px 0 8px 0;
                }
                .options-table td {
                    padding: 2.5px 0;
                }
                .box {
                    width: 16px;
                    height: 16px;
                    border: 1.5px solid #000;
                    text-align: center;
                    line-height: 14px;
                    font-size: 11pt;
                    font-weight: bold;
                }
                .box.checked {
                    background-color: #f0fdf4;
                }
                .closing {
                    text-align: justify;
                    margin: 8px 0 12px 0;
                    font-size: 10.5pt;
                }
                .footer-table {
                    width: 100%;
                    margin-top: 10px;
                    border-collapse: collapse;
                }
                .footer-table td {
                    vertical-align: top;
                }
                .sig-img {
                    max-height: 48px;
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
            
            <div style="margin: 6px 0;">
                <div>আমার স্বামী/ পিতা-র কাজ/ পেশার বিবরণ: <span class="bold-val">${guardianProfession.ifBlank { "...................................." }}</span></div>
                <div>মাসিক আয়: <span class="bold-val">${monthlyIncome.ifBlank { "...................." }}</span></div>
                <div>গৃহের বিবরণ: <span class="bold-val">${houseDescription.ifBlank { "...................................." }}</span></div>
                <div>জমির পরিমান: <span class="bold-val">${landAmount.ifBlank { "...................." }}</span></div>
            </div>
            
            <div style="margin-top: 8px; font-weight: 500;">
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
                    <td style="width: 60%; font-size: 10pt; line-height: 1.6;">
                        <div>তারিখ: <span class="bold-val">$formDate</span></div>
                        <div>ফোন নম্বর: <span class="bold-val">${mobileNo.ifBlank { "...................." }}</span></div>
                        <div>আইডি নম্বর: <span class="bold-val">${idNo.ifBlank { "...................." }}</span></div>
                        <div>আধার নম্বর: <span class="bold-val">${aadhaarNo.ifBlank { "...................." }}</span></div>
                        <div>ভোটার নম্বর: <span class="bold-val">${voterNo.ifBlank { "...................." }}</span></div>
                        <div style="font-weight: 500; margin-top: 3px;">এর সাথে আমার পরিবারের সকল সদস্যর নথি সংযোগ করলাম</div>
                        <div>আধার কার্ড ও ভোটার স্লিপের জেরক্স।</div>
                    </td>
                    <td style="width: 40%; text-align: center;">
                        <div style="font-weight: bold; margin-bottom: 8px;">বিনীত-</div>
                        $signatureHtml
                    </td>
                </tr>
            </table>
        </body>
        </html>
    """.trimIndent()
}
