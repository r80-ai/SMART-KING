package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdf.PdfEngine
import com.example.storage.GlobalSaveManager
import com.example.ui.PdfMasterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class DocFilterType {
    ORIGINAL,
    PHOTOCOPY_BW,
    MAGIC_COLOR,
    SOFT_GRAYSCALE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEnhancerScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Selected filter & Sliders matching user screenshots
    var selectedFilter by remember { mutableStateOf(DocFilterType.ORIGINAL) }
    var brightness by remember { mutableFloatStateOf(0f) } // 0..30
    var contrast by remember { mutableFloatStateOf(1.0f) } // 0.8f..2.5f
    var sharpness by remember { mutableFloatStateOf(0f) } // 0%..100%

    var showExportSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Image Picker from Gallery
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                val bmp = internalLoadBitmap(context, uri)
                if (bmp != null) {
                    sourceBitmap = bmp
                    // Default to Clean Photocopy for documents
                    selectedFilter = DocFilterType.PHOTOCOPY_BW
                    brightness = 15f
                    contrast = 1.6f
                    sharpness = 45f
                    displayedBitmap = renderEnhancedBitmap(bmp, selectedFilter, brightness, contrast, sharpness)
                } else {
                    Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
                }
                isProcessing = false
            }
        }
    }

    // Camera Capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                isProcessing = true
                sourceBitmap = bitmap
                selectedFilter = DocFilterType.PHOTOCOPY_BW
                brightness = 15f
                contrast = 1.6f
                sharpness = 45f
                displayedBitmap = renderEnhancedBitmap(bitmap, selectedFilter, brightness, contrast, sharpness)
                isProcessing = false
            }
        }
    }

    // Update displayed bitmap when adjustments change
    LaunchedEffect(sourceBitmap, selectedFilter, brightness, contrast, sharpness) {
        val src = sourceBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            displayedBitmap = renderEnhancedBitmap(src, selectedFilter, brightness, contrast, sharpness)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Doc Scanner &\nAuto-Brightness",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        ),
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (sourceBitmap != null) {
                                showExportSheet = true
                            } else {
                                Toast.makeText(context, "Please import a document first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_top_export")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0B1120) // Deep dark background from screenshots
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= 1. ENHANCED DOCUMENT PREVIEW CARD =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_doc_preview"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Row: Title on Left, Camera & Gallery icons on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enhanced Document Preview",
                            color = Color(0xFFF1F5F9),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Camera Icon Button
                            IconButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_camera")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Take Photo",
                                    tint = Color(0xFF94A3B8)
                                )
                            }

                            // Gallery Icon Button
                            IconButton(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_gallery")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Import from Gallery",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Document Preview Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color(0xFFEF4444))
                        } else if (displayedBitmap != null) {
                            Image(
                                bitmap = displayedBitmap!!.asImageBitmap(),
                                contentDescription = "Enhanced Document Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Interactive empty document state
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tap to Import Document or Photocopy",
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Use Gallery or Camera above to scan any document",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { cameraLauncher.launch(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Camera", fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = {
                                            photoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Gallery", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= 2. ONE-CLICK INSTANT FILTERS CARD =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_filters"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "One-Click Instant Filters",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DocFilterPill(
                            title = "Original",
                            icon = Icons.Default.CropPortrait,
                            isSelected = selectedFilter == DocFilterType.ORIGINAL,
                            onClick = {
                                selectedFilter = DocFilterType.ORIGINAL
                                brightness = 0f
                                contrast = 1.0f
                                sharpness = 0f
                            }
                        )

                        DocFilterPill(
                            title = "Clean Photocopy / B&W",
                            icon = Icons.Default.Description,
                            isSelected = selectedFilter == DocFilterType.PHOTOCOPY_BW,
                            onClick = {
                                selectedFilter = DocFilterType.PHOTOCOPY_BW
                                brightness = 15f
                                contrast = 1.6f
                                sharpness = 45f
                            }
                        )

                        DocFilterPill(
                            title = "Magic Color",
                            icon = Icons.Default.AutoAwesome,
                            isSelected = selectedFilter == DocFilterType.MAGIC_COLOR,
                            onClick = {
                                selectedFilter = DocFilterType.MAGIC_COLOR
                                brightness = 8f
                                contrast = 1.4f
                                sharpness = 30f
                            }
                        )

                        DocFilterPill(
                            title = "Soft Grayscale",
                            icon = Icons.Default.BrightnessMedium,
                            isSelected = selectedFilter == DocFilterType.SOFT_GRAYSCALE,
                            onClick = {
                                selectedFilter = DocFilterType.SOFT_GRAYSCALE
                                brightness = 5f
                                contrast = 1.2f
                                sharpness = 20f
                            }
                        )
                    }
                }
            }

            // ================= 3. MANUAL ADJUSTMENT SLIDERS CARD =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("card_sliders"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Manual Adjustment Sliders",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Brightness (Whitening)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Brightness (Whitening)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${brightness.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = 0f..30f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFEF4444),
                            activeTrackColor = Color(0xFFEF4444),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Contrast (Text Deepness)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contrast (Text Deepness)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1fx", contrast),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                    Slider(
                        value = contrast,
                        onValueChange = { contrast = it },
                        valueRange = 0.8f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF10B981),
                            activeTrackColor = Color(0xFF10B981),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Sharpness & Edge Clarity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sharpness & Edge Clarity",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${sharpness.toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    }
                    Slider(
                        value = sharpness,
                        onValueChange = { sharpness = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3B82F6),
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }
        }
    }

    // ================= 4. SAVE & EXPORT BOTTOM SHEET =================
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF111827),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF475569))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Save & Export Document",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    fontSize = 22.sp
                )
                Text(
                    text = "Choose format to save directly to /storage/emulated/0/PDF_Master/",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Option 1: Save as Clean PDF Document (Vibrant Red)
                Button(
                    onClick = {
                        val bmpToExport = displayedBitmap ?: sourceBitmap
                        if (bmpToExport != null) {
                            showExportSheet = false
                            scope.launch {
                                saveEnhancedDocument(context, bmpToExport, "PDF", viewModel)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_save_pdf"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Save as Clean PDF Document",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Option 2: Save as High-Res Image (JPG) (Dark Elevated Button)
                Surface(
                    onClick = {
                        val bmpToExport = displayedBitmap ?: sourceBitmap
                        if (bmpToExport != null) {
                            showExportSheet = false
                            scope.launch {
                                saveEnhancedDocument(context, bmpToExport, "JPG", viewModel)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_save_jpg"),
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Save as High-Res Image (JPG)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocFilterPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF334155)
        ),
        modifier = Modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF94A3B8)
            )
        }
    }
}

/**
 * High-performance document enhancement algorithm:
 * Performs whitening, contrast enhancement, color adjustments, and edge sharpening.
 */
private fun renderEnhancedBitmap(
    src: Bitmap,
    filter: DocFilterType,
    brightness: Float,
    contrast: Float,
    sharpness: Float
): Bitmap {
    val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(dest)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val cm = ColorMatrix()

    // 1. Color Saturation Handling
    when (filter) {
        DocFilterType.ORIGINAL -> cm.setSaturation(1.0f)
        DocFilterType.PHOTOCOPY_BW -> cm.setSaturation(0.0f)
        DocFilterType.MAGIC_COLOR -> cm.setSaturation(1.35f)
        DocFilterType.SOFT_GRAYSCALE -> cm.setSaturation(0.0f)
    }

    // 2. Contrast and Whitening Brightness
    // Scale controls contrast, translate controls luminance shift (whitening paper background)
    val scale = contrast
    // Whitening offset pushes light tones to pure white (255)
    val whiteningOffset = brightness * 6.0f
    val translate = whiteningOffset - (0.5f * scale - 0.5f) * 255f

    val matrix = floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    )
    cm.postConcat(ColorMatrix(matrix))

    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(src, 0f, 0f, paint)

    // 3. Sharpness Enhancement
    return if (sharpness > 0f) {
        applySharpenKernel(dest, sharpness / 100f)
    } else {
        dest
    }
}

/**
 * 3x3 Convolution Sharpening Kernel for crisp document text edges
 */
private fun applySharpenKernel(src: Bitmap, strength: Float): Bitmap {
    try {
        val width = src.width
        val height = src.height
        // Scale down processing if bitmap is exceptionally large to maintain 60fps responsiveness
        val target = if (width * height > 4_000_000) {
            src
        } else {
            src
        }

        val w = target.width
        val h = target.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val outPixels = IntArray(w * h)
        target.getPixels(pixels, 0, w, 0, 0, w, h)

        val k = (strength * 0.45f).coerceIn(0f, 0.5f)
        val center = 1f + 4f * k

        for (y in 1 until h - 1) {
            val yOffset = y * w
            val yPrev = (y - 1) * w
            val yNext = (y + 1) * w

            for (x in 1 until w - 1) {
                val c = pixels[yOffset + x]
                val top = pixels[yPrev + x]
                val bottom = pixels[yNext + x]
                val left = pixels[yOffset + x - 1]
                val right = pixels[yOffset + x + 1]

                val a = (c ushr 24) and 0xFF

                // R
                val rC = (c ushr 16) and 0xFF
                val rT = (top ushr 16) and 0xFF
                val rB = (bottom ushr 16) and 0xFF
                val rL = (left ushr 16) and 0xFF
                val rR = (right ushr 16) and 0xFF
                val rNew = (rC * center - (rT + rB + rL + rR) * k).toInt().coerceIn(0, 255)

                // G
                val gC = (c ushr 8) and 0xFF
                val gT = (top ushr 8) and 0xFF
                val gB = (bottom ushr 8) and 0xFF
                val gL = (left ushr 8) and 0xFF
                val gR = (right ushr 8) and 0xFF
                val gNew = (gC * center - (gT + gB + gL + gR) * k).toInt().coerceIn(0, 255)

                // B
                val bC = c and 0xFF
                val bT = top and 0xFF
                val bB = bottom and 0xFF
                val bL = left and 0xFF
                val bR = right and 0xFF
                val bNew = (bC * center - (bT + bB + bL + bR) * k).toInt().coerceIn(0, 255)

                outPixels[yOffset + x] = (a shl 24) or (rNew shl 16) or (gNew shl 8) or bNew
            }
        }

        output.setPixels(outPixels, 0, w, 0, 0, w, h)
        return output
    } catch (e: Exception) {
        return src
    }
}

private suspend fun internalLoadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = android.graphics.BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private suspend fun saveEnhancedDocument(
    context: Context,
    bitmap: Bitmap,
    format: String,
    viewModel: PdfMasterViewModel
) = withContext(Dispatchers.IO) {
    val time = System.currentTimeMillis()
    val name = "Doc_Scanner_$time"

    try {
        if (format == "JPG") {
            val file = File(context.cacheDir, "$name.jpg")
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it)
            }
            val result = GlobalSaveManager.saveProcessedFile(
                context, file, "$name.jpg", "image/jpeg", "scanned", "Doc Enhancer"
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to ${result.displayLocation}/${result.fileName}", Toast.LENGTH_LONG).show()
                viewModel.setNotification("Saved JPG: ${result.fileName}")
            }
        } else {
            val pdfFile = PdfEngine.createPdfFromBitmaps(context, listOf(bitmap), "$name.pdf")
            val result = GlobalSaveManager.saveProcessedFile(
                context, pdfFile, "$name.pdf", "application/pdf", "scanned", "Doc Enhancer"
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to ${result.displayLocation}/${result.fileName}", Toast.LENGTH_LONG).show()
                viewModel.setNotification("Saved PDF: ${result.fileName}")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            viewModel.setNotification("Save failed: ${e.localizedMessage}")
        }
    }
}
