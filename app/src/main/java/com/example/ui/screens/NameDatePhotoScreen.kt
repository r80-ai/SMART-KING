package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
import kotlin.math.max

enum class DatePrefixOption(val label: String, val prefix: String) {
    DOP("D.O.P. :", "D.O.P. : "),
    DOB("D.O.B. :", "D.O.B. : "),
    NONE("None", "")
}

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
    var datePrefix by remember { mutableStateOf(DatePrefixOption.DOP) }
    
    // Border On / Off switch state
    var isBorderOn by remember { mutableStateOf(true) }
    
    var barHeightFactor by remember { mutableFloatStateOf(0.18f) }
    var showOriginal by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Pick photo from gallery
    val galleryPicker = rememberLauncherForActivityResult(
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

    // Capture photo from camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            sourceBitmap = bitmap
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

    fun applyNameDateOverlay() {
        val src = sourceBitmap ?: return
        scope.launch {
            isProcessing = true
            resultBitmap = withContext(Dispatchers.Default) {
                overlayNameAndDate(
                    src = src,
                    name = name,
                    date = date,
                    datePrefix = datePrefix.prefix,
                    isBorderOn = isBorderOn,
                    barHeightFactor = barHeightFactor
                )
            }
            isProcessing = false
        }
    }

    LaunchedEffect(name, date, datePrefix, isBorderOn, barHeightFactor, sourceBitmap) {
        applyNameDateOverlay()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Name & Date on Photo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Passport & Exam Photo Creator",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (resultBitmap != null) {
                        IconButton(
                            onClick = {
                                sharePassportPhoto(context, resultBitmap!!)
                            },
                            modifier = Modifier.testTag("btn_share_namedate_photo")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Photo")
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    savePassportPhoto(context, resultBitmap!!, name, viewModel)
                                }
                            },
                            modifier = Modifier.testTag("btn_save_namedate_photo")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Photo")
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
            // Preview Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    } else if (sourceBitmap != null) {
                        val displayBmp = if (showOriginal) sourceBitmap!! else (resultBitmap ?: sourceBitmap!!)
                        val borderModifier = if (isBorderOn) {
                            Modifier.border(4.dp, androidx.compose.ui.graphics.Color.Black, RoundedCornerShape(0.dp))
                        } else {
                            Modifier
                        }

                        Image(
                            bitmap = displayBmp.asImageBitmap(),
                            contentDescription = "Passport Photo Preview",
                            modifier = Modifier
                                .fillMaxHeight()
                                .then(borderModifier),
                            contentScale = ContentScale.Fit
                        )

                        // Top Badges Overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Resolution Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    text = "${sourceBitmap!!.width} × ${sourceBitmap!!.height} px",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Compare Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (showOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    contentColor = if (showOriginal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.clickable { showOriginal = !showOriginal }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (showOriginal) "Original" else "Preview",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Change Button in top bar next to Preview
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Change",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty State - Import Prompts
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select Candidate Passport Photo",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upload passport photo to add Name, Date & Border",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier.testTag("btn_select_gallery_photo")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gallery")
                                }
                                OutlinedButton(
                                    onClick = { cameraLauncher.launch(null) },
                                    modifier = Modifier.testTag("btn_capture_camera_photo")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Camera")
                                }
                            }
                        }
                    }
                }
            }

            // Below Photo Preview Card: Change Photo Button
            if (sourceBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Candidate Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.uppercase() },
                    label = { Text("Candidate Name (প্রার্থীর নাম)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_candidate_name"),
                    singleLine = true,
                    placeholder = { Text("e.g. RAHUL GUPTA") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Name")
                            }
                        }
                    }
                )

                // Date Field
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (তারিখ: DOP / DOB)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_photo_date"),
                    singleLine = true,
                    placeholder = { Text("DD/MM/YYYY") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (date.isNotEmpty()) {
                                IconButton(onClick = { date = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Date")
                                }
                            }
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        }
                    }
                )

                // Date Presets & Prefix Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Date Format & Prefix:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DatePrefixOption.entries.forEach { option ->
                            FilterChip(
                                selected = datePrefix == option,
                                onClick = { datePrefix = option },
                                label = { Text(option.label) }
                            )
                        }
                        
                        AssistChip(
                            onClick = {
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                date = sdf.format(Calendar.getInstance().time)
                            },
                            label = { Text("Today") },
                            leadingIcon = {
                                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // BORDER TOGGLE (On / Off)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_photo_border_control"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBorderOn) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBorderOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CropSquare,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Border",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (isBorderOn) "4dp outer black border ON" else "Border OFF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isBorderOn,
                            onCheckedChange = { isBorderOn = it },
                            modifier = Modifier.testTag("switch_photo_border")
                        )
                    }
                }

                // Strip Height Fine-Tuning
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Name & Date Bar Height",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(barHeightFactor * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = barHeightFactor,
                            onValueChange = { barHeightFactor = it },
                            valueRange = 0.14f..0.25f,
                            steps = 11,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                Button(
                    onClick = {
                        val bmp = resultBitmap ?: sourceBitmap
                        if (bmp != null) {
                            scope.launch {
                                savePassportPhoto(context, bmp, name, viewModel)
                            }
                        } else {
                            Toast.makeText(context, "Please select a photo first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_photo_primary"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = (resultBitmap != null || sourceBitmap != null)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Passport Photo (JPG)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val bmp = resultBitmap ?: sourceBitmap
                            if (bmp != null) {
                                sharePassportPhoto(context, bmp)
                            } else {
                                Toast.makeText(context, "Please select a photo first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = (resultBitmap != null || sourceBitmap != null)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    OutlinedButton(
                        onClick = {
                            val bmp = resultBitmap ?: sourceBitmap
                            if (bmp != null) {
                                scope.launch {
                                    save8PhotoSheet(context, bmp, name, viewModel)
                                }
                            } else {
                                Toast.makeText(context, "Please select a photo first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = (resultBitmap != null || sourceBitmap != null)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("8-Photo Sheet")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private suspend fun internalLoadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { 
            android.graphics.BitmapFactory.decodeStream(it)
        }
    } catch (e: Exception) {
        null
    }
}

fun overlayNameAndDate(
    src: Bitmap,
    name: String,
    date: String,
    datePrefix: String,
    isBorderOn: Boolean,
    barHeightFactor: Float
): Bitmap {
    val width = src.width
    val height = src.height
    
    val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(dest)
    
    // Draw original image
    canvas.drawBitmap(src, 0f, 0f, null)
    
    val hasText = name.isNotBlank() || date.isNotBlank()
    val barHeight = (height * barHeightFactor).toInt()
    
    // Draw white info bar at bottom if text exists or border is requested
    if (hasText) {
        val barPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, (height - barHeight).toFloat(), width.toFloat(), height.toFloat(), barPaint)
        
        val fullDateText = if (date.isNotBlank()) "$datePrefix$date" else ""
        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val totalLines = if (name.isNotBlank() && fullDateText.isNotBlank()) 2 else 1
        var fontSize = barHeight / (totalLines + 1.25f)
        
        // Measure text width and scale down if it exceeds canvas margins
        textPaint.textSize = fontSize
        val maxAvailableWidth = width * 0.90f
        val nameW = if (name.isNotBlank()) textPaint.measureText(name.uppercase()) else 0f
        val dateW = if (fullDateText.isNotBlank()) textPaint.measureText(fullDateText) else 0f
        val maxW = maxOf(nameW, dateW)
        if (maxW > maxAvailableWidth && maxW > 0f) {
            fontSize *= (maxAvailableWidth / maxW)
            textPaint.textSize = fontSize
        }
        
        val centerX = width / 2f
        if (name.isNotBlank() && fullDateText.isNotBlank()) {
            canvas.drawText(name.uppercase(), centerX, height - barHeight + fontSize * 1.05f, textPaint)
            canvas.drawText(fullDateText, centerX, height - barHeight + fontSize * 2.25f, textPaint)
        } else if (name.isNotBlank()) {
            canvas.drawText(name.uppercase(), centerX, height - barHeight / 2f + fontSize * 0.35f, textPaint)
        } else if (fullDateText.isNotBlank()) {
            canvas.drawText(fullDateText, centerX, height - barHeight / 2f + fontSize * 0.35f, textPaint)
        }
    }
    
    // Simple, clean 4-corner rectangular outer border around entire passport photo (black)
    if (isBorderOn) {
        val strokePx = max(4f, width * 0.016f)
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            isAntiAlias = true
        }
        val halfStroke = strokePx / 2f
        
        // 4-corner rectangular outer border
        canvas.drawRect(halfStroke, halfStroke, width - halfStroke, height - halfStroke, borderPaint)
        
        // Divider line between photo and bottom white bar
        if (hasText) {
            val dividerY = height - barHeight.toFloat()
            canvas.drawLine(0f, dividerY, width.toFloat(), dividerY, borderPaint)
        }
    }
    
    return dest
}

private suspend fun savePassportPhoto(
    context: Context, 
    bitmap: Bitmap, 
    candidateName: String,
    viewModel: PdfMasterViewModel
) = withContext(Dispatchers.IO) {
    val time = System.currentTimeMillis()
    val cleanName = candidateName.replace("\\s+".toRegex(), "_").ifBlank { "Candidate" }
    val fileName = "Passport_${cleanName}_$time.jpg"
    
    try {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { 
            bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it)
        }
        val result = GlobalSaveManager.saveProcessedFile(
            context, file, fileName, "image/jpeg", "passport", "Passport Photo with Name & Date"
        )
        viewModel.setNotification("Saved to ${result.displayLocation}/${result.fileName}")
    } catch (e: Exception) {
        viewModel.setNotification("Save failed: ${e.message}")
    }
}

private suspend fun save8PhotoSheet(
    context: Context,
    singlePhoto: Bitmap,
    candidateName: String,
    viewModel: PdfMasterViewModel
) = withContext(Dispatchers.IO) {
    try {
        // Standard 4×6 inch sheet @ 300 DPI = 1200 × 1800 px (Portrait: 1200 wide, 1800 high)
        val sheetWidth = 1200
        val sheetHeight = 1800
        val sheetBitmap = Bitmap.createBitmap(sheetWidth, sheetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)
        
        // Clean white photo paper
        canvas.drawColor(Color.WHITE)
        
        // 8 photos layout: 4 rows × 2 columns
        val cols = 2
        val rows = 4
        
        val marginX = 80f
        val marginY = 80f
        val gapX = 40f
        val gapY = 40f
        
        val photoW = ((sheetWidth - (2 * marginX) - (gapX * (cols - 1))) / cols).toInt()
        val photoH = ((sheetHeight - (2 * marginY) - (gapY * (rows - 1))) / rows).toInt()
        
        val scaledPhoto = Bitmap.createScaledBitmap(singlePhoto, photoW, photoH, true)
        
        val cutLinePaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val posX = marginX + c * (photoW + gapX)
                val posY = marginY + r * (photoH + gapY)
                canvas.drawBitmap(scaledPhoto, posX, posY, null)
                
                // Draw fine cut guide around photo
                canvas.drawRect(posX, posY, posX + photoW, posY + photoH, cutLinePaint)
            }
        }
        
        val time = System.currentTimeMillis()
        val cleanName = candidateName.replace("\\s+".toRegex(), "_").ifBlank { "Candidate" }
        val fileName = "PassportSheet_4x6_${cleanName}_$time.jpg"
        
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { 
            sheetBitmap.compress(Bitmap.CompressFormat.JPEG, 96, it)
        }
        val result = GlobalSaveManager.saveProcessedFile(
            context, file, fileName, "image/jpeg", "passport", "8-Photo Print Sheet (4×6)"
        )
        viewModel.setNotification("Saved 8-Photo Sheet to ${result.displayLocation}/${result.fileName}")
    } catch (e: Exception) {
        viewModel.setNotification("Sheet creation failed: ${e.message}")
    }
}

private fun sharePassportPhoto(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "shared_passport")
        cachePath.mkdirs()
        val file = File(cachePath, "Passport_NameDate_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Passport Photo"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
