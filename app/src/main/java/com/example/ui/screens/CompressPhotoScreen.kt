package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.model.PdfFileRecord
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import com.example.ui.AppScreen

/**
 * Data model for selected images in the Compress Photo workflow.
 * Uses lightweight thumbnails and memory-efficient decoding to prevent OutOfMemoryError.
 */
data class SelectedImageItem(
    val uri: Uri,
    val name: String,
    val originalSizeBytes: Long,
    val width: Int,
    val height: Int,
    val thumbnailBitmap: Bitmap?,
    val isProcessing: Boolean = false,
    val compressedFile: File? = null,
    val resultSizeBytes: Long? = null,
    val resultDimensions: String? = null,
    val resultQuality: Int? = null,
    val resultBitmap: Bitmap? = null,
    val isSavedToGallery: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressPhotoScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedImages by remember { mutableStateOf<List<SelectedImageItem>>(emptyList()) }
    var globalTargetSize by remember { mutableStateOf("50") }
    var globalUnit by remember { mutableStateOf("KB") }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }

    var showSavedDialog by remember { mutableStateOf(false) }
    var savedUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var previewDialogItem by remember { mutableStateOf<SelectedImageItem?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Real Android Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val newItems = uris.mapNotNull { uri ->
                    loadImageItemData(context, uri)
                }
                if (newItems.isNotEmpty()) {
                    selectedImages = selectedImages + newItems
                    validationError = null
                }
            }
        }
    }

    val handleBackPress: () -> Unit = {
        when {
            previewDialogItem != null -> previewDialogItem = null
            showSavedDialog -> showSavedDialog = false
            else -> onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackPress()
    }

    // Safely executes the REAL compression calculation on IO thread
    fun performUpdateCompression() {
        focusManager.clearFocus()
        val (targetBytes, err) = validateTargetSize(globalTargetSize, globalUnit)
        if (err != null || targetBytes == null) {
            validationError = err ?: "Please enter a valid target size."
            Toast.makeText(context, validationError ?: "Invalid size", Toast.LENGTH_SHORT).show()
            return
        }
        validationError = null

        if (selectedImages.isEmpty()) {
            Toast.makeText(context, "Please select at least one photo first.", Toast.LENGTH_SHORT).show()
            return
        }

        isUpdating = true
        coroutineScope.launch {
            try {
                val updatedList = withContext(Dispatchers.IO) {
                    processAllImagesCompression(
                        context = context,
                        items = selectedImages,
                        targetBytes = targetBytes,
                        targetSizeStr = globalTargetSize,
                        unit = globalUnit
                    )
                }
                selectedImages = updatedList

                // Persist successful compressions to Recent Files
                updatedList.forEach { item ->
                    item.compressedFile?.let { file ->
                        viewModel.addRecentFile(
                            PdfFileRecord(
                                name = file.name,
                                filePath = file.absolutePath,
                                fileSize = file.length(),
                                pageCount = 1,
                                category = "photo",
                                tag = "Compressed"
                            )
                        )
                    }
                }
                Toast.makeText(context, "Compression settings updated for $globalTargetSize $globalUnit", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                t.printStackTrace()
                Toast.makeText(context, "Compression error: ${t.message ?: "Could not process image"}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
    }

AppScreen(isScrollable = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("compress_photo_screen")
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Compress Photo",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Custom Target Size (KB / MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = handleBackPress,
                    modifier = Modifier.testTag("compress_photo_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Scrollable Body (weight 1f)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("compress_photo_scrollable_body"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Action row: "+ Add More Images" and "Clear All"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("add_more_photos_btn"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedImages.isEmpty()) "+ Select Image" else "+ Add More Images",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            softWrap = false
                        )
                    }

                    if (selectedImages.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedImages = emptyList() },
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("clear_all_photos_btn"),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear All",
                                maxLines = 1,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Section header: "Selected Images (N)"
            item {
                Text(
                    text = if (selectedImages.isEmpty()) "Selected Images (0)" else "Selected Images (${selectedImages.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (selectedImages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ToolBlue.copy(alpha = 0.06f)),
                        border = BorderStroke(1.dp, ToolBlue.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(ToolBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Select Photo",
                                    tint = ToolBlue,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Photos Selected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap '+ Select Image' above to choose photos from your device gallery. You can set your desired target size below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(selectedImages) { item ->
                    CleanImageCard(
                        item = item,
                        targetSizeFormatted = "$globalTargetSize $globalUnit",
                        onRemove = {
                            selectedImages = selectedImages.filter { it.uri != item.uri }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Sticky Target Size Section (ALWAYS VISIBLE ABOVE BOTTOM NAVIGATION)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("target_size_sticky_section"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Target Size",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Input row: [ 50 ] [ KB ▼ ] [ Update ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = globalTargetSize,
                        onValueChange = {
                            globalTargetSize = it.filter { ch -> ch.isDigit() || ch == '.' }
                            validationError = null
                        },
                        placeholder = { Text("50") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("target_size_input"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (selectedImages.isEmpty()) {
                                    Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                                } else {
                                    performUpdateCompression()
                                }
                            }
                        ),
                        isError = validationError != null
                    )

                    // Unit Selector: [ KB ▼ ]
                    Box {
                        OutlinedButton(
                            onClick = { unitDropdownExpanded = true },
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("target_size_unit_btn"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(globalUnit, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select unit")
                        }
                        DropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("KB", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    globalUnit = "KB"
                                    unitDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("MB", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    globalUnit = "MB"
                                    unitDropdownExpanded = false
                                }
                            )
                        }
                    }

                    // Update Button
                    Button(
                        onClick = {
                            if (selectedImages.isEmpty()) {
                                Toast.makeText(context, "Please select at least one photo first", Toast.LENGTH_SHORT).show()
                            } else {
                                performUpdateCompression()
                            }
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("target_size_update_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isUpdating,
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Updating...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Update",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Post-compression actual results area
                val compressedItems = selectedImages.filter { it.compressedFile != null && it.resultSizeBytes != null }
                if (compressedItems.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ToolGreen.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (selectedImages.size == 1) {
                                val single = selectedImages.first()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Original Size: ${formatFileSize(single.originalSizeBytes)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Target Size: $globalTargetSize $globalUnit",
                                        fontSize = 12.sp,
                                        color = ToolBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Actual Result: ${formatFileSize(single.resultSizeBytes ?: 0L)}",
                                        fontSize = 13.sp,
                                        color = ToolGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (single.resultQuality != null && single.resultDimensions != null) {
                                        Text(
                                            text = "Quality: ${single.resultQuality}% • ${single.resultDimensions}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                val totalOrig = selectedImages.sumOf { it.originalSizeBytes }
                                val totalRes = compressedItems.sumOf { it.resultSizeBytes ?: 0L }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Original: ${formatFileSize(totalOrig)}", fontSize = 12.sp)
                                    Text("Target: $globalTargetSize $globalUnit", fontSize = 12.sp, color = ToolBlue)
                                    Text("Actual: ${formatFileSize(totalRes)}", fontSize = 12.sp, color = ToolGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Action Buttons: [ Preview Result ] [ Save to Gallery ] [ Share ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                previewDialogItem = compressedItems.firstOrNull()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("preview_result_btn"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Preview Result",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                if (isSaving) return@Button
                                isSaving = true
                                coroutineScope.launch {
                                    try {
                                        var saved = 0
                                        val uris = mutableListOf<android.net.Uri>()
                                        val currentImages = selectedImages.toList()
                                        val newImages = currentImages.map { item ->
                                            val file = item.compressedFile
                                            if (file != null && !item.isSavedToGallery) {
                                                val uri = saveImageToGlobalStorage(context, file)
                                                if (uri != null) {
                                                    uris.add(uri)
                                                    saved++
                                                    item.copy(isSavedToGallery = true)
                                                } else item
                                            } else item
                                        }
                                        selectedImages = newImages
                                        if (saved > 0) {
                                            savedUris = uris
                                            showSavedDialog = true
                                        } else {
                                            Toast.makeText(context, "Failed to save to gallery or already saved", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("save_to_gallery_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedImages.size == 1) "Save to Gallery" else "Save All (${compressedItems.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        IconButton(
                            onClick = {
                                val file = compressedItems.firstOrNull()?.compressedFile
                                if (file != null) {
                                    shareImageFile(context, file)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(ToolBlue.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .testTag("share_result_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = ToolBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
    }

    // Fullscreen Preview Result Dialog
    previewDialogItem?.let { previewItem ->
        Dialog(
            onDismissRequest = { previewDialogItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler(enabled = true) { previewDialogItem = null }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = previewItem.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Result: ${formatFileSize(previewItem.resultSizeBytes ?: 0L)} • ${previewItem.resultDimensions ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { previewDialogItem = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    previewItem.compressedFile?.let { shareImageFile(context, it) }
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )

                    var zoomScale by remember { mutableFloatStateOf(1f) }
                    var panOffset by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black)
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
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val displayBmp = previewItem.resultBitmap ?: previewItem.thumbnailBitmap
                        if (displayBmp != null) {
                            Image(
                                bitmap = displayBmp.asImageBitmap(),
                                contentDescription = "Preview Compressed Photo",
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                },
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("No preview available", color = Color.White)
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isSaving || previewItem.isSavedToGallery) return@Button
                                    val file = previewItem.compressedFile
                                    if (file != null) {
                                        isSaving = true
                                        coroutineScope.launch {
                                            try {
                                                val uri = saveImageToGlobalStorage(context, file)
                                                if (uri != null) {
                                                    selectedImages = selectedImages.map {
                                                        if (it.uri == previewItem.uri) it.copy(isSavedToGallery = true) else it
                                                    }
                                                    Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
                                                }
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isSaving && !previewItem.isSavedToGallery,
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Gallery", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }

                            OutlinedButton(
                                onClick = { previewDialogItem = null },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Close", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean image card showing:
 * [Thumbnail] Filename
 *             Original: 365.7 KB
 *             1280 × 2772
 *             Est. Result / Actual Result: XX KB
 *             Quality: XX%
 *             ✕ (remove button)
 */
@Composable
fun CleanImageCard(
    item: SelectedImageItem,
    targetSizeFormatted: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_image_card_${item.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            if (item.thumbnailBitmap != null) {
                Image(
                    bitmap = item.thumbnailBitmap.asImageBitmap(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Original: ${formatFileSize(item.originalSizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.width > 0 && item.height > 0) {
                    Text(
                        text = "${item.width} × ${item.height}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.resultSizeBytes != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Actual Result: ${formatFileSize(item.resultSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = CrimsonPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.resultQuality != null) {
                        Text(
                            text = "Quality: ${item.resultQuality}% • ${item.resultDimensions ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Est. Target: $targetSizeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = ToolBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Remove Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("remove_image_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// INPUT VALIDATION & MEMORY-SAFE CALCULATION ENGINE
// -------------------------------------------------------------

/**
 * Validates target size input safely against:
 * - Empty string
 * - Non-numeric characters
 * - Zero or negative value
 * - Extreme values (< 5 KB or > 100 MB)
 * - Decimal/Integer conversion
 */
fun validateTargetSize(input: String, unit: String): Pair<Long?, String?> {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) {
        return Pair(null, "Please enter a valid target size.")
    }
    val value = trimmed.toDoubleOrNull()
    if (value == null) {
        return Pair(null, "Please enter a valid target size.")
    }
    if (value <= 0.0) {
        return Pair(null, "Target size must be greater than 0.")
    }
    val bytes = when (unit.uppercase(Locale.ROOT)) {
        "KB" -> (value * 1024.0).toLong()
        "MB" -> (value * 1024.0 * 1024.0).toLong()
        else -> (value * 1024.0).toLong()
    }
    if (bytes < 1024L * 5L) {
        return Pair(null, "Target size too small (minimum 5 KB).")
    }
    if (bytes > 1024L * 1024L * 100L) {
        return Pair(null, "Target size too large (maximum 100 MB).")
    }
    return Pair(bytes, null)
}

/**
 * Loads image metadata and decodes ONLY a lightweight thumbnail into memory.
 * Never loads the full-resolution original image into memory to avoid OutOfMemoryError.
 */
suspend fun loadImageItemData(context: Context, uri: Uri): SelectedImageItem? = withContext(Dispatchers.IO) {
    try {
        var name = "IMG_${System.currentTimeMillis()}.jpg"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val n = cursor.getString(nameIndex)
                    if (!n.isNullOrBlank()) name = n
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }

        // 1. Decode bounds only (Zero pixel allocation in memory)
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }
        val origW = boundsOptions.outWidth
        val origH = boundsOptions.outHeight
        if (origW <= 0 || origH <= 0) return@withContext null

        // 2. Decode a small thumbnail (max 200x200)
        val targetThumbSize = 200
        var thumbSample = 1
        while (origW / thumbSample > targetThumbSize * 2 || origH / thumbSample > targetThumbSize * 2) {
            thumbSample *= 2
        }

        val thumbOptions = BitmapFactory.Options().apply {
            inSampleSize = thumbSample
            inPreferredConfig = Bitmap.Config.RGB_565 // Low memory footprint
        }
        val thumbBitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, thumbOptions)
        }

        val calculatedSize = if (size > 0) size else {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
        }

        SelectedImageItem(
            uri = uri,
            name = name,
            originalSizeBytes = calculatedSize,
            width = origW,
            height = origH,
            thumbnailBitmap = thumbBitmap
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Sequential, memory-safe compression processor for all selected images.
 * Processes images one-by-one so large images never cause memory spikes or OOM.
 */
suspend fun processAllImagesCompression(
    context: Context,
    items: List<SelectedImageItem>,
    targetBytes: Long,
    targetSizeStr: String,
    unit: String
): List<SelectedImageItem> = withContext(Dispatchers.IO) {
    val results = mutableListOf<SelectedImageItem>()
    for (item in items) {
        try {
            val processed = compressSingleImage(context, item, targetBytes)
            results.add(processed)
        } catch (t: Throwable) {
            t.printStackTrace()
            results.add(item.copy(error = t.message))
        }
    }
    results
}

/**
 * Real adaptive compression algorithm:
 * 1. Calculates safe inSampleSize based on targetBytes to prevent decoding full huge bitmap.
 * 2. Tunes JPEG quality via binary search (10 to 95).
 * 3. If target size is very small, scales resolution down iteratively until target size is reached.
 * 4. Measures actual output file length and returns real file size, quality, and dimensions.
 */
fun compressSingleImage(
    context: Context,
    item: SelectedImageItem,
    targetBytes: Long
): SelectedImageItem {
    val dir = File(context.filesDir, "compressed_photos")
    if (!dir.exists()) dir.mkdirs()

    // Query bounds first
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(item.uri)?.use {
        BitmapFactory.decodeStream(it, null, boundsOptions)
    }
    val origW = boundsOptions.outWidth
    val origH = boundsOptions.outHeight
    if (origW <= 0 || origH <= 0) {
        return item.copy(error = "Invalid dimensions")
    }

    // Calculate inSampleSize based on pixel budget to avoid OOM
    // For JPEG, 1 pixel is ~ 0.1 to 0.2 bytes
    val pixelBudget = (targetBytes * 8L).coerceIn(40000L, 8000000L)
    var sampleSize = 1
    while ((origW / sampleSize) * (origH / sampleSize) > pixelBudget * 2 && sampleSize < 16) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    val baseBitmap = context.contentResolver.openInputStream(item.uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: return item.copy(error = "Could not decode image")

    var currentBitmap = baseBitmap
    var bestBytes: ByteArray? = null
    var bestQuality = 80
    var bestDiff = Long.MAX_VALUE
    var finalBitmap = baseBitmap

    try {
        // Step 1: Binary search on JPEG quality with the decoded bitmap
        var lowQ = 10
        var highQ = 95
        var currentBestBytes: ByteArray? = null
        var currentBestQuality = 80

        for (i in 0..5) {
            val midQ = (lowQ + highQ) / 2
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, midQ, stream)
            val size = stream.size().toLong()

            if (size <= targetBytes) {
                val diff = targetBytes - size
                if (diff < bestDiff) {
                    bestDiff = diff
                    currentBestBytes = stream.toByteArray()
                    currentBestQuality = midQ
                }
                lowQ = midQ + 1
            } else {
                highQ = midQ - 1
            }
        }

        if (currentBestBytes != null) {
            bestBytes = currentBestBytes
            bestQuality = currentBestQuality
        } else {
            // Step 2: If even low quality is above targetBytes, downscale resolution
            var scale = 0.8f
            while (scale >= 0.1f && bestBytes == null) {
                val newW = (currentBitmap.width * scale).toInt().coerceAtLeast(40)
                val newH = (currentBitmap.height * scale).toInt().coerceAtLeast(40)
                val scaled = Bitmap.createScaledBitmap(currentBitmap, newW, newH, true)

                var sLow = 15
                var sHigh = 90
                for (j in 0..4) {
                    val sMid = (sLow + sHigh) / 2
                    val sStream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, sMid, sStream)
                    val sSize = sStream.size().toLong()

                    if (sSize <= targetBytes) {
                        bestBytes = sStream.toByteArray()
                        bestQuality = sMid
                        if (currentBitmap != baseBitmap) currentBitmap.recycle()
                        currentBitmap = scaled
                        break
                    } else {
                        sHigh = sMid - 1
                    }
                }

                if (bestBytes == null && scaled != currentBitmap && scaled != baseBitmap) {
                    scaled.recycle()
                }
                scale -= 0.15f
            }
        }

        // Fallback: minimal quality if still not within budget
        if (bestBytes == null) {
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 20, stream)
            bestBytes = stream.toByteArray()
            bestQuality = 20
        }

        finalBitmap = currentBitmap

        // Output file naming: original_name_compressed.jpg or _compressed_(1).jpg
        val rawBase = item.name.substringBeforeLast(".")
        val safeBase = if (rawBase.isNotBlank()) rawBase else "IMG_${System.currentTimeMillis()}"
        var outFile = File(dir, "${safeBase}_compressed.jpg")
        var counter = 1
        while (outFile.exists()) {
            outFile = File(dir, "${safeBase}_compressed_($counter).jpg")
            counter++
        }

        FileOutputStream(outFile).use { fos ->
            fos.write(bestBytes)
            fos.flush()
        }

        val previewBmp = BitmapFactory.decodeFile(outFile.absolutePath)

        return item.copy(
            compressedFile = outFile,
            resultSizeBytes = outFile.length(),
            resultDimensions = "${finalBitmap.width} × ${finalBitmap.height}",
            resultQuality = bestQuality,
            resultBitmap = previewBmp ?: item.thumbnailBitmap,
            isSavedToGallery = false,
            error = null
        )
    } finally {
        if (currentBitmap != baseBitmap && currentBitmap != finalBitmap) {
            currentBitmap.recycle()
        }
        if (baseBitmap != finalBitmap) {
            baseBitmap.recycle()
        }
    }
}

// -------------------------------------------------------------
// SAVE VIA GLOBAL SAVE MANAGER & GALLERY VIA MEDIASTORE
// -------------------------------------------------------------

suspend fun saveImageToGlobalStorage(context: Context, imageFile: File): Uri? {
    return saveImageToGallery(context, imageFile)
}

fun saveImageToGallery(context: Context, imageFile: File): Uri? {
    try {
        val resolver = context.contentResolver
        val baseName = imageFile.name.substringBeforeLast(".")
        val extension = "jpg"

        var displayName = "${baseName}.jpg"
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
            displayName = "${baseName}_($counter).$extension"
            counter++
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PDFMaster_Compressed")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "PDFMaster_Compressed")
                if (!appDir.exists()) appDir.mkdirs()
                val destFile = File(appDir, displayName)
                put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues) ?: return null

        resolver.openOutputStream(uri)?.use { outStream ->
            FileInputStream(imageFile).use { inStream ->
                inStream.copyTo(outStream)
            }
            outStream.flush()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        MediaScannerConnection.scanFile(context, arrayOf(imageFile.absolutePath), arrayOf("image/jpeg"), null)

        return uri
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun shareImageFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Compressed Photo"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot share file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}
