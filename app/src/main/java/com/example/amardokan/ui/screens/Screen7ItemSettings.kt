package com.example.amardokan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amardokan.data.ServiceItem
import com.example.amardokan.ui.AmarDokanColors
import com.example.amardokan.ui.AmarDokanViewModel
import com.example.amardokan.ui.BanglaNumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen7ItemSettings(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val services by viewModel.services.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var serviceToEdit by remember { mutableStateOf<ServiceItem?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Dialog state
    var inputNameBn by remember { mutableStateOf("") }
    var inputNameEn by remember { mutableStateOf("") }
    var inputRate by remember { mutableStateOf("") }
    var inputUnit by remember { mutableStateOf("পাতা") }

    Scaffold(
        topBar = {
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
                        text = "আইটেম ও রেট সেটিংস",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { showResetConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "ডিফল্ট রেট রিস্টোর",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    inputNameBn = ""
                    inputNameEn = ""
                    inputRate = ""
                    inputUnit = "পাতা"
                    showAddDialog = true
                },
                containerColor = AmarDokanColors.PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_service")
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন সার্ভিস যোগ")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmarDokanColors.ContentBg)
                .testTag("screen_item_settings"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Notice banner
            item {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "এখানে নির্ধারিত রেট নতুন বিলে প্রযোজ্য হবে। পূর্বে তৈরি হওয়া কোনো বিলের রেট বা হিসাবে কোনো পরিবর্তন হবে না।",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            items(services, key = { it.id }) { service ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmarDokanColors.BorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.nameBn,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmarDokanColors.TextPrimary
                            )
                            if (service.nameEn.isNotBlank()) {
                                Text(
                                    text = service.nameEn,
                                    fontSize = 12.sp,
                                    color = AmarDokanColors.TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "প্রতি ${service.unitBn}",
                                    fontSize = 11.sp,
                                    color = AmarDokanColors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹" + BanglaNumberUtil.toBangla(service.defaultRate.toInt().toString()),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmarDokanColors.PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(
                                onClick = {
                                    serviceToEdit = service
                                    inputNameBn = service.nameBn
                                    inputNameEn = service.nameEn
                                    inputRate = service.defaultRate.toString()
                                    inputUnit = service.unitBn
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "এডিট", tint = AmarDokanColors.TextSecondary, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { viewModel.deleteService(service) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------- ADD SERVICE DIALOG ----------------
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন সার্ভিস / কাজ যোগ করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputNameBn,
                        onValueChange = { inputNameBn = it },
                        label = { Text("সার্ভিসের নাম (বাংলায়)") },
                        placeholder = { Text("যেমন: পাসপোর্ট ছবি প্রিন্ট") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputNameEn,
                        onValueChange = { inputNameEn = it },
                        label = { Text("ইংরেজি নাম (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputRate,
                            onValueChange = { inputRate = it },
                            label = { Text("দর / রেট (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputUnit,
                            onValueChange = { inputUnit = it },
                            label = { Text("একক (পাতা/টি)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameBn = inputNameBn.trim()
                        val rate = inputRate.toDoubleOrNull() ?: 0.0
                        if (nameBn.isNotBlank() && rate > 0.0) {
                            viewModel.addService(nameBn, inputNameEn.trim(), rate, inputUnit.trim())
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("সেভ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("বাতিল") }
            }
        )
    }

    // ---------------- EDIT SERVICE DIALOG ----------------
    if (serviceToEdit != null) {
        AlertDialog(
            onDismissRequest = { serviceToEdit = null },
            title = { Text("সার্ভিস রেট পরিবর্তন করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputNameBn,
                        onValueChange = { inputNameBn = it },
                        label = { Text("সার্ভিসের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputRate,
                            onValueChange = { inputRate = it },
                            label = { Text("দর / রেট (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputUnit,
                            onValueChange = { inputUnit = it },
                            label = { Text("একক (পাতা/টি)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameBn = inputNameBn.trim()
                        val rate = inputRate.toDoubleOrNull() ?: serviceToEdit!!.defaultRate
                        if (nameBn.isNotBlank() && rate > 0.0) {
                            viewModel.updateService(
                                serviceToEdit!!.copy(
                                    nameBn = nameBn,
                                    nameEn = inputNameEn.trim(),
                                    defaultRate = rate,
                                    unitBn = inputUnit.trim()
                                )
                            )
                            serviceToEdit = null
                        }
                    }
                ) {
                    Text("আপডেট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { serviceToEdit = null }) { Text("বাতিল") }
            }
        )
    }

    // ---------------- RESET TO DEFAULT DIALOG ----------------
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("ডিফল্ট রেট রিস্টোর করবেন?", fontWeight = FontWeight.Bold) },
            text = {
                Text("এটি মূল ৫টি সার্ভিস (সাদা-কালো জেরক্স ₹৩, কালার প্রিন্ট ₹১০, স্ক্যান ₹৫, লেমিনেশন ₹২০, অনলাইন ফর্ম ₹৫০) পুনরায় তালিকায় যুক্ত করবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDefaultServices()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("হ্যাঁ, রিস্টোর করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
