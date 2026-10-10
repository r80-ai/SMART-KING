package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storage.GlobalSaveManager
import com.example.ui.PdfMasterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameDatePhotoScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                sourceBitmap = internalLoadBitmap(context, uri)
                isProcessing = false
            }
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            date = sdf.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun generatePreview() {
        val src = sourceBitmap ?: return
        if (name.isBlank() && date.isBlank()) {
            resultBitmap = src
            return
        }
        scope.launch {
            isProcessing = true
            resultBitmap = withContext(Dispatchers.Default) {
                overlayNameAndDate(src, name, date)
            }
            isProcessing = false
        }
    }

    LaunchedEffect(name, date, sourceBitmap) {
        generatePreview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Name & Date on Photo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (resultBitmap != null) {
                        IconButton(onClick = {
                            scope.launch {
                                savePassportPhoto(context, resultBitmap!!, viewModel)
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save JPEG")
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
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator()
                } else if (resultBitmap != null) {
                    Image(
                        bitmap = resultBitmap!!.asImageBitmap(),
                        contentDescription = "Result Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, Modifier.size(64.dp), tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Text("Select Photo")
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Candidate Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. RAHUL GUPTA") }
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (DOB or DOP)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("DD/MM/YYYY") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    }
                )
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

private fun overlayNameAndDate(src: Bitmap, name: String, date: String): Bitmap {
    val width = src.width
    val height = src.height
    
    val barHeight = (height * 0.18f).toInt()
    val dest = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(dest)
    
    canvas.drawBitmap(src, 0f, 0f, null)
    
    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, (height - barHeight).toFloat(), width.toFloat(), height.toFloat(), paint)
    
    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    
    val totalLines = if (name.isNotBlank() && date.isNotBlank()) 2 else 1
    val fontSize = barHeight / (totalLines + 1.2f)
    textPaint.textSize = fontSize
    
    val centerX = width / 2f
    if (name.isNotBlank() && date.isNotBlank()) {
        canvas.drawText(name.uppercase(), centerX, height - barHeight + fontSize * 0.9f, textPaint)
        canvas.drawText(date, centerX, height - barHeight + fontSize * 2.1f, textPaint)
    } else if (name.isNotBlank()) {
        canvas.drawText(name.uppercase(), centerX, height - barHeight / 2f + fontSize / 3f, textPaint)
    } else if (date.isNotBlank()) {
        canvas.drawText(date, centerX, height - barHeight / 2f + fontSize / 3f, textPaint)
    }
    
    return dest
}

private suspend fun savePassportPhoto(
    context: android.content.Context, 
    bitmap: Bitmap, 
    viewModel: PdfMasterViewModel
) = withContext(Dispatchers.IO) {
    val time = System.currentTimeMillis()
    val name = "Passport_NameDate_$time.jpg"
    
    try {
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { 
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
        }
        val result = GlobalSaveManager.saveProcessedFile(
            context, file, name, "image/jpeg", "scanned", "Name-Date Photo"
        )
        viewModel.setNotification("Saved to ${result.displayLocation}/${result.fileName}")
    } catch (e: Exception) {
        viewModel.setNotification("Save failed: ${e.message}")
    }
}
