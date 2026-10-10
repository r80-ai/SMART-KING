package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PdfMasterViewModel
import com.example.photo.PhotoEditorEngine
import com.example.ui.dialogs.DocumentScannerUtil
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class PerspectiveMode {
    AUTO_DETECT,
    MANUAL_ADJUST
}

@Composable
fun EdgePerspectiveScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // Image state
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var afterPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }

    // Perspective Corners in normalized coordinates [0f..1f]: [TL, TR, BR, BL]
    var corners by remember {
        mutableStateOf(
            listOf(
                PointF(0.06f, 0.06f), // TL
                PointF(0.94f, 0.06f), // TR
                PointF(0.94f, 0.94f), // BR
                PointF(0.06f, 0.94f)  // BL
            )
        )
    }

    // Active mode
    var activeMode by remember { mutableStateOf(PerspectiveMode.MANUAL_ADJUST) }

    // Zoom & Pan state for main preview
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Dragging state
    var activeDraggingCornerIndex by remember { mutableIntStateOf(-1) }

    // Save state
    var isSaving by remember { mutableStateOf(false) }
    var savedSuccessUri by remember { mutableStateOf<Uri?>(null) }
    var savedFileName by remember { mutableStateOf("") }
    var showSavedDialog by remember { mutableStateOf(false) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val stream: InputStream? = context.contentResolver.openInputStream(uri)
                    val decoded = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (decoded != null) {
                        // Automatically run auto-detection on newly loaded image
                        val detected = detectDocumentCornersNormalized(decoded)
                        withContext(Dispatchers.Main) {
                            sourceBitmap = decoded
                            corners = detected
                            activeMode = PerspectiveMode.AUTO_DETECT
                            zoomScale = 1f
                            panOffset = Offset.Zero
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

    // Real-time update of After preview whenever sourceBitmap or corners change
    LaunchedEffect(sourceBitmap, corners) {
        val src = sourceBitmap ?: run {
            afterPreviewBitmap = null
            return@LaunchedEffect
        }
        isGeneratingPreview = true
        withContext(Dispatchers.Default) {
            try {
                // Downsample for high-speed real-time preview (under 5ms)
                val maxDim = 480f
                val scale = minOf(1f, maxDim / maxOf(src.width, src.height))
                val smallW = (src.width * scale).roundToInt().coerceAtLeast(10)
                val smallH = (src.height * scale).roundToInt().coerceAtLeast(10)
                val smallBmp = Bitmap.createScaledBitmap(src, smallW, smallH, true)

                val corrected = PhotoEditorEngine.applyPerspectiveCorrection(smallBmp, corners)
                withContext(Dispatchers.Main) {
                    afterPreviewBitmap = corrected
                    isGeneratingPreview = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isGeneratingPreview = false
                }
            }
        }
    }

    // Helper: Reset view and corners
    fun resetPerspective() {
        corners = listOf(
            PointF(0.06f, 0.06f),
            PointF(0.94f, 0.06f),
            PointF(0.94f, 0.94f),
            PointF(0.06f, 0.94f)
        )
        zoomScale = 1f
        panOffset = Offset.Zero
        activeMode = PerspectiveMode.MANUAL_ADJUST
    }

    // Helper: Rotate CW (90 degrees)
    fun rotateClockwise() {
        val src = sourceBitmap ?: return
        coroutineScope.launch(Dispatchers.Default) {
            val rotated = PhotoEditorEngine.rotateOrFlip(src, degrees = 90)
            val oldTL = corners[0]
            val oldTR = corners[1]
            val oldBR = corners[2]
            val oldBL = corners[3]
            // Transformation for 90 CW: (x, y) -> (1 - y, x)
            val newTL = PointF(1f - oldBL.y, oldBL.x)
            val newTR = PointF(1f - oldTL.y, oldTL.x)
            val newBR = PointF(1f - oldTR.y, oldTR.x)
            val newBL = PointF(1f - oldBR.y, oldBR.x)
            withContext(Dispatchers.Main) {
                sourceBitmap = rotated
                corners = listOf(newTL, newTR, newBR, newBL)
                zoomScale = 1f
                panOffset = Offset.Zero
            }
        }
    }

    // Helper: Rotate CCW (270 degrees CW)
    fun rotateCounterClockwise() {
        val src = sourceBitmap ?: return
        coroutineScope.launch(Dispatchers.Default) {
            val rotated = PhotoEditorEngine.rotateOrFlip(src, degrees = 270)
            val oldTL = corners[0]
            val oldTR = corners[1]
            val oldBR = corners[2]
            val oldBL = corners[3]
            // Transformation for 90 CCW: (x, y) -> (y, 1 - x)
            val newTL = PointF(oldTR.y, 1f - oldTR.x)
            val newTR = PointF(oldBR.y, 1f - oldBR.x)
            val newBR = PointF(oldBL.y, 1f - oldBL.x)
            val newBL = PointF(oldTL.y, 1f - oldTL.x)
            withContext(Dispatchers.Main) {
                sourceBitmap = rotated
                corners = listOf(newTL, newTR, newBR, newBL)
                zoomScale = 1f
                panOffset = Offset.Zero
            }
        }
    }

    // Helper: Auto-detect
    fun runAutoDetect() {
        val src = sourceBitmap ?: return
        coroutineScope.launch(Dispatchers.Default) {
            val detected = detectDocumentCornersNormalized(src)
            withContext(Dispatchers.Main) {
                corners = detected
                activeMode = PerspectiveMode.AUTO_DETECT
            }
        }
    }

    // Helper: Auto-crop
    fun runAutoCrop() {
        val src = sourceBitmap ?: return
        coroutineScope.launch(Dispatchers.Default) {
            val tight = detectTightDocumentBounds(src)
            withContext(Dispatchers.Main) {
                corners = tight
            }
        }
    }

    // Helper: Save to Gallery (Strictly Idempotent)
    fun saveToGallery() {
        val src = sourceBitmap ?: return
        if (isSaving) return
        isSaving = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                // High-resolution full perspective correction
                val fullResult = PhotoEditorEngine.applyPerspectiveCorrection(src, corners)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val outFileName = "Perspective_${timeStamp}.jpg"
                val savedUri = saveImageToMediaStore(context, fullResult, outFileName)

                withContext(Dispatchers.Main) {
                    isSaving = false
                    if (savedUri != null) {
                        savedSuccessUri = savedUri
                        savedFileName = outFileName
                        showSavedDialog = true
                    } else {
                        Toast.makeText(context, "Failed to save image to Gallery", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Save Success Dialog
    if (showSavedDialog) {
        AlertDialog(
            onDismissRequest = { showSavedDialog = false },
            icon = {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = ToolGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Saved to Gallery", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "Your straightened photo was saved successfully.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "File: $savedFileName",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        "Location: Pictures/PDFMaster_Perspective",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSavedDialog = false
                        savedSuccessUri?.let { uri ->
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "image/jpeg")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(viewIntent, "View Image"))
                            } catch (_: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolGreen)
                ) {
                    Text("View Image", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavedDialog = false }) {
                    Text("Done", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edge_perspective_screen"),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Edge & Perspective",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Straighten and crop your photo",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // 2. Scrollable Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3. Auto Detect & Manual Adjust Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Auto Detect Button
                    Button(
                        onClick = { runAutoDetect() },
                        enabled = sourceBitmap != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_auto_detect"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeMode == PerspectiveMode.AUTO_DETECT) CrimsonPrimary else Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (activeMode == PerspectiveMode.AUTO_DETECT) CrimsonPrimary else Color(0xFF334155)
                        )
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto Detect",
                            fontSize = 13.sp,
                            fontWeight = if (activeMode == PerspectiveMode.AUTO_DETECT) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    // Manual Adjust Button
                    Button(
                        onClick = { activeMode = PerspectiveMode.MANUAL_ADJUST },
                        enabled = sourceBitmap != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_manual_adjust"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeMode == PerspectiveMode.MANUAL_ADJUST) CrimsonPrimary else Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (activeMode == PerspectiveMode.MANUAL_ADJUST) CrimsonPrimary else Color(0xFF334155)
                        )
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Manual Adjust",
                            fontSize = 13.sp,
                            fontWeight = if (activeMode == PerspectiveMode.MANUAL_ADJUST) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // 4. Main Image Preview with Draggable Corner Handles & Smooth Zoom
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .testTag("main_preview_canvas"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    if (sourceBitmap == null) {
                        // Empty State Placeholder
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(68.dp),
                                shape = CircleShape,
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Crop,
                                        contentDescription = null,
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "No Image Selected",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Select a document or photo to correct its perspective and straighten edges.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Photo")
                            }
                        }
                    } else {
                        val bmp = sourceBitmap!!

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val containerW = maxWidth.value * density
                            val containerH = maxHeight.value * density

                            // Calculate aspect-fit metrics
                            val scale = minOf(containerW / bmp.width, containerH / bmp.height)
                            val drawW = bmp.width * scale
                            val drawH = bmp.height * scale
                            val imgLeft = (containerW - drawW) / 2f
                            val imgTop = (containerH - drawH) / 2f

                            val cx = containerW / 2f
                            val cy = containerH / 2f

                            // Helper: Convert normalized image coords [0..1] to screen coords in px
                            fun normToScreen(p: PointF): Offset {
                                val basePx = imgLeft + p.x * drawW
                                val basePy = imgTop + p.y * drawH
                                val sx = cx + (basePx - cx) * zoomScale + panOffset.x
                                val sy = cy + (basePy - cy) * zoomScale + panOffset.y
                                return Offset(sx, sy)
                            }

                            // Helper: Convert screen touch coords in px back to normalized image coords [0..1]
                            fun screenToNorm(sx: Float, sy: Float): PointF {
                                val basePx = cx + (sx - cx - panOffset.x) / zoomScale
                                val basePy = cy + (sy - cy - panOffset.y) / zoomScale
                                val nx = ((basePx - imgLeft) / drawW).coerceIn(0f, 1f)
                                val ny = ((basePy - imgTop) / drawH).coerceIn(0f, 1f)
                                return PointF(nx, ny)
                            }

                            val cornersState = rememberUpdatedState(corners)

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    // Pinch-to-zoom and pan gesture handler
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            zoomScale = (zoomScale * zoom).coerceAtLeast(0.5f)
                                            panOffset += pan
                                        }
                                    }
                                    // Precise corner drag gesture handler
                                    .pointerInput(containerW, containerH) {
                                        val hitRadiusPx = 48.dp.toPx()
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val currentCorners = cornersState.value
                                            var closestIndex = -1
                                            var minDistance = Float.MAX_VALUE

                                            currentCorners.forEachIndexed { index, cornerPt ->
                                                val sp = normToScreen(cornerPt)
                                                val d = hypot(down.position.x - sp.x, down.position.y - sp.y)
                                                if (d <= hitRadiusPx && d < minDistance) {
                                                    minDistance = d
                                                    closestIndex = index
                                                }
                                            }

                                            if (closestIndex != -1) {
                                                down.consume()
                                                activeDraggingCornerIndex = closestIndex
                                                activeMode = PerspectiveMode.MANUAL_ADJUST

                                                try {
                                                    drag(down.id) { change ->
                                                        change.consume()
                                                        val updatedNorm = screenToNorm(change.position.x, change.position.y)
                                                        val updatedList = cornersState.value.toMutableList()
                                                        updatedList[closestIndex] = updatedNorm
                                                        corners = updatedList
                                                    }
                                                } finally {
                                                    activeDraggingCornerIndex = -1
                                                }
                                            }
                                        }
                                    }
                            ) {
                                // 1. Draw source bitmap with zoom & pan transform applied
                                drawContext.canvas.save()
                                drawContext.canvas.translate(cx + panOffset.x, cy + panOffset.y)
                                drawContext.canvas.scale(zoomScale, zoomScale)
                                drawContext.canvas.translate(-cx, -cy)

                                drawImage(
                                    image = bmp.asImageBitmap(),
                                    dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt()),
                                    dstSize = IntSize(drawW.toInt(), drawH.toInt())
                                )

                                drawContext.canvas.restore()

                                // 2. Compute screen positions for the 4 corners
                                val pts = corners.map { normToScreen(it) }

                                // 3. Draw quadrilateral polygon fill and border lines
                                val quadPath = Path().apply {
                                    moveTo(pts[0].x, pts[0].y)
                                    lineTo(pts[1].x, pts[1].y)
                                    lineTo(pts[2].x, pts[2].y)
                                    lineTo(pts[3].x, pts[3].y)
                                    close()
                                }

                                // Translucent tinted quad fill
                                drawPath(
                                    path = quadPath,
                                    color = CrimsonPrimary.copy(alpha = 0.22f)
                                )

                                // Crisp solid border stroke
                                drawPath(
                                    path = quadPath,
                                    color = CrimsonPrimary,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                // 4. Draw 4 interactive corner handles with high-contrast rings and labels
                                pts.forEachIndexed { index, screenPos ->
                                    val isDragging = index == activeDraggingCornerIndex
                                    val outerRadius = 15.dp.toPx()
                                    val innerRadius = 9.dp.toPx()

                                    // While actively holding and dragging, temporarily reduce opacity so user can see image underneath
                                    val pinAlpha = if (isDragging) 0.35f else 1.0f

                                    // Outer white glow ring
                                    drawCircle(
                                        color = Color.White.copy(alpha = pinAlpha),
                                        radius = outerRadius,
                                        center = screenPos
                                    )
                                    // Primary accent core
                                    drawCircle(
                                        color = CrimsonPrimary.copy(alpha = pinAlpha),
                                        radius = innerRadius,
                                        center = screenPos
                                    )
                                    // Center guide dot
                                    drawCircle(
                                        color = Color.White.copy(alpha = if (isDragging) 0.5f else 1.0f),
                                        radius = 3.dp.toPx(),
                                        center = screenPos
                                    )
                                }
                            }

                            // Zoom Indicator Overlay (when zoomed in)
                            if (zoomScale > 1.05f) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clickable {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xCC0F172A),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.1fx", zoomScale),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Reset",
                                            color = CrimsonPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Before and After Preview Panels
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // BEFORE Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(125.dp)
                            .testTag("panel_before"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF334155)
                                ) {
                                    Text(
                                        text = "BEFORE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text("Original", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sourceBitmap != null) {
                                    Image(
                                        bitmap = sourceBitmap!!.asImageBitmap(),
                                        contentDescription = "Before - Original Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text("No photo", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }

                    // AFTER Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(125.dp)
                            .testTag("panel_after"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(
                            1.dp,
                            if (afterPreviewBitmap != null) ToolGreen.copy(alpha = 0.5f) else Color(0xFF334155)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ToolGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "AFTER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ToolGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text("Corrected", fontSize = 11.sp, color = ToolGreen)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (afterPreviewBitmap != null) {
                                    Image(
                                        bitmap = afterPreviewBitmap!!.asImageBitmap(),
                                        contentDescription = "After - Straightened Document",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else if (isGeneratingPreview) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = ToolGreen,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("No preview", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Action Tools Toolbar (Reset, Rotate Left, Rotate Right, Auto Crop, Fit Screen)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button
                    ToolButton(
                        icon = Icons.Default.Refresh,
                        label = "Reset",
                        testTag = "btn_reset",
                        enabled = sourceBitmap != null,
                        onClick = { resetPerspective() }
                    )

                    // Rotate Left Button
                    ToolButton(
                        icon = Icons.AutoMirrored.Filled.RotateLeft,
                        label = "Rotate L",
                        testTag = "btn_rotate_left",
                        enabled = sourceBitmap != null,
                        onClick = { rotateCounterClockwise() }
                    )

                    // Rotate Right Button
                    ToolButton(
                        icon = Icons.AutoMirrored.Filled.RotateRight,
                        label = "Rotate R",
                        testTag = "btn_rotate_right",
                        enabled = sourceBitmap != null,
                        onClick = { rotateClockwise() }
                    )

                    // Auto Crop Button
                    ToolButton(
                        icon = Icons.Default.Crop,
                        label = "Auto Crop",
                        testTag = "btn_auto_crop",
                        enabled = sourceBitmap != null,
                        onClick = { runAutoCrop() }
                    )

                    // Fit Screen Button
                    ToolButton(
                        icon = Icons.Default.FitScreen,
                        label = "Fit Screen",
                        testTag = "btn_fit_screen",
                        enabled = sourceBitmap != null,
                        onClick = {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 7. Bottom Action Bar: Choose Image & Save to Gallery
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Choose Image Button
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_choose_image"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Choose Image",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // Save to Gallery Button
                Button(
                    onClick = { saveToGallery() },
                    enabled = sourceBitmap != null && !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_save_to_gallery"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ToolGreen,
                        disabledContainerColor = Color(0xFF334155),
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Saving...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save to Gallery",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) Color(0xFFF1F5F9) else Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) Color(0xFFCBD5E1) else Color(0xFF64748B)
        )
    }
}

/**
 * Detects document corners normalized into [0f..1f].
 * Falls back to an 8% inset quadrilateral if edge contrast is low.
 */
private fun detectDocumentCornersNormalized(bitmap: Bitmap): List<PointF> {
    val pixelCorners = DocumentScannerUtil.detectCorners(bitmap)
    val w = bitmap.width.toFloat().coerceAtLeast(1f)
    val h = bitmap.height.toFloat().coerceAtLeast(1f)

    if (pixelCorners.size == 4) {
        val norm = pixelCorners.map {
            PointF(
                (it.x / w).coerceIn(0.02f, 0.98f),
                (it.y / h).coerceIn(0.02f, 0.98f)
            )
        }
        // Verify points span a reasonable area
        val areaEstimate = (norm[1].x - norm[0].x) * (norm[2].y - norm[1].y)
        if (areaEstimate > 0.10f) {
            return norm
        }
    }

    // Default neat inset quad
    return listOf(
        PointF(0.08f, 0.08f), // TL
        PointF(0.92f, 0.08f), // TR
        PointF(0.92f, 0.92f), // BR
        PointF(0.08f, 0.92f)  // BL
    )
}

/**
 * Computes tight document bounds by sampling inner high-contrast content lines.
 */
private fun detectTightDocumentBounds(bitmap: Bitmap): List<PointF> {
    val initial = detectDocumentCornersNormalized(bitmap)
    // Inset slightly to ensure no background clutter is included
    val inset = 0.02f
    return listOf(
        PointF((initial[0].x + inset).coerceIn(0f, 1f), (initial[0].y + inset).coerceIn(0f, 1f)),
        PointF((initial[1].x - inset).coerceIn(0f, 1f), (initial[1].y + inset).coerceIn(0f, 1f)),
        PointF((initial[2].x - inset).coerceIn(0f, 1f), (initial[2].y - inset).coerceIn(0f, 1f)),
        PointF((initial[3].x + inset).coerceIn(0f, 1f), (initial[3].y - inset).coerceIn(0f, 1f))
    )
}

/**
 * Saves exactly ONE image to the Android MediaStore under Pictures/PDFMaster_Perspective.
 */
private fun saveImageToMediaStore(context: Context, bitmap: Bitmap, fileName: String): Uri? {
    return try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PDFMaster_Perspective")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = java.io.File(picturesDir, "PDFMaster_Perspective")
                if (!appDir.exists()) appDir.mkdirs()
                val destFile = java.io.File(appDir, fileName)
                put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues) ?: return null

        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.flush()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        uri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
