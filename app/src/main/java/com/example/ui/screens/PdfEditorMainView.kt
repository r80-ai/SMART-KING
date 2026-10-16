package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Typeface as AndroidTypeface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfFileRecord
import com.example.pdf.PdfEngine
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
import kotlin.math.roundToInt

/**
 * Editor tools enum
 */
enum class EditorTool {
    VIEW, TEXT, IMAGE, DRAW, HIGHLIGHTER, ERASER
}

/**
 * History action for Undo / Redo
 */
sealed class EditHistoryAction {
    data class AddItem(val item: PdfEngine.EditItem) : EditHistoryAction()
    data class RemoveItem(val item: PdfEngine.EditItem) : EditHistoryAction()
    data class ModifyItem(val oldItem: PdfEngine.EditItem, val newItem: PdfEngine.EditItem) : EditHistoryAction()
}

/**
 * Font option with label and font identifier
 */
data class FontOption(
    val id: String,
    val displayName: String,
    val previewText: String,
    val fontFamily: FontFamily = FontFamily.Default
)

/**
 * PDFViewerEditor alias for PdfEditorMainView
 */
@Composable
fun PDFViewerEditor(
    fileRecord: PdfFileRecord,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    PdfEditorMainView(
        fileRecord = fileRecord,
        viewModel = viewModel,
        onBack = onBack
    )
}

