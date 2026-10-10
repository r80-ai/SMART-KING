package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.storage.GlobalSaveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// Domain Models
data class PhotoSpec(val name: String, val widthMm: Float, val heightMm: Float, val group: String)
data class PaperSpec(val name: String, val widthMm: Float, val heightMm: Float)

enum class PaperLayoutMode(val displayName: String, val subtitle: String, val photosPerRow: Int) {
    PORTRAIT("Portrait", "3 photos / row", 3),
    LANDSCAPE("Landscape", "5 photos / row", 5);

    companion object {
        val LONG_PAPER get() = PORTRAIT
        val WIDE_PAPER get() = LANDSCAPE
    }
}

object PassportPrintLayoutEngine {
    fun is4x6Paper(paper: PaperSpec): Boolean {
        return (paper.name.contains("4", ignoreCase = true) && paper.name.contains("6", ignoreCase = true)) ||
                (minOf(paper.widthMm, paper.heightMm) in 95f..110f && maxOf(paper.widthMm, paper.heightMm) in 145f..160f)
    }

    fun getPaperRowCapacity(paper: PaperSpec, mode: PaperLayoutMode = PaperLayoutMode.PORTRAIT): Int {
        return if (is4x6Paper(paper)) {
            mode.photosPerRow
        } else if (paper.name.contains("A4", ignoreCase = true) || (paper.widthMm in 205f..215f)) {
            6
        } else {
            maxOf(1, (paper.widthMm / 35f).toInt())
        }
    }

    fun getPaperRowCapacity(mode: PaperLayoutMode): Int = mode.photosPerRow

    fun getPaperRowCapacity(paperName: String): Int {
        return when {
            paperName.contains("Landscape", ignoreCase = true) || paperName.contains("Wide", ignoreCase = true) -> 5
            paperName.contains("A4", ignoreCase = true) -> 6
            else -> 3
        }
    }

    fun getPaperDimensionsPx(paper: PaperSpec, mode: PaperLayoutMode = PaperLayoutMode.PORTRAIT, dpi: Int = 300): Pair<Int, Int> {
        val (wMm, hMm) = if (is4x6Paper(paper)) {
            when (mode) {
                PaperLayoutMode.PORTRAIT -> Pair(minOf(paper.widthMm, paper.heightMm), maxOf(paper.widthMm, paper.heightMm))
                PaperLayoutMode.LANDSCAPE -> Pair(maxOf(paper.widthMm, paper.heightMm), minOf(paper.widthMm, paper.heightMm))
            }
        } else {
            // Strictly fixed physical paper dimensions for all other papers (A4, 5x7, 6x8, A5, Custom)
            Pair(paper.widthMm, paper.heightMm)
        }
        val paperW = (wMm / 25.4f * dpi).toInt()
        val paperH = (hMm / 25.4f * dpi).toInt()
        return Pair(paperW, paperH)
    }

    fun getPaperDimensionsPx(paper: PaperSpec, dpi: Int = 300): Pair<Int, Int> {
        return getPaperDimensionsPx(paper, PaperLayoutMode.PORTRAIT, dpi)
    }

    fun calculateLongPaperHeightPx(
        photoHeightPx: Int,
        rowCount: Int,
        topMarginPx: Int,
        bottomMarginPx: Int,
        gapPx: Int
    ): Int {
        if (rowCount <= 0) return topMarginPx + bottomMarginPx
        return topMarginPx + (rowCount * photoHeightPx) + ((rowCount - 1) * gapPx) + bottomMarginPx
    }

    fun generatePrintLayouts(
        photo: Bitmap,
        paper: PaperSpec,
        withBorder: Boolean,
        dpi: Int = 300,
        totalCopies: Int,
        mode: PaperLayoutMode = PaperLayoutMode.PORTRAIT
    ): List<Bitmap> {
        return if (is4x6Paper(paper)) {
            generate4x6PrintLayouts(photo, paper, withBorder, dpi, totalCopies, mode)
        } else {
            generateStandardPrintLayouts(photo, paper, withBorder, dpi, totalCopies)
        }
    }

