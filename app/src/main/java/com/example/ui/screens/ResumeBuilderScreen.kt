package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.*
import com.example.storage.GlobalSaveManager
import com.example.storage.SaveResult
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResumeBuilderScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    var resumeData by remember { mutableStateOf(ResumeData()) }
    var selectedTemplate by remember { mutableStateOf(ResumeTemplate.FORMAL_CURVE) }
    var showPreview by remember { mutableStateOf(false) }

    val steps = listOf("Personal", "Education", "Experience", "Skills")
    val stepColors = listOf(ToolPurple, ToolOrange, ToolOrange, ToolTeal)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Resume / CV Maker", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showPreview = !showPreview }) {
                            Text(
                                if (showPreview) "Edit Form" else "Preview",
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (!showPreview) {
                    Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { if (currentStep > 0) currentStep-- else onBack() },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(if (currentStep == 0) "Cancel" else "Previous", color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = {
                                    if (currentStep < 3) currentStep++
                                    else showPreview = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                            ) {
                                Text(if (currentStep == 3) "Resume Ready" else "Next", color = Color.White)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                if (showPreview) {
                    ResumePreviewSection(
                        data = resumeData,
                        template = selectedTemplate,
                        onTemplateChange = { selectedTemplate = it },
                        onBack = { showPreview = false }
                    )
                } else {
                    // Step Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        steps.forEachIndexed { index, title ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable {
                                        currentStep = index
                                        showPreview = false
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index <= currentStep) stepColors[index]
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (index == currentStep) 2.dp else 0.dp,
                                            color = if (index == currentStep) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (index < currentStep) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text("${index + 1}", color = if (index <= currentStep) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    color = if (index <= currentStep) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (index == currentStep) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentStep) {
                            0 -> PersonalDetailsSection(resumeData) { resumeData = it }
                            1 -> EducationSection(resumeData) { resumeData = it }
                            2 -> ExperienceSection(resumeData) { resumeData = it }
                            3 -> SkillsAndOtherSection(resumeData) { resumeData = it }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFieldWithPicker(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        onValueChange(sdf.format(Date(it)))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
        placeholder = { Text("DD-MM-YYYY", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onNext?.invoke() },
            onDone = { onNext?.invoke() }
        ),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun PersonalDetailsSection(data: ResumeData, onUpdate: (ResumeData) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            onUpdate(data.copy(photo = bitmap))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Personal Details", fontWeight = FontWeight.Bold, color = ToolPurple, style = MaterialTheme.typography.titleMedium)

        // 1. Profile Photo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .clickable { photoPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (data.photo != null) {
                    androidx.compose.foundation.Image(
                        bitmap = data.photo.asImageBitmap(),
                        contentDescription = "Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Profile Photo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Max 2MB (JPG, PNG, WebP)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (data.photo != null) {
                    TextButton(onClick = { onUpdate(data.copy(photo = null)) }) {
                        Text("Remove Photo", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Title & Full Name
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var expanded by remember { mutableStateOf(false) }
            val titles = listOf("Mr.", "Mrs.", "Ms.", "Dr.", "Sri", "Srimati", "Kumari")
            Box(modifier = Modifier.weight(0.3f)) {
                OutlinedTextField(
                    value = data.personalDetails.title,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Title", color = MaterialTheme.colorScheme.onSurface) },
                    trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface) } },
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    titles.forEach { title ->
                        DropdownMenuItem(
                            text = { Text(title) },
                            onClick = {
                                onUpdate(data.copy(personalDetails = data.personalDetails.copy(title = title)))
                                expanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = data.personalDetails.fullName,
                onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(fullName = it))) },
                label = { Text("Full Name", color = MaterialTheme.colorScheme.onSurface) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.weight(0.7f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // 3. Father / Guardian Name (directly below Full Name)
        OutlinedTextField(
            value = data.personalDetails.fatherName,
            onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(fatherName = it))) },
            label = { Text("Father / Guardian Name", color = MaterialTheme.colorScheme.onSurface) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // 4. Date of Birth (Calendar picker dialog + manual entry)
        DateFieldWithPicker(
            label = "Date of Birth",
            value = data.personalDetails.dob,
            onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(dob = it))) },
            imeAction = ImeAction.Next,
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        // 5. Gender & 6. Marital Status (Dropdowns)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var gExpanded by remember { mutableStateOf(false) }
            val genders = listOf("Select", "Male", "Female", "Other")
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = data.personalDetails.gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender", color = MaterialTheme.colorScheme.onSurface) },
                    trailingIcon = { IconButton(onClick = { gExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface) } },
                    modifier = Modifier.fillMaxWidth().clickable { gExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                DropdownMenu(expanded = gExpanded, onDismissRequest = { gExpanded = false }) {
                    genders.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g) },
                            onClick = {
                                onUpdate(data.copy(personalDetails = data.personalDetails.copy(gender = g)))
                                gExpanded = false
                            }
                        )
                    }
                }
            }
            var mExpanded by remember { mutableStateOf(false) }
            val status = listOf("Select", "Single", "Married", "Other")
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = data.personalDetails.maritalStatus,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Marital Status", color = MaterialTheme.colorScheme.onSurface) },
                    trailingIcon = { IconButton(onClick = { mExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface) } },
                    modifier = Modifier.fillMaxWidth().clickable { mExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                DropdownMenu(expanded = mExpanded, onDismissRequest = { mExpanded = false }) {
                    status.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s) },
                            onClick = {
                                onUpdate(data.copy(personalDetails = data.personalDetails.copy(maritalStatus = s)))
                                mExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 7. Professional Title, Phone, Email
        OutlinedTextField(
            value = data.personalDetails.professionalTitle,
            onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(professionalTitle = it))) },
            label = { Text("Professional Title (e.g. Graphic Designer, Teacher)", color = MaterialTheme.colorScheme.onSurface) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = data.personalDetails.phone,
                onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(phone = it))) },
                label = { Text("Phone", color = MaterialTheme.colorScheme.onSurface) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            OutlinedTextField(
                value = data.personalDetails.email,
                onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(email = it))) },
                label = { Text("Email", color = MaterialTheme.colorScheme.onSurface) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // 8. Address Details: House/Flat, Landmark, Area/Locality
        Text("Address Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurface)

        OutlinedTextField(
            value = data.personalDetails.houseNo,
            onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(houseNo = it))) },
            label = { Text("House No / Flat", color = MaterialTheme.colorScheme.onSurface) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        OutlinedTextField(
            value = data.personalDetails.landmark,
            onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(landmark = it))) },
            label = { Text("Landmark / Nearby", color = MaterialTheme.colorScheme.onSurface) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        OutlinedTextField(
            value = data.personalDetails.area,
            onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(area = it))) },
            label = { Text("Area / Locality", color = MaterialTheme.colorScheme.onSurface) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // 9. State / City: Indian State dropdown selector (default "West Bengal") alongside Pincode
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            var stateExpanded by remember { mutableStateOf(false) }
            val indianStates = listOf(
                "West Bengal", "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar",
                "Chhattisgarh", "Goa", "Gujarat", "Haryana", "Himachal Pradesh",
                "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra",
                "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha",
                "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana",
                "Tripura", "Uttar Pradesh", "Uttarakhand", "Delhi",
                "Jammu and Kashmir", "Ladakh", "Puducherry", "Chandigarh", "Other"
            )
            Box(modifier = Modifier.weight(0.6f)) {
                OutlinedTextField(
                    value = data.personalDetails.stateCity.ifBlank { "West Bengal" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("State", color = MaterialTheme.colorScheme.onSurface) },
                    trailingIcon = { IconButton(onClick = { stateExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface) } },
                    modifier = Modifier.fillMaxWidth().clickable { stateExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                DropdownMenu(
                    expanded = stateExpanded,
                    onDismissRequest = { stateExpanded = false },
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    indianStates.forEach { st ->
                        DropdownMenuItem(
                            text = { Text(st) },
                            onClick = {
                                onUpdate(data.copy(personalDetails = data.personalDetails.copy(stateCity = st)))
                                stateExpanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = data.personalDetails.pincode,
                onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(pincode = it))) },
                label = { Text("Pincode", color = MaterialTheme.colorScheme.onSurface) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.weight(0.4f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // 10. Nationality (Default "Indian")
        OutlinedTextField(
            value = data.personalDetails.nationality.ifBlank { "Indian" },
            onValueChange = { onUpdate(data.copy(personalDetails = data.personalDetails.copy(nationality = it))) },
            label = { Text("Nationality", color = MaterialTheme.colorScheme.onSurface) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Text("Career Objective", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurface)
        
        val objectives = listOf(
            "To secure a challenging position in a reputable organization...",
            "Highly motivated and enthusiastic individual seeking an entry-level position...",
            "To leverage my technical skills and professional expertise...",
            "Seeking a position as a [Job Title] where I can contribute my experience..."
        )
        var objExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Choose a sample objective", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = { IconButton(onClick = { objExpanded = true }) { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurface) } },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            DropdownMenu(expanded = objExpanded, onDismissRequest = { objExpanded = false }) {
                objectives.forEach { obj ->
                    DropdownMenuItem(
                        text = { Text(obj, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            onUpdate(data.copy(objective = obj))
                            objExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = data.objective,
            onValueChange = { onUpdate(data.copy(objective = it)) },
            label = { Text("Objective Statement", color = MaterialTheme.colorScheme.onSurface) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun EducationSection(data: ResumeData, onUpdate: (ResumeData) -> Unit) {
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        if (data.education.isEmpty()) {
            onUpdate(data.copy(education = listOf(EducationItem())))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Education", fontWeight = FontWeight.Bold, color = ToolOrange, style = MaterialTheme.typography.titleMedium)

        data.education.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Education #${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = {
                            val newList = data.education.toMutableList().apply { removeAt(index) }
                            onUpdate(data.copy(education = newList))
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    OutlinedTextField(
                        value = item.degree,
                        onValueChange = {
                            val newList = data.education.toMutableList().apply { this[index] = item.copy(degree = it) }
                            onUpdate(data.copy(education = newList))
                        },
                        label = { Text("Degree / Exam Name", color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = item.school,
                        onValueChange = {
                            val newList = data.education.toMutableList().apply { this[index] = item.copy(school = it) }
                            onUpdate(data.copy(education = newList))
                        },
                        label = { Text("School / College / University", color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = item.year,
                            onValueChange = {
                                val newList = data.education.toMutableList().apply { this[index] = item.copy(year = it) }
                                onUpdate(data.copy(education = newList))
                            },
                            label = { Text("Year", color = MaterialTheme.colorScheme.onSurface) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        OutlinedTextField(
                            value = item.result,
                            onValueChange = {
                                val newList = data.education.toMutableList().apply { this[index] = item.copy(result = it) }
                                onUpdate(data.copy(education = newList))
                            },
                            label = { Text("Result / %", color = MaterialTheme.colorScheme.onSurface) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val newList = data.education + EducationItem()
                onUpdate(data.copy(education = newList))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ToolOrange)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Education")
        }
    }
}

@Composable
fun ExperienceSection(data: ResumeData, onUpdate: (ResumeData) -> Unit) {
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        if (data.experience.isEmpty()) {
            onUpdate(data.copy(experience = listOf(ExperienceItem())))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Experience", fontWeight = FontWeight.Bold, color = ToolOrange, style = MaterialTheme.typography.titleMedium)

        data.experience.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Experience #${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = {
                            val newList = data.experience.toMutableList().apply { removeAt(index) }
                            onUpdate(data.copy(experience = newList))
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    OutlinedTextField(
                        value = item.jobTitle,
                        onValueChange = {
                            val newList = data.experience.toMutableList().apply { this[index] = item.copy(jobTitle = it) }
                            onUpdate(data.copy(experience = newList))
                        },
                        label = { Text("Job Title", color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = item.company,
                        onValueChange = {
                            val newList = data.experience.toMutableList().apply { this[index] = item.copy(company = it) }
                            onUpdate(data.copy(experience = newList))
                        },
                        label = { Text("Company / Organization", color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = item.period,
                        onValueChange = {
                            val newList = data.experience.toMutableList().apply { this[index] = item.copy(period = it) }
                            onUpdate(data.copy(experience = newList))
                        },
                        label = { Text("Period (e.g. Jan 2020 - Present)", color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = item.details,
                        onValueChange = {
                            val newList = data.experience.toMutableList().apply { this[index] = item.copy(details = it) }
                            onUpdate(data.copy(experience = newList))
                        },
                        label = { Text("Work Details", color = MaterialTheme.colorScheme.onSurface) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }

        Button(
            onClick = {
                val newList = data.experience + ExperienceItem()
                onUpdate(data.copy(experience = newList))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ToolOrange)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Experience")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsAndOtherSection(data: ResumeData, onUpdate: (ResumeData) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var customSkillInput by remember { mutableStateOf("") }
    var customLangInput by remember { mutableStateOf("") }

    val sigPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            onUpdate(data.copy(signature = bitmap))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Skills & Other Details", fontWeight = FontWeight.Bold, color = ToolTeal, style = MaterialTheme.typography.titleMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Visibility & Formatting", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = data.showEducation, onCheckedChange = { onUpdate(data.copy(showEducation = it)) })
                    Text("Show Education", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = data.showExperience, onCheckedChange = { onUpdate(data.copy(showExperience = it)) })
                    Text("Show Experience", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = data.showSkills, onCheckedChange = { onUpdate(data.copy(showSkills = it)) })
                    Text("Show Skills & Languages", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = data.showObjective, onCheckedChange = { onUpdate(data.copy(showObjective = it)) })
                    Text("Show Career Objective", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Skills Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Selected Skills (${data.skills.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (data.skills.isNotEmpty()) {
                        TextButton(onClick = { onUpdate(data.copy(skills = emptyList())) }) {
                            Text("Clear All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Distinct box displaying selected skills dynamically
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    if (data.skills.isEmpty()) {
                        Text("No skills selected yet. Tap suggestions below or add a custom skill.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            data.skills.forEach { skill ->
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        val newList = data.skills - skill
                                        onUpdate(data.copy(skills = newList))
                                    },
                                    label = { Text(skill, fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove $skill", modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }
                    }
                }

                // Add Custom Skill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customSkillInput,
                        onValueChange = { customSkillInput = it },
                        placeholder = { Text("Add custom skill (e.g. Kotlin, Driving)", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (customSkillInput.isNotBlank() && !data.skills.contains(customSkillInput.trim())) {
                                onUpdate(data.copy(skills = data.skills + customSkillInput.trim()))
                                customSkillInput = ""
                            }
                        }),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Button(
                        onClick = {
                            if (customSkillInput.isNotBlank() && !data.skills.contains(customSkillInput.trim())) {
                                onUpdate(data.copy(skills = data.skills + customSkillInput.trim()))
                                customSkillInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToolTeal)
                    ) {
                        Text("Add")
                    }
                }

                Text("Suggestions", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val skillOptions = listOf(
                    "Computer Basics", "MS Word", "MS Excel", "MS PowerPoint", "Internet & Email",
                    "Data Entry", "Typing", "Tally / Accounting", "Customer Service",
                    "Communication", "Sales & Marketing", "Teamwork", "Time Management",
                    "Problem Solving", "Office Administration", "Basic Hardware / IT"
                )
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    skillOptions.forEach { skill ->
                        val isSelected = data.skills.contains(skill)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newList = if (isSelected) data.skills - skill else data.skills + skill
                                onUpdate(data.copy(skills = newList))
                            },
                            label = { Text(skill, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Languages Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Selected Languages (${data.languages.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (data.languages.isNotEmpty()) {
                        TextButton(onClick = { onUpdate(data.copy(languages = emptyList())) }) {
                            Text("Clear All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Distinct box displaying selected languages dynamically
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    if (data.languages.isEmpty()) {
                        Text("No languages selected yet. Tap suggestions below or add custom language.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            data.languages.forEach { lang ->
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        val newList = data.languages - lang
                                        onUpdate(data.copy(languages = newList))
                                    },
                                    label = { Text(lang, fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove $lang", modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }
                    }
                }

                // Add Custom Language
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customLangInput,
                        onValueChange = { customLangInput = it },
                        placeholder = { Text("Add language (e.g. French, Japanese)", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (customLangInput.isNotBlank() && !data.languages.contains(customLangInput.trim())) {
                                onUpdate(data.copy(languages = data.languages + customLangInput.trim()))
                                customLangInput = ""
                            }
                        }),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Button(
                        onClick = {
                            if (customLangInput.isNotBlank() && !data.languages.contains(customLangInput.trim())) {
                                onUpdate(data.copy(languages = data.languages + customLangInput.trim()))
                                customLangInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToolTeal)
                    ) {
                        Text("Add")
                    }
                }

                Text("Suggestions", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val langOptions = listOf("Bengali", "Hindi", "English", "Urdu", "Odia", "Assamese", "Nepali", "Marathi", "Gujarati", "Punjabi", "Tamil", "Telugu", "Kannada", "Malayalam")
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    langOptions.forEach { lang ->
                        val isSelected = data.languages.contains(lang)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newList = if (isSelected) data.languages - lang else data.languages + lang
                                onUpdate(data.copy(languages = newList))
                            },
                            label = { Text(lang, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Text("Additional Details & Signature", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface)

        OutlinedTextField(
            value = data.additionalInfo,
            onValueChange = { onUpdate(data.copy(additionalInfo = it)) },
            label = { Text("Additional Information / Certifications", color = MaterialTheme.colorScheme.onSurface) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateFieldWithPicker(
                label = "Date",
                value = data.date,
                onValueChange = { onUpdate(data.copy(date = it)) },
                imeAction = ImeAction.Next,
                onNext = { focusManager.moveFocus(FocusDirection.Right) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = data.place,
                onValueChange = { onUpdate(data.copy(place = it)) },
                label = { Text("Place", color = MaterialTheme.colorScheme.onSurface) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = { sigPicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload Signature Image")
        }
        if (data.signature != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.foundation.Image(
                    bitmap = data.signature.asImageBitmap(),
                    contentDescription = "Signature",
                    modifier = Modifier.height(50.dp).width(120.dp),
                    contentScale = ContentScale.Fit
                )
                TextButton(onClick = { onUpdate(data.copy(signature = null)) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun InteractiveViewer(
    modifier: Modifier = Modifier,
    minScale: Float = 0.8f,
    maxScale: Float = 4.0f,
    scale: Float = 1.0f,
    onScaleChange: (Float) -> Unit = {},
    offset: Offset = Offset.Zero,
    onOffsetChange: (Offset) -> Unit = {},
    content: @Composable () -> Unit
) {
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .pointerInput(minScale, maxScale) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (currentScale * zoom).coerceIn(minScale, maxScale)
                    onScaleChange(newScale)
                    val newOffset = if (newScale > 1.0f) {
                        Offset(currentOffset.x + pan.x, currentOffset.y + pan.y)
                    } else {
                        Offset.Zero
                    }
                    onOffsetChange(newOffset)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val a4Width = 595f
        val a4Height = 842f
        val availableW = (maxWidth.value - 16f).coerceAtLeast(100f)
        val availableH = (maxHeight.value - 16f).coerceAtLeast(100f)
        val fitScale = minOf(
            availableW / a4Width,
            availableH / a4Height
        ).coerceAtLeast(0.1f)

        val totalScale = fitScale * scale

        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = totalScale
                    scaleY = totalScale
                    translationX = offset.x
                    translationY = offset.y
                }
                .requiredSize(a4Width.dp, a4Height.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun ResumePreviewSection(
    data: ResumeData,
    template: ResumeTemplate,
    onTemplateChange: (ResumeTemplate) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveFileNameInput by remember { mutableStateOf("") }
    var lastSaveResult by remember { mutableStateOf<SaveResult?>(null) }
    var showSavedSuccessDialog by remember { mutableStateOf(false) }

    // Dialog for Custom Filename Input Prompt
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon = { Icon(Icons.Default.Download, contentDescription = null, tint = ToolGreen) },
            title = { Text("Save Resume as PDF", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter a file name for your resume. It will be saved into the PDF Master folder on your device.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = saveFileNameInput,
                        onValueChange = { saveFileNameInput = it },
                        label = { Text("File Name") },
                        suffix = { Text(".pdf", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Destination: /storage/emulated/0/PDF_Master/",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosenName = saveFileNameInput.trim().ifBlank {
                            "Resume_${data.personalDetails.fullName.trim().replace("\\s+".toRegex(), "_").ifBlank { "CV" }}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
                        }
                        showSaveDialog = false
                        isExporting = true
                        coroutineScope.launch {
                            try {
                                val result = exportResumeToPdf(context, data, template, chosenName)
                                isExporting = false
                                if (result != null) {
                                    lastSaveResult = result
                                    showSavedSuccessDialog = true
                                    Toast.makeText(context, "Saved to ${result.fileName}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isExporting = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolGreen)
                ) {
                    Text("Save PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Feedback Dialog with Open and Share actions
    if (showSavedSuccessDialog && lastSaveResult != null) {
        val result = lastSaveResult!!
        AlertDialog(
            onDismissRequest = { showSavedSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ToolGreen, modifier = Modifier.size(36.dp)) },
            title = { Text("Resume Saved Successfully!", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your CV has been generated and saved to your device.", fontSize = 13.sp)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("File Name:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(result.fileName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Saved Path:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(result.savedFile?.absolutePath ?: result.displayLocation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSavedSuccessDialog = false
                        GlobalSaveManager.openFile(context, result.savedUri, result.savedFile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open PDF")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, result.savedUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Resume"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                    TextButton(onClick = { showSavedSuccessDialog = false }) {
                        Text("Done")
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Live Preview",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        zoomScale = (zoomScale - 0.25f).coerceIn(0.8f, 4.0f)
                        if (zoomScale <= 1.0f) panOffset = Offset.Zero
                    }
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "${(zoomScale * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        zoomScale = (zoomScale + 0.25f).coerceIn(0.8f, 4.0f)
                    }
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
                }
                TextButton(
                    onClick = {
                        zoomScale = 1.0f
                        panOffset = Offset.Zero
                    }
                ) {
                    Text("Fit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            InteractiveViewer(
                minScale = 0.8f,
                maxScale = 4.0f,
                scale = zoomScale,
                onScaleChange = { zoomScale = it },
                offset = panOffset,
                onOffsetChange = { panOffset = it },
                modifier = Modifier.fillMaxSize()
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    ResumeTemplateRenderer(data = data, template = template)
                }
            }
        }

        Text("Select Template", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(template.displayName)
                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ResumeTemplate.entries.forEach { t ->
                    DropdownMenuItem(text = { Text(t.displayName) }, onClick = { onTemplateChange(t); expanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val defaultBaseName = "Resume_${data.personalDetails.fullName.trim().replace("\\s+".toRegex(), "_").ifBlank { "CV" }}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
                    saveFileNameInput = defaultBaseName
                    showSaveDialog = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                enabled = !isExporting
            ) {
                if (isExporting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save PDF")
            }
            Button(
                onClick = {
                    isExporting = true
                    coroutineScope.launch {
                        try {
                            val printDocName = "Resume_Print_${System.currentTimeMillis()}"
                            val result = exportResumeToPdf(context, data, template, printDocName)
                            isExporting = false
                            if (result != null) {
                                GlobalSaveManager.openFile(context, result.savedUri, result.savedFile)
                            } else {
                                Toast.makeText(context, "Failed to prepare document for printing", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            isExporting = false
                            Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ToolIndigo),
                enabled = !isExporting
            ) {
                Icon(Icons.Default.Print, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Print")
            }
        }
    }
}

@Composable
fun ResumeTemplateRenderer(data: ResumeData, template: ResumeTemplate) {
    when (template) {
        ResumeTemplate.FORMAL_CURVE -> FormalCurveTemplate(data)
        ResumeTemplate.PROFESSIONAL_SIDEBAR -> ProfessionalSidebarTemplate(data)
        ResumeTemplate.ACCENT_EXECUTIVE -> AccentExecutiveTemplate(data)
        ResumeTemplate.PROFILE_HEADER -> ProfileHeaderTemplate(data)
        ResumeTemplate.BIO_DATA_CLASSIC -> BioDataClassicTemplate(data)
        ResumeTemplate.FRESH_PROFESSIONAL -> FreshProfessionalTemplate(data)
    }
}

@Composable
fun TemplateHeader(data: ResumeData, accentColor: Color = Color.Black) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${data.personalDetails.title} ${data.personalDetails.fullName}".ifBlank { "FULL NAME" }.uppercase(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = accentColor
            )
            Text(
                text = data.personalDetails.professionalTitle.ifBlank { "Professional Title" },
                fontSize = 12.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Phone: ${data.personalDetails.phone} | Email: ${data.personalDetails.email}", fontSize = 9.sp, color = Color.Gray)
        }
        if (data.photo != null) {
            androidx.compose.foundation.Image(
                bitmap = data.photo.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun TemplateSectionTitle(title: String, accentColor: Color = Color.Black) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(title.uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = accentColor)
        HorizontalDivider(modifier = Modifier.padding(top = 2.dp), thickness = 1.dp, color = accentColor.copy(alpha = 0.3f))
    }
}

@Composable
fun ColumnScope.TemplateFooter(data: ResumeData) {
    Spacer(modifier = Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text("Date: ${data.date}", fontSize = 9.sp, color = Color.Black)
            Text("Place: ${data.place}", fontSize = 9.sp, color = Color.Black)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (data.signature != null) {
                androidx.compose.foundation.Image(
                    bitmap = data.signature.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.height(30.dp).width(70.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }
            HorizontalDivider(modifier = Modifier.width(90.dp), thickness = 0.5.dp, color = Color.Black)
            Text("Signature", fontSize = 8.sp, color = Color.Black)
        }
    }
}

@Composable
fun FormalCurveTemplate(data: ResumeData) {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TemplateHeader(data, accentColor = ToolPurple)
            
            if (data.showObjective && data.objective.isNotBlank()) {
                TemplateSectionTitle("Career Objective", ToolPurple)
                Text(data.objective, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            }

            if (data.showEducation && data.education.isNotEmpty()) {
                TemplateSectionTitle("Education", ToolPurple)
                data.education.forEach { edu ->
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text("${edu.degree} - ${edu.school}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
                        Text("Year: ${edu.year} | Result: ${edu.result}", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }

            if (data.showExperience && data.experience.isNotEmpty()) {
                TemplateSectionTitle("Experience", ToolPurple)
                data.experience.forEach { exp ->
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text("${exp.jobTitle} at ${exp.company}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
                        Text(exp.period, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(exp.details, fontSize = 9.sp, color = Color.DarkGray)
                    }
                }
            }

            if (data.showSkills && (data.skills.isNotEmpty() || data.languages.isNotEmpty())) {
                TemplateSectionTitle("Skills & Languages", ToolPurple)
                if (data.skills.isNotEmpty()) Text("Skills: ${data.skills.joinToString(", ")}", fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                if (data.languages.isNotEmpty()) Text("Languages: ${data.languages.joinToString(", ")}", fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp))
            }

            TemplateSectionTitle("Personal Details", ToolPurple)
            Column(modifier = Modifier.padding(top = 4.dp)) {
                PreviewDetailRow("Father's Name", data.personalDetails.fatherName)
                PreviewDetailRow("DOB", data.personalDetails.dob)
                PreviewDetailRow("Gender", data.personalDetails.gender)
                PreviewDetailRow("Marital Status", data.personalDetails.maritalStatus)
                PreviewDetailRow("Address", "${data.personalDetails.houseNo}, ${data.personalDetails.area}, ${data.personalDetails.stateCity} - ${data.personalDetails.pincode}")
            }

            TemplateFooter(data)
        }
    }
}

@Composable
fun ProfessionalSidebarTemplate(data: ResumeData) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(0.35f).fillMaxHeight().background(Color(0xFFF3F4F6)).padding(15.dp)) {
            if (data.photo != null) {
                androidx.compose.foundation.Image(
                    bitmap = data.photo.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(80.dp).background(Color.LightGray, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("CONTACT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
            Text("Phone:\n${data.personalDetails.phone}", fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            Text("Email:\n${data.personalDetails.email}", fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            
            if (data.showSkills && data.skills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("SKILLS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                data.skills.forEach { skill ->
                    Text("• $skill", fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp))
                }
            }
            
            if (data.languages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("LANGUAGES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                Text(data.languages.joinToString("\n"), fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Column(modifier = Modifier.weight(0.65f).fillMaxHeight().padding(20.dp)) {
            Text("${data.personalDetails.fullName}".uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
            Text(data.personalDetails.professionalTitle, fontSize = 12.sp, color = Color.Gray)
            
            if (data.showObjective && data.objective.isNotBlank()) {
                TemplateSectionTitle("Profile", Color.Black)
                Text(data.objective, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            }

            if (data.showExperience && data.experience.isNotEmpty()) {
                TemplateSectionTitle("Experience", Color.Black)
                data.experience.forEach { exp ->
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Text(exp.jobTitle, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
                        Text(exp.company, fontWeight = FontWeight.Medium, fontSize = 9.sp, color = Color.DarkGray)
                        Text(exp.period, fontSize = 8.sp, color = Color.Gray)
                    }
                }
            }

            if (data.showEducation && data.education.isNotEmpty()) {
                TemplateSectionTitle("Education", Color.Black)
                data.education.forEach { edu ->
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Text(edu.degree, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
                        Text(edu.school, fontSize = 9.sp, color = Color.DarkGray)
                    }
                }
            }
            
            TemplateFooter(data)
        }
    }
}

@Composable
fun AccentExecutiveTemplate(data: ResumeData) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0xFF1F2937)).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
            Column {
                Text("${data.personalDetails.fullName}".uppercase(), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                Text(data.personalDetails.professionalTitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("📞 ${data.personalDetails.phone}", fontSize = 9.sp, color = Color.DarkGray)
                Text("📧 ${data.personalDetails.email}", fontSize = 9.sp, color = Color.DarkGray)
            }
            
            if (data.showObjective && data.objective.isNotBlank()) {
                TemplateSectionTitle("Summary", Color(0xFF1F2937))
                Text(data.objective, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    if (data.showExperience && data.experience.isNotEmpty()) {
                        TemplateSectionTitle("Work Experience", Color(0xFF1F2937))
                        data.experience.forEach { exp ->
                            Column(modifier = Modifier.padding(top = 6.dp)) {
                                Text(exp.jobTitle, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text(exp.company, fontSize = 9.sp, color = Color.DarkGray)
                                Text(exp.period, fontSize = 8.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (data.showEducation && data.education.isNotEmpty()) {
                        TemplateSectionTitle("Education", Color(0xFF1F2937))
                        data.education.forEach { edu ->
                            Column(modifier = Modifier.padding(top = 6.dp)) {
                                Text(edu.degree, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text(edu.school, fontSize = 9.sp, color = Color.DarkGray)
                                Text("Result: ${edu.result}", fontSize = 8.sp, color = Color.Gray)
                            }
                        }
                    }
                    if (data.showSkills && data.skills.isNotEmpty()) {
                        TemplateSectionTitle("Expertise", Color(0xFF1F2937))
                        Text(data.skills.joinToString(", "), fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            TemplateFooter(data)
        }
    }
}

@Composable
fun ProfileHeaderTemplate(data: ResumeData) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (data.photo != null) {
                androidx.compose.foundation.Image(
                    bitmap = data.photo.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(15.dp))
            Column {
                Text("${data.personalDetails.fullName}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.Black)
                Text(data.personalDetails.professionalTitle, fontSize = 14.sp, color = ToolOrange, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("📍 ${data.personalDetails.stateCity} | 📧 ${data.personalDetails.email}", fontSize = 10.sp, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(15.dp))
        if (data.showObjective) {
            Text(data.objective, fontSize = 10.sp, color = Color.DarkGray)
        }

        TemplateSectionTitle("Career Path", ToolOrange)
        data.experience.forEach { exp ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(exp.period, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(70.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(exp.jobTitle, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text(exp.company, fontSize = 9.sp, color = Color.DarkGray)
                }
            }
        }

        TemplateSectionTitle("Learning", ToolOrange)
        data.education.forEach { edu ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(edu.year, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(70.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(edu.degree, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text(edu.school, fontSize = 9.sp, color = Color.DarkGray)
                }
            }
        }
        TemplateFooter(data)
    }
}

@Composable
fun BioDataClassicTemplate(data: ResumeData) {
    Column(modifier = Modifier.fillMaxSize().padding(25.dp)) {
        Text("BIO-DATA", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                PreviewDetailRow("Full Name", "${data.personalDetails.title} ${data.personalDetails.fullName}")
                PreviewDetailRow("Father's Name", data.personalDetails.fatherName)
                PreviewDetailRow("Date of Birth", data.personalDetails.dob)
                PreviewDetailRow("Gender", data.personalDetails.gender)
                PreviewDetailRow("Marital Status", data.personalDetails.maritalStatus)
                PreviewDetailRow("Nationality", data.personalDetails.nationality.ifBlank { "Indian" })
            }
            if (data.photo != null) {
                androidx.compose.foundation.Image(
                    bitmap = data.photo.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).border(1.dp, Color.Black),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        TemplateSectionTitle("Educational Qualification", Color.Black)
        data.education.forEach { edu ->
            Text("• ${edu.degree} from ${edu.school} in ${edu.year} (${edu.result})", fontSize = 10.sp, color = Color.Black, modifier = Modifier.padding(top = 4.dp))
        }

        TemplateSectionTitle("Address", Color.Black)
        Text("${data.personalDetails.houseNo}, ${data.personalDetails.landmark}, ${data.personalDetails.area}, ${data.personalDetails.stateCity} - ${data.personalDetails.pincode}", fontSize = 10.sp, color = Color.Black, modifier = Modifier.padding(top = 4.dp))
        
        TemplateSectionTitle("Contact", Color.Black)
        Text("Phone: ${data.personalDetails.phone}", fontSize = 10.sp, color = Color.Black)
        Text("Email: ${data.personalDetails.email}", fontSize = 10.sp, color = Color.Black)
        
        TemplateFooter(data)
    }
}

@Composable
fun FreshProfessionalTemplate(data: ResumeData) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().background(ToolTeal).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(data.personalDetails.fullName, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                Text(data.personalDetails.professionalTitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
            }
            if (data.photo != null) {
                androidx.compose.foundation.Image(
                    bitmap = data.photo.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            if (data.showObjective) {
                Text(data.objective, fontSize = 10.sp, color = Color.DarkGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
            
            TemplateSectionTitle("Experience", ToolTeal)
            data.experience.forEach { exp ->
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Text(exp.jobTitle, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("${exp.company} | ${exp.period}", fontSize = 9.sp, color = ToolTeal)
                }
            }

            TemplateSectionTitle("Education", ToolTeal)
            data.education.forEach { edu ->
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Text(edu.degree, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("${edu.school} | ${edu.year}", fontSize = 9.sp, color = ToolTeal)
                }
            }
            TemplateFooter(data)
        }
    }
}

@Composable
fun PreviewDetailRow(label: String, value: String) {
    if (value.isBlank() || value.contains("null")) return
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text("$label:", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Color.Black, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 10.sp, color = Color.DarkGray)
    }
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    } catch (e: Exception) {
        null
    }
}

suspend fun exportResumeToPdf(
    context: Context,
    data: ResumeData,
    template: ResumeTemplate,
    fileName: String
): SaveResult? = withContext(Dispatchers.IO) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72 DPI
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Background
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Accent header color based on template
        val accentColor = when (template) {
            ResumeTemplate.FORMAL_CURVE -> android.graphics.Color.rgb(109, 40, 217)
            ResumeTemplate.ACCENT_EXECUTIVE -> android.graphics.Color.rgb(31, 41, 55)
            ResumeTemplate.FRESH_PROFESSIONAL -> android.graphics.Color.rgb(13, 148, 136)
            ResumeTemplate.PROFILE_HEADER -> android.graphics.Color.rgb(234, 88, 12)
            ResumeTemplate.PROFESSIONAL_SIDEBAR -> android.graphics.Color.rgb(51, 65, 85)
            ResumeTemplate.BIO_DATA_CLASSIC -> android.graphics.Color.rgb(17, 24, 39)
        }

        var currentY = 45f

        // Top Header
        paint.color = accentColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        val fullNameText = "${data.personalDetails.title} ${data.personalDetails.fullName}".ifBlank { "FULL NAME" }.uppercase()
        canvas.drawText(fullNameText, 45f, currentY + 15f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        paint.color = android.graphics.Color.DKGRAY
        val titleText = data.personalDetails.professionalTitle.ifBlank { "Professional Title" }
        canvas.drawText(titleText, 45f, currentY + 32f, paint)

        paint.textSize = 9.5f
        paint.color = android.graphics.Color.GRAY
        val contactText = "Phone: ${data.personalDetails.phone}   |   Email: ${data.personalDetails.email}"
        canvas.drawText(contactText, 45f, currentY + 46f, paint)

        // Draw photo if provided
        if (data.photo != null) {
            val photoDest = Rect(475, 35, 550, 110)
            canvas.drawBitmap(data.photo, null, photoDest, null)
            val borderPaint = Paint().apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRect(photoDest, borderPaint)
        }

        currentY += 62f
        paint.color = accentColor
        paint.strokeWidth = 1.5f
        canvas.drawLine(45f, currentY, 550f, currentY, paint)
        currentY += 16f

        fun drawSectionTitle(title: String) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            paint.color = accentColor
            canvas.drawText(title.uppercase(), 45f, currentY, paint)
            currentY += 4f
            paint.color = android.graphics.Color.LTGRAY
            paint.strokeWidth = 0.8f
            canvas.drawLine(45f, currentY, 550f, currentY, paint)
            currentY += 14f
        }

        fun drawWrappedText(text: String, x: Float, maxWidthChars: Int = 80, textSize: Float = 9.5f, color: Int = android.graphics.Color.DKGRAY) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = textSize
            paint.color = color
            val lines = text.chunked(maxWidthChars)
            lines.forEach { line ->
                canvas.drawText(line, x, currentY, paint)
                currentY += 13f
            }
        }

        // Objective
        if (data.showObjective && data.objective.isNotBlank()) {
            drawSectionTitle("Career Objective")
            drawWrappedText(data.objective, 45f, 85)
            currentY += 6f
        }

        // Experience
        if (data.showExperience && data.experience.isNotEmpty()) {
            drawSectionTitle("Work Experience")
            data.experience.forEach { exp ->
                if (exp.jobTitle.isNotBlank() || exp.company.isNotBlank()) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 10.5f
                    paint.color = android.graphics.Color.BLACK
                    canvas.drawText("${exp.jobTitle} - ${exp.company}", 45f, currentY, paint)
                    currentY += 12f

                    if (exp.period.isNotBlank()) {
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                        paint.textSize = 9f
                        paint.color = android.graphics.Color.GRAY
                        canvas.drawText(exp.period, 45f, currentY, paint)
                        currentY += 12f
                    }

                    if (exp.details.isNotBlank()) {
                        drawWrappedText(exp.details, 45f, 85, 9f)
                    }
                    currentY += 4f
                }
            }
            currentY += 4f
        }

        // Education
        if (data.showEducation && data.education.isNotEmpty()) {
            drawSectionTitle("Education")
            data.education.forEach { edu ->
                if (edu.degree.isNotBlank() || edu.school.isNotBlank()) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 10f
                    paint.color = android.graphics.Color.BLACK
                    val eduLine = "${edu.degree}  -  ${edu.school}"
                    canvas.drawText(eduLine, 45f, currentY, paint)
                    currentY += 12f

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 9f
                    paint.color = android.graphics.Color.DKGRAY
                    val detailsLine = "Year: ${edu.year.ifBlank { "N/A" }}   |   Result: ${edu.result.ifBlank { "N/A" }}"
                    canvas.drawText(detailsLine, 45f, currentY, paint)
                    currentY += 14f
                }
            }
            currentY += 4f
        }

        // Skills & Languages
        if (data.showSkills && (data.skills.isNotEmpty() || data.languages.isNotEmpty())) {
            drawSectionTitle("Skills & Languages")
            if (data.skills.isNotEmpty()) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 9.5f
                paint.color = android.graphics.Color.BLACK
                canvas.drawText("Skills:", 45f, currentY, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = android.graphics.Color.DKGRAY
                drawWrappedText(data.skills.joinToString(", "), 95f, 75)
            }
            if (data.languages.isNotEmpty()) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 9.5f
                paint.color = android.graphics.Color.BLACK
                canvas.drawText("Languages:", 45f, currentY, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = android.graphics.Color.DKGRAY
                drawWrappedText(data.languages.joinToString(", "), 115f, 70)
            }
            currentY += 4f
        }

        // Personal Details
        drawSectionTitle("Personal Details")
        fun drawDetailItem(label: String, value: String) {
            if (value.isBlank()) return
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            paint.color = android.graphics.Color.BLACK
            canvas.drawText("$label:", 45f, currentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText(value, 160f, currentY, paint)
            currentY += 13f
        }

        drawDetailItem("Father / Guardian Name", data.personalDetails.fatherName)
        drawDetailItem("Date of Birth", data.personalDetails.dob)
        drawDetailItem("Gender", data.personalDetails.gender)
        drawDetailItem("Marital Status", data.personalDetails.maritalStatus)
        drawDetailItem("Nationality", data.personalDetails.nationality.ifBlank { "Indian" })
        val addressFull = "${data.personalDetails.houseNo}, ${data.personalDetails.landmark}, ${data.personalDetails.area}, ${data.personalDetails.stateCity} - ${data.personalDetails.pincode}".trim(',', ' ', '-')
        drawDetailItem("Address", addressFull)

        if (data.additionalInfo.isNotBlank()) {
            currentY += 4f
            drawSectionTitle("Additional Information")
            drawWrappedText(data.additionalInfo, 45f, 85)
        }

        // Bottom Footer (Date & Place on left, Signature on right)
        val footerY = 790f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("Date: ${data.date.ifBlank { "___________" }}", 45f, footerY, paint)
        canvas.drawText("Place: ${data.place.ifBlank { "___________" }}", 45f, footerY + 15f, paint)

        if (data.signature != null) {
            val sigDest = Rect(445, 745, 545, 785)
            canvas.drawBitmap(data.signature, null, sigDest, null)
        }
        paint.color = android.graphics.Color.BLACK
        paint.strokeWidth = 0.8f
        canvas.drawLine(440f, 790f, 550f, 790f, paint)
        paint.textSize = 8.5f
        canvas.drawText("Authorized Signature", 455f, 802f, paint)

        pdfDocument.finishPage(page)

        // Save to cache temp file
        val tempFile = File(context.cacheDir, "resume_export_${System.currentTimeMillis()}.pdf")
        tempFile.outputStream().use {
            pdfDocument.writeTo(it)
        }
        pdfDocument.close()

        // Use GlobalSaveManager to save to /storage/emulated/0/PDF_Master/
        val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        val result = GlobalSaveManager.saveProcessedFile(
            context = context,
            tempSourceFile = tempFile,
            desiredFileName = cleanName,
            mimeType = "application/pdf",
            category = "resume",
            tag = "Resume"
        )
        tempFile.delete()
        result
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