/**
 * PdfEditorMainView - Simplified, high-performance PDF editor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorMainView(
    fileRecord: PdfFileRecord,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var currentFile by remember(fileRecord) { mutableStateOf(fileRecord) }
    val pages by viewModel.renderedPages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingMessage by viewModel.processingMessage.collectAsState()

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentTool by remember { mutableStateOf(EditorTool.VIEW) }

    // Viewport transforms
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var fitPageMode by remember { mutableStateOf(true) }
    var showThumbnailTray by remember { mutableStateOf(false) }

    // Edit items & history
    val editItems = remember { mutableStateListOf<PdfEngine.EditItem>() }
    val deletedPages = remember { mutableStateListOf<Int>() }
    val pageRotations = remember { mutableStateMapOf<Int, Int>() }
    var selectedAnnotationId by remember { mutableStateOf<String?>(null) }
    val undoStack = remember { mutableStateListOf<EditHistoryAction>() }
    val redoStack = remember { mutableStateListOf<EditHistoryAction>() }

    // Active page size in pixels
    var pageContainerSize by remember { mutableStateOf(IntSize(1, 1)) }

    // Undo / Redo helpers
    fun pushAction(action: EditHistoryAction) {
        undoStack.add(action)
        redoStack.clear()
    }

    fun handleUndo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeAt(undoStack.lastIndex)
        when (action) {
            is EditHistoryAction.AddItem -> {
                editItems.removeAll { it.id == action.item.id }
                if (selectedAnnotationId == action.item.id) selectedAnnotationId = null
            }
            is EditHistoryAction.RemoveItem -> {
                editItems.add(action.item)
                selectedAnnotationId = action.item.id
            }
            is EditHistoryAction.ModifyItem -> {
                val idx = editItems.indexOfFirst { it.id == action.newItem.id }
                if (idx != -1) {
                    editItems[idx] = action.oldItem
                }
            }
        }
        redoStack.add(action)
    }

    fun handleRedo() {
        if (redoStack.isEmpty()) return
        val action = redoStack.removeAt(redoStack.lastIndex)
        when (action) {
            is EditHistoryAction.AddItem -> {
                editItems.add(action.item)
                selectedAnnotationId = action.item.id
            }
            is EditHistoryAction.RemoveItem -> {
                editItems.removeAll { it.id == action.item.id }
                if (selectedAnnotationId == action.item.id) selectedAnnotationId = null
            }
            is EditHistoryAction.ModifyItem -> {
                val idx = editItems.indexOfFirst { it.id == action.oldItem.id }
                if (idx != -1) {
                    editItems[idx] = action.newItem
                }
            }
        }
        undoStack.add(action)
    }

    // Text tool state
    var showAddTextDialog by remember { mutableStateOf(false) }
    var textInputContent by remember { mutableStateOf("") }
    var textColor by remember { mutableIntStateOf(AndroidColor.BLACK) }
    var textFontSize by remember { mutableFloatStateOf(24f) }
    var textFontFamily by remember { mutableStateOf("BENGALI") }
    var textIsBold by remember { mutableStateOf(false) }
    var textIsItalic by remember { mutableStateOf(false) }

    // Drawing state
    var drawColor by remember { mutableIntStateOf(AndroidColor.rgb(220, 38, 38)) }
    var strokeThickness by remember { mutableFloatStateOf(4f) }
    var highlighterWidth by remember { mutableFloatStateOf(28f) }
    var highlighterColor by remember { mutableIntStateOf(AndroidColor.rgb(250, 204, 21)) }
    val activeStroke = remember { mutableStateListOf<Pair<Float, Float>>() }

    // Ensure pages are loaded
    LaunchedEffect(currentFile.filePath, currentFile.uriString) {
        viewModel.loadPagesForViewer(currentFile, forceReload = true)
    }

    // Clamp page index
    LaunchedEffect(pages.size) {
        if (pages.isNotEmpty() && currentPageIndex >= pages.size) {
            currentPageIndex = (pages.size - 1).coerceAtLeast(0)
        }
    }

    // Image Picker for inserting pictures onto PDF
    var isReplacingImage by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            withContext(Dispatchers.Main) {
                                val targetW = (pageContainerSize.width * 0.45f).coerceIn(120f, 400f)
                                val targetH = (targetW * bmp.height / bmp.width.coerceAtLeast(1)).coerceAtLeast(40f)
                                val startX = ((pageContainerSize.width - targetW) / 2f).coerceAtLeast(20f)
                                val startY = ((pageContainerSize.height - targetH) / 2f).coerceAtLeast(20f)

                                val newItem = PdfEngine.EditItem(
                                    pageIndex = currentPageIndex,
                                    type = PdfEngine.AnnotationType.IMAGE,
                                    imageBitmap = bmp,
                                    originalImageBitmap = bmp,
                                    posX = startX,
                                    posY = startY,
                                    imageWidth = targetW,
                                    imageHeight = targetH,
                                    baseWidth = pageContainerSize.width.toFloat(),
                                    baseHeight = pageContainerSize.height.toFloat()
                                )
                                editItems.add(newItem)
                                selectedAnnotationId = newItem.id
                                pushAction(EditHistoryAction.AddItem(newItem))
                                currentTool = EditorTool.VIEW
                                Toast.makeText(context, "Image added. Drag & resize freely!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val totalPages = pages.size.coerceAtLeast(currentFile.pageCount).coerceAtLeast(1)
    val currentRotation = pageRotations[currentPageIndex] ?: 0
    val animatedRotation by animateFloatAsState(
        targetValue = currentRotation.toFloat(),
        label = "pageRotation"
    )

    BackHandler {
        if (selectedAnnotationId != null) {
            selectedAnnotationId = null
        } else if (showThumbnailTray) {
            showThumbnailTray = false
        } else {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("pdf_editor_main_view")
    ) {
        // ====================================================
        // 1. TOP APP BAR: [←] Document.pdf [Undo] [Redo] [Share] [Save]
        // Three-dot menu removed; Undo and Redo added near Share and Save!
        // ====================================================
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button + Document title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = currentFile.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (pages.isNotEmpty()) "Page ${currentPageIndex + 1} of ${pages.size}" else "${currentFile.pageCount} pages",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Actions: Undo, Redo, Share, Save (No three-dot menu)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Undo
                    IconButton(
                        onClick = { handleUndo() },
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier.testTag("editor_top_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) Color.White else Color(0xFF64748B)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { handleRedo() },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.testTag("editor_top_redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) Color.White else Color(0xFF64748B)
                        )
                    }

                    // Share
                    IconButton(
                        onClick = { viewModel.shareFile(context, currentFile) },
                        modifier = Modifier.testTag("editor_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }

                    // Save
                    Button(
                        onClick = {
                            val outputName = "${currentFile.nameWithoutExtension()}_edited.pdf"
                            viewModel.saveEditedPdf(
                                sourceRecord = currentFile,
                                items = editItems.toList(),
                                deletedPages = deletedPages.toSet(),
                                pageRotations = pageRotations.toMap(),
                                outputName = outputName
                            ) { newRec ->
                                currentFile = newRec
                                Toast.makeText(context, "Saved as $outputName", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("editor_top_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ====================================================
        // 2. MAIN PREVIEW CONTAINER
        // - "Zoom In" and "Fit Width" relocated to TOP area of container
        // - PDF Canvas with interactive draggable/resizable text & image manipulation
        // - Canvas shift during drawing fixed
        // - Highlighter with customizable touch size
        // - Eraser effectively selects and deletes paths
        // ====================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .background(Color(0xFF0B1120)),
            contentAlignment = Alignment.Center
        ) {
            if (pages.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = CrimsonPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = processingMessage.ifBlank { "Opening document..." },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Could not render document pages",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.loadPagesForViewer(currentFile, forceReload = true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Rendering")
                        }
                    }
                }
            } else {
                val currentBmp = pages.getOrNull(currentPageIndex) ?: pages.firstOrNull()
                if (currentBmp != null) {
                    // FIX: Viewport pan/zoom is active ONLY when tool is VIEW.
                    // This prevents canvas shift or resizing during drawing/highlighting!
                    val isPanZoomEnabled = (currentTool == EditorTool.VIEW && selectedAnnotationId == null)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isPanZoomEnabled) {
                                if (isPanZoomEnabled) {
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val oldScale = zoomScale
                                        val newScale = (zoomScale * zoom).coerceIn(0.75f, 6.0f)
                                        if (newScale != oldScale) {
                                            panOffset = (panOffset - centroid) * (newScale / oldScale) + centroid + pan
                                            zoomScale = newScale
                                        } else {
                                            panOffset += pan
                                        }
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (zoomScale > 1.2f) {
                                            zoomScale = 1.0f
                                            panOffset = Offset.Zero
                                        } else {
                                            val targetScale = 2.0f
                                            panOffset = (panOffset - tapOffset) * (targetScale / zoomScale) + tapOffset
                                            zoomScale = targetScale
                                        }
                                    },
                                    onTap = {
                                        // Tap empty space deselects
                                        selectedAnnotationId = null
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(2.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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

                                val pageModifier = if (fitPageMode) {
                                    if (pageAspect > containerAspect) {
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(pageAspect)
                                    } else {
                                        Modifier
                                            .fillMaxHeight()
                                            .aspectRatio(pageAspect)
                                    }
                                } else {
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(pageAspect)
                                }

                                // Interactive PDF Page Canvas Container
                                Box(
                                    modifier = pageModifier
                                        .background(Color.White)
                                        .shadow(8.dp)
                                        .onSizeChanged { size ->
                                            if (size.width > 0 && size.height > 0) {
                                                pageContainerSize = size
                                            }
                                        }
                                ) {
                                    // 1. Rendered PDF Page Background Image
                                    Image(
                                        bitmap = currentBmp.asImageBitmap(),
                                        contentDescription = "PDF Page ${currentPageIndex + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 2. Vector Drawing / Highlighter / Eraser Layer
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(currentTool, drawColor, strokeThickness, highlighterColor, highlighterWidth) {
                                                if (currentTool != EditorTool.DRAW && currentTool != EditorTool.HIGHLIGHTER && currentTool != EditorTool.ERASER) return@pointerInput

                                                awaitEachGesture {
                                                    val down = awaitFirstDown()
                                                    down.consume()
                                                    val startX = down.position.x
                                                    val startY = down.position.y

                                                    if (currentTool == EditorTool.ERASER) {
                                                        // ERASER LOGIC: Check intersection with existing strokes
                                                        fun eraseAt(x: Float, y: Float) {
                                                            val radiusSq = 32f * 32f
                                                            val toErase = editItems.filter { item ->
                                                                item.pageIndex == currentPageIndex &&
                                                                        (item.type == PdfEngine.AnnotationType.PEN || item.type == PdfEngine.AnnotationType.HIGHLIGHTER) &&
                                                                        item.points.any { (px, py) ->
                                                                            val dx = px - x
                                                                            val dy = py - y
                                                                            (dx * dx + dy * dy) <= radiusSq
                                                                        }
                                                            }
                                                            toErase.forEach { item ->
                                                                editItems.remove(item)
                                                                pushAction(EditHistoryAction.RemoveItem(item))
                                                            }
                                                        }

                                                        eraseAt(startX, startY)
                                                        do {
                                                            val event = awaitPointerEvent()
                                                            val change = event.changes.firstOrNull() ?: break
                                                            if (change.pressed) {
                                                                change.consume()
                                                                eraseAt(change.position.x, change.position.y)
                                                            }
                                                        } while (event.changes.any { it.pressed })

                                                    } else {
                                                        // DRAW / HIGHLIGHTER LOGIC
                                                        val points = mutableListOf(Pair(startX, startY))
                                                        activeStroke.clear()
                                                        activeStroke.add(Pair(startX, startY))

                                                        do {
                                                            val event = awaitPointerEvent()
                                                            val change = event.changes.firstOrNull() ?: break
                                                            if (change.pressed) {
                                                                change.consume()
                                                                points.add(Pair(change.position.x, change.position.y))
                                                                activeStroke.add(Pair(change.position.x, change.position.y))
                                                            }
                                                        } while (event.changes.any { it.pressed })

                                                        if (points.size > 1) {
                                                            val isHighlight = (currentTool == EditorTool.HIGHLIGHTER)
                                                            val newItem = PdfEngine.EditItem(
                                                                pageIndex = currentPageIndex,
                                                                type = if (isHighlight) PdfEngine.AnnotationType.HIGHLIGHTER else PdfEngine.AnnotationType.PEN,
                                                                colorHex = if (isHighlight) highlighterColor else drawColor,
                                                                strokeWidth = if (isHighlight) highlighterWidth else strokeThickness,
                                                                opacity = if (isHighlight) 0.40f else 1.0f,
                                                                points = points.toList(),
                                                                baseWidth = pageContainerSize.width.toFloat(),
                                                                baseHeight = pageContainerSize.height.toFloat()
                                                            )
                                                            editItems.add(newItem)
                                                            pushAction(EditHistoryAction.AddItem(newItem))
                                                            activeStroke.clear()
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        // Draw existing pen and highlighter strokes
                                        val pageDrawings = editItems.filter {
                                            it.pageIndex == currentPageIndex &&
                                                    (it.type == PdfEngine.AnnotationType.PEN || it.type == PdfEngine.AnnotationType.HIGHLIGHTER)
                                        }

                                        for (item in pageDrawings) {
                                            val pts = item.points
                                            if (pts.size >= 2) {
                                                val path = androidx.compose.ui.graphics.Path()
                                                path.moveTo(pts[0].first, pts[0].second)
                                                for (i in 1 until pts.size) {
                                                    path.lineTo(pts[i].first, pts[i].second)
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = Color(item.colorHex).copy(alpha = item.opacity),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                        width = item.strokeWidth.dp.toPx(),
                                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                                    )
                                                )
                                            }
                                        }

                                        // Draw active in-progress stroke
                                        if (activeStroke.size >= 2) {
                                            val isHighlight = (currentTool == EditorTool.HIGHLIGHTER)
                                            val path = androidx.compose.ui.graphics.Path()
                                            path.moveTo(activeStroke[0].first, activeStroke[0].second)
                                            for (i in 1 until activeStroke.size) {
                                                path.lineTo(activeStroke[i].first, activeStroke[i].second)
                                            }
                                            drawPath(
                                                path = path,
                                                color = Color(if (isHighlight) highlighterColor else drawColor).copy(
                                                    alpha = if (isHighlight) 0.40f else 1.0f
                                                ),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = (if (isHighlight) highlighterWidth else strokeThickness).dp.toPx(),
                                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }

                                    // 3. Interactive Draggable & Resizable Overlays for TEXT and IMAGE items
                                    val pageInteractiveItems = editItems.filter {
                                        it.pageIndex == currentPageIndex &&
                                                (it.type == PdfEngine.AnnotationType.TEXT || it.type == PdfEngine.AnnotationType.IMAGE)
                                    }

                                    for (item in pageInteractiveItems) {
                                        key(item.id) {
                                            if (item.type == PdfEngine.AnnotationType.TEXT) {
                                                InteractiveTextOverlay(
                                                    item = item,
                                                    isSelected = (selectedAnnotationId == item.id),
                                                    pageBounds = pageContainerSize,
                                                    onSelect = { selectedAnnotationId = item.id },
                                                    onPositionChange = { newX, newY ->
                                                        val idx = editItems.indexOfFirst { it.id == item.id }
                                                        if (idx != -1) {
                                                            val old = editItems[idx]
                                                            editItems[idx] = old.copy(posX = newX, posY = newY)
                                                        }
                                                    },
                                                    onSizeScale = { scale ->
                                                        val idx = editItems.indexOfFirst { it.id == item.id }
                                                        if (idx != -1) {
                                                            val old = editItems[idx]
                                                            val updated = (old.fontSize * scale).coerceIn(10f, 120f)
                                                            editItems[idx] = old.copy(fontSize = updated)
                                                        }
                                                    },
                                                    onDelete = {
                                                        editItems.remove(item)
                                                        if (selectedAnnotationId == item.id) selectedAnnotationId = null
                                                        pushAction(EditHistoryAction.RemoveItem(item))
                                                        Toast.makeText(context, "Text removed", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            } else if (item.type == PdfEngine.AnnotationType.IMAGE) {
                                                InteractiveImageOverlay(
                                                    item = item,
                                                    isSelected = (selectedAnnotationId == item.id),
                                                    pageBounds = pageContainerSize,
                                                    onSelect = { selectedAnnotationId = item.id },
                                                    onPositionChange = { newX, newY ->
                                                        val idx = editItems.indexOfFirst { it.id == item.id }
                                                        if (idx != -1) {
                                                            val old = editItems[idx]
                                                            editItems[idx] = old.copy(posX = newX, posY = newY)
                                                        }
                                                    },
                                                    onDimensionScale = { scale ->
                                                        val idx = editItems.indexOfFirst { it.id == item.id }
                                                        if (idx != -1) {
                                                            val old = editItems[idx]
                                                            val nw = (old.imageWidth * scale).coerceIn(40f, 800f)
                                                            val nh = (old.imageHeight * scale).coerceIn(40f, 800f)
                                                            editItems[idx] = old.copy(imageWidth = nw, imageHeight = nh)
                                                        }
                                                    },
                                                    onRotate = {
                                                        val idx = editItems.indexOfFirst { it.id == item.id }
                                                        if (idx != -1) {
                                                            val old = editItems[idx]
                                                            editItems[idx] = old.copy(imageRotation = (old.imageRotation + 90f) % 360f)
                                                        }
                                                    },
                                                    onDelete = {
                                                        editItems.remove(item)
                                                        if (selectedAnnotationId == item.id) selectedAnnotationId = null
                                                        pushAction(EditHistoryAction.RemoveItem(item))
                                                        Toast.makeText(context, "Image removed", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ====================================================
                        // RELOCATED "Zoom In" and "Fit Width" BUTTONS:
                        // Positioned at the TOP AREA of the PDF preview container
                        // ====================================================
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .testTag("top_viewport_controls")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                // Zoom In / Zoom Out button
                                FilledTonalButton(
                                    onClick = {
                                        zoomScale = if (zoomScale > 1.2f) 1.0f else 2.0f
                                        panOffset = Offset.Zero
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF334155),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("btn_zoom_in_top")
                                ) {
                                    Icon(
                                        imageVector = if (zoomScale > 1.2f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                                        contentDescription = "Zoom In",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (zoomScale > 1.2f) "Zoom Out" else "Zoom In",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Fit Width / Fit Page button
                                FilledTonalButton(
                                    onClick = { fitPageMode = !fitPageMode },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF334155),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("btn_fit_width_top")
                                ) {
                                    Icon(
                                        imageVector = if (fitPageMode) Icons.Default.AspectRatio else Icons.Default.FitScreen,
                                        contentDescription = "Fit Width",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (fitPageMode) "Fit Width" else "Fit Page",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Page navigation stepper in top pill
                                IconButton(
                                    onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                    enabled = currentPageIndex > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NavigateBefore,
                                        contentDescription = "Previous Page",
                                        tint = if (currentPageIndex > 0) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = "${currentPageIndex + 1}/$totalPages",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { showThumbnailTray = !showThumbnailTray }
                                        .padding(horizontal = 2.dp)
                                )

                                IconButton(
                                    onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                                    enabled = currentPageIndex < totalPages - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NavigateNext,
                                        contentDescription = "Next Page",
                                        tint = if (currentPageIndex < totalPages - 1) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // ====================================================
                        // HIGHLIGHTER TOUCH CUSTOMIZER WIDGET
                        // Drag to enlarge/shrink highlighter width freely with touch
                        // ====================================================
                        if (currentTool == EditorTool.HIGHLIGHTER) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 12.dp)
                            ) {
                                HighlighterTouchCustomizer(
                                    currentWidth = highlighterWidth,
                                    currentColor = highlighterColor,
                                    onWidthChange = { highlighterWidth = it },
                                    onColorChange = { highlighterColor = it }
                                )
                            }
                        }

                        // ====================================================
                        // PEN COLOR / STROKE CUSTOMIZER WIDGET
                        // ====================================================
                        if (currentTool == EditorTool.DRAW) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 12.dp)
                            ) {
                                PenTouchCustomizer(
                                    currentThickness = strokeThickness,
                                    currentColor = drawColor,
                                    onThicknessChange = { strokeThickness = it },
                                    onColorChange = { drawColor = it }
                                )
                            }
                        }

                        // ====================================================
                        // ERASER MODE FEEDBACK BANNER
                        // ====================================================
                        if (currentTool == EditorTool.ERASER) {
                            Surface(
                                color = CrimsonPrimary.copy(alpha = 0.90f),
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 52.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AutoFixNormal, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Eraser Active: Drag or tap paths to delete", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                        }

                        // ====================================================
                        // SIMPLIFIED FLOATING EDITING TOOLS (BOTTOM PALETTE)
                        // Old bloated bottom toolbar with "Page, Edit, Text, Image, Draw, More" is REMOVED!
                        // Direct tools: View, Text, Image, Draw, Highlight, Eraser
                        // ====================================================
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.95f),
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .testTag("floating_editor_tools")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                EditorToolChip(
                                    label = "View",
                                    icon = Icons.Default.PanTool,
                                    isSelected = currentTool == EditorTool.VIEW,
                                    onClick = {
                                        currentTool = EditorTool.VIEW
                                        selectedAnnotationId = null
                                    }
                                )

                                EditorToolChip(
                                    label = "Text",
                                    icon = Icons.Default.TextFields,
                                    isSelected = currentTool == EditorTool.TEXT,
                                    onClick = {
                                        currentTool = EditorTool.TEXT
                                        textInputContent = ""
                                        showAddTextDialog = true
                                    }
                                )

                                EditorToolChip(
                                    label = "Image",
                                    icon = Icons.Default.Image,
                                    isSelected = currentTool == EditorTool.IMAGE,
                                    onClick = {
                                        currentTool = EditorTool.IMAGE
                                        isReplacingImage = false
                                        imagePickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )

                                EditorToolChip(
                                    label = "Draw",
                                    icon = Icons.Default.Draw,
                                    isSelected = currentTool == EditorTool.DRAW,
                                    onClick = {
                                        currentTool = EditorTool.DRAW
                                        selectedAnnotationId = null
                                    }
                                )

                                EditorToolChip(
                                    label = "Highlight",
                                    icon = Icons.Default.Highlight,
                                    isSelected = currentTool == EditorTool.HIGHLIGHTER,
                                    onClick = {
                                        currentTool = EditorTool.HIGHLIGHTER
                                        selectedAnnotationId = null
                                    }
                                )

                                EditorToolChip(
                                    label = "Eraser",
                                    icon = Icons.Default.AutoFixNormal,
                                    isSelected = currentTool == EditorTool.ERASER,
                                    onClick = {
                                        currentTool = EditorTool.ERASER
                                        selectedAnnotationId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====================================================
        // EXPANDABLE THUMBNAIL TRAY
        // ====================================================
        if (showThumbnailTray && pages.isNotEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shadowElevation = 8.dp
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(pages) { index, bmp ->
                        val isSelected = (index == currentPageIndex)
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
                                    .height(52.dp)
                                    .width(40.dp)
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
    }

    // ====================================================
    // TEXT INSERTION DIALOG
    // Supporting Bengali, English, and Handwriting styles + Color & Size
    // ====================================================
    if (showAddTextDialog) {
        val fontChoices = listOf(
            FontOption("BENGALI", "বাংলা (Bengali)", "বাংলা লিখুন"),
            FontOption("DEFAULT", "English (Sans)", "Modern Sans"),
            FontOption("SERIF", "English (Serif)", "Formal Serif", FontFamily.Serif),
            FontOption("HANDWRITING", "Handwriting ✍", "Cursive Style", FontFamily.Cursive),
            FontOption("MONOSPACE", "Monospace", "Clean Mono", FontFamily.Monospace)
        )

        val quickColors = listOf(
            AndroidColor.BLACK to "Black",
            AndroidColor.rgb(220, 38, 38) to "Red",
            AndroidColor.rgb(37, 99, 235) to "Blue",
            AndroidColor.rgb(22, 163, 74) to "Green",
            AndroidColor.rgb(147, 51, 234) to "Purple",
            AndroidColor.rgb(234, 88, 12) to "Orange"
        )

        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = {
                Text(
                    text = "Add Text to Page",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = textInputContent,
                        onValueChange = { textInputContent = it },
                        placeholder = { Text("এখানে লিখুন / Type text here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_add_text_field"),
                        minLines = 2,
                        maxLines = 4
                    )

                    // FONT SELECTION (Bengali, English, Handwriting)
                    Text(
                        text = "Font Style / ফন্ট স্টাইল:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        fontChoices.forEach { font ->
                            val isChosen = (textFontFamily == font.id)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChosen) CrimsonPrimary else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isChosen) CrimsonPrimary else Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable { textFontFamily = font.id }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = font.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChosen) Color.White else Color(0xFF1E293B),
                                        fontFamily = font.fontFamily
                                    )
                                    Text(
                                        text = font.previewText,
                                        fontSize = 10.sp,
                                        color = if (isChosen) Color.White.copy(alpha = 0.85f) else Color(0xFF64748B),
                                        fontFamily = font.fontFamily
                                    )
                                }
                            }
                        }
                    }

                    // COLOR SELECTION
                    Text(text = "Color / রং:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickColors.forEach { (c, _) ->
                            val isSelected = (textColor == c)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) CrimsonPrimary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { textColor = c }
                            )
                        }
                    }

                    // SIZE & STYLES (BOLD / ITALIC)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Size: ${textFontSize.toInt()}sp", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { textFontSize = (textFontSize - 4f).coerceAtLeast(12f) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Smaller", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { textFontSize = (textFontSize + 4f).coerceAtMost(72f) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Larger", modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalIconToggleButton(
                                checked = textIsBold,
                                onCheckedChange = { textIsBold = it },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(16.dp))
                            }
                            FilledTonalIconToggleButton(
                                checked = textIsItalic,
                                onCheckedChange = { textIsItalic = it },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInputContent.isNotBlank()) {
                            // Place at center of page so it is IMMEDIATELY 100% VISIBLE!
                            val targetX = ((pageContainerSize.width - 240f) / 2f).coerceAtLeast(40f)
                            val targetY = ((pageContainerSize.height - 80f) / 2f).coerceAtLeast(60f)

                            val newItem = PdfEngine.EditItem(
                                pageIndex = currentPageIndex,
                                type = PdfEngine.AnnotationType.TEXT,
                                text = textInputContent.trim(),
                                colorHex = textColor,
                                fontSize = textFontSize,
                                isBold = textIsBold,
                                isItalic = textIsItalic,
                                fontFamily = textFontFamily,
                                posX = targetX,
                                posY = targetY,
                                baseWidth = pageContainerSize.width.toFloat(),
                                baseHeight = pageContainerSize.height.toFloat()
                            )
                            editItems.add(newItem)
                            selectedAnnotationId = newItem.id
                            pushAction(EditHistoryAction.AddItem(newItem))
                            currentTool = EditorTool.VIEW
                            Toast.makeText(context, "Text added! Drag & resize freely.", Toast.LENGTH_SHORT).show()
                        }
                        showAddTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("btn_confirm_add_text")
                ) {
                    Text("Add to Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}

/**
 * Interactive Draggable & Resizable Text Overlay
 * Fixes: Invisible text bug, free resizing with touch gestures, free dragging
 */
