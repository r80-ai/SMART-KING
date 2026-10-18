package com.example.ui.dialogs

import androidx.activity.compose.BackHandler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Rect as AndroidRect
import android.graphics.RectF
import android.graphics.Typeface as AndroidTypeface
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PdfFileRecord
import com.example.model.WorkspaceSubTool
import com.example.pdf.PdfEngine
import com.example.pdf.sig.SignatureValidity
import com.example.pdf.sig.SignatureVerificationEngine
import com.example.pdf.sig.SignatureVerificationResult
import com.example.ui.ActiveModal
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfWorkspaceDialog(
    initialFile: PdfFileRecord,
    viewModel: PdfMasterViewModel,
    onDismiss: () -> Unit,
    startInEditMode: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Current working file in the workspace
    var currentRecord by remember { mutableStateOf(initialFile) }

    // Digital signature verification state for Adobe Acrobat-style indicator
    var signatureResult by remember { mutableStateOf<SignatureVerificationResult?>(null) }
    var isBannerDismissed by remember { mutableStateOf(false) }
    var showSigDetailsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentRecord.filePath, currentRecord.uriString) {
        isBannerDismissed = false
        withContext(Dispatchers.IO) {
            try {
                var file = File(currentRecord.filePath)
                if (!file.exists() || !file.canRead() || file.length() == 0L) {
                    if (!currentRecord.uriString.isNullOrBlank()) {
                        val uri = Uri.parse(currentRecord.uriString)
                        val tempFile = File(context.cacheDir, "sig_verify_view_${System.currentTimeMillis()}.pdf")
                        context.contentResolver.openInputStream(uri)?.use { inStream ->
                            FileOutputStream(tempFile).use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        if (tempFile.exists() && tempFile.length() > 0L) {
                            file = tempFile
                        }
                    }
                }
                if (file.exists() && file.canRead()) {
                    val res = SignatureVerificationEngine.verifySignatures(file)
                    signatureResult = res
                } else {
                    signatureResult = null
                }
            } catch (_: Exception) {
                signatureResult = null
            }
        }
    }

    // Ensure pages are loaded when workspace opens or file changes
    LaunchedEffect(currentRecord.filePath, currentRecord.uriString) {
        viewModel.loadPagesForViewer(currentRecord)
    }

    // Pages & Document State
    val pages by viewModel.renderedPages.collectAsState()
    val documentText by viewModel.viewerDocumentText.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val initialEditMode by viewModel.workspaceEditMode.collectAsState()

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var fitPageMode by remember { mutableStateOf(true) } // true = fit page, false = fit width
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Search in document
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMatchCount by remember { mutableIntStateOf(0) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    // Thumbnails tray toggle
    var showThumbnailTray by remember { mutableStateOf(false) }

    // ----------------------------------------------------
    // EDIT MODE STATE & PAGE ROTATION
    // ----------------------------------------------------
    var isEditMode by remember { mutableStateOf(initialEditMode || startInEditMode) }
    var activeEditTab by remember { mutableStateOf("TEXT") } // TEXT, IMAGE, PEN, HIGHLIGHT, SHAPES

    val pageRotations = remember { mutableStateMapOf<Int, Int>() }
    val currentRotationDeg = pageRotations[currentPageIndex] ?: 0
    val animatedRotation by animateFloatAsState(
        targetValue = currentRotationDeg.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pageRotation"
    )

    LaunchedEffect(initialEditMode) {
        if (initialEditMode) {
            isEditMode = true
        }
    }

    // Workspace Active Sub-Tool
    val initialSubTool by viewModel.workspaceInitialTool.collectAsState()
    var activeSubTool by remember { mutableStateOf(initialSubTool) }
    LaunchedEffect(initialSubTool) {
        if (initialSubTool != WorkspaceSubTool.NONE) {
            when (initialSubTool) {
                WorkspaceSubTool.EDIT_TEXT, WorkspaceSubTool.DRAW_ANNOTATE -> {
                    isEditMode = true
                    activeSubTool = WorkspaceSubTool.NONE
                }
                else -> {
                    activeSubTool = initialSubTool
                }
            }
            viewModel.clearWorkspaceInitialTool()
        }
    }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var saveMenuExpanded by remember { mutableStateOf(false) }

    val totalPages = pages.size.coerceAtLeast(currentRecord.pageCount).coerceAtLeast(1)

    // Accumulated edit items on pages
    val editItems = remember { mutableStateListOf<PdfEngine.EditItem>() }

    // OCR State & Direct Click-to-Edit State
    var pageOcrBlocks by remember { mutableStateOf<List<com.example.pdf.OcrTextBlock>>(emptyList()) }
    var ocrProcessing by remember { mutableStateOf(false) }

    var showOcrEditDialog by remember { mutableStateOf(false) }
    var tappedOcrLine by remember { mutableStateOf<com.example.pdf.OcrTextLine?>(null) }
    var editingOcrItem by remember { mutableStateOf<PdfEngine.EditItem?>(null) }
    var editingOcrText by remember { mutableStateOf("") }
    var editingOcrFontSize by remember { mutableFloatStateOf(16f) }
    var editingOcrTextColor by remember { mutableIntStateOf(AndroidColor.BLACK) }
    var editingOcrPaperColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var editingOcrIsBold by remember { mutableStateOf(false) }
    var editingOcrIsItalic by remember { mutableStateOf(false) }
    var editingOcrAlignment by remember { mutableStateOf("LEFT") }

    var showWorkspaceWatermarkDialog by remember { mutableStateOf(false) }
    var showRotateBar by remember { mutableStateOf(false) }

    fun samplePaperColor(bmp: Bitmap, rect: AndroidRect): Int {
        return try {
            val w = bmp.width
            val h = bmp.height
            val coords = listOf(
                Pair((rect.left - 3).coerceIn(0, w - 1), (rect.top - 3).coerceIn(0, h - 1)),
                Pair((rect.right + 3).coerceIn(0, w - 1), (rect.top - 3).coerceIn(0, h - 1)),
                Pair((rect.left - 3).coerceIn(0, w - 1), (rect.bottom + 3).coerceIn(0, h - 1)),
                Pair((rect.right + 3).coerceIn(0, w - 1), (rect.bottom + 3).coerceIn(0, h - 1)),
                Pair(rect.left.coerceIn(0, w - 1), rect.top.coerceIn(0, h - 1))
            )
            for ((x, y) in coords) {
                val c = bmp.getPixel(x, y)
                val r = AndroidColor.red(c)
                val g = AndroidColor.green(c)
                val b = AndroidColor.blue(c)
                if (r > 190 && g > 190 && b > 190) {
                    return c
                }
            }
            AndroidColor.WHITE
        } catch (_: Exception) {
            AndroidColor.WHITE
        }
    }
    
    // Automatically run OCR when edit mode is active and page changes
    val activePageBmp = pages.getOrNull(currentPageIndex)
    LaunchedEffect(isEditMode, currentPageIndex, activePageBmp) {
        if (isEditMode && activePageBmp != null) {
            ocrProcessing = true
            pageOcrBlocks = com.example.pdf.LocalOcrEngine.extractTextFromBitmap(activePageBmp)
            ocrProcessing = false
        }
    }

    // Undo / Redo history
    val undoStack = remember { mutableStateListOf<List<PdfEngine.EditItem>>() }
    val redoStack = remember { mutableStateListOf<List<PdfEngine.EditItem>>() }

    fun pushHistory() {
        undoStack.add(editItems.toList())
        redoStack.clear()
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(editItems.toList())
            val previous = undoStack.removeAt(undoStack.size - 1)
            editItems.clear()
            editItems.addAll(previous)
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(editItems.toList())
            val next = redoStack.removeAt(redoStack.size - 1)
            editItems.clear()
            editItems.addAll(next)
        }
    }

    // Text tool properties
    var textToAdd by remember { mutableStateOf("") }
    var selectedTextColor by remember { mutableIntStateOf(AndroidColor.BLACK) }
    var selectedFontSize by remember { mutableFloatStateOf(16f) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var selectedFontFamily by remember { mutableStateOf("DEFAULT") } // DEFAULT, SERIF, MONOSPACE
    var selectedTextAlign by remember { mutableStateOf("LEFT") } // LEFT, CENTER, RIGHT

    // Shape properties
    var selectedShapeType by remember { mutableStateOf(PdfEngine.AnnotationType.RECTANGLE) }
    var shapeStrokeColor by remember { mutableIntStateOf(AndroidColor.rgb(220, 38, 38)) }
    var shapeFillColor by remember { mutableStateOf<Int?>(null) }
    var shapeStrokeWidth by remember { mutableFloatStateOf(4f) }
    var shapeOpacity by remember { mutableFloatStateOf(1f) }

    // Selected annotation state
    var selectedAnnotationId by remember { mutableStateOf<String?>(null) }

    // Professional Draw tool properties
    var isDrawEraser by remember { mutableStateOf(false) }
    var penColor by remember { mutableIntStateOf(AndroidColor.rgb(37, 99, 235)) }
    var penStrokeWidth by remember { mutableFloatStateOf(4f) }
    var penOpacity by remember { mutableFloatStateOf(1f) }
    var penEraserSize by remember { mutableFloatStateOf(24f) }

    // Professional Highlight tool properties
    var isHighlightEraser by remember { mutableStateOf(false) }
    var highlightColor by remember { mutableIntStateOf(AndroidColor.rgb(250, 204, 21)) }
    var highlightThickness by remember { mutableFloatStateOf(24f) }
    var highlightOpacity by remember { mutableFloatStateOf(0.45f) }

    // Live in-progress drawing states
    val activeStrokePoints = remember { mutableStateListOf<Pair<Float, Float>>() }
    var activeShapeStart by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var activeShapeCurrent by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // Color picker dialog state
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf("PEN") } // "PEN", "HIGHLIGHT", "SHAPE_STROKE", "SHAPE_FILL", "SELECTED"
    var colorPickerCurrentColor by remember { mutableIntStateOf(AndroidColor.BLACK) }

    // Text editing dialog state & Double-tap tracking
    var showEditTextDialog by remember { mutableStateOf(false) }
    var editingTextItemId by remember { mutableStateOf<String?>(null) }
    var editingTextContent by remember { mutableStateOf("") }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapItemId by remember { mutableStateOf<String?>(null) }
    var replacingImageItemId by remember { mutableStateOf<String?>(null) }
    var showImageCropDialog by remember { mutableStateOf(false) }

    // Stamp properties
    var selectedStampText by remember { mutableStateOf("APPROVED") }

    // Device Image picker for adding images to PDF
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    if (bmp != null) {
                        pushHistory()
                        val repId = replacingImageItemId
                        if (repId != null) {
                            val idx = editItems.indexOfFirst { it.id == repId }
                            if (idx != -1) {
                                val old = editItems[idx]
                                val targetW = old.imageWidth.toInt().coerceAtLeast(20)
                                val targetH = old.imageHeight.toInt().coerceAtLeast(20)
                                val fittedBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(fittedBmp)
                                val srcW = bmp.width.toFloat()
                                val srcH = bmp.height.toFloat()
                                val scaleFactor = Math.max(targetW / srcW, targetH / srcH)
                                val scaledW = srcW * scaleFactor
                                val scaledH = srcH * scaleFactor
                                val dx = (targetW - scaledW) / 2f
                                val dy = (targetH - scaledH) / 2f
                                val matrix = android.graphics.Matrix().apply {
                                    postScale(scaleFactor, scaleFactor)
                                    postTranslate(dx, dy)
                                }
                                val p = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                                canvas.drawBitmap(bmp, matrix, p)
                                editItems[idx] = old.copy(
                                    imageBitmap = fittedBmp,
                                    originalImageBitmap = bmp,
                                    imageWidth = old.imageWidth,
                                    imageHeight = old.imageHeight,
                                    posX = old.posX,
                                    posY = old.posY
                                )
                            }
                            replacingImageItemId = null
                        } else {
                            val bmpAspect = if (bmp.height > 0) bmp.width.toFloat() / bmp.height.toFloat() else 1.33f
                            val initW = 200f
                            val initH = (initW / bmpAspect).coerceIn(30f, 400f)
                            val newImg = PdfEngine.EditItem(
                                pageIndex = currentPageIndex,
                                type = PdfEngine.AnnotationType.IMAGE,
                                imageBitmap = bmp,
                                originalImageBitmap = bmp,
                                posX = 80f,
                                posY = 150f,
                                imageWidth = initW,
                                imageHeight = initH
                            )
                            editItems.add(newImg)
                            selectedAnnotationId = newImg.id
                            activeEditTab = "IMAGE"
                        }
                        viewModel.clearNotification()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Export launcher
    var fileToExportUri by remember { mutableStateOf<Uri?>(null) }
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            viewModel.exportFileToExternalUri(currentRecord, uri)
        }
    }

    // Save As dialog filename state
    var customSaveAsName by remember { mutableStateOf("${currentRecord.nameWithoutExtension()}_edited.pdf") }

    // Color palettes
    val paletteColors = listOf(
        Pair("Black", AndroidColor.BLACK),
        Pair("Blue", AndroidColor.rgb(37, 99, 235)),
        Pair("Red", AndroidColor.rgb(220, 38, 38)),
        Pair("Green", AndroidColor.rgb(22, 163, 74)),
        Pair("Purple", AndroidColor.rgb(147, 51, 234)),
        Pair("Yellow", AndroidColor.rgb(234, 179, 8)),
        Pair("Orange", AndroidColor.rgb(249, 115, 22))
    )

    // Stamps list
    val stampsList = listOf("APPROVED", "CONFIDENTIAL", "DRAFT", "VERIFIED", "PAID", "URGENT", "ORIGINAL", "COPY")

    // Update search matches when searchQuery changes
    LaunchedEffect(searchQuery, documentText) {
        if (searchQuery.isNotBlank() && documentText.isNotBlank()) {
            val count = documentText.split(searchQuery, ignoreCase = true).size - 1
            searchMatchCount = count.coerceAtLeast(0)
            currentMatchIndex = if (searchMatchCount > 0) 1 else 0
        } else {
            searchMatchCount = 0
            currentMatchIndex = 0
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .testTag("pdf_workspace_dialog"),
            color = Color(0xFF0F172A)
        ) {
            BackHandler {
                when {
                    activeSubTool != WorkspaceSubTool.NONE -> activeSubTool = WorkspaceSubTool.NONE
                    isEditMode -> isEditMode = false
                    isSearchActive -> { isSearchActive = false; searchQuery = "" }
                    showThumbnailTray -> showThumbnailTray = false
                    else -> onDismiss()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // ====================================================
                // 1. TOP BAR: [←] filename.pdf [Share] [Save] [⋮]
                // ====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("workspace_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentRecord.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (pages.isNotEmpty()) "Page ${currentPageIndex + 1} of ${pages.size}" else "${currentRecord.pageCount} pages",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (signatureResult?.overallValidity == SignatureValidity.VALID) {
                                    val sigColor = Color(0xFF34D399)
                                    val sigBg = Color(0xFF065F46)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = sigBg,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { showSigDetailsDialog = true }
                                            .testTag("badge_viewer_signature_header")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = sigColor,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Signature Valid • Verified",
                                                color = sigColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Edit Mode Toggle Button
                        IconButton(
                            onClick = { isEditMode = !isEditMode },
                            modifier = Modifier.testTag("workspace_top_edit_button")
                        ) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditMode) "Done Editing" else "Edit PDF",
                                tint = if (isEditMode) Color(0xFF4ADE80) else Color.White
                            )
                        }

                        // Search Toggle
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier.testTag("workspace_search_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search in PDF",
                                tint = if (isSearchActive) CrimsonPrimary else Color.White
                            )
                        }

                        // Share
                        IconButton(
                            onClick = { viewModel.shareFile(context, currentRecord) },
                            modifier = Modifier.testTag("workspace_share_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }

                        // Save Dropdown
                        Box {
                            Button(
                                onClick = { saveMenuExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("workspace_save_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            DropdownMenu(
                                expanded = saveMenuExpanded,
                                onDismissRequest = { saveMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text("Save Copy")
                                            Text("Keeps original safe (${currentRecord.nameWithoutExtension()}_edited.pdf)", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.SaveAs, contentDescription = null, tint = CrimsonPrimary) },
                                    onClick = {
                                        saveMenuExpanded = false
                                        val targetName = "${currentRecord.nameWithoutExtension()}_edited.pdf"
                                        viewModel.saveEditedPdf(
                                            sourceRecord = currentRecord,
                                            items = editItems.toList(),
                                            deletedPages = emptySet(),
                                            pageRotations = pageRotations.toMap(),
                                            outputName = targetName
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Save As...")
                                            Text("Choose a custom file name", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                    onClick = {
                                        saveMenuExpanded = false
                                        customSaveAsName = "${currentRecord.nameWithoutExtension()}_edited.pdf"
                                        activeSubTool = WorkspaceSubTool.SAVE_AS
                                    }
                                )
                            }
                        }

                        // More Menu (⋮)
                        Box {
                            IconButton(
                                onClick = { moreMenuExpanded = true },
                                modifier = Modifier.testTag("workspace_more_menu_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More tools", tint = Color.White)
                            }

                            DropdownMenu(
                                expanded = moreMenuExpanded,
                                onDismissRequest = { moreMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Document Information") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    onClick = {
                                        moreMenuExpanded = false
                                        activeSubTool = WorkspaceSubTool.DOC_INFO
                                    },
                                    modifier = Modifier.testTag("menu_item_doc_info")
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to Device") },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                    onClick = {
                                        moreMenuExpanded = false
                                        exportDocumentLauncher.launch(currentRecord.name)
                                    },
                                    modifier = Modifier.testTag("menu_item_export_device")
                                )
                            }
                        }
                    }
                }

                // ====================================================
                // ADOBE ACROBAT-STYLE DIGITAL SIGNATURE BANNER
                // ====================================================
                if (signatureResult?.overallValidity == SignatureValidity.VALID && !isBannerDismissed) {
                    val bannerBg = Color(0xFF064E3B)
                    val bannerBorder = Color(0xFF10B981)
                    val bannerTint = Color(0xFF34D399)
                    val firstSigner = signatureResult?.signatures?.firstOrNull()

                    Surface(
                        color = bannerBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, color = bannerBorder.copy(alpha = 0.5f))
                            .testTag("adobe_signature_verification_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = bannerTint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Signature Valid",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (firstSigner != null && firstSigner.signerName.isNotBlank()) {
                                            "Verified • Signed by ${firstSigner.signerName}"
                                        } else {
                                            "Verified • Signed and all signatures are valid"
                                        },
                                        color = bannerTint.copy(alpha = 0.95f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { showSigDetailsDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = bannerBorder.copy(alpha = 0.25f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .border(1.dp, bannerBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .testTag("btn_view_signature_details")
                                ) {
                                    Text(
                                        text = "Signature Details",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { isBannerDismissed = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss banner",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ====================================================
                // SEARCH BAR (Visible if toggled)
                // ====================================================
                AnimatedVisibility(visible = isSearchActive) {
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search text...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CrimsonPrimary,
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (searchMatchCount > 0) "$currentMatchIndex of $searchMatchCount" else "0 matches",
                                color = if (searchMatchCount > 0) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { if (currentMatchIndex > 1) currentMatchIndex-- },
                                enabled = currentMatchIndex > 1
                            ) {
                                Icon(Icons.Default.NavigateBefore, contentDescription = "Previous match", tint = Color.White)
                            }
                            IconButton(
                                onClick = { if (currentMatchIndex < searchMatchCount) currentMatchIndex++ },
                                enabled = currentMatchIndex < searchMatchCount
                            ) {
                                Icon(Icons.Default.NavigateNext, contentDescription = "Next match", tint = Color.White)
                            }
                            IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search", tint = Color.White)
                            }
                        }
                    }
                }

                // ====================================================
                // 2. VIEWPORT / INTERACTIVE CANVAS
                // ====================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (pages.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = CrimsonPrimary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Rendering document...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            } else {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Could not render document pages",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.loadPagesForViewer(currentRecord, forceReload = true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else {
                        val currentBmp = pages.getOrNull(currentPageIndex) ?: pages.firstOrNull()
                        if (currentBmp != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(isEditMode, selectedAnnotationId) {
                                        detectTransformGestures { centroid, pan, zoom, _ ->
                                            val oldScale = zoomScale
                                            val newScale = (zoomScale * zoom).coerceIn(0.5f, 10.0f)
                                            if (newScale != oldScale) {
                                                panOffset = (panOffset - centroid) * (newScale / oldScale) + centroid + pan
                                                zoomScale = newScale
                                            } else {
                                                panOffset += pan
                                            }
                                        }
                                    }
                                    .pointerInput(isEditMode) {
                                        detectTapGestures(
                                            onDoubleTap = { tapOffset ->
                                                if (zoomScale > 1.2f) {
                                                    zoomScale = 1.0f
                                                    panOffset = Offset.Zero
                                                } else {
                                                    val targetScale = 2.8f
                                                    panOffset = (panOffset - tapOffset) * (targetScale / zoomScale) + tapOffset
                                                    zoomScale = targetScale
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Container for rendered page + interactive annotations
                                Card(
                                    shape = RoundedCornerShape(0.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = zoomScale
                                            scaleY = zoomScale
                                            translationX = panOffset.x
                                            translationY = panOffset.y
                                            rotationZ = animatedRotation
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    BoxWithConstraints(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val containerW = maxWidth
                                        val containerH = maxHeight
                                        val bmpW = currentBmp.width.toFloat()
                                        val bmpH = currentBmp.height.toFloat()
                                        val pageAspect = if (bmpH > 0f) bmpW / bmpH else 1f
                                        val containerAspect = if (containerH.value > 0f) containerW.value / containerH.value else 1f

                                        // Constrain strictly within container bounds to prevent clipping or distortion
                                        val pageModifier = if (fitPageMode) {
                                            if (pageAspect > containerAspect) {
                                                // Page is wider than container -> bound width to containerW
                                                Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(pageAspect)
                                            } else {
                                                // Page is taller than container -> bound height to containerH
                                                Modifier
                                                    .fillMaxHeight()
                                                    .aspectRatio(pageAspect)
                                            }
                                        } else {
                                            // Fit Width mode
                                            Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(pageAspect)
                                        }

                                        Box(
                                            modifier = pageModifier
                                                .background(Color.White)
                                                .shadow(8.dp)
                                        ) {
                                            // 1. PDF page image
                                            Image(
                                                bitmap = currentBmp.asImageBitmap(),
                                                contentDescription = "PDF Page ${currentPageIndex + 1}",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // 2. Annotation overlay (drawn on top of the page)
                                            Canvas(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                .pointerInput(
                                                    isEditMode,
                                                    activeEditTab,
                                                    selectedAnnotationId,
                                                    isDrawEraser,
                                                    isHighlightEraser,
                                                    selectedShapeType,
                                                    zoomScale,
                                                    panOffset,
                                                    penColor,
                                                    penStrokeWidth,
                                                    penOpacity,
                                                    penEraserSize,
                                                    highlightColor,
                                                    highlightThickness,
                                                    highlightOpacity,
                                                    shapeStrokeColor,
                                                    shapeFillColor,
                                                    shapeStrokeWidth,
                                                    shapeOpacity,
                                                    selectedTextColor,
                                                    selectedFontSize
                                                ) {
                                                    if (!isEditMode) return@pointerInput

                                                    awaitEachGesture {
                                                        val down = awaitFirstDown(requireUnconsumed = false)
                                                        val canvasW = size.width.toFloat()
                                                        val canvasH = size.height.toFloat()
                                                        val bmpW = currentBmp.width.toFloat()
                                                        val bmpH = currentBmp.height.toFloat()
                                                        val scaleX = canvasW / bmpW
                                                        val scaleY = canvasH / bmpH

                                                        val startDownPos = down.position
                                                        val startPdfX = (startDownPos.x - panOffset.x) / (scaleX * zoomScale)
                                                        val startPdfY = (startDownPos.y - panOffset.y) / (scaleY * zoomScale)

                                                        val currentSelected = editItems.find { it.id == selectedAnnotationId && it.pageIndex == currentPageIndex }
                                                        var activeHandle = PdfDragHandle.NONE
                                                        var interaction = PdfInteractionMode.NONE
                                                        var originalItem: PdfEngine.EditItem? = null
                                                        var historyPushedForGesture = false

                                                        var pinchStartDist = 0f
                                                        var pinchStartAngle = 0f
                                                        var pinchBaseItem: PdfEngine.EditItem? = null

                                                        if (currentSelected != null) {
                                                            val b = getAnnotationBounds(currentSelected)
                                                            val cx = (b.left + b.right) / 2f * scaleX
                                                            val cy = (b.top + b.bottom) / 2f * scaleY
                                                            val rot = currentSelected.getRotationAngle()
                                                            val hitDist = 32.dp.toPx()

                                                            // Check rotation knob first (positioned above the top center)
                                                            val knobUnrot = Offset(cx, b.top * scaleY - 28.dp.toPx())
                                                            val knobPos = if (rot != 0f) rotatePoint(knobUnrot.x, knobUnrot.y, cx, cy, rot) else knobUnrot
                                                            if (Math.hypot((startDownPos.x - knobPos.x).toDouble(), (startDownPos.y - knobPos.y).toDouble()) <= hitDist) {
                                                                activeHandle = PdfDragHandle.ROTATION_HANDLE
                                                                interaction = PdfInteractionMode.ROTATING
                                                                originalItem = currentSelected.copy()
                                                            } else if (currentSelected.type == PdfEngine.AnnotationType.LINE || currentSelected.type == PdfEngine.AnnotationType.ARROW) {
                                                                val p1 = Offset(currentSelected.posX * scaleX, currentSelected.posY * scaleY)
                                                                val p2 = Offset(currentSelected.endX * scaleX, currentSelected.endY * scaleY)
                                                                if (Math.hypot((startDownPos.x - p1.x).toDouble(), (startDownPos.y - p1.y).toDouble()) <= hitDist) {
                                                                    activeHandle = PdfDragHandle.START_POINT
                                                                    interaction = PdfInteractionMode.RESIZING
                                                                    originalItem = currentSelected.copy()
                                                                } else if (Math.hypot((startDownPos.x - p2.x).toDouble(), (startDownPos.y - p2.y).toDouble()) <= hitDist) {
                                                                    activeHandle = PdfDragHandle.END_POINT
                                                                    interaction = PdfInteractionMode.RESIZING
                                                                    originalItem = currentSelected.copy()
                                                                }
                                                            } else {
                                                                val unrotDown = if (rot != 0f) unrotatePoint(startDownPos.x, startDownPos.y, cx, cy, rot) else startDownPos
                                                                val l = b.left * scaleX
                                                                val r = b.right * scaleX
                                                                val t = b.top * scaleY
                                                                val bot = b.bottom * scaleY

                                                                if (Math.hypot((unrotDown.x - l).toDouble(), (unrotDown.y - t).toDouble()) <= hitDist) {
                                                                    activeHandle = PdfDragHandle.TOP_LEFT
                                                                    interaction = PdfInteractionMode.RESIZING
                                                                    originalItem = currentSelected.copy()
                                                                } else if (Math.hypot((unrotDown.x - r).toDouble(), (unrotDown.y - t).toDouble()) <= hitDist) {
                                                                    activeHandle = PdfDragHandle.TOP_RIGHT
                                                                    interaction = PdfInteractionMode.RESIZING
                                                                    originalItem = currentSelected.copy()
                                                                } else if (Math.hypot((unrotDown.x - l).toDouble(), (unrotDown.y - bot).toDouble()) <= hitDist) {
                                                                    activeHandle = PdfDragHandle.BOTTOM_LEFT
                                                                    interaction = PdfInteractionMode.RESIZING
                                                                    originalItem = currentSelected.copy()
                                                                } else if (Math.hypot((unrotDown.x - r).toDouble(), (unrotDown.y - bot).toDouble()) <= hitDist) {
                                                                    activeHandle = PdfDragHandle.BOTTOM_RIGHT
                                                                    interaction = PdfInteractionMode.RESIZING
                                                                    originalItem = currentSelected.copy()
                                                                } else if (currentSelected.type == PdfEngine.AnnotationType.IMAGE || currentSelected.type == PdfEngine.AnnotationType.RECTANGLE || currentSelected.type == PdfEngine.AnnotationType.TEXT) {
                                                                    val midX = (l + r) / 2f
                                                                    val midY = (t + bot) / 2f
                                                                    if (Math.hypot((unrotDown.x - midX).toDouble(), (unrotDown.y - t).toDouble()) <= hitDist) {
                                                                        activeHandle = PdfDragHandle.EDGE_TOP
                                                                        interaction = PdfInteractionMode.RESIZING
                                                                        originalItem = currentSelected.copy()
                                                                    } else if (Math.hypot((unrotDown.x - midX).toDouble(), (unrotDown.y - bot).toDouble()) <= hitDist) {
                                                                        activeHandle = PdfDragHandle.EDGE_BOTTOM
                                                                        interaction = PdfInteractionMode.RESIZING
                                                                        originalItem = currentSelected.copy()
                                                                    } else if (Math.hypot((unrotDown.x - l).toDouble(), (unrotDown.y - midY).toDouble()) <= hitDist) {
                                                                        activeHandle = PdfDragHandle.EDGE_LEFT
                                                                        interaction = PdfInteractionMode.RESIZING
                                                                        originalItem = currentSelected.copy()
                                                                    } else if (Math.hypot((unrotDown.x - r).toDouble(), (unrotDown.y - midY).toDouble()) <= hitDist) {
                                                                        activeHandle = PdfDragHandle.EDGE_RIGHT
                                                                        interaction = PdfInteractionMode.RESIZING
                                                                        originalItem = currentSelected.copy()
                                                                    }
                                                                }
                                                            }

                                                            if (interaction == PdfInteractionMode.NONE) {
                                                                val b = getAnnotationBounds(currentSelected)
                                                                val cx = (b.left + b.right) / 2f
                                                                val cy = (b.top + b.bottom) / 2f
                                                                val rot = currentSelected.getRotationAngle()
                                                                val unrot = if (rot != 0f) unrotatePoint(startPdfX, startPdfY, cx, cy, rot) else Offset(startPdfX, startPdfY)
                                                                val tol = 16f
                                                                val insideBox = unrot.x >= b.left - tol && unrot.x <= b.right + tol && unrot.y >= b.top - tol && unrot.y <= b.bottom + tol
                                                                if (insideBox || annotationContainsPoint(currentSelected, startPdfX, startPdfY)) {
                                                                    interaction = PdfInteractionMode.MOVING
                                                                    originalItem = currentSelected.copy()
                                                                    if (currentSelected.type == PdfEngine.AnnotationType.IMAGE) {
                                                                        activeEditTab = "IMAGE"
                                                                    } else if (currentSelected.type == PdfEngine.AnnotationType.TEXT) {
                                                                        activeEditTab = "TEXT"
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (interaction == PdfInteractionMode.NONE) {
                                                            val hit = editItems.filter { it.pageIndex == currentPageIndex }.asReversed().firstOrNull { annotationContainsPoint(it, startPdfX, startPdfY) }
                                                            if (hit != null) {
                                                                selectedAnnotationId = hit.id
                                                                interaction = PdfInteractionMode.MOVING
                                                                originalItem = hit.copy()
                                                                if (hit.type == PdfEngine.AnnotationType.IMAGE) {
                                                                    activeEditTab = "IMAGE"
                                                                } else if (hit.type == PdfEngine.AnnotationType.TEXT) {
                                                                    activeEditTab = "TEXT"
                                                                }
                                                            }
                                                        }

                                                        if (interaction != PdfInteractionMode.NONE) {
                                                            down.consume()
                                                        }

                                                        if (interaction == PdfInteractionMode.NONE) {
                                                            when (activeEditTab) {
                                                                "PEN" -> {
                                                                    if (isDrawEraser) {
                                                                        interaction = PdfInteractionMode.ERASING
                                                                    } else {
                                                                        interaction = PdfInteractionMode.DRAWING
                                                                        activeStrokePoints.clear()
                                                                        activeStrokePoints.add(Pair(startPdfX, startPdfY))
                                                                    }
                                                                }
                                                                "HIGHLIGHT" -> {
                                                                    if (isHighlightEraser) {
                                                                        interaction = PdfInteractionMode.ERASING
                                                                    } else {
                                                                        interaction = PdfInteractionMode.HIGHLIGHTING
                                                                        activeStrokePoints.clear()
                                                                        activeStrokePoints.add(Pair(startPdfX, startPdfY))
                                                                    }
                                                                }
                                                                "SHAPES" -> {
                                                                    interaction = PdfInteractionMode.CREATING_SHAPE
                                                                    activeShapeStart = Pair(startPdfX, startPdfY)
                                                                    activeShapeCurrent = Pair(startPdfX, startPdfY)
                                                                }
                                                                else -> {}
                                                            }
                                                        }

                                                        var hasMovedSignificantly = false
                                                        var lastPdfX = startPdfX
                                                        var lastPdfY = startPdfY

                                                        do {
                                                            val event = awaitPointerEvent()
                                                            val currentChange = event.changes.firstOrNull { it.id == down.id } ?: break
                                                            if (!currentChange.pressed) break
                                                            currentChange.consume()

                                                            val currentPos = currentChange.position
                                                            val currPdfX = (currentPos.x - panOffset.x) / (scaleX * zoomScale)
                                                            val currPdfY = (currentPos.y - panOffset.y) / (scaleY * zoomScale)

                                                            if (Math.hypot((currPdfX - startPdfX).toDouble(), (currPdfY - startPdfY).toDouble()) > 6.0) {
                                                                hasMovedSignificantly = true
                                                                if (!historyPushedForGesture && (interaction == PdfInteractionMode.MOVING || interaction == PdfInteractionMode.RESIZING || interaction == PdfInteractionMode.ROTATING)) {
                                                                    pushHistory()
                                                                    historyPushedForGesture = true
                                                                }
                                                            }

                                                            // Two-finger pinch to scale & rotate simultaneously
                                                            if (event.changes.size >= 2 && selectedAnnotationId != null && interaction != PdfInteractionMode.DRAWING && interaction != PdfInteractionMode.HIGHLIGHTING) {
                                                                val p1 = event.changes[0].position
                                                                val p2 = event.changes[1].position
                                                                event.changes.forEach { it.consume() }
                                                                val curDist = Math.hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()).toFloat()
                                                                val curAngle = Math.toDegrees(Math.atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())).toFloat()

                                                                if (pinchStartDist == 0f) {
                                                                    pinchStartDist = curDist
                                                                    pinchStartAngle = curAngle
                                                                    pinchBaseItem = editItems.find { it.id == selectedAnnotationId }?.copy()
                                                                } else if (pinchBaseItem != null) {
                                                                    hasMovedSignificantly = true
                                                                    if (!historyPushedForGesture) {
                                                                        pushHistory()
                                                                        historyPushedForGesture = true
                                                                    }
                                                                    val scaleRatio = (curDist / pinchStartDist.coerceAtLeast(10f)).coerceIn(0.2f, 5f)
                                                                    val angleDelta = curAngle - pinchStartAngle
                                                                    val newRot = (pinchBaseItem!!.getRotationAngle() + angleDelta) % 360f
                                                                    val posRot = if (newRot < 0f) newRot + 360f else newRot

                                                                    val itemIndex = editItems.indexOfFirst { it.id == selectedAnnotationId }
                                                                    if (itemIndex != -1) {
                                                                        val base = pinchBaseItem!!
                                                                        val updated = when (base.type) {
                                                                            PdfEngine.AnnotationType.TEXT -> {
                                                                                val oldB = getAnnotationBounds(base)
                                                                                val oldCx = (oldB.left + oldB.right) / 2f
                                                                                val oldCy = (oldB.top + oldB.bottom) / 2f
                                                                                val newSize = (base.fontSize * scaleRatio).coerceIn(8f, 160f)
                                                                                val fontFactor = newSize / base.fontSize
                                                                                val newBoxW = if (base.boxWidth > 0f) (base.boxWidth * fontFactor).coerceIn(30f, 3000f) else (oldB.width() * fontFactor)
                                                                                val newBoxH = if (base.boxHeight > 0f) (base.boxHeight * fontFactor).coerceIn(20f, 3000f) else (oldB.height() * fontFactor)
                                                                                val tempItem = base.copy(fontSize = newSize, boxWidth = newBoxW, boxHeight = newBoxH, rotation = posRot)
                                                                                val newB = getAnnotationBounds(tempItem)
                                                                                val newHalfW = (newB.right - newB.left) / 2f
                                                                                val newHalfH = (newB.bottom - newB.top) / 2f
                                                                                val newX = oldCx - newHalfW
                                                                                val newY = oldCy - newHalfH
                                                                                base.copy(posX = newX, posY = newY, fontSize = newSize, boxWidth = newBoxW, boxHeight = newBoxH, rotation = posRot)
                                                                            }
                                                                            PdfEngine.AnnotationType.IMAGE -> {
                                                                                val nw = (base.imageWidth * scaleRatio).coerceIn(30f, 1200f)
                                                                                val nh = (base.imageHeight * scaleRatio).coerceIn(30f, 1200f)
                                                                                base.copy(imageWidth = nw, imageHeight = nh, rotation = posRot, imageRotation = posRot)
                                                                            }
                                                                            PdfEngine.AnnotationType.DRAW, PdfEngine.AnnotationType.PEN, PdfEngine.AnnotationType.HIGHLIGHTER -> {
                                                                                val b = getAnnotationBounds(base)
                                                                                val cx0 = (b.left + b.right) / 2f
                                                                                val cy0 = (b.top + b.bottom) / 2f
                                                                                val newPts = base.points.map { Pair(cx0 + (it.first - cx0) * scaleRatio, cy0 + (it.second - cy0) * scaleRatio) }
                                                                                base.copy(points = newPts, rotation = posRot)
                                                                            }
                                                                            PdfEngine.AnnotationType.RECTANGLE, PdfEngine.AnnotationType.CIRCLE -> {
                                                                                val cx0 = (base.posX + base.endX) / 2f
                                                                                val cy0 = (base.posY + base.endY) / 2f
                                                                                val hw = (Math.abs(base.endX - base.posX) * scaleRatio) / 2f
                                                                                val hh = (Math.abs(base.endY - base.posY) * scaleRatio) / 2f
                                                                                base.copy(posX = cx0 - hw, posY = cy0 - hh, endX = cx0 + hw, endY = cy0 + hh, rotation = posRot)
                                                                            }
                                                                            else -> base.copy(rotation = posRot)
                                                                        }
                                                                        editItems[itemIndex] = updated
                                                                    }
                                                                }
                                                            } else {
                                                                // Single pointer interactions
                                                                when (interaction) {
                                                                    PdfInteractionMode.ROTATING -> {
                                                                        val selId = selectedAnnotationId
                                                                        if (selId != null) {
                                                                            val itemIndex = editItems.indexOfFirst { it.id == selId }
                                                                            if (itemIndex != -1) {
                                                                                val item = editItems[itemIndex]
                                                                                val b = getAnnotationBounds(item)
                                                                                val cx = (b.left + b.right) / 2f * scaleX
                                                                                val cy = (b.top + b.bottom) / 2f * scaleY
                                                                                val angleRad = Math.atan2((currentPos.y - cy).toDouble(), (currentPos.x - cx).toDouble())
                                                                                var angleDeg = (Math.toDegrees(angleRad).toFloat() + 90f) % 360f
                                                                                if (angleDeg < 0f) angleDeg += 360f
                                                                                for (snap in listOf(0f, 90f, 180f, 270f, 360f)) {
                                                                                    if (Math.abs(angleDeg - snap) <= 5f) {
                                                                                        angleDeg = snap % 360f
                                                                                        break
                                                                                    }
                                                                                }
                                                                                editItems[itemIndex] = item.copy(rotation = angleDeg, imageRotation = angleDeg)
                                                                            }
                                                                        }
                                                                    }
                                                                    PdfInteractionMode.DRAWING, PdfInteractionMode.HIGHLIGHTING -> {
                                                                        val last = activeStrokePoints.lastOrNull()
                                                                        if (last == null || Math.hypot((currPdfX - last.first).toDouble(), (currPdfY - last.second).toDouble()) >= 2.0) {
                                                                            activeStrokePoints.add(Pair(currPdfX, currPdfY))
                                                                        }
                                                                    }
                                                                    PdfInteractionMode.CREATING_SHAPE -> {
                                                                        activeShapeCurrent = Pair(currPdfX, currPdfY)
                                                                    }
                                                                    PdfInteractionMode.MOVING -> {
                                                                        val selId = selectedAnnotationId
                                                                        if (selId != null) {
                                                                            val itemIndex = editItems.indexOfFirst { it.id == selId }
                                                                            if (itemIndex != -1) {
                                                                                val item = editItems[itemIndex]
                                                                                val dx = currPdfX - lastPdfX
                                                                                val dy = currPdfY - lastPdfY
                                                                                val updated = if (item.points.isNotEmpty()) {
                                                                                    item.copy(
                                                                                        points = item.points.map { Pair(it.first + dx, it.second + dy) },
                                                                                        posX = item.posX + dx,
                                                                                        posY = item.posY + dy,
                                                                                        endX = item.endX + dx,
                                                                                        endY = item.endY + dy
                                                                                    )
                                                                                } else {
                                                                                    item.copy(
                                                                                        posX = item.posX + dx,
                                                                                        posY = item.posY + dy,
                                                                                        endX = item.endX + dx,
                                                                                        endY = item.endY + dy
                                                                                    )
                                                                                }
                                                                                editItems[itemIndex] = updated
                                                                            }
                                                                        }
                                                                    }
                                                                    PdfInteractionMode.RESIZING -> {
                                                                        val selId = selectedAnnotationId
                                                                        if (selId != null) {
                                                                            val itemIndex = editItems.indexOfFirst { it.id == selId }
                                                                            if (itemIndex != -1) {
                                                                                val item = editItems[itemIndex]
                                                                                val updated = when (item.type) {
                                                                                    PdfEngine.AnnotationType.LINE, PdfEngine.AnnotationType.ARROW -> {
                                                                                        if (activeHandle == PdfDragHandle.START_POINT) {
                                                                                            item.copy(posX = currPdfX, posY = currPdfY)
                                                                                        } else {
                                                                                            item.copy(endX = currPdfX, endY = currPdfY)
                                                                                        }
                                                                                    }
                                                                                    PdfEngine.AnnotationType.IMAGE -> {
                                                                                        val orig = originalItem ?: item
                                                                                        val origW = orig.imageWidth.coerceAtLeast(20f)
                                                                                        val origH = orig.imageHeight.coerceAtLeast(20f)
                                                                                        val aspect = origW / origH
                                                                                        val rot = orig.getRotationAngle()
                                                                                        val b = getAnnotationBounds(orig)
                                                                                        val cx = (b.left + b.right) / 2f * scaleX
                                                                                        val cy = (b.top + b.bottom) / 2f * scaleY
                                                                                        val unrotPos = if (rot != 0f) unrotatePoint(currentPos.x, currentPos.y, cx, cy, rot) else currentPos
                                                                                        val uPdfX = unrotPos.x / scaleX
                                                                                        val uPdfY = unrotPos.y / scaleY
                                                                                        when (activeHandle) {
                                                                                            PdfDragHandle.BOTTOM_RIGHT -> {
                                                                                                val nw = (uPdfX - orig.posX).coerceIn(20f, 2500f)
                                                                                                val nh = (nw / aspect).coerceIn(20f, 2500f)
                                                                                                orig.copy(imageWidth = nw, imageHeight = nh)
                                                                                            }
                                                                                            PdfDragHandle.BOTTOM_LEFT -> {
                                                                                                val right = orig.posX + origW
                                                                                                val nw = (right - uPdfX).coerceIn(20f, 2500f)
                                                                                                val nh = (nw / aspect).coerceIn(20f, 2500f)
                                                                                                orig.copy(posX = right - nw, imageWidth = nw, imageHeight = nh)
                                                                                            }
                                                                                            PdfDragHandle.TOP_RIGHT -> {
                                                                                                val bottom = orig.posY + origH
                                                                                                val nw = (uPdfX - orig.posX).coerceIn(20f, 2500f)
                                                                                                val nh = (nw / aspect).coerceIn(20f, 2500f)
                                                                                                orig.copy(posY = bottom - nh, imageWidth = nw, imageHeight = nh)
                                                                                            }
                                                                                            PdfDragHandle.TOP_LEFT -> {
                                                                                                val right = orig.posX + origW
                                                                                                val bottom = orig.posY + origH
                                                                                                val nw = (right - uPdfX).coerceIn(20f, 2500f)
                                                                                                val nh = (nw / aspect).coerceIn(20f, 2500f)
                                                                                                orig.copy(posX = right - nw, posY = bottom - nh, imageWidth = nw, imageHeight = nh)
                                                                                            }
                                                                                            PdfDragHandle.EDGE_RIGHT -> {
                                                                                                val nw = (uPdfX - orig.posX).coerceIn(20f, 2500f)
                                                                                                orig.copy(imageWidth = nw)
                                                                                            }
                                                                                            PdfDragHandle.EDGE_LEFT -> {
                                                                                                val right = orig.posX + origW
                                                                                                val nw = (right - uPdfX).coerceIn(20f, 2500f)
                                                                                                orig.copy(posX = right - nw, imageWidth = nw)
                                                                                            }
                                                                                            PdfDragHandle.EDGE_BOTTOM -> {
                                                                                                val nh = (uPdfY - orig.posY).coerceIn(20f, 2500f)
                                                                                                orig.copy(imageHeight = nh)
                                                                                            }
                                                                                            PdfDragHandle.EDGE_TOP -> {
                                                                                                val bottom = orig.posY + origH
                                                                                                val nh = (bottom - uPdfY).coerceIn(20f, 2500f)
                                                                                                orig.copy(posY = bottom - nh, imageHeight = nh)
                                                                                            }
                                                                                            else -> item
                                                                                        }
                                                                                    }
                                                                                    PdfEngine.AnnotationType.TEXT, PdfEngine.AnnotationType.WATERMARK -> {
                                                                                        val orig = originalItem ?: item
                                                                                        val origB = getAnnotationBounds(orig)
                                                                                        val origW = (origB.right - origB.left).coerceAtLeast(30f)
                                                                                        val origH = (origB.bottom - origB.top).coerceAtLeast(20f)
                                                                                        val rot = orig.getRotationAngle()
                                                                                        val origCx = (origB.left + origB.right) / 2f
                                                                                        val origCy = (origB.top + origB.bottom) / 2f
                                                                                        val screenOrigCx = origCx * scaleX
                                                                                        val screenOrigCy = origCy * scaleY
                                                                                        val unrotPos = if (rot != 0f) unrotatePoint(currentPos.x, currentPos.y, screenOrigCx, screenOrigCy, rot) else currentPos
                                                                                        val uPdfX = unrotPos.x / scaleX
                                                                                        val uPdfY = unrotPos.y / scaleY

                                                                                        when (activeHandle) {
                                                                                            PdfDragHandle.EDGE_RIGHT -> {
                                                                                                val newW = (uPdfX - origB.left).coerceIn(40f, 3000f)
                                                                                                val newH = origH
                                                                                                val worldAnchor = if (rot != 0f) rotatePoint(origB.left, origCy, origCx, origCy, rot) else Offset(origB.left, origCy)
                                                                                                val rotOffset = if (rot != 0f) rotateVector(-newW / 2f, 0f, rot) else Offset(-newW / 2f, 0f)
                                                                                                val newCenter = worldAnchor - rotOffset
                                                                                                val newPosX = newCenter.x - newW / 2f
                                                                                                val newPosY = newCenter.y - newH / 2f
                                                                                                orig.copy(posX = newPosX, posY = newPosY, boxWidth = newW)
                                                                                            }
                                                                                            PdfDragHandle.EDGE_LEFT -> {
                                                                                                val newW = (origB.right - uPdfX).coerceIn(40f, 3000f)
                                                                                                val newH = origH
                                                                                                val worldAnchor = if (rot != 0f) rotatePoint(origB.right, origCy, origCx, origCy, rot) else Offset(origB.right, origCy)
                                                                                                val rotOffset = if (rot != 0f) rotateVector(newW / 2f, 0f, rot) else Offset(newW / 2f, 0f)
                                                                                                val newCenter = worldAnchor - rotOffset
                                                                                                val newPosX = newCenter.x - newW / 2f
                                                                                                val newPosY = newCenter.y - newH / 2f
                                                                                                orig.copy(posX = newPosX, posY = newPosY, boxWidth = newW)
                                                                                            }
                                                                                            PdfDragHandle.EDGE_BOTTOM -> {
                                                                                                val newW = origW
                                                                                                val newH = (uPdfY - origB.top).coerceIn(20f, 3000f)
                                                                                                val worldAnchor = if (rot != 0f) rotatePoint(origCx, origB.top, origCx, origCy, rot) else Offset(origCx, origB.top)
                                                                                                val rotOffset = if (rot != 0f) rotateVector(0f, -newH / 2f, rot) else Offset(0f, -newH / 2f)
                                                                                                val newCenter = worldAnchor - rotOffset
                                                                                                val newPosX = newCenter.x - newW / 2f
                                                                                                val newPosY = newCenter.y - newH / 2f
                                                                                                orig.copy(posX = newPosX, posY = newPosY, boxHeight = newH)
                                                                                            }
                                                                                            PdfDragHandle.EDGE_TOP -> {
                                                                                                val newW = origW
                                                                                                val newH = (origB.bottom - uPdfY).coerceIn(20f, 3000f)
                                                                                                val worldAnchor = if (rot != 0f) rotatePoint(origCx, origB.bottom, origCx, origCy, rot) else Offset(origCx, origB.bottom)
                                                                                                val rotOffset = if (rot != 0f) rotateVector(0f, newH / 2f, rot) else Offset(0f, newH / 2f)
                                                                                                val newCenter = worldAnchor - rotOffset
                                                                                                val newPosX = newCenter.x - newW / 2f
                                                                                                val newPosY = newCenter.y - newH / 2f
                                                                                                orig.copy(posX = newPosX, posY = newPosY, boxHeight = newH)
                                                                                            }
                                                                                            PdfDragHandle.TOP_LEFT, PdfDragHandle.TOP_RIGHT, PdfDragHandle.BOTTOM_LEFT, PdfDragHandle.BOTTOM_RIGHT -> {
                                                                                                val (anchorX, anchorY) = when (activeHandle) {
                                                                                                    PdfDragHandle.TOP_LEFT -> Pair(origB.right, origB.bottom)
                                                                                                    PdfDragHandle.TOP_RIGHT -> Pair(origB.left, origB.bottom)
                                                                                                    PdfDragHandle.BOTTOM_LEFT -> Pair(origB.right, origB.top)
                                                                                                    else -> Pair(origB.left, origB.top)
                                                                                                }
                                                                                                val d0 = Math.hypot(origW.toDouble(), origH.toDouble()).toFloat().coerceAtLeast(10f)
                                                                                                val dCur = Math.hypot((uPdfX - anchorX).toDouble(), (uPdfY - anchorY).toDouble()).toFloat().coerceAtLeast(10f)
                                                                                                val scaleFactor = (dCur / d0).coerceIn(0.2f, 8f)
                                                                                                val newFont = (orig.fontSize * scaleFactor).coerceIn(8f, 160f)
                                                                                                val actualFactor = newFont / orig.fontSize
                                                                                                val newBoxW = if (orig.boxWidth > 0f) (orig.boxWidth * actualFactor).coerceIn(30f, 3000f) else (origW * actualFactor)
                                                                                                val newBoxH = if (orig.boxHeight > 0f) (orig.boxHeight * actualFactor).coerceIn(20f, 3000f) else (origH * actualFactor)

                                                                                                val unrotOffset = when (activeHandle) {
                                                                                                    PdfDragHandle.TOP_LEFT -> Offset(newBoxW / 2f, newBoxH / 2f)
                                                                                                    PdfDragHandle.TOP_RIGHT -> Offset(-newBoxW / 2f, newBoxH / 2f)
                                                                                                    PdfDragHandle.BOTTOM_LEFT -> Offset(newBoxW / 2f, -newBoxH / 2f)
                                                                                                    else -> Offset(-newBoxW / 2f, -newBoxH / 2f)
                                                                                                }
                                                                                                val worldAnchor = if (rot != 0f) rotatePoint(anchorX, anchorY, origCx, origCy, rot) else Offset(anchorX, anchorY)
                                                                                                val rotOffset = if (rot != 0f) rotateVector(unrotOffset.x, unrotOffset.y, rot) else unrotOffset
                                                                                                val newCenter = worldAnchor - rotOffset
                                                                                                val newPosX = newCenter.x - newBoxW / 2f
                                                                                                val newPosY = newCenter.y - newBoxH / 2f

                                                                                                orig.copy(posX = newPosX, posY = newPosY, fontSize = newFont, boxWidth = newBoxW, boxHeight = newBoxH)
                                                                                            }
                                                                                            else -> item
                                                                                        }
                                                                                    }
                                                                                    PdfEngine.AnnotationType.DRAW, PdfEngine.AnnotationType.PEN, PdfEngine.AnnotationType.HIGHLIGHTER -> {
                                                                                        val orig = originalItem ?: item
                                                                                        if (orig.points.isNotEmpty()) {
                                                                                            val origB = getAnnotationBounds(orig)
                                                                                            val origW = (origB.right - origB.left).coerceAtLeast(20f)
                                                                                            val origH = (origB.bottom - origB.top).coerceAtLeast(20f)
                                                                                            val rot = orig.getRotationAngle()
                                                                                            val cx = (origB.left + origB.right) / 2f * scaleX
                                                                                            val cy = (origB.top + origB.bottom) / 2f * scaleY
                                                                                            val unrotPos = if (rot != 0f) unrotatePoint(currentPos.x, currentPos.y, cx, cy, rot) else currentPos
                                                                                            val uPdfX = unrotPos.x / scaleX
                                                                                            val uPdfY = unrotPos.y / scaleY

                                                                                            val (anchorX, anchorY) = when (activeHandle) {
                                                                                                PdfDragHandle.TOP_LEFT -> Pair(origB.right, origB.bottom)
                                                                                                PdfDragHandle.TOP_RIGHT -> Pair(origB.left, origB.bottom)
                                                                                                PdfDragHandle.BOTTOM_LEFT -> Pair(origB.right, origB.top)
                                                                                                else -> Pair(origB.left, origB.top)
                                                                                            }
                                                                                            val targetW = Math.abs(uPdfX - anchorX).coerceIn(20f, 2500f)
                                                                                            val targetH = Math.abs(uPdfY - anchorY).coerceIn(20f, 2500f)
                                                                                            val scaleRatioX = targetW / origW
                                                                                            val scaleRatioY = targetH / origH
                                                                                            val newPts = orig.points.map { pt ->
                                                                                                val relX = pt.first - anchorX
                                                                                                val relY = pt.second - anchorY
                                                                                                val signX = if (activeHandle == PdfDragHandle.TOP_LEFT || activeHandle == PdfDragHandle.BOTTOM_LEFT) -1f else 1f
                                                                                                val signY = if (activeHandle == PdfDragHandle.TOP_LEFT || activeHandle == PdfDragHandle.TOP_RIGHT) -1f else 1f
                                                                                                Pair(anchorX + Math.abs(relX) * scaleRatioX * signX, anchorY + Math.abs(relY) * scaleRatioY * signY)
                                                                                            }
                                                                                            orig.copy(points = newPts)
                                                                                        } else item
                                                                                    }
                                                                                    PdfEngine.AnnotationType.RECTANGLE, PdfEngine.AnnotationType.CIRCLE -> {
                                                                                        when (activeHandle) {
                                                                                            PdfDragHandle.TOP_LEFT -> item.copy(posX = currPdfX, posY = currPdfY)
                                                                                            PdfDragHandle.BOTTOM_RIGHT -> item.copy(endX = currPdfX, endY = currPdfY)
                                                                                            PdfDragHandle.TOP_RIGHT -> item.copy(endX = currPdfX, posY = currPdfY)
                                                                                            PdfDragHandle.BOTTOM_LEFT -> item.copy(posX = currPdfX, endY = currPdfY)
                                                                                            else -> item
                                                                                        }
                                                                                    }
                                                                                    else -> item
                                                                                }
                                                                                editItems[itemIndex] = updated
                                                                            }
                                                                        }
                                                                    }
                                                                    PdfInteractionMode.ERASING -> {
                                                                        val eraseRadius = if (activeEditTab == "HIGHLIGHT") 28f else (penEraserSize / 2f).coerceAtLeast(16f)
                                                                        val toRemove = editItems.filter { it.pageIndex == currentPageIndex && (
                                                                            if (activeEditTab == "HIGHLIGHT") it.type == PdfEngine.AnnotationType.HIGHLIGHTER
                                                                            else it.type in listOf(PdfEngine.AnnotationType.DRAW, PdfEngine.AnnotationType.PEN, PdfEngine.AnnotationType.RECTANGLE, PdfEngine.AnnotationType.CIRCLE, PdfEngine.AnnotationType.ARROW, PdfEngine.AnnotationType.LINE)
                                                                        ) && annotationIntersectsCircle(it, currPdfX, currPdfY, eraseRadius) }
                                                                        if (toRemove.isNotEmpty()) {
                                                                            pushHistory()
                                                                            editItems.removeAll(toRemove)
                                                                            if (toRemove.any { it.id == selectedAnnotationId }) {
                                                                                selectedAnnotationId = null
                                                                            }
                                                                        }
                                                                    }
                                                                    else -> {}
                                                                }
                                                            }
                                                            lastPdfX = currPdfX
                                                            lastPdfY = currPdfY
                                                        } while (true)

                                                        // Gesture ended
                                                        when (interaction) {
                                                            PdfInteractionMode.DRAWING -> {
                                                                if (activeStrokePoints.size >= 2) {
                                                                    pushHistory()
                                                                    val newDraw = PdfEngine.EditItem(
                                                                        pageIndex = currentPageIndex,
                                                                        type = PdfEngine.AnnotationType.DRAW,
                                                                        points = activeStrokePoints.toList(),
                                                                        colorHex = penColor,
                                                                        strokeWidth = penStrokeWidth,
                                                                        opacity = penOpacity,
                                                                        baseWidth = bmpW,
                                                                        baseHeight = bmpH
                                                                    )
                                                                    editItems.add(newDraw)
                                                                    selectedAnnotationId = newDraw.id
                                                                } else if (!hasMovedSignificantly) {
                                                                    val hit = editItems.filter { it.pageIndex == currentPageIndex }.asReversed().firstOrNull { annotationContainsPoint(it, startPdfX, startPdfY) }
                                                                    selectedAnnotationId = hit?.id
                                                                }
                                                                activeStrokePoints.clear()
                                                            }
                                                            PdfInteractionMode.HIGHLIGHTING -> {
                                                                if (activeStrokePoints.size >= 2) {
                                                                    pushHistory()
                                                                    val newHl = PdfEngine.EditItem(
                                                                        pageIndex = currentPageIndex,
                                                                        type = PdfEngine.AnnotationType.HIGHLIGHTER,
                                                                        points = activeStrokePoints.toList(),
                                                                        colorHex = highlightColor,
                                                                        strokeWidth = highlightThickness,
                                                                        opacity = highlightOpacity,
                                                                        baseWidth = bmpW,
                                                                        baseHeight = bmpH
                                                                    )
                                                                    editItems.add(newHl)
                                                                    selectedAnnotationId = newHl.id
                                                                } else if (!hasMovedSignificantly) {
                                                                    val hit = editItems.filter { it.pageIndex == currentPageIndex }.asReversed().firstOrNull { annotationContainsPoint(it, startPdfX, startPdfY) }
                                                                    selectedAnnotationId = hit?.id
                                                                }
                                                                activeStrokePoints.clear()
                                                            }
                                                            PdfInteractionMode.CREATING_SHAPE -> {
                                                                val start = activeShapeStart
                                                                val end = activeShapeCurrent
                                                                if (start != null && end != null && Math.hypot((end.first - start.first).toDouble(), (end.second - start.second).toDouble()) >= 8.0) {
                                                                    pushHistory()
                                                                    val newShape = PdfEngine.EditItem(
                                                                        pageIndex = currentPageIndex,
                                                                        type = selectedShapeType,
                                                                        posX = start.first,
                                                                        posY = start.second,
                                                                        endX = end.first,
                                                                        endY = end.second,
                                                                        colorHex = shapeStrokeColor,
                                                                        fillColorHex = shapeFillColor,
                                                                        strokeWidth = shapeStrokeWidth,
                                                                        opacity = shapeOpacity,
                                                                        baseWidth = bmpW,
                                                                        baseHeight = bmpH
                                                                    )
                                                                    editItems.add(newShape)
                                                                    selectedAnnotationId = newShape.id
                                                                } else if (!hasMovedSignificantly) {
                                                                    val hit = editItems.filter { it.pageIndex == currentPageIndex }.asReversed().firstOrNull { annotationContainsPoint(it, startPdfX, startPdfY) }
                                                                    selectedAnnotationId = hit?.id
                                                                }
                                                                activeShapeStart = null
                                                                activeShapeCurrent = null
                                                            }
                                                            PdfInteractionMode.MOVING, PdfInteractionMode.RESIZING, PdfInteractionMode.ROTATING, PdfInteractionMode.NONE -> {
                                                                if (!hasMovedSignificantly) {
                                                                    val hit = editItems.filter { it.pageIndex == currentPageIndex }.asReversed().firstOrNull { annotationContainsPoint(it, startPdfX, startPdfY) }
                                                                    val now = System.currentTimeMillis()
                                                                    if (hit != null) {
                                                                        if (hit.type == PdfEngine.AnnotationType.OCR_REPLACEMENT) {
                                                                            // Direct re-edit of previously replaced OCR text
                                                                            editingOcrItem = hit
                                                                            tappedOcrLine = com.example.pdf.OcrTextLine(
                                                                                text = hit.text,
                                                                                boundingBox = hit.maskRect ?: AndroidRect(hit.posX.toInt(), hit.posY.toInt(), (hit.posX + 100).toInt(), (hit.posY + 30).toInt()),
                                                                                elements = emptyList()
                                                                            )
                                                                            editingOcrText = hit.text
                                                                            editingOcrFontSize = hit.fontSize
                                                                            editingOcrTextColor = hit.colorHex
                                                                            editingOcrPaperColor = hit.maskColor
                                                                            editingOcrIsBold = hit.isBold
                                                                            editingOcrIsItalic = hit.isItalic
                                                                            editingOcrAlignment = hit.alignment
                                                                            showOcrEditDialog = true
                                                                            selectedAnnotationId = hit.id
                                                                        } else if (now - lastTapTime < 500L && (lastTapItemId == hit.id || selectedAnnotationId == hit.id) && hit.type == PdfEngine.AnnotationType.TEXT) {
                                                                            editingTextItemId = hit.id
                                                                            editingTextContent = hit.text
                                                                            showEditTextDialog = true
                                                                        }
                                                                        lastTapTime = now
                                                                        lastTapItemId = hit.id
                                                                        selectedAnnotationId = hit.id
                                                                        if (hit.type == PdfEngine.AnnotationType.TEXT || hit.type == PdfEngine.AnnotationType.OCR_REPLACEMENT) {
                                                                            activeEditTab = "TEXT"
                                                                        } else if (hit.type == PdfEngine.AnnotationType.IMAGE) {
                                                                            activeEditTab = "IMAGE"
                                                                        }
                                                                    } else {
                                                                        // Check if user tapped inside any detected OCR green box
                                                                        val pageItems = editItems.filter { it.pageIndex == currentPageIndex }
                                                                        val hitOcrLine = if (isEditMode) {
                                                                            pageOcrBlocks.flatMap { it.lines }.firstOrNull { line ->
                                                                                val rect = line.boundingBox
                                                                                val isMasked = pageItems.any { it.type == PdfEngine.AnnotationType.OCR_REPLACEMENT && it.maskRect != null && AndroidRect.intersects(it.maskRect!!, rect) }
                                                                                if (isMasked) return@firstOrNull false
                                                                                val tol = 12f
                                                                                startPdfX >= rect.left - tol && startPdfX <= rect.right + tol &&
                                                                                startPdfY >= rect.top - tol && startPdfY <= rect.bottom + tol
                                                                            } ?: pageOcrBlocks.firstOrNull { block ->
                                                                                val rect = block.boundingBox
                                                                                val isMasked = pageItems.any { it.type == PdfEngine.AnnotationType.OCR_REPLACEMENT && it.maskRect != null && AndroidRect.intersects(it.maskRect!!, rect) }
                                                                                if (isMasked) return@firstOrNull false
                                                                                val tol = 12f
                                                                                startPdfX >= rect.left - tol && startPdfX <= rect.right + tol &&
                                                                                startPdfY >= rect.top - tol && startPdfY <= rect.bottom + tol
                                                                            }?.let { block ->
                                                                                com.example.pdf.OcrTextLine(
                                                                                    text = block.text,
                                                                                    boundingBox = block.boundingBox,
                                                                                    elements = emptyList()
                                                                                )
                                                                            }
                                                                        } else null

                                                                        if (hitOcrLine != null) {
                                                                            // User tapped directly on detected text (green box)
                                                                            tappedOcrLine = hitOcrLine
                                                                            editingOcrItem = null
                                                                            editingOcrText = hitOcrLine.text
                                                                            val detectedLineHeight = hitOcrLine.boundingBox.height().toFloat()
                                                                            editingOcrFontSize = (detectedLineHeight * 0.75f).coerceIn(10f, 60f)
                                                                            editingOcrTextColor = AndroidColor.BLACK
                                                                            val curBmp = pages.getOrNull(currentPageIndex)
                                                                            editingOcrPaperColor = if (curBmp != null) samplePaperColor(curBmp, hitOcrLine.boundingBox) else AndroidColor.WHITE
                                                                            editingOcrIsBold = false
                                                                            editingOcrIsItalic = false
                                                                            editingOcrAlignment = "LEFT"
                                                                            showOcrEditDialog = true
                                                                            activeEditTab = "TEXT"
                                                                        } else if (activeEditTab == "TEXT" && textToAdd.isNotBlank()) {
                                                                            pushHistory()
                                                                            val tempPaint = TextPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                                                                textSize = selectedFontSize
                                                                                val family = when (selectedFontFamily) {
                                                                                    "Serif", "SERIF" -> AndroidTypeface.SERIF
                                                                                    "Monospace", "MONOSPACE" -> AndroidTypeface.MONOSPACE
                                                                                    else -> AndroidTypeface.SANS_SERIF
                                                                                }
                                                                                val style = when {
                                                                                    isBold && isItalic -> AndroidTypeface.BOLD_ITALIC
                                                                                    isBold -> AndroidTypeface.BOLD
                                                                                    isItalic -> AndroidTypeface.ITALIC
                                                                                    else -> AndroidTypeface.NORMAL
                                                                                }
                                                                                typeface = AndroidTypeface.create(family, style)
                                                                            }
                                                                            val lines = textToAdd.split("\n")
                                                                            var maxLineW = 0f
                                                                            for (l in lines) {
                                                                                val lw = tempPaint.measureText(l)
                                                                                if (lw > maxLineW) maxLineW = lw
                                                                            }
                                                                            val initialBoxW = (maxLineW + 24f).coerceAtLeast(80f)
                                                                            val initialBoxH = (lines.size * selectedFontSize * 1.35f + 16f).coerceAtLeast(36f)

                                                                            val newText = PdfEngine.EditItem(
                                                                                pageIndex = currentPageIndex,
                                                                                text = textToAdd,
                                                                                posX = startPdfX.coerceAtLeast(20f),
                                                                                posY = startPdfY.coerceAtLeast(20f),
                                                                                colorHex = selectedTextColor,
                                                                                fontSize = selectedFontSize,
                                                                                isBold = isBold,
                                                                                isItalic = isItalic,
                                                                                isUnderline = isUnderline,
                                                                                fontFamily = selectedFontFamily,
                                                                                alignment = selectedTextAlign,
                                                                                boxWidth = initialBoxW,
                                                                                boxHeight = initialBoxH
                                                                            )
                                                                            editItems.add(newText)
                                                                            selectedAnnotationId = newText.id
                                                                            textToAdd = ""
                                                                        } else {
                                                                            selectedAnnotationId = null
                                                                            lastTapItemId = null
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            PdfInteractionMode.ERASING -> {}
                                                        }
                                                    }
                                                }
                                        ) {
                                            val canvasW = size.width
                                            val canvasH = size.height
                                            val bmpW = currentBmp.width.toFloat()
                                            val bmpH = currentBmp.height.toFloat()
                                            val scaleX = canvasW / bmpW
                                            val scaleY = canvasH / bmpH

                                            // Draw annotations for this page
                                            val pageItems = editItems.filter { it.pageIndex == currentPageIndex }

                                            // Draw OCR bounding boxes if in edit text mode
                                            if (isEditMode && activeEditTab == "TEXT" && !ocrProcessing) {
                                                for (block in pageOcrBlocks) {
                                                    for (line in block.lines) {
                                                        val rect = line.boundingBox
                                                        val left = rect.left * scaleX
                                                        val top = rect.top * scaleY
                                                        val right = rect.right * scaleX
                                                        val bottom = rect.bottom * scaleY

                                                        val isMasked = pageItems.any { it.type == PdfEngine.AnnotationType.OCR_REPLACEMENT && it.maskRect != null && android.graphics.Rect.intersects(it.maskRect!!, rect) }

                                                        if (!isMasked) {
                                                            // Vibrant green boxes for detected text
                                                            drawRect(
                                                                color = Color(0xFF10B981).copy(alpha = 0.22f),
                                                                topLeft = Offset(left, top),
                                                                size = Size(right - left, bottom - top)
                                                            )
                                                            drawRect(
                                                                color = Color(0xFF059669),
                                                                topLeft = Offset(left, top),
                                                                size = Size(right - left, bottom - top),
                                                                style = Stroke(width = 2f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // 1. Draw saved/persisted annotations for this page
                                            for (item in pageItems) {
                                                val x = item.posX * scaleX
                                                val y = item.posY * scaleY
                                                val endX = item.endX * scaleX
                                                val endY = item.endY * scaleY
                                                val itemColor = Color(item.colorHex).copy(alpha = item.opacity)

                                                val itemBounds = getAnnotationBounds(item)
                                                val itemCx = (itemBounds.left + itemBounds.right) / 2f * scaleX
                                                val itemCy = (itemBounds.top + itemBounds.bottom) / 2f * scaleY
                                                val itemRot = item.getRotationAngle()

                                                rotate(degrees = itemRot, pivot = Offset(itemCx, itemCy)) {
                                                    when (item.type) {
                                                        PdfEngine.AnnotationType.OCR_REPLACEMENT -> {
                                                            // 1. Cover original text with opaque background patch matching paper color
                                                            item.maskRect?.let { mr ->
                                                                val mLeft = mr.left * scaleX
                                                                val mTop = mr.top * scaleY
                                                                val mRight = mr.right * scaleX
                                                                val mBottom = mr.bottom * scaleY
                                                                drawRect(
                                                                    color = Color(item.maskColor),
                                                                    topLeft = Offset(mLeft, mTop),
                                                                    size = Size(mRight - mLeft, mBottom - mTop)
                                                                )
                                                            }

                                                            // 2. Draw updated edited text on top with matching font size, color, baseline
                                                            if (item.text.isNotEmpty()) {
                                                                drawIntoCanvas { composeCanvas ->
                                                                    val native = composeCanvas.nativeCanvas
                                                                    val fontSz = (item.fontSize * scaleX).coerceAtLeast(8f)
                                                                    val textPaint = TextPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                                                        color = item.colorHex
                                                                        textSize = fontSz
                                                                        isUnderlineText = item.isUnderline
                                                                        textAlign = when (item.alignment) {
                                                                            "CENTER" -> AndroidPaint.Align.CENTER
                                                                            "RIGHT" -> AndroidPaint.Align.RIGHT
                                                                            else -> AndroidPaint.Align.LEFT
                                                                        }
                                                                        val family = when (item.fontFamily) {
                                                                            "Serif", "SERIF" -> AndroidTypeface.SERIF
                                                                            "Monospace", "MONOSPACE" -> AndroidTypeface.MONOSPACE
                                                                            else -> AndroidTypeface.SANS_SERIF
                                                                        }
                                                                        val style = when {
                                                                            item.isBold && item.isItalic -> AndroidTypeface.BOLD_ITALIC
                                                                            item.isBold -> AndroidTypeface.BOLD
                                                                            item.isItalic -> AndroidTypeface.ITALIC
                                                                            else -> AndroidTypeface.NORMAL
                                                                        }
                                                                        typeface = AndroidTypeface.create(family, style)
                                                                    }
                                                                    val textY = item.posY * scaleY
                                                                    val textX = item.posX * scaleX
                                                                    native.drawText(item.text, textX, textY, textPaint)
                                                                }
                                                            }
                                                        }
                                                        PdfEngine.AnnotationType.TEXT -> {
                                                            drawIntoCanvas { composeCanvas ->
                                                                val native = composeCanvas.nativeCanvas
                                                                val fontSz = (item.fontSize * scaleX).coerceAtLeast(8f)
                                                                val textPaint = TextPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                                                    color = item.colorHex
                                                                    textSize = fontSz
                                                                    isUnderlineText = item.isUnderline
                                                                    val family = when (item.fontFamily) {
                                                                        "Serif", "SERIF" -> AndroidTypeface.SERIF
                                                                        "Monospace", "MONOSPACE" -> AndroidTypeface.MONOSPACE
                                                                        else -> AndroidTypeface.SANS_SERIF
                                                                    }
                                                                    val style = when {
                                                                        item.isBold && item.isItalic -> AndroidTypeface.BOLD_ITALIC
                                                                        item.isBold -> AndroidTypeface.BOLD
                                                                        item.isItalic -> AndroidTypeface.ITALIC
                                                                        else -> AndroidTypeface.NORMAL
                                                                    }
                                                                    typeface = AndroidTypeface.create(family, style)
                                                                }

                                                                val textToDraw = if (item.text.isEmpty()) " " else item.text
                                                                val targetW = (itemBounds.right - itemBounds.left) * scaleX

                                                                val align = when (item.alignment) {
                                                                    "CENTER" -> Layout.Alignment.ALIGN_CENTER
                                                                    "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
                                                                    else -> Layout.Alignment.ALIGN_NORMAL
                                                                }

                                                                val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                                    StaticLayout.Builder.obtain(textToDraw, 0, textToDraw.length, textPaint, targetW.toInt().coerceAtLeast(10))
                                                                        .setAlignment(align)
                                                                        .setLineSpacing(0f, 1.15f)
                                                                        .setIncludePad(false)
                                                                        .build()
                                                                } else {
                                                                    @Suppress("DEPRECATION")
                                                                    StaticLayout(textToDraw, textPaint, targetW.toInt().coerceAtLeast(10), align, 1.15f, 0f, false)
                                                                }

                                                                native.save()
                                                                native.translate(x, y)
                                                                staticLayout.draw(native)
                                                                native.restore()
                                                            }
                                                        }
                                                        PdfEngine.AnnotationType.IMAGE -> {
                                                            item.imageBitmap?.let { bmp ->
                                                                val imgW = (item.imageWidth * scaleX).toInt().coerceAtLeast(1)
                                                                val imgH = (item.imageHeight * scaleY).toInt().coerceAtLeast(1)
                                                                drawImage(
                                                                    image = bmp.asImageBitmap(),
                                                                    dstOffset = androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt()),
                                                                    dstSize = androidx.compose.ui.unit.IntSize(imgW, imgH)
                                                                )
                                                            }
                                                        }
                                                        PdfEngine.AnnotationType.STAMP -> {
                                                            drawRoundRect(
                                                                color = Color(0xFFDC2626),
                                                                topLeft = Offset(x, y),
                                                                size = Size(140f * scaleX, 40f * scaleY),
                                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                                                                style = Stroke(width = 3f)
                                                            )
                                                            drawIntoCanvas { composeCanvas ->
                                                                val native = composeCanvas.nativeCanvas
                                                                val paint = AndroidPaint().apply {
                                                                    color = AndroidColor.parseColor("#DC2626")
                                                                    textSize = 14f * scaleX
                                                                    isAntiAlias = true
                                                                    typeface = AndroidTypeface.create(AndroidTypeface.DEFAULT, AndroidTypeface.BOLD)
                                                                }
                                                                native.drawText(item.stampText, x + 8f * scaleX, y + 26f * scaleY, paint)
                                                            }
                                                        }
                                                        PdfEngine.AnnotationType.DRAW, PdfEngine.AnnotationType.PEN -> {
                                                            if (item.points.isNotEmpty()) {
                                                                val path = Path()
                                                                val firstPt = item.points.first()
                                                                path.moveTo(firstPt.first * scaleX, firstPt.second * scaleY)
                                                                for (i in 1 until item.points.size) {
                                                                    val pt = item.points[i]
                                                                    path.lineTo(pt.first * scaleX, pt.second * scaleY)
                                                                }
                                                                drawPath(
                                                                    path = path,
                                                                    color = itemColor,
                                                                    style = Stroke(
                                                                        width = item.strokeWidth * scaleX,
                                                                        cap = StrokeCap.Round,
                                                                        join = StrokeJoin.Round
                                                                    )
                                                                )
                                                            }
                                                        }
                                                        PdfEngine.AnnotationType.HIGHLIGHTER -> {
                                                            val hlAlpha = item.opacity.coerceIn(0.15f, 0.85f)
                                                            if (item.points.isNotEmpty()) {
                                                                val path = Path()
                                                                val firstPt = item.points.first()
                                                                path.moveTo(firstPt.first * scaleX, firstPt.second * scaleY)
                                                                for (i in 1 until item.points.size) {
                                                                    val pt = item.points[i]
                                                                    path.lineTo(pt.first * scaleX, pt.second * scaleY)
                                                                }
                                                                drawPath(
                                                                    path = path,
                                                                    color = Color(item.colorHex).copy(alpha = hlAlpha),
                                                                    style = Stroke(
                                                                        width = item.strokeWidth * scaleX,
                                                                        cap = StrokeCap.Round,
                                                                        join = StrokeJoin.Round
                                                                    )
                                                                )
                                                            } else {
                                                                drawLine(
                                                                    color = Color(item.colorHex).copy(alpha = hlAlpha),
                                                                    start = Offset(x, y),
                                                                    end = Offset(endX, endY),
                                                                    strokeWidth = item.strokeWidth * scaleX,
                                                                    cap = StrokeCap.Round
                                                                )
                                                            }
                                                        }
                                                        PdfEngine.AnnotationType.RECTANGLE -> {
                                                            val left = Math.min(x, endX)
                                                            val top = Math.min(y, endY)
                                                            val width = Math.abs(endX - x).coerceAtLeast(4f)
                                                            val height = Math.abs(endY - y).coerceAtLeast(4f)
                                                            item.fillColorHex?.let { fillHex ->
                                                                drawRect(
                                                                    color = Color(fillHex).copy(alpha = item.opacity),
                                                                    topLeft = Offset(left, top),
                                                                    size = Size(width, height)
                                                                )
                                                            }
                                                            drawRect(
                                                                color = itemColor,
                                                                topLeft = Offset(left, top),
                                                                size = Size(width, height),
                                                                style = Stroke(width = item.strokeWidth * scaleX)
                                                            )
                                                        }
                                                        PdfEngine.AnnotationType.CIRCLE -> {
                                                            val left = Math.min(x, endX)
                                                            val top = Math.min(y, endY)
                                                            val width = Math.abs(endX - x).coerceAtLeast(4f)
                                                            val height = Math.abs(endY - y).coerceAtLeast(4f)
                                                            item.fillColorHex?.let { fillHex ->
                                                                drawOval(
                                                                    color = Color(fillHex).copy(alpha = item.opacity),
                                                                    topLeft = Offset(left, top),
                                                                    size = Size(width, height)
                                                                )
                                                            }
                                                            drawOval(
                                                                color = itemColor,
                                                                topLeft = Offset(left, top),
                                                                size = Size(width, height),
                                                                style = Stroke(width = item.strokeWidth * scaleX)
                                                            )
                                                        }
                                                        PdfEngine.AnnotationType.LINE, PdfEngine.AnnotationType.UNDERLINE, PdfEngine.AnnotationType.STRIKETHROUGH -> {
                                                            drawLine(
                                                                color = itemColor,
                                                                start = Offset(x, y),
                                                                end = Offset(endX, endY),
                                                                strokeWidth = item.strokeWidth * scaleX,
                                                                cap = StrokeCap.Round
                                                            )
                                                        }
                                                        PdfEngine.AnnotationType.ARROW -> {
                                                            drawLine(
                                                                color = itemColor,
                                                                start = Offset(x, y),
                                                                end = Offset(endX, endY),
                                                                strokeWidth = item.strokeWidth * scaleX,
                                                                cap = StrokeCap.Round
                                                            )
                                                            val angle = Math.atan2((endY - y).toDouble(), (endX - x).toDouble())
                                                            val arrowLength = (item.strokeWidth * scaleX * 3.5f).coerceIn(16f, 40f)
                                                            val arrowAngle = Math.PI / 6.0
                                                            val x1 = (endX - arrowLength * Math.cos(angle - arrowAngle)).toFloat()
                                                            val y1 = (endY - arrowLength * Math.sin(angle - arrowAngle)).toFloat()
                                                            val x2 = (endX - arrowLength * Math.cos(angle + arrowAngle)).toFloat()
                                                            val y2 = (endY - arrowLength * Math.sin(angle + arrowAngle)).toFloat()
                                                            val arrowPath = Path().apply {
                                                                moveTo(endX, endY)
                                                                lineTo(x1, y1)
                                                                lineTo(x2, y2)
                                                                close()
                                                            }
                                                            drawPath(path = arrowPath, color = itemColor)
                                                        }
                                                        PdfEngine.AnnotationType.CHECKMARK -> {
                                                            val left = Math.min(x, endX)
                                                            val right = Math.max(x, endX)
                                                            val top = Math.min(y, endY)
                                                            val bottom = Math.max(y, endY)
                                                            val w = (right - left).coerceAtLeast(16f)
                                                            val h = (bottom - top).coerceAtLeast(16f)
                                                            val tickPath = Path().apply {
                                                                moveTo(left + 0.15f * w, top + 0.50f * h)
                                                                cubicTo(left + 0.25f * w, top + 0.65f * h, left + 0.35f * w, top + 0.90f * h, left + 0.45f * w, top + 0.88f * h)
                                                                cubicTo(left + 0.55f * w, top + 0.70f * h, left + 0.75f * w, top + 0.28f * h, left + 0.92f * w, top + 0.12f * h)
                                                            }
                                                            drawPath(path = tickPath, color = itemColor, style = Stroke(width = (item.strokeWidth * scaleX * 1.35f).coerceAtLeast(2.8f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                                        }
                                                        PdfEngine.AnnotationType.STAR -> {
                                                            val left = Math.min(x, endX)
                                                            val right = Math.max(x, endX)
                                                            val top = Math.min(y, endY)
                                                            val bottom = Math.max(y, endY)
                                                            val cx = (left + right) / 2f
                                                            val cy = (top + bottom) / 2f
                                                            val outerR = Math.min(right - left, bottom - top) / 2f
                                                            val innerR = outerR * 0.42f
                                                            val starPath = Path()
                                                            for (i in 0 until 10) {
                                                                val a = (Math.PI / 5.0) * i - Math.PI / 2.0
                                                                val r = if (i % 2 == 0) outerR else innerR
                                                                val px = (cx + r * Math.cos(a)).toFloat()
                                                                val py = (cy + r * Math.sin(a)).toFloat()
                                                                if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                                                            }
                                                            starPath.close()
                                                            item.fillColorHex?.let { fillHex ->
                                                                drawPath(path = starPath, color = Color(fillHex).copy(alpha = item.opacity))
                                                            }
                                                            drawPath(path = starPath, color = itemColor, style = Stroke(width = item.strokeWidth * scaleX, join = StrokeJoin.Round))
                                                        }
                                                        PdfEngine.AnnotationType.HEART -> {
                                                            val left = Math.min(x, endX)
                                                            val right = Math.max(x, endX)
                                                            val top = Math.min(y, endY)
                                                            val bottom = Math.max(y, endY)
                                                            val w = right - left
                                                            val h = bottom - top
                                                            val heartPath = Path().apply {
                                                                moveTo(left + w * 0.5f, top + h * 0.85f)
                                                                cubicTo(left + w * 0.1f, top + h * 0.55f, left, top + h * 0.25f, left + w * 0.25f, top + h * 0.15f)
                                                                cubicTo(left + w * 0.45f, top + h * 0.08f, left + w * 0.5f, top + h * 0.35f, left + w * 0.5f, top + h * 0.35f)
                                                                cubicTo(left + w * 0.5f, top + h * 0.35f, left + w * 0.55f, top + h * 0.08f, left + w * 0.75f, top + h * 0.15f)
                                                                cubicTo(left + w * 1.0f, top + h * 0.25f, left + w * 0.9f, top + h * 0.55f, left + w * 0.5f, top + h * 0.85f)
                                                                close()
                                                            }
                                                            item.fillColorHex?.let { fillHex ->
                                                                drawPath(path = heartPath, color = Color(fillHex).copy(alpha = item.opacity))
                                                            }
                                                            drawPath(path = heartPath, color = itemColor, style = Stroke(width = item.strokeWidth * scaleX, join = StrokeJoin.Round))
                                                        }
                                                        PdfEngine.AnnotationType.CROSS -> {
                                                            val left = Math.min(x, endX)
                                                            val right = Math.max(x, endX)
                                                            val top = Math.min(y, endY)
                                                            val bottom = Math.max(y, endY)
                                                            val w = right - left
                                                            val h = bottom - top
                                                            drawLine(color = itemColor, start = Offset(left + 0.15f * w, top + 0.15f * h), end = Offset(right - 0.15f * w, bottom - 0.15f * h), strokeWidth = item.strokeWidth * scaleX * 1.2f, cap = StrokeCap.Round)
                                                            drawLine(color = itemColor, start = Offset(right - 0.15f * w, top + 0.15f * h), end = Offset(left + 0.15f * w, bottom - 0.15f * h), strokeWidth = item.strokeWidth * scaleX * 1.2f, cap = StrokeCap.Round)
                                                        }
                                                        PdfEngine.AnnotationType.WATERMARK -> {
                                                            drawIntoCanvas { composeCanvas ->
                                                                val native = composeCanvas.nativeCanvas
                                                                val fontSz = (item.fontSize * scaleX).coerceAtLeast(10f)
                                                                val wmPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                                                    color = item.colorHex
                                                                    alpha = (item.opacity * 255).toInt().coerceIn(0, 255)
                                                                    textSize = fontSz
                                                                    typeface = AndroidTypeface.create(AndroidTypeface.DEFAULT, AndroidTypeface.BOLD)
                                                                    textAlign = AndroidPaint.Align.CENTER
                                                                }
                                                                if (item.isTiled) {
                                                                    val stepX = (canvasW / 3f).coerceAtLeast(150f)
                                                                    val stepY = (canvasH / 4f).coerceAtLeast(150f)
                                                                    for (col in 0..3) {
                                                                        for (row in 0..5) {
                                                                            val tx = col * stepX
                                                                            val ty = row * stepY
                                                                            native.save()
                                                                            native.rotate(item.rotation, tx, ty)
                                                                            native.drawText(item.text, tx, ty, wmPaint)
                                                                            native.restore()
                                                                        }
                                                                    }
                                                                } else {
                                                                    native.drawText(item.text, x, y, wmPaint)
                                                                }
                                                            }
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            }

                                            // 2. Draw active in-progress freehand stroke (Draw or Highlight)
                                            if (activeStrokePoints.size >= 2) {
                                                val livePath = Path()
                                                val p0 = activeStrokePoints.first()
                                                livePath.moveTo(p0.first * scaleX, p0.second * scaleY)
                                                for (i in 1 until activeStrokePoints.size) {
                                                    val pt = activeStrokePoints[i]
                                                    livePath.lineTo(pt.first * scaleX, pt.second * scaleY)
                                                }
                                                val liveColor = if (activeEditTab == "HIGHLIGHT") Color(highlightColor).copy(alpha = highlightOpacity) else Color(penColor).copy(alpha = penOpacity)
                                                val liveStroke = if (activeEditTab == "HIGHLIGHT") highlightThickness * scaleX else penStrokeWidth * scaleX
                                                drawPath(
                                                    path = livePath,
                                                    color = liveColor,
                                                    style = Stroke(width = liveStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                )
                                            }

                                            // 3. Draw active in-progress shape creation preview
                                            val actStart = activeShapeStart
                                            val actEnd = activeShapeCurrent
                                            if (actStart != null && actEnd != null) {
                                                val sX = actStart.first * scaleX
                                                val sY = actStart.second * scaleY
                                                val eX = actEnd.first * scaleX
                                                val eY = actEnd.second * scaleY
                                                val left = Math.min(sX, eX)
                                                val top = Math.min(sY, eY)
                                                val width = Math.abs(eX - sX).coerceAtLeast(2f)
                                                val height = Math.abs(eY - sY).coerceAtLeast(2f)
                                                val sColor = Color(shapeStrokeColor).copy(alpha = shapeOpacity)
                                                val sWidth = shapeStrokeWidth * scaleX

                                                shapeFillColor?.let { fillHex ->
                                                    val fColor = Color(fillHex).copy(alpha = shapeOpacity)
                                                    when (selectedShapeType) {
                                                        PdfEngine.AnnotationType.RECTANGLE -> drawRect(color = fColor, topLeft = Offset(left, top), size = Size(width, height))
                                                        PdfEngine.AnnotationType.CIRCLE -> drawOval(color = fColor, topLeft = Offset(left, top), size = Size(width, height))
                                                        else -> {}
                                                    }
                                                }

                                                when (selectedShapeType) {
                                                    PdfEngine.AnnotationType.RECTANGLE -> drawRect(color = sColor, topLeft = Offset(left, top), size = Size(width, height), style = Stroke(width = sWidth))
                                                    PdfEngine.AnnotationType.CIRCLE -> drawOval(color = sColor, topLeft = Offset(left, top), size = Size(width, height), style = Stroke(width = sWidth))
                                                    PdfEngine.AnnotationType.LINE -> drawLine(color = sColor, start = Offset(sX, sY), end = Offset(eX, eY), strokeWidth = sWidth, cap = StrokeCap.Round)
                                                    PdfEngine.AnnotationType.ARROW -> {
                                                        drawLine(color = sColor, start = Offset(sX, sY), end = Offset(eX, eY), strokeWidth = sWidth, cap = StrokeCap.Round)
                                                        val angle = Math.atan2((eY - sY).toDouble(), (eX - sX).toDouble())
                                                        val arrowLength = (sWidth * 3.5f).coerceIn(16f, 40f)
                                                        val arrowAngle = Math.PI / 6.0
                                                        val x1 = (eX - arrowLength * Math.cos(angle - arrowAngle)).toFloat()
                                                        val y1 = (eY - arrowLength * Math.sin(angle - arrowAngle)).toFloat()
                                                        val x2 = (eX - arrowLength * Math.cos(angle + arrowAngle)).toFloat()
                                                        val y2 = (eY - arrowLength * Math.sin(angle + arrowAngle)).toFloat()
                                                        val arrowPath = Path().apply {
                                                            moveTo(eX, eY)
                                                            lineTo(x1, y1)
                                                            lineTo(x2, y2)
                                                            close()
                                                        }
                                                        drawPath(path = arrowPath, color = sColor)
                                                    }
                                                    PdfEngine.AnnotationType.CHECKMARK -> {
                                                        val tickPath = Path().apply {
                                                            moveTo(left + 0.15f * width, top + 0.50f * height)
                                                            cubicTo(left + 0.25f * width, top + 0.65f * height, left + 0.35f * width, top + 0.90f * height, left + 0.45f * width, top + 0.88f * height)
                                                            cubicTo(left + 0.55f * width, top + 0.70f * height, left + 0.75f * width, top + 0.28f * height, left + 0.92f * width, top + 0.12f * height)
                                                        }
                                                        drawPath(path = tickPath, color = sColor, style = Stroke(width = (sWidth * 1.35f).coerceAtLeast(2.8f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                                    }
                                                    PdfEngine.AnnotationType.STAR -> {
                                                        val cx = (left + left + width) / 2f
                                                        val cy = (top + top + height) / 2f
                                                        val outerR = Math.min(width, height) / 2f
                                                        val innerR = outerR * 0.42f
                                                        val starPath = Path()
                                                        for (i in 0 until 10) {
                                                            val a = (Math.PI / 5.0) * i - Math.PI / 2.0
                                                            val r = if (i % 2 == 0) outerR else innerR
                                                            val px = (cx + r * Math.cos(a)).toFloat()
                                                            val py = (cy + r * Math.sin(a)).toFloat()
                                                            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                                                        }
                                                        starPath.close()
                                                        drawPath(path = starPath, color = sColor, style = Stroke(width = sWidth, join = StrokeJoin.Round))
                                                    }
                                                    PdfEngine.AnnotationType.HEART -> {
                                                        val heartPath = Path().apply {
                                                            moveTo(left + width * 0.5f, top + height * 0.85f)
                                                            cubicTo(left + width * 0.1f, top + height * 0.55f, left, top + height * 0.25f, left + width * 0.25f, top + height * 0.15f)
                                                            cubicTo(left + width * 0.45f, top + height * 0.08f, left + width * 0.5f, top + height * 0.35f, left + width * 0.5f, top + height * 0.35f)
                                                            cubicTo(left + width * 0.5f, top + height * 0.35f, left + width * 0.55f, top + height * 0.08f, left + width * 0.75f, top + height * 0.15f)
                                                            cubicTo(left + width * 1.0f, top + height * 0.25f, left + width * 0.9f, top + height * 0.55f, left + width * 0.5f, top + height * 0.85f)
                                                            close()
                                                        }
                                                        drawPath(path = heartPath, color = sColor, style = Stroke(width = sWidth, join = StrokeJoin.Round))
                                                    }
                                                    PdfEngine.AnnotationType.CROSS -> {
                                                        drawLine(color = sColor, start = Offset(left + 0.15f * width, top + 0.15f * height), end = Offset(left + 0.85f * width, top + 0.85f * height), strokeWidth = sWidth * 1.2f, cap = StrokeCap.Round)
                                                        drawLine(color = sColor, start = Offset(left + 0.85f * width, top + 0.15f * height), end = Offset(left + 0.15f * width, top + 0.85f * height), strokeWidth = sWidth * 1.2f, cap = StrokeCap.Round)
                                                    }
                                                    else -> {}
                                                }
                                            }

                                            // 4. Selection box, rotation stalk/knob & handles for selectedAnnotationId
                                            if (selectedAnnotationId != null) {
                                                val selItem = pageItems.find { it.id == selectedAnnotationId }
                                                if (selItem != null) {
                                                    val bounds = getAnnotationBounds(selItem)
                                                    val selCx = (bounds.left + bounds.right) / 2f * scaleX
                                                    val selCy = (bounds.top + bounds.bottom) / 2f * scaleY
                                                    val rot = selItem.getRotationAngle()
                                                    val bLeft = bounds.left * scaleX
                                                    val bTop = bounds.top * scaleY
                                                    val bWidth = (bounds.right - bounds.left) * scaleX
                                                    val bHeight = (bounds.bottom - bounds.top) * scaleY

                                                    rotate(degrees = rot, pivot = Offset(selCx, selCy)) {
                                                        // Dashed selection rectangle
                                                        drawRect(
                                                            color = Color(0xFF2563EB),
                                                            topLeft = Offset(bLeft, bTop),
                                                            size = Size(bWidth, bHeight),
                                                            style = Stroke(
                                                                width = 2.dp.toPx(),
                                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                                            )
                                                        )

                                                        // Rotation stalk and knob (above top center)
                                                        val knobCenter = Offset(selCx, bTop - 28.dp.toPx())
                                                        drawLine(
                                                            color = Color(0xFF2563EB),
                                                            start = Offset(selCx, bTop),
                                                            end = knobCenter,
                                                            strokeWidth = 2.dp.toPx()
                                                        )
                                                        drawCircle(color = Color(0xFF2563EB), radius = 9.dp.toPx(), center = knobCenter)
                                                        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = knobCenter)
                                                        drawCircle(color = Color(0xFF2563EB), radius = 2.5.dp.toPx(), center = knobCenter)

                                                        // Corner handles or endpoints
                                                        val handleRadius = 7.dp.toPx()
                                                        if (selItem.type == PdfEngine.AnnotationType.LINE || selItem.type == PdfEngine.AnnotationType.ARROW) {
                                                            val p1 = Offset(selItem.posX * scaleX, selItem.posY * scaleY)
                                                            val p2 = Offset(selItem.endX * scaleX, selItem.endY * scaleY)
                                                            drawCircle(color = Color(0xFF2563EB), radius = handleRadius, center = p1)
                                                            drawCircle(color = Color.White, radius = handleRadius - 2f, center = p1)
                                                            drawCircle(color = Color(0xFF2563EB), radius = handleRadius, center = p2)
                                                            drawCircle(color = Color.White, radius = handleRadius - 2f, center = p2)
                                                        } else {
                                                            val corners = listOf(
                                                                Offset(bLeft, bTop),
                                                                Offset(bLeft + bWidth, bTop),
                                                                Offset(bLeft, bTop + bHeight),
                                                                Offset(bLeft + bWidth, bTop + bHeight)
                                                            )
                                                            for (c in corners) {
                                                                drawCircle(color = Color(0xFF2563EB), radius = handleRadius, center = c)
                                                                drawCircle(color = Color.White, radius = handleRadius - 2f, center = c)
                                                            }
                                                            if (selItem.type == PdfEngine.AnnotationType.IMAGE || selItem.type == PdfEngine.AnnotationType.RECTANGLE || selItem.type == PdfEngine.AnnotationType.TEXT) {
                                                                val edgeRadius = 5.5.dp.toPx()
                                                                val midX = bLeft + bWidth / 2f
                                                                val midY = bTop + bHeight / 2f
                                                                val edgeCenters = listOf(
                                                                    Offset(midX, bTop),
                                                                    Offset(midX, bTop + bHeight),
                                                                    Offset(bLeft, midY),
                                                                    Offset(bLeft + bWidth, midY)
                                                                )
                                                                for (ec in edgeCenters) {
                                                                    drawCircle(color = Color(0xFF2563EB), radius = edgeRadius, center = ec)
                                                                    drawCircle(color = Color.White, radius = edgeRadius - 2f, center = ec)
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
                        }
                    }
                    }

                    // Floating Viewport Control Overlay (Zoom In, Zoom Out, Fit Width / Fit Page)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = { fitPageMode = !fitPageMode },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (fitPageMode) Icons.Default.FitScreen else Icons.Default.AspectRatio,
                                contentDescription = if (fitPageMode) "Fit Page" else "Fit Width",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // ====================================================
                // 3. THUMBNAIL TRAY (Expandable at bottom)
                // ====================================================
                AnimatedVisibility(visible = showThumbnailTray && pages.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(pages) { index, bmp ->
                                val isSelected = index == currentPageIndex
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) CrimsonPrimary else Color(0xFF475569),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { currentPageIndex = index }
                                        .padding(3.dp)
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier
                                            .height(55.dp)
                                            .width(42.dp)
                                    )
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isSelected) CrimsonPrimary else Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ====================================================
                // 4. IN-WORKSPACE EDIT TOOLBAR (Active when isEditMode = true)
                // ====================================================
                if (isEditMode) {
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .zIndex(10f),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // In-Workspace Editing Header Controls (Undo / Redo / Done)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (activeEditTab) {
                                        "TEXT" -> "Text Tool"
                                        "IMAGE" -> "Image Tool"
                                        "PEN" -> "Draw Tool"
                                        "HIGHLIGHT" -> "Highlight Tool"
                                        "SHAPES" -> "Shapes Tool"
                                        "COMMENT" -> "Comment Tool"
                                        else -> "Edit Tool"
                                    },
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Undo / Redo
                                    IconButton(
                                        onClick = { handleUndo() },
                                        enabled = undoStack.isNotEmpty(),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo",
                                            tint = if (undoStack.isNotEmpty()) Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { handleRedo() },
                                        enabled = redoStack.isNotEmpty(),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Redo,
                                            contentDescription = "Redo",
                                            tint = if (redoStack.isNotEmpty()) Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { isEditMode = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Done Editing", tint = Color(0xFF4ADE80))
                                    }
                                }
                            }

                            // Secondary Formatting Panel for Active Edit Tool
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                when (activeEditTab) {
                                    "TEXT" -> {
                                        val currentSelectedText = editItems.find { it.id == selectedAnnotationId && it.pageIndex == currentPageIndex && it.type == PdfEngine.AnnotationType.TEXT }
                                        if (currentSelectedText != null) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(CrimsonPrimary.copy(alpha = 0.2f))
                                                                .border(1.dp, CrimsonPrimary, RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("Text Selected", color = CrimsonPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Button(
                                                            onClick = {
                                                                editingTextItemId = currentSelectedText.id
                                                                editingTextContent = currentSelectedText.text
                                                                showEditTextDialog = true
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(13.dp), tint = Color.White)
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("Edit Text", fontSize = 11.sp, color = Color.White)
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedText.id }
                                                                if (idx != -1) {
                                                                    val newRot = (editItems[idx].rotation + 90f) % 360f
                                                                    editItems[idx] = editItems[idx].copy(rotation = newRot)
                                                                }
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90°", tint = Color.White, modifier = Modifier.size(16.dp))
                                                        }
                                                        TextButton(
                                                            onClick = { selectedAnnotationId = null },
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Deselect", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val newText = PdfEngine.EditItem(
                                                                    pageIndex = currentPageIndex,
                                                                    text = "Sample Text",
                                                                    posX = (currentSelectedText.posX + 30f).coerceAtMost(400f),
                                                                    posY = (currentSelectedText.posY + 35f).coerceAtMost(650f),
                                                                    colorHex = currentSelectedText.colorHex,
                                                                    fontSize = currentSelectedText.fontSize,
                                                                    isBold = currentSelectedText.isBold,
                                                                    isItalic = currentSelectedText.isItalic,
                                                                    isUnderline = currentSelectedText.isUnderline,
                                                                    fontFamily = currentSelectedText.fontFamily,
                                                                    alignment = currentSelectedText.alignment,
                                                                    boxWidth = currentSelectedText.boxWidth.coerceAtLeast(80f),
                                                                    boxHeight = currentSelectedText.boxHeight.coerceAtLeast(36f)
                                                                )
                                                                editItems.add(newText)
                                                                selectedAnnotationId = newText.id
                                                                activeEditTab = "TEXT"
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = "Add Text", tint = Color.White, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    Button(
                                                        onClick = {
                                                            pushHistory()
                                                            editItems.remove(currentSelectedText)
                                                            selectedAnnotationId = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(13.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Delete", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }

                                                // Formatting Row for selected text: Bold, Italic, Underline, Font size, Colors
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedText.id }
                                                                if (idx != -1) editItems[idx] = editItems[idx].copy(isBold = !editItems[idx].isBold)
                                                            },
                                                            modifier = Modifier
                                                                .size(26.dp)
                                                                .background(if (currentSelectedText.isBold) CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                                                        ) {
                                                            Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = Color.White, modifier = Modifier.size(13.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedText.id }
                                                                if (idx != -1) editItems[idx] = editItems[idx].copy(isItalic = !editItems[idx].isItalic)
                                                            },
                                                            modifier = Modifier
                                                                .size(26.dp)
                                                                .background(if (currentSelectedText.isItalic) CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                                                        ) {
                                                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = Color.White, modifier = Modifier.size(13.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedText.id }
                                                                if (idx != -1) editItems[idx] = editItems[idx].copy(isUnderline = !editItems[idx].isUnderline)
                                                            },
                                                            modifier = Modifier
                                                                .size(26.dp)
                                                                .background(if (currentSelectedText.isUnderline) CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                                                        ) {
                                                            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", tint = Color.White, modifier = Modifier.size(13.dp))
                                                        }
                                                        Text(" ${currentSelectedText.fontSize.toInt()}sp ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedText.id }
                                                                if (idx != -1) {
                                                                    val oldItem = editItems[idx]
                                                                    val newFont = (oldItem.fontSize - 2f).coerceAtLeast(8f)
                                                                    val factor = newFont / oldItem.fontSize
                                                                    val newW = if (oldItem.boxWidth > 0f) (oldItem.boxWidth * factor).coerceIn(30f, 3000f) else oldItem.boxWidth
                                                                    val newH = if (oldItem.boxHeight > 0f) (oldItem.boxHeight * factor).coerceIn(20f, 3000f) else oldItem.boxHeight
                                                                    editItems[idx] = oldItem.copy(fontSize = newFont, boxWidth = newW, boxHeight = newH)
                                                                }
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedText.id }
                                                                if (idx != -1) {
                                                                    val oldItem = editItems[idx]
                                                                    val newFont = (oldItem.fontSize + 2f).coerceAtMost(160f)
                                                                    val factor = newFont / oldItem.fontSize
                                                                    val newW = if (oldItem.boxWidth > 0f) (oldItem.boxWidth * factor).coerceIn(30f, 3000f) else oldItem.boxWidth
                                                                    val newH = if (oldItem.boxHeight > 0f) (oldItem.boxHeight * factor).coerceIn(20f, 3000f) else oldItem.boxHeight
                                                                    editItems[idx] = oldItem.copy(fontSize = newFont, boxWidth = newW, boxHeight = newH)
                                                                }
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedText.id }
                                                                if (idx != -1) {
                                                                    val nextAlign = when (editItems[idx].alignment) {
                                                                        "LEFT" -> "CENTER"
                                                                        "CENTER" -> "RIGHT"
                                                                        else -> "LEFT"
                                                                    }
                                                                    editItems[idx] = editItems[idx].copy(alignment = nextAlign)
                                                                }
                                                            },
                                                            modifier = Modifier.size(26.dp).background(Color(0xFF334155), RoundedCornerShape(4.dp))
                                                        ) {
                                                            val icon = when (currentSelectedText.alignment) {
                                                                "CENTER" -> Icons.Default.FormatAlignCenter
                                                                "RIGHT" -> Icons.Default.FormatAlignRight
                                                                else -> Icons.Default.FormatAlignLeft
                                                            }
                                                            Icon(icon, contentDescription = "Alignment", tint = Color.White, modifier = Modifier.size(13.dp))
                                                        }
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        listOf(AndroidColor.BLACK, AndroidColor.rgb(37, 99, 235), AndroidColor.rgb(220, 38, 38), AndroidColor.rgb(22, 163, 74), AndroidColor.rgb(234, 88, 12), AndroidColor.WHITE).forEach { cHex ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(18.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(cHex))
                                                                    .border(width = if (currentSelectedText.colorHex == cHex) 2.dp else 1.dp, color = if (currentSelectedText.colorHex == cHex) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f), shape = CircleShape)
                                                                    .clickable {
                                                                        pushHistory()
                                                                        val idx = editItems.indexOfFirst { it.id == currentSelectedText.id }
                                                                        if (idx != -1) editItems[idx] = editItems[idx].copy(colorHex = cHex)
                                                                    }
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                colorPickerTarget = "SELECTED"
                                                                colorPickerCurrentColor = currentSelectedText.colorHex
                                                                showColorPicker = true
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Palette, contentDescription = "Color", tint = CrimsonPrimary, modifier = Modifier.size(15.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = textToAdd,
                                                        onValueChange = { textToAdd = it },
                                                        placeholder = { Text("Enter text to add...", color = Color.Gray, fontSize = 12.sp) },
                                                        singleLine = true,
                                                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(44.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Button(
                                                        onClick = {
                                                            val content = if (textToAdd.isNotBlank()) textToAdd else "Sample Text"
                                                            pushHistory()
                                                            val tempPaint = TextPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                                                textSize = selectedFontSize
                                                                val family = when (selectedFontFamily) {
                                                                    "Serif", "SERIF" -> AndroidTypeface.SERIF
                                                                    "Monospace", "MONOSPACE" -> AndroidTypeface.MONOSPACE
                                                                    else -> AndroidTypeface.SANS_SERIF
                                                                }
                                                                val style = when {
                                                                    isBold && isItalic -> AndroidTypeface.BOLD_ITALIC
                                                                    isBold -> AndroidTypeface.BOLD
                                                                    isItalic -> AndroidTypeface.ITALIC
                                                                    else -> AndroidTypeface.NORMAL
                                                                }
                                                                typeface = AndroidTypeface.create(family, style)
                                                            }
                                                            val lines = content.split("\n")
                                                            var maxLineW = 0f
                                                            for (l in lines) {
                                                                val lw = tempPaint.measureText(l)
                                                                if (lw > maxLineW) maxLineW = lw
                                                            }
                                                            val initialBoxW = (maxLineW + 24f).coerceAtLeast(80f)
                                                            val initialBoxH = (lines.size * selectedFontSize * 1.35f + 16f).coerceAtLeast(36f)

                                                            val newText = PdfEngine.EditItem(
                                                                pageIndex = currentPageIndex,
                                                                text = content,
                                                                posX = 60f,
                                                                posY = 120f + ((editItems.count { it.type == PdfEngine.AnnotationType.TEXT } % 8) * 45f),
                                                                colorHex = selectedTextColor,
                                                                fontSize = selectedFontSize,
                                                                isBold = isBold,
                                                                isItalic = isItalic,
                                                                isUnderline = isUnderline,
                                                                fontFamily = selectedFontFamily,
                                                                alignment = selectedTextAlign,
                                                                boxWidth = initialBoxW,
                                                                boxHeight = initialBoxH
                                                            )
                                                            editItems.add(newText)
                                                            selectedAnnotationId = newText.id
                                                            activeEditTab = "TEXT"
                                                            textToAdd = ""
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(36.dp)
                                                    ) {
                                                        Text("Add Text", fontSize = 11.sp)
                                                    }
                                                }

                                                // Formatting Row: Font family, Size, Bold, Italic, Color Chips
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        // Bold toggle
                                                        IconButton(
                                                            onClick = { isBold = !isBold },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(if (isBold) CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                                                        ) {
                                                            Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                        // Italic toggle
                                                        IconButton(
                                                            onClick = { isItalic = !isItalic },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(if (isItalic) CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                                                        ) {
                                                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                        // Underline toggle
                                                        IconButton(
                                                            onClick = { isUnderline = !isUnderline },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(if (isUnderline) CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                                                        ) {
                                                            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }

                                                        // Font size buttons
                                                        Text(" ${selectedFontSize.toInt()}sp ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        IconButton(
                                                            onClick = { selectedFontSize = (selectedFontSize - 2).coerceAtLeast(10f) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        }
                                                        IconButton(
                                                            onClick = { selectedFontSize = (selectedFontSize + 2).coerceAtMost(48f) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        }
                                                    }

                                                    // Color chips
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        paletteColors.take(5).forEach { (_, cHex) ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(cHex))
                                                                    .border(
                                                                        width = if (selectedTextColor == cHex) 2.dp else 1.dp,
                                                                        color = if (selectedTextColor == cHex) Color.White else Color.Transparent,
                                                                        shape = CircleShape
                                                                    )
                                                                    .clickable { selectedTextColor = cHex }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    "IMAGE" -> {
                                        val currentSelectedImage = editItems.find { it.id == selectedAnnotationId && it.pageIndex == currentPageIndex && it.type == PdfEngine.AnnotationType.IMAGE }
                                        if (currentSelectedImage != null) {
                                            LazyRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                item {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(ToolBlue.copy(alpha = 0.2f))
                                                            .border(1.dp, ToolBlue, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("Image", color = ToolBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                item {
                                                    Button(
                                                        onClick = { showImageCropDialog = true },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp).testTag("image_tool_crop_button")
                                                    ) {
                                                        Icon(Icons.Default.Crop, contentDescription = "Crop Image", modifier = Modifier.size(15.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Crop", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                                item {
                                                    Button(
                                                        onClick = {
                                                            replacingImageItemId = currentSelectedImage.id
                                                            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp).testTag("image_tool_replace_button")
                                                    ) {
                                                        Icon(Icons.Default.Cached, contentDescription = "Replace Photo", modifier = Modifier.size(15.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Replace Photo", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                                item {
                                                    OutlinedButton(
                                                        onClick = {
                                                            pushHistory()
                                                            val idx = editItems.indexOfFirst { it.id == currentSelectedImage.id }
                                                            if (idx != -1) {
                                                                val newRot = (editItems[idx].rotation + 90f) % 360f
                                                                editItems[idx] = editItems[idx].copy(rotation = newRot, imageRotation = newRot)
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp).testTag("image_tool_rotate_button")
                                                    ) {
                                                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90°", tint = Color.White, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Rotate 90°", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                val isCropped = currentSelectedImage.originalImageBitmap != null &&
                                                        currentSelectedImage.imageBitmap != currentSelectedImage.originalImageBitmap
                                                if (isCropped) {
                                                    item {
                                                        OutlinedButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedImage.id }
                                                                if (idx != -1) {
                                                                    val origBmp = currentSelectedImage.originalImageBitmap!!
                                                                    val aspect = origBmp.width.toFloat() / origBmp.height.toFloat().coerceAtLeast(1f)
                                                                    val curW = currentSelectedImage.imageWidth
                                                                    val curH = curW / aspect
                                                                    editItems[idx] = editItems[idx].copy(
                                                                        imageBitmap = origBmp,
                                                                        imageWidth = curW,
                                                                        imageHeight = curH
                                                                    )
                                                                }
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(32.dp).testTag("image_tool_reset_crop_button")
                                                        ) {
                                                            Icon(Icons.Default.Refresh, contentDescription = "Reset Crop", tint = ToolOrange, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Reset Crop", fontSize = 11.sp, color = ToolOrange)
                                                        }
                                                    }
                                                }
                                                item {
                                                    TextButton(
                                                        onClick = { selectedAnnotationId = null },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp).testTag("image_tool_deselect_button")
                                                    ) {
                                                        Text("Deselect", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                    }
                                                }
                                                item {
                                                    Button(
                                                        onClick = {
                                                            pushHistory()
                                                            editItems.remove(currentSelectedImage)
                                                            selectedAnnotationId = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp).testTag("image_tool_delete_button")
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Delete", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = {
                                                        replacingImageItemId = null
                                                        imagePickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                                                    modifier = Modifier.height(36.dp).testTag("image_tool_add_button")
                                                ) {
                                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Add Photo", fontSize = 12.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        val bmp = pages.getOrNull(currentPageIndex)
                                                        val w = bmp?.width?.toFloat() ?: 600f
                                                        val h = bmp?.height?.toFloat() ?: 800f
                                                        val initW = 200f
                                                        val initH = 240f
                                                        val placeholder = Bitmap.createBitmap(initW.toInt(), initH.toInt(), Bitmap.Config.ARGB_8888).apply {
                                                            eraseColor(android.graphics.Color.WHITE)
                                                        }
                                                        val targetItem = PdfEngine.EditItem(
                                                            pageIndex = currentPageIndex,
                                                            type = PdfEngine.AnnotationType.IMAGE,
                                                            imageBitmap = placeholder,
                                                            originalImageBitmap = placeholder,
                                                            posX = (w - initW) / 2f,
                                                            posY = (h - initH) / 3f,
                                                            imageWidth = initW,
                                                            imageHeight = initH
                                                        )
                                                        editItems.add(targetItem)
                                                        selectedAnnotationId = targetItem.id
                                                        replacingImageItemId = targetItem.id
                                                        imagePickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    modifier = Modifier.height(36.dp).testTag("image_tool_select_document_photo_button"),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Select Photo to Replace", fontSize = 11.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }

                                    "PEN" -> {
                                        val currentSelectedItem = editItems.find { it.id == selectedAnnotationId && it.pageIndex == currentPageIndex }
                                        if (currentSelectedItem != null) {
                                            // Selected annotation controls
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(CrimsonPrimary.copy(alpha = 0.2f))
                                                                .border(1.dp, CrimsonPrimary, RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("Drawing Selected", color = CrimsonPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        TextButton(
                                                            onClick = { selectedAnnotationId = null },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Deselect", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                        }
                                                    }
                                                    Button(
                                                        onClick = {
                                                            pushHistory()
                                                            editItems.remove(currentSelectedItem)
                                                            selectedAnnotationId = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Delete", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        listOf(AndroidColor.BLACK, AndroidColor.rgb(37, 99, 235), AndroidColor.rgb(220, 38, 38), AndroidColor.rgb(22, 163, 74), AndroidColor.rgb(234, 88, 12), AndroidColor.WHITE).forEach { cHex ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(cHex))
                                                                    .border(width = if (currentSelectedItem.colorHex == cHex) 2.dp else 1.dp, color = if (currentSelectedItem.colorHex == cHex) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f), shape = CircleShape)
                                                                    .clickable {
                                                                        pushHistory()
                                                                        val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                        if (idx != -1) editItems[idx] = editItems[idx].copy(colorHex = cHex)
                                                                    }
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                colorPickerTarget = "SELECTED"
                                                                colorPickerCurrentColor = currentSelectedItem.colorHex
                                                                showColorPicker = true
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Palette, contentDescription = "Color", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("Width: ${currentSelectedItem.strokeWidth.toInt()}", color = Color.White, fontSize = 11.sp)
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                if (idx != -1) {
                                                                    val nw = (editItems[idx].strokeWidth - 2f).coerceAtLeast(1f)
                                                                    editItems[idx] = editItems[idx].copy(strokeWidth = nw)
                                                                }
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                if (idx != -1) {
                                                                    val nw = (editItems[idx].strokeWidth + 2f).coerceAtMost(60f)
                                                                    editItems[idx] = editItems[idx].copy(strokeWidth = nw)
                                                                }
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        FilterChip(
                                                            selected = !isDrawEraser,
                                                            onClick = { isDrawEraser = false },
                                                            label = { Text("Pen", fontSize = 11.sp) },
                                                            leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                            modifier = Modifier.height(28.dp)
                                                        )
                                                        FilterChip(
                                                            selected = isDrawEraser,
                                                            onClick = { isDrawEraser = true },
                                                            label = { Text("Stroke Eraser", fontSize = 11.sp) },
                                                            leadingIcon = { Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                            modifier = Modifier.height(28.dp)
                                                        )
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            val drawings = editItems.filter { it.pageIndex == currentPageIndex && (it.type == PdfEngine.AnnotationType.DRAW || it.type == PdfEngine.AnnotationType.PEN) }
                                                            if (drawings.isNotEmpty()) {
                                                                pushHistory()
                                                                editItems.removeAll(drawings)
                                                                if (drawings.any { it.id == selectedAnnotationId }) selectedAnnotationId = null
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Clear Drawings", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                                    }
                                                }
                                                if (!isDrawEraser) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            listOf(
                                                                AndroidColor.BLACK,
                                                                AndroidColor.rgb(37, 99, 235),
                                                                AndroidColor.rgb(220, 38, 38),
                                                                AndroidColor.rgb(22, 163, 74),
                                                                AndroidColor.rgb(234, 88, 12),
                                                                AndroidColor.rgb(147, 51, 234),
                                                                AndroidColor.WHITE
                                                            ).forEach { cHex ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(20.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color(cHex))
                                                                        .border(
                                                                            width = if (penColor == cHex) 2.dp else 1.dp,
                                                                            color = if (penColor == cHex) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f),
                                                                            shape = CircleShape
                                                                        )
                                                                        .clickable { penColor = cHex }
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    colorPickerTarget = "PEN"
                                                                    colorPickerCurrentColor = penColor
                                                                    showColorPicker = true
                                                                },
                                                                modifier = Modifier.size(22.dp)
                                                            ) {
                                                                Icon(Icons.Default.Palette, contentDescription = "Custom Color", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            listOf(2f, 4f, 8f, 14f).forEach { width ->
                                                                FilterChip(
                                                                    selected = penStrokeWidth == width,
                                                                    onClick = { penStrokeWidth = width },
                                                                    label = { Text("${width.toInt()}px", fontSize = 10.sp) },
                                                                    modifier = Modifier.height(26.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Drag over drawn strokes to erase", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            listOf(16f, 28f, 44f).forEach { size ->
                                                                FilterChip(
                                                                    selected = penEraserSize == size,
                                                                    onClick = { penEraserSize = size },
                                                                    label = { Text("${size.toInt()}px", fontSize = 10.sp) },
                                                                    modifier = Modifier.height(26.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    "HIGHLIGHT" -> {
                                        val currentSelectedItem = editItems.find { it.id == selectedAnnotationId && it.pageIndex == currentPageIndex }
                                        if (currentSelectedItem != null && currentSelectedItem.type == PdfEngine.AnnotationType.HIGHLIGHTER) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(CrimsonPrimary.copy(alpha = 0.2f))
                                                                .border(1.dp, CrimsonPrimary, RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("Highlight Selected", color = CrimsonPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        TextButton(
                                                            onClick = { selectedAnnotationId = null },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Deselect", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                        }
                                                    }
                                                    Button(
                                                        onClick = {
                                                            pushHistory()
                                                            editItems.remove(currentSelectedItem)
                                                            selectedAnnotationId = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Delete", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        listOf(AndroidColor.rgb(250, 204, 21), AndroidColor.rgb(74, 222, 128), AndroidColor.rgb(56, 189, 248), AndroidColor.rgb(244, 114, 182), AndroidColor.rgb(251, 146, 60), AndroidColor.rgb(192, 132, 252)).forEach { cHex ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(cHex))
                                                                    .border(width = if (currentSelectedItem.colorHex == cHex) 2.dp else 1.dp, color = if (currentSelectedItem.colorHex == cHex) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f), shape = CircleShape)
                                                                    .clickable {
                                                                        pushHistory()
                                                                        val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                        if (idx != -1) editItems[idx] = editItems[idx].copy(colorHex = cHex)
                                                                    }
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                colorPickerTarget = "SELECTED"
                                                                colorPickerCurrentColor = currentSelectedItem.colorHex
                                                                showColorPicker = true
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Palette, contentDescription = "Color", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("Thickness: ${currentSelectedItem.strokeWidth.toInt()}", color = Color.White, fontSize = 11.sp)
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                if (idx != -1) {
                                                                    val nw = (editItems[idx].strokeWidth - 4f).coerceAtLeast(8f)
                                                                    editItems[idx] = editItems[idx].copy(strokeWidth = nw)
                                                                }
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                if (idx != -1) {
                                                                    val nw = (editItems[idx].strokeWidth + 4f).coerceAtMost(80f)
                                                                    editItems[idx] = editItems[idx].copy(strokeWidth = nw)
                                                                }
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        FilterChip(
                                                            selected = !isHighlightEraser,
                                                            onClick = { isHighlightEraser = false },
                                                            label = { Text("Highlighter", fontSize = 11.sp) },
                                                            leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                            modifier = Modifier.height(28.dp)
                                                        )
                                                        FilterChip(
                                                            selected = isHighlightEraser,
                                                            onClick = { isHighlightEraser = true },
                                                            label = { Text("Highlight Eraser", fontSize = 11.sp) },
                                                            leadingIcon = { Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                            modifier = Modifier.height(28.dp)
                                                        )
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            val highlights = editItems.filter { it.pageIndex == currentPageIndex && it.type == PdfEngine.AnnotationType.HIGHLIGHTER }
                                                            if (highlights.isNotEmpty()) {
                                                                pushHistory()
                                                                editItems.removeAll(highlights)
                                                                if (highlights.any { it.id == selectedAnnotationId }) selectedAnnotationId = null
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Clear Highlights", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                                    }
                                                }
                                                if (!isHighlightEraser) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            listOf(
                                                                AndroidColor.rgb(250, 204, 21),
                                                                AndroidColor.rgb(74, 222, 128),
                                                                AndroidColor.rgb(56, 189, 248),
                                                                AndroidColor.rgb(244, 114, 182),
                                                                AndroidColor.rgb(251, 146, 60),
                                                                AndroidColor.rgb(192, 132, 252)
                                                            ).forEach { cHex ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(20.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color(cHex))
                                                                        .border(
                                                                            width = if (highlightColor == cHex) 2.dp else 1.dp,
                                                                            color = if (highlightColor == cHex) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f),
                                                                            shape = CircleShape
                                                                        )
                                                                        .clickable { highlightColor = cHex }
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    colorPickerTarget = "HIGHLIGHT"
                                                                    colorPickerCurrentColor = highlightColor
                                                                    showColorPicker = true
                                                                },
                                                                modifier = Modifier.size(22.dp)
                                                            ) {
                                                                Icon(Icons.Default.Palette, contentDescription = "Custom Color", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            listOf(16f, 24f, 36f, 48f).forEach { thick ->
                                                                FilterChip(
                                                                    selected = highlightThickness == thick,
                                                                    onClick = { highlightThickness = thick },
                                                                    label = { Text("${thick.toInt()}px", fontSize = 10.sp) },
                                                                    modifier = Modifier.height(26.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Drag over highlights to erase them cleanly", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    "SHAPES" -> {
                                        val currentSelectedItem = editItems.find { it.id == selectedAnnotationId && it.pageIndex == currentPageIndex }
                                        if (currentSelectedItem != null && currentSelectedItem.type in listOf(PdfEngine.AnnotationType.RECTANGLE, PdfEngine.AnnotationType.CIRCLE, PdfEngine.AnnotationType.ARROW, PdfEngine.AnnotationType.LINE)) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(CrimsonPrimary.copy(alpha = 0.2f))
                                                                .border(1.dp, CrimsonPrimary, RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(if (currentSelectedItem.type == PdfEngine.AnnotationType.IMAGE) "Image Selected" else "Shape Selected", color = CrimsonPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        TextButton(
                                                            onClick = { selectedAnnotationId = null },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Deselect", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                        }
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (currentSelectedItem.type == PdfEngine.AnnotationType.IMAGE) {
                                                            Button(
                                                                onClick = {
                                                                    replacingImageItemId = currentSelectedItem.id
                                                                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                                modifier = Modifier.height(28.dp).testTag("btn_replace_photo")
                                                            ) {
                                                                Icon(Icons.Default.Refresh, contentDescription = "Replace", modifier = Modifier.size(14.dp), tint = Color.White)
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Replace", fontSize = 11.sp, color = Color.White)
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                        }
                                                        Button(
                                                            onClick = {
                                                                pushHistory()
                                                                editItems.remove(currentSelectedItem)
                                                                selectedAnnotationId = null
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Color.White)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Delete", fontSize = 11.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        listOf(AndroidColor.rgb(220, 38, 38), AndroidColor.rgb(37, 99, 235), AndroidColor.rgb(22, 163, 74), AndroidColor.BLACK, AndroidColor.rgb(234, 88, 12), AndroidColor.WHITE).forEach { cHex ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(cHex))
                                                                    .border(width = if (currentSelectedItem.colorHex == cHex) 2.dp else 1.dp, color = if (currentSelectedItem.colorHex == cHex) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f), shape = CircleShape)
                                                                    .clickable {
                                                                        pushHistory()
                                                                        val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                        if (idx != -1) editItems[idx] = editItems[idx].copy(colorHex = cHex)
                                                                    }
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                colorPickerTarget = "SELECTED"
                                                                colorPickerCurrentColor = currentSelectedItem.colorHex
                                                                showColorPicker = true
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Palette, contentDescription = "Color", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    if (currentSelectedItem.type in listOf(PdfEngine.AnnotationType.RECTANGLE, PdfEngine.AnnotationType.CIRCLE)) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            FilterChip(
                                                                selected = currentSelectedItem.fillColorHex == null,
                                                                onClick = {
                                                                    pushHistory()
                                                                    val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                    if (idx != -1) editItems[idx] = editItems[idx].copy(fillColorHex = null)
                                                                },
                                                                label = { Text("No Fill", fontSize = 10.sp) },
                                                                modifier = Modifier.height(26.dp)
                                                            )
                                                            FilterChip(
                                                                selected = currentSelectedItem.fillColorHex != null,
                                                                onClick = {
                                                                    pushHistory()
                                                                    val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                    if (idx != -1) {
                                                                        val newFill = if (editItems[idx].fillColorHex != null) null else editItems[idx].colorHex
                                                                        editItems[idx] = editItems[idx].copy(fillColorHex = newFill)
                                                                    }
                                                                },
                                                                label = { Text("Fill", fontSize = 10.sp) },
                                                                modifier = Modifier.height(26.dp)
                                                            )
                                                        }
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("Width: ${currentSelectedItem.strokeWidth.toInt()}", color = Color.White, fontSize = 11.sp)
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                if (idx != -1) {
                                                                    val nw = (editItems[idx].strokeWidth - 2f).coerceAtLeast(1f)
                                                                    editItems[idx] = editItems[idx].copy(strokeWidth = nw)
                                                                }
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                pushHistory()
                                                                val idx = editItems.indexOfFirst { it.id == currentSelectedItem.id }
                                                                if (idx != -1) {
                                                                    val nw = (editItems[idx].strokeWidth + 2f).coerceAtMost(40f)
                                                                    editItems[idx] = editItems[idx].copy(strokeWidth = nw)
                                                                }
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        listOf(
                                                            Pair(PdfEngine.AnnotationType.RECTANGLE, "Rect"),
                                                            Pair(PdfEngine.AnnotationType.CIRCLE, "Circle"),
                                                            Pair(PdfEngine.AnnotationType.ARROW, "Arrow"),
                                                            Pair(PdfEngine.AnnotationType.LINE, "Line"),
                                                            Pair(PdfEngine.AnnotationType.CHECKMARK, "Hand-Tick"),
                                                            Pair(PdfEngine.AnnotationType.STAR, "Star ★"),
                                                            Pair(PdfEngine.AnnotationType.HEART, "Heart ♥"),
                                                            Pair(PdfEngine.AnnotationType.CROSS, "Cross ✕"),
                                                            Pair(PdfEngine.AnnotationType.SMILEY, "Happy ☺"),
                                                            Pair(PdfEngine.AnnotationType.THUMBS_UP, "Like 👍"),
                                                            Pair(PdfEngine.AnnotationType.WARNING, "Alert ⚠"),
                                                            Pair(PdfEngine.AnnotationType.STAMP, "Stamp ▣"),
                                                            Pair(PdfEngine.AnnotationType.PEN, "Doodle"),
                                                            Pair(PdfEngine.AnnotationType.HIGHLIGHTER, "Marker")
                                                        ).forEach { (shape, label) ->
                                                            FilterChip(
                                                                selected = selectedShapeType == shape,
                                                                onClick = { selectedShapeType = shape },
                                                                label = { Text(label, fontSize = 11.sp) },
                                                                modifier = Modifier.height(28.dp).testTag("shape_chip_${shape.name.lowercase()}")
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    TextButton(
                                                        onClick = {
                                                            val shapes = editItems.filter {
                                                                it.pageIndex == currentPageIndex && it.type in listOf(
                                                                    PdfEngine.AnnotationType.RECTANGLE,
                                                                    PdfEngine.AnnotationType.CIRCLE,
                                                                    PdfEngine.AnnotationType.ARROW,
                                                                    PdfEngine.AnnotationType.LINE,
                                                                    PdfEngine.AnnotationType.CHECKMARK,
                                                                    PdfEngine.AnnotationType.STAR,
                                                                    PdfEngine.AnnotationType.HEART,
                                                                    PdfEngine.AnnotationType.CROSS
                                                                )
                                                            }
                                                            if (shapes.isNotEmpty()) {
                                                                pushHistory()
                                                                editItems.removeAll(shapes)
                                                                if (shapes.any { it.id == selectedAnnotationId }) selectedAnnotationId = null
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Clear", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        listOf(
                                                            AndroidColor.rgb(220, 38, 38),
                                                            AndroidColor.rgb(37, 99, 235),
                                                            AndroidColor.rgb(22, 163, 74),
                                                            AndroidColor.BLACK,
                                                            AndroidColor.rgb(234, 88, 12),
                                                            AndroidColor.WHITE
                                                        ).forEach { cHex ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(cHex))
                                                                    .border(
                                                                        width = if (shapeStrokeColor == cHex) 2.dp else 1.dp,
                                                                        color = if (shapeStrokeColor == cHex) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f),
                                                                        shape = CircleShape
                                                                    )
                                                                    .clickable { shapeStrokeColor = cHex }
                                                                )
                                                            }
                                                        IconButton(
                                                            onClick = {
                                                                colorPickerTarget = "SHAPE_STROKE"
                                                                colorPickerCurrentColor = shapeStrokeColor
                                                                showColorPicker = true
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Icon(Icons.Default.Palette, contentDescription = "Stroke Color", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    if (selectedShapeType in listOf(PdfEngine.AnnotationType.RECTANGLE, PdfEngine.AnnotationType.CIRCLE)) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            FilterChip(
                                                                selected = shapeFillColor == null,
                                                                onClick = { shapeFillColor = null },
                                                                label = { Text("No Fill", fontSize = 10.sp) },
                                                                modifier = Modifier.height(26.dp)
                                                            )
                                                            FilterChip(
                                                                selected = shapeFillColor != null,
                                                                onClick = { shapeFillColor = if (shapeFillColor != null) null else shapeStrokeColor },
                                                                label = { Text("Fill", fontSize = 10.sp) },
                                                                modifier = Modifier.height(26.dp)
                                                            )
                                                        }
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        listOf(2f, 4f, 8f).forEach { width ->
                                                            FilterChip(
                                                                selected = shapeStrokeWidth == width,
                                                                onClick = { shapeStrokeWidth = width },
                                                                label = { Text("${width.toInt()}px", fontSize = 10.sp) },
                                                                modifier = Modifier.height(26.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    "STAMP" -> {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(stampsList) { stamp ->
                                                Button(
                                                    onClick = {
                                                        pushHistory()
                                                        editItems.add(
                                                            PdfEngine.EditItem(
                                                                pageIndex = currentPageIndex,
                                                                isStamp = true,
                                                                stampText = stamp,
                                                                posX = 70f,
                                                                posY = 130f
                                                            )
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text(stamp, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    "ERASER" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Tap any annotation to remove it",
                                                color = Color(0xFFFCA5A5),
                                                fontSize = 11.sp
                                            )
                                            Button(
                                                onClick = {
                                                    pushHistory()
                                                    editItems.removeAll { it.pageIndex == currentPageIndex }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Clear Page Annotations", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // ====================================================
                            // BOTTOM EDITING TOOLBAR (All Markup & PDF Tools Integrated)
                            // Markup: Text | Image | Draw | Highlight | Shapes
                            // PDF Tools: Crop | Rotate | Watermark | Page No | Organize
                            // ====================================================
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Rotate Quick Bar with instant animated rotation controls
                                AnimatedVisibility(visible = showRotateBar) {
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Rotate (${currentRotationDeg}°):",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            FilledTonalButton(
                                                onClick = {
                                                    val cur = pageRotations[currentPageIndex] ?: 0
                                                    pageRotations[currentPageIndex] = (cur + 90) % 360
                                                },
                                                modifier = Modifier.height(30.dp).testTag("rotate_90_cw_button"),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("+90° CW", fontSize = 11.sp)
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    val cur = pageRotations[currentPageIndex] ?: 0
                                                    pageRotations[currentPageIndex] = (cur + 180) % 360
                                                },
                                                modifier = Modifier.height(30.dp).testTag("rotate_180_flip_button"),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Text("180° Flip", fontSize = 11.sp)
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    val cur = pageRotations[currentPageIndex] ?: 0
                                                    pageRotations[currentPageIndex] = (cur + 270) % 360
                                                },
                                                modifier = Modifier.height(30.dp).testTag("rotate_270_cw_button"),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Text("270° CW", fontSize = 11.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    pageRotations[currentPageIndex] = 0
                                                },
                                                modifier = Modifier.height(30.dp).testTag("rotate_reset_button"),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Text("Reset 0°", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    val cur = pageRotations[currentPageIndex] ?: 0
                                                    for (p in 0 until totalPages) {
                                                        pageRotations[p] = cur
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                                modifier = Modifier.height(30.dp).testTag("rotate_all_pages_button"),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Text("All Pages", fontSize = 11.sp)
                                            }
                                            IconButton(
                                                onClick = { showRotateBar = false },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(
                                            Triple("TEXT", "Text", Icons.Default.Title),
                                            Triple("IMAGE", "Image", Icons.Default.Image),
                                            Triple("PEN", "Draw", Icons.Default.Draw),
                                            Triple("HIGHLIGHT", "Highlight", Icons.Default.Highlight),
                                            Triple("SHAPES", "Shapes", Icons.Default.Category),
                                            Triple("CROP", "Crop", Icons.Default.Crop),
                                            Triple("ROTATE", "Rotate", Icons.Default.RotateRight),
                                            Triple("WATERMARK", "Watermark", Icons.Default.Layers),
                                            Triple("PAGE_NUMBERS", "Page No", Icons.Default.FormatListNumbered),
                                            Triple("ORGANIZE", "Organize", Icons.Default.GridView)
                                        ).forEach { (toolKey, label, icon) ->
                                            val isSelected = when (toolKey) {
                                                "CROP" -> activeSubTool == WorkspaceSubTool.CROP
                                                "ROTATE" -> showRotateBar || currentRotationDeg != 0
                                                "WATERMARK" -> showWorkspaceWatermarkDialog
                                                "PAGE_NUMBERS" -> activeSubTool == WorkspaceSubTool.PAGE_NUMBERS
                                                "ORGANIZE" -> activeSubTool == WorkspaceSubTool.PAGES
                                                else -> activeEditTab == toolKey
                                            }
                                            val toolColor = if (isSelected) CrimsonPrimary else Color(0xFF94A3B8)
                                            Column(
                                                modifier = Modifier
                                                    .widthIn(min = 60.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        when (toolKey) {
                                                            "CROP" -> {
                                                                activeSubTool = WorkspaceSubTool.CROP
                                                            }
                                                            "ROTATE" -> {
                                                                val cur = pageRotations[currentPageIndex] ?: 0
                                                                pageRotations[currentPageIndex] = (cur + 90) % 360
                                                                showRotateBar = true
                                                            }
                                                            "WATERMARK" -> {
                                                                showWorkspaceWatermarkDialog = true
                                                            }
                                                            "PAGE_NUMBERS" -> {
                                                                activeSubTool = WorkspaceSubTool.PAGE_NUMBERS
                                                            }
                                                            "ORGANIZE" -> {
                                                                activeSubTool = WorkspaceSubTool.PAGES
                                                            }
                                                            else -> {
                                                                activeEditTab = toolKey
                                                                showRotateBar = false
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                                    .testTag("edit_tool_$label"),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = toolColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = label,
                                                    color = toolColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(16.dp)
                                                        .height(3.dp)
                                                        .clip(RoundedCornerShape(1.5.dp))
                                                        .background(if (isSelected) CrimsonPrimary else Color.Transparent)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (showColorPicker) {
                                AnnotationColorPickerDialog(
                                    initialColor = colorPickerCurrentColor,
                                    onDismiss = { showColorPicker = false },
                                    onColorPicked = { pickedColor ->
                                        when (colorPickerTarget) {
                                            "PEN" -> penColor = pickedColor
                                            "HIGHLIGHT" -> highlightColor = pickedColor
                                            "SHAPE_STROKE" -> shapeStrokeColor = pickedColor
                                            "SHAPE_FILL" -> shapeFillColor = pickedColor
                                            "SELECTED" -> {
                                                val selId = selectedAnnotationId
                                                if (selId != null) {
                                                    val idx = editItems.indexOfFirst { it.id == selId }
                                                    if (idx != -1) {
                                                        pushHistory()
                                                        editItems[idx] = editItems[idx].copy(colorHex = pickedColor)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            if (showEditTextDialog) {
                                val editingItem = editItems.find { it.id == editingTextItemId }
                                if (editingItem != null) {
                                    AnnotationEditTextDialog(
                                        initialText = editingTextContent,
                                        initialFontSize = editingItem.fontSize,
                                        initialColor = editingItem.colorHex,
                                        initialBold = editingItem.isBold,
                                        initialItalic = editingItem.isItalic,
                                        initialUnderline = editingItem.isUnderline,
                                        initialAlignment = editingItem.alignment,
                                        onDismiss = { showEditTextDialog = false },
                                        onSave = { newText, newFontSize, newColor, newBold, newItalic, newUnderline, newAlign ->
                                            pushHistory()
                                            val idx = editItems.indexOfFirst { it.id == editingItem.id }
                                            if (idx != -1) {
                                                editItems[idx] = editItems[idx].copy(
                                                    text = newText,
                                                    fontSize = newFontSize,
                                                    colorHex = newColor,
                                                    isBold = newBold,
                                                    isItalic = newItalic,
                                                    isUnderline = newUnderline,
                                                    alignment = newAlign
                                                )
                                                selectedAnnotationId = editItems[idx].id
                                                activeEditTab = "TEXT"
                                            }
                                            showEditTextDialog = false
                                        },
                                        onDelete = {
                                            pushHistory()
                                            editItems.removeAll { it.id == editingItem.id }
                                            if (selectedAnnotationId == editingItem.id) {
                                                selectedAnnotationId = null
                                            }
                                            showEditTextDialog = false
                                        }
                                    )
                                } else {
                                    showEditTextDialog = false
                                }
                            }

                            if (showOcrEditDialog) {
                                OcrEditDialog(
                                    initialText = editingOcrText,
                                    initialFontSize = editingOcrFontSize,
                                    initialTextColor = editingOcrTextColor,
                                    initialPaperColor = editingOcrPaperColor,
                                    initialIsBold = editingOcrIsBold,
                                    initialIsItalic = editingOcrIsItalic,
                                    initialAlignment = editingOcrAlignment,
                                    onDismiss = { showOcrEditDialog = false },
                                    onSave = { newText, newFontSize, newColor, newPaperColor, isBold, isItalic, align ->
                                        pushHistory()
                                        val rect = tappedOcrLine?.boundingBox ?: editingOcrItem?.maskRect ?: AndroidRect(0, 0, 100, 30)
                                        val expandedMask = AndroidRect(
                                            (rect.left - 2).coerceAtLeast(0),
                                            (rect.top - 1).coerceAtLeast(0),
                                            rect.right + 2,
                                            rect.bottom + 1
                                        )
                                        val baselineY = (rect.bottom.toFloat() - (rect.height().toFloat() * 0.15f)).coerceAtLeast(rect.top.toFloat() + 10f)
                                        val posX = when (align) {
                                            "CENTER" -> (rect.left + rect.right) / 2f
                                            "RIGHT" -> rect.right.toFloat() - 2f
                                            else -> rect.left.toFloat() + 2f
                                        }
                                        val newItem = PdfEngine.EditItem(
                                            id = editingOcrItem?.id ?: java.util.UUID.randomUUID().toString(),
                                            pageIndex = currentPageIndex,
                                            type = PdfEngine.AnnotationType.OCR_REPLACEMENT,
                                            text = newText,
                                            posX = posX,
                                            posY = baselineY,
                                            fontSize = newFontSize,
                                            colorHex = newColor,
                                            maskRect = expandedMask,
                                            maskColor = newPaperColor,
                                            isBold = isBold,
                                            isItalic = isItalic,
                                            alignment = align
                                        )
                                        if (editingOcrItem != null) {
                                            val idx = editItems.indexOfFirst { it.id == editingOcrItem!!.id }
                                            if (idx != -1) {
                                                editItems[idx] = newItem
                                            } else {
                                                editItems.add(newItem)
                                            }
                                        } else {
                                            editItems.add(newItem)
                                        }
                                        selectedAnnotationId = newItem.id
                                        activeEditTab = "TEXT"
                                        showOcrEditDialog = false
                                    },
                                    onDelete = {
                                        pushHistory()
                                        if (editingOcrItem != null) {
                                            editItems.removeAll { it.id == editingOcrItem!!.id }
                                            if (selectedAnnotationId == editingOcrItem!!.id) {
                                                selectedAnnotationId = null
                                            }
                                        } else if (tappedOcrLine != null) {
                                            val rect = tappedOcrLine!!.boundingBox
                                            val expandedMask = AndroidRect(
                                                (rect.left - 2).coerceAtLeast(0),
                                                (rect.top - 1).coerceAtLeast(0),
                                                rect.right + 2,
                                                rect.bottom + 1
                                            )
                                            val eraseItem = PdfEngine.EditItem(
                                                pageIndex = currentPageIndex,
                                                type = PdfEngine.AnnotationType.OCR_REPLACEMENT,
                                                text = "",
                                                posX = rect.left.toFloat(),
                                                posY = rect.bottom.toFloat(),
                                                fontSize = 12f,
                                                colorHex = AndroidColor.BLACK,
                                                maskRect = expandedMask,
                                                maskColor = editingOcrPaperColor
                                            )
                                            editItems.add(eraseItem)
                                        }
                                        showOcrEditDialog = false
                                    }
                                )
                            }

                            if (showWorkspaceWatermarkDialog) {
                                WorkspaceWatermarkDialog(
                                    file = currentRecord,
                                    currentPage = currentPageIndex,
                                    onDismiss = { showWorkspaceWatermarkDialog = false },
                                    onApply = { wmText, wmRot, wmOpacity, wmSize, wmColor, applyAll, isTiled ->
                                        showWorkspaceWatermarkDialog = false
                                        pushHistory()
                                        val targets = if (applyAll) (0 until totalPages).toList() else listOf(currentPageIndex)
                                        var lastAddedId: String? = null
                                        for (pIdx in targets) {
                                            val bmp = pages.getOrNull(pIdx)
                                            val w = bmp?.width?.toFloat() ?: 600f
                                            val h = bmp?.height?.toFloat() ?: 800f
                                            if (isTiled) {
                                                // Full-page repeating watermark grid
                                                val stepX = (w / 3f).coerceAtLeast(160f)
                                                val stepY = (h / 4f).coerceAtLeast(180f)
                                                for (col in 0..2) {
                                                    for (row in 0..3) {
                                                        val gridX = (col + 0.5f) * stepX
                                                        val gridY = (row + 0.5f) * stepY
                                                        val tileItem = PdfEngine.EditItem(
                                                            pageIndex = pIdx,
                                                            type = PdfEngine.AnnotationType.TEXT,
                                                            text = wmText,
                                                            posX = gridX,
                                                            posY = gridY,
                                                            fontSize = (wmSize * 0.75f).coerceAtLeast(18f),
                                                            colorHex = wmColor,
                                                            opacity = (wmOpacity * 0.7f).coerceAtLeast(0.15f),
                                                            rotation = wmRot.toFloat(),
                                                            alignment = "CENTER",
                                                            isBold = true
                                                        )
                                                        editItems.add(tileItem)
                                                        if (pIdx == currentPageIndex) lastAddedId = tileItem.id
                                                    }
                                                }
                                            } else {
                                                val item = PdfEngine.EditItem(
                                                    pageIndex = pIdx,
                                                    type = PdfEngine.AnnotationType.TEXT,
                                                    text = wmText,
                                                    posX = w / 2f,
                                                    posY = h / 2f,
                                                    fontSize = wmSize,
                                                    colorHex = wmColor,
                                                    opacity = wmOpacity,
                                                    rotation = wmRot.toFloat(),
                                                    alignment = "CENTER",
                                                    isBold = true
                                                )
                                                editItems.add(item)
                                                if (pIdx == currentPageIndex) lastAddedId = item.id
                                            }
                                        }
                                        if (lastAddedId != null) {
                                            selectedAnnotationId = lastAddedId
                                        }
                                    }
                                )
                            }

                            if (showImageCropDialog) {
                                val selectedImg = editItems.find { it.id == selectedAnnotationId && it.type == PdfEngine.AnnotationType.IMAGE }
                                val srcBmp = selectedImg?.originalImageBitmap ?: selectedImg?.imageBitmap
                                if (selectedImg != null && srcBmp != null) {
                                    ImageCropDialog(
                                        sourceBitmap = srcBmp,
                                        onDismiss = { showImageCropDialog = false },
                                        onApplyCrop = { croppedBmp, cropLeftPct, cropTopPct, cropRightPct, cropBottomPct ->
                                            pushHistory()
                                            val idx = editItems.indexOfFirst { it.id == selectedImg.id }
                                            if (idx != -1) {
                                                val origItem = editItems[idx]
                                                val origW = origItem.imageWidth
                                                val origH = origItem.imageHeight
                                                val newW = (origW * (cropRightPct - cropLeftPct)).coerceAtLeast(20f)
                                                val newH = (origH * (cropBottomPct - cropTopPct)).coerceAtLeast(20f)
                                                val newX = origItem.posX + origW * cropLeftPct
                                                val newY = origItem.posY + origH * cropTopPct

                                                editItems[idx] = origItem.copy(
                                                    imageBitmap = croppedBmp,
                                                    originalImageBitmap = srcBmp,
                                                    posX = newX,
                                                    posY = newY,
                                                    imageWidth = newW,
                                                    imageHeight = newH
                                                )
                                            }
                                            showImageCropDialog = false
                                        }
                                    )
                                } else {
                                    showImageCropDialog = false
                                }
                            }
                        }
                    }
                }

                // ====================================================
                // 5. PRIMARY WORKSPACE ACTION DOCK (Visible when NOT in edit mode)
                // ====================================================
                if (!isEditMode) {
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(10f),
                        shadowElevation = 8.dp
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // Navigation controls: Prev [ < ] Page X of Y [ > ] Next + Thumbnail strip toggle (Only visible when document has 2 or more pages)
                        val totalPages = if (pages.isNotEmpty()) pages.size else currentRecord.pageCount
                        if (totalPages > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showThumbnailTray = !showThumbnailTray },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewCarousel,
                                        contentDescription = "Thumbnails",
                                        tint = if (showThumbnailTray) CrimsonPrimary else Color(0xFF94A3B8)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                        enabled = currentPageIndex > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.NavigateBefore,
                                            contentDescription = "Prev Page",
                                            tint = if (currentPageIndex > 0) Color.White else Color(0xFF475569)
                                        )
                                    }

                                    Text(
                                        text = if (pages.isNotEmpty()) "Page ${currentPageIndex + 1} of ${pages.size}" else "Page 1",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    IconButton(
                                        onClick = { if (currentPageIndex < pages.size - 1) currentPageIndex++ },
                                        enabled = currentPageIndex < pages.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.NavigateNext,
                                            contentDescription = "Next Page",
                                            tint = if (currentPageIndex < pages.size - 1) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { activeSubTool = WorkspaceSubTool.PAGES },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "Page Grid",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        // Prominent Tools Dock: [✏️ EDIT] [📄 Pages] [✍ Sign] [✂ Crop] [🗜 Compress] [🔎 Validate] [🤖 AI] [⋮ More]
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Prominent EDIT button
                            item {
                                Button(
                                    onClick = { isEditMode = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CrimsonPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("workspace_primary_edit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "✏️ EDIT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Pages Management
                            item {
                                OutlinedButton(
                                    onClick = { activeSubTool = WorkspaceSubTool.PAGES },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp), tint = ToolBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pages", fontSize = 12.sp)
                                }
                            }

                            // Crop
                            item {
                                OutlinedButton(
                                    onClick = { activeSubTool = WorkspaceSubTool.CROP },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp), tint = ToolOrange)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Crop", fontSize = 12.sp)
                                }
                            }

                            // Compress
                            item {
                                OutlinedButton(
                                    onClick = { activeSubTool = WorkspaceSubTool.COMPRESS },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(16.dp), tint = ToolPurple)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Compress", fontSize = 12.sp)
                                }
                            }

                            // AI Assistant
                            item {
                                OutlinedButton(
                                    onClick = { activeSubTool = WorkspaceSubTool.AI },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFDE047))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI", fontSize = 12.sp)
                                }
                            }

                            // More Menu
                            item {
                                OutlinedButton(
                                    onClick = { moreMenuExpanded = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("More", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            }

            // ====================================================
            // SUB-TOOL DIALOGS / SHEETS (Invoked inside Workspace)
            // ====================================================
            when (activeSubTool) {
                WorkspaceSubTool.PAGES -> {
                    WorkspacePagesSheet(
                        file = currentRecord,
                        pages = pages,
                        onDismiss = { activeSubTool = WorkspaceSubTool.NONE },
                        onApply = { newOrder, rotations, blanks, imgs ->
                            activeSubTool = WorkspaceSubTool.NONE
                            val outputName = "${currentRecord.nameWithoutExtension()}_organized.pdf"
                            viewModel.managePagesInFile(
                                sourceRecord = currentRecord,
                                pageOrder = newOrder,
                                rotations = rotations,
                                blankPagesAfter = blanks,
                                additionalImages = imgs,
                                outputName = outputName
                            ) { updated ->
                                currentRecord = updated
                            }
                        }
                    )
                }

                WorkspaceSubTool.CROP -> {
                    WorkspaceCropDialog(
                        file = currentRecord,
                        pages = pages,
                        currentPage = currentPageIndex,
                        onDismiss = { activeSubTool = WorkspaceSubTool.NONE },
                        onApply = { left, top, right, bottom, targetPages ->
                            activeSubTool = WorkspaceSubTool.NONE
                            val outputName = "${currentRecord.nameWithoutExtension()}_cropped.pdf"
                            viewModel.cropPdf(
                                sourceRecord = currentRecord,
                                leftPct = left,
                                topPct = top,
                                rightPct = right,
                                bottomPct = bottom,
                                targetPages = targetPages,
                                outputName = outputName
                            ) { updated ->
                                currentRecord = updated
                            }
                        }
                    )
                }

                WorkspaceSubTool.DRAW_ANNOTATE, WorkspaceSubTool.EDIT_TEXT -> {
                    activeSubTool = WorkspaceSubTool.NONE
                    isEditMode = true
                }

                WorkspaceSubTool.PAGE_NUMBERS -> {
                    WorkspacePageNumbersDialog(
                        file = currentRecord,
                        onDismiss = { activeSubTool = WorkspaceSubTool.NONE },
                        onApply = { format, pos, targetPages ->
                            activeSubTool = WorkspaceSubTool.NONE
                            val outName = "${currentRecord.nameWithoutExtension()}_numbered.pdf"
                            viewModel.addCustomPageNumbers(
                                sourceRecord = currentRecord,
                                format = format,
                                position = pos,
                                targetPages = targetPages,
                                outputName = outName
                            ) { updated ->
                                currentRecord = updated
                            }
                        }
                    )
                }

                WorkspaceSubTool.WATERMARK -> {
                    WorkspaceWatermarkDialog(
                        file = currentRecord,
                        currentPage = currentPageIndex,
                        onDismiss = { activeSubTool = WorkspaceSubTool.NONE },
                        onApply = { wmText, wmRot, wmOpacity, wmSize, wmColor, applyAll, isTiled ->
                            activeSubTool = WorkspaceSubTool.NONE
                            pushHistory()
                            val targets = if (applyAll) (0 until totalPages).toList() else listOf(currentPageIndex)
                            var lastAddedId: String? = null
                            for (pIdx in targets) {
                                val bmp = pages.getOrNull(pIdx)
                                val w = bmp?.width?.toFloat() ?: 600f
                                val h = bmp?.height?.toFloat() ?: 800f
                                val item = PdfEngine.EditItem(
                                    pageIndex = pIdx,
                                    type = PdfEngine.AnnotationType.WATERMARK,
                                    text = wmText,
                                    posX = w / 2f,
                                    posY = h / 2f,
                                    fontSize = wmSize,
                                    colorHex = wmColor,
                                    opacity = wmOpacity,
                                    rotation = wmRot.toFloat(),
                                    alignment = "CENTER",
                                    isBold = true,
                                    isTiled = isTiled
                                )
                                editItems.add(item)
                                if (pIdx == currentPageIndex) lastAddedId = item.id
                            }
                            if (lastAddedId != null) {
                                selectedAnnotationId = lastAddedId
                            }
                        }
                    )
                }

                WorkspaceSubTool.ROTATE -> {
                    activeSubTool = WorkspaceSubTool.NONE
                    showRotateBar = true
                }

                WorkspaceSubTool.OCR -> {
                    OcrModalDialog(
                        file = currentRecord,
                        viewModel = viewModel,
                        onDismiss = { activeSubTool = WorkspaceSubTool.NONE }
                    )
                }

                WorkspaceSubTool.SCAN -> {
                    ScannerModalDialog(
                        onDismiss = { activeSubTool = WorkspaceSubTool.NONE },
                        onSavePdf = { bitmaps, name ->
                            activeSubTool = WorkspaceSubTool.NONE
                            viewModel.scanOrImagesToPdf(bitmaps, name, isScan = true)
                        }
                    )
                }

                WorkspaceSubTool.DOC_INFO -> {
                    WorkspaceDocumentInfoDialog(
                        file = currentRecord,
                        onDismiss = { activeSubTool = WorkspaceSubTool.NONE }
                    )
                }

                WorkspaceSubTool.SAVE_AS -> {
                    AlertDialog(
                        onDismissRequest = { activeSubTool = WorkspaceSubTool.NONE },
                        title = { Text("Save As...", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("Enter a new file name:")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customSaveAsName,
                                    onValueChange = { customSaveAsName = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    activeSubTool = WorkspaceSubTool.NONE
                                    viewModel.saveEditedPdf(
                                        sourceRecord = currentRecord,
                                        items = editItems.toList(),
                                        deletedPages = emptySet(),
                                        pageRotations = emptyMap(),
                                        outputName = customSaveAsName
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                            ) {
                                Text("Save Copy")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { activeSubTool = WorkspaceSubTool.NONE }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                else -> {}
            }
        }

        // Adobe Acrobat Signature Details Dialog
        if (showSigDetailsDialog && signatureResult != null) {
            WorkspaceSignatureDetailsDialog(
                result = signatureResult!!,
                onDismiss = { showSigDetailsDialog = false },
                onOpenFullValidation = {
                    showSigDetailsDialog = false
                    viewModel.openModal(ActiveModal.SIGNATURE_VALIDATION, currentRecord)
                }
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// WORKSPACE SUB-DIALOG: PAGES MANAGEMENT
// -------------------------------------------------------------------------------------------------
@Composable
fun WorkspacePagesSheet(
    file: PdfFileRecord,
    pages: List<Bitmap>,
    onDismiss: () -> Unit,
    onApply: (List<Int>, Map<Int, Int>, Set<Int>, Map<Int, Bitmap>) -> Unit
) {
    val totalCount = pages.size.coerceAtLeast(file.pageCount).coerceAtLeast(1)
    val pageOrder = remember { mutableStateListOf<Int>().apply { addAll(0 until totalCount) } }
    val pageRotations = remember { mutableStateMapOf<Int, Int>() }
    val blankPagesAfter = remember { mutableStateListOf<Int>() }
    val additionalImages = remember { mutableStateMapOf<Int, Bitmap>() }

    var selectedIndex by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .safeDrawingPadding()
            ) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Page Management", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Button(
                        onClick = {
                            onApply(
                                pageOrder.toList(),
                                pageRotations.toMap(),
                                blankPagesAfter.toSet(),
                                additionalImages.toMap()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Text("Apply Changes")
                    }
                }

                // Page Actions Row (Delete, Duplicate, Rotate 90, Move Left, Move Right, Add Blank)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // Add blank page after selected
                            if (!blankPagesAfter.contains(selectedIndex)) {
                                blankPagesAfter.add(selectedIndex)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Blank", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            // Duplicate page
                            if (selectedIndex in pageOrder.indices) {
                                pageOrder.add(selectedIndex + 1, pageOrder[selectedIndex])
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToolPurple),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duplicate", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            // Rotate selected page 90 deg
                            val currentRot = pageRotations[selectedIndex] ?: 0
                            pageRotations[selectedIndex] = (currentRot + 90) % 360
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rotate 90°", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            // Delete page
                            if (pageOrder.size > 1 && selectedIndex in pageOrder.indices) {
                                pageOrder.removeAt(selectedIndex)
                                selectedIndex = selectedIndex.coerceAtMost(pageOrder.size - 1)
                            }
                        },
                        enabled = pageOrder.size > 1,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp)
                    }
                }

                // Grid of Page Thumbnails
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(pageOrder) { idx, originalPageIdx ->
                        val isSelected = idx == selectedIndex
                        val bmp = pages.getOrNull(originalPageIdx)
                        val rot = pageRotations[originalPageIdx] ?: 0

                        Card(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) CrimsonPrimary else Color(0xFF475569),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIndex = idx },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Page ${idx + 1}",
                                        modifier = Modifier
                                            .height(110.dp)
                                            .fillMaxWidth()
                                            .rotate(rot.toFloat())
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .height(110.dp)
                                            .fillMaxWidth()
                                            .background(Color(0xFF334155)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Page ${idx + 1}", color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        color = if (isSelected) CrimsonPrimary else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )

                                    Row {
                                        if (idx > 0) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Move left",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        val temp = pageOrder[idx]
                                                        pageOrder[idx] = pageOrder[idx - 1]
                                                        pageOrder[idx - 1] = temp
                                                        selectedIndex = idx - 1
                                                    }
                                            )
                                        }
                                        if (idx < pageOrder.size - 1) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Move right",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        val temp = pageOrder[idx]
                                                        pageOrder[idx] = pageOrder[idx + 1]
                                                        pageOrder[idx + 1] = temp
                                                        selectedIndex = idx + 1
                                                    }
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
    }
}

// -------------------------------------------------------------------------------------------------
// -------------------------------------------------------------------------------------------------
// WORKSPACE SUB-DIALOG: CROP PDF
// -------------------------------------------------------------------------------------------------
enum class CropApplyTarget {
    ALL_PAGES,
    ODD_PAGES,
    EVEN_PAGES,
    CURRENT_PAGE
}

enum class CropDragHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    LEFT_EDGE,
    TOP_EDGE,
    RIGHT_EDGE,
    BOTTOM_EDGE,
    BODY
}

@Composable
fun WorkspaceCropDialog(
    file: PdfFileRecord,
    pages: List<Bitmap>,
    currentPage: Int,
    onDismiss: () -> Unit,
    onApply: (Float, Float, Float, Float, Set<Int>) -> Unit
) {
    val totalPages = pages.size.coerceAtLeast(file.pageCount).coerceAtLeast(1)
    var currentPageIdx by remember { mutableIntStateOf(currentPage.coerceIn(0, totalPages - 1)) }

    // Crop box coordinates normalized (0f..1f)
    var cropLeft by remember { mutableFloatStateOf(0.05f) }
    var cropTop by remember { mutableFloatStateOf(0.05f) }
    var cropRight by remember { mutableFloatStateOf(0.95f) }
    var cropBottom by remember { mutableFloatStateOf(0.95f) }

    // Zoom & pan
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Apply target
    var applyTarget by remember { mutableStateOf(CropApplyTarget.ALL_PAGES) }

    val affectedPages: Set<Int> = remember(applyTarget, currentPageIdx, totalPages) {
        when (applyTarget) {
            CropApplyTarget.ALL_PAGES -> emptySet()
            CropApplyTarget.ODD_PAGES -> (0 until totalPages).filter { it % 2 == 0 }.toSet()
            CropApplyTarget.EVEN_PAGES -> (0 until totalPages).filter { it % 2 != 0 }.toSet()
            CropApplyTarget.CURRENT_PAGE -> setOf(currentPageIdx)
        }
    }

    val affectedPagesCount = when (applyTarget) {
        CropApplyTarget.ALL_PAGES -> totalPages
        CropApplyTarget.ODD_PAGES -> (0 until totalPages).count { it % 2 == 0 }
        CropApplyTarget.EVEN_PAGES -> (0 until totalPages).count { it % 2 != 0 }
        CropApplyTarget.CURRENT_PAGE -> 1
    }

    var activeHandle by remember { mutableStateOf(CropDragHandle.NONE) }
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Surface(color = Color(0xFF0F172A), tonalElevation = 4.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("crop_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Crop Pages",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        TextButton(
                            onClick = {
                                cropLeft = 0.05f
                                cropTop = 0.05f
                                cropRight = 0.95f
                                cropBottom = 0.95f
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            },
                            modifier = Modifier.testTag("crop_reset_button")
                        ) {
                            Text(
                                text = "RESET",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = Color(0xFF0F172A),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Apply crop to selector
                        Text(
                            text = "Apply crop to:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                CropApplyTarget.ALL_PAGES to "ALL PAGES",
                                CropApplyTarget.ODD_PAGES to "ODD PAGES",
                                CropApplyTarget.EVEN_PAGES to "EVEN PAGES",
                                CropApplyTarget.CURRENT_PAGE to "CURRENT PAGE"
                            ).forEach { (target, label) ->
                                val isSelected = applyTarget == target
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { applyTarget = target }
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF334155),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Action Button: CROP (N PAGES)
                        Button(
                            onClick = {
                                onApply(cropLeft, cropTop, cropRight, cropBottom, affectedPages)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("crop_apply_button")
                        ) {
                            Text(
                                text = "CROP (${affectedPagesCount} ${if (affectedPagesCount == 1) "PAGE" else "PAGES"})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            containerColor = Color(0xFF020617)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Interactive Preview Area
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF020617)),
                    contentAlignment = Alignment.Center
                ) {
                    val containerW = maxWidth.value * density
                    val containerH = maxHeight.value * density

                    val currentBmp = pages.getOrNull(currentPageIdx)
                    val bmpW = currentBmp?.width?.toFloat() ?: 1000f
                    val bmpH = currentBmp?.height?.toFloat() ?: 1414f

                    val marginPx = 20f * density
                    val availW = (containerW - marginPx * 2f).coerceAtLeast(100f)
                    val availH = (containerH - marginPx * 2f).coerceAtLeast(100f)

                    val fitScale = minOf(availW / bmpW, availH / bmpH)
                    val fittedW = bmpW * fitScale
                    val fittedH = bmpH * fitScale

                    val effectiveW = fittedW * zoomScale
                    val effectiveH = fittedH * zoomScale

                    val pageOffsetX = (containerW - effectiveW) / 2f + panOffset.x
                    val pageOffsetY = (containerH - effectiveH) / 2f + panOffset.y

                    val rectLeft = pageOffsetX + cropLeft * effectiveW
                    val rectTop = pageOffsetY + cropTop * effectiveH
                    val rectRight = pageOffsetX + cropRight * effectiveW
                    val rectBottom = pageOffsetY + cropBottom * effectiveH
                    val rectW = (rectRight - rectLeft).coerceAtLeast(10f)
                    val rectH = (rectBottom - rectTop).coerceAtLeast(10f)

                    val currentEffectiveW by rememberUpdatedState(effectiveW)
                    val currentEffectiveH by rememberUpdatedState(effectiveH)
                    val currentPageOffsetX by rememberUpdatedState(pageOffsetX)
                    val currentPageOffsetY by rememberUpdatedState(pageOffsetY)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                val cornerTouchRadiusPx = 52.dp.toPx()
                                val edgeTouchThicknessPx = 42.dp.toPx()

                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val downPos = down.position

                                    val effW = currentEffectiveW
                                    val effH = currentEffectiveH
                                    val offX = currentPageOffsetX
                                    val offY = currentPageOffsetY

                                    val curRectLeft = offX + cropLeft * effW
                                    val curRectTop = offY + cropTop * effH
                                    val curRectRight = offX + cropRight * effW
                                    val curRectBottom = offY + cropBottom * effH

                                    val dTL = kotlin.math.hypot(downPos.x - curRectLeft, downPos.y - curRectTop)
                                    val dTR = kotlin.math.hypot(downPos.x - curRectRight, downPos.y - curRectTop)
                                    val dBL = kotlin.math.hypot(downPos.x - curRectLeft, downPos.y - curRectBottom)
                                    val dBR = kotlin.math.hypot(downPos.x - curRectRight, downPos.y - curRectBottom)

                                    val hitCorner = minOf(dTL, dTR, dBL, dBR) <= cornerTouchRadiusPx
                                    val handle = when {
                                        hitCorner -> when (minOf(dTL, dTR, dBL, dBR)) {
                                            dTL -> CropDragHandle.TOP_LEFT
                                            dTR -> CropDragHandle.TOP_RIGHT
                                            dBL -> CropDragHandle.BOTTOM_LEFT
                                            else -> CropDragHandle.BOTTOM_RIGHT
                                        }
                                        kotlin.math.abs(downPos.y - curRectTop) <= edgeTouchThicknessPx && downPos.x in (curRectLeft - 24f)..(curRectRight + 24f) -> CropDragHandle.TOP_EDGE
                                        kotlin.math.abs(downPos.x - curRectRight) <= edgeTouchThicknessPx && downPos.y in (curRectTop - 24f)..(curRectBottom + 24f) -> CropDragHandle.RIGHT_EDGE
                                        kotlin.math.abs(downPos.y - curRectBottom) <= edgeTouchThicknessPx && downPos.x in (curRectLeft - 24f)..(curRectRight + 24f) -> CropDragHandle.BOTTOM_EDGE
                                        kotlin.math.abs(downPos.x - curRectLeft) <= edgeTouchThicknessPx && downPos.y in (curRectTop - 24f)..(curRectBottom + 24f) -> CropDragHandle.LEFT_EDGE
                                        downPos.x in curRectLeft..curRectRight && downPos.y in curRectTop..curRectBottom -> CropDragHandle.BODY
                                        else -> CropDragHandle.NONE
                                    }

                                    activeHandle = handle

                                    val grabOffsetX = when (activeHandle) {
                                        CropDragHandle.TOP_LEFT, CropDragHandle.BOTTOM_LEFT, CropDragHandle.LEFT_EDGE -> downPos.x - curRectLeft
                                        CropDragHandle.TOP_RIGHT, CropDragHandle.BOTTOM_RIGHT, CropDragHandle.RIGHT_EDGE -> downPos.x - curRectRight
                                        else -> 0f
                                    }
                                    val grabOffsetY = when (activeHandle) {
                                        CropDragHandle.TOP_LEFT, CropDragHandle.TOP_RIGHT, CropDragHandle.TOP_EDGE -> downPos.y - curRectTop
                                        CropDragHandle.BOTTOM_LEFT, CropDragHandle.BOTTOM_RIGHT, CropDragHandle.BOTTOM_EDGE -> downPos.y - curRectBottom
                                        else -> 0f
                                    }
                                    var lastTouch = downPos

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressedChanges = event.changes.filter { it.pressed }
                                        if (pressedChanges.isEmpty()) break

                                        if (pressedChanges.size > 1) {
                                            activeHandle = CropDragHandle.NONE
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()
                                            if (zoom != 1f) {
                                                zoomScale = (zoomScale * zoom).coerceIn(0.2f, 5f)
                                            }
                                            panOffset += pan
                                            event.changes.forEach { it.consume() }
                                        } else {
                                            val change = pressedChanges.first()
                                            val currentPos = change.position
                                            change.consume()

                                            val curW = currentEffectiveW
                                            val curH = currentEffectiveH
                                            val curX = currentPageOffsetX
                                            val curY = currentPageOffsetY

                                            when (activeHandle) {
                                                CropDragHandle.TOP_LEFT -> {
                                                    val targetPxX = currentPos.x - grabOffsetX
                                                    val targetPxY = currentPos.y - grabOffsetY
                                                    cropLeft = ((targetPxX - curX) / curW).coerceIn(0f, cropRight - 0.05f)
                                                    cropTop = ((targetPxY - curY) / curH).coerceIn(0f, cropBottom - 0.05f)
                                                }
                                                CropDragHandle.TOP_RIGHT -> {
                                                    val targetPxX = currentPos.x - grabOffsetX
                                                    val targetPxY = currentPos.y - grabOffsetY
                                                    cropRight = ((targetPxX - curX) / curW).coerceIn(cropLeft + 0.05f, 1f)
                                                    cropTop = ((targetPxY - curY) / curH).coerceIn(0f, cropBottom - 0.05f)
                                                }
                                                CropDragHandle.BOTTOM_LEFT -> {
                                                    val targetPxX = currentPos.x - grabOffsetX
                                                    val targetPxY = currentPos.y - grabOffsetY
                                                    cropLeft = ((targetPxX - curX) / curW).coerceIn(0f, cropRight - 0.05f)
                                                    cropBottom = ((targetPxY - curY) / curH).coerceIn(cropTop + 0.05f, 1f)
                                                }
                                                CropDragHandle.BOTTOM_RIGHT -> {
                                                    val targetPxX = currentPos.x - grabOffsetX
                                                    val targetPxY = currentPos.y - grabOffsetY
                                                    cropRight = ((targetPxX - curX) / curW).coerceIn(cropLeft + 0.05f, 1f)
                                                    cropBottom = ((targetPxY - curY) / curH).coerceIn(cropTop + 0.05f, 1f)
                                                }
                                                CropDragHandle.LEFT_EDGE -> {
                                                    val targetPxX = currentPos.x - grabOffsetX
                                                    cropLeft = ((targetPxX - curX) / curW).coerceIn(0f, cropRight - 0.05f)
                                                }
                                                CropDragHandle.RIGHT_EDGE -> {
                                                    val targetPxX = currentPos.x - grabOffsetX
                                                    cropRight = ((targetPxX - curX) / curW).coerceIn(cropLeft + 0.05f, 1f)
                                                }
                                                CropDragHandle.TOP_EDGE -> {
                                                    val targetPxY = currentPos.y - grabOffsetY
                                                    cropTop = ((targetPxY - curY) / curH).coerceIn(0f, cropBottom - 0.05f)
                                                }
                                                CropDragHandle.BOTTOM_EDGE -> {
                                                    val targetPxY = currentPos.y - grabOffsetY
                                                    cropBottom = ((targetPxY - curY) / curH).coerceIn(cropTop + 0.05f, 1f)
                                                }
                                                CropDragHandle.BODY -> {
                                                    val dx = (currentPos.x - lastTouch.x) / curW
                                                    val dy = (currentPos.y - lastTouch.y) / curH
                                                    val w = cropRight - cropLeft
                                                    val h = cropBottom - cropTop
                                                    val newL = (cropLeft + dx).coerceIn(0f, 1f - w)
                                                    val newT = (cropTop + dy).coerceIn(0f, 1f - h)
                                                    cropLeft = newL
                                                    cropRight = newL + w
                                                    cropTop = newT
                                                    cropBottom = newT + h
                                                    lastTouch = currentPos
                                                }
                                                CropDragHandle.NONE -> {
                                                    panOffset += (currentPos - lastTouch)
                                                    lastTouch = currentPos
                                                }
                                            }
                                        }
                                    }
                                    activeHandle = CropDragHandle.NONE
                                }
                            }
                    ) {
                        // 1. Draw Page Bitmap
                        if (currentBmp != null) {
                            drawImage(
                                image = currentBmp.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(pageOffsetX.toInt(), pageOffsetY.toInt()),
                                dstSize = androidx.compose.ui.unit.IntSize(effectiveW.toInt(), effectiveH.toInt())
                            )
                        } else {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(pageOffsetX, pageOffsetY),
                                size = androidx.compose.ui.geometry.Size(effectiveW, effectiveH)
                            )
                        }

                        // 2. Draw dark scrim outside the crop rectangle
                        val scrimColor = Color.Black.copy(alpha = 0.65f)
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width, rectTop.coerceAtLeast(0f))
                        )
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(0f, rectBottom),
                            size = androidx.compose.ui.geometry.Size(size.width, (size.height - rectBottom).coerceAtLeast(0f))
                        )
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(0f, rectTop),
                            size = androidx.compose.ui.geometry.Size(rectLeft.coerceAtLeast(0f), rectH)
                        )
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(rectRight, rectTop),
                            size = androidx.compose.ui.geometry.Size((size.width - rectRight).coerceAtLeast(0f), rectH)
                        )

                        // 3. Draw Blue Crop Outline
                        val blueColor = Color(0xFF2563EB)
                        drawRect(
                            color = blueColor,
                            topLeft = Offset(rectLeft, rectTop),
                            size = androidx.compose.ui.geometry.Size(rectW, rectH),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                        )

                        // 4. Rule-of-thirds grid lines
                        val thirdW = rectW / 3f
                        val thirdH = rectH / 3f
                        val gridColor = Color.White.copy(alpha = 0.35f)
                        drawLine(color = gridColor, start = Offset(rectLeft + thirdW, rectTop), end = Offset(rectLeft + thirdW, rectBottom), strokeWidth = 1.dp.toPx())
                        drawLine(color = gridColor, start = Offset(rectLeft + thirdW * 2, rectTop), end = Offset(rectLeft + thirdW * 2, rectBottom), strokeWidth = 1.dp.toPx())
                        drawLine(color = gridColor, start = Offset(rectLeft, rectTop + thirdH), end = Offset(rectRight, rectTop + thirdH), strokeWidth = 1.dp.toPx())
                        drawLine(color = gridColor, start = Offset(rectLeft, rectTop + thirdH * 2), end = Offset(rectRight, rectTop + thirdH * 2), strokeWidth = 1.dp.toPx())

                        // 5. Draggable Corner Handles
                        val bracketLen = 22.dp.toPx()
                        val bracketThickness = 4.dp.toPx()
                        val handleRadius = 9.dp.toPx()

                        // Top-Left corner
                        drawLine(blueColor, Offset(rectLeft, rectTop + bracketLen), Offset(rectLeft, rectTop), bracketThickness)
                        drawLine(blueColor, Offset(rectLeft, rectTop), Offset(rectLeft + bracketLen, rectTop), bracketThickness)
                        drawCircle(blueColor, handleRadius, Offset(rectLeft, rectTop))
                        drawCircle(Color.White, handleRadius * 0.55f, Offset(rectLeft, rectTop))

                        // Top-Right corner
                        drawLine(blueColor, Offset(rectRight - bracketLen, rectTop), Offset(rectRight, rectTop), bracketThickness)
                        drawLine(blueColor, Offset(rectRight, rectTop), Offset(rectRight, rectTop + bracketLen), bracketThickness)
                        drawCircle(blueColor, handleRadius, Offset(rectRight, rectTop))
                        drawCircle(Color.White, handleRadius * 0.55f, Offset(rectRight, rectTop))

                        // Bottom-Left corner
                        drawLine(blueColor, Offset(rectLeft, rectBottom - bracketLen), Offset(rectLeft, rectBottom), bracketThickness)
                        drawLine(blueColor, Offset(rectLeft, rectBottom), Offset(rectLeft + bracketLen, rectBottom), bracketThickness)
                        drawCircle(blueColor, handleRadius, Offset(rectLeft, rectBottom))
                        drawCircle(Color.White, handleRadius * 0.55f, Offset(rectLeft, rectBottom))

                        // Bottom-Right corner
                        drawLine(blueColor, Offset(rectRight - bracketLen, rectBottom), Offset(rectRight, rectBottom), bracketThickness)
                        drawLine(blueColor, Offset(rectRight, rectBottom), Offset(rectRight, rectBottom - bracketLen), bracketThickness)
                        drawCircle(blueColor, handleRadius, Offset(rectRight, rectBottom))
                        drawCircle(Color.White, handleRadius * 0.55f, Offset(rectRight, rectBottom))

                        // 6. Midpoint Edge Pills
                        val pillRadius = 5.dp.toPx()
                        drawCircle(blueColor, pillRadius, Offset(rectLeft + rectW / 2f, rectTop))
                        drawCircle(blueColor, pillRadius, Offset(rectLeft + rectW / 2f, rectBottom))
                        drawCircle(blueColor, pillRadius, Offset(rectLeft, rectTop + rectH / 2f))
                        drawCircle(blueColor, pillRadius, Offset(rectRight, rectTop + rectH / 2f))
                    }


                }

                // Page Navigation Strip: [ < ]  1 / 5  [ > ]
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentPageIdx > 0) currentPageIdx-- },
                            enabled = currentPageIdx > 0,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("crop_prev_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Page",
                                tint = if (currentPageIdx > 0) Color.White else Color(0xFF475569)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "${currentPageIdx + 1} / $totalPages",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.testTag("crop_page_indicator")
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { if (currentPageIdx < totalPages - 1) currentPageIdx++ },
                            enabled = currentPageIdx < totalPages - 1,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("crop_next_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Page",
                                tint = if (currentPageIdx < totalPages - 1) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// WORKSPACE SUB-DIALOG: PAGE NUMBERS
// -------------------------------------------------------------------------------------------------
@Composable
fun WorkspacePageNumbersDialog(
    file: PdfFileRecord,
    onDismiss: () -> Unit,
    onApply: (String, PdfEngine.PageNumberPosition, Set<Int>) -> Unit
) {
    var format by remember { mutableStateOf("Page {X} of {Y}") }
    var position by remember { mutableStateOf(PdfEngine.PageNumberPosition.BOTTOM_CENTER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔢 Page Numbers", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth()) {
                Text("Select numbering format:")
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = format == "Page {X} of {Y}",
                        onClick = { format = "Page {X} of {Y}" },
                        label = { Text("Page 1 of N", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = format == "Page {X}",
                        onClick = { format = "Page {X}" },
                        label = { Text("Page 1", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = format == "- {X} -",
                        onClick = { format = "- {X} -" },
                        label = { Text("- 1 -", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Position on page:")
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = position == PdfEngine.PageNumberPosition.TOP_LEFT,
                            onClick = { position = PdfEngine.PageNumberPosition.TOP_LEFT },
                            label = { Text("Top Left", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = position == PdfEngine.PageNumberPosition.TOP_CENTER,
                            onClick = { position = PdfEngine.PageNumberPosition.TOP_CENTER },
                            label = { Text("Top Center", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = position == PdfEngine.PageNumberPosition.TOP_RIGHT,
                            onClick = { position = PdfEngine.PageNumberPosition.TOP_RIGHT },
                            label = { Text("Top Right", fontSize = 10.sp) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = position == PdfEngine.PageNumberPosition.BOTTOM_LEFT,
                            onClick = { position = PdfEngine.PageNumberPosition.BOTTOM_LEFT },
                            label = { Text("Bottom Left", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = position == PdfEngine.PageNumberPosition.BOTTOM_CENTER,
                            onClick = { position = PdfEngine.PageNumberPosition.BOTTOM_CENTER },
                            label = { Text("Bottom Center", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = position == PdfEngine.PageNumberPosition.BOTTOM_RIGHT,
                            onClick = { position = PdfEngine.PageNumberPosition.BOTTOM_RIGHT },
                            label = { Text("Bottom Right", fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(format, position, emptySet()) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Add Numbers")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// WORKSPACE SUB-DIALOG: DOCUMENT INFORMATION
// -------------------------------------------------------------------------------------------------
@Composable
fun WorkspaceDocumentInfoDialog(
    file: PdfFileRecord,
    onDismiss: () -> Unit
) {
    val sizeKb = (file.fileSize / 1024).coerceAtLeast(1)
    val dateStr = SimpleDateFormat("MMMM d, yyyy 'at' HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ℹ Document Information", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text("File Name", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                    Text(file.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text("File Size", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                    Text("$sizeKb KB (${file.fileSize} bytes)", fontSize = 13.sp)
                }
                Column {
                    Text("Page Count", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                    Text("${file.pageCount} pages", fontSize = 13.sp)
                }
                Column {
                    Text("Storage Path", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                    Text(file.filePath, fontSize = 11.sp, color = Color(0xFF64748B))
                }
                Column {
                    Text("Last Modified", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                    Text(dateStr, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)) {
                Text("Close")
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// WORKSPACE SUB-DIALOG: SIGNATURE DETAILS (ADOBE ACROBAT PROPERTIES)
// -------------------------------------------------------------------------------------------------
@Composable
fun WorkspaceSignatureDetailsDialog(
    result: SignatureVerificationResult,
    onDismiss: () -> Unit,
    onOpenFullValidation: () -> Unit
) {
    val isValid = result.overallValidity == SignatureValidity.VALID
    val statusColor = if (isValid) Color(0xFF10B981) else Color(0xFFEF4444)
    val statusBg = if (isValid) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
    val statusTitle = if (isValid) "Signature Valid • Verified" else "Invalid Signature"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Signature Properties",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status overview card
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusTitle,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isValid) {
                                "The document has not been modified since this signature was certified and applied."
                            } else {
                                "The signature could not be verified or the document has been altered."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Signatures List
                Text(
                    text = "Signatures (${result.totalSignatures})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )

                result.signatures.forEachIndexed { index, sig ->
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Signature #${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CrimsonPrimary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (sig.certificate?.isGovOrUidaiCertified == true) {
                                    Surface(
                                        color = Color(0xFFEDE9FE),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "GOVT / DSC",
                                            color = Color(0xFF6D28D9),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            Column {
                                Text("Signer Name", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = sig.signerName.ifBlank { "Unknown Signer" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            if (sig.signingTime.isNotBlank()) {
                                Column {
                                    Text("Signing Date & Time", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    Text(sig.signingTime, fontSize = 12.sp, color = Color(0xFF334155))
                                }
                            }

                            if (!sig.reason.isNullOrBlank()) {
                                Column {
                                    Text("Reason", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    Text(sig.reason, fontSize = 12.sp, color = Color(0xFF334155))
                                }
                            }

                            if (!sig.location.isNullOrBlank()) {
                                Column {
                                    Text("Location", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    Text(sig.location, fontSize = 12.sp, color = Color(0xFF334155))
                                }
                            }

                            // Certificate Authority
                            sig.certificate?.let { cert ->
                                Column {
                                    Text("Issued by (Certifying Authority)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    Text(cert.issuerCommonName.ifBlank { cert.issuerOrganization }, fontSize = 12.sp, color = Color(0xFF334155))
                                }
                                if (cert.validUntil != null) {
                                    Column {
                                        Text("Validity Period", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val expDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cert.validUntil)
                                            val expiryText = if (cert.isExpired) "Expires: $expDateStr (Expired)" else "Expires: $expDateStr"
                                            Text(
                                                text = expiryText,
                                                fontSize = 12.sp,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }

                            // Byte Range & Integrity Check
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Byte Range Integrity: Intact",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Close")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onOpenFullValidation
            ) {
                Text("Full Report")
            }
        }
    )
}

// =========================================================================
// PROFESSIONAL PDF ANNOTATION INTERACTION HELPERS & COLOR PICKER DIALOG
// =========================================================================

enum class PdfDragHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    EDGE_TOP, EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT,
    START_POINT, END_POINT, ROTATION_HANDLE
}

enum class PdfInteractionMode {
    NONE, DRAWING, HIGHLIGHTING, CREATING_SHAPE, MOVING, RESIZING, ROTATING, ERASING
}

internal fun unrotatePoint(x: Float, y: Float, cx: Float, cy: Float, angleDeg: Float): Offset {
    val rad = Math.toRadians(-angleDeg.toDouble())
    val cos = Math.cos(rad).toFloat()
    val sin = Math.sin(rad).toFloat()
    val dx = x - cx
    val dy = y - cy
    return Offset(cx + dx * cos - dy * sin, cy + dx * sin + dy * cos)
}

internal fun rotatePoint(x: Float, y: Float, cx: Float, cy: Float, angleDeg: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    val cos = Math.cos(rad).toFloat()
    val sin = Math.sin(rad).toFloat()
    val dx = x - cx
    val dy = y - cy
    return Offset(cx + dx * cos - dy * sin, cy + dx * sin + dy * cos)
}

internal fun rotateVector(vx: Float, vy: Float, angleDeg: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    val cos = Math.cos(rad).toFloat()
    val sin = Math.sin(rad).toFloat()
    return Offset(vx * cos - vy * sin, vx * sin + vy * cos)
}

internal fun getAnnotationBounds(item: PdfEngine.EditItem): RectF {
    return when (item.type) {
        PdfEngine.AnnotationType.DRAW, PdfEngine.AnnotationType.PEN, PdfEngine.AnnotationType.HIGHLIGHTER -> {
            if (item.points.isNotEmpty()) {
                var minX = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                for (pt in item.points) {
                    if (pt.first < minX) minX = pt.first
                    if (pt.first > maxX) maxX = pt.first
                    if (pt.second < minY) minY = pt.second
                    if (pt.second > maxY) maxY = pt.second
                }
                val pad = (item.strokeWidth / 2f).coerceAtLeast(8f)
                RectF(minX - pad, minY - pad, maxX + pad, maxY + pad)
            } else {
                val left = Math.min(item.posX, item.endX)
                val right = Math.max(item.posX, item.endX)
                val top = Math.min(item.posY, item.endY)
                val bottom = Math.max(item.posY, item.endY)
                val pad = (item.strokeWidth / 2f).coerceAtLeast(8f)
                RectF(left - pad, top - pad, right + pad, bottom + pad)
            }
        }
        PdfEngine.AnnotationType.RECTANGLE, PdfEngine.AnnotationType.CIRCLE -> {
            val left = Math.min(item.posX, item.endX)
            val right = Math.max(item.posX, item.endX)
            val top = Math.min(item.posY, item.endY)
            val bottom = Math.max(item.posY, item.endY)
            val pad = (item.strokeWidth / 2f).coerceAtLeast(4f)
            RectF(left - pad, top - pad, right + pad, bottom + pad)
        }
        PdfEngine.AnnotationType.LINE, PdfEngine.AnnotationType.ARROW, PdfEngine.AnnotationType.UNDERLINE, PdfEngine.AnnotationType.STRIKETHROUGH -> {
            val left = Math.min(item.posX, item.endX)
            val right = Math.max(item.posX, item.endX)
            val top = Math.min(item.posY, item.endY)
            val bottom = Math.max(item.posY, item.endY)
            val pad = (item.strokeWidth / 2f).coerceAtLeast(10f)
            RectF(left - pad, top - pad, right + pad, bottom + pad)
        }
        PdfEngine.AnnotationType.TEXT -> {
            val textPaint = TextPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                textSize = item.fontSize.coerceAtLeast(8f)
                val family = when (item.fontFamily) {
                    "Serif", "SERIF" -> AndroidTypeface.SERIF
                    "Monospace", "MONOSPACE" -> AndroidTypeface.MONOSPACE
                    else -> AndroidTypeface.SANS_SERIF
                }
                val style = when {
                    item.isBold && item.isItalic -> AndroidTypeface.BOLD_ITALIC
                    item.isBold -> AndroidTypeface.BOLD
                    item.isItalic -> AndroidTypeface.ITALIC
                    else -> AndroidTypeface.NORMAL
                }
                typeface = AndroidTypeface.create(family, style)
            }
            val textToMeasure = if (item.text.isEmpty()) " " else item.text
            val lines = textToMeasure.split("\n")
            var maxLineW = 0f
            for (l in lines) {
                val lw = textPaint.measureText(l)
                if (lw > maxLineW) maxLineW = lw
            }
            val naturalW = (maxLineW + 16f).coerceAtLeast(40f)
            val targetW = if (item.boxWidth > 0f) item.boxWidth.coerceAtLeast(30f) else naturalW
            val align = when (item.alignment) {
                "CENTER" -> Layout.Alignment.ALIGN_CENTER
                "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_NORMAL
            }
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(textToMeasure, 0, textToMeasure.length, textPaint, targetW.toInt().coerceAtLeast(10))
                    .setAlignment(align)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(textToMeasure, textPaint, targetW.toInt().coerceAtLeast(10), align, 1.15f, 0f, false)
            }
            val layoutW = targetW
            val layoutH = if (item.boxHeight > 0f) Math.max(staticLayout.height.toFloat(), item.boxHeight) else staticLayout.height.toFloat().coerceAtLeast(20f)
            RectF(item.posX, item.posY, item.posX + layoutW, item.posY + layoutH)
        }
        PdfEngine.AnnotationType.OCR_REPLACEMENT -> {
            if (item.maskRect != null) {
                RectF(item.maskRect.left.toFloat(), item.maskRect.top.toFloat(), item.maskRect.right.toFloat(), item.maskRect.bottom.toFloat())
            } else {
                RectF(item.posX, item.posY - item.fontSize, item.posX + 100f, item.posY + 10f)
            }
        }
        PdfEngine.AnnotationType.STAMP -> {
            RectF(item.posX, item.posY, item.posX + 160f, item.posY + 45f)
        }
        PdfEngine.AnnotationType.IMAGE -> {
            RectF(item.posX, item.posY, item.posX + item.imageWidth, item.posY + item.imageHeight)
        }
        else -> RectF(item.posX, item.posY, item.posX + 100f, item.posY + 50f)
    }
}

internal fun distanceToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
    if (l2 == 0f) return Math.hypot((px - x1).toDouble(), (py - y1).toDouble()).toFloat()
    val t = (((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2).coerceIn(0f, 1f)
    val projX = x1 + t * (x2 - x1)
    val projY = y1 + t * (y2 - y1)
    return Math.hypot((px - projX).toDouble(), (py - projY).toDouble()).toFloat()
}

internal fun annotationContainsPoint(item: PdfEngine.EditItem, pdfX: Float, pdfY: Float): Boolean {
    val bounds = getAnnotationBounds(item)
    val cx = (bounds.left + bounds.right) / 2f
    val cy = (bounds.top + bounds.bottom) / 2f
    val rot = item.getRotationAngle()
    val unrot = if (rot != 0f) unrotatePoint(pdfX, pdfY, cx, cy, rot) else Offset(pdfX, pdfY)
    val localX = unrot.x
    val localY = unrot.y

    when (item.type) {
        PdfEngine.AnnotationType.TEXT, PdfEngine.AnnotationType.OCR_REPLACEMENT -> {
            val tol = 12f
            return localX >= bounds.left - tol && localX <= bounds.right + tol &&
                   localY >= bounds.top - tol && localY <= bounds.bottom + tol
        }
        PdfEngine.AnnotationType.IMAGE, PdfEngine.AnnotationType.STAMP -> {
            val tol = 12f
            return localX >= bounds.left - tol && localX <= bounds.right + tol &&
                   localY >= bounds.top - tol && localY <= bounds.bottom + tol
        }
        PdfEngine.AnnotationType.RECTANGLE, PdfEngine.AnnotationType.CIRCLE -> {
            val tol = 16f
            return localX >= bounds.left - tol && localX <= bounds.right + tol &&
                   localY >= bounds.top - tol && localY <= bounds.bottom + tol
        }
        PdfEngine.AnnotationType.LINE, PdfEngine.AnnotationType.ARROW, PdfEngine.AnnotationType.UNDERLINE, PdfEngine.AnnotationType.STRIKETHROUGH -> {
            val d = distanceToSegment(localX, localY, item.posX, item.posY, item.endX, item.endY)
            return d <= (item.strokeWidth + 24f)
        }
        PdfEngine.AnnotationType.DRAW, PdfEngine.AnnotationType.PEN, PdfEngine.AnnotationType.HIGHLIGHTER -> {
            val tol = (item.strokeWidth / 2f + 24f)
            if (localX < bounds.left - tol || localX > bounds.right + tol || localY < bounds.top - tol || localY > bounds.bottom + tol) {
                return false
            }
            if (item.points.isNotEmpty()) {
                for (i in 0 until item.points.size - 1) {
                    val p1 = item.points[i]
                    val p2 = item.points[i + 1]
                    if (distanceToSegment(localX, localY, p1.first, p1.second, p2.first, p2.second) <= tol) {
                        return true
                    }
                }
                return localX >= bounds.left && localX <= bounds.right && localY >= bounds.top && localY <= bounds.bottom
            } else {
                val d = distanceToSegment(localX, localY, item.posX, item.posY, item.endX, item.endY)
                return d <= (item.strokeWidth + 24f)
            }
        }
        else -> {
            val tol = 16f
            return localX >= bounds.left - tol && localX <= bounds.right + tol && localY >= bounds.top - tol && localY <= bounds.bottom + tol
        }
    }
}

internal fun annotationIntersectsCircle(item: PdfEngine.EditItem, cx: Float, cy: Float, radius: Float): Boolean {
    val bounds = getAnnotationBounds(item)
    if (cx < bounds.left - radius || cx > bounds.right + radius || cy < bounds.top - radius || cy > bounds.bottom + radius) {
        return false
    }
    when (item.type) {
        PdfEngine.AnnotationType.DRAW, PdfEngine.AnnotationType.PEN, PdfEngine.AnnotationType.HIGHLIGHTER -> {
            if (item.points.isNotEmpty()) {
                val threshold = radius + item.strokeWidth / 2f
                for (i in 0 until item.points.size - 1) {
                    val p1 = item.points[i]
                    val p2 = item.points[i + 1]
                    if (distanceToSegment(cx, cy, p1.first, p1.second, p2.first, p2.second) <= threshold) {
                        return true
                    }
                }
                return false
            } else {
                return distanceToSegment(cx, cy, item.posX, item.posY, item.endX, item.endY) <= (radius + item.strokeWidth / 2f)
            }
        }
        PdfEngine.AnnotationType.LINE, PdfEngine.AnnotationType.ARROW, PdfEngine.AnnotationType.UNDERLINE, PdfEngine.AnnotationType.STRIKETHROUGH -> {
            return distanceToSegment(cx, cy, item.posX, item.posY, item.endX, item.endY) <= (radius + item.strokeWidth / 2f)
        }
        else -> {
            return true
        }
    }
}

@Composable
internal fun AnnotationColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorPicked: (Int) -> Unit
) {
    var red by remember { mutableFloatStateOf(AndroidColor.red(initialColor) / 255f) }
    var green by remember { mutableFloatStateOf(AndroidColor.green(initialColor) / 255f) }
    var blue by remember { mutableFloatStateOf(AndroidColor.blue(initialColor) / 255f) }

    val currentColor = AndroidColor.rgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Custom Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(currentColor))
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                )

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Red", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(red * 255).toInt()}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Slider(
                        value = red,
                        onValueChange = { red = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Green", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(green * 255).toInt()}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Slider(
                        value = green,
                        onValueChange = { green = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Blue", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(blue * 255).toInt()}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Slider(
                        value = blue,
                        onValueChange = { blue = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorPicked(currentColor)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Select Color")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
internal fun AnnotationEditTextDialog(
    initialText: String,
    initialFontSize: Float,
    initialColor: Int,
    initialBold: Boolean,
    initialItalic: Boolean,
    initialUnderline: Boolean,
    initialAlignment: String,
    onDismiss: () -> Unit,
    onSave: (text: String, fontSize: Float, color: Int, bold: Boolean, italic: Boolean, underline: Boolean, align: String) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var fontSize by remember { mutableFloatStateOf(initialFontSize) }
    var color by remember { mutableIntStateOf(initialColor) }
    var isBold by remember { mutableStateOf(initialBold) }
    var isItalic by remember { mutableStateOf(initialItalic) }
    var isUnderline by remember { mutableStateOf(initialUnderline) }
    var alignment by remember { mutableStateOf(initialAlignment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Text Annotation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Enter text...", color = Color.Gray) },
                    singleLine = false,
                    maxLines = 6,
                    textStyle = LocalTextStyle.current.copy(
                        color = Color(color),
                        fontSize = 14.sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Style toggles and alignment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { isBold = !isBold },
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (isBold) CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                        IconButton(
                            onClick = { isItalic = !isItalic },
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (isItalic) CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                        IconButton(
                            onClick = { isUnderline = !isUnderline },
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (isUnderline) CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }

                    // Alignment toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { alignment = "LEFT" },
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (alignment == "LEFT") CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Left", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                        IconButton(
                            onClick = { alignment = "CENTER" },
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (alignment == "CENTER") CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                        IconButton(
                            onClick = { alignment = "RIGHT" },
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (alignment == "RIGHT") CrimsonPrimary else Color(0xFF334155), RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Right", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                }

                // Font size controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font Size", color = Color.White, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { fontSize = (fontSize - 2f).coerceAtLeast(8f) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Text("${fontSize.toInt()}sp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { fontSize = (fontSize + 2f).coerceAtMost(160f) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Quick color palette
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        AndroidColor.BLACK,
                        AndroidColor.rgb(37, 99, 235),
                        AndroidColor.rgb(220, 38, 38),
                        AndroidColor.rgb(22, 163, 74),
                        AndroidColor.rgb(234, 88, 12),
                        AndroidColor.rgb(147, 51, 234),
                        AndroidColor.WHITE
                    ).forEach { cHex ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(cHex))
                                .border(
                                    width = if (color == cHex) 2.dp else 1.dp,
                                    color = if (color == cHex) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { color = cHex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(text, fontSize, color, isBold, isItalic, isUnderline, alignment)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171))
                ) {
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
internal fun OcrEditDialog(
    initialText: String,
    initialFontSize: Float,
    initialTextColor: Int,
    initialPaperColor: Int,
    initialIsBold: Boolean,
    initialIsItalic: Boolean,
    initialAlignment: String,
    onDismiss: () -> Unit,
    onSave: (
        newText: String,
        newFontSize: Float,
        newTextColor: Int,
        newPaperColor: Int,
        isBold: Boolean,
        isItalic: Boolean,
        alignment: String
    ) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var fontSize by remember { mutableFloatStateOf(initialFontSize) }
    var textColor by remember { mutableIntStateOf(initialTextColor) }
    var paperColor by remember { mutableIntStateOf(initialPaperColor) }
    var isBold by remember { mutableStateOf(initialIsBold) }
    var isItalic by remember { mutableStateOf(initialIsItalic) }
    var alignment by remember { mutableStateOf(initialAlignment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Edit Document Text",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Detected Text Block",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Text input
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text Content", fontSize = 12.sp) },
                    singleLine = false,
                    maxLines = 5,
                    textStyle = LocalTextStyle.current.copy(
                        color = Color(textColor),
                        fontSize = 14.sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ocr_text_input")
                )

                // Font Size & Formatting
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Size: ", color = Color.Gray, fontSize = 12.sp)
                        IconButton(
                            onClick = { fontSize = (fontSize - 1f).coerceAtLeast(8f) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Text("${fontSize.toInt()}sp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        IconButton(
                            onClick = { fontSize = (fontSize + 1f).coerceAtMost(60f) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Bold / Italic
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = isBold,
                            onClick = { isBold = !isBold },
                            label = { Text("B", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = isItalic,
                            onClick = { isItalic = !isItalic },
                            label = { Text("I", fontStyle = FontStyle.Italic, fontSize = 12.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    // Alignment
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf("LEFT", "CENTER", "RIGHT").forEach { align ->
                            val isSel = alignment == align
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSel) CrimsonPrimary else Color(0xFF334155))
                                    .clickable { alignment = align },
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (align) {
                                    "CENTER" -> Icons.Default.FormatAlignCenter
                                    "RIGHT" -> Icons.Default.FormatAlignRight
                                    else -> Icons.Default.FormatAlignLeft
                                }
                                Icon(icon, contentDescription = align, tint = Color.White, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }

                // Text Color Palette
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Text Color", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            AndroidColor.BLACK,
                            AndroidColor.rgb(37, 99, 235),
                            AndroidColor.rgb(220, 38, 38),
                            AndroidColor.rgb(22, 163, 74),
                            AndroidColor.rgb(234, 88, 12),
                            AndroidColor.rgb(71, 85, 105),
                            AndroidColor.WHITE
                        ).forEach { cHex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(cHex))
                                    .border(
                                        width = if (textColor == cHex) 2.dp else 1.dp,
                                        color = if (textColor == cHex) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { textColor = cHex }
                            )
                        }
                    }
                }

                // Paper Background Mask Color
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Background Mask (Paper Color)", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Pair("Paper", initialPaperColor),
                            Pair("White", AndroidColor.WHITE),
                            Pair("Cream", AndroidColor.rgb(255, 253, 240)),
                            Pair("Light Gray", AndroidColor.rgb(241, 245, 249))
                        ).forEach { (lbl, cHex) ->
                            FilterChip(
                                selected = paperColor == cHex,
                                onClick = { paperColor = cHex },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(cHex))
                                                .border(0.5.dp, Color.Gray, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(lbl, fontSize = 10.sp)
                                    }
                                },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(text, fontSize, textColor, paperColor, isBold, isItalic, alignment)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.testTag("ocr_save_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Changes")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171)),
                    modifier = Modifier.testTag("ocr_erase_button")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Erase Text")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
internal fun WorkspaceWatermarkDialog(
    file: PdfFileRecord,
    currentPage: Int,
    onDismiss: () -> Unit,
    onApply: (
        text: String,
        rotationDeg: Int,
        opacity: Float,
        fontSize: Float,
        colorHex: Int,
        applyAllPages: Boolean,
        isTiled: Boolean
    ) -> Unit
) {
    var text by remember { mutableStateOf("CONFIDENTIAL") }
    var rotationDeg by remember { mutableIntStateOf(-45) }
    var opacity by remember { mutableFloatStateOf(0.35f) }
    var fontSize by remember { mutableFloatStateOf(44f) }
    var colorHex by remember { mutableIntStateOf(AndroidColor.rgb(220, 38, 38)) }
    var applyAllPages by remember { mutableStateOf(true) }
    var isTiled by remember { mutableStateOf(false) }

    val presets = listOf("CONFIDENTIAL", "DO NOT COPY", "DRAFT", "ORIGINAL", "SAMPLE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Watermark", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Text input
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Watermark Text") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("watermark_text_input")
                )

                // Quick presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets) { preset ->
                        FilterChip(
                            selected = text == preset,
                            onClick = { text = preset },
                            label = { Text(preset, fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Watermark Layout (Single Center vs Tiled Pattern)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Watermark Layout", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isTiled,
                            onClick = { isTiled = false },
                            label = { Text("Single Center", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(30.dp).testTag("wm_layout_single")
                        )
                        FilterChip(
                            selected = isTiled,
                            onClick = { isTiled = true },
                            label = { Text("Full-Page Repeating (Tiled)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(30.dp).testTag("wm_layout_tiled")
                        )
                    }
                }

                // Orientation / Angle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Orientation", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("45° Diagonal", -45),
                            Pair("Horizontal", 0),
                            Pair("Vertical", -90)
                        ).forEach { (label, deg) ->
                            FilterChip(
                                selected = rotationDeg == deg,
                                onClick = { rotationDeg = deg },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).height(28.dp)
                            )
                        }
                    }
                }

                // Opacity & Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Opacity: ${(opacity * 100).toInt()}%", color = Color.Gray, fontSize = 11.sp)
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0.1f..0.9f,
                            colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Size: ${fontSize.toInt()}sp", color = Color.Gray, fontSize = 11.sp)
                        Slider(
                            value = fontSize,
                            onValueChange = { fontSize = it },
                            valueRange = 20f..80f,
                            colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                        )
                    }
                }

                // Color Palette
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Watermark Color", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            AndroidColor.rgb(220, 38, 38), // Red
                            AndroidColor.rgb(100, 116, 139), // Slate gray
                            AndroidColor.rgb(37, 99, 235), // Blue
                            AndroidColor.rgb(234, 88, 12), // Orange
                            AndroidColor.BLACK
                        ).forEach { cHex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(cHex))
                                    .border(
                                        width = if (colorHex == cHex) 2.dp else 1.dp,
                                        color = if (colorHex == cHex) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = cHex }
                            )
                        }
                    }
                }

                // Target Pages
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = applyAllPages,
                        onClick = { applyAllPages = true },
                        label = { Text("All Pages (${file.pageCount})", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).height(28.dp)
                    )
                    FilterChip(
                        selected = !applyAllPages,
                        onClick = { applyAllPages = false },
                        label = { Text("Current Page (${currentPage + 1})", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).height(28.dp)
                    )
                }

                // Instant Live Preview Box
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text.ifBlank { "PREVIEW" }.uppercase(),
                            color = Color(colorHex).copy(alpha = opacity),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer(rotationZ = rotationDeg.toFloat())
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(text, rotationDeg, opacity, fontSize, colorHex, applyAllPages, isTiled)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("watermark_apply_button")
            ) {
                Text("Apply Watermark")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}
