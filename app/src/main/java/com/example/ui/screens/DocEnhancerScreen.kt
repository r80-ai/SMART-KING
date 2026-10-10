package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEnhancerScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var brightness by remember { mutableStateOf(1f) }
    var contrast by remember { mutableStateOf(1f) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                sourceBitmap = internalLoadBitmap(context, uri)
                enhancedBitmap = sourceBitmap
                isProcessing = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            sourceBitmap = bitmap
            enhancedBitmap = bitmap
        }
    }

    fun applyFilters(type: String) {
        val src = sourceBitmap ?: return
        scope.launch {
            isProcessing = true
            enhancedBitmap = withContext(Dispatchers.Default) {
                when (type) {
                    "BW" -> applyBWPhotocopyFilter(src)
                    "MAGIC" -> applyMagicColorFilter(src)
                    "MANUAL" -> applyManualAdjustments(src, brightness, contrast)
                    else -> src
                }
            }
            isProcessing = false
        }
    }

    LaunchedEffect(brightness, contrast) {
        if (sourceBitmap != null) {
            applyFilters("MANUAL")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doc Scanner & Enhancer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (enhancedBitmap != null) {
                        IconButton(onClick = {
                            scope.launch {
                                saveEnhanced(context, enhancedBitmap!!, "JPG", viewModel)
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save JPG")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                saveEnhanced(context, enhancedBitmap!!, "PDF", viewModel)
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Save PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator()
                } else if (enhancedBitmap != null) {
                    Image(
                        bitmap = enhancedBitmap!!.asImageBitmap(),
                        contentDescription = "Enhanced Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Import a document to start", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row {
                            Button(onClick = { cameraLauncher.launch(null) }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Camera")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gallery")
                            }
                        }
                    }
                }
            }

            if (sourceBitmap != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Filters", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { applyFilters("BW") },
                            label = { Text("B&W Photocopy") },
                            leadingIcon = { Icon(Icons.Default.FilterBAndW, contentDescription = null, Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { applyFilters("MAGIC") },
                            label = { Text("Magic Color") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { 
                                brightness = 1f
                                contrast = 1f
                                enhancedBitmap = sourceBitmap
                            },
                            label = { Text("Reset") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Manual Adjustments", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    
                    Text("Brightness: ${(brightness * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = 0.5f..2f
                    )

                    Text("Contrast: ${(contrast * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = contrast,
                        onValueChange = { contrast = it },
                        valueRange = 0.5f..2.5f
                    )
                }
            }
        }
    }
}

private suspend fun internalLoadBitmap(context: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { 
            android.graphics.BitmapFactory.decodeStream(it)
        }
    } catch (e: Exception) {
        null
    }
}

private fun applyBWPhotocopyFilter(src: Bitmap): Bitmap {
    val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(dest)
    val paint = Paint()
    
    val cm = ColorMatrix()
    cm.setSaturation(0f)
    
    val contrast = 2.0f
    val brightness = 10f
    val scale = contrast
    val translate = brightness - (0.5f * scale - 0.5f) * 255f
    
    val matrix = floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    )
    
    cm.postConcat(ColorMatrix(matrix))
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(src, 0f, 0f, paint)
    return dest
}

private fun applyMagicColorFilter(src: Bitmap): Bitmap {
    val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(dest)
    val paint = Paint()
    
    val cm = ColorMatrix()
    cm.setSaturation(1.4f)
    
    val scale = 1.2f
    val translate = -10f
    val matrix = floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    )
    cm.postConcat(ColorMatrix(matrix))
    
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(src, 0f, 0f, paint)
    return dest
}

private fun applyManualAdjustments(src: Bitmap, brightness: Float, contrast: Float): Bitmap {
    val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(dest)
    val paint = Paint()
    
    val scale = contrast
    val translate = (brightness - 1f) * 255f - (0.5f * scale - 0.5f) * 255f
    
    val matrix = floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    )
    
    paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
    canvas.drawBitmap(src, 0f, 0f, paint)
    return dest
}

private suspend fun saveEnhanced(
    context: android.content.Context, 
    bitmap: Bitmap, 
    format: String,
    viewModel: PdfMasterViewModel
) = withContext(Dispatchers.IO) {
    val time = System.currentTimeMillis()
    val name = "Enhanced_Doc_$time"
    
    try {
        if (format == "JPG") {
            val file = File(context.cacheDir, "$name.jpg")
            FileOutputStream(file).use { 
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
            }
            val result = GlobalSaveManager.saveProcessedFile(
                context, file, "$name.jpg", "image/jpeg", "scanned", "Doc Enhancer"
            )
            viewModel.setNotification("Saved to ${result.displayLocation}/${result.fileName}")
        } else {
            val pdfFile = PdfEngine.createPdfFromBitmaps(context, listOf(bitmap), "$name.pdf")
            val result = GlobalSaveManager.saveProcessedFile(
                context, pdfFile, "$name.pdf", "application/pdf", "scanned", "Doc Enhancer"
            )
            viewModel.setNotification("Saved to ${result.displayLocation}/${result.fileName}")
        }
    } catch (e: Exception) {
        viewModel.setNotification("Save failed: ${e.message}")
    }
}