    fun generate4x6PrintLayouts(
        photo: Bitmap,
        paper: PaperSpec,
        withBorder: Boolean,
        dpi: Int = 300,
        totalCopies: Int,
        mode: PaperLayoutMode = PaperLayoutMode.PORTRAIT
    ): List<Bitmap> {
        if (totalCopies <= 0) return emptyList()

        val (paperW, paperH) = getPaperDimensionsPx(paper, mode, dpi)
        val cols = mode.photosPerRow

        // Practical small margins and gaps
        val margin = maxOf(4, (3.5f / 25.4f * dpi).toInt())
        val gap = maxOf(2, (2f / 25.4f * dpi).toInt())

        // Real 4 × 6 inch photo paper:
        // Portrait: 3 photos per row, 4 rows fit (max 12 photos per sheet)
        // Landscape: 5 photos per row, 3 rows fit (max 15 photos per sheet)
        val maxRows = if (mode == PaperLayoutMode.PORTRAIT) 4 else 3
        val availW = paperW - 2 * margin - (cols - 1) * gap
        val availH = paperH - 2 * margin - (maxRows - 1) * gap
        val maxSlotW = availW / cols
        val maxSlotH = availH / maxRows

        val aspect = photo.width.toFloat() / photo.height.toFloat()
        val drawW: Int
        val drawH: Int
        if (maxSlotW.toFloat() / aspect <= maxSlotH) {
            drawW = maxSlotW
            drawH = (drawW / aspect).toInt()
        } else {
            drawH = maxSlotH
            drawW = (drawH * aspect).toInt()
        }

        val photosPerPage = cols * maxRows
        val totalPages = maxOf(1, ceil(totalCopies.toDouble() / photosPerPage).toInt())

        val scaledPhoto = if (drawW != photo.width || drawH != photo.height) {
            Bitmap.createScaledBitmap(photo, drawW, drawH, true)
        } else {
            photo
        }

        val totalRowW = cols * drawW + (cols - 1) * gap
        val startX = (paperW - totalRowW) / 2
        val startY = margin

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = maxOf(2f, (0.5f / 25.4f * dpi))
        }

        val pages = mutableListOf<Bitmap>()
        var drawn = 0

        for (p in 0 until totalPages) {
            val page = Bitmap.createBitmap(paperW, paperH, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
            val canvas = Canvas(page)

            for (r in 0 until maxRows) {
                val py = startY + r * (drawH + gap)
                for (c in 0 until cols) {
                    if (drawn >= totalCopies) break
                    val px = startX + c * (drawW + gap)

                    canvas.drawBitmap(scaledPhoto, px.toFloat(), py.toFloat(), null)

                    if (withBorder) {
                        canvas.drawRect(
                            px.toFloat(),
                            py.toFloat(),
                            (px + drawW).toFloat(),
                            (py + drawH).toFloat(),
                            borderPaint
                        )
                    }
                    drawn++
                }
                if (drawn >= totalCopies && p == totalPages - 1) break
            }
            pages.add(page)
        }

        if (scaledPhoto != photo) {
            scaledPhoto.recycle()
        }

        return pages
    }

    fun generateStandardPrintLayouts(
        photo: Bitmap,
        paper: PaperSpec,
        withBorder: Boolean,
        dpi: Int = 300,
        totalCopies: Int
    ): List<Bitmap> {
        if (totalCopies <= 0) return emptyList()

        val paperW = (paper.widthMm / 25.4f * dpi).toInt()
        val paperH = (paper.heightMm / 25.4f * dpi).toInt()

        val isA4 = paper.name.contains("A4", ignoreCase = true) || (paper.widthMm in 205f..215f)
        val cols = if (isA4) {
            6
        } else {
            maxOf(1, (paper.widthMm / 35f).toInt())
        }

        // Photo size MUST remain strictly fixed: not enlarged, not stretched, aspect ratio preserved
        val drawW = photo.width
        val drawH = photo.height

        // Horizontal spacing: small, consistent white gap between adjacent photos
        val gapX = maxOf(2, (2f / 25.4f * dpi).toInt())
        val totalPhotosW = cols * drawW + (cols - 1) * gapX
        val startX = maxOf(0, (paperW - totalPhotosW) / 2)

        // Positioned near TOP of page
        val startY = maxOf(4, (6f / 25.4f * dpi).toInt())
        val gapY = maxOf(2, (2f / 25.4f * dpi).toInt())

        // Max rows that physically fit vertically on this fixed physical sheet
        val maxRows = maxOf(1, (paperH - startY) / (drawH + gapY))
        val photosPerPage = cols * maxRows
        val totalPages = maxOf(1, ceil(totalCopies.toDouble() / photosPerPage).toInt())

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = maxOf(2f, (0.5f / 25.4f * dpi))
        }

