package com.example.ui.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.PointF

enum class ScannerState {
    CHOOSING_SOURCE,
    CROP_ADJUST,
    PROCESSING,
    PREVIEW_RESULT,
    SAVED
}

@Composable
fun ScannerModalDialog(
    isEdgePerspectiveOnly: Boolean = false,
    onDismiss: () -> Unit,
    onSavePdf: ((List<Bitmap>, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentState by remember { mutableStateOf(ScannerState.CHOOSING_SOURCE) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    
    var corners by remember { mutableStateOf<List<PointF>>(emptyList()) }
    var transformedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var filterMode by remember { mutableStateOf("Original") } // Original, Grayscale, B&W, Color
    
    var savedFileUri by remember { mutableStateOf<Uri?>(null) }
    var savedFilePath by remember { mutableStateOf("") }
    var savedFileSize by remember { mutableStateOf(0L) }

    // Temporary file for camera
    val tempImageFile = remember {
        File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
    }
    val cameraUri = remember {
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", tempImageFile)
        } catch (e: Exception) { null }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bmp = BitmapFactory.decodeFile(tempImageFile.absolutePath)
                    if (bmp != null) {
                        val c = DocumentScannerUtil.detectCorners(bmp)
                        withContext(Dispatchers.Main) {
                            currentBitmap = bmp
                            corners = c
                            currentState = ScannerState.CROP_ADJUST
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            val c = DocumentScannerUtil.detectCorners(bmp)
                            withContext(Dispatchers.Main) {
                                currentBitmap = bmp
                                corners = c
                                currentState = ScannerState.CROP_ADJUST
                            }
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
    
    // Automatically launch gallery if it's edge perspective only
    // Removed to allow user to choose between Camera and Gallery

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edge & Perspective",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
                    )
                }

                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    when (currentState) {
                        ScannerState.CHOOSING_SOURCE -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(
                                    onClick = { cameraUri?.let { cameraLauncher.launch(it) } },
                                    modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Camera")
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gallery")
                                }
                            }
                        }
                        
                        ScannerState.CROP_ADJUST -> {
                            currentBitmap?.let { bmp ->
                                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    val canvasW = maxWidth.value
                                    val canvasH = maxHeight.value
                                    
                                    val scale = minOf(canvasW / bmp.width, canvasH / bmp.height)
                                    val drawW = bmp.width * scale
                                    val drawH = bmp.height * scale
                                    val offsetX = (canvasW - drawW) / 2f
                                    val offsetY = (canvasH - drawH) / 2f
                                    
                                    val density = LocalDensity.current.density
                                    
                                    // Helper to convert bitmap coordinates to screen coordinates
                                    fun ptToScreen(p: PointF): Offset {
                                        return Offset(
                                            offsetX * density + p.x * scale * density,
                                            offsetY * density + p.y * scale * density
                                        )
                                    }
                                    
                                    // Helper to convert screen coordinates to bitmap coordinates
                                    fun screenToPt(o: Offset): PointF {
                                        return PointF(
                                            ((o.x / density - offsetX) / scale).coerceIn(0f, bmp.width.toFloat()),
                                            ((o.y / density - offsetY) / scale).coerceIn(0f, bmp.height.toFloat())
                                        )
                                    }
                                    
                                    var draggingIdx by remember { mutableStateOf(-1) }

                                    Canvas(
                                        modifier = Modifier.fillMaxSize()
                                            .pointerInput(Unit) {
                                                detectTransformGestures { _, pan, zoom, _ ->
                                                    zoomScale = (zoomScale * zoom).coerceAtLeast(0.1f)
                                                    panOffset += pan
                                                }
                                            }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onDoubleTap = {
                                                        zoomScale = if (zoomScale > 1f) 1f else 2.5f
                                                        panOffset = Offset.Zero
                                                    }
                                                )
                                            }
                                            .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = { start ->
                                                    val touch = start
                                                    draggingIdx = corners.indexOfFirst { p ->
                                                        val sp = ptToScreen(p)
                                                        val dx = sp.x - touch.x
                                                        val dy = sp.y - touch.y
                                                        Math.hypot(dx.toDouble(), dy.toDouble()) < 100f
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    if (draggingIdx in corners.indices) {
                                                        val newPos = change.position
                                                        val newCorners = corners.toMutableList()
                                                        newCorners[draggingIdx] = screenToPt(newPos)
                                                        corners = newCorners
                                                    } else {
                                                        panOffset += dragAmount
                                                    }
                                                },
                                                onDragEnd = { draggingIdx = -1 },
                                                onDragCancel = { draggingIdx = -1 }
                                            )
                                        }
                                        .graphicsLayer {
                                            scaleX = zoomScale
                                            scaleY = zoomScale
                                            translationX = panOffset.x
                                            translationY = panOffset.y
                                        }
                                    ) {
                                        // Draw Image
                                        drawImage(
                                            image = bmp.asImageBitmap(),
                                            dstSize = androidx.compose.ui.unit.IntSize((drawW*density).toInt(), (drawH*density).toInt()),
                                            dstOffset = androidx.compose.ui.unit.IntOffset((offsetX*density).toInt(), (offsetY*density).toInt())
                                        )
                                        
                                        // Draw Polygon
                                        if (corners.size == 4) {
                                            val path = androidx.compose.ui.graphics.Path().apply {
                                                val p0 = ptToScreen(corners[0])
                                                moveTo(p0.x, p0.y)
                                                for (i in 1..3) {
                                                    val p = ptToScreen(corners[i])
                                                    lineTo(p.x, p.y)
                                                }
                                                close()
                                            }
                                            drawPath(
                                                path = path,
                                                color = CrimsonPrimary.copy(alpha = 0.3f)
                                            )
                                            drawPath(
                                                path = path,
                                                color = CrimsonPrimary,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                                            )
                                            
                                            // Draw Corners
                                            for (p in corners) {
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = 20f,
                                                    center = ptToScreen(p)
                                                )
                                                drawCircle(
                                                    color = CrimsonPrimary,
                                                    radius = 16f,
                                                    center = ptToScreen(p)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        ScannerState.PROCESSING -> {
                            CircularProgressIndicator(color = CrimsonPrimary)
                        }
                        
                        ScannerState.PREVIEW_RESULT -> {
                            transformedBitmap?.let { bmp ->
                                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    val canvasW = maxWidth.value
                                    val canvasH = maxHeight.value
                                    val scale = minOf(canvasW / bmp.width, canvasH / bmp.height)
                                    val drawW = bmp.width * scale
                                    val drawH = bmp.height * scale
                                    val offsetX = (canvasW - drawW) / 2f
                                    val offsetY = (canvasH - drawH) / 2f
                                    val density = LocalDensity.current.density
                                    
                                    Canvas(
                                        modifier = Modifier.fillMaxSize()
                                            .pointerInput(Unit) {
                                                detectTransformGestures { _, pan, zoom, _ ->
                                                    zoomScale = (zoomScale * zoom).coerceAtLeast(0.1f)
                                                    panOffset += pan
                                                }
                                            }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onDoubleTap = {
                                                        zoomScale = if (zoomScale > 1f) 1f else 2.5f
                                                        panOffset = Offset.Zero
                                                    }
                                                )
                                            }
                                            .graphicsLayer {
                                                scaleX = zoomScale
                                                scaleY = zoomScale
                                                translationX = panOffset.x
                                                translationY = panOffset.y
                                            }
                                    ) {
                                        drawImage(
                                            image = bmp.asImageBitmap(),
                                            dstSize = androidx.compose.ui.unit.IntSize((drawW*density).toInt(), (drawH*density).toInt()),
                                            dstOffset = androidx.compose.ui.unit.IntOffset((offsetX*density).toInt(), (offsetY*density).toInt())
                                        )
                                    }
                                }
                            }
                        }
                        
                        ScannerState.SAVED -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Saved Successfully", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Name: ${savedFilePath.substringAfterLast("/")}", color = Color.Gray, fontSize = 14.sp)
                                Text("Size: ${formatFileSizeScanner(savedFileSize)}", color = Color.Gray, fontSize = 14.sp)
                                Text("Location: PDFMaster_Scans", color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Button(
                                        onClick = { 
                                            savedFileUri?.let { uri ->
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "image/jpeg")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Open Image"))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                    ) {
                                        Text("Open")
                                    }
                                    Button(
                                        onClick = { 
                                            // View Folder - opens gallery/photos app
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                type = "image/*"
                                            }
                                            context.startActivity(Intent.createChooser(intent, "View Folder"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                    ) {
                                        Text("Folder")
                                    }
                                    Button(
                                        onClick = { 
                                            savedFileUri?.let { shareImageUri(context, it) }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                    ) {
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Bottom Bar Actions
                if (currentState == ScannerState.CROP_ADJUST) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { 
                            currentBitmap = null
                            currentState = ScannerState.CHOOSING_SOURCE
                        }) {
                            Text("Retake", color = Color.White)
                        }
                        Button(
                            onClick = {
                                currentState = ScannerState.PROCESSING
                                coroutineScope.launch(Dispatchers.IO) {
                                    currentBitmap?.let { bmp ->
                                        val transformed = DocumentScannerUtil.applyPerspective(
                                            bmp, corners[0], corners[1], corners[2], corners[3]
                                        )
                                        withContext(Dispatchers.Main) {
                                            transformedBitmap = DocumentScannerUtil.applyFilter(transformed, filterMode)
                                            currentState = ScannerState.PREVIEW_RESULT
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Text("Crop / Apply")
                        }
                    }
                } else if (currentState == ScannerState.PREVIEW_RESULT) {
                    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B))) {
                        LazyRow(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val filters = listOf("Original", "Color", "Grayscale", "B&W")
                            items(filters) { f ->
                                FilterChip(
                                    selected = filterMode == f,
                                    onClick = {
                                        filterMode = f
                                        currentState = ScannerState.PROCESSING
                                        coroutineScope.launch(Dispatchers.IO) {
                                            currentBitmap?.let { bmp ->
                                                val transformed = DocumentScannerUtil.applyPerspective(
                                                    bmp, corners[0], corners[1], corners[2], corners[3]
                                                )
                                                val filtered = DocumentScannerUtil.applyFilter(transformed, filterMode)
                                                withContext(Dispatchers.Main) {
                                                    transformedBitmap = filtered
                                                    currentState = ScannerState.PREVIEW_RESULT
                                                }
                                            }
                                        }
                                    },
                                    label = { Text(f) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { currentState = ScannerState.CROP_ADJUST }) {
                                Text("Adjust Corners", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        transformedBitmap?.let { bmp ->
                                            try {
                                                val savedInfo = saveScannedImage(context, bmp, isEdgePerspectiveOnly)
                                                if (savedInfo != null) {
                                                    // Add to recent files
                                                    com.example.storage.GlobalSaveManager.saveProcessedFile(
                                                        context = context,
                                                        tempSourceFile = savedInfo.second,
                                                        desiredFileName = savedInfo.second.name,
                                                        mimeType = "image/jpeg",
                                                        category = "scans",
                                                        tag = if (isEdgePerspectiveOnly) "Edge & Perspective" else "Scanned Document"
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        savedFileUri = savedInfo.first
                                                        savedFilePath = savedInfo.second.absolutePath
                                                        savedFileSize = savedInfo.second.length()
                                                        currentState = ScannerState.SAVED
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ToolGreen)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save to Gallery")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatFileSizeScanner(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

fun saveScannedImage(context: Context, bitmap: Bitmap, isEdgePerspectiveOnly: Boolean): Pair<Uri, File>? {
    try {
        val prefix = if (isEdgePerspectiveOnly) "EdgePerspective" else "Scanned"
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val baseName = "${prefix}_${timestamp}"
        val extension = "jpg"
        var displayName = "${baseName}.jpg"
        
        val cacheFile = File(context.cacheDir, displayName)
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        val resolver = context.contentResolver
        var counter = 1
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        while (true) {
            val cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                arrayOf(displayName),
                null
            )
            val exists = cursor?.use { it.count > 0 } ?: false
            if (!exists) break
            displayName = "${baseName}_${counter}.$extension"
            counter++
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PDFMaster_Scans")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "PDFMaster_Scans")
                if (!appDir.exists()) appDir.mkdirs()
                val destFile = File(appDir, displayName)
                put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
            }
        }

        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues) ?: return null
        resolver.openOutputStream(uri)?.use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
            outStream.flush()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        
        val finalFile = File(context.cacheDir, displayName)
        cacheFile.renameTo(finalFile)

        return Pair(uri, finalFile)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun shareImageUri(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Scanned Document"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