@Composable
fun InteractiveTextOverlay(
    item: PdfEngine.EditItem,
    isSelected: Boolean,
    pageBounds: IntSize,
    onSelect: () -> Unit,
    onPositionChange: (newX: Float, newY: Float) -> Unit,
    onSizeScale: (scale: Float) -> Unit,
    onDelete: () -> Unit
) {
    var posX by remember(item.posX) { mutableFloatStateOf(item.posX) }
    var posY by remember(item.posY) { mutableFloatStateOf(item.posY) }

    val fontChoice = when (item.fontFamily) {
        "SERIF" -> FontFamily.Serif
        "HANDWRITING", "CURSIVE" -> FontFamily.Cursive
        "MONOSPACE" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
            .pointerInput(item.id) {
                // Free dragging across the canvas
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val maxW = (pageBounds.width - 40).coerceAtLeast(100).toFloat()
                        val maxH = (pageBounds.height - 40).coerceAtLeast(100).toFloat()
                        posX = (posX + dragAmount.x).coerceIn(0f, maxW)
                        posY = (posY + dragAmount.y).coerceIn(0f, maxH)
                        onPositionChange(posX, posY)
                    }
                )
            }
            .pointerInput(item.id) {
                // Pinch to resize font size with touch gestures
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1.0f) {
                        onSizeScale(zoom)
                    }
                }
            }
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier
                        .border(
                            BorderStroke(1.5.dp, ToolBlue),
                            RoundedCornerShape(6.dp)
                        )
                        .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                }
            )
    ) {
        Column {
            // Selected toolbar with Resize stepper & Delete button
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    // Shrink
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF334155),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onSizeScale(0.85f) }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Shrink", tint = Color.White, modifier = Modifier.padding(3.dp))
                    }

                    // Enlarge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF334155),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onSizeScale(1.15f) }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Enlarge", tint = Color.White, modifier = Modifier.padding(3.dp))
                    }

                    // Delete
                    Surface(
                        shape = CircleShape,
                        color = CrimsonPrimary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onDelete() }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(3.dp))
                    }
                }
            }

            // Visible Text Content
            Text(
                text = item.text,
                color = Color(item.colorHex),
                fontSize = item.fontSize.sp,
                fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (item.isItalic) FontStyle.Italic else FontStyle.Normal,
                fontFamily = fontChoice
            )
        }
    }
}

