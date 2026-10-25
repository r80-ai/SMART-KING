package com.example.amardokan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var itemToEdit by remember { mutableStateOf<ServiceItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("আইটেম ও রেট সেটিংস", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmarDokanColors.NavyDarkHeader,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    itemToEdit = null
                    showAddDialog = true
                },
                containerColor = AmarDokanColors.PrimaryBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_service_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন আইটেম যোগ করুন")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmarDokanColors.ContentBg)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(services, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    text = item.nameBn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AmarDokanColors.TextPrimary
                                )
                                if (item.nameEn.isNotBlank()) {
                                    Text(
                                        text = item.nameEn,
                                        fontSize = 12.sp,
                                        color = AmarDokanColors.TextSecondary
                                    )
                                }
                                Text(
                                    text = "রেট: ₹${item.defaultRate.toInt()} প্রতি ${item.unitBn}",
                                    fontSize = 13.sp,
                                    color = AmarDokanColors.PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    itemToEdit = item
                                    showAddDialog = true
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "এডিট",
                                        tint = AmarDokanColors.PrimaryBlue
                                    )
                                }
                                if (!item.isDefault) {
                                    IconButton(onClick = { viewModel.deleteServiceItem(item) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "মুছুন",
                                            tint = AmarDokanColors.DueRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var nameBn by remember { mutableStateOf(itemToEdit?.nameBn ?: "") }
        var nameEn by remember { mutableStateOf(itemToEdit?.nameEn ?: "") }
        var rateText by remember { mutableStateOf(itemToEdit?.defaultRate?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
        var unitBn by remember { mutableStateOf(itemToEdit?.unitBn ?: "পাতা") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                itemToEdit = null
            },
            title = {
                Text(
                    text = if (itemToEdit == null) "নতুন সার্ভিস আইটেম যোগ করুন" else "আইটেম এডিট করুন",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameBn,
                        onValueChange = { nameBn = it },
                        label = { Text("আইটেমের নাম (বাংলায়) *") },
                        placeholder = { Text("যেমন: জেরক্স, রঙিন প্রিন্ট") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text("ইংরেজি নাম (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: Xerox, Color Print") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("মূল্য / রেট (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unitBn,
                        onValueChange = { unitBn = it },
                        label = { Text("একক (Unit)") },
                        placeholder = { Text("যেমন: পাতা, কপি, ফাইল") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = rateText.toDoubleOrNull() ?: 0.0
                        if (nameBn.isBlank()) {
                            viewModel.showMessage("আইটেমের নাম দিন")
                            return@Button
                        }
                        if (rate <= 0.0) {
                            viewModel.showMessage("সঠিক মূল্য দিন")
                            return@Button
                        }
                        if (itemToEdit == null) {
                            viewModel.addServiceItem(nameBn, nameEn, rate, unitBn)
                        } else {
                            viewModel.updateServiceItem(
                                itemToEdit!!.copy(
                                    nameBn = nameBn,
                                    nameEn = nameEn,
                                    defaultRate = rate,
                                    unitBn = unitBn
                                )
                            )
                        }
                        showAddDialog = false
                        itemToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.PrimaryBlue)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    itemToEdit = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
