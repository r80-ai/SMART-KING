package com.example.ui.screens

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import com.example.storage.StoragePermissionHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Rect as AndroidRect
import android.graphics.Typeface as AndroidTypeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import kotlin.math.roundToInt
import com.example.model.PdfFileRecord
import com.example.pdf.PdfEngine
import com.example.pdf.PdfToJpgConversionResult
import com.example.pdf.PdfToJpgConverter
import com.example.storage.GlobalSaveManager
import com.example.ui.PdfMasterViewModel
import com.example.ui.ActiveModal
import com.example.photo.birefnet.BiRefNetLiteEngine
import com.example.ui.dialogs.PdfImageBgRemovalChoiceDialog
import com.example.ui.dialogs.PdfImageBgRemovalProgressDialog
import com.example.ui.dialogs.PdfImageManualEraserDialog
import com.example.ui.dialogs.WorkspaceCropDialog
import com.example.ui.dialogs.WorkspacePageNumbersDialog
import com.example.ui.dialogs.WorkspacePagesSheet
import com.example.ui.dialogs.WorkspaceWatermarkDialog
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

enum class EditorTopTab {
    NONE, PAGE, EDIT, TEXT, IMAGE, PEN, MORE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorMainView(
    fileRecord: PdfFileRecord,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentFile by remember(fileRecord) { mutableStateOf(fileRecord) }
    val allFiles by viewModel.allFiles.collectAsState()
    val pages by viewModel.renderedPages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingMessage by viewModel.processingMessage.collectAsState()

    val showViewerPasswordDialog by viewModel.showViewerPasswordDialog.collectAsState()
    val viewerPasswordError by viewModel.viewerPasswordError.collectAsState()
    var viewerPasswordInput by remember { mutableStateOf("") }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var activeTab by remember { mutableStateOf(EditorTopTab.NONE) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var fitPageMode by remember { mutableStateOf(true) }
    var showThumbnailTray by remember { mutableStateOf(false) }

    // Edit history & accumulated items
    val editItems = remember { mutableStateListOf<PdfEngine.EditItem>() }
    val deletedPages = remember { mutableStateListOf<Int>() }
    val pageRotations = remember { mutableStateMapOf<Int, Int>() }
    var selectedAnnotationId by remember { mutableStateOf<String?>(null) }
    var pageBackgroundColor by remember { mutableStateOf(Color.White) }
    var pageContentScale by remember { mutableFloatStateOf(1.0f) }

    fun getScaledPageBitmap(original: Bitmap, scale: Float): Bitmap {
        if (scale == 1.0f) return original
        val w = original.width
        val h = original.height
        val scaled = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(scaled)
        canvas.drawColor(android.graphics.Color.WHITE)
        val matrix = android.graphics.Matrix().apply {
            postScale(scale, scale, w / 2f, h / 2f)
        }
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }
        canvas.drawBitmap(original, matrix, paint)
        return scaled
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

    // Text tool state
    var showAddTextDialog by remember { mutableStateOf(false) }
    var textInputContent by remember { mutableStateOf("") }
    var textColor by remember { mutableIntStateOf(AndroidColor.RED) }
    var textFontSize by remember { mutableFloatStateOf(28f) }
    var textIsBold by remember { mutableStateOf(false) }
    var textIsItalic by remember { mutableStateOf(false) }
    var textAlignment by remember { mutableStateOf("LEFT") }
    var showLayerManagerDialog by remember { mutableStateOf(false) }
    var pdfCanvasWidthPx by remember { mutableFloatStateOf(800f) }
    var pdfCanvasHeightPx by remember { mutableFloatStateOf(1100f) }
    var textLayerUpdateTrigger by remember { mutableIntStateOf(0) }
    var textOverlayViewRef by remember { mutableStateOf<TextOverlayView?>(null) }

    // Pen & Annotation tool state
    var penSubTool by remember { mutableStateOf(AnnotationCanvasOverlayView.ToolMode.PEN) }
    var penColor by remember { mutableIntStateOf(AndroidColor.rgb(37, 99, 235)) }
    var penStrokeThickness by remember { mutableFloatStateOf(4f) }
    var penOpacity by remember { mutableFloatStateOf(1.0f) }

    var highlightColor by remember { mutableIntStateOf(AndroidColor.rgb(250, 204, 21)) }
    var highlightWidth by remember { mutableFloatStateOf(20f) }
    var highlightOpacity by remember { mutableFloatStateOf(0.40f) }

    var shapeType by remember { mutableStateOf(AnnotationCanvasOverlayView.ShapeType.RECTANGLE) }
    var shapeColor by remember { mutableIntStateOf(AndroidColor.rgb(220, 38, 38)) }
    var shapeStrokeWidth by remember { mutableFloatStateOf(3.5f) }
    var shapeFillColor by remember { mutableStateOf<Int?>(null) }

    var eraserSize by remember { mutableStateOf(AnnotationCanvasOverlayView.EraserSize.MEDIUM) }
    var eraserType by remember { mutableStateOf(AnnotationCanvasOverlayView.EraserType.STROKE) }
    var annotationCanvasRef by remember { mutableStateOf<AnnotationCanvasOverlayView?>(null) }

    // Sub-dialogs state
    var showPagesSheet by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showWatermarkWorkspaceDialog by remember { mutableStateOf(false) }
    var showPageNumbersWorkspaceDialog by remember { mutableStateOf(false) }
    var showChangePageSizeDialog by remember { mutableStateOf(false) }
    var showResizePagesDialog by remember { mutableStateOf(false) }
    var showMarginsDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showMergePdfDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var saveAsNameInput by remember { mutableStateOf("${currentFile.nameWithoutExtension()}_edited.pdf") }
    var showStoragePermissionRationaleDialog by remember { mutableStateOf(false) }
    var pendingSaveFileName by remember { mutableStateOf<String?>(null) }
    var pendingSaveSuccessCallback by remember { mutableStateOf<((PdfFileRecord) -> Unit)?>(null) }
    var showBgRemovalChoiceDialog by remember { mutableStateOf(false) }
    var isRemovingBackground by remember { mutableStateOf(false) }
    var showManualEraserDialog by remember { mutableStateOf(false) }
    var manualEraserTargetItem by remember { mutableStateOf<PdfEngine.EditItem?>(null) }

    val requestSavePdf: (String, ((PdfFileRecord) -> Unit)?) -> Unit = { outputName, onComplete ->
        val hasFullPerm = StoragePermissionHelper.hasFullStoragePermission(context)
        val isRootWritable = StoragePermissionHelper.isRootExternalFolderWritable(context)
        if (!hasFullPerm && !isRootWritable) {
            pendingSaveFileName = outputName
            pendingSaveSuccessCallback = onComplete
            showStoragePermissionRationaleDialog = true
        } else {
            viewModel.saveEditedPdf(
                sourceRecord = currentFile,
                items = editItems.toList(),
                deletedPages = deletedPages.toSet(),
                pageRotations = pageRotations.toMap(),
                outputName = outputName,
                pageScale = pageContentScale
            ) { newRec ->
                currentFile = newRec
                onComplete?.invoke(newRec)
            }
        }
    }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var isProtectAction by remember { mutableStateOf(true) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    var showJpgExportDialog by remember { mutableStateOf(false) }
    var exportedJpgResult by remember { mutableStateOf<PdfToJpgConversionResult?>(null) }
    var exportedJpgLocation by remember { mutableStateOf("") }

    val handleConvertToImages: () -> Unit = {
        coroutineScope.launch {
            try {
                val res = PdfToJpgConverter.convertPdfToJpg(context, File(currentFile.filePath))
                var lastLoc = "Documents / PDF_Master"
                for (page in res.pages) {
                    val saveRes = GlobalSaveManager.saveProcessedFile(
                        context = context,
                        tempSourceFile = page.file,
                        desiredFileName = page.fileName,
                        mimeType = "image/jpeg",
                        category = "created",
                        tag = "PDF to JPG"
                    )
                    page.savedUri = saveRes.savedUri
                    page.isSaved = true
                    lastLoc = saveRes.displayLocation
                    viewModel.addRecentFile(saveRes.fileRecord)
                }
                exportedJpgResult = res
                exportedJpgLocation = lastLoc
                showJpgExportDialog = true
            } catch (e: Exception) {
                Toast.makeText(context, "Conversion failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // SAF Document Export Launcher
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            viewModel.exportFileToExternalUri(currentFile, uri)
        }
    }

    // Ensure pages are loaded whenever this file is opened or refreshed
    LaunchedEffect(currentFile.filePath, currentFile.uriString) {
        viewModel.loadPagesForViewer(currentFile, forceReload = true)
    }

    // Clamp page index when pages list updates
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
                                if (isReplacingImage && selectedAnnotationId != null) {
                                    val idx = editItems.indexOfFirst { it.id == selectedAnnotationId }
                                    if (idx != -1) {
                                        pushHistory()
                                        val old = editItems[idx]
                                        editItems[idx] = old.copy(
                                            imageBitmap = bmp,
                                            originalImageBitmap = bmp
                                        )
                                        Toast.makeText(context, "Image replaced", Toast.LENGTH_SHORT).show()
                                    }
                                    isReplacingImage = false
                                } else {
                                    pushHistory()
                                    val pageBmp = pages.getOrNull(currentPageIndex)
                                    val pageW = pageBmp?.width?.toFloat() ?: 600f
                                    val pageH = pageBmp?.height?.toFloat() ?: 800f
                                    val initW = (pageW * 0.45f).coerceIn(120f, 500f)
                                    val initH = (initW * bmp.height / bmp.width.coerceAtLeast(1)).coerceAtLeast(40f)
                                    val initX = ((pageW - initW) / 2f).coerceAtLeast(0f)
                                    val initY = ((pageH - initH) / 3f).coerceAtLeast(0f)

                                    val newItem = PdfEngine.EditItem(
                                        pageIndex = currentPageIndex,
                                        type = PdfEngine.AnnotationType.IMAGE,
                                        imageBitmap = bmp,
                                        originalImageBitmap = bmp,
                                        posX = initX,
                                        posY = initY,
                                        imageWidth = initW,
                                        imageHeight = initH,
                                        baseWidth = pageW,
                                        baseHeight = pageH
                                    )
                                    editItems.add(newItem)
                                    selectedAnnotationId = newItem.id
                                    activeTab = EditorTopTab.IMAGE
                                    Toast.makeText(context, "Image added! Drag and pinch to reposition & resize", Toast.LENGTH_SHORT).show()
                                }
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
        if (showThumbnailTray) {
            showThumbnailTray = false
        } else if (activeTab != EditorTopTab.NONE) {
            activeTab = EditorTopTab.NONE
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("pdf_editor_main_view")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // ====================================================
        // 1. TOP APP BAR: [←] Document.pdf [Share] [Save] [⋮]
        // ====================================================
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
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

                // Center: Toolbar tools (Page, Edit, Text, Image, Draw, More)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorToolbarTabItem(
                        label = "Page",
                        icon = Icons.Default.Description,
                        isSelected = activeTab == EditorTopTab.PAGE,
                        onClick = { activeTab = if (activeTab == EditorTopTab.PAGE) EditorTopTab.NONE else EditorTopTab.PAGE }
                    )
                    EditorToolbarTabItem(
                        label = "Edit",
                        icon = Icons.Default.Tune,
                        isSelected = activeTab == EditorTopTab.EDIT,
                        onClick = { activeTab = if (activeTab == EditorTopTab.EDIT) EditorTopTab.NONE else EditorTopTab.EDIT }
                    )
                    EditorToolbarTabItem(
                        label = "Text",
                        icon = Icons.Default.TextFields,
                        isSelected = activeTab == EditorTopTab.TEXT,
                        onClick = { activeTab = if (activeTab == EditorTopTab.TEXT) EditorTopTab.NONE else EditorTopTab.TEXT }
                    )
                    EditorToolbarTabItem(
                        label = "Image",
                        icon = Icons.Default.Image,
                        isSelected = activeTab == EditorTopTab.IMAGE,
                        onClick = { activeTab = if (activeTab == EditorTopTab.IMAGE) EditorTopTab.NONE else EditorTopTab.IMAGE }
                    )
                    EditorToolbarTabItem(
                        label = "Pen",
                        icon = Icons.Default.Edit,
                        isSelected = activeTab == EditorTopTab.PEN,
                        onClick = { activeTab = if (activeTab == EditorTopTab.PEN) EditorTopTab.NONE else EditorTopTab.PEN }
                    )
                    EditorToolbarTabItem(
                        label = "More",
                        icon = Icons.Default.MoreHoriz,
                        isSelected = activeTab == EditorTopTab.MORE,
                        onClick = { activeTab = if (activeTab == EditorTopTab.MORE) EditorTopTab.NONE else EditorTopTab.MORE }
                    )
                }

                // Right side: Layers button and 3-dots overflow menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showLayerManagerDialog = true },
                        modifier = Modifier.testTag("editor_top_layers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Layers",
                            tint = if (editItems.any { it.pageIndex == currentPageIndex }) Color(0xFF60A5FA) else Color.White
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { moreMenuExpanded = true },
                            modifier = Modifier.testTag("editor_top_more_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            // Layers Option
                            DropdownMenuItem(
                                text = { Text("Layers") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA)
                                    )
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    showLayerManagerDialog = true
                                },
                                modifier = Modifier.testTag("menu_item_layers")
                            )

                            // Save Option
                            DropdownMenuItem(
                            text = { Text("Save") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = CrimsonPrimary
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                val outputName = "${currentFile.nameWithoutExtension()}_edited.pdf"
                                requestSavePdf(outputName) { newRec ->
                                    Toast.makeText(context, "Saved as $outputName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("menu_item_save")
                        )

                        // Save As Option
                        DropdownMenuItem(
                            text = { Text("Save As...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SaveAs,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                saveAsNameInput = "${currentFile.nameWithoutExtension()}_edited.pdf"
                                showSaveAsDialog = true
                            },
                            modifier = Modifier.testTag("menu_item_save_as")
                        )

                        // Share Option
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                viewModel.shareFile(context, currentFile)
                            },
                            modifier = Modifier.testTag("menu_item_share")
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Undo Option
                        DropdownMenuItem(
                            text = { Text("Undo") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8)
                                )
                            },
                            enabled = undoStack.isNotEmpty(),
                            onClick = {
                                moreMenuExpanded = false
                                handleUndo()
                            },
                            modifier = Modifier.testTag("menu_item_undo")
                        )

                        // Redo Option
                        DropdownMenuItem(
                            text = { Text("Redo") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = null,
                                    tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8)
                                )
                            },
                            enabled = redoStack.isNotEmpty(),
                            onClick = {
                                moreMenuExpanded = false
                                handleRedo()
                            },
                            modifier = Modifier.testTag("menu_item_redo")
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Export to Device
                        DropdownMenuItem(
                            text = { Text("Export to Device") },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                exportDocumentLauncher.launch(currentFile.name)
                            },
                            modifier = Modifier.testTag("menu_item_export_device")
                        )

                        // Export to JPG Images
                        DropdownMenuItem(
                            text = { Text("Export to JPG Images") },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                handleConvertToImages()
                            },
                            modifier = Modifier.testTag("menu_item_export_jpg")
                        )

                        // Compress PDF
                        DropdownMenuItem(
                            text = { Text("Compress PDF") },
                            leadingIcon = { Icon(Icons.Default.Compress, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                viewModel.openModal(ActiveModal.COMPRESS, currentFile)
                            },
                            modifier = Modifier.testTag("menu_item_compress")
                        )

                        // Document Info
                        DropdownMenuItem(
                            text = { Text("Document Info") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                Toast.makeText(
                                    context,
                                    "${currentFile.name}\nPages: $totalPages\nSize: ${(currentFile.fileSize / 1024).coerceAtLeast(1)} KB",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier.testTag("menu_item_doc_info")
                        )
                    }
                }
            }
        }
    }

