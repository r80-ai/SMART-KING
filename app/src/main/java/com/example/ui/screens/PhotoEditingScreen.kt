package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.photo.BgOptionType
import com.example.photo.BgRefineMode
import com.example.photo.birefnet.BiRefNetLiteEngine
import com.example.photo.MaskStroke
import com.example.photo.CropPreset
import com.example.photo.DrawingStroke
import com.example.photo.GradientDirection
import androidx.compose.material.icons.filled.FlashOn
import com.example.photo.PhotoEditCategory
import com.example.photo.PhotoEditTool
import com.example.photo.PhotoEditorEngine
import com.example.photo.FaceRelightEngine
import com.example.photo.PhotoFonts
import com.example.photo.TextAlignment
import com.example.photo.TextItem
import com.example.storage.GlobalSaveManager
import com.example.storage.SaveResult
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private val DarkBackground = Color(0xFF090D16)
private val DarkCardSurface = Color(0xFF131B2E)
private val DarkBarBg = Color(0xFF0F172A)
private val DarkBorder = Color(0xFF1E293B)
private val AccentPink = Color(0xFFFF4081)

@Composable
fun PhotoEditingScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Preload BiRefNet Lite model in background so BG Remove is instant
    LaunchedEffect(Unit) {
        BiRefNetLiteEngine.preloadModelAsync(context.applicationContext)
    }

    // 1. Image Lifecycle & History Management
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val undoStack = remember { mutableStateListOf<Bitmap>() }
    val redoStack = remember { mutableStateListOf<Bitmap>() }

    // Live preview bitmap for interactive adjustments (Crop, Adjust, Blur, etc.)
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // 2. Active Category & Tool State
    var selectedCategory by remember { mutableStateOf(PhotoEditCategory.BASIC) }
    var activeTool by remember { mutableStateOf(PhotoEditTool.NONE) }

    // Dialog States
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showSavedDialog by remember { mutableStateOf(false) }
    var showLayerManagerDialog by remember { mutableStateOf(false) }
    var lastSaveResult by remember { mutableStateOf<SaveResult?>(null) }

    // Tool Specific States
    // CROP
    var cropRect by remember { mutableStateOf(RectF(0.05f, 0.05f, 0.95f, 0.95f)) }
    var cropPreset by remember { mutableStateOf(CropPreset.FREE) }

    // RESIZE
    var resizeWidthText by remember { mutableStateOf("") }
    var resizeHeightText by remember { mutableStateOf("") }
    var maintainAspectRatio by remember { mutableStateOf(true) }

    // ADJUST (Sliders)
    var brightnessValue by remember { mutableFloatStateOf(0f) }   // -100..+100
    var contrastValue by remember { mutableFloatStateOf(0f) }   // -100..+100
    var saturationValue by remember { mutableFloatStateOf(0f) }   // -100..+100
    var enhanceValue by remember { mutableFloatStateOf(0f) }   // 0..100
    var sharpenValue by remember { mutableFloatStateOf(0f) }   // 0..100
    var flashIntensity by remember { mutableFloatStateOf(60f) }   // 0..100
    var isFlashProcessing by remember { mutableStateOf(false) }
    var flashRelitBaseBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // BLUR
    var blurBrushSize by remember { mutableFloatStateOf(28f) }
    var blurStrength by remember { mutableFloatStateOf(12f) }
    val blurStrokes = remember { mutableStateListOf<List<PointF>>() }
    var currentBlurStroke by remember { mutableStateOf<List<PointF>>(emptyList()) }

    // ERASE / REMOVE
    var eraseBrushSize by remember { mutableFloatStateOf(32f) }
    val eraseStrokes = remember { mutableStateListOf<List<PointF>>() }
    var currentEraseStroke by remember { mutableStateOf<List<PointF>>(emptyList()) }

    // PERSPECTIVE
    var perspectiveCorners by remember {
        mutableStateOf(
            listOf(
                PointF(0.12f, 0.12f), // TL
                PointF(0.88f, 0.12f), // TR
                PointF(0.88f, 0.88f), // BR
                PointF(0.12f, 0.88f)  // BL
            )
        )
    }

    // BACKGROUND REMOVE
    var bgOptionType by remember { mutableStateOf(BgOptionType.TRANSPARENT) }
    var bgSolidColor by remember { mutableIntStateOf(android.graphics.Color.WHITE) }
    var bgGradientColor1 by remember { mutableIntStateOf(android.graphics.Color.parseColor("#3B82F6")) }
    var bgGradientColor2 by remember { mutableIntStateOf(android.graphics.Color.parseColor("#EC4899")) }
    var bgGradientDirection by remember { mutableStateOf(GradientDirection.VERTICAL) }
    var bgCustomImage by remember { mutableStateOf<Bitmap?>(null) }
    var extractedSubjectBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bgOriginalMask by remember { mutableStateOf<FloatArray?>(null) }
    var bgWorkingMask by remember { mutableStateOf<FloatArray?>(null) }
    var bgMaskWidth by remember { mutableIntStateOf(0) }
    var bgMaskHeight by remember { mutableIntStateOf(0) }
    var bgOriginalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isBgModelProcessing by remember { mutableStateOf(false) }
    var bgModelProgressMessage by remember { mutableStateOf("Removing background with BiRefNet AI...") }
    var bgSubTab by remember { mutableIntStateOf(0) }
    var bgSubjectTranslationX by remember { mutableFloatStateOf(0f) }
    var bgSubjectTranslationY by remember { mutableFloatStateOf(0f) }
    var bgSubjectScale by remember { mutableFloatStateOf(1f) }
    var bgSubjectRotation by remember { mutableFloatStateOf(0f) }
    var bgRefineMode by remember { mutableStateOf(BgRefineMode.NONE) }
    var bgRefineBrushSize by remember { mutableFloatStateOf(28f) }
    var bgRefineIsSoft by remember { mutableStateOf(false) }
    val bgRefineStrokes = remember { mutableStateListOf<MaskStroke>() }
    val bgRefineRedoStrokes = remember { mutableStateListOf<MaskStroke>() }
    var showCustomBgColorPicker by remember { mutableStateOf(false) }
    var showCustomTextColorPicker by remember { mutableStateOf(false) }
    var textSubTab by remember { mutableIntStateOf(0) }

    val updateBgComposite: (Bitmap?) -> Unit = { subject ->
        val sub = subject ?: extractedSubjectBitmap
        if (sub != null) {
            previewBitmap = PhotoEditorEngine.compositeSubjectOverBackground(
                subjectBitmap = sub,
                bgType = bgOptionType,
                solidColor = bgSolidColor,
                gradientColor1 = bgGradientColor1,
                gradientColor2 = bgGradientColor2,
                gradientDirection = bgGradientDirection,
                bgImage = bgCustomImage,
                subjectTranslationX = bgSubjectTranslationX,
                subjectTranslationY = bgSubjectTranslationY,
                subjectScale = bgSubjectScale,
                subjectRotation = bgSubjectRotation
            )
        }
    }

    val runBiRefNetBgRemoval: () -> Unit = {
        val curr = currentBitmap
        if (curr != null) {
            activeTool = PhotoEditTool.BG_REMOVE
            bgOriginalBitmap = curr
            isBgModelProcessing = true
            bgModelProgressMessage = "Removing background with BiRefNet Lite AI..."
            bgSubjectTranslationX = 0f
            bgSubjectTranslationY = 0f
            bgSubjectScale = 1f
            bgSubjectRotation = 0f
            bgRefineStrokes.clear()
            bgRefineRedoStrokes.clear()
            bgSubTab = 0
            bgOptionType = BgOptionType.TRANSPARENT
            coroutineScope.launch(Dispatchers.Default) {
                val res = BiRefNetLiteEngine.removeBackground(context.applicationContext, curr)
                withContext(Dispatchers.Main) {
                    if (res.isSuccess) {
                        val seg = res.getOrThrow()
                        bgOriginalMask = seg.alphaMask.clone()
                        bgWorkingMask = seg.alphaMask.clone()
                        bgMaskWidth = seg.maskWidth
                        bgMaskHeight = seg.maskHeight
                        extractedSubjectBitmap = seg.transparentForeground
                        updateBgComposite(seg.transparentForeground)
                    } else {
                        val err = res.exceptionOrNull()
                        val errMsg = err?.localizedMessage ?: err?.message ?: "Background removal failed"
                        Toast.makeText(context, "BiRefNet Lite error: $errMsg", Toast.LENGTH_LONG).show()
                    }
                    isBgModelProcessing = false
                }
            }
        }
    }

    // Live GPU ColorFilter for zero-latency, 120fps slider response for Brightness, Contrast & Saturation
    val liveColorFilter = remember(activeTool, brightnessValue, contrastValue, saturationValue) {
        if (activeTool == PhotoEditTool.BRIGHTNESS || activeTool == PhotoEditTool.CONTRAST || activeTool == PhotoEditTool.SATURATION) {
            val b = if (activeTool == PhotoEditTool.BRIGHTNESS) brightnessValue / 100f else 0f
            val c = if (activeTool == PhotoEditTool.CONTRAST) 1f + (contrastValue / 100f) else 1f
            val s = if (activeTool == PhotoEditTool.SATURATION) 1f + (saturationValue / 100f) else 1f

            val cm = ColorMatrix()
            cm.setToSaturation(s.coerceAtLeast(0f))
            val scale = c
            val translate = b * 255f + (1f - scale) * 128f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.timesAssign(contrastMatrix)
            ColorFilter.colorMatrix(cm)
        } else {
            null
        }
    }

    // EYE COMPARISON
    var isHoldingEyeComparison by remember { mutableStateOf(false) }

    // TEXT
    val textItems = remember { mutableStateListOf<TextItem>() }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var showEditSelectedTextDialog by remember { mutableStateOf(false) }
    var editingTextDraft by remember { mutableStateOf("") }
    var selectedFontCategory by remember { mutableStateOf("English") }

    // DRAWING
    var drawBrushSize by remember { mutableFloatStateOf(10f) }
    var drawOpacity by remember { mutableFloatStateOf(1f) }
    var drawColor by remember { mutableStateOf(android.graphics.Color.RED) }
    var isDrawEraser by remember { mutableStateOf(false) }
    var isColorPickerActive by remember { mutableStateOf(false) }
    val drawStrokes = remember { mutableStateListOf<DrawingStroke>() }
    val drawRedoStrokes = remember { mutableStateListOf<DrawingStroke>() }
    var isDrawingPanMode by remember { mutableStateOf(false) }
    var currentDrawStrokePoints by remember { mutableStateOf<List<PointF>>(emptyList()) }

    fun undoDrawingStroke() {
        if (drawStrokes.isNotEmpty()) {
            val stroke = drawStrokes.removeLast()
            drawRedoStrokes.add(stroke)
        }
    }

    fun redoDrawingStroke() {
        if (drawRedoStrokes.isNotEmpty()) {
            val stroke = drawRedoStrokes.removeLast()
            drawStrokes.add(stroke)
        }
    }

    fun clearDrawingStrokes() {
        drawStrokes.clear()
        drawRedoStrokes.clear()
    }

    // Helper: Push state to Undo History
    fun pushUndoHistory() {
        val curr = currentBitmap ?: return
        if (undoStack.size >= 15) {
            val oldest = undoStack.removeAt(0)
            if (oldest != originalBitmap && oldest != currentBitmap && !oldest.isRecycled) {
                oldest.recycle()
            }
        }
        undoStack.add(curr.copy(Bitmap.Config.ARGB_8888, true))
        redoStack.clear()
    }

    // Helper: Perform Undo
    fun performUndo() {
        if (undoStack.isEmpty()) return
        val current = currentBitmap ?: return
        redoStack.add(current)
        currentBitmap = undoStack.removeLast()
        flashRelitBaseBitmap?.recycle()
        flashRelitBaseBitmap = null
        previewBitmap = null
        activeTool = PhotoEditTool.NONE
    }

    // Helper: Perform Redo
    fun performRedo() {
        if (redoStack.isEmpty()) return
        val current = currentBitmap ?: return
        undoStack.add(current)
        currentBitmap = redoStack.removeLast()
        flashRelitBaseBitmap?.recycle()
        flashRelitBaseBitmap = null
        previewBitmap = null
        activeTool = PhotoEditTool.NONE
    }

    // Helper: Perform Reset to exact Original
    fun performReset() {
        val orig = originalBitmap ?: return
        undoStack.clear()
        redoStack.clear()
        currentBitmap = orig.copy(Bitmap.Config.ARGB_8888, true)
        flashRelitBaseBitmap?.recycle()
        flashRelitBaseBitmap = null
        flashIntensity = 60f
        previewBitmap = null
        extractedSubjectBitmap = null
        bgCustomImage = null
        bgOptionType = BgOptionType.TRANSPARENT
        brightnessValue = 0f
        contrastValue = 0f
        saturationValue = 0f
        enhanceValue = 0f
        sharpenValue = 0f
        textItems.clear()
        drawStrokes.clear()
        drawRedoStrokes.clear()
        isDrawingPanMode = false
        activeTool = PhotoEditTool.NONE
        Toast.makeText(context, "Restored to original image", Toast.LENGTH_SHORT).show()
    }

    // Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    isProcessing = true
                    val stream = context.contentResolver.openInputStream(uri)
                    val loaded = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (loaded != null) {
                        withContext(Dispatchers.Main) {
                            originalBitmap = loaded
                            currentBitmap = loaded.copy(Bitmap.Config.ARGB_8888, true)
                            flashRelitBaseBitmap?.recycle()
                            flashRelitBaseBitmap = null
                            flashIntensity = 60f
                            undoStack.clear()
                            redoStack.clear()
                            previewBitmap = null
                            extractedSubjectBitmap = null
                            activeTool = PhotoEditTool.NONE
                            isProcessing = false
                        }
                    }
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        Toast.makeText(context, "Failed to load photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Background Image Picker launcher (for BG_REMOVE image background)
    val bgImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val loaded = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (loaded != null) {
                        withContext(Dispatchers.Main) {
                            bgCustomImage = loaded
                            bgOptionType = BgOptionType.IMAGE
                            val subject = extractedSubjectBitmap ?: currentBitmap
                            if (subject != null) {
                                previewBitmap = PhotoEditorEngine.compositeSubjectOverBackground(
                                    subjectBitmap = subject,
                                    bgType = BgOptionType.IMAGE,
                                    bgImage = loaded
                                )
                            }
                        }
                    }
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load background image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Save final edited image
    fun saveFinalEditedImage() {
        val finalBmp = currentBitmap ?: return
        isProcessing = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // If there are uncommitted text or drawing items, burn them into final image
                var outputBmp = finalBmp
                if (textItems.isNotEmpty()) {
                    outputBmp = PhotoEditorEngine.burnText(outputBmp, textItems)
                }
                if (drawStrokes.isNotEmpty()) {
                    outputBmp = PhotoEditorEngine.burnDrawing(outputBmp, drawStrokes)
                }

                val hasAlpha = outputBmp.hasAlpha()
                val format = if (hasAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val extension = if (hasAlpha) "png" else "jpg"
                val mime = if (hasAlpha) "image/png" else "image/jpeg"

                val tempFile = File(context.cacheDir, "edited_photo_${System.currentTimeMillis()}.$extension")
                FileOutputStream(tempFile).use { out ->
                    outputBmp.compress(format, 95, out)
                }

                val saveResult = GlobalSaveManager.saveProcessedFile(
                    context = context,
                    tempSourceFile = tempFile,
                    desiredFileName = "EditedPhoto_${System.currentTimeMillis()}.$extension",
                    mimeType = mime,
                    category = "photo_edit",
                    tag = "Edited Photo"
                )

                withContext(Dispatchers.Main) {
                    isProcessing = false
                    lastSaveResult = saveResult
                    showSavedDialog = true
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    Toast.makeText(context, "Failed to save photo: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // System Back & On-Screen Back Handler
    val handleBackPress: () -> Unit = {
        when {
            showResetConfirmDialog -> showResetConfirmDialog = false
            showSavedDialog -> showSavedDialog = false
            showEditSelectedTextDialog -> showEditSelectedTextDialog = false
            showCustomBgColorPicker -> showCustomBgColorPicker = false
            showCustomTextColorPicker -> showCustomTextColorPicker = false
            isColorPickerActive -> isColorPickerActive = false
            activeTool != PhotoEditTool.NONE -> {
                previewBitmap = null
                extractedSubjectBitmap = null
                brightnessValue = 0f
                contrastValue = 0f
                saturationValue = 0f
                enhanceValue = 0f
                sharpenValue = 0f
                activeTool = PhotoEditTool.NONE
            }
            else -> onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackPress()
    }

    // -------------------------------------------------------------
    // Main Layout
    // -------------------------------------------------------------
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("photo_editing_screen"),
        color = DarkBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ---------------------------------------------------------
            // Top Bar: Back, Undo, Redo, Reset, Save
            // ---------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBarBg)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = handleBackPress, modifier = Modifier.testTag("photo_editor_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Photo Editor",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // UNDO
                    IconButton(
                        onClick = {
                            if (activeTool == PhotoEditTool.DRAWING) {
                                undoDrawingStroke()
                            } else {
                                performUndo()
                            }
                        },
                        enabled = if (activeTool == PhotoEditTool.DRAWING) drawStrokes.isNotEmpty() else undoStack.isNotEmpty(),
                        modifier = Modifier.testTag("photo_editor_undo_button")
                    ) {
                        val isUndoEnabled = if (activeTool == PhotoEditTool.DRAWING) drawStrokes.isNotEmpty() else undoStack.isNotEmpty()
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (isUndoEnabled) Color.White else Color(0xFF64748B)
                        )
                    }

                    // REDO
                    IconButton(
                        onClick = {
                            if (activeTool == PhotoEditTool.DRAWING) {
                                redoDrawingStroke()
                            } else {
                                performRedo()
                            }
                        },
                        enabled = if (activeTool == PhotoEditTool.DRAWING) drawRedoStrokes.isNotEmpty() else redoStack.isNotEmpty(),
                        modifier = Modifier.testTag("photo_editor_redo_button")
                    ) {
                        val isRedoEnabled = if (activeTool == PhotoEditTool.DRAWING) drawRedoStrokes.isNotEmpty() else redoStack.isNotEmpty()
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (isRedoEnabled) Color.White else Color(0xFF64748B)
                        )
                    }

                    // RESET
                    IconButton(
                        onClick = { showResetConfirmDialog = true },
                        enabled = originalBitmap != null && (undoStack.isNotEmpty() || currentBitmap != originalBitmap || textItems.isNotEmpty() || drawStrokes.isNotEmpty()),
                        modifier = Modifier.testTag("photo_editor_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (originalBitmap != null && (undoStack.isNotEmpty() || currentBitmap != originalBitmap)) AccentPink else Color(0xFF64748B)
                        )
                    }

                    // EYE BUTTON (Hold to compare with Original Photo)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("photo_editor_eye_compare_button")
                            .clip(CircleShape)
                            .background(if (isHoldingEyeComparison) CrimsonPrimary.copy(alpha = 0.25f) else Color.Transparent)
                            .pointerInput(originalBitmap) {
                                if (originalBitmap == null) return@pointerInput
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        when (event.type) {
                                            PointerEventType.Press -> {
                                                isHoldingEyeComparison = true
                                            }
                                            PointerEventType.Release, PointerEventType.Unknown -> {
                                                isHoldingEyeComparison = false
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isHoldingEyeComparison) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Hold to view original photo",
                            tint = if (isHoldingEyeComparison) AccentPink else if (originalBitmap != null) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // LAYERS BUTTON
                    IconButton(
                        onClick = { showLayerManagerDialog = true },
                        modifier = Modifier.testTag("photo_editor_layers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Layers",
                            tint = if (textItems.isNotEmpty()) Color(0xFF60A5FA) else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // SAVE PHOTO
                    Button(
                        onClick = { saveFinalEditedImage() },
                        enabled = currentBitmap != null && !isProcessing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.testTag("photo_editor_save_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // ---------------------------------------------------------
            // Center Stage / Viewport
            // ---------------------------------------------------------
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .pointerInput(activeTool, isDrawingPanMode) {
                        if (activeTool == PhotoEditTool.DRAWING && !isDrawingPanMode) {
                            // Two-finger pinch and pan only while drawing, so single-finger touches draw with 100% precision without panning the image away
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedPointers = event.changes.filter { it.pressed }
                                    if (pressedPointers.size >= 2) {
                                        val pan = event.calculatePan()
                                        val zoom = event.calculateZoom()
                                        if (zoom != 1f || pan != Offset.Zero) {
                                            zoomScale = (zoomScale * zoom).coerceIn(0.5f, 10f)
                                            panOffset += pan
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        } else {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(0.5f, 10f)
                                panOffset += pan
                            }
                        }
                    }
                    .pointerInput(activeTool) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (zoomScale > 1.05f || panOffset != Offset.Zero) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    zoomScale = 2.5f
                                    panOffset = Offset.Zero
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val displayBmp = if (isHoldingEyeComparison && originalBitmap != null) {
                    originalBitmap
                } else {
                    previewBitmap ?: currentBitmap
                }

                // Floating Zoom indicator & quick reset button when zoomed in
                if (zoomScale > 1.05f || panOffset != Offset.Zero) {
                    Surface(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${(zoomScale * 100).roundToInt()}%",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Zoom to 100%",
                                    tint = AccentPink,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                if (displayBmp != null) {
                    val containerW = maxWidth.value
                    val containerH = maxHeight.value
                    val pad = 12f
                    val availW = (containerW - pad * 2f).coerceAtLeast(10f)
                    val availH = (containerH - pad * 2f).coerceAtLeast(10f)

                    val bmpW = displayBmp.width.toFloat()
                    val bmpH = displayBmp.height.toFloat()
                    val scale = minOf(availW / bmpW, availH / bmpH)
                    val displayW = (bmpW * scale).dp
                    val displayH = (bmpH * scale).dp

                    Box(
                        modifier = Modifier
                            .size(displayW, displayH)
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Checkerboard pattern behind transparent background
                        if (activeTool == PhotoEditTool.BG_REMOVE && bgOptionType == BgOptionType.TRANSPARENT && !isHoldingEyeComparison) {
                            CheckerboardPattern(modifier = Modifier.fillMaxSize())
                        }

                        // Image Display
                        Image(
                            bitmap = displayBmp.asImageBitmap(),
                            contentDescription = "Photo being edited",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = if (isHoldingEyeComparison) null else liveColorFilter
                        )

                        if (isBgModelProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = CrimsonPrimary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                                    Text(bgModelProgressMessage, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        if (isFlashProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = Color(0xFFFFD54F), strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                                    Text("Turning on flash...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Interactive Overlays depending on active tool (hidden during Eye comparison)
                        if (!isHoldingEyeComparison && !isBgModelProcessing && !isFlashProcessing) {
                            when (activeTool) {
                                PhotoEditTool.CROP -> {
                                    CropOverlay(
                                        cropRect = cropRect,
                                        onCropRectChange = { cropRect = it },
                                        aspectPreset = cropPreset
                                    )
                                }
                                PhotoEditTool.PERSPECTIVE -> {
                                    PerspectiveOverlay(
                                        corners = perspectiveCorners,
                                        onCornersChange = { perspectiveCorners = it }
                                    )
                                }
                                PhotoEditTool.BLUR -> {
                                    TouchStrokeCanvas(
                                        strokes = blurStrokes,
                                        currentStroke = currentBlurStroke,
                                        strokeColor = Color(0x6600E5FF),
                                        brushSizeDp = blurBrushSize,
                                        onStrokePointAdded = { currentBlurStroke = currentBlurStroke + it },
                                        onStrokeCompleted = {
                                            if (currentBlurStroke.isNotEmpty()) {
                                                blurStrokes.add(currentBlurStroke)
                                                currentBlurStroke = emptyList()
                                            }
                                        }
                                    )
                                }
                                PhotoEditTool.ERASE -> {
                                    TouchStrokeCanvas(
                                        strokes = eraseStrokes,
                                        currentStroke = currentEraseStroke,
                                        strokeColor = Color(0x66E11D48),
                                        brushSizeDp = eraseBrushSize,
                                        onStrokePointAdded = { currentEraseStroke = currentEraseStroke + it },
                                        onStrokeCompleted = {
                                            if (currentEraseStroke.isNotEmpty()) {
                                                eraseStrokes.add(currentEraseStroke)
                                                currentEraseStroke = emptyList()
                                            }
                                        }
                                    )
                                }
                                PhotoEditTool.BG_REMOVE -> {
                                    if (bgRefineMode != BgRefineMode.NONE) {
                                        RefineMaskOverlay(
                                            refineMode = bgRefineMode,
                                            brushSizeDp = bgRefineBrushSize,
                                            isSoftBrush = bgRefineIsSoft,
                                            onStrokeCompleted = { stroke ->
                                                bgRefineStrokes.add(stroke)
                                                bgRefineRedoStrokes.clear()
                                                val orig = bgOriginalBitmap ?: currentBitmap ?: return@RefineMaskOverlay
                                                var mask = bgWorkingMask
                                                var w = bgMaskWidth
                                                var h = bgMaskHeight
                                                if (mask == null || w <= 0 || h <= 0) {
                                                    w = orig.width
                                                    h = orig.height
                                                    bgMaskWidth = w
                                                    bgMaskHeight = h
                                                    mask = FloatArray(w * h) { 1.0f }
                                                    bgWorkingMask = mask
                                                    bgOriginalMask = mask.clone()
                                                }
                                                val radPx = stroke.radiusPx * (w.toFloat() / 320f).coerceAtLeast(1f)
                                                for (pt in stroke.points) {
                                                    BiRefNetLiteEngine.applyBrushToMask(
                                                        mask = mask,
                                                        width = w,
                                                        height = h,
                                                        centerX = pt.x * w,
                                                        centerY = pt.y * h,
                                                        radius = radPx,
                                                        isRestore = stroke.isRestore,
                                                        isSoft = stroke.isSoft
                                                    )
                                                }
                                                val refinedSub = BiRefNetLiteEngine.createRefinedForegroundBitmap(orig, mask, w, h)
                                                if (!stroke.isRestore && !stroke.isSoft) {
                                                    PhotoEditorEngine.applyPorterDuffClear(
                                                        bitmap = refinedSub,
                                                        points = stroke.points,
                                                        radiusPx = radPx
                                                    )
                                                }
                                                extractedSubjectBitmap = refinedSub
                                                updateBgComposite(refinedSub)
                                            }
                                        )
                                    } else if (bgSubTab == 2) {
                                        ForegroundTransformOverlay(
                                            translationX = bgSubjectTranslationX,
                                            translationY = bgSubjectTranslationY,
                                            scale = bgSubjectScale,
                                            rotation = bgSubjectRotation,
                                            onTransformChange = { tx, ty, sc, rot ->
                                                bgSubjectTranslationX = tx
                                                bgSubjectTranslationY = ty
                                                bgSubjectScale = sc
                                                bgSubjectRotation = rot
                                                updateBgComposite(extractedSubjectBitmap)
                                            }
                                        )
                                    }
                                }
                                PhotoEditTool.DRAWING -> {
                                    if (isColorPickerActive) {
                                        EyedropperOverlay(
                                            bitmap = displayBmp,
                                            currentColor = drawColor,
                                            onColorSampled = { sampled ->
                                                drawColor = sampled
                                            },
                                            onColorConfirmed = { sampled ->
                                                drawColor = sampled
                                                isColorPickerActive = false
                                                isDrawEraser = false
                                                isDrawingPanMode = false
                                            }
                                        )
                                    } else {
                                        DrawingOverlayCanvas(
                                            strokes = drawStrokes,
                                            currentStrokePoints = currentDrawStrokePoints,
                                            currentColor = drawColor,
                                            currentBrushSizeDp = drawBrushSize,
                                            currentOpacity = drawOpacity,
                                            isEraser = isDrawEraser,
                                            isPanMode = isDrawingPanMode,
                                            onStrokePointAdded = { currentDrawStrokePoints = currentDrawStrokePoints + it },
                                            onStrokeCompleted = {
                                                if (currentDrawStrokePoints.isNotEmpty()) {
                                                    drawStrokes.add(
                                                        DrawingStroke(
                                                            points = currentDrawStrokePoints,
                                                            color = drawColor,
                                                            brushSizePx = drawBrushSize,
                                                            opacity = drawOpacity,
                                                            isEraser = isDrawEraser
                                                        )
                                                    )
                                                    drawRedoStrokes.clear()
                                                    currentDrawStrokePoints = emptyList()
                                                }
                                            },
                                            onStrokeCancelled = {
                                                currentDrawStrokePoints = emptyList()
                                            }
                                        )
                                    }
                                }
                                else -> {}
                            }

                            // Persistent, Non-Destructive Text Layers (Always rendered over photo, editable anytime)
                            if (textItems.isNotEmpty() || activeTool == PhotoEditTool.TEXT) {
                                TextOverlayCanvas(
                                    textItems = textItems,
                                    selectedTextId = if (activeTool == PhotoEditTool.TEXT) selectedTextId else null,
                                    onSelectText = { id ->
                                        if (id.isNotEmpty()) {
                                            activeTool = PhotoEditTool.TEXT
                                            selectedTextId = id
                                            val itm = textItems.find { it.id == id }
                                            if (itm != null) editingTextDraft = itm.text
                                        } else {
                                            selectedTextId = null
                                        }
                                    },
                                    onUpdateTextItem = { updatedItem ->
                                        val idx = textItems.indexOfFirst { it.id == updatedItem.id }
                                        if (idx >= 0) {
                                            textItems[idx] = updatedItem
                                        }
                                    },
                                    onEditText = { item ->
                                        activeTool = PhotoEditTool.TEXT
                                        selectedTextId = item.id
                                        editingTextDraft = item.text
                                        textSubTab = 0
                                    },
                                    onDeleteText = { id ->
                                        pushUndoHistory()
                                        textItems.removeAll { it.id == id }
                                        if (selectedTextId == id) selectedTextId = null
                                    },
                                    onStartTransform = {
                                        pushUndoHistory()
                                    }
                                )
                            }
                        } else {
                            // Floating Pill indicating Original Photo is being viewed
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .background(Color(0xEE090D16), RoundedCornerShape(20.dp))
                                    .border(1.5.dp, AccentPink, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = AccentPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "ORIGINAL PHOTO PREVIEW",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty State: Prompt user to pick a photo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(DarkCardSurface)
                                .border(1.dp, DarkBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Photo Selected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Import a photo from your gallery to begin editing with professional tools.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("select_photo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Photo", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CrimsonPrimary)
                    }
                }
            }

            // ---------------------------------------------------------
            // Active Tool Control Panel (if a tool is active)
            // ---------------------------------------------------------
            if (currentBitmap != null && activeTool != PhotoEditTool.NONE) {
                Surface(
                    color = DarkCardSurface,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Tool Title and Cancel/Apply Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeTool.displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        if (activeTool == PhotoEditTool.BG_REMOVE) {
                                            val orig = bgOriginalBitmap
                                            if (orig != null) {
                                                currentBitmap = orig
                                            }
                                            bgOriginalBitmap = null
                                            bgOriginalMask = null
                                            bgWorkingMask = null
                                            bgRefineStrokes.clear()
                                            bgRefineRedoStrokes.clear()
                                            bgSubTab = 0
                                        }
                                        if (activeTool == PhotoEditTool.DRAWING) {
                                            drawStrokes.clear()
                                            drawRedoStrokes.clear()
                                            currentDrawStrokePoints = emptyList()
                                            isDrawingPanMode = false
                                        }
                                        if (activeTool == PhotoEditTool.FLASH) {
                                            flashRelitBaseBitmap?.recycle()
                                            flashRelitBaseBitmap = null
                                            flashIntensity = 60f
                                        }
                                        previewBitmap = null
                                        extractedSubjectBitmap = null
                                        brightnessValue = 0f
                                        contrastValue = 0f
                                        saturationValue = 0f
                                        enhanceValue = 0f
                                        sharpenValue = 0f
                                        activeTool = PhotoEditTool.NONE
                                    }
                                ) {
                                    Text("Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                }
                                Button(
                                    onClick = {
                                        val curr = currentBitmap ?: return@Button
                                        pushUndoHistory()
                                        when (activeTool) {
                                            PhotoEditTool.CROP -> {
                                                currentBitmap = PhotoEditorEngine.cropBitmap(curr, cropRect)
                                                cropRect = RectF(0.05f, 0.05f, 0.95f, 0.95f)
                                            }
                                            PhotoEditTool.RESIZE -> {
                                                val targetW = resizeWidthText.toIntOrNull() ?: curr.width
                                                val targetH = resizeHeightText.toIntOrNull() ?: curr.height
                                                currentBitmap = PhotoEditorEngine.resizeBitmap(curr, targetW, targetH)
                                            }
                                            PhotoEditTool.BRIGHTNESS, PhotoEditTool.CONTRAST, PhotoEditTool.SATURATION -> {
                                                val bright = brightnessValue / 100f
                                                val contr = 1f + (contrastValue / 100f)
                                                val sat = 1f + (saturationValue / 100f)
                                                currentBitmap = PhotoEditorEngine.applyColorAdjustments(curr, bright, contr, sat)
                                                brightnessValue = 0f
                                                contrastValue = 0f
                                                saturationValue = 0f
                                            }
                                            PhotoEditTool.ENHANCE, PhotoEditTool.SHARPEN -> {
                                                val enh = enhanceValue / 100f
                                                val shp = sharpenValue / 100f
                                                currentBitmap = PhotoEditorEngine.applyEnhanceAndSharpen(curr, enh, shp)
                                                enhanceValue = 0f
                                                sharpenValue = 0f
                                            }
                                            PhotoEditTool.FLASH -> {
                                                val relit = flashRelitBaseBitmap
                                                if (relit != null) {
                                                    currentBitmap = previewBitmap ?: FaceRelightEngine.blendRelit(curr, relit, flashIntensity / 100f)
                                                    relit.recycle()
                                                }
                                                flashRelitBaseBitmap = null
                                                flashIntensity = 60f
                                            }
                                            PhotoEditTool.BLUR -> {
                                                currentBitmap = PhotoEditorEngine.applySelectiveBlur(
                                                    bitmap = curr,
                                                    strokes = blurStrokes,
                                                    brushRadiusPx = blurBrushSize * (curr.width / 400f),
                                                    blurRadius = blurStrength.roundToInt().coerceIn(1, 25)
                                                )
                                                blurStrokes.clear()
                                            }
                                            PhotoEditTool.ERASE -> {
                                                currentBitmap = PhotoEditorEngine.applyObjectRemoval(
                                                    bitmap = curr,
                                                    strokes = eraseStrokes,
                                                    brushRadiusPx = eraseBrushSize * (curr.width / 400f)
                                                )
                                                eraseStrokes.clear()
                                            }
                                            PhotoEditTool.PERSPECTIVE -> {
                                                currentBitmap = PhotoEditorEngine.applyPerspectiveCorrection(curr, perspectiveCorners)
                                            }
                                            PhotoEditTool.BG_REMOVE -> {
                                                val subject = extractedSubjectBitmap ?: curr
                                                currentBitmap = previewBitmap ?: PhotoEditorEngine.compositeSubjectOverBackground(
                                                    subjectBitmap = subject,
                                                    bgType = bgOptionType,
                                                    solidColor = bgSolidColor,
                                                    gradientColor1 = bgGradientColor1,
                                                    gradientColor2 = bgGradientColor2,
                                                    gradientDirection = bgGradientDirection,
                                                    bgImage = bgCustomImage,
                                                    subjectTranslationX = bgSubjectTranslationX,
                                                    subjectTranslationY = bgSubjectTranslationY,
                                                    subjectScale = bgSubjectScale,
                                                    subjectRotation = bgSubjectRotation
                                                )
                                                extractedSubjectBitmap = null
                                                bgOriginalBitmap = null
                                                bgOriginalMask = null
                                                bgWorkingMask = null
                                                bgRefineStrokes.clear()
                                                bgRefineRedoStrokes.clear()
                                                bgSubTab = 0
                                            }
                                            PhotoEditTool.TEXT -> {
                                                selectedTextId = null
                                            }
                                            PhotoEditTool.DRAWING -> {
                                                currentBitmap = PhotoEditorEngine.burnDrawing(curr, drawStrokes)
                                                drawStrokes.clear()
                                                drawRedoStrokes.clear()
                                                isDrawingPanMode = false
                                            }
                                            else -> {}
                                        }
                                        previewBitmap = null
                                        activeTool = PhotoEditTool.NONE
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Specific Tool Controls
                        when (activeTool) {
                            PhotoEditTool.CROP -> {
                                Text("Select aspect ratio preset or drag crop handles freely:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                AspectRatioChips(
                                    selectedPreset = cropPreset,
                                    onPresetSelected = { preset ->
                                        cropPreset = preset
                                        val r = preset.ratio
                                        if (r != null) {
                                            val curr = currentBitmap ?: return@AspectRatioChips
                                            val imgRatio = curr.width.toFloat() / curr.height.toFloat()
                                            if (r > imgRatio) {
                                                val newH = (0.8f * (imgRatio / r)).coerceIn(0.2f, 0.9f)
                                                cropRect = RectF(0.1f, (1f - newH) / 2f, 0.9f, (1f + newH) / 2f)
                                            } else {
                                                val newW = (0.8f * (r / imgRatio)).coerceIn(0.2f, 0.9f)
                                                cropRect = RectF((1f - newW) / 2f, 0.1f, (1f + newW) / 2f, 0.9f)
                                            }
                                        }
                                    }
                                )
                            }

                            PhotoEditTool.RESIZE -> {
                                val curr = currentBitmap
                                if (curr != null) {
                                    LaunchedEffect(curr) {
                                        if (resizeWidthText.isEmpty()) resizeWidthText = curr.width.toString()
                                        if (resizeHeightText.isEmpty()) resizeHeightText = curr.height.toString()
                                    }
                                    Text("Current Dimensions: ${curr.width} × ${curr.height} px", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = resizeWidthText,
                                            onValueChange = { newW ->
                                                resizeWidthText = newW.filter { it.isDigit() }
                                                if (maintainAspectRatio) {
                                                    val w = resizeWidthText.toIntOrNull()
                                                    if (w != null && curr.width > 0) {
                                                        resizeHeightText = ((w.toFloat() / curr.width.toFloat()) * curr.height).roundToInt().toString()
                                                    }
                                                }
                                            },
                                            label = { Text("Width (px)", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = CrimsonPrimary,
                                                unfocusedBorderColor = DarkBorder
                                            )
                                        )
                                        OutlinedTextField(
                                            value = resizeHeightText,
                                            onValueChange = { newH ->
                                                resizeHeightText = newH.filter { it.isDigit() }
                                                if (maintainAspectRatio) {
                                                    val h = resizeHeightText.toIntOrNull()
                                                    if (h != null && curr.height > 0) {
                                                        resizeWidthText = ((h.toFloat() / curr.height.toFloat()) * curr.width).roundToInt().toString()
                                                    }
                                                }
                                            },
                                            label = { Text("Height (px)", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = CrimsonPrimary,
                                                unfocusedBorderColor = DarkBorder
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Maintain Aspect Ratio", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                        Switch(
                                            checked = maintainAspectRatio,
                                            onCheckedChange = { maintainAspectRatio = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                                        )
                                    }
                                }
                            }

                            PhotoEditTool.BRIGHTNESS -> {
                                Text("Brightness: ${brightnessValue.roundToInt()}%", color = Color.White, fontSize = 13.sp)
                                Slider(
                                    value = brightnessValue,
                                    onValueChange = { brightnessValue = it },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                )
                            }

                            PhotoEditTool.CONTRAST -> {
                                Text("Contrast: ${contrastValue.roundToInt()}%", color = Color.White, fontSize = 13.sp)
                                Slider(
                                    value = contrastValue,
                                    onValueChange = { contrastValue = it },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                )
                            }

                            PhotoEditTool.SATURATION -> {
                                Text("Color Intensity: ${(100 + saturationValue).roundToInt()}%", color = Color.White, fontSize = 13.sp)
                                Slider(
                                    value = saturationValue,
                                    onValueChange = { saturationValue = it },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                )
                            }

                            PhotoEditTool.ENHANCE -> {
                                Text("Enhance Clarity: ${enhanceValue.roundToInt()}%", color = Color.White, fontSize = 13.sp)
                                Slider(
                                    value = enhanceValue,
                                    onValueChange = { enhanceValue = it },
                                    onValueChangeFinished = {
                                        val curr = currentBitmap ?: return@Slider
                                        previewBitmap = PhotoEditorEngine.applyEnhanceAndSharpen(curr, enhance = enhanceValue / 100f, sharpen = sharpenValue / 100f)
                                    },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                )
                            }

                            PhotoEditTool.SHARPEN -> {
                                Text("Sharpen Details: ${sharpenValue.roundToInt()}%", color = Color.White, fontSize = 13.sp)
                                Slider(
                                    value = sharpenValue,
                                    onValueChange = { sharpenValue = it },
                                    onValueChangeFinished = {
                                        val curr = currentBitmap ?: return@Slider
                                        previewBitmap = PhotoEditorEngine.applyEnhanceAndSharpen(curr, enhance = enhanceValue / 100f, sharpen = sharpenValue / 100f)
                                    },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                )
                            }

                            PhotoEditTool.FLASH -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FlashOn,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD54F),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "AI Flash Light (Face Relight)",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "${flashIntensity.roundToInt()}%",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = flashIntensity,
                                        onValueChange = {
                                            flashIntensity = it
                                            val curr = currentBitmap
                                            val relit = flashRelitBaseBitmap
                                            if (curr != null && relit != null) {
                                                previewBitmap = FaceRelightEngine.blendRelit(curr, relit, it / 100f)
                                            }
                                        },
                                        valueRange = 0f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFFD54F),
                                            activeTrackColor = Color(0xFFFFD54F),
                                            inactiveTrackColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.testTag("flash_intensity_slider")
                                    )
                                    Text(
                                        text = "Intelligently illuminates face contours, fills harsh shadows & adds specular eye catchlights.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            PhotoEditTool.BLUR -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Draw with finger over area to blur. Adjust strength below:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    if (blurStrokes.isNotEmpty()) {
                                        TextButton(onClick = { blurStrokes.clear() }) {
                                            Text("Clear", color = CrimsonPrimary, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Strength: ${blurStrength.roundToInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                                    Slider(
                                        value = blurStrength,
                                        onValueChange = { blurStrength = it },
                                        valueRange = 2f..25f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Brush Size: ${blurBrushSize.roundToInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                                    Slider(
                                        value = blurBrushSize,
                                        onValueChange = { blurBrushSize = it },
                                        valueRange = 10f..60f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                    )
                                }
                            }

                            PhotoEditTool.ERASE -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Highlight unwanted objects to erase and auto-fill:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    if (eraseStrokes.isNotEmpty()) {
                                        TextButton(onClick = { eraseStrokes.clear() }) {
                                            Text("Clear", color = CrimsonPrimary, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Brush Size: ${eraseBrushSize.roundToInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                                    Slider(
                                        value = eraseBrushSize,
                                        onValueChange = { eraseBrushSize = it },
                                        valueRange = 10f..80f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                    )
                                }
                            }

                            PhotoEditTool.PERSPECTIVE -> {
                                Text("Drag the 4 corner pins to match document borders:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            perspectiveCorners = listOf(
                                                PointF(0.05f, 0.05f),
                                                PointF(0.95f, 0.05f),
                                                PointF(0.95f, 0.95f),
                                                PointF(0.05f, 0.95f)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Full Frame", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }

                            PhotoEditTool.BG_REMOVE -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Sub-tabs row: Background, Refine, Position
                                    val bgTabs = listOf(
                                        Triple(0, "Background", Icons.Default.Palette),
                                        Triple(1, "Refine", Icons.Default.Brush),
                                        Triple(2, "Position", Icons.Default.AspectRatio)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E293B)),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        bgTabs.forEach { (idx, label, icon) ->
                                            val isSelected = bgSubTab == idx
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        bgSubTab = idx
                                                        if (idx != 1) {
                                                            bgRefineMode = BgRefineMode.NONE
                                                        }
                                                    }
                                                    .background(if (isSelected) CrimsonPrimary else Color.Transparent)
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        icon,
                                                        contentDescription = label,
                                                        tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    when (bgSubTab) {
                                        0 -> {
                                            // Background Tab (Transparent, Color, Gradient, Image)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val options = listOf(
                                                    Triple(BgOptionType.TRANSPARENT, "Transparent", Icons.Default.InvertColors),
                                                    Triple(BgOptionType.SOLID_COLOR, "Color", Icons.Default.Palette),
                                                    Triple(BgOptionType.GRADIENT, "Gradient", Icons.Default.AutoAwesome),
                                                    Triple(BgOptionType.IMAGE, "Image", Icons.Default.AddPhotoAlternate)
                                                )

                                                options.forEach { (type, label, icon) ->
                                                    val isSelected = bgOptionType == type
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            bgOptionType = type
                                                            updateBgComposite(null)
                                                        },
                                                        label = { Text(label, fontSize = 11.sp, color = if (isSelected) Color.White else Color(0xFFCBD5E1)) },
                                                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSelected) Color.White else Color(0xFF94A3B8)) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = CrimsonPrimary,
                                                            containerColor = Color(0xFF1E293B)
                                                        ),
                                                        border = FilterChipDefaults.filterChipBorder(
                                                            enabled = true,
                                                            selected = isSelected,
                                                            borderColor = if (isSelected) CrimsonPrimary else Color(0xFF334155)
                                                        )
                                                    )
                                                }
                                            }

                                            // Sub-options depending on background type
                                            when (bgOptionType) {
                                                BgOptionType.TRANSPARENT -> {
                                                    Text("Background removed with transparent alpha layer. Saves as lossless PNG.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                }
                                                BgOptionType.SOLID_COLOR -> {
                                                    val colorsList = listOf(
                                                        AndroidColor.WHITE,
                                                        AndroidColor.BLACK,
                                                        AndroidColor.parseColor("#F1F5F9"),
                                                        AndroidColor.parseColor("#3B82F6"),
                                                        AndroidColor.parseColor("#E11D48"),
                                                        AndroidColor.parseColor("#10B981"),
                                                        AndroidColor.parseColor("#F59E0B"),
                                                        AndroidColor.parseColor("#8B5CF6"),
                                                        AndroidColor.parseColor("#06B6D4")
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        colorsList.forEach { col ->
                                                            val isSelected = bgSolidColor == col
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(col))
                                                                    .border(
                                                                        width = if (isSelected) 3.dp else 1.dp,
                                                                        color = if (isSelected) CrimsonPrimary else Color(0xFF64748B),
                                                                        shape = CircleShape
                                                                    )
                                                                    .clickable {
                                                                        bgSolidColor = col
                                                                        updateBgComposite(null)
                                                                    }
                                                            )
                                                        }
                                                        // Custom Color Picker Button
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF1E293B))
                                                                .border(1.dp, Color.White, CircleShape)
                                                                .clickable { showCustomBgColorPicker = true },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Default.Palette, contentDescription = "Custom Color", tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                                BgOptionType.GRADIENT -> {
                                                    val gradientPresets = listOf(
                                                        Triple("Ocean", AndroidColor.parseColor("#1E3A8A"), AndroidColor.parseColor("#3B82F6")),
                                                        Triple("Sunset", AndroidColor.parseColor("#E11D48"), AndroidColor.parseColor("#F59E0B")),
                                                        Triple("Aurora", AndroidColor.parseColor("#065F46"), AndroidColor.parseColor("#10B981")),
                                                        Triple("Cosmic", AndroidColor.parseColor("#4C1D95"), AndroidColor.parseColor("#EC4899")),
                                                        Triple("Slate", AndroidColor.parseColor("#0F172A"), AndroidColor.parseColor("#475569"))
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        gradientPresets.forEach { (label, c1, c2) ->
                                                            val isSelected = bgGradientColor1 == c1 && bgGradientColor2 == c2
                                                            Button(
                                                                onClick = {
                                                                    bgGradientColor1 = c1
                                                                    bgGradientColor2 = c2
                                                                    updateBgComposite(null)
                                                                },
                                                                shape = RoundedCornerShape(8.dp),
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = if (isSelected) CrimsonPrimary else Color(0xFF1E293B)
                                                                ),
                                                                modifier = Modifier.height(32.dp)
                                                            ) {
                                                                Text(label, fontSize = 11.sp, color = Color.White)
                                                            }
                                                        }

                                                        // Toggle Direction
                                                        OutlinedButton(
                                                            onClick = {
                                                                val nextDir = if (bgGradientDirection == GradientDirection.VERTICAL) GradientDirection.HORIZONTAL else GradientDirection.VERTICAL
                                                                bgGradientDirection = nextDir
                                                                updateBgComposite(null)
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.height(32.dp)
                                                        ) {
                                                            Text(
                                                                if (bgGradientDirection == GradientDirection.VERTICAL) "Vertical" else "Horizontal",
                                                                fontSize = 11.sp,
                                                                color = Color(0xFFCBD5E1)
                                                            )
                                                        }
                                                    }
                                                }
                                                BgOptionType.IMAGE -> {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Button(
                                                            onClick = { bgImagePickerLauncher.launch("image/*") },
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                                            modifier = Modifier.height(34.dp)
                                                        ) {
                                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(if (bgCustomImage != null) "Change Photo" else "Select Background", fontSize = 11.sp)
                                                        }
                                                        if (bgCustomImage != null) {
                                                            Text("Custom photo applied", color = Color(0xFF10B981), fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        1 -> {
                                            // Refine Tab (Manual Brush: Erase / Restore)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    FilterChip(
                                                        selected = bgRefineMode == BgRefineMode.ERASE_BG,
                                                        onClick = { bgRefineMode = if (bgRefineMode == BgRefineMode.ERASE_BG) BgRefineMode.NONE else BgRefineMode.ERASE_BG },
                                                        label = { Text("Erase", fontSize = 11.sp, color = if (bgRefineMode == BgRefineMode.ERASE_BG) Color.White else Color(0xFFCBD5E1)) },
                                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CrimsonPrimary, containerColor = Color(0xFF1E293B))
                                                    )
                                                    FilterChip(
                                                        selected = bgRefineMode == BgRefineMode.RESTORE_SUBJECT,
                                                        onClick = { bgRefineMode = if (bgRefineMode == BgRefineMode.RESTORE_SUBJECT) BgRefineMode.NONE else BgRefineMode.RESTORE_SUBJECT },
                                                        label = { Text("Restore", fontSize = 11.sp, color = if (bgRefineMode == BgRefineMode.RESTORE_SUBJECT) Color.White else Color(0xFFCBD5E1)) },
                                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF10B981), containerColor = Color(0xFF1E293B))
                                                    )
                                                    FilterChip(
                                                        selected = bgRefineIsSoft,
                                                        onClick = { bgRefineIsSoft = !bgRefineIsSoft },
                                                        label = { Text("Soft Edge", fontSize = 11.sp, color = if (bgRefineIsSoft) Color.White else Color(0xFFCBD5E1)) },
                                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CrimsonPrimary, containerColor = Color(0xFF1E293B))
                                                    )
                                                }

                                                // Undo/Redo/Reset
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    IconButton(
                                                        onClick = {
                                                            if (bgRefineStrokes.isNotEmpty()) {
                                                                bgRefineRedoStrokes.add(bgRefineStrokes.removeLast())
                                                                val orig = bgOriginalBitmap ?: currentBitmap ?: return@IconButton
                                                                val origMask = bgOriginalMask ?: return@IconButton
                                                                val w = bgMaskWidth
                                                                val h = bgMaskHeight
                                                                if (w > 0 && h > 0 && origMask.size == w * h) {
                                                                    val m = origMask.clone()
                                                                    for (s in bgRefineStrokes) {
                                                                        for (pt in s.points) {
                                                                            BiRefNetLiteEngine.applyBrushToMask(
                                                                                mask = m,
                                                                                width = w,
                                                                                height = h,
                                                                                centerX = pt.x * w,
                                                                                centerY = pt.y * h,
                                                                                radius = s.radiusPx * (w.toFloat() / 320f).coerceAtLeast(1f),
                                                                                isRestore = s.isRestore,
                                                                                isSoft = s.isSoft
                                                                            )
                                                                        }
                                                                    }
                                                                    val refinedSub = BiRefNetLiteEngine.createRefinedForegroundBitmap(orig, m, w, h)
                                                                    bgWorkingMask = m
                                                                    extractedSubjectBitmap = refinedSub
                                                                    updateBgComposite(refinedSub)
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp),
                                                        enabled = bgRefineStrokes.isNotEmpty()
                                                    ) {
                                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Refine", tint = if (bgRefineStrokes.isNotEmpty()) Color.White else Color(0xFF475569), modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            if (bgRefineRedoStrokes.isNotEmpty()) {
                                                                val stroke = bgRefineRedoStrokes.removeLast()
                                                                bgRefineStrokes.add(stroke)
                                                                val orig = bgOriginalBitmap ?: currentBitmap ?: return@IconButton
                                                                val mask = bgWorkingMask
                                                                val w = bgMaskWidth
                                                                val h = bgMaskHeight
                                                                if (mask != null && w > 0 && h > 0) {
                                                                    for (pt in stroke.points) {
                                                                        BiRefNetLiteEngine.applyBrushToMask(
                                                                            mask = mask,
                                                                            width = w,
                                                                            height = h,
                                                                            centerX = pt.x * w,
                                                                            centerY = pt.y * h,
                                                                            radius = stroke.radiusPx * (w.toFloat() / 320f).coerceAtLeast(1f),
                                                                            isRestore = stroke.isRestore,
                                                                            isSoft = stroke.isSoft
                                                                        )
                                                                    }
                                                                    val refinedSub = BiRefNetLiteEngine.createRefinedForegroundBitmap(orig, mask, w, h)
                                                                    extractedSubjectBitmap = refinedSub
                                                                    updateBgComposite(refinedSub)
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp),
                                                        enabled = bgRefineRedoStrokes.isNotEmpty()
                                                    ) {
                                                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo Refine", tint = if (bgRefineRedoStrokes.isNotEmpty()) Color.White else Color(0xFF475569), modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            bgRefineStrokes.clear()
                                                            bgRefineRedoStrokes.clear()
                                                            val orig = bgOriginalBitmap ?: currentBitmap ?: return@IconButton
                                                            val origMask = bgOriginalMask ?: return@IconButton
                                                            val w = bgMaskWidth
                                                            val h = bgMaskHeight
                                                            if (w > 0 && h > 0 && origMask.size == w * h) {
                                                                val m = origMask.clone()
                                                                val restoredSub = BiRefNetLiteEngine.createRefinedForegroundBitmap(orig, m, w, h)
                                                                bgWorkingMask = m
                                                                extractedSubjectBitmap = restoredSub
                                                                updateBgComposite(restoredSub)
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp),
                                                        enabled = bgRefineStrokes.isNotEmpty() || bgRefineRedoStrokes.isNotEmpty()
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Clear Refine", tint = if (bgRefineStrokes.isNotEmpty() || bgRefineRedoStrokes.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF475569), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Brush Size: ${bgRefineBrushSize.roundToInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                                                Slider(
                                                    value = bgRefineBrushSize,
                                                    onValueChange = { bgRefineBrushSize = it },
                                                    valueRange = 10f..100f,
                                                    modifier = Modifier.weight(1f),
                                                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                                )
                                            }
                                        }
                                        2 -> {
                                            // Position & Transform Tab
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Scale: ${(bgSubjectScale * 100).roundToInt()}%", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                                                Slider(
                                                    value = bgSubjectScale,
                                                    onValueChange = {
                                                        bgSubjectScale = it
                                                        updateBgComposite(null)
                                                    },
                                                    valueRange = 0.3f..3.0f,
                                                    modifier = Modifier.weight(1f),
                                                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Rotation: ${bgSubjectRotation.roundToInt()}°", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                                                Slider(
                                                    value = bgSubjectRotation,
                                                    onValueChange = {
                                                        bgSubjectRotation = it
                                                        updateBgComposite(null)
                                                    },
                                                    valueRange = -180f..180f,
                                                    modifier = Modifier.weight(1f),
                                                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        bgSubjectTranslationX = 0f
                                                        bgSubjectTranslationY = 0f
                                                        updateBgComposite(null)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(32.dp)
                                                ) {
                                                    Text("Center", fontSize = 11.sp, color = Color.White)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        bgSubjectTranslationX = 0f
                                                        bgSubjectTranslationY = 0f
                                                        bgSubjectScale = 1f
                                                        bgSubjectRotation = 0f
                                                        updateBgComposite(null)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(32.dp)
                                                ) {
                                                    Text("Reset Transform", fontSize = 11.sp, color = Color.White)
                                                }
                                            }
                                            Text("Tip: You can also drag, pinch to zoom, and twist to rotate directly on the photo above.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            PhotoEditTool.TEXT -> {
                                val activeItem = textItems.find { it.id == selectedTextId }

                                PhotoAdvancedTextToolPanel(
                                    selectedItem = activeItem,
                                    onAddText = {
                                        pushUndoHistory()
                                        val newItem = TextItem(
                                            text = if (selectedFontCategory == "Bengali") "বাংলা লেখা" else "Sample Text",
                                            fontId = if (selectedFontCategory == "Bengali") "bn_sans" else "sans_serif"
                                        )
                                        textItems.add(newItem)
                                        selectedTextId = newItem.id
                                        editingTextDraft = newItem.text
                                    },
                                    onUpdateItem = { updated ->
                                        val idx = textItems.indexOfFirst { it.id == updated.id }
                                        if (idx >= 0) {
                                            textItems[idx] = updated
                                        }
                                    },
                                    onDeleteItem = { id ->
                                        pushUndoHistory()
                                        textItems.removeAll { it.id == id }
                                        if (selectedTextId == id) selectedTextId = null
                                    },
                                    onCopyItem = { itemToCopy ->
                                        pushUndoHistory()
                                        val copied = itemToCopy.copy(
                                            id = java.util.UUID.randomUUID().toString(),
                                            normX = (itemToCopy.normX + 0.04f).coerceIn(0f, 0.95f),
                                            normY = (itemToCopy.normY + 0.04f).coerceIn(0f, 0.95f)
                                        )
                                        textItems.add(copied)
                                        selectedTextId = copied.id
                                        editingTextDraft = copied.text
                                    },
                                    onBringToFront = { itemToFront ->
                                        pushUndoHistory()
                                        val idx = textItems.indexOfFirst { it.id == itemToFront.id }
                                        if (idx >= 0 && idx < textItems.size - 1) {
                                            val itm = textItems.removeAt(idx)
                                            textItems.add(itm)
                                            selectedTextId = itm.id
                                        }
                                    },
                                    onSendToBack = { itemToBack ->
                                        pushUndoHistory()
                                        val idx = textItems.indexOfFirst { it.id == itemToBack.id }
                                        if (idx > 0) {
                                            val itm = textItems.removeAt(idx)
                                            textItems.add(0, itm)
                                            selectedTextId = itm.id
                                        }
                                    },
                                    onOpenLayers = { showLayerManagerDialog = true }
                                )
                            }

                            PhotoEditTool.DRAWING -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = !isDrawEraser && !isColorPickerActive && !isDrawingPanMode,
                                            onClick = {
                                                isDrawEraser = false
                                                isColorPickerActive = false
                                                isDrawingPanMode = false
                                            },
                                            label = { Text("Brush", fontSize = 12.sp) }
                                        )
                                        FilterChip(
                                            selected = isDrawEraser && !isColorPickerActive && !isDrawingPanMode,
                                            onClick = {
                                                isDrawEraser = true
                                                isColorPickerActive = false
                                                isDrawingPanMode = false
                                            },
                                            label = { Text("Eraser", fontSize = 12.sp) }
                                        )
                                        FilterChip(
                                            selected = isDrawingPanMode,
                                            onClick = {
                                                isDrawingPanMode = true
                                                isColorPickerActive = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.PanTool,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            },
                                            label = { Text("Pan", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AccentPink,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                        FilterChip(
                                            selected = isColorPickerActive,
                                            onClick = {
                                                isColorPickerActive = true
                                                isDrawEraser = false
                                                isDrawingPanMode = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Colorize,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            },
                                            label = { Text("Picker", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AccentPink,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(drawColor))
                                                .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                                        )

                                        // Undo Drawing
                                        IconButton(
                                            onClick = { undoDrawingStroke() },
                                            enabled = drawStrokes.isNotEmpty(),
                                            modifier = Modifier.size(28.dp).testTag("drawing_undo_button")
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Undo,
                                                contentDescription = "Undo stroke",
                                                tint = if (drawStrokes.isNotEmpty()) Color.White else Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Redo Drawing
                                        IconButton(
                                            onClick = { redoDrawingStroke() },
                                            enabled = drawRedoStrokes.isNotEmpty(),
                                            modifier = Modifier.size(28.dp).testTag("drawing_redo_button")
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Redo,
                                                contentDescription = "Redo stroke",
                                                tint = if (drawRedoStrokes.isNotEmpty()) Color.White else Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Clear all strokes
                                        if (drawStrokes.isNotEmpty()) {
                                            IconButton(
                                                onClick = { clearDrawingStrokes() },
                                                modifier = Modifier.size(28.dp).testTag("drawing_clear_button")
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Clear all",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isColorPickerActive) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Colorize,
                                                contentDescription = null,
                                                tint = AccentPink,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Touch or drag on photo to sample pixel color",
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Size: ${drawBrushSize.roundToInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                        Slider(
                                            value = drawBrushSize,
                                            onValueChange = { drawBrushSize = it },
                                            valueRange = 2f..50f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                        )
                                    }
                                    if (!isDrawEraser) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Opacity: ${(drawOpacity * 100).roundToInt()}%", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                            Slider(
                                                value = drawOpacity,
                                                onValueChange = { drawOpacity = it },
                                                valueRange = 0.1f..1f,
                                                modifier = Modifier.weight(1f),
                                                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                                            )
                                        }
                                        ColorPaletteRow(
                                            selectedColor = drawColor,
                                            onColorSelected = { drawColor = it }
                                        )
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }

            // ---------------------------------------------------------
            // Tool Selection Strip & Category Tabs (when no tool is currently actively open)
            // ---------------------------------------------------------
            if (currentBitmap != null && activeTool == PhotoEditTool.NONE) {
                Surface(
                    color = DarkBarBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Tool Buttons for Selected Category
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (selectedCategory) {
                                PhotoEditCategory.BASIC -> {
                                    ToolButton(
                                        icon = Icons.Default.Crop,
                                        label = "Crop",
                                        testTag = "tool_crop",
                                        onClick = { activeTool = PhotoEditTool.CROP }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.RotateLeft,
                                        label = "Rotate Left",
                                        testTag = "tool_rotate_left",
                                        onClick = {
                                            val curr = currentBitmap ?: return@ToolButton
                                            pushUndoHistory()
                                            currentBitmap = PhotoEditorEngine.rotateOrFlip(curr, degrees = -90)
                                        }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.RotateRight,
                                        label = "Rotate Right",
                                        testTag = "tool_rotate_right",
                                        onClick = {
                                            val curr = currentBitmap ?: return@ToolButton
                                            pushUndoHistory()
                                            currentBitmap = PhotoEditorEngine.rotateOrFlip(curr, degrees = 90)
                                        }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.InvertColors,
                                        label = "Flip H",
                                        testTag = "tool_flip_h",
                                        onClick = {
                                            val curr = currentBitmap ?: return@ToolButton
                                            pushUndoHistory()
                                            currentBitmap = PhotoEditorEngine.rotateOrFlip(curr, flipHorizontal = true)
                                        }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.InvertColors,
                                        label = "Flip V",
                                        testTag = "tool_flip_v",
                                        onClick = {
                                            val curr = currentBitmap ?: return@ToolButton
                                            pushUndoHistory()
                                            currentBitmap = PhotoEditorEngine.rotateOrFlip(curr, flipVertical = true)
                                        }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.AspectRatio,
                                        label = "Resize",
                                        testTag = "tool_resize",
                                        onClick = { activeTool = PhotoEditTool.RESIZE }
                                    )
                                }

                                PhotoEditCategory.ADJUST -> {
                                    ToolButton(
                                        icon = Icons.Default.FlashOn,
                                        label = "Flash",
                                        testTag = "tool_flash",
                                        onClick = {
                                            activeTool = PhotoEditTool.FLASH
                                            val curr = currentBitmap
                                            if (curr != null) {
                                                val cached = flashRelitBaseBitmap
                                                if (cached != null && !cached.isRecycled && cached.width == curr.width && cached.height == curr.height) {
                                                    previewBitmap = FaceRelightEngine.blendRelit(curr, cached, flashIntensity / 100f)
                                                } else {
                                                    previewBitmap = null
                                                    isFlashProcessing = true
                                                    coroutineScope.launch {
                                                        try {
                                                            val relit = FaceRelightEngine.generateFullRelitBitmap(context, curr)
                                                            flashRelitBaseBitmap = relit
                                                            previewBitmap = FaceRelightEngine.blendRelit(curr, relit, flashIntensity / 100f)
                                                        } catch (e: Exception) {
                                                            Log.e("PhotoEditing", "Flash error", e)
                                                            Toast.makeText(context, "Flash effect failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        } finally {
                                                            isFlashProcessing = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.WbSunny,
                                        label = "Brightness",
                                        testTag = "tool_brightness",
                                        onClick = {
                                            activeTool = PhotoEditTool.BRIGHTNESS
                                            previewBitmap = null
                                            brightnessValue = 0f
                                        }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.Contrast,
                                        label = "Contrast",
                                        testTag = "tool_contrast",
                                        onClick = {
                                            activeTool = PhotoEditTool.CONTRAST
                                            previewBitmap = null
                                            contrastValue = 0f
                                        }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.Palette,
                                        label = "Saturation",
                                        testTag = "tool_saturation",
                                        onClick = {
                                            activeTool = PhotoEditTool.SATURATION
                                            previewBitmap = null
                                            saturationValue = 0f
                                        }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.AutoAwesome,
                                        label = "Enhance",
                                        testTag = "tool_enhance",
                                        onClick = {
                                            activeTool = PhotoEditTool.ENHANCE
                                            previewBitmap = null
                                            enhanceValue = 0f
                                        }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.AutoFixHigh,
                                        label = "Sharpen",
                                        testTag = "tool_sharpen",
                                        onClick = {
                                            activeTool = PhotoEditTool.SHARPEN
                                            previewBitmap = null
                                            sharpenValue = 0f
                                        }
                                    )
                                }

                                PhotoEditCategory.TOOLS -> {
                                    ToolButton(
                                        icon = Icons.Default.Tune,
                                        label = "Blur",
                                        testTag = "tool_blur",
                                        onClick = { activeTool = PhotoEditTool.BLUR }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.Highlight,
                                        label = "Erase",
                                        testTag = "tool_erase",
                                        onClick = { activeTool = PhotoEditTool.ERASE }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.AspectRatio,
                                        label = "Perspective",
                                        testTag = "tool_perspective",
                                        onClick = { activeTool = PhotoEditTool.PERSPECTIVE }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.AutoFixHigh,
                                        label = "BG Remove",
                                        testTag = "tool_bg_remove",
                                        onClick = {
                                            runBiRefNetBgRemoval()
                                        }
                                    )
                                }

                                PhotoEditCategory.MARKUP -> {
                                    ToolButton(
                                        icon = Icons.Default.TextFields,
                                        label = "Text",
                                        testTag = "tool_text",
                                        onClick = { activeTool = PhotoEditTool.TEXT }
                                    )
                                    ToolButton(
                                        icon = Icons.Default.Brush,
                                        label = "Drawing",
                                        testTag = "tool_drawing",
                                        onClick = { activeTool = PhotoEditTool.DRAWING }
                                    )
                                }
                            }
                        }

                        // Category Navigation Tabs (BASIC, ADJUST, TOOLS, MARKUP)
                        TabRow(
                            selectedTabIndex = selectedCategory.ordinal,
                            containerColor = DarkCardSurface,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedCategory.ordinal]),
                                    color = CrimsonPrimary
                                )
                            }
                        ) {
                            PhotoEditCategory.values().forEach { category ->
                                val isSelected = category == selectedCategory
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedCategory = category },
                                    text = {
                                        Text(
                                            text = category.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) CrimsonPrimary else Color(0xFF94A3B8)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Dialogs: Reset Confirmation
    // -------------------------------------------------------------
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Photo Edits?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("All modifications will be removed and your original photo will be restored.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        performReset()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Reset All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = DarkCardSurface
        )
    }

    // -------------------------------------------------------------
    // Dialogs: Saved Success Dialog
    // -------------------------------------------------------------
    // -------------------------------------------------------------
    // Dialogs: Custom Color Picker (Background & Text)
    // -------------------------------------------------------------
    if (showCustomBgColorPicker) {
        CustomColorPickerDialog(
            initialColor = bgSolidColor,
            onColorSelected = { col ->
                bgSolidColor = col
                val curr = currentBitmap
                if (curr != null) {
                    val subject = extractedSubjectBitmap ?: curr
                    previewBitmap = PhotoEditorEngine.compositeSubjectOverBackground(
                        subjectBitmap = subject,
                        bgType = BgOptionType.SOLID_COLOR,
                        solidColor = col,
                        gradientColor1 = bgGradientColor1,
                        gradientColor2 = bgGradientColor2,
                        gradientDirection = bgGradientDirection,
                        bgImage = bgCustomImage
                    )
                }
                showCustomBgColorPicker = false
            },
            onDismiss = { showCustomBgColorPicker = false }
        )
    }

    if (showCustomTextColorPicker) {
        val activeItem = textItems.find { it.id == selectedTextId }
        CustomColorPickerDialog(
            initialColor = activeItem?.color ?: AndroidColor.WHITE,
            onColorSelected = { col ->
                if (activeItem != null) {
                    val idx = textItems.indexOfFirst { it.id == activeItem.id }
                    if (idx >= 0) {
                        textItems[idx] = activeItem.copy(color = col)
                    }
                }
                showCustomTextColorPicker = false
            },
            onDismiss = { showCustomTextColorPicker = false }
        )
    }

    if (showLayerManagerDialog) {
        val layers = textItems.reversed().map { item ->
            LayerEntry(
                id = item.id,
                title = if (item.text.isNotBlank()) item.text else "Text Layer",
                subtitle = "Text",
                isText = true,
                thumbnailBitmap = null,
                isVisible = item.isVisible,
                isLocked = item.isLocked,
                opacity = item.opacity
            )
        }

        FullLayerManagerDialog(
            layers = layers,
            selectedId = selectedTextId,
            onSelect = { id ->
                selectedTextId = id
                activeTool = PhotoEditTool.TEXT
            },
            onToggleVisibility = { id ->
                pushUndoHistory()
                val idx = textItems.indexOfFirst { it.id == id }
                if (idx != -1) {
                    val itm = textItems[idx]
                    textItems[idx] = itm.copy(isVisible = !itm.isVisible)
                }
            },
            onToggleLock = { id ->
                pushUndoHistory()
                val idx = textItems.indexOfFirst { it.id == id }
                if (idx != -1) {
                    val itm = textItems[idx]
                    textItems[idx] = itm.copy(isLocked = !itm.isLocked)
                }
            },
            onChangeOpacity = { id, newOpacity ->
                val idx = textItems.indexOfFirst { it.id == id }
                if (idx != -1) {
                    val itm = textItems[idx]
                    textItems[idx] = itm.copy(opacity = newOpacity)
                }
            },
            onMoveUp = { reversedIndex ->
                pushUndoHistory()
                val origIndex = (textItems.size - 1) - reversedIndex
                if (origIndex < textItems.size - 1) {
                    val itm = textItems.removeAt(origIndex)
                    textItems.add(origIndex + 1, itm)
                }
            },
            onMoveDown = { reversedIndex ->
                pushUndoHistory()
                val origIndex = (textItems.size - 1) - reversedIndex
                if (origIndex > 0) {
                    val itm = textItems.removeAt(origIndex)
                    textItems.add(origIndex - 1, itm)
                }
            },
            onDelete = { id ->
                pushUndoHistory()
                textItems.removeAll { it.id == id }
                if (selectedTextId == id) selectedTextId = null
            },
            onDismiss = { showLayerManagerDialog = false }
        )
    }

    if (showSavedDialog && lastSaveResult != null) {
        val result = lastSaveResult!!
        Dialog(
            onDismissRequest = { showSavedDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x2210B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Photo Saved Successfully", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(result.fileName, color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text("Saved to ${result.displayLocation}", color = Color(0xFF64748B), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (result.fileName.endsWith(".png", true)) "image/png" else "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, result.savedUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Edited Photo"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = Color.White, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                showSavedDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