        val pages = mutableListOf<Bitmap>()
        var drawn = 0

        for (p in 0 until totalPages) {
            val page = Bitmap.createBitmap(paperW, paperH, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
            val canvas = Canvas(page)
            // Lower remaining area remains completely WHITE/EMPTY

            for (r in 0 until maxRows) {
                val py = startY + r * (drawH + gapY)
                for (c in 0 until cols) {
                    if (drawn >= totalCopies) break
                    val px = startX + c * (drawW + gapX)

                    canvas.drawBitmap(photo, px.toFloat(), py.toFloat(), null)

                    if (withBorder) {
                        canvas.drawRect(
                            px.toFloat(),
                            py.toFloat(),
                            (px + drawW).toFloat(),
                            (py + drawH).toFloat(),
                            borderPaint
                        )
                    }
                    drawn++
                }
                if (drawn >= totalCopies && p == totalPages - 1) break
            }
            pages.add(page)
        }

        return pages
    }

    fun generateFixedPrintLayouts(
        photo: Bitmap,
        paper: PaperSpec,
        withBorder: Boolean,
        dpi: Int = 300,
        totalCopies: Int,
        mode: PaperLayoutMode = PaperLayoutMode.PORTRAIT
    ): List<Bitmap> {
        return generatePrintLayouts(photo, paper, withBorder, dpi, totalCopies, mode)
    }

    fun generateLongPaperLayout(
        photo: Bitmap,
        paper: PaperSpec,
        withBorder: Boolean,
        dpi: Int = 300,
        totalCopies: Int
    ): List<Bitmap> {
        return generatePrintLayouts(photo, paper, withBorder, dpi, totalCopies, PaperLayoutMode.PORTRAIT)
    }

    fun generateWidePaperLayout(
        photo: Bitmap,
        paper: PaperSpec,
        withBorder: Boolean,
        dpi: Int = 300,
        totalCopies: Int
    ): List<Bitmap> {
        return generatePrintLayouts(photo, paper, withBorder, dpi, totalCopies, PaperLayoutMode.LANDSCAPE)
    }
}

val PHOTO_SPECS = listOf(
    PhotoSpec("Passport Photo", 35f, 45f, "ID & Government Photo"),
    PhotoSpec("Indian Passport Photo", 35f, 45f, "ID & Government Photo"),
    PhotoSpec("PAN Card – NSDL / Protean", 25f, 35f, "ID & Government Photo"),
    PhotoSpec("PAN Card – UTIITSL", 25f, 35f, "ID & Government Photo"),
    PhotoSpec("Aadhaar / ID Photo", 35f, 45f, "ID & Government Photo"),
    PhotoSpec("Visa Photo", 35f, 45f, "ID & Government Photo"),
    
    PhotoSpec("Job Application Photo", 35f, 45f, "Form Photo"),
    PhotoSpec("School Photo", 25f, 35f, "Form Photo"),
    PhotoSpec("College Photo", 35f, 45f, "Form Photo"),
    PhotoSpec("Stamp Size Photo", 20f, 25f, "Form Photo"),
    PhotoSpec("2 × 2 Photo", 50.8f, 50.8f, "Form Photo"), // 2x2 inch = 50.8 mm
    PhotoSpec("Wallet Photo", 54f, 86f, "Form Photo"),
    
    PhotoSpec("4 × 6 inch", 101.6f, 152.4f, "Photo Print"),
    PhotoSpec("5 × 7 inch", 127f, 177.8f, "Photo Print"),
    PhotoSpec("6 × 8 inch", 152.4f, 203.2f, "Photo Print"),
    PhotoSpec("8 × 10 inch", 203.2f, 254f, "Photo Print"),
    PhotoSpec("8 × 12 inch", 203.2f, 304.8f, "Photo Print"),
    PhotoSpec("10 × 12 inch", 254f, 304.8f, "Photo Print"),
    PhotoSpec("10 × 15 inch", 254f, 381f, "Photo Print"),
    PhotoSpec("12 × 18 inch", 304.8f, 457.2f, "Photo Print"),
    PhotoSpec("16 × 20 inch", 406.4f, 508f, "Photo Print"),
    PhotoSpec("16 × 24 inch", 406.4f, 609.6f, "Photo Print"),
    PhotoSpec("20 × 24 inch", 508f, 609.6f, "Photo Print"),
    PhotoSpec("20 × 30 inch", 508f, 762f, "Photo Print"),
    PhotoSpec("24 × 36 inch", 609.6f, 914.4f, "Photo Print"),
)