        // ====================================================
        // 3. SUB-OPTIONS PANEL (When a top tab is active)
        // ====================================================
        AnimatedVisibility(visible = activeTab != EditorTopTab.NONE) {
            Surface(
                color = Color(0xFF1B2333),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 6.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeTab) {
                        EditorTopTab.PAGE -> {
                            PageOptionsSection(
                                onReorderPages = { showPagesSheet = true },
                                onRotatePage = {
                                    val cur = pageRotations[currentPageIndex] ?: 0
                                    pageRotations[currentPageIndex] = (cur + 90) % 360
                                    Toast.makeText(context, "Page ${currentPageIndex + 1} rotated +90°", Toast.LENGTH_SHORT).show()
                                },
                                onDeletePage = {
                                    if (totalPages > 1) {
                                        deletedPages.add(currentPageIndex)
                                        Toast.makeText(context, "Page ${currentPageIndex + 1} marked for deletion", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Cannot delete the only page in the document", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDuplicatePage = {
                                    showPagesSheet = true
                                },
                                onInsertBlankPage = {
                                    showPagesSheet = true
                                },
                                onExtractPage = {
                                    val sanitized = "${currentFile.nameWithoutExtension()}_p${currentPageIndex + 1}.pdf"
                                    viewModel.splitPdf(currentFile, listOf(currentPageIndex), sanitized, "Extract Page", "Single page extracted") {
                                        Toast.makeText(context, "Page ${currentPageIndex + 1} extracted successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onSplitPdf = {
                                    val outName = "${currentFile.nameWithoutExtension()}_split.pdf"
                                    viewModel.splitPdf(currentFile, listOf(currentPageIndex), outName)
                                    Toast.makeText(context, "Splitting PDF...", Toast.LENGTH_SHORT).show()
                                },
                                onMergePdf = {
                                    showMergePdfDialog = true
                                },
                                onSelectAll = {
                                    Toast.makeText(context, "All $totalPages pages selected for batch actions", Toast.LENGTH_SHORT).show()
                                },
                                onApplyChanges = {
                                    val outputName = "${currentFile.nameWithoutExtension()}_edited.pdf"
                                    requestSavePdf(outputName) { newRec ->
                                        deletedPages.clear()
                                        Toast.makeText(context, "Page changes applied!", Toast.LENGTH_SHORT).show()
                                    }
                                    activeTab = EditorTopTab.NONE
                                }
                            )
                        }

                        EditorTopTab.EDIT -> {
                            EditOptionsSection(
                                onCrop = { showCropDialog = true },
                                onResize = { showResizePagesDialog = true },
                                onChangePageSize = { showChangePageSizeDialog = true },
                                onMargins = { showMarginsDialog = true },
                                onWatermark = { showWatermarkWorkspaceDialog = true },
                                onBackground = { showBackgroundDialog = true },
                                onApplyChanges = {
                                    val outputName = "${currentFile.nameWithoutExtension()}_edited.pdf"
                                    requestSavePdf(outputName) { newRec ->
                                        Toast.makeText(context, "Edits applied!", Toast.LENGTH_SHORT).show()
                                    }
                                    activeTab = EditorTopTab.NONE
                                }
                            )
                        }

                        EditorTopTab.TEXT -> {
                            val currentSelectedTextItem = editItems.firstOrNull { it.id == selectedAnnotationId && it.type == PdfEngine.AnnotationType.TEXT }
                                ?: editItems.firstOrNull { it.pageIndex == currentPageIndex && it.type == PdfEngine.AnnotationType.TEXT }

                            AdvancedTextToolPanel(
                                selectedItem = currentSelectedTextItem,
                                pageWidthPx = pdfCanvasWidthPx,
                                pageHeightPx = pdfCanvasHeightPx,
                                onAddNewText = {
                                    textInputContent = ""
                                    selectedAnnotationId = null
                                    showAddTextDialog = true
                                },
                                onOpenEditDialog = {
                                    textInputContent = currentSelectedTextItem?.text ?: ""
                                    showAddTextDialog = true
                                },
                                onDeleteSelected = {
                                    currentSelectedTextItem?.let { item ->
                                        pushHistory()
                                        editItems.removeAll { it.id == item.id }
                                        selectedAnnotationId = null
                                        Toast.makeText(context, "Text deleted", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCopySelected = {
                                    currentSelectedTextItem?.let { item ->
                                        pushHistory()
                                        val copy = item.copy(
                                            id = java.util.UUID.randomUUID().toString(),
                                            posX = item.posX + 20f,
                                            posY = item.posY + 20f
                                        )
                                        editItems.add(copy)
                                        selectedAnnotationId = copy.id
                                        textLayerUpdateTrigger++
                                        Toast.makeText(context, "Text duplicated", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onBringToFront = {
                                    currentSelectedTextItem?.let { item ->
                                        pushHistory()
                                        editItems.remove(item)
                                        editItems.add(item)
                                        textLayerUpdateTrigger++
                                        Toast.makeText(context, "Brought to front", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onSendToBack = {
                                    currentSelectedTextItem?.let { item ->
                                        pushHistory()
                                        editItems.remove(item)
                                        editItems.add(0, item)
                                        textLayerUpdateTrigger++
                                        Toast.makeText(context, "Sent to back", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onUpdateItem = { updated ->
                                    val idx = editItems.indexOfFirst { it.id == updated.id }
                                    if (idx != -1) {
                                        editItems[idx] = updated
                                    }
                                    textLayerUpdateTrigger++
                                    textOverlayViewRef?.let { overlay ->
                                        overlay.updateSingleLayer(updated)
                                    }
                                },
                                onHistoryPush = { pushHistory() },
                                onOpenLayers = { showLayerManagerDialog = true }
                            )
                        }

                        EditorTopTab.IMAGE -> {
                            ImageOptionsSection(
                                onAddImage = {
                                    isReplacingImage = false
                                    imagePickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onReplaceImage = {
                                    if (selectedAnnotationId != null) {
                                        isReplacingImage = true
                                        imagePickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    } else {
                                        Toast.makeText(context, "Tap an image to select it first", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRotateSelectedImage = {
                                    selectedAnnotationId?.let { id ->
                                        val idx = editItems.indexOfFirst { it.id == id }
                                        if (idx != -1) {
                                            pushHistory()
                                            val old = editItems[idx]
                                            editItems[idx] = old.copy(imageRotation = (old.imageRotation + 90f) % 360f)
                                            Toast.makeText(context, "Image rotated +90°", Toast.LENGTH_SHORT).show()
                                        }
                                    } ?: Toast.makeText(context, "Tap an image first", Toast.LENGTH_SHORT).show()
                                },
                                onResizeImage = { factor ->
                                    selectedAnnotationId?.let { id ->
                                        val idx = editItems.indexOfFirst { it.id == id }
                                        if (idx != -1) {
                                            pushHistory()
                                            val old = editItems[idx]
                                            val newW = (old.imageWidth * factor).coerceIn(40f, 800f)
                                            val newH = (old.imageHeight * factor).coerceIn(40f, 800f)
                                            editItems[idx] = old.copy(imageWidth = newW, imageHeight = newH)
                                        }
                                    }
                                },
                                onDeleteSelectedImage = {
                                    selectedAnnotationId?.let { id ->
                                        pushHistory()
                                        editItems.removeAll { it.id == id }
                                        selectedAnnotationId = null
                                        Toast.makeText(context, "Image removed", Toast.LENGTH_SHORT).show()
                                    } ?: Toast.makeText(context, "Tap an image first", Toast.LENGTH_SHORT).show()
                                },
                                onRemoveBackground = {
                                    val selectedItem = editItems.firstOrNull { it.id == selectedAnnotationId && it.type == PdfEngine.AnnotationType.IMAGE }
                                    if (selectedItem != null && selectedItem.imageBitmap != null) {
                                        showBgRemovalChoiceDialog = true
                                    } else {
                                        Toast.makeText(context, "Tap an image to select it first", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        EditorTopTab.PEN -> {
                            PenSubToolbarSection(
                                activeSubTool = penSubTool,
                                onSubToolChange = { penSubTool = it },
                                penColor = penColor,
                                onPenColorChange = { penColor = it },
                                penStrokeThickness = penStrokeThickness,
                                onPenStrokeThicknessChange = { penStrokeThickness = it },
                                penOpacity = penOpacity,
                                onPenOpacityChange = { penOpacity = it },
                                highlightColor = highlightColor,
                                onHighlightColorChange = { highlightColor = it },
                                highlightWidth = highlightWidth,
                                onHighlightWidthChange = { highlightWidth = it },
                                highlightOpacity = highlightOpacity,
                                onHighlightOpacityChange = { highlightOpacity = it },
                                shapeType = shapeType,
                                onShapeTypeChange = { shapeType = it },
                                shapeColor = shapeColor,
                                onShapeColorChange = { shapeColor = it },
                                shapeStrokeWidth = shapeStrokeWidth,
                                onShapeStrokeWidthChange = { shapeStrokeWidth = it },
                                shapeFillColor = shapeFillColor,
                                onShapeFillColorChange = { shapeFillColor = it },
                                eraserType = eraserType,
                                onEraserTypeChange = { eraserType = it },
                                eraserSize = eraserSize,
                                onEraserSizeChange = { eraserSize = it },
                                selectedAnnotationId = selectedAnnotationId,
                                onDeleteSelectedAnnotation = {
                                    selectedAnnotationId?.let { id ->
                                        pushHistory()
                                        editItems.removeAll { it.id == id }
                                        selectedAnnotationId = null
                                        annotationCanvasRef?.selectedAnnotationId = null
                                        annotationCanvasRef?.setAnnotations(
                                            editItems.filter { it.pageIndex == currentPageIndex && it.type != PdfEngine.AnnotationType.TEXT && it.type != PdfEngine.AnnotationType.IMAGE },
                                            currentPageIndex
                                        )
                                        Toast.makeText(context, "Annotation deleted", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClearAll = {
                                    val toRemove = editItems.filter {
                                        it.pageIndex == currentPageIndex && (
                                            it.type == PdfEngine.AnnotationType.PEN ||
                                            it.type == PdfEngine.AnnotationType.DRAW ||
                                            it.type == PdfEngine.AnnotationType.HIGHLIGHTER ||
                                            it.type == PdfEngine.AnnotationType.RECTANGLE ||
                                            it.type == PdfEngine.AnnotationType.CIRCLE ||
                                            it.type == PdfEngine.AnnotationType.LINE ||
                                            it.type == PdfEngine.AnnotationType.ARROW
                                        )
                                    }
                                    if (toRemove.isNotEmpty()) {
                                        pushHistory()
                                        editItems.removeAll(toRemove)
                                        selectedAnnotationId = null
                                        annotationCanvasRef?.selectedAnnotationId = null
                                        annotationCanvasRef?.setAnnotations(
                                            editItems.filter { it.pageIndex == currentPageIndex && it.type != PdfEngine.AnnotationType.TEXT && it.type != PdfEngine.AnnotationType.IMAGE },
                                            currentPageIndex
                                        )
                                        Toast.makeText(context, "Page annotations cleared", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onUndo = { handleUndo() },
                                canUndo = undoStack.isNotEmpty(),
                                onDone = { activeTab = EditorTopTab.NONE }
                            )
                        }

                        EditorTopTab.MORE -> {
                            MoreOptionsSection(
                                onCompress = {
                                    activeTab = EditorTopTab.NONE
                                    viewModel.openModal(ActiveModal.COMPRESS, currentFile)
                                },
                                onConvertToImages = {
                                    handleConvertToImages()
                                },
                                onProtectPassword = {
                                    isProtectAction = true
                                    showPasswordDialog = true
                                },
                                onRemovePassword = {
                                    isProtectAction = false
                                    viewModel.toggleProtectFile(currentFile, "")
                                    Toast.makeText(context, "Password protection removed", Toast.LENGTH_SHORT).show()
                                },
                                onAddPageNumbers = {
                                    showPageNumbersWorkspaceDialog = true
                                },
                                onSaveExport = {
                                    showSaveAsDialog = true
                                },
                                onExportSAF = {
                                    exportDocumentLauncher.launch(currentFile.name)
                                },
                                onApplyChanges = {
                                    val outputName = "${currentFile.nameWithoutExtension()}_edited.pdf"
                                    requestSavePdf(outputName) { newRec ->
                                        Toast.makeText(context, "Document updated!", Toast.LENGTH_SHORT).show()
                                    }
                                    activeTab = EditorTopTab.NONE
                                }
                            )
                        }

                        EditorTopTab.NONE -> {}
                    }
                }
            }
        }

        // ====================================================
        // 4. MAIN VIEWPORT: PDF Page Display & Annotation Canvas
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentFile.name,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    val isPenActive = activeTab == EditorTopTab.PEN
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isPenActive) {
                                if (!isPenActive) {
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
                            .pointerInput(isPenActive) {
                                if (!isPenActive) {
                                    detectTapGestures(
                                        onDoubleTap = { tapOffset ->
                                            if (zoomScale > 1.2f) {
                                                zoomScale = 1.0f
                                                panOffset = Offset.Zero
                                            } else {
                                                val targetScale = 2.5f
                                                panOffset = (panOffset - tapOffset) * (targetScale / zoomScale) + tapOffset
                                                zoomScale = targetScale
                                            }
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Floating Zoom & Fit Controls Overlay on top of preview container
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .zIndex(10f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFF1E293B).copy(alpha = 0.88f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .clickable {
                                        zoomScale = if (zoomScale > 1.2f) 1.0f else 2.0f
                                        panOffset = Offset.Zero
                                    }
                                    .testTag("btn_overlay_zoom")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (zoomScale > 1.2f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                                        contentDescription = "Zoom",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (zoomScale > 1.2f) "Zoom Out" else "Zoom In",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFF1E293B).copy(alpha = 0.88f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .clickable { fitPageMode = !fitPageMode }
                                    .testTag("btn_overlay_fit_screen")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (fitPageMode) Icons.Default.AspectRatio else Icons.Default.FitScreen,
                                        contentDescription = "Fit to Screen",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (fitPageMode) "Fit Width" else "Fit Page",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                    rotationZ = animatedRotation
                                },
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

                            BoxWithConstraints(
                                modifier = pageModifier
                                    .background(pageBackgroundColor)
                            ) {
                                val density = LocalDensity.current
                                val pageWidthPx = constraints.maxWidth.toFloat()
                                val pageHeightPx = constraints.maxHeight.toFloat()
                                if (pageWidthPx > 0f && pageHeightPx > 0f) {
                                    pdfCanvasWidthPx = pageWidthPx
                                    pdfCanvasHeightPx = pageHeightPx
                                }

                                    // 1. PDF Renderer, AnnotationCanvasOverlayView & TextOverlayView hosted inside FrameLayout via pdf_editor_canvas layout
                                    AndroidView(
                                        modifier = Modifier.fillMaxSize(),
                                        factory = { ctx ->
                                            val inflater = LayoutInflater.from(ctx)
                                            val root = inflater.inflate(R.layout.pdf_editor_canvas, null, false) as FrameLayout
                                            val imgView = root.findViewById<ImageView>(R.id.pdf_renderer_view)
                                            val annotOverlay = root.findViewById<AnnotationCanvasOverlayView>(R.id.annotation_canvas_overlay_view)
                                            val overlay = root.findViewById<TextOverlayView>(R.id.text_overlay_view)

                                            val scaledInitialBmp = if (pageContentScale != 1.0f && currentBmp != null) {
                                                getScaledPageBitmap(currentBmp, pageContentScale)
                                            } else {
                                                currentBmp
                                            }
                                            imgView.setImageBitmap(scaledInitialBmp)

                                            annotOverlay.visibility = View.VISIBLE
                                            annotOverlay.onHistoryPush = { pushHistory() }
                                            annotOverlay.onAnnotationAdded = { item ->
                                                editItems.add(item)
                                                textLayerUpdateTrigger++
                                            }
                                            annotOverlay.onAnnotationsRemoved = { removedItems ->
                                                editItems.removeAll(removedItems)
                                                if (removedItems.any { it.id == selectedAnnotationId }) {
                                                    selectedAnnotationId = null
                                                }
                                                textLayerUpdateTrigger++
                                            }
                                            annotOverlay.onAnnotationUpdated = { updatedItem ->
                                                val idx = editItems.indexOfFirst { it.id == updatedItem.id }
                                                if (idx != -1) {
                                                    editItems[idx] = updatedItem
                                                }
                                                textLayerUpdateTrigger++
                                            }
                                            annotOverlay.onAnnotationSelected = { id ->
                                                selectedAnnotationId = id
                                            }
                                            annotationCanvasRef = annotOverlay

                                            overlay.visibility = View.VISIBLE
                                            overlay.bringToFront()

                                            overlay.onLayerSelected = { id ->
                                                selectedAnnotationId = id
                                                if (id != null) activeTab = EditorTopTab.TEXT
                                            }
                                            overlay.onLayerUpdated = { updatedItem ->
                                                val idx = editItems.indexOfFirst { it.id == updatedItem.id }
                                                if (idx != -1) {
                                                    editItems[idx] = updatedItem
                                                }
                                                textLayerUpdateTrigger++
                                            }
                                            overlay.onLayerEditRequested = { item ->
                                                selectedAnnotationId = item.id
                                                textInputContent = item.text
                                                showAddTextDialog = true
                                            }
                                            overlay.onHistoryPush = {
                                                pushHistory()
                                            }

                                            textOverlayViewRef = overlay
                                            root
                                        },
                                        update = { root ->
                                            val imgView = root.findViewById<ImageView>(R.id.pdf_renderer_view)
                                            val displayBmp = if (pageContentScale != 1.0f && currentBmp != null) {
                                                getScaledPageBitmap(currentBmp, pageContentScale)
                                            } else {
                                                currentBmp
                                            }
                                            imgView.setImageBitmap(displayBmp)

                                            val annotOverlay = root.findViewById<AnnotationCanvasOverlayView>(R.id.annotation_canvas_overlay_view)
                                            annotationCanvasRef = annotOverlay
                                            annotOverlay.selectedAnnotationId = selectedAnnotationId
                                            annotOverlay.eraserType = eraserType
                                            annotOverlay.setAnnotations(
                                                editItems.filter { it.pageIndex == currentPageIndex && it.type != PdfEngine.AnnotationType.TEXT && it.type != PdfEngine.AnnotationType.IMAGE },
                                                currentPageIndex
                                            )
                                            val isPenActive = activeTab == EditorTopTab.PEN
                                            annotOverlay.isInteractionEnabled = isPenActive
                                            if (isPenActive) {
                                                when (penSubTool) {
                                                    AnnotationCanvasOverlayView.ToolMode.SELECT -> {
                                                        annotOverlay.setSelectMode()
                                                    }
                                                    AnnotationCanvasOverlayView.ToolMode.PEN -> {
                                                        annotOverlay.setPenMode(penColor, penStrokeThickness, penOpacity)
                                                    }
                                                    AnnotationCanvasOverlayView.ToolMode.HIGHLIGHT -> {
                                                        annotOverlay.setHighlightMode(highlightColor, highlightWidth, highlightOpacity)
                                                    }
                                                    AnnotationCanvasOverlayView.ToolMode.SHAPE -> {
                                                        annotOverlay.setShapeMode(shapeType, shapeColor, shapeStrokeWidth, penOpacity, shapeFillColor)
                                                    }
                                                    AnnotationCanvasOverlayView.ToolMode.ERASER -> {
                                                        annotOverlay.setEraserMode(eraserSize, eraserType)
                                                    }
                                                }
                                                annotOverlay.elevation = 24f * root.resources.displayMetrics.density
                                                annotOverlay.bringToFront()
                                            } else {
                                                annotOverlay.elevation = 8f * root.resources.displayMetrics.density
                                            }

                                            val overlay = root.findViewById<TextOverlayView>(R.id.text_overlay_view)
                                            textOverlayViewRef = overlay
                                            overlay.visibility = View.VISIBLE
                                            overlay.isInteractionsEnabled = !isPenActive
                                            if (!isPenActive) {
                                                overlay.elevation = 20f * root.resources.displayMetrics.density
                                                overlay.bringToFront()
                                            } else {
                                                overlay.elevation = 4f * root.resources.displayMetrics.density
                                            }
                                            val _trigger = textLayerUpdateTrigger
                                            overlay.updateLayers(
                                                items = editItems.filter { it.pageIndex == currentPageIndex && it.type == PdfEngine.AnnotationType.TEXT },
                                                selectedId = selectedAnnotationId
                                            )
                                        }
                                    )

                                    // 3. Interactive Image Overlays: freely drag, reposition, and resize using touch gestures
                                    val pageImageItems = editItems.filter { it.pageIndex == currentPageIndex && it.type == PdfEngine.AnnotationType.IMAGE }
                                    for (imgItem in pageImageItems) {
                                        val isSelected = selectedAnnotationId == imgItem.id
                                        val imgBmp = imgItem.imageBitmap

                                        if (imgBmp != null) {
                                            val widthDp = with(density) { imgItem.imageWidth.toDp() }
                                            val heightDp = with(density) { imgItem.imageHeight.toDp() }
                                            val offsetX = imgItem.posX
                                            val offsetY = imgItem.posY

                                            Box(
                                                modifier = Modifier
                                                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                                                    .size(widthDp, heightDp)
                                                    .graphicsLayer {
                                                        rotationZ = imgItem.imageRotation
                                                    }
                                                    .pointerInput(imgItem.id, isPenActive) {
                                                        if (!isPenActive) {
                                                            detectTapGestures(
                                                                onTap = {
                                                                    selectedAnnotationId = imgItem.id
                                                                    activeTab = EditorTopTab.IMAGE
                                                                }
                                                            )
                                                        }
                                                    }
                                                    .pointerInput(imgItem.id, isPenActive) {
                                                        if (!isPenActive) {
                                                            detectDragGestures(
                                                                onDragStart = {
                                                                    pushHistory()
                                                                    selectedAnnotationId = imgItem.id
                                                                    activeTab = EditorTopTab.IMAGE
                                                                },
                                                                onDrag = { change, dragAmount ->
                                                                    change.consume()
                                                                    val scale = zoomScale.coerceAtLeast(0.1f)
                                                                    val dX = dragAmount.x / scale
                                                                    val dY = dragAmount.y / scale
                                                                    val idx = editItems.indexOfFirst { it.id == imgItem.id }
                                                                    if (idx != -1) {
                                                                        val cur = editItems[idx]
                                                                        val newX = (cur.posX + dX).coerceIn(-cur.imageWidth * 0.4f, pageWidthPx - cur.imageWidth * 0.6f)
                                                                        val newY = (cur.posY + dY).coerceIn(-cur.imageHeight * 0.4f, pageHeightPx - cur.imageHeight * 0.6f)
                                                                        editItems[idx] = cur.copy(posX = newX, posY = newY)
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                            ) {
                                                // 1. Image Bitmap
                                                Image(
                                                    bitmap = imgBmp.asImageBitmap(),
                                                    contentDescription = "Added Image",
                                                    contentScale = ContentScale.FillBounds,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(2.dp))
                                                )

                                                // 2. Custom Image Adjustment Box (Clean Standard Crop/Selection Frame)
                                                if (isSelected) {
                                                    // Clean rectangular bounding box
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .border(1.5.dp, ToolBlue, RoundedCornerShape(2.dp))
                                                    )

                                                    val cornerTouchSize = 36.dp
                                                    val handleCircleSize = 13.dp
                                                    val handleColor = ToolBlue
                                                    val handleInner = Color.White

                                                    // --- 4 CORNER HANDLES (Fluid 2D Scaling / Resizing) ---

                                                    // TOP-LEFT CORNER HANDLE
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .offset(x = (-18).dp, y = (-18).dp)
                                                            .size(cornerTouchSize)
                                                            .pointerInput(imgItem.id) {
                                                                detectDragGestures(
                                                                    onDragStart = { pushHistory() },
                                                                    onDrag = { change, dragAmount ->
                                                                        change.consume()
                                                                        val scale = zoomScale.coerceAtLeast(0.1f)
                                                                        val dX = dragAmount.x / scale
                                                                        val dY = dragAmount.y / scale
                                                                        val idx = editItems.indexOfFirst { it.id == imgItem.id }
                                                                        if (idx != -1) {
                                                                            val cur = editItems[idx]
                                                                            val newW = (cur.imageWidth - dX).coerceIn(40f, cur.posX + cur.imageWidth)
                                                                            val deltaW = cur.imageWidth - newW
                                                                            val newX = cur.posX + deltaW
                                                                            val newH = (cur.imageHeight - dY).coerceIn(40f, cur.posY + cur.imageHeight)
                                                                            val deltaH = cur.imageHeight - newH
                                                                            val newY = cur.posY + deltaH
                                                                            editItems[idx] = cur.copy(posX = newX, posY = newY, imageWidth = newW, imageHeight = newH)
                                                                        }
                                                                    }
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(handleCircleSize)
                                                                .background(handleInner, CircleShape)
                                                                .border(2.dp, handleColor, CircleShape)
                                                        )
                                                    }

                                                    // TOP-RIGHT CORNER HANDLE
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 18.dp, y = (-18).dp)
                                                            .size(cornerTouchSize)
                                                            .pointerInput(imgItem.id) {
                                                                detectDragGestures(
                                                                    onDragStart = { pushHistory() },
                                                                    onDrag = { change, dragAmount ->
                                                                        change.consume()
                                                                        val scale = zoomScale.coerceAtLeast(0.1f)
                                                                        val dX = dragAmount.x / scale
                                                                        val dY = dragAmount.y / scale
                                                                        val idx = editItems.indexOfFirst { it.id == imgItem.id }
                                                                        if (idx != -1) {
                                                                            val cur = editItems[idx]
                                                                            val newW = (cur.imageWidth + dX).coerceIn(40f, pageWidthPx - cur.posX)
                                                                            val newH = (cur.imageHeight - dY).coerceIn(40f, cur.posY + cur.imageHeight)
                                                                            val deltaH = cur.imageHeight - newH
                                                                            val newY = cur.posY + deltaH
                                                                            editItems[idx] = cur.copy(posY = newY, imageWidth = newW, imageHeight = newH)
                                                                        }
                                                                    }
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(handleCircleSize)
                                                                .background(handleInner, CircleShape)
                                                                .border(2.dp, handleColor, CircleShape)
                                                        )
                                                    }

                                                    // BOTTOM-LEFT CORNER HANDLE
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomStart)
                                                            .offset(x = (-18).dp, y = 18.dp)
                                                            .size(cornerTouchSize)
                                                            .pointerInput(imgItem.id) {
                                                                detectDragGestures(
                                                                    onDragStart = { pushHistory() },
                                                                    onDrag = { change, dragAmount ->
                                                                        change.consume()
                                                                        val scale = zoomScale.coerceAtLeast(0.1f)
                                                                        val dX = dragAmount.x / scale
                                                                        val dY = dragAmount.y / scale
                                                                        val idx = editItems.indexOfFirst { it.id == imgItem.id }
                                                                        if (idx != -1) {
                                                                            val cur = editItems[idx]
                                                                            val newW = (cur.imageWidth - dX).coerceIn(40f, cur.posX + cur.imageWidth)
                                                                            val deltaW = cur.imageWidth - newW
                                                                            val newX = cur.posX + deltaW
                                                                            val newH = (cur.imageHeight + dY).coerceIn(40f, pageHeightPx - cur.posY)
                                                                            editItems[idx] = cur.copy(posX = newX, imageWidth = newW, imageHeight = newH)
                                                                        }
                                                                    }
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(handleCircleSize)
                                                                .background(handleInner, CircleShape)
                                                                .border(2.dp, handleColor, CircleShape)
                                                        )
                                                    }

                                                    // BOTTOM-RIGHT CORNER HANDLE
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .offset(x = 18.dp, y = 18.dp)
                                                            .size(cornerTouchSize)
                                                            .pointerInput(imgItem.id) {
                                                                detectDragGestures(
                                                                    onDragStart = { pushHistory() },
                                                                    onDrag = { change, dragAmount ->
                                                                        change.consume()
                                                                        val scale = zoomScale.coerceAtLeast(0.1f)
                                                                        val dX = dragAmount.x / scale
                                                                        val dY = dragAmount.y / scale
                                                                        val idx = editItems.indexOfFirst { it.id == imgItem.id }
                                                                        if (idx != -1) {
                                                                            val cur = editItems[idx]
                                                                            val newW = (cur.imageWidth + dX).coerceIn(40f, pageWidthPx - cur.posX)
                                                                            val newH = (cur.imageHeight + dY).coerceIn(40f, pageHeightPx - cur.posY)
                                                                            editItems[idx] = cur.copy(imageWidth = newW, imageHeight = newH)
                                                                        }
                                                                    }
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(handleCircleSize)
                                                                .background(handleInner, CircleShape)
                                                                .border(2.dp, handleColor, CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // 4. Text overlays and bounding boxes are rendered directly on TextOverlayView (in pdf_editor_canvas)
                                    }
                                }
                            }
                        }

                        // Floating Page Indicator Pill
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.88f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 12.dp)
                                .clickable { showThumbnailTray = !showThumbnailTray }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ViewCarousel, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${currentPageIndex + 1} / $totalPages",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====================================================
        // THUMBNAIL TRAY (Expandable at bottom)
        // ====================================================
        AnimatedVisibility(visible = showThumbnailTray && pages.isNotEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(95.dp),
                shadowElevation = 8.dp
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
    }

    // ====================================================
    // FULL DIALOGS: Pages Sheet, Crop, Watermark, Numbers, etc.
    // ====================================================
    if (showPagesSheet) {
        WorkspacePagesSheet(
            file = currentFile,
            pages = pages,
            onDismiss = { showPagesSheet = false },
            onApply = { newOrder, rotations, blanks, imgs ->
                showPagesSheet = false
                val outputName = "${currentFile.nameWithoutExtension()}_organized.pdf"
                viewModel.managePagesInFile(
                    sourceRecord = currentFile,
                    pageOrder = newOrder,
                    rotations = rotations,
                    blankPagesAfter = blanks,
                    additionalImages = imgs,
                    outputName = outputName
                ) { newRec ->
                    currentFile = newRec
                    currentPageIndex = 0
                    Toast.makeText(context, "Page organization applied!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showCropDialog) {
        WorkspaceCropDialog(
            file = currentFile,
            pages = pages,
            currentPage = currentPageIndex,
            onDismiss = { showCropDialog = false },
            onApply = { l, t, r, b, targetPages ->
                showCropDialog = false
                val outputName = "${currentFile.nameWithoutExtension()}_cropped.pdf"
                viewModel.cropPdf(
                    sourceRecord = currentFile,
                    leftPct = l,
                    topPct = t,
                    rightPct = r,
                    bottomPct = b,
                    targetPages = targetPages,
                    outputName = outputName
                ) { newRec ->
                    currentFile = newRec
                    Toast.makeText(context, "Pages cropped successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showWatermarkWorkspaceDialog) {
        WorkspaceWatermarkDialog(
            file = currentFile,
            currentPage = currentPageIndex,
            onDismiss = { showWatermarkWorkspaceDialog = false },
            onApply = { text, rot, opacity, size, color, allPages, isTiled ->
                showWatermarkWorkspaceDialog = false
                val outName = "${currentFile.nameWithoutExtension()}_watermarked.pdf"
                viewModel.applyWatermarkStandalone(
                    sourceRecord = currentFile,
                    text = text,
                    rotationDeg = rot,
                    outputName = outName,
                    isTiled = isTiled,
                    fontSize = size,
                    opacity = opacity,
                    colorHex = color
                ) { newRec ->
                    currentFile = newRec
                    Toast.makeText(context, "Watermark applied!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showPageNumbersWorkspaceDialog) {
        WorkspacePageNumbersDialog(
            file = currentFile,
            onDismiss = { showPageNumbersWorkspaceDialog = false },
            onApply = { format, pos, targetPages ->
                showPageNumbersWorkspaceDialog = false
                val outName = "${currentFile.nameWithoutExtension()}_numbered.pdf"
                viewModel.addCustomPageNumbers(
                    sourceRecord = currentFile,
                    format = format,
                    position = pos,
                    targetPages = targetPages,
                    outputName = outName
                ) { newRec ->
                    currentFile = newRec
                    Toast.makeText(context, "Page numbers added!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showChangePageSizeDialog) {
        val pageSizes = listOf("A4 (210 × 297 mm)", "US Letter (8.5 × 11 in)", "US Legal (8.5 × 14 in)", "A3 (297 × 420 mm)", "Executive")
        var selectedSize by remember { mutableStateOf(pageSizes.first()) }
        AlertDialog(
            onDismissRequest = { showChangePageSizeDialog = false },
            title = { Text("Change Page Size") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select standard paper size to apply:", fontSize = 13.sp, color = Color.Gray)
                    pageSizes.forEach { size ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSize = size }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedSize == size,
                                onClick = { selectedSize = size }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(size, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showChangePageSizeDialog = false
                        Toast.makeText(context, "Page size set to $selectedSize", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                ) {
                    Text("Apply Size")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePageSizeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResizePagesDialog) {
        val scales = listOf(50, 75, 100, 125, 150)
        var selectedScale by remember { mutableIntStateOf(100) }
        AlertDialog(
            onDismissRequest = { showResizePagesDialog = false },
            title = { Text("Resize / Scale Pages") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select content scale percentage:", fontSize = 13.sp, color = Color.Gray)
                    scales.forEach { s ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedScale = s }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedScale == s,
                                onClick = { selectedScale = s }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$s%", fontSize = 14.sp, fontWeight = if (s == 100) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newScale = selectedScale / 100f
                        pageContentScale = newScale
                        showResizePagesDialog = false
                        Toast.makeText(context, "Pages scaled to $selectedScale%", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                ) {
                    Text("Apply Scale")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResizePagesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMarginsDialog) {
        val margins = listOf("Normal (1 inch / 72 pt)", "Narrow (0.5 inch / 36 pt)", "Wide (1.5 inch / 108 pt)", "None (0 pt)")
        var selectedMargin by remember { mutableStateOf(margins.first()) }
        AlertDialog(
            onDismissRequest = { showMarginsDialog = false },
            title = { Text("Page Margins") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select document margin width:", fontSize = 13.sp, color = Color.Gray)
                    margins.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMargin = m }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedMargin == m,
                                onClick = { selectedMargin = m }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(m, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMarginsDialog = false
                        Toast.makeText(context, "Margins updated to $selectedMargin", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                ) {
                    Text("Apply Margins")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarginsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBackgroundDialog) {
        val bgOptions = listOf(
            Pair("White", Color.White),
            Pair("Warm Cream", Color(0xFFFDF6E2)),
            Pair("Soft Mint", Color(0xFFE8F5E9)),
            Pair("Light Sky", Color(0xFFE1F5FE)),
            Pair("Dark Slate", Color(0xFF1E293B))
        )
        AlertDialog(
            onDismissRequest = { showBackgroundDialog = false },
            title = { Text("Page Background Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose background color for viewing and printing:", fontSize = 13.sp, color = Color.Gray)
                    bgOptions.forEach { (name, col) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pageBackgroundColor = col }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(1.dp, Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, fontSize = 14.sp, fontWeight = if (pageBackgroundColor == col) FontWeight.Bold else FontWeight.Normal)
                            if (pageBackgroundColor == col) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, tint = CrimsonPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBackgroundDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMergePdfDialog) {
        val otherFiles = allFiles.filter { it.id != currentFile.id }
        AlertDialog(
            onDismissRequest = { showMergePdfDialog = false },
            title = { Text("Merge with Another PDF") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                    if (otherFiles.isEmpty()) {
                        Text("No other saved PDF files found to merge.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        Text("Select a file to append to this document:", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(otherFiles) { file ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showMergePdfDialog = false
                                            val outName = "${currentFile.nameWithoutExtension()}_merged.pdf"
                                            viewModel.mergePdfs(listOf(currentFile, file), outName) { newRec ->
                                                currentFile = newRec
                                                Toast.makeText(context, "Merged with ${file.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CrimsonPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(file.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                            Text("${file.pageCount} pages", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMergePdfDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddTextDialog) {
        val editingItem = editItems.firstOrNull { it.id == selectedAnnotationId && it.type == PdfEngine.AnnotationType.TEXT }
        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text(if (editingItem != null) "Edit Text" else "Insert Text") },
            text = {
                OutlinedTextField(
                    value = textInputContent,
                    onValueChange = { textInputContent = it },
                    placeholder = { Text("Type text to place on page...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 6,
                    trailingIcon = {
                        if (textInputContent.isNotEmpty()) {
                            IconButton(onClick = { textInputContent = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear text", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInputContent.isNotBlank()) {
                            pushHistory()
                            if (editingItem != null) {
                                val idx = editItems.indexOfFirst { it.id == editingItem.id }
                                if (idx != -1) {
                                    val updated = editingItem.copy(text = textInputContent)
                                    editItems[idx] = updated
                                    textOverlayViewRef?.apply {
                                        visibility = View.VISIBLE
                                        bringToFront()
                                        addOrSelectLayer(updated)
                                        postInvalidate()
                                    }
                                }
                                textLayerUpdateTrigger++
                                Toast.makeText(context, "Text updated", Toast.LENGTH_SHORT).show()
                            } else {
                                val overlay = textOverlayViewRef
                                val canvasW = if (overlay != null && overlay.width > 0) overlay.width.toFloat() else if (pdfCanvasWidthPx > 0f) pdfCanvasWidthPx else 800f
                                val canvasH = if (overlay != null && overlay.height > 0) overlay.height.toFloat() else if (pdfCanvasHeightPx > 0f) pdfCanvasHeightPx else 1100f
                                val defaultW = 320f
                                val defaultH = 110f
                                // Initial Coordinates: strictly at center of visible PDF page viewport
                                // startX = overlayView.width / 2f, startY = overlayView.height / 3f. Fallback to (200f, 200f)
                                val startX = if (overlay != null && overlay.width > 0) overlay.width / 2f else (canvasW / 2f)
                                val startY = if (overlay != null && overlay.height > 0) overlay.height / 3f else (canvasH / 3f)
                                val initX = if (startX > 0f) (startX - defaultW / 2f).coerceAtLeast(20f) else 200f
                                val initY = if (startY > 0f) (startY - defaultH / 2f).coerceAtLeast(30f) else 200f

                                val defaultColor = if (textColor != 0) textColor else AndroidColor.RED
                                val defaultFontSize = if (textFontSize >= 24f) textFontSize else 28f

                                val newItem = PdfEngine.EditItem(
                                    pageIndex = currentPageIndex,
                                    type = PdfEngine.AnnotationType.TEXT,
                                    text = textInputContent,
                                    colorHex = defaultColor,
                                    fontSize = defaultFontSize,
                                    isBold = textIsBold,
                                    isItalic = textIsItalic,
                                    alignment = textAlignment,
                                    posX = initX,
                                    posY = initY,
                                    boxWidth = defaultW,
                                    boxHeight = defaultH,
                                    isVisible = true,
                                    opacity = 1f,
                                    baseWidth = canvasW,
                                    baseHeight = canvasH
                                )
                                editItems.add(newItem)
                                selectedAnnotationId = newItem.id
                                activeTab = EditorTopTab.TEXT
                                textLayerUpdateTrigger++

                                // Force View Attachment & Visibility: programmatically ensure overlayView.visibility = View.VISIBLE and invoke overlayView.bringToFront()
                                // Invalidation: Immediately call overlayView.postInvalidate() right after appending the new text layer object
                                overlay?.apply {
                                    visibility = View.VISIBLE
                                    bringToFront()
                                    addOrSelectLayer(newItem)
                                    postInvalidate()
                                }
                                Toast.makeText(context, "Text added to page", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showAddTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                ) {
                    Text(if (editingItem != null) "Apply" else "Add Text")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    JpgExportSuccessDialog(
        show = showJpgExportDialog,
        result = exportedJpgResult,
        location = exportedJpgLocation,
        context = context,
        onDismiss = { showJpgExportDialog = false }
    )

    if (showLayerManagerDialog) {
        val pageLayers = editItems
            .filter { it.pageIndex == currentPageIndex }
            .reversed()
            .map { item ->
                LayerEntry(
                    id = item.id,
                    title = if (item.type == PdfEngine.AnnotationType.TEXT) {
                        if (item.text.isNotBlank()) item.text else "Text Layer"
                    } else "Image Layer",
                    subtitle = if (item.type == PdfEngine.AnnotationType.TEXT) "Text" else "Image",
                    isText = item.type == PdfEngine.AnnotationType.TEXT,
                    thumbnailBitmap = item.imageBitmap,
                    isVisible = item.isVisible,
                    isLocked = item.isLocked,
                    opacity = item.opacity
                )
            }

        FullLayerManagerDialog(
            layers = pageLayers,
            selectedId = selectedAnnotationId,
            onSelect = { id -> selectedAnnotationId = id },
            onToggleVisibility = { id ->
                pushHistory()
                val idx = editItems.indexOfFirst { it.id == id }
                if (idx != -1) {
                    val itm = editItems[idx]
                    editItems[idx] = itm.copy(isVisible = !itm.isVisible)
                }
            },
            onToggleLock = { id ->
                pushHistory()
                val idx = editItems.indexOfFirst { it.id == id }
                if (idx != -1) {
                    val itm = editItems[idx]
                    editItems[idx] = itm.copy(isLocked = !itm.isLocked)
                }
            },
            onChangeOpacity = { id, newOpacity ->
                val idx = editItems.indexOfFirst { it.id == id }
                if (idx != -1) {
                    val itm = editItems[idx]
                    editItems[idx] = itm.copy(opacity = newOpacity)
                }
            },
            onMoveUp = { reversedIndex ->
                pushHistory()
                val pageItemIndices = editItems.mapIndexedNotNull { i, item -> if (item.pageIndex == currentPageIndex) i else null }
                val origIndexInPage = (pageItemIndices.size - 1) - reversedIndex
                if (origIndexInPage < pageItemIndices.size - 1) {
                    val actualIdx1 = pageItemIndices[origIndexInPage]
                    val actualIdx2 = pageItemIndices[origIndexInPage + 1]
                    val temp = editItems[actualIdx1]
                    editItems[actualIdx1] = editItems[actualIdx2]
                    editItems[actualIdx2] = temp
                }
            },
            onMoveDown = { reversedIndex ->
                pushHistory()
                val pageItemIndices = editItems.mapIndexedNotNull { i, item -> if (item.pageIndex == currentPageIndex) i else null }
                val origIndexInPage = (pageItemIndices.size - 1) - reversedIndex
                if (origIndexInPage > 0) {
                    val actualIdx1 = pageItemIndices[origIndexInPage]
                    val actualIdx2 = pageItemIndices[origIndexInPage - 1]
                    val temp = editItems[actualIdx1]
                    editItems[actualIdx1] = editItems[actualIdx2]
                    editItems[actualIdx2] = temp
                }
            },
            onDelete = { id ->
                pushHistory()
                editItems.removeAll { it.id == id }
                if (selectedAnnotationId == id) selectedAnnotationId = null
            },
            onDismiss = { showLayerManagerDialog = false }
        )
    }

    if (showBgRemovalChoiceDialog) {
        PdfImageBgRemovalChoiceDialog(
            onDismiss = { showBgRemovalChoiceDialog = false },
            onAutoAiRemove = {
                showBgRemovalChoiceDialog = false
                val selectedItem = editItems.firstOrNull { it.id == selectedAnnotationId && it.type == PdfEngine.AnnotationType.IMAGE }
                if (selectedItem == null || selectedItem.imageBitmap == null) {
                    Toast.makeText(context, "Tap an image to select it first", Toast.LENGTH_SHORT).show()
                } else {
                    isRemovingBackground = true
                    coroutineScope.launch(Dispatchers.Default) {
                        try {
                            val targetBmp = selectedItem.originalImageBitmap ?: selectedItem.imageBitmap!!
                            val result = BiRefNetLiteEngine.removeBackground(context.applicationContext, targetBmp)
                            withContext(Dispatchers.Main) {
                                isRemovingBackground = false
                                if (result.isSuccess) {
                                    val seg = result.getOrThrow()
                                    pushHistory()
                                    val idx = editItems.indexOfFirst { it.id == selectedItem.id }
                                    if (idx != -1) {
                                        val old = editItems[idx]
                                        editItems[idx] = old.copy(
                                            imageBitmap = seg.transparentForeground,
                                            originalImageBitmap = old.originalImageBitmap ?: old.imageBitmap,
                                            alphaMask = seg.alphaMask,
                                            maskWidth = seg.maskWidth,
                                            maskHeight = seg.maskHeight
                                        )
                                        textLayerUpdateTrigger++
                                        Toast.makeText(context, "Background removed with BiRefNet Lite AI!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val err = result.exceptionOrNull()
                                    val msg = err?.localizedMessage ?: err?.message ?: "Background removal failed"
                                    Toast.makeText(context, "AI Background Removal error: $msg", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isRemovingBackground = false
                                Toast.makeText(context, "Failed to remove background: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            },
            onManualEraser = {
                showBgRemovalChoiceDialog = false
                val selectedItem = editItems.firstOrNull { it.id == selectedAnnotationId && it.type == PdfEngine.AnnotationType.IMAGE }
                if (selectedItem != null && selectedItem.imageBitmap != null) {
                    manualEraserTargetItem = selectedItem
                    showManualEraserDialog = true
                } else {
                    Toast.makeText(context, "Tap an image to select it first", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (isRemovingBackground) {
        PdfImageBgRemovalProgressDialog()
    }

    if (showManualEraserDialog && manualEraserTargetItem != null) {
        val target = manualEraserTargetItem!!
        val currentImg = target.imageBitmap
        val origImg = target.originalImageBitmap ?: target.imageBitmap
        if (currentImg != null && origImg != null) {
            PdfImageManualEraserDialog(
                imageBitmap = currentImg,
                originalBitmap = origImg,
                onApply = { updatedBmp ->
                    pushHistory()
                    val idx = editItems.indexOfFirst { it.id == target.id }
                    if (idx != -1) {
                        val old = editItems[idx]
                        editItems[idx] = old.copy(
                            imageBitmap = updatedBmp,
                            originalImageBitmap = old.originalImageBitmap ?: old.imageBitmap
                        )
                        textLayerUpdateTrigger++
                        Toast.makeText(context, "Image updated with manual refine!", Toast.LENGTH_SHORT).show()
                    }
                    showManualEraserDialog = false
                    manualEraserTargetItem = null
                },
                onDismiss = {
                    showManualEraserDialog = false
                    manualEraserTargetItem = null
                }
            )
        } else {
            showManualEraserDialog = false
            manualEraserTargetItem = null
        }
    }

    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("Save Document As") },
            text = {
                OutlinedTextField(
                    value = saveAsNameInput,
                    onValueChange = { saveAsNameInput = it },
                    label = { Text("File Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveAsDialog = false
                        requestSavePdf(saveAsNameInput) { newRec ->
                            Toast.makeText(context, "Saved as $saveAsNameInput", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStoragePermissionRationaleDialog) {
        val activity = context as? Activity
        AlertDialog(
            onDismissRequest = { showStoragePermissionRationaleDialog = false },
            icon = {
                Icon(
                    Icons.Default.FolderSpecial,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Storage Access & Save Location", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "On Android 11+, saving directly into device root folders (\"Smart King\") requires All Files Access permission.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "You can grant permission in Settings, or save safely into App Documents storage without permissions.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStoragePermissionRationaleDialog = false
                        // Proceed to save directly to app storage / Documents
                        pendingSaveFileName?.let { name ->
                            viewModel.saveEditedPdf(
                                sourceRecord = currentFile,
                                items = editItems.toList(),
                                deletedPages = deletedPages.toSet(),
                                pageRotations = pageRotations.toMap(),
                                outputName = name,
                                pageScale = pageContentScale
                            ) { newRec ->
                                currentFile = newRec
                                pendingSaveSuccessCallback?.invoke(newRec)
                                Toast.makeText(context, "Saved as $name", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("btn_save_app_storage")
                ) {
                    Text("Save to App Storage")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { showStoragePermissionRationaleDialog = false }
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            showStoragePermissionRationaleDialog = false
                            if (activity != null) {
                                StoragePermissionHelper.requestStoragePermission(activity)
                            } else {
                                StoragePermissionHelper.openAppSettings(context)
                            }
                        },
                        modifier = Modifier.testTag("btn_grant_storage_settings")
                    ) {
                        Text("Settings")
                    }
                }
            }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(if (isProtectAction) "Protect PDF with Password" else "Remove Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isProtectAction) {
                        Text("Set a password to encrypt this PDF document:", fontSize = 13.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Remove password protection from this document?", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPasswordDialog = false
                        if (isProtectAction) {
                            viewModel.toggleProtectFile(currentFile, passwordInput)
                            Toast.makeText(context, "Password protection applied!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.toggleProtectFile(currentFile, "")
                            Toast.makeText(context, "Password protection removed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                ) {
                    Text(if (isProtectAction) "Protect" else "Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showViewerPasswordDialog) {
        val viewerFile by viewModel.currentViewerFile.collectAsState()
        val targetFile = viewerFile ?: currentFile
        val displayFileName = targetFile.resolveActualFileName(context)
        val fileDisplayText = if (displayFileName == "Unknown PDF") "Unknown PDF" else "\"$displayFileName\""

        AlertDialog(
            onDismissRequest = { viewModel.dismissViewerPasswordDialog() },
            title = {
                Text(
                    text = "Encrypted PDF - Password Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "File: $fileDisplayText",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("dialog_encrypted_filename")
                    )
                    Text(
                        text = "This PDF file is password-protected. Please enter the password to decrypt and view the document.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    if (!viewerPasswordError.isNullOrBlank()) {
                        Text(
                            text = viewerPasswordError ?: "",
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(
                        value = viewerPasswordInput,
                        onValueChange = { viewerPasswordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("viewer_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.loadPagesForViewer(targetFile, forceReload = true, password = viewerPasswordInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("btn_unlock_pdf")
                ) {
                    Text("Unlock & Open")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissViewerPasswordDialog()
                        onBack()
                    },
                    modifier = Modifier.testTag("btn_cancel_unlock")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EditorToolbarTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) ToolBlue else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("tab_$label")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PageOptionsSection(
    onReorderPages: () -> Unit,
    onRotatePage: () -> Unit,
    onDeletePage: () -> Unit,
    onDuplicatePage: () -> Unit,
    onInsertBlankPage: () -> Unit,
    onExtractPage: () -> Unit,
    onSplitPdf: () -> Unit,
    onMergePdf: () -> Unit,
    onSelectAll: () -> Unit,
    onApplyChanges: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Reorder", Icons.Default.SwapVert, ToolBlue, onReorderPages)
            ToolButtonCard(Modifier.weight(1f), "Rotate Page", Icons.Default.RotateRight, ToolBlue, onRotatePage)
            ToolButtonCard(Modifier.weight(1f), "Delete Page", Icons.Default.Delete, CrimsonPrimary, onDeletePage)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Duplicate", Icons.Default.ContentCopy, ToolPurple, onDuplicatePage)
            ToolButtonCard(Modifier.weight(1f), "Insert Blank", Icons.Default.AddBox, ToolGreen, onInsertBlankPage)
            ToolButtonCard(Modifier.weight(1f), "Extract Page", Icons.Default.UploadFile, ToolOrange, onExtractPage)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Split PDF", Icons.Default.CallSplit, ToolBlue, onSplitPdf)
            ToolButtonCard(Modifier.weight(1f), "Merge PDF", Icons.Default.MergeType, ToolGreen, onMergePdf)
            ToolButtonCard(Modifier.weight(1f), "Select All", Icons.Default.SelectAll, ToolPurple, onSelectAll)
        }
    }
}

@Composable
private fun EditOptionsSection(
    onCrop: () -> Unit,
    onResize: () -> Unit,
    onChangePageSize: () -> Unit,
    onMargins: () -> Unit,
    onWatermark: () -> Unit,
    onBackground: () -> Unit,
    onApplyChanges: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Crop Pages", Icons.Default.Crop, ToolOrange, onCrop)
            ToolButtonCard(Modifier.weight(1f), "Resize Pages", Icons.Default.ZoomOutMap, ToolBlue, onResize)
            ToolButtonCard(Modifier.weight(1f), "Page Size", Icons.Default.FormatSize, ToolPurple, onChangePageSize)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Margins", Icons.Default.BorderOuter, ToolGreen, onMargins)
            ToolButtonCard(Modifier.weight(1f), "Watermark", Icons.Default.Layers, ToolPurple, onWatermark)
            ToolButtonCard(Modifier.weight(1f), "Background", Icons.Default.Palette, ToolOrange, onBackground)
        }
    }
}


@Composable
private fun TextOptionsSection(
    onAddText: () -> Unit,
    textColor: Int,
    onColorChange: (Int) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    isBold: Boolean,
    onToggleBold: () -> Unit,
    isItalic: Boolean,
    onToggleItalic: () -> Unit,
    alignment: String,
    onAlignmentChange: (String) -> Unit,
    onDeleteSelectedText: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Text Insertion & Editing", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onAddText,
                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("btn_insert_text_action")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Text", fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(AndroidColor.BLACK, AndroidColor.RED, AndroidColor.BLUE, AndroidColor.GREEN, AndroidColor.WHITE).forEach { c ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (textColor == c) 2.dp else 1.dp,
                            color = if (textColor == c) CrimsonPrimary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(c) }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = isBold,
                onClick = onToggleBold,
                label = { Text("B", fontWeight = FontWeight.Bold) },
                modifier = Modifier.height(30.dp)
            )
            FilterChip(
                selected = isItalic,
                onClick = onToggleItalic,
                label = { Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) },
                modifier = Modifier.height(30.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onDeleteSelectedText,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Selected Text", tint = CrimsonPrimary)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Align:", color = Color(0xFF94A3B8), fontSize = 11.sp)
            FilterChip(
                selected = alignment == "LEFT",
                onClick = { onAlignmentChange("LEFT") },
                label = { Text("Left", fontSize = 10.sp) },
                modifier = Modifier.height(28.dp)
            )
            FilterChip(
                selected = alignment == "CENTER",
                onClick = { onAlignmentChange("CENTER") },
                label = { Text("Center", fontSize = 10.sp) },
                modifier = Modifier.height(28.dp)
            )
            FilterChip(
                selected = alignment == "RIGHT",
                onClick = { onAlignmentChange("RIGHT") },
                label = { Text("Right", fontSize = 10.sp) },
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

@Composable
private fun ImageOptionsSection(
    onAddImage: () -> Unit,
    onReplaceImage: () -> Unit,
    onRotateSelectedImage: () -> Unit,
    onResizeImage: (Float) -> Unit = {},
    onDeleteSelectedImage: () -> Unit,
    onRemoveBackground: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Add Image", Icons.Default.AddPhotoAlternate, ToolBlue, onAddImage)
            ToolButtonCard(Modifier.weight(1f), "Replace", Icons.Default.Sync, ToolPurple, onReplaceImage)
            ToolButtonCard(Modifier.weight(1f), "Rotate 90°", Icons.Default.RotateRight, ToolOrange, onRotateSelectedImage)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Remove BG", Icons.Default.AutoFixHigh, ToolGreen, onRemoveBackground)
            ToolButtonCard(Modifier.weight(1f), "Delete Image", Icons.Default.Delete, CrimsonPrimary, onDeleteSelectedImage)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PenSubToolbarSection(
    activeSubTool: AnnotationCanvasOverlayView.ToolMode,
    onSubToolChange: (AnnotationCanvasOverlayView.ToolMode) -> Unit,
    penColor: Int,
    onPenColorChange: (Int) -> Unit,
    penStrokeThickness: Float,
    onPenStrokeThicknessChange: (Float) -> Unit,
    penOpacity: Float,
    onPenOpacityChange: (Float) -> Unit,
    highlightColor: Int,
    onHighlightColorChange: (Int) -> Unit,
    highlightWidth: Float,
    onHighlightWidthChange: (Float) -> Unit,
    highlightOpacity: Float,
    onHighlightOpacityChange: (Float) -> Unit,
    shapeType: AnnotationCanvasOverlayView.ShapeType,
    onShapeTypeChange: (AnnotationCanvasOverlayView.ShapeType) -> Unit,
    shapeColor: Int,
    onShapeColorChange: (Int) -> Unit,
    shapeStrokeWidth: Float,
    onShapeStrokeWidthChange: (Float) -> Unit,
    shapeFillColor: Int?,
    onShapeFillColorChange: (Int?) -> Unit,
    eraserType: AnnotationCanvasOverlayView.EraserType,
    onEraserTypeChange: (AnnotationCanvasOverlayView.EraserType) -> Unit,
    eraserSize: AnnotationCanvasOverlayView.EraserSize,
    onEraserSizeChange: (AnnotationCanvasOverlayView.EraserSize) -> Unit,
    selectedAnnotationId: String? = null,
    onDeleteSelectedAnnotation: () -> Unit = {},
    onClearAll: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pen_subtoolbar_panel")
    ) {
        // Top Sub-tool Bar Header: Title, Undo, Clear, Done
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pen & Annotations",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(28.dp).testTag("btn_pen_undo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("btn_pen_clear_page")
                ) {
                    Text("Clear Page", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDone,
                    modifier = Modifier.size(28.dp).testTag("btn_pen_done")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sub-tool switcher tabs: Select, Freehand Pen, Highlight, Shapes, Eraser
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = activeSubTool == AnnotationCanvasOverlayView.ToolMode.SELECT,
                onClick = { onSubToolChange(AnnotationCanvasOverlayView.ToolMode.SELECT) },
                label = { Text("Select", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_select")
            )
            FilterChip(
                selected = activeSubTool == AnnotationCanvasOverlayView.ToolMode.PEN,
                onClick = { onSubToolChange(AnnotationCanvasOverlayView.ToolMode.PEN) },
                label = { Text("Pen", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_pen")
            )
            FilterChip(
                selected = activeSubTool == AnnotationCanvasOverlayView.ToolMode.HIGHLIGHT,
                onClick = { onSubToolChange(AnnotationCanvasOverlayView.ToolMode.HIGHLIGHT) },
                label = { Text("Highlight", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_highlight")
            )
            FilterChip(
                selected = activeSubTool == AnnotationCanvasOverlayView.ToolMode.SHAPE,
                onClick = { onSubToolChange(AnnotationCanvasOverlayView.ToolMode.SHAPE) },
                label = { Text("Shapes", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.CropSquare, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_shapes")
            )
            FilterChip(
                selected = activeSubTool == AnnotationCanvasOverlayView.ToolMode.ERASER,
                onClick = { onSubToolChange(AnnotationCanvasOverlayView.ToolMode.ERASER) },
                label = { Text("Eraser", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_eraser")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tool-specific options row
        when (activeSubTool) {
            AnnotationCanvasOverlayView.ToolMode.SELECT -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (selectedAnnotationId != null) "Annotation Selected" else "Select Mode Active",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (selectedAnnotationId != null) {
                            TextButton(
                                onClick = onDeleteSelectedAnnotation,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("btn_delete_selected_annotation")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = if (selectedAnnotationId != null)
                            "Drag inside box to move. Drag corner handles to scale / resize."
                        else
                            "Tap any pen stroke, highlight, or shape to select, move, and resize it.",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }

            AnnotationCanvasOverlayView.ToolMode.PEN -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Size presets
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Size:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        listOf(2f to "Fine", 4f to "Medium", 8f to "Thick", 14f to "Bold").forEach { (sz, lbl) ->
                            FilterChip(
                                selected = penStrokeThickness == sz,
                                onClick = { onPenStrokeThicknessChange(sz) },
                                label = { Text(lbl, fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }

                    // Color palette & Opacity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Color:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        listOf(
                            AndroidColor.rgb(37, 99, 235), // Royal Blue
                            AndroidColor.rgb(220, 38, 38), // Crimson Red
                            AndroidColor.rgb(16, 185, 129), // Emerald Green
                            AndroidColor.rgb(245, 158, 11), // Amber Orange
                            AndroidColor.rgb(147, 51, 234), // Purple
                            AndroidColor.BLACK,
                            AndroidColor.WHITE
                        ).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (penColor == c) 2.dp else 1.dp,
                                        color = if (penColor == c) Color.White else Color(0xFF475569),
                                        shape = CircleShape
                                    )
                                    .clickable { onPenColorChange(c) }
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${(penOpacity * 100).toInt()}%", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Slider(
                            value = penOpacity,
                            onValueChange = onPenOpacityChange,
                            valueRange = 0.2f..1.0f,
                            modifier = Modifier.weight(1f).height(24.dp)
                        )
                    }
                }
            }

            AnnotationCanvasOverlayView.ToolMode.HIGHLIGHT -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Width presets
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Width:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        listOf(12f to "12dp", 18f to "18dp", 26f to "26dp", 36f to "36dp").forEach { (w, lbl) ->
                            FilterChip(
                                selected = highlightWidth == w,
                                onClick = { onHighlightWidthChange(w) },
                                label = { Text(lbl, fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }

                    // Highlighter Colors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Color:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        listOf(
                            AndroidColor.rgb(250, 204, 21), // Fluorescent Yellow
                            AndroidColor.rgb(74, 222, 128), // Neon Green
                            AndroidColor.rgb(56, 189, 248), // Sky Cyan
                            AndroidColor.rgb(244, 114, 182), // Neon Pink
                            AndroidColor.rgb(251, 146, 60), // Vibrant Orange
                            AndroidColor.rgb(192, 132, 252) // Lavender
                        ).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (highlightColor == c) 2.dp else 1.dp,
                                        color = if (highlightColor == c) Color.White else Color(0xFF475569),
                                        shape = CircleShape
                                    )
                                    .clickable { onHighlightColorChange(c) }
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${(highlightOpacity * 100).toInt()}%", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Slider(
                            value = highlightOpacity,
                            onValueChange = onHighlightOpacityChange,
                            valueRange = 0.15f..0.70f,
                            modifier = Modifier.weight(1f).height(24.dp)
                        )
                    }
                }
            }

            AnnotationCanvasOverlayView.ToolMode.SHAPE -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Shape Type Picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = shapeType == AnnotationCanvasOverlayView.ShapeType.RECTANGLE,
                            onClick = { onShapeTypeChange(AnnotationCanvasOverlayView.ShapeType.RECTANGLE) },
                            label = { Text("Rectangle", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.CropSquare, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(26.dp)
                        )
                        FilterChip(
                            selected = shapeType == AnnotationCanvasOverlayView.ShapeType.CIRCLE,
                            onClick = { onShapeTypeChange(AnnotationCanvasOverlayView.ShapeType.CIRCLE) },
                            label = { Text("Circle", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Circle, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(26.dp)
                        )
                        FilterChip(
                            selected = shapeType == AnnotationCanvasOverlayView.ShapeType.LINE,
                            onClick = { onShapeTypeChange(AnnotationCanvasOverlayView.ShapeType.LINE) },
                            label = { Text("Line", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.HorizontalRule, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(26.dp)
                        )
                        FilterChip(
                            selected = shapeType == AnnotationCanvasOverlayView.ShapeType.ARROW,
                            onClick = { onShapeTypeChange(AnnotationCanvasOverlayView.ShapeType.ARROW) },
                            label = { Text("Arrow", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(26.dp)
                        )
                    }

                    // Stroke Colors & Fill toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Color:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        listOf(
                            AndroidColor.rgb(220, 38, 38), // Crimson
                            AndroidColor.rgb(37, 99, 235), // Blue
                            AndroidColor.rgb(16, 185, 129), // Green
                            AndroidColor.rgb(245, 158, 11), // Orange
                            AndroidColor.BLACK,
                            AndroidColor.WHITE
                        ).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (shapeColor == c) 2.dp else 1.dp,
                                        color = if (shapeColor == c) Color.White else Color(0xFF475569),
                                        shape = CircleShape
                                    )
                                    .clickable { onShapeColorChange(c) }
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = shapeFillColor != null,
                            onClick = { onShapeFillColorChange(if (shapeFillColor == null) shapeColor else null) },
                            label = { Text(if (shapeFillColor != null) "Filled" else "Outline", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )

                        Text("Drag to draw", color = Color(0xFF64748B), fontSize = 10.sp)
                    }
                }
            }

            AnnotationCanvasOverlayView.ToolMode.ERASER -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Dual Eraser Mode Switcher: Mode A (Stroke Eraser) vs Mode B (Manual Eraser)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Eraser Mode:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        FilterChip(
                            selected = eraserType == AnnotationCanvasOverlayView.EraserType.STROKE,
                            onClick = { onEraserTypeChange(AnnotationCanvasOverlayView.EraserType.STROKE) },
                            label = { Text("Stroke Eraser (Auto)", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp).testTag("chip_eraser_stroke")
                        )
                        FilterChip(
                            selected = eraserType == AnnotationCanvasOverlayView.EraserType.MANUAL,
                            onClick = { onEraserTypeChange(AnnotationCanvasOverlayView.EraserType.MANUAL) },
                            label = { Text("Manual Eraser (Partial)", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp).testTag("chip_eraser_manual")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Eraser Size:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            listOf(
                                AnnotationCanvasOverlayView.EraserSize.SMALL to "Small",
                                AnnotationCanvasOverlayView.EraserSize.MEDIUM to "Medium",
                                AnnotationCanvasOverlayView.EraserSize.LARGE to "Large"
                            ).forEach { (sz, lbl) ->
                                FilterChip(
                                    selected = eraserSize == sz,
                                    onClick = { onEraserSizeChange(sz) },
                                    label = { Text(lbl, fontSize = 10.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }

                        Text(
                            text = "PDF Protected",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = if (eraserType == AnnotationCanvasOverlayView.EraserType.STROKE)
                            "Mode A: Tap any stroke, highlight, or shape to erase entire object instantly."
                        else
                            "Mode B: Drag finger across annotations to erase only the touched portion.",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreOptionsSection(
    onCompress: () -> Unit,
    onConvertToImages: () -> Unit,
    onProtectPassword: () -> Unit,
    onRemovePassword: () -> Unit,
    onAddPageNumbers: () -> Unit,
    onSaveExport: () -> Unit,
    onExportSAF: () -> Unit,
    onApplyChanges: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Compress", Icons.Default.Compress, ToolGreen, onCompress)
            ToolButtonCard(Modifier.weight(1f), "To Images", Icons.Default.Image, ToolOrange, onConvertToImages)
            ToolButtonCard(Modifier.weight(1f), "Protect", Icons.Default.Lock, ToolPurple, onProtectPassword)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Remove Pass", Icons.Default.LockOpen, ToolPurple, onRemovePassword)
            ToolButtonCard(Modifier.weight(1f), "Page Numbers", Icons.Default.FormatListNumbered, ToolBlue, onAddPageNumbers)
            ToolButtonCard(Modifier.weight(1f), "Save As", Icons.Default.SaveAs, ToolBlue, onSaveExport)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButtonCard(Modifier.weight(1f), "Export to Device", Icons.Default.Download, ToolGreen, onExportSAF)
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}

@Composable
private fun ToolButtonCard(
    modifier: Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF243046),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, Color(0xFF334155)),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =========================================================================
// EXPORT NOTIFICATION & CONFIRMATION DIALOG WITH "FILE SHOW" ACTION
// =========================================================================
@Composable
private fun JpgExportSuccessDialog(
    show: Boolean,
    result: PdfToJpgConversionResult?,
    location: String,
    context: Context,
    onDismiss: () -> Unit
) {
    if (show && result != null) {
        val firstPage = result.pages.firstOrNull()
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Export to JPG Successful") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Successfully converted ${result.pages.size} page(s) to high-quality JPG.")
                    Text("Saved location: $location", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            if (firstPage != null) {
                                GlobalSaveManager.openFile(
                                    context = context,
                                    uri = firstPage.savedUri,
                                    file = firstPage.file,
                                    mimeType = "image/jpeg"
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open File")
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            GlobalSaveManager.openFolder(
                                context = context,
                                treeUriStr = firstPage?.savedUri?.toString(),
                                targetFile = firstPage?.file
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_file_show"),
                        colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("File Show")
                    }
                }
            }
        )
    }
}
