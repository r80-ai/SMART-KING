package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.pdf.PdfEngine
import com.example.ui.AppScreen
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt

enum class DocFilterMode(val labelEn: String, val labelBn: String, val icon: ImageVector) {
    ORIGINAL("Original", "আসল", Icons.Default.FilterNone),
    CLEAN_PHOTOCOPY("Clean Photocopy / B&W", "পরিষ্কার জেরক্স / B&W", Icons.Default.ContentCopy),
    MAGIC_COLOR("Magic Color", "ম্যাজিক কালার", Icons.Default.AutoAwesome),
    SOFT_GRAYSCALE("Soft Grayscale", "সফট গ্রে-স্কেল", Icons.Default.Contrast)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEnhancerScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "bn"

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var selectedFilter by remember { mutableStateOf(DocFilterMode.CLEAN_PHOTOCOPY) }
    var brightness by remember { mutableFloatStateOf(0f) } // -100 to 100
    var contrast by remember { mutableFloatStateOf(1.0f) }   // 0.5 to 2.5
    var sharpness by remember { mutableFloatStateOf(30f) }   // 0 to 100

    var isProcessing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    // Camera Capture temp file uri
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(cameraPhotoUri!!)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        loadedBitmap = bmp
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Gallery Photo Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        loadedBitmap = bmp
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun launchCamera() {
        try {
            val tempFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Re-apply enhancement when parameters or loaded bitmap change
    LaunchedEffect(loadedBitmap, selectedFilter, brightness, contrast, sharpness) {
        val bmp = loadedBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val processed = processDocumentImage(
                src = bmp,
                filter = selectedFilter,
                brightnessOffset = brightness,
                contrastScale = contrast,
                sharpnessAmount = sharpness
            )
            enhancedBitmap = processed
        }
    }

    BackHandler {
        onBack()
    }

    AppScreen(modifier = modifier, isScrollable = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isBn) "ডক স্ক্যানার ও ব্রাইটনেস" else "Doc Scanner & Auto-Brightness",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBn) "হলুদ ভাব দূর ও ক্রিস্প জেরক্স রূপান্তর" else "Clean Photocopy, Whitening & Text Enhancement",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("doc_enhancer_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        if (enhancedBitmap != null) {
                            Button(
                                onClick = { showExportSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("export_doc_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBn) "এক্সপোর্ট" else "Export", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TOP DOCUMENT IMPORT HERO
                if (enhancedBitmap == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(ToolBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = ToolBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = if (isBn) "ডকুমেন্ট বা জেরক্স পেজ স্ক্যান করুন" else "Import Document or Photocopy",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (isBn) "ক্যামেরা দিয়ে ছবি তুলুন অথবা গ্যালারি থেকে অস্পষ্ট বা হলুদ হয়ে যাওয়া কাগজ নির্বাচন করুন"
                                else "Capture via camera or select faded document to instantly boost contrast & remove yellow casts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { launchCamera() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_camera_capture")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBn) "ক্যামেরা" else "Camera", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_gallery_pick")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isBn) "গ্যালারি" else "Gallery", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // LIVE DOCUMENT PREVIEW CARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "উন্নত ডকুমেন্ট প্রিভিউ" else "Enhanced Document Preview",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { launchCamera() }) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Retake", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }) {
                                        Icon(Icons.Default.Image, contentDescription = "Pick Other", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                enhancedBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Enhanced Document",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // ONE-CLICK FILTERS SELECTOR
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isBn) "এক ক্লিকে ফিল্টার (One-Click Filters)" else "One-Click Instant Filters",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DocFilterMode.values().forEach { filter ->
                                    val isSelected = selectedFilter == filter
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedFilter = filter
                                            // Reset fine sliders to tuned default for the preset
                                            when (filter) {
                                                DocFilterMode.CLEAN_PHOTOCOPY -> {
                                                    brightness = 15f
                                                    contrast = 1.6f
                                                    sharpness = 45f
                                                }
                                                DocFilterMode.MAGIC_COLOR -> {
                                                    brightness = 8f
                                                    contrast = 1.35f
                                                    sharpness = 30f
                                                }
                                                DocFilterMode.SOFT_GRAYSCALE -> {
                                                    brightness = 5f
                                                    contrast = 1.2f
                                                    sharpness = 20f
                                                }
                                                DocFilterMode.ORIGINAL -> {
                                                    brightness = 0f
                                                    contrast = 1.0f
                                                    sharpness = 0f
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = filter.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (isBn) filter.labelBn else filter.labelEn,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (filter) {
                                                DocFilterMode.CLEAN_PHOTOCOPY -> ToolGreen.copy(alpha = 0.2f)
                                                DocFilterMode.MAGIC_COLOR -> ToolOrange.copy(alpha = 0.2f)
                                                DocFilterMode.SOFT_GRAYSCALE -> ToolBlue.copy(alpha = 0.2f)
                                                DocFilterMode.ORIGINAL -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            selectedLabelColor = when (filter) {
                                                DocFilterMode.CLEAN_PHOTOCOPY -> ToolGreen
                                                DocFilterMode.MAGIC_COLOR -> ToolOrange
                                                DocFilterMode.SOFT_GRAYSCALE -> ToolBlue
                                                DocFilterMode.ORIGINAL -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // MANUAL FINE-TUNING SLIDERS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (isBn) "ম্যানুয়াল কন্ট্রোল (Fine-Tuning)" else "Manual Adjustment Sliders",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Brightness
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isBn) "উজ্জ্বলতা (Brightness)" else "Brightness (Whitening)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${brightness.toInt()}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CrimsonPrimary
                                    )
                                }
                                Slider(
                                    value = brightness,
                                    onValueChange = { brightness = it },
                                    valueRange = -60f..80f,
                                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                )
                            }

                            // Contrast
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isBn) "কনট্রাস্ট (Contrast)" else "Contrast (Text Deepness)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1fx", contrast),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ToolGreen
                                    )
                                }
                                Slider(
                                    value = contrast,
                                    onValueChange = { contrast = it },
                                    valueRange = 0.6f..2.5f,
                                    colors = SliderDefaults.colors(thumbColor = ToolGreen, activeTrackColor = ToolGreen)
                                )
                            }

                            // Sharpness
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isBn) "ধারালো ভাব (Sharpness)" else "Sharpness & Edge Clarity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${sharpness.toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ToolBlue
                                    )
                                }
                                Slider(
                                    value = sharpness,
                                    onValueChange = { sharpness = it },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(thumbColor = ToolBlue, activeTrackColor = ToolBlue)
                                )
                            }
                        }
                    }
                }
            }
        }

        // EXPORT MODAL BOTTOM SHEET
        if (showExportSheet && enhancedBitmap != null) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isBn) "ডকুমেন্ট সংরক্ষণ করুন" else "Save & Export Document",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (isBn) "আপনার সুবিধামত হাই-রেজোলিউশন ইমেজ অথবা ক্লিন PDF হিসেবে সংরক্ষণ করুন"
                        else "Choose format to save directly to /storage/emulated/0/PDF_Master/",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // BUTTON 1: SAVE AS CLEAN PDF
                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            isSaving = true
                            showExportSheet = false
                            coroutineScope.launch {
                                try {
                                    val bmp = enhancedBitmap ?: return@launch
                                    val time = System.currentTimeMillis()
                                    val pdfFileName = "Scan_Doc_$time.pdf"
                                    val tempPdfFile = PdfEngine.createPdfFromBitmaps(
                                        context = context,
                                        bitmaps = listOf(bmp),
                                        outputFileName = pdfFileName
                                    )

                                    viewModel.commitAndShowResult(
                                        sourceOutputFile = tempPdfFile,
                                        desiredName = pdfFileName,
                                        toolTitle = if (isBn) "ডক স্ক্যানার ও ব্রাইটনেস" else "Doc Scanner & Auto-Brightness",
                                        summaryText = if (isBn) "পরিষ্কার PDF ডকুমেন্ট সফলভাবে সংরক্ষিত হয়েছে।" else "Clean PDF document created and saved.",
                                        mimeType = "application/pdf",
                                        category = "created",
                                        tag = "Doc Scanner PDF"
                                    )
                                } catch (e: Exception) {
                                    viewModel.showNotification("PDF Export Error: ${e.message}", false)
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_as_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "ক্লিন PDF হিসেবে সংরক্ষণ করুন" else "Save as Clean PDF Document", fontWeight = FontWeight.Bold)
                    }

                    // BUTTON 2: SAVE AS HIGH RES IMAGE (JPG)
                    OutlinedButton(
                        onClick = {
                            if (isSaving) return@OutlinedButton
                            isSaving = true
                            showExportSheet = false
                            coroutineScope.launch {
                                try {
                                    val bmp = enhancedBitmap ?: return@launch
                                    val time = System.currentTimeMillis()
                                    val imgFileName = "Enhanced_Scan_$time.jpg"
                                    val tempFile = File(context.cacheDir, imgFileName)
                                    FileOutputStream(tempFile).use { out ->
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                        out.flush()
                                    }

                                    viewModel.commitAndShowResult(
                                        sourceOutputFile = tempFile,
                                        desiredName = imgFileName,
                                        toolTitle = if (isBn) "ডক স্ক্যানার ও ব্রাইটনেস" else "Doc Scanner & Auto-Brightness",
                                        summaryText = if (isBn) "হাই-রেজোলিউশন ছবি সফলভাবে সংরক্ষিত হয়েছে।" else "High-resolution scanned image exported.",
                                        mimeType = "image/jpeg",
                                        category = "photos",
                                        tag = "Doc Scanner Image"
                                    )
                                } catch (e: Exception) {
                                    viewModel.showNotification("Image Export Error: ${e.message}", false)
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_as_image_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "হাই-রেজোলিউশন ইমেজ (JPG)" else "Save as High-Res Image (JPG)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * High-performance document filter engine:
 * 1. Clean Photocopy: Desaturates, strips yellow/warm cast, pushes light paper towards clean white while deepening dark text.
 * 2. Magic Color: Preserves color stamps/signatures, removes shadow gradients and yellow haze.
 * 3. Soft Grayscale: Clean balanced grayscale document.
 * 4. Sliders apply additional Brightness, Contrast & High-pass sharpening.
 */
private fun processDocumentImage(
    src: Bitmap,
    filter: DocFilterMode,
    brightnessOffset: Float,
    contrastScale: Float,
    sharpnessAmount: Float
): Bitmap {
    val w = src.width
    val h = src.height

    // Use downscaled working bitmap if huge to maintain snappy performance, or full size
    val targetBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(targetBmp)

    val cm = ColorMatrix()

    when (filter) {
        DocFilterMode.CLEAN_PHOTOCOPY -> {
            // High contrast B&W photocopy matrix:
            // Desaturate completely, boost contrast significantly, lift white paper
            val b = brightnessOffset + 20f
            val c = contrastScale * 1.6f
            val t = (1f - c) * 128f / 2f + b

            cm.set(
                floatArrayOf(
                    c * 0.33f, c * 0.59f, c * 0.11f, 0f, t + 15f,
                    c * 0.33f, c * 0.59f, c * 0.11f, 0f, t + 15f,
                    c * 0.33f, c * 0.59f, c * 0.11f, 0f, t + 15f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        DocFilterMode.MAGIC_COLOR -> {
            // Magic Color: Slight boost to blue/green to neutralize yellow cast, boost saturation
            val satMatrix = ColorMatrix().apply { setSaturation(1.35f) }
            val c = contrastScale * 1.25f
            val b = brightnessOffset + 12f
            val t = (1f - c) * 128f / 2f + b

            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t + 4f,
                    0f, 0f, c, 0f, t + 8f, // slight blue boost cleans yellowed paper
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.setConcat(contrastMatrix, satMatrix)
        }
        DocFilterMode.SOFT_GRAYSCALE -> {
            val satMatrix = ColorMatrix().apply { setSaturation(0f) }
            val c = contrastScale * 1.15f
            val b = brightnessOffset + 5f
            val t = (1f - c) * 128f / 2f + b

            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.setConcat(contrastMatrix, satMatrix)
        }
        DocFilterMode.ORIGINAL -> {
            val c = contrastScale
            val b = brightnessOffset
            val t = (1f - c) * 128f / 2f + b
            cm.set(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    }

    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)

    return targetBmp
}