val PAPER_SPECS = listOf(
    PaperSpec("4 × 6 inch", 101.6f, 152.4f),
    PaperSpec("5 × 7 inch", 127f, 177.8f),
    PaperSpec("6 × 8 inch", 152.4f, 203.2f),
    PaperSpec("A4", 210f, 297f),
    PaperSpec("A5", 148f, 210f),
    PaperSpec("Custom", 210f, 297f) // placeholder
)

fun formatPhotoSpec(spec: PhotoSpec): String {
    val cmW = spec.widthMm / 10f
    val cmH = spec.heightMm / 10f
    val inW = spec.widthMm / 25.4f
    val inH = spec.heightMm / 25.4f
    return String.format(java.util.Locale.US, "%s\n%.1f × %.1f cm\n(%.1f × %.1f mm)\n(%.2f × %.2f inch)", spec.name, cmW, cmH, spec.widthMm, spec.heightMm, inW, inH)
}

@Composable
fun PassportPhotoScreen(viewModel: PdfMasterViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var selectedSpec by remember { mutableStateOf(PHOTO_SPECS.first()) }
    var customWidth by remember { mutableStateOf("35") }
    var customHeight by remember { mutableStateOf("45") }
    var isCustom by remember { mutableStateOf(false) }
    var customUnit by remember { mutableStateOf("mm") }
    var dpi by remember { mutableStateOf(300) }
    
    var selectedPaper by remember { mutableStateOf(PAPER_SPECS.first()) }
    var selectedPaperMode by remember { mutableStateOf(PaperLayoutMode.PORTRAIT) }
    var withBorder by remember { mutableStateOf(true) }
    var copiesText by remember { mutableStateOf("6") }
    var currentCopies by remember { mutableIntStateOf(6) }
    
    var showPreview by remember { mutableStateOf(false) }
    var generatedLayoutBmps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var croppedFaceBmp by remember { mutableStateOf<Bitmap?>(null) }
    
    // Preview pagination
    var currentPreviewPage by remember { mutableStateOf(0) }
    
    // Transformations
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var activeFramingMode by remember { mutableStateOf("fit") } // "fit", "original", "selected"
    var previewBoxWidth by remember { mutableFloatStateOf(1f) }
    var previewBoxHeight by remember { mutableFloatStateOf(1f) }
    
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        withContext(Dispatchers.Main) {
                            sourceBitmap = bmp
                            activeFramingMode = "fit"
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    // System Back Handler for Passport Photo Screen & Preview
    BackHandler(enabled = true) {
        if (showPreview) {
            showPreview = false
        } else {
            onBack()
        }
    }
    
    AppScreen(isScrollable = false) {
    Column(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0F172A))) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF1E293B)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = androidx.compose.ui.graphics.Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Passport Size Photo", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        
        if (sourceBitmap == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { photoPicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Text("Select Photo from Gallery")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workspace
                item {
                    val aspect = if (isCustom) {
                        val w = customWidth.toFloatOrNull() ?: 35f
                        val h = customHeight.toFloatOrNull() ?: 45f
                        w / h
                    } else {
                        selectedSpec.widthMm / selectedSpec.heightMm
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .aspectRatio(aspect)
                                .background(androidx.compose.ui.graphics.Color.White)
                                .clipToBounds()
                                .onSizeChanged {
                                    if (it.width > 0 && it.height > 0) {
                                        previewBoxWidth = it.width.toFloat()
                                        previewBoxHeight = it.height.toFloat()
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        activeFramingMode = "selected"
                                        scale = (scale * zoom).coerceAtLeast(0.1f)
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                        ) {
                            Image(
                                bitmap = sourceBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    ),
                                contentScale = ContentScale.Fit
                            )
                            
                            // Crop boundary indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = if (activeFramingMode == "selected") 2.dp else 1.dp,
                                        color = if (activeFramingMode == "selected") CrimsonPrimary else androidx.compose.ui.graphics.Color(0xFFCBD5E1)
                                    )
                            )
                        }
                    }
                    
                    // Reset Full Image Fit
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                        TextButton(
                            onClick = {
                                activeFramingMode = "fit"
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            modifier = Modifier.testTag("btn_reset_full_fit")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Full Image Fit")
                        }
                    }

                    // Side-by-side action buttons: 1. Resize Original, 2. Resize Selected Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Resize Original: Automatically resizes & scales photo to fill the target frame completely with zero gaps
                        Button(
                            onClick = {
                                activeFramingMode = "original"
                                sourceBitmap?.let { bmp ->
                                    val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
                                    val boxRatio = aspect
                                    val fillScale = max(boxRatio / imgRatio, imgRatio / boxRatio)
                                    scale = fillScale
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_resize_original"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeFramingMode == "original") CrimsonPrimary else androidx.compose.ui.graphics.Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Fullscreen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = androidx.compose.ui.graphics.Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Resize Original",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }

                        // 2. Resize Selected Area: Allows manual drag, zoom, and framing within the target spec frame
                        Button(
                            onClick = {
                                activeFramingMode = "selected"
                                sourceBitmap?.let { bmp ->
                                    val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
                                    val boxRatio = aspect
                                    val fillScale = max(boxRatio / imgRatio, imgRatio / boxRatio)
                                    if (scale < fillScale) {
                                        scale = fillScale
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_resize_selected_area"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeFramingMode == "selected") CrimsonPrimary else androidx.compose.ui.graphics.Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Crop,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = androidx.compose.ui.graphics.Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Resize Selected Area",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Mode feedback and intuitive manual controls
                    if (activeFramingMode == "selected") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drag to pan, pinch/buttons to zoom",
                                color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            )
                            Surface(
                                color = androidx.compose.ui.graphics.Color(0xFF1E293B),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { scale = (scale / 1.15f).coerceAtLeast(0.1f) },
                                        modifier = Modifier.size(32.dp).testTag("btn_zoom_out")
                                    ) {
                                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        text = "${(scale * 100).toInt()}%",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                    IconButton(
                                        onClick = { scale = (scale * 1.15f).coerceAtLeast(0.1f) },
                                        modifier = Modifier.size(32.dp).testTag("btn_zoom_in")
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { offsetX = 0f; offsetY = 0f },
                                        modifier = Modifier.size(32.dp).testTag("btn_center_photo")
                                    ) {
                                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    } else if (activeFramingMode == "original") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ToolGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto-filled to frame (No empty spaces)",
                                color = ToolGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Specifications
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Photo Specification", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var expandedSpec by remember { mutableStateOf(false) }
                            
                            OutlinedButton(
                                onClick = { expandedSpec = !expandedSpec },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isCustom) "Custom Size" else formatPhotoSpec(selectedSpec),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            
                            AnimatedVisibility(visible = expandedSpec) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    PHOTO_SPECS.groupBy { it.group }.forEach { (group, specs) ->
                                        Text(
                                            text = group,
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                        )
                                        specs.forEach { spec ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                                    .clickable { 
                                                        selectedSpec = spec
                                                        isCustom = false
                                                        expandedSpec = false
                                                        if (activeFramingMode == "original") {
                                                            sourceBitmap?.let { bmp ->
                                                                val newAspect = spec.widthMm / spec.heightMm
                                                                val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
                                                                scale = max(newAspect / imgRatio, imgRatio / newAspect)
                                                                offsetX = 0f
                                                                offsetY = 0f
                                                            }
                                                        } else if (activeFramingMode == "fit") {
                                                            scale = 1f; offsetX = 0f; offsetY = 0f
                                                        }
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0)),
                                                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF8FAFC))
                                            ) {
                                                Text(
                                                    text = formatPhotoSpec(spec),
                                                    color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                                                    modifier = Modifier.padding(14.dp),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    lineHeight = 20.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable { 
                                                isCustom = true
                                                expandedSpec = false
                                                if (activeFramingMode == "original") {
                                                    sourceBitmap?.let { bmp ->
                                                        val w = customWidth.toFloatOrNull() ?: 35f
                                                        val h = customHeight.toFloatOrNull() ?: 45f
                                                        val newAspect = if (h > 0) w / h else 1f
                                                        val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
                                                        scale = max(newAspect / imgRatio, imgRatio / newAspect)
                                                        offsetX = 0f
                                                        offsetY = 0f
                                                    }
                                                } else if (activeFramingMode == "fit") {
                                                    scale = 1f; offsetX = 0f; offsetY = 0f 
                                                }
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0)),
                                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF8FAFC))
                                    ) {
                                        Text(
                                            text = "Custom Size",
                                            color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                                            modifier = Modifier.padding(14.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            if (isCustom) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customWidth,
                                        onValueChange = { customWidth = it },
                                        label = { Text("Width") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    OutlinedTextField(
                                        value = customHeight,
                                        onValueChange = { customHeight = it },
                                        label = { Text("Height") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    OutlinedTextField(
                                        value = customUnit,
                                        onValueChange = { customUnit = it },
                                        label = { Text("Unit") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Print Settings
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Print & Layout Settings", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            var expandedPaper by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedPaper = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Paper Size: ${selectedPaper.name}")
                                }
                                DropdownMenu(expanded = expandedPaper, onDismissRequest = { expandedPaper = false }) {
                                    PAPER_SPECS.forEach { paper ->
                                        DropdownMenuItem(
                                            text = { Text(paper.name) },
                                            onClick = { selectedPaper = paper; expandedPaper = false }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = copiesText,
                                onValueChange = { copiesText = it },
                                label = { Text("Number of Photos / Copies (1-100)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { keyboardController?.hide() }
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = withBorder, onCheckedChange = { withBorder = it })
                                Text("Black Border around each photo", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }
            }
            
            // Bottom Action
            Row(
                modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF1E293B)).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { photoPicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF334155))
                ) {
                    Text("Change Photo")
                }
                Button(
                    onClick = {
                        val copies = copiesText.toIntOrNull() ?: 1
                        if (copies !in 1..1000) {
                            Toast.makeText(context, "Please enter copies between 1 and 100", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        coroutineScope.launch(Dispatchers.Default) {
                            try {
                                var wMm = if (isCustom) customWidth.toFloatOrNull() ?: 35f else selectedSpec.widthMm
                                var hMm = if (isCustom) customHeight.toFloatOrNull() ?: 45f else selectedSpec.heightMm
                                
                                if (customUnit.lowercase() == "cm") { wMm *= 10f; hMm *= 10f }
                                else if (customUnit.lowercase() == "inch" || customUnit.lowercase() == "in") { wMm *= 25.4f; hMm *= 25.4f }
                                
                                val pxW = (wMm / 25.4f * dpi).toInt()
                                val pxH = (hMm / 25.4f * dpi).toInt()
                                
                                // Create single cropped photo
                                val cropped = createCroppedFace(sourceBitmap!!, pxW, pxH, scale, offsetX, offsetY, previewBoxWidth, previewBoxHeight)
                                
                                // Create photo paper layout using selected paper mode
                                val layoutPages = PassportPrintLayoutEngine.generatePrintLayouts(
                                    photo = cropped,
                                    paper = selectedPaper,
                                    withBorder = withBorder,
                                    dpi = dpi,
                                    totalCopies = copies,
                                    mode = selectedPaperMode
                                )
                                
                                withContext(Dispatchers.Main) {
                                    croppedFaceBmp = cropped
                                    currentCopies = copies
                                    generatedLayoutBmps = layoutPages
                                    currentPreviewPage = 0
                                    showPreview = true
                                }
                            } catch (e: Exception) { 
                                e.printStackTrace() 
                                withContext(Dispatchers.Main) { Toast.makeText(context, "Error generating layout", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Generate Preview")
                }
            }
        }
    }
    }
    
    // Preview Dialog
    if (showPreview && generatedLayoutBmps.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler(enabled = true) { showPreview = false }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = androidx.compose.ui.graphics.Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    // Header (Sticky Top)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.ui.graphics.Color(0xFF1E293B))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showPreview = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = androidx.compose.ui.graphics.Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Print Preview", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            val is4x6 = PassportPrintLayoutEngine.is4x6Paper(selectedPaper)
                            val headerSub = if (is4x6) {
                                "Paper: ${selectedPaper.name} | ${selectedPaperMode.displayName} | Copies: $currentCopies | Pages: ${generatedLayoutBmps.size}"
                            } else {
                                "Paper: ${selectedPaper.name} | Copies: $currentCopies | Pages: ${generatedLayoutBmps.size}"
                            }
                            Text(headerSub, color = androidx.compose.ui.graphics.Color.LightGray, fontSize = 12.sp)
                        }
                    }

                    // Scrollable Area containing both the preview image and all action buttons
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        // Preview Image Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val bmp = generatedLayoutBmps[currentPreviewPage]
                                val aspect = bmp.width.toFloat() / bmp.height.toFloat()
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(aspect),
                                    contentScale = ContentScale.Fit
                                )
                                if (generatedLayoutBmps.size > 1) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        IconButton(onClick = { if (currentPreviewPage > 0) currentPreviewPage-- }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                                        }
                                        Text("Page ${currentPreviewPage + 1} of ${generatedLayoutBmps.size}", color = androidx.compose.ui.graphics.Color.White)
                                        IconButton(onClick = { if (currentPreviewPage < generatedLayoutBmps.size - 1) currentPreviewPage++ }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Photo Paper Layout Controls Card
                        val is4x6 = PassportPrintLayoutEngine.is4x6Paper(selectedPaper)
                        val rowCapacity = PassportPrintLayoutEngine.getPaperRowCapacity(selectedPaper, selectedPaperMode)

                        if (is4x6) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Paper Layout Mode",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PaperLayoutMode.values().forEach { mode ->
                                            val isSelected = selectedPaperMode == mode
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        if (selectedPaperMode != mode) {
                                                            selectedPaperMode = mode
                                                            croppedFaceBmp?.let { bmp ->
                                                                val updated = PassportPrintLayoutEngine.generatePrintLayouts(
                                                                    photo = bmp,
                                                                    paper = selectedPaper,
                                                                    withBorder = withBorder,
                                                                    dpi = dpi,
                                                                    totalCopies = currentCopies,
                                                                    mode = mode
                                                                )
                                                                generatedLayoutBmps = updated
                                                                currentPreviewPage = 0
                                                            }
                                                        }
                                                    },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) CrimsonPrimary else androidx.compose.ui.graphics.Color(0xFF334155)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = mode.displayName,
                                                        fontWeight = FontWeight.Bold,
                                                        color = androidx.compose.ui.graphics.Color.White,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = mode.subtitle,
                                                        color = if (isSelected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f) else androidx.compose.ui.graphics.Color.LightGray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${selectedPaperMode.displayName} • ${selectedPaperMode.subtitle} • Paper: ${selectedPaper.name}",
                                        color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                croppedFaceBmp?.let { bmp ->
                                                    val newCopies = currentCopies + rowCapacity
                                                    currentCopies = newCopies
                                                    copiesText = newCopies.toString()
                                                    val updated = PassportPrintLayoutEngine.generatePrintLayouts(
                                                        photo = bmp,
                                                        paper = selectedPaper,
                                                        withBorder = withBorder,
                                                        dpi = dpi,
                                                        totalCopies = newCopies,
                                                        mode = selectedPaperMode
                                                    )
                                                    generatedLayoutBmps = updated
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("add_line_photo_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ToolBlue,
                                                contentColor = androidx.compose.ui.graphics.Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add Line Photo (+${rowCapacity})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }

                                        if (currentCopies > rowCapacity) {
                                            OutlinedButton(
                                                onClick = {
                                                    croppedFaceBmp?.let { bmp ->
                                                        val newCopies = (currentCopies - rowCapacity).coerceAtLeast(1)
                                                        currentCopies = newCopies
                                                        copiesText = newCopies.toString()
                                                        val updated = PassportPrintLayoutEngine.generatePrintLayouts(
                                                            photo = bmp,
                                                            paper = selectedPaper,
                                                            withBorder = withBorder,
                                                            dpi = dpi,
                                                            totalCopies = newCopies,
                                                            mode = selectedPaperMode
                                                        )
                                                        generatedLayoutBmps = updated
                                                        if (currentPreviewPage >= updated.size) {
                                                            currentPreviewPage = maxOf(0, updated.size - 1)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("remove_line_photo_button"),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = androidx.compose.ui.graphics.Color.LightGray
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Remove Line", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Paper: ${selectedPaper.name} • ${rowCapacity} photos / row",
                                        color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                croppedFaceBmp?.let { bmp ->
                                                    val newCopies = currentCopies + rowCapacity
                                                    currentCopies = newCopies
                                                    copiesText = newCopies.toString()
                                                    val updated = PassportPrintLayoutEngine.generatePrintLayouts(
                                                        photo = bmp,
                                                        paper = selectedPaper,
                                                        withBorder = withBorder,
                                                        dpi = dpi,
                                                        totalCopies = newCopies
                                                    )
                                                    generatedLayoutBmps = updated
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("add_line_photo_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ToolBlue,
                                                contentColor = androidx.compose.ui.graphics.Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add Line Photo (+${rowCapacity})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }

                                        if (currentCopies > rowCapacity) {
                                            OutlinedButton(
                                                onClick = {
                                                    croppedFaceBmp?.let { bmp ->
                                                        val newCopies = (currentCopies - rowCapacity).coerceAtLeast(1)
                                                        currentCopies = newCopies
                                                        copiesText = newCopies.toString()
                                                        val updated = PassportPrintLayoutEngine.generatePrintLayouts(
                                                            photo = bmp,
                                                            paper = selectedPaper,
                                                            withBorder = withBorder,
                                                            dpi = dpi,
                                                            totalCopies = newCopies
                                                        )
                                                        generatedLayoutBmps = updated
                                                        if (currentPreviewPage >= updated.size) {
                                                            currentPreviewPage = maxOf(0, updated.size - 1)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("remove_line_photo_button"),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = androidx.compose.ui.graphics.Color.LightGray
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Remove Line", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Actions Column - always reachable and never cut off by viewport
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Color(0xFF1E293B))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            croppedFaceBmp?.let { bmp ->
                                                saveImage("Single_Passport_Photo.jpg", bmp, context)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    Text("Save Single (JPG)", fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                                Button(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            // Save all pages
                                            generatedLayoutBmps.forEachIndexed { index, bmp ->
                                                val name = if (generatedLayoutBmps.size == 1) "Print_Layout.jpg" else "Print_Layout_Page${index+1}.jpg"
                                                saveImage(name, bmp, context)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    Text("Save Print (JPG)", fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            if (generatedLayoutBmps.isNotEmpty()) {
                                                savePdf("Print_Layout.pdf", generatedLayoutBmps, context, selectedPaper)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    Text("Save Print (PDF)", fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                                Button(
                                    onClick = {
                                        if (generatedLayoutBmps.isNotEmpty()) {
                                            androidx.print.PrintHelper(context).apply {
                                                scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
                                            }.printBitmap("Passport_Print", generatedLayoutBmps.first())
                                        }
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Gray),
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Print", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createCroppedFace(
    src: Bitmap,
    targetW: Int,
    targetH: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    previewBoxW: Float,
    previewBoxH: Float
): Bitmap {
    val result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    
    // Simulate what the Compose UI is showing.
    // The Box has an aspect ratio and fits the image using scale/translation.
    // We map the transform back to the target resolution.
    val canvasPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(Color.WHITE) // White background for empty space
    
    val imgRatio = src.width.toFloat() / src.height.toFloat()
    val boxRatio = targetW.toFloat() / targetH.toFloat()
    
    var baseScale = 1f
    var dx = 0f
    var dy = 0f
    
    // Contain logic (Fit inside completely, matching ContentScale.Fit)
    if (imgRatio > boxRatio) {
        // Image is wider than box -> scale to width
        baseScale = targetW.toFloat() / src.width.toFloat()
        dy = (targetH - src.height * baseScale) / 2f
    } else {
        // Image is taller than box -> scale to height
        baseScale = targetH.toFloat() / src.height.toFloat()
        dx = (targetW - src.width * baseScale) / 2f
    }
    
    val cx = targetW / 2f
    val cy = targetH / 2f
    
    // Scale the translation by targetW / previewBoxW so that movement on screen matches movement on exported bitmap
    val targetOffsetX = if (previewBoxW > 0f) offsetX * (targetW.toFloat() / previewBoxW) else offsetX
    val targetOffsetY = if (previewBoxH > 0f) offsetY * (targetH.toFloat() / previewBoxH) else offsetY
    
    val matrix = android.graphics.Matrix()
    
    // 1. Initial fit (Contain)
    matrix.postScale(baseScale, baseScale)
    matrix.postTranslate(dx, dy)
    
    // 2. User scale around center (matching Compose graphicsLayer behavior)
    matrix.postScale(scale, scale, cx, cy)
    
    // 3. User pan translation
    matrix.postTranslate(targetOffsetX, targetOffsetY)
    
    canvas.drawBitmap(src, matrix, canvasPaint)
    return result
}

private suspend fun saveImage(name: String, bmp: Bitmap, context: Context) {
    try {
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        val uri = GlobalSaveManager.saveProcessedFile(context, file, name, "image/jpeg", "passport", "Passport Photo")
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Saved to PDFMaster/$name", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

private suspend fun savePdf(name: String, pages: List<Bitmap>, context: Context, paper: PaperSpec) {
    try {
        val doc = PdfDocument()
        
        for ((index, bmp) in pages.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, index + 1).create()
            val page = doc.startPage(pageInfo)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(page)
        }
        
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()
        
        val uri = GlobalSaveManager.saveProcessedFile(context, file, name, "application/pdf", "passport", "Passport Photo")
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Saved to PDFMaster/$name", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