/**
 * Interactive Draggable & Resizable Image Overlay
 * Fixes: Fixed positioning of added images, allowing free dragging and resizing
 */
@Composable
fun InteractiveImageOverlay(
    item: PdfEngine.EditItem,
    isSelected: Boolean,
    pageBounds: IntSize,
    onSelect: () -> Unit,
    onPositionChange: (newX: Float, newY: Float) -> Unit,
    onDimensionScale: (scale: Float) -> Unit,
    onRotate: () -> Unit,
    onDelete: () -> Unit
) {
    var posX by remember(item.posX) { mutableFloatStateOf(item.posX) }
    var posY by remember(item.posY) { mutableFloatStateOf(item.posY) }
    val bmp = item.imageBitmap ?: return

    Box(
        modifier = Modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
            .size(item.imageWidth.dp, item.imageHeight.dp)
            .pointerInput(item.id) {
                // Free dragging
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val maxW = (pageBounds.width - 30).coerceAtLeast(60).toFloat()
                        val maxH = (pageBounds.height - 30).coerceAtLeast(60).toFloat()
                        posX = (posX + dragAmount.x).coerceIn(0f, maxW)
                        posY = (posY + dragAmount.y).coerceIn(0f, maxH)
                        onPositionChange(posX, posY)
                    }
                )
            }
            .pointerInput(item.id) {
                // Free resizing with pinch touch gesture
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1.0f) {
                        onDimensionScale(zoom)
                    }
                }
            }
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier
                        .border(BorderStroke(2.dp, ToolBlue), RoundedCornerShape(4.dp))
                        .padding(2.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Added Image",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = item.imageRotation
                }
        )

        // Action controls when selected (Rotate, Resize handles, Delete)
        if (isSelected) {
            // Delete button at top-left
            Surface(
                shape = CircleShape,
                color = CrimsonPrimary,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.TopStart)
                    .clickable { onDelete() }
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete Image", tint = Color.White, modifier = Modifier.padding(4.dp))
            }

            // Rotate button at top-right
            Surface(
                shape = CircleShape,
                color = Color(0xFF334155),
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onRotate() }
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate Image", tint = Color.White, modifier = Modifier.padding(4.dp))
            }

            // Drag-Resize handle at bottom-right
            Surface(
                shape = CircleShape,
                color = ToolBlue,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.BottomEnd)
                    .pointerInput(item.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = (dragAmount.x + dragAmount.y) / 100f
                            val factor = (1f + delta).coerceIn(0.7f, 1.3f)
                            onDimensionScale(factor)
                        }
                    }
            ) {
                Icon(Icons.Default.OpenInFull, contentDescription = "Resize Handle", tint = Color.White, modifier = Modifier.padding(4.dp))
            }
        }
    }
}

