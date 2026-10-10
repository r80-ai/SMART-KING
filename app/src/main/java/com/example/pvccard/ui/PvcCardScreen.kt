package com.example.pvccard.ui

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.pvccard.model.PVC_CARD_HEIGHT_PX
import com.example.pvccard.model.PVC_CARD_WIDTH_PX
import com.example.pvccard.model.PvcCardTemplate
import com.example.pvccard.model.PvcSheetLayout
import com.example.pvccard.model.PvcWorkflowStep
import com.example.pvccard.model.PvcCardItem
import com.example.pvccard.model.RectFNorm
import com.example.pvccard.viewmodel.PvcCardUiState
import com.example.pvccard.viewmodel.PvcCardViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvcCardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PvcCardViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusNotification) {
        uiState.statusNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar("Error: $err")
            viewModel.clearNotification()
        }
    }

    BackHandler {
        when (uiState.workflowStep) {
            PvcWorkflowStep.PRINT_SHEET -> viewModel.setWorkflowStep(PvcWorkflowStep.ENHANCE_PREVIEW)
            PvcWorkflowStep.ENHANCE_PREVIEW -> viewModel.setWorkflowStep(PvcWorkflowStep.MANUAL_CROP)
            PvcWorkflowStep.MANUAL_CROP -> viewModel.setWorkflowStep(PvcWorkflowStep.IMPORT)
            PvcWorkflowStep.IMPORT -> onBack()
        }
    }

    // PDF document picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadPdf(context, uri)
        }
    }

    // Scanned image picker (Front / Full sheet)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.loadImage(context, uri, isBackImage = false)
        }
    }

    // Separate Back image picker
    val backImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.loadImage(context, uri, isBackImage = true)
        }
    }

    // Export crop preset JSON launcher
    val exportPresetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = viewModel.generatePresetJson()
            viewModel.writePresetToFile(context, uri, json)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("pvc_card_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PVC Smart Card Maker", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "CR80 Standard (86 × 54 mm @ 300 DPI)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("pvc_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Step Navigation Bar
            PvcWorkflowStepBar(
                currentStep = uiState.workflowStep,
                onStepSelected = { viewModel.setWorkflowStep(it) }
            )

            // Content per workflow step
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState.workflowStep) {
                    PvcWorkflowStep.IMPORT -> {
                        ImportAndAutoCropStep(
                            uiState = uiState,
                            onPickPdf = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            onPickImage = { imagePickerLauncher.launch("image/*") },
                            onPickBackImage = { backImagePickerLauncher.launch("image/*") },
                            onSelectTemplate = { viewModel.selectTemplate(context, it) },
                            onAutoExtract = { viewModel.autoExtractFromDocument() },
                            onGoToManual = { viewModel.setWorkflowStep(PvcWorkflowStep.MANUAL_CROP) }
                        )
                    }

                    PvcWorkflowStep.MANUAL_CROP -> {
                        ManualCropAndStraightenStep(
                            uiState = uiState,
                            onStraightenLine = { x1, y1, x2, y2 -> viewModel.calculateAndApplyStraighten(x1, y1, x2, y2) },
                            onRotate = { viewModel.rotateByAngle(90f) },
                            onSetRotationAngle = { viewModel.setRotationAngle(it) },
                            onAdjustRotation = { viewModel.adjustRotationAngle(it) },
                            onResetRotation = { viewModel.resetRotation() },
                            onSideChanged = { viewModel.setActiveCropSide(it) },
                            onCropBoxChanged = { side, rect -> viewModel.updateCropBox(side, rect) },
                            onNudge = { side, dxNorm, dyNorm -> viewModel.nudgeCropBox(side, dxNorm, dyNorm) },
                            onResize = { side, dwNorm, dhNorm -> viewModel.resizeCropBox(side, dwNorm, dhNorm) },
                            onSavePreset = { viewModel.saveCropPreset(context) },
                            onResetPreset = { viewModel.resetCropPreset(context) },
                            onExportPreset = { exportPresetLauncher.launch("${uiState.selectedTemplate.id}_crop_preset.json") },
                            onConfirmCrop = { viewModel.confirmManualCrop(it) },
                            onCropBothSides = { viewModel.cropBothSides() },
                            onSwapSides = { viewModel.swapFrontAndBack() },
                            onNextStep = { viewModel.setWorkflowStep(PvcWorkflowStep.ENHANCE_PREVIEW) }
                        )
                    }

                    PvcWorkflowStep.ENHANCE_PREVIEW -> {
                        EnhanceAndPreviewStep(
                            uiState = uiState,
                            onBrightnessChanged = { viewModel.setBrightness(it) },
                            onContrastChanged = { viewModel.setContrast(it) },
                            onToggleTextDarken = { viewModel.toggleTextDarken() },
                            onToggleBorder = { viewModel.toggleBorder() },
                            onCopiesChanged = { viewModel.setCardCopies(it) },
                            onNameChanged = { viewModel.setCardName(it) },
                            onSwapSides = { viewModel.swapFrontAndBack() },
                            onAddToQueue = { viewModel.addCurrentCardToQueue() },
                            onGoToSheet = { viewModel.setWorkflowStep(PvcWorkflowStep.PRINT_SHEET) }
                        )
                    }

                    PvcWorkflowStep.PRINT_SHEET -> {
                        PrintSheetStep(
                            uiState = uiState,
                            onLayoutChanged = { viewModel.setSheetLayout(it) },
                            onPageChanged = { viewModel.setPreviewSheetPage(it) },
                            onRemoveCard = { viewModel.removeCardFromQueue(it) },
                            onQueueCopiesDelta = { id, delta -> viewModel.updateQueueCardCopies(id, delta) },
                            onExportPdf = { viewModel.exportPdf(context) },
                            onExportImage = { viewModel.exportImage(context) },
                            onDirectPrint = { viewModel.printSheet(context) }
                        )
                    }
                }

                // Global Processing Overlay
                if (uiState.isProcessing) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = CrimsonPrimary)
                                    Text(uiState.processingMessage, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Password Protected PDF Dialog
    if (uiState.isPasswordProtected) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPasswordDialog() },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonPrimary) },
            title = { Text("Password Protected PDF") },
            text = {
                Column {
                    if (!uiState.sourceDocumentFileName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = uiState.sourceDocumentFileName ?: "Document.pdf",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text(
                        "This PDF file is encrypted with a password. Please enter the password to unlock.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Hint: For e-Aadhaar, password is first 4 letters of your name in CAPITAL + year of birth (e.g. RAHU1990).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            uiState.errorMessage ?: "",
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.passwordInput,
                        onValueChange = { viewModel.updatePasswordInput(it) },
                        label = { Text("PDF Password") },
                        singleLine = true,
                        trailingIcon = {
                            if (uiState.passwordInput.isNotEmpty()) {
                                TextButton(onClick = { viewModel.updatePasswordInput(uiState.passwordInput.uppercase()) }) {
                                    Text("CAPS", fontSize = 11.sp, color = CrimsonPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("pdf_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        uiState.sourceDocumentUri?.let { uri ->
                            viewModel.loadPdf(context, uri, uiState.passwordInput.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("btn_unlock_pdf")
                ) {
                    Text("Unlock & Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPasswordDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * 4-Step horizontal navigation tabs with active badges.
 */
@Composable
private fun PvcWorkflowStepBar(
    currentStep: PvcWorkflowStep,
    onStepSelected: (PvcWorkflowStep) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PvcWorkflowStep.entries.forEach { step ->
                val isSelected = currentStep == step
                val chipColor = if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = chipColor,
                    modifier = Modifier
                        .clickable { onStepSelected(step) }
                        .testTag("step_tab_${step.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = step.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 1: Import Document & Canonical One-Tap Auto-Crop
 */
@Composable
private fun ImportAndAutoCropStep(
    uiState: PvcCardUiState,
    onPickPdf: () -> Unit,
    onPickImage: () -> Unit,
    onPickBackImage: () -> Unit,
    onSelectTemplate: (PvcCardTemplate) -> Unit,
    onAutoExtract: () -> Unit,
    onGoToManual: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Canonical Card Template Presets
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "1. Select Card Preset",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Preset coordinates automatically isolate Front & Back at 300 DPI",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PvcCardTemplate.entries) { template ->
                            val isSelected = uiState.selectedTemplate == template
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectTemplate(template) },
                                label = { Text(template.displayName, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("preset_${template.id}")
                            )
                        }
                    }
                }
            }
        }

        // 2. Document Source Picker Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "2. Select Card Source File",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Pick original PDF (e-Aadhaar, PAN, Voter) or high-res scanned image",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPickPdf,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.weight(1f).testTag("btn_pick_pdf"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick PDF", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onPickImage,
                            modifier = Modifier.weight(1f).testTag("btn_pick_image"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Image", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onPickBackImage,
                        modifier = Modifier.fillMaxWidth().testTag("btn_pick_back_image"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Separate Back Side Image (Optional)", fontSize = 12.sp)
                    }
                }
            }
        }

        // Document Loaded Preview & Actions
        if (uiState.sourceDocumentBitmap != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Loaded Document Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "${uiState.sourceDocumentBitmap.width} × ${uiState.sourceDocumentBitmap.height} px",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                .background(Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = uiState.sourceDocumentBitmap.asImageBitmap(),
                                contentDescription = "Loaded document",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = onGoToManual,
                            modifier = Modifier.fillMaxWidth().testTag("btn_manual_deskew"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manual De-Skew & Custom Crop", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 2: Manual De-Skew & Locked Aspect-Ratio Crop with Nudge D-Pad and Canonical Presets
 */
@Composable
private fun ManualCropAndStraightenStep(
    uiState: PvcCardUiState,
    onStraightenLine: (Float, Float, Float, Float) -> Unit,
    onRotate: () -> Unit,
    onSetRotationAngle: (Float) -> Unit,
    onAdjustRotation: (Float) -> Unit,
    onResetRotation: () -> Unit,
    onSideChanged: (String) -> Unit,
    onCropBoxChanged: (String, RectFNorm) -> Unit,
    onNudge: (String, Float, Float) -> Unit,
    onResize: (String, Float, Float) -> Unit,
    onSavePreset: () -> Unit,
    onResetPreset: () -> Unit,
    onExportPreset: () -> Unit,
    onConfirmCrop: (String) -> Unit,
    onCropBothSides: () -> Unit,
    onSwapSides: () -> Unit,
    onNextStep: () -> Unit
) {
    val doc = uiState.sourceDocumentBitmap
    if (doc == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please import a document in Step 1 first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var isStraightenMode by remember { mutableStateOf(false) }
    var showStraightenPanel by remember { mutableStateOf(false) }
    var lineStart by remember { mutableStateOf<Offset?>(null) }
    var lineEnd by remember { mutableStateOf<Offset?>(null) }
    var dpadOffsetX by remember { mutableStateOf(0f) }
    var dpadOffsetY by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Active Card Preset Badge & Tool Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Crop,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = uiState.selectedTemplate.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onSavePreset,
                    modifier = Modifier.testTag("btn_save_crop_preset")
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Lock & Save Crop Position",
                        tint = CrimsonPrimary
                    )
                }
                IconButton(
                    onClick = onResetPreset,
                    modifier = Modifier.testTag("btn_reset_crop_preset")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset Crop Position",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onExportPreset,
                    modifier = Modifier.testTag("btn_export_crop_preset")
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Export Preset to File",
                        tint = ToolBlue
                    )
                }
                IconButton(
                    onClick = { showStraightenPanel = !showStraightenPanel },
                    modifier = Modifier.testTag("btn_toggle_straighten_panel")
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Angle & Straighten",
                        tint = if (showStraightenPanel || uiState.rotationDegrees != 0f) CrimsonPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 3. Expandable Straighten / De-Skew Panel
        AnimatedVisibility(visible = showStraightenPanel) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Straighten / কোণ: ${String.format("%.1f", uiState.rotationDegrees)}°",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = onResetRotation,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_reset_rotation")
                            ) {
                                Text("Reset (0°)", fontSize = 11.sp, color = CrimsonPrimary)
                            }
                            IconButton(
                                onClick = { isStraightenMode = !isStraightenMode },
                                modifier = Modifier.size(28.dp).testTag("btn_toggle_straighten_line")
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Draw Edge Line",
                                    tint = if (isStraightenMode) CrimsonPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Slider(
                        value = uiState.rotationDegrees.coerceIn(-15f, 15f),
                        onValueChange = { onSetRotationAngle(it) },
                        valueRange = -15f..15f,
                        colors = SliderDefaults.colors(
                            thumbColor = CrimsonPrimary,
                            activeTrackColor = CrimsonPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("slider_straighten_angle")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(-1.0f, -0.2f, 0.2f, 1.0f).forEach { step ->
                            OutlinedButton(
                                onClick = { onAdjustRotation(step) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("btn_angle_step_$step")
                            ) {
                                Text(if (step > 0) "+$step°" else "$step°", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        if (isStraightenMode) {
            Surface(
                color = CrimsonPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    "Straighten Mode: Drag a line along any tilted card edge to auto-level to 0°.",
                    color = CrimsonPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Interactive Document Canvas & Floating 4-Way Nudge D-Pad
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .background(Color.Black.copy(alpha = 0.05f))
        ) {
            val canvasW = constraints.maxWidth.toFloat()
            val canvasH = constraints.maxHeight.toFloat()

            val imageAspect = doc.width.toFloat() / doc.height.toFloat()
            val containerAspect = canvasW / canvasH

            val (dispW, dispH) = if (imageAspect > containerAspect) {
                Pair(canvasW, canvasW / imageAspect)
            } else {
                Pair(canvasH * imageAspect, canvasH)
            }
            val offsetX = (canvasW - dispW) / 2f
            val offsetY = (canvasH - dispH) / 2f

            val activeNorm = if (uiState.activeCropSide == "FRONT") uiState.frontCropNorm else uiState.backCropNorm

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isStraightenMode, uiState.activeCropSide) {
                        detectDragGestures(
                            onDragStart = { start ->
                                if (isStraightenMode) {
                                    lineStart = start
                                    lineEnd = start
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (isStraightenMode) {
                                    lineEnd = change.position
                                } else {
                                    val dNormX = dragAmount.x / dispW
                                    val dNormY = dragAmount.y / dispH
                                    val maxLeft = (1f - activeNorm.width).coerceAtLeast(0f)
                                    val maxTop = (1f - activeNorm.height).coerceAtLeast(0f)
                                    val newLeft = (activeNorm.left + dNormX).coerceIn(0f, maxLeft)
                                    val newTop = (activeNorm.top + dNormY).coerceIn(0f, maxTop)
                                    onCropBoxChanged(uiState.activeCropSide, activeNorm.copy(left = newLeft, top = newTop))
                                }
                            },
                            onDragEnd = {
                                if (isStraightenMode) {
                                    val p1 = lineStart
                                    val p2 = lineEnd
                                    if (p1 != null && p2 != null) {
                                        onStraightenLine(p1.x, p1.y, p2.x, p2.y)
                                    }
                                    lineStart = null
                                    lineEnd = null
                                    isStraightenMode = false
                                }
                            }
                        )
                    }
            ) {
                // 1. Draw Document Image
                val destRect = androidx.compose.ui.unit.IntRect(
                    offsetX.toInt(),
                    offsetY.toInt(),
                    (offsetX + dispW).toInt(),
                    (offsetY + dispH).toInt()
                )
                drawImage(
                    image = doc.asImageBitmap(),
                    dstOffset = androidx.compose.ui.unit.IntOffset(destRect.left, destRect.top),
                    dstSize = androidx.compose.ui.unit.IntSize(destRect.width, destRect.height)
                )

                // 2. Draw Crop Box locked to CR80 aspect ratio (1012 × 638 px)
                val boxX = offsetX + activeNorm.left * dispW
                val boxY = offsetY + activeNorm.top * dispH
                val boxW = activeNorm.width * dispW
                val boxH = activeNorm.height * dispH

                // Dim overlay outside crop box (4 rectangles)
                val dimColor = Color.Black.copy(alpha = 0.45f)
                drawRect(color = dimColor, topLeft = Offset(0f, 0f), size = Size(size.width, boxY))
                drawRect(color = dimColor, topLeft = Offset(0f, boxY + boxH), size = Size(size.width, size.height - (boxY + boxH)))
                drawRect(color = dimColor, topLeft = Offset(0f, boxY), size = Size(boxX, boxH))
                drawRect(color = dimColor, topLeft = Offset(boxX + boxW, boxY), size = Size(size.width - (boxX + boxW), boxH))

                rotate(
                    degrees = uiState.rotationDegrees,
                    pivot = Offset(boxX + boxW / 2f, boxY + boxH / 2f)
                ) {
                    // Draw Crop Boundary
                    drawRect(
                        color = if (uiState.activeCropSide == "FRONT") CrimsonPrimary else ToolBlue,
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw Corner Handles
                    val handleRadius = 8.dp.toPx()
                    val handleColor = if (uiState.activeCropSide == "FRONT") CrimsonPrimary else ToolBlue
                    
                    val handlePositions = listOf(
                        Offset(boxX, boxY), // TL
                        Offset(boxX + boxW, boxY), // TR
                        Offset(boxX + boxW, boxY + boxH), // BR
                        Offset(boxX, boxY + boxH), // BL
                        Offset(boxX + boxW / 2f, boxY), // T
                        Offset(boxX + boxW, boxY + boxH / 2f), // R
                        Offset(boxX + boxW / 2f, boxY + boxH), // B
                        Offset(boxX, boxY + boxH / 2f) // L
                    )

                    handlePositions.forEach { pos ->
                        drawCircle(
                            color = Color.White,
                            radius = handleRadius,
                            center = pos
                        )
                        drawCircle(
                            color = handleColor,
                            radius = handleRadius * 0.7f,
                            center = pos
                        )
                    }
                }

                // 3. Draw Straighten Line if active
                if (isStraightenMode && lineStart != null && lineEnd != null) {
                    drawLine(
                        color = Color.Yellow,
                        start = lineStart!!,
                        end = lineEnd!!,
                        strokeWidth = 4.dp.toPx()
                    )
                }
            }

            // Movable Floating Translucent 8-Way Nudge & Resize D-Pad
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(dpadOffsetX.toInt(), dpadOffsetY.toInt()) }
                    .padding(10.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            dpadOffsetX += dragAmount.x
                            dpadOffsetY += dragAmount.y
                        }
                    }
                    .testTag("floating_nudge_dpad")
            ) {
                PvcNudgeDpad(
                    onNudge = { dxPixels, dyPixels ->
                        val dxNorm = dxPixels / dispW
                        val dyNorm = dyPixels / dispH
                        onNudge(uiState.activeCropSide, dxNorm, dyNorm)
                    },
                    onResize = { dwPixels, dhPixels ->
                        val dNormW = dwPixels / dispW
                        val dNormH = dhPixels / dispH
                        onResize(uiState.activeCropSide, dNormW, dNormH)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Card Sides Visual Status & Swap Strip
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Front Mini Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSideChanged("FRONT") }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.frontBitmap != null) {
                        Image(
                            bitmap = uiState.frontBitmap.asImageBitmap(),
                            contentDescription = "Front cropped",
                            modifier = Modifier
                                .size(46.dp, 29.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.5.dp, CrimsonPrimary, RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp, 29.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Front", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                    Column {
                        Text("সামনে (Front)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (uiState.frontBitmap != null) "✓ প্রস্তুত" else "ক্রপ করুন",
                            fontSize = 10.sp,
                            color = if (uiState.frontBitmap != null) ToolGreen else CrimsonPrimary
                        )
                    }
                }

                // Swap Button in center
                IconButton(
                    onClick = onSwapSides,
                    modifier = Modifier.size(36.dp).testTag("btn_swap_front_back")
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Swap Front & Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Back Mini Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSideChanged("BACK") }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.backBitmap != null) {
                        Image(
                            bitmap = uiState.backBitmap.asImageBitmap(),
                            contentDescription = "Back cropped",
                            modifier = Modifier
                                .size(46.dp, 29.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.5.dp, ToolBlue, RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp, 29.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Back", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                    Column {
                        Text("পেছনে (Back)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (uiState.backBitmap != null) "✓ প্রস্তুত" else "ক্রপ করুন",
                            fontSize = 10.sp,
                            color = if (uiState.backBitmap != null) ToolGreen else ToolBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onConfirmCrop(uiState.activeCropSide) },
                modifier = Modifier.weight(1f).testTag("btn_confirm_crop"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (uiState.activeCropSide == "FRONT") "Crop Front" else "Crop Back",
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onNextStep,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.weight(1f).testTag("btn_next_enhance"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Next ➔", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Upgraded 8-way interactive floating D-Pad controller for micro-adjusting position and resizing crop box.
 */
@Composable
private fun PvcNudgeDpad(
    onNudge: (dxPixels: Float, dyPixels: Float) -> Unit,
    onResize: (dwPixels: Float, dhPixels: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var stepPixels by remember { mutableStateOf(2f) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Row: Outer Top (+H) & Inner Top (▲)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                DpadHoldButton(
                    symbol = "+H",
                    description = "Increase Height",
                    onClick = { onResize(0f, stepPixels) },
                    modifier = Modifier.testTag("btn_expand_height")
                )
                DpadHoldButton(
                    symbol = "▲",
                    description = "Move Up",
                    onClick = { onNudge(0f, -stepPixels) },
                    modifier = Modifier.testTag("btn_nudge_up")
                )
            }

            // Middle Row: Left group, Center Step Button, Right group
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outer Left (-W) & Inner Left (◀)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    DpadHoldButton(
                        symbol = "-W",
                        description = "Decrease Width",
                        onClick = { onResize(-stepPixels, 0f) },
                        modifier = Modifier.testTag("btn_shrink_width")
                    )
                    DpadHoldButton(
                        symbol = "◀",
                        description = "Move Left",
                        onClick = { onNudge(-stepPixels, 0f) },
                        modifier = Modifier.testTag("btn_nudge_left")
                    )
                }

                // Center Step Toggle Button (2px -> 5px -> 10px -> 2px)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .clickable {
                            stepPixels = when (stepPixels) {
                                2f -> 5f
                                5f -> 10f
                                else -> 2f
                            }
                        }
                        .testTag("btn_toggle_nudge_step"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${stepPixels.toInt()}px",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Inner Right (▶) & Outer Right (+W)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    DpadHoldButton(
                        symbol = "▶",
                        description = "Move Right",
                        onClick = { onNudge(stepPixels, 0f) },
                        modifier = Modifier.testTag("btn_nudge_right")
                    )
                    DpadHoldButton(
                        symbol = "+W",
                        description = "Increase Width",
                        onClick = { onResize(stepPixels, 0f) },
                        modifier = Modifier.testTag("btn_expand_width")
                    )
                }
            }

            // Bottom Row: Inner Down (▼) & Outer Bottom (-H)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                DpadHoldButton(
                    symbol = "▼",
                    description = "Move Down",
                    onClick = { onNudge(0f, stepPixels) },
                    modifier = Modifier.testTag("btn_nudge_down")
                )
                DpadHoldButton(
                    symbol = "-H",
                    description = "Decrease Height",
                    onClick = { onResize(0f, -stepPixels) },
                    modifier = Modifier.testTag("btn_shrink_height")
                )
            }
        }
    }
}

/**
 * D-Pad button supporting both instant tap and smooth continuous panning on hold.
 */
@Composable
private fun DpadHoldButton(
    symbol: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onClick()
            delay(240)
            while (isActive && isPressed) {
                onClick()
                delay(40)
            }
        }
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isPressed) CrimsonPrimary else Color.White.copy(alpha = 0.22f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


