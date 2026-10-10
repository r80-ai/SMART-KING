package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.photo.SplitCameraProcessor
import com.example.photo.SplitLayout
import com.example.storage.GlobalSaveManager
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class CameraFlashMode(val label: String, val icon: ImageVector) {
    AUTO("Auto", Icons.Default.FlashAuto),
    ON("On", Icons.Default.FlashOn),
    OFF("Off", Icons.Default.FlashOff)
}

@Composable
fun SplitCameraScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Camera permission check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Active Layout: 1x2 Vertical (default) or 2x1 Horizontal
    var selectedLayout by remember { mutableStateOf(SplitLayout.VERTICAL_1X2) }
    var showLayoutDialog by remember { mutableStateOf(false) }

    // Split ratio: 0.05f to 0.95f, default 0.50f (50% / 50%)
    var splitRatio by remember { mutableFloatStateOf(0.50f) }

    // Camera configurations
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(CameraFlashMode.AUTO) }

    // Capture states
    var isCapturing by remember { mutableStateOf(false) }
    // firstPhotoBitmap: LEFT for 1x2, TOP for 2x1
    var firstPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // secondPhotoBitmap: RIGHT for 1x2, BOTTOM for 2x1
    var secondPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var savedFileName by remember { mutableStateOf<String?>(null) }
    var showShutterFlash by remember { mutableStateOf(false) }

    // Status message for capturing (e.g. "Ready for 2nd shot")
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val handleBackPress: () -> Unit = {
        when {
            showLayoutDialog -> showLayoutDialog = false
            firstPhotoBitmap != null && secondPhotoBitmap != null -> {
                firstPhotoBitmap = null
                secondPhotoBitmap = null
                savedFileName = null
            }
            firstPhotoBitmap != null -> {
                firstPhotoBitmap = null
            }
            else -> onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackPress()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("split_camera_screen")
    ) {
        if (!hasCameraPermission) {
            CameraPermissionRequiredContent(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onBack = onBack
            )
        } else if (firstPhotoBitmap != null && secondPhotoBitmap != null) {
            // Both captured: Result Preview Screen
            CapturedSplitPhotoPreview(
                layout = selectedLayout,
                firstPhoto = firstPhotoBitmap!!,
                secondPhoto = secondPhotoBitmap!!,
                splitRatio = splitRatio,
                isSaving = isSaving,
                savedFileName = savedFileName,
                onRetakeFirst = {
                    firstPhotoBitmap = null
                    savedFileName = null
                },
                onRetakeSecond = {
                    secondPhotoBitmap = null
                    savedFileName = null
                },
                onSave = { blendSeam, showDivider ->
                    isSaving = true
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val combined = SplitCameraProcessor.createCombinedBitmap(
                                firstPhoto = firstPhotoBitmap!!,
                                secondPhoto = secondPhotoBitmap!!,
                                layout = selectedLayout,
                                splitRatio = splitRatio,
                                blendSeam = blendSeam,
                                drawDividerLine = showDivider
                            )
                            val prefix = if (selectedLayout == SplitLayout.VERTICAL_1X2) "Split1x2" else "Split2x1"
                            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
                            val tempFile = File(context.cacheDir, fileName)
                            FileOutputStream(tempFile).use { out ->
                                combined.compress(Bitmap.CompressFormat.JPEG, 96, out)
                            }
                            val saveResult = GlobalSaveManager.saveProcessedFile(
                                context = context,
                                tempSourceFile = tempFile,
                                desiredFileName = fileName,
                                mimeType = "image/jpeg"
                            )
                            withContext(Dispatchers.Main) {
                                isSaving = false
                                if (saveResult.isSuccess) {
                                    savedFileName = saveResult.fileName
                                    Toast.makeText(context, "Photo Saved: ${saveResult.fileName}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to save: ${saveResult.errorMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isSaving = false
                                Toast.makeText(context, "Error saving photo: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onShare = { blendSeam, showDivider ->
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val combined = SplitCameraProcessor.createCombinedBitmap(
                                firstPhoto = firstPhotoBitmap!!,
                                secondPhoto = secondPhotoBitmap!!,
                                layout = selectedLayout,
                                splitRatio = splitRatio,
                                blendSeam = blendSeam,
                                drawDividerLine = showDivider
                            )
                            val shareFile = File(context.cacheDir, "share_split_camera.jpg")
                            FileOutputStream(shareFile).use { out ->
                                combined.compress(Bitmap.CompressFormat.JPEG, 96, out)
                            }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
                            withContext(Dispatchers.Main) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Split Photo"))
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onBack = {
                    firstPhotoBitmap = null
                    secondPhotoBitmap = null
                    savedFileName = null
                }
            )
        } else {
            // Live Split Camera View (Capturing Shot 1 or Shot 2)
            LiveSplitCameraContent(
                layout = selectedLayout,
                splitRatio = splitRatio,
                onSplitRatioChange = { splitRatio = it },
                lensFacing = lensFacing,
                flashMode = flashMode,
                isCapturing = isCapturing,
                firstPhoto = firstPhotoBitmap,
                secondPhoto = secondPhotoBitmap,
                statusMessage = statusMessage,
                onToggleFlash = {
                    flashMode = when (flashMode) {
                        CameraFlashMode.AUTO -> CameraFlashMode.ON
                        CameraFlashMode.ON -> CameraFlashMode.OFF
                        CameraFlashMode.OFF -> CameraFlashMode.AUTO
                    }
                },
                onSwitchCamera = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                onOpenLayoutSelection = { showLayoutDialog = true },
                onResetDivider = { splitRatio = 0.50f },
                onCapture = { imageCapture ->
                    if (isCapturing) return@LiveSplitCameraContent
                    isCapturing = true
                    showShutterFlash = true

                    val mainExecutor = ContextCompat.getMainExecutor(context)
                    imageCapture.takePicture(
                        mainExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                coroutineScope.launch(Dispatchers.Default) {
                                    try {
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                        val rawBitmap = imageProxy.toBitmap()
                                        val isFront = (lensFacing == CameraSelector.LENS_FACING_FRONT)

                                        val matrix = Matrix().apply {
                                            if (rotationDegrees != 0) {
                                                postRotate(rotationDegrees.toFloat())
                                            }
                                            if (isFront) {
                                                postScale(-1f, 1f)
                                            }
                                        }

                                        val orientedBmp = if (!matrix.isIdentity) {
                                            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                        } else {
                                            rawBitmap
                                        }

                                        withContext(Dispatchers.Main) {
                                            if (firstPhotoBitmap == null && secondPhotoBitmap == null) {
                                                // First shot captured
                                                firstPhotoBitmap = orientedBmp
                                                statusMessage = "Ready for 2nd shot"
                                            } else if (firstPhotoBitmap != null && secondPhotoBitmap == null) {
                                                // Second shot captured
                                                secondPhotoBitmap = orientedBmp
                                                statusMessage = null
                                            } else if (firstPhotoBitmap == null && secondPhotoBitmap != null) {
                                                // First shot retaken
                                                firstPhotoBitmap = orientedBmp
                                                statusMessage = null
                                            }
                                            isCapturing = false
                                        }

                                        if (secondPhotoBitmap == null) {
                                            delay(2500)
                                            withContext(Dispatchers.Main) {
                                                if (statusMessage == "Ready for 2nd shot") {
                                                    statusMessage = null
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SplitCamera", "Error processing capture", e)
                                        withContext(Dispatchers.Main) {
                                            isCapturing = false
                                            Toast.makeText(context, "Error processing photo: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        imageProxy.close()
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isCapturing = false
                                Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                onBack = handleBackPress
            )
        }

        if (showShutterFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
            LaunchedEffect(Unit) {
                delay(80L)
                showShutterFlash = false
            }
        }

        if (showLayoutDialog) {
            SplitLayoutSelectionDialog(
                currentLayout = selectedLayout,
                onSelectLayout = { newLayout ->
                    if (selectedLayout != newLayout) {
                        selectedLayout = newLayout
                        firstPhotoBitmap = null
                        secondPhotoBitmap = null
                        savedFileName = null
                        statusMessage = null
                        splitRatio = 0.50f
                        Toast.makeText(context, "Switched to ${newLayout.shortLabel}", Toast.LENGTH_SHORT).show()
                    }
                    showLayoutDialog = false
                },
                onDismiss = { showLayoutDialog = false }
            )
        }
    }
}

@Composable
private fun LiveSplitCameraContent(
    layout: SplitLayout,
    splitRatio: Float,
    onSplitRatioChange: (Float) -> Unit,
    lensFacing: Int,
    flashMode: CameraFlashMode,
    isCapturing: Boolean,
    firstPhoto: Bitmap?,
    secondPhoto: Bitmap?,
    statusMessage: String?,
    onToggleFlash: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenLayoutSelection: () -> Unit,
    onResetDivider: () -> Unit,
    onCapture: (ImageCapture) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewViewWidth by remember { mutableFloatStateOf(1f) }
    var previewViewHeight by remember { mutableFloatStateOf(1f) }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureInstance by remember { mutableStateOf<ImageCapture?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lensFacing) {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(
                            when (flashMode) {
                                CameraFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                                CameraFlashMode.ON -> ImageCapture.FLASH_MODE_ON
                                CameraFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                            }
                        )
                        .build()

                    val cameraSelector = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    val boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    cameraInstance = boundCamera
                    imageCaptureInstance = imageCapture
                } catch (e: Exception) {
                    Log.e("SplitCamera", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e("SplitCamera", "ProcessCameraProvider initialization failed", e)
        }
    }

    LaunchedEffect(flashMode, imageCaptureInstance) {
        imageCaptureInstance?.flashMode = when (flashMode) {
            CameraFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            CameraFlashMode.ON -> ImageCapture.FLASH_MODE_ON
            CameraFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (ignored: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            Surface(
                shape = CircleShape,
                color = Color(0x66000000),
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onBack() }
                    .testTag("split_camera_back_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Layout Selector Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x66000000),
                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                modifier = Modifier
                    .clickable { onOpenLayoutSelection() }
                    .testTag("split_camera_layout_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (layout == SplitLayout.VERTICAL_1X2) Icons.Default.ViewColumn else Icons.Default.ViewStream,
                        contentDescription = "Layout",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (layout == SplitLayout.VERTICAL_1X2) "1×2" else "2×1",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Flash Toggle Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x66000000),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier
                    .clickable { onToggleFlash() }
                    .testTag("camera_flash_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = flashMode.icon,
                        contentDescription = "Flash: ${flashMode.label}",
                        tint = if (flashMode == CameraFlashMode.OFF) Color(0xFF94A3B8) else Color(0xFFFFD54F),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = flashMode.label.uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Switch Camera Button
            Surface(
                shape = CircleShape,
                color = Color(0x66000000),
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onSwitchCamera() }
                    .testTag("switch_camera_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Camera Preview Area with Interactive Split Divider
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .clipToBounds()
                .onSizeChanged {
                    previewViewWidth = it.width.toFloat()
                    previewViewHeight = it.height.toFloat()
                }
                .pointerInput(layout, previewViewWidth, previewViewHeight) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (layout == SplitLayout.VERTICAL_1X2 && previewViewWidth > 0) {
                            val deltaRatio = dragAmount.x / previewViewWidth
                            onSplitRatioChange((splitRatio + deltaRatio).coerceIn(0.05f, 0.95f))
                        } else if (layout == SplitLayout.HORIZONTAL_2X1 && previewViewHeight > 0) {
                            val deltaRatio = dragAmount.y / previewViewHeight
                            onSplitRatioChange((splitRatio + deltaRatio).coerceIn(0.05f, 0.95f))
                        }
                    }
                }
        ) {
            // Live Camera Feed
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Dimming overlay on the waiting section before first capture
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (firstPhoto == null && secondPhoto == null) {
                    if (layout == SplitLayout.VERTICAL_1X2) {
                        val splitX = size.width * splitRatio
                        drawRect(
                            color = Color(0x88000000),
                            topLeft = Offset(splitX, 0f),
                            size = Size(size.width - splitX, size.height)
                        )
                    } else {
                        val splitY = size.height * splitRatio
                        drawRect(
                            color = Color(0x88000000),
                            topLeft = Offset(0f, splitY),
                            size = Size(size.width, size.height - splitY)
                        )
                    }
                }
            }

            // Draw captured photo(s) on their designated half
            if (layout == SplitLayout.VERTICAL_1X2) {
                if (firstPhoto != null) {
                    Image(
                        bitmap = firstPhoto.asImageBitmap(),
                        contentDescription = "Captured Left Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(right = size.width * splitRatio) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
                if (secondPhoto != null) {
                    Image(
                        bitmap = secondPhoto.asImageBitmap(),
                        contentDescription = "Captured Right Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(left = size.width * splitRatio) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            } else {
                // HORIZONTAL_2X1
                if (firstPhoto != null) {
                    Image(
                        bitmap = firstPhoto.asImageBitmap(),
                        contentDescription = "Captured Top Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(bottom = size.height * splitRatio) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
                if (secondPhoto != null) {
                    Image(
                        bitmap = secondPhoto.asImageBitmap(),
                        contentDescription = "Captured Bottom Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(top = size.height * splitRatio) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            }

            // Status message badge overlay (e.g. "Ready for 2nd shot")
            if (statusMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xDD0F172A),
                        border = BorderStroke(1.dp, CrimsonPrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusMessage,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Status badges for sections
            if (layout == SplitLayout.VERTICAL_1X2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC0F172A),
                        border = BorderStroke(1.dp, Color(0xFF475569))
                    ) {
                        Text(
                            text = if (firstPhoto == null) "CAPTURE LEFT" else "LEFT SAVED ✓",
                            color = if (firstPhoto == null) Color(0xFFE2E8F0) else Color(0xFF4ADE80),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (firstPhoto != null && secondPhoto == null) CrimsonPrimary.copy(alpha = 0.88f) else Color(0xCC0F172A),
                        border = BorderStroke(1.dp, if (firstPhoto != null && secondPhoto == null) Color(0xFFFF80AB) else Color(0xFF475569))
                    ) {
                        Text(
                            text = if (secondPhoto != null) "RIGHT SAVED ✓" else if (firstPhoto != null) "CAPTURE RIGHT" else "WAITING...",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // Top Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .align(Alignment.TopCenter),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC0F172A),
                        border = BorderStroke(1.dp, Color(0xFF475569))
                    ) {
                        Text(
                            text = if (firstPhoto == null) "CAPTURE TOP" else "TOP SAVED ✓",
                            color = if (firstPhoto == null) Color(0xFFE2E8F0) else Color(0xFF4ADE80),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Bottom Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (firstPhoto != null && secondPhoto == null) CrimsonPrimary.copy(alpha = 0.88f) else Color(0xCC0F172A),
                        border = BorderStroke(1.dp, if (firstPhoto != null && secondPhoto == null) Color(0xFFFF80AB) else Color(0xFF475569))
                    ) {
                        Text(
                            text = if (secondPhoto != null) "BOTTOM SAVED ✓" else if (firstPhoto != null) "CAPTURE BOTTOM" else "WAITING...",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Interactive Divider Line & Handle
            if (layout == SplitLayout.VERTICAL_1X2) {
                val splitPx = previewViewWidth * splitRatio

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .offset { IntOffset(splitPx.roundToInt() - 1.dp.roundToPx(), 0) }
                        .width(2.5.dp)
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.92f))
                        .shadow(elevation = 4.dp)
                )

                // Divider Drag Handle (Centered Vertically)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (splitPx - 24.dp.roundToPx()).roundToInt(),
                                y = ((previewViewHeight / 2f) - 24.dp.roundToPx()).roundToInt()
                            )
                        }
                        .size(48.dp)
                        .testTag("split_camera_divider_handle")
                        .pointerInput(previewViewWidth) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (previewViewWidth > 0) {
                                    val deltaRatio = dragAmount.x / previewViewWidth
                                    onSplitRatioChange((splitRatio + deltaRatio).coerceIn(0.05f, 0.95f))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xF00F172A),
                        border = BorderStroke(2.dp, Color.White),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Drag Divider",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            } else {
                // 2x1 Horizontal Divider
                val splitPy = previewViewHeight * splitRatio

                // Horizontal Divider Line
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, splitPy.roundToInt() - 1.dp.roundToPx()) }
                        .height(2.5.dp)
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.92f))
                        .shadow(elevation = 4.dp)
                )

                // Divider Drag Handle (Centered Horizontally)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = ((previewViewWidth / 2f) - 24.dp.roundToPx()).roundToInt(),
                                y = (splitPy - 24.dp.roundToPx()).roundToInt()
                            )
                        }
                        .size(48.dp)
                        .testTag("split_camera_divider_handle")
                        .pointerInput(previewViewHeight) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (previewViewHeight > 0) {
                                    val deltaRatio = dragAmount.y / previewViewHeight
                                    onSplitRatioChange((splitRatio + deltaRatio).coerceIn(0.05f, 0.95f))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xF00F172A),
                        border = BorderStroke(2.dp, Color.White),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.UnfoldMore,
                                contentDescription = "Drag Divider",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Divider to 50:50
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x33FFFFFF),
                modifier = Modifier
                    .clickable { onResetDivider() }
                    .testTag("reset_split_divider_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "50:50",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Capture Shutter Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(5.dp)
                    .background(Color.Transparent)
                    .clickable(enabled = !isCapturing && imageCaptureInstance != null) {
                        imageCaptureInstance?.let { onCapture(it) }
                    }
                    .testTag("camera_capture_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        color = CrimsonPrimary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Divider Direction Hint
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x33FFFFFF)
            ) {
                Text(
                    text = if (layout == SplitLayout.VERTICAL_1X2) "Drag ↔" else "Drag ↕",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CapturedSplitPhotoPreview(
    layout: SplitLayout,
    firstPhoto: Bitmap,
    secondPhoto: Bitmap,
    splitRatio: Float,
    isSaving: Boolean,
    savedFileName: String?,
    onRetakeFirst: () -> Unit,
    onRetakeSecond: () -> Unit,
    onSave: (blendSeam: Boolean, showDivider: Boolean) -> Unit,
    onShare: (blendSeam: Boolean, showDivider: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showDivider by remember { mutableStateOf(false) }
    var isSeamBlended by remember(firstPhoto, secondPhoto, layout, splitRatio) { mutableStateOf(false) }
    var blendedBitmap by remember(firstPhoto, secondPhoto, layout, splitRatio) { mutableStateOf<Bitmap?>(null) }
    var isBlendingInProgress by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("preview_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Split Photo (${if (layout == SplitLayout.VERTICAL_1X2) "1×2" else "2×1"})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { onShare(isSeamBlended, showDivider) },
                modifier = Modifier.testTag("split_camera_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White
                )
            }
        }

        // Photo Display Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF020617)),
            contentAlignment = Alignment.Center
        ) {
            if (isSeamBlended && blendedBitmap != null) {
                Image(
                    bitmap = blendedBitmap!!.asImageBitmap(),
                    contentDescription = "Blended Split Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (layout == SplitLayout.VERTICAL_1X2) {
                    Image(
                        bitmap = firstPhoto.asImageBitmap(),
                        contentDescription = "Left Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(right = size.width * splitRatio) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                    Image(
                        bitmap = secondPhoto.asImageBitmap(),
                        contentDescription = "Right Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(left = size.width * splitRatio) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                } else {
                    // HORIZONTAL_2X1
                    Image(
                        bitmap = firstPhoto.asImageBitmap(),
                        contentDescription = "Top Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(bottom = size.height * splitRatio) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                    Image(
                        bitmap = secondPhoto.asImageBitmap(),
                        contentDescription = "Bottom Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(top = size.height * splitRatio) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            }

            // Visible Divider Line Overlay when toggled
            if (showDivider) {
                if (layout == SplitLayout.VERTICAL_1X2) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val splitX = size.width * splitRatio
                                clipRect(left = splitX - 1.5.dp.toPx(), right = splitX + 1.5.dp.toPx()) {
                                    drawRect(Color.White)
                                }
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val splitY = size.height * splitRatio
                                clipRect(top = splitY - 1.5.dp.toPx(), bottom = splitY + 1.5.dp.toPx()) {
                                    drawRect(Color.White)
                                }
                            }
                    )
                }
            }

            if (isBlendingInProgress) {
                CircularProgressIndicator(
                    color = CrimsonPrimary,
                    modifier = Modifier.size(38.dp),
                    strokeWidth = 3.dp
                )
            }

            if (savedFileName != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xDD10B981),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, "Saved", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Saved: $savedFileName",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Feature Controls: Show/Hide Divider and Blend Seam
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Show / Hide Divider Button
            OutlinedButton(
                onClick = { showDivider = !showDivider },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("toggle_divider_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (showDivider) CrimsonPrimary else Color(0xFF64748B)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (showDivider) CrimsonPrimary.copy(alpha = 0.15f) else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = if (layout == SplitLayout.VERTICAL_1X2) Icons.Default.ViewColumn else Icons.Default.ViewStream,
                    contentDescription = null,
                    tint = if (showDivider) Color(0xFFFF80AB) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showDivider) "Hide Divider" else "Show Divider",
                    color = if (showDivider) Color(0xFFFF80AB) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Smart Seam Blend Button
            OutlinedButton(
                onClick = {
                    if (isBlendingInProgress) return@OutlinedButton
                    if (!isSeamBlended) {
                        if (firstPhoto.isRecycled || secondPhoto.isRecycled ||
                            firstPhoto.width <= 0 || firstPhoto.height <= 0 ||
                            secondPhoto.width <= 0 || secondPhoto.height <= 0
                        ) {
                            Toast.makeText(context, "Unable to blend: invalid photos", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        if (blendedBitmap != null && !blendedBitmap!!.isRecycled) {
                            isSeamBlended = true
                        } else {
                            isBlendingInProgress = true
                            coroutineScope.launch(Dispatchers.Default) {
                                try {
                                    val result = SplitCameraProcessor.createCombinedBitmap(
                                        firstPhoto = firstPhoto,
                                        secondPhoto = secondPhoto,
                                        layout = layout,
                                        splitRatio = splitRatio,
                                        blendSeam = true,
                                        drawDividerLine = false
                                    )
                                    if (result.isRecycled || result.width <= 0 || result.height <= 0) {
                                        throw IllegalStateException("Generated invalid blended bitmap")
                                    }
                                    withContext(Dispatchers.Main) {
                                        blendedBitmap = result
                                        isSeamBlended = true
                                        isBlendingInProgress = false
                                    }
                                } catch (t: Throwable) {
                                    Log.e("SplitCamera", "Error blending seam safely caught", t)
                                    withContext(Dispatchers.Main) {
                                        isBlendingInProgress = false
                                        isSeamBlended = false
                                        blendedBitmap = null
                                        Toast.makeText(context, "Unable to blend seam. Original photo kept.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } else {
                        isSeamBlended = false
                    }
                },
                enabled = !isBlendingInProgress,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("blend_seam_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSeamBlended) CrimsonPrimary else Color(0xFF64748B)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSeamBlended) CrimsonPrimary.copy(alpha = 0.15f) else Color.Transparent
                )
            ) {
                if (isBlendingInProgress) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Blending...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(
                        text = if (isSeamBlended) "↩ Undo Blend" else "✨ Blend Seam",
                        color = if (isSeamBlended) Color(0xFFFF80AB) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Retake Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetakeFirst,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("retake_first_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF64748B))
            ) {
                Text(
                    text = if (layout == SplitLayout.VERTICAL_1X2) "Retake Left" else "Retake Top",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
            OutlinedButton(
                onClick = onRetakeSecond,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("retake_second_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF64748B))
            ) {
                Text(
                    text = if (layout == SplitLayout.VERTICAL_1X2) "Retake Right" else "Retake Bottom",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Save Photo Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Button(
                onClick = { onSave(isSeamBlended, showDivider) },
                enabled = !isSaving && !isBlendingInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("split_camera_save_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, "Save", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (savedFileName != null) "Saved ✓" else "Save Photo",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SplitLayoutSelectionDialog(
    currentLayout: SplitLayout,
    onSelectLayout: (SplitLayout) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCBD5E1),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Select Split Layout",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SplitLayoutItemCard(
                    title = "1×2 — Vertical",
                    subtitle = "LEFT + RIGHT",
                    icon = Icons.Default.ViewColumn,
                    isSelected = currentLayout == SplitLayout.VERTICAL_1X2,
                    onClick = { onSelectLayout(SplitLayout.VERTICAL_1X2) }
                )
                SplitLayoutItemCard(
                    title = "2×1 — Horizontal",
                    subtitle = "TOP + BOTTOM",
                    icon = Icons.Default.ViewStream,
                    isSelected = currentLayout == SplitLayout.HORIZONTAL_2X1,
                    onClick = { onSelectLayout(SplitLayout.HORIZONTAL_2X1) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
private fun SplitLayoutItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) CrimsonPrimary.copy(alpha = 0.15f) else Color(0xFF1E293B),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) CrimsonPrimary else Color(0xFF334155)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CrimsonPrimary else Color(0xFF334155),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = if (isSelected) Color(0xFFFF80AB) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionRequiredContent(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = CrimsonPrimary.copy(alpha = 0.15f),
            border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CameraAlt, "Camera", tint = CrimsonPrimary, modifier = Modifier.size(38.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Camera Access Required",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Split Camera operates 100% locally on your device to show live camera views and capture split photos. Please grant camera permission to continue.",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
        ) {
            Text("Grant Permission", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF475569))
        ) {
            Text("Go Back", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