/**
 * Highlighter Touch Customizer Widget
 * Allows users to freely customize highlighter size using touch gestures (drag to enlarge/shrink)
 */
@Composable
fun HighlighterTouchCustomizer(
    currentWidth: Float,
    currentColor: Int,
    onWidthChange: (Float) -> Unit,
    onColorChange: (Int) -> Unit
) {
    val highlighterColors = listOf(
        AndroidColor.rgb(250, 204, 21) to "Yellow",
        AndroidColor.rgb(34, 197, 94) to "Green",
        AndroidColor.rgb(56, 189, 248) to "Blue",
        AndroidColor.rgb(244, 114, 182) to "Pink",
        AndroidColor.rgb(251, 146, 60) to "Orange"
    )

    Surface(
        color = Color(0xFF1E293B).copy(alpha = 0.94f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.width(54.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
        ) {
            Text(
                text = "SIZE",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            // Live touch preview & drag vertical handle to enlarge/shrink
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF334155))
                    .pointerInput(Unit) {
                        // Drag up = enlarge, Drag down = shrink
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount.y * 0.4f
                            val updated = (currentWidth + delta).coerceIn(10f, 65f)
                            onWidthChange(updated)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(currentWidth.coerceIn(8f, 38f).dp)
                        .clip(CircleShape)
                        .background(Color(currentColor).copy(alpha = 0.55f))
                )
            }

            Text(
                text = "${currentWidth.toInt()}px",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Divider(color = Color(0xFF475569), thickness = 1.dp)

            // Color dots
            highlighterColors.forEach { (colorInt, _) ->
                val isSel = (currentColor == colorInt)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            width = if (isSel) 2.dp else 0.dp,
                            color = if (isSel) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(colorInt) }
                )
            }
        }
    }
}

/**
 * Pen Touch Customizer Widget
 */
@Composable
fun PenTouchCustomizer(
    currentThickness: Float,
    currentColor: Int,
    onThicknessChange: (Float) -> Unit,
    onColorChange: (Int) -> Unit
) {
    val penColors = listOf(
        AndroidColor.rgb(220, 38, 38) to "Red",
        AndroidColor.BLACK to "Black",
        AndroidColor.rgb(37, 99, 235) to "Blue",
        AndroidColor.rgb(22, 163, 74) to "Green"
    )

    Surface(
        color = Color(0xFF1E293B).copy(alpha = 0.94f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.width(50.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
        ) {
            Text(
                text = "PEN",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            // Live touch preview & drag vertical handle to enlarge/shrink
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF334155))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount.y * 0.15f
                            val updated = (currentThickness + delta).coerceIn(2f, 24f)
                            onThicknessChange(updated)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(currentThickness.coerceIn(3f, 24f).dp)
                        .clip(CircleShape)
                        .background(Color(currentColor))
                )
            }

            Text(
                text = "${currentThickness.toInt()}px",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Divider(color = Color(0xFF475569), thickness = 1.dp)

            // Color dots
            penColors.forEach { (colorInt, _) ->
                val isSel = (currentColor == colorInt)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            width = if (isSel) 2.dp else 0.dp,
                            color = if (isSel) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(colorInt) }
                )
            }
        }
    }
}

/**
 * Tool chip button in bottom floating bar
 */
@Composable
fun EditorToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) CrimsonPrimary else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
