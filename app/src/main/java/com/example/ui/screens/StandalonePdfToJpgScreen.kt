package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.model.PdfFileRecord
import com.example.pdf.JpgPageItem
import com.example.pdf.PdfToJpgConversionResult
import com.example.pdf.PdfToJpgConverter
import com.example.storage.GlobalSaveManager
import com.example.ui.PdfMasterViewModel
import com.example.ui.dialogs.PdfInputSelectionScreen
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StandalonePdfToJpgScreen(
    initialFile: PdfFileRecord?,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedRecord by remember(initialFile) { mutableStateOf(initialFile) }

    // SAF Picker for selecting/changing PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPdfFromUri(uri) { record ->
                selectedRecord = record
            }
        }
    }

    // Auto-launch picker if opened without a preselected file
    LaunchedEffect(Unit) {
        if (selectedRecord == null) {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }
    }

    if (selectedRecord == null) {
        PdfInputSelectionScreen(
            toolTitle = "PDF to JPG",
            toolSubtitle = "Convert PDF pages to high-quality JPG images",
            toolIcon = Icons.Default.Image,
            badgeColor = ToolOrange,
            allowMultiple = false,
            viewModel = viewModel,
            onBack = onBack,
            onSingleFileSelected = { file ->
                selectedRecord = file
            }
        )
        return
    }

    val record = selectedRecord!!
    val originalPdfFile = remember(record) { File(record.filePath) }
    // Exact byte size of the original PDF
    val originalExactBytes = remember(record, originalPdfFile) {
        if (originalPdfFile.exists()) originalPdfFile.length() else record.fileSize
    }

    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableFloatStateOf(0f) }
    var conversionStatusMessage by remember { mutableStateOf("") }
    var conversionError by remember { mutableStateOf<String?>(null) }
    var conversionResult by remember { mutableStateOf<PdfToJpgConversionResult?>(null) }

    // State guards to prevent duplicate saves & callbacks
    var isSavingAll by remember { mutableStateOf(false) }
    var isSavedAll by remember { mutableStateOf(false) }
    var allSavedLocation by remember { mutableStateOf<String?>(null) }
    var lastSaveClickTime by remember { mutableLongStateOf(0L) }
    var previewImageItem by remember { mutableStateOf<JpgPageItem?>(null) }

    val startConversion: () -> Unit = {
        if (!isConverting && originalPdfFile.exists()) {
            isConverting = true
            conversionError = null
            conversionProgress = 0f
            conversionStatusMessage = "Starting high-fidelity conversion..."

            coroutineScope.launch {
                try {
                    val result = PdfToJpgConverter.convertPdfToJpg(
                        context = context,
                        pdfFile = originalPdfFile,
                        originalSizeBytes = originalExactBytes,
                        onProgress = { current, total, pass, msg ->
                            if (total > 0) {
                                conversionProgress = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            }
                            conversionStatusMessage = msg
                        }
                    )
                    conversionResult = result
                    isConverting = false
                } catch (e: Exception) {
                    conversionError = e.message ?: "Failed to convert PDF to JPG"
                    isConverting = false
                }
            }
        }
    }

    // Auto-trigger conversion if not yet converted
    LaunchedEffect(record) {
        if (conversionResult == null && !isConverting) {
            startConversion()
        }
    }

    // Single unified save action with debounce and atomic guard
    val performSaveAll: () -> Unit = {
        val currentResult = conversionResult
        if (currentResult != null) {
            val now = System.currentTimeMillis()
            if (isSavingAll) {
                // Ignore while in progress
            } else if (isSavedAll) {
                Toast.makeText(
                    context,
                    "Already saved to ${allSavedLocation ?: "storage"}",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (now - lastSaveClickTime >= 1000L) {
                lastSaveClickTime = now
                isSavingAll = true
                coroutineScope.launch {
                    try {
                        val location = saveAllJpgs(
                            context = context,
                            viewModel = viewModel,
                            pages = currentResult.pages
                        )
                        isSavedAll = true
                        allSavedLocation = location
                        Toast.makeText(
                            context,
                            if (currentResult.pages.size == 1) "Saved JPG to $location"
                            else "Saved ${currentResult.pages.size} JPGs to $location",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isSavingAll = false
                    }
                }
            }
        }
    }

    BackHandler(enabled = true) {
        if (previewImageItem != null) {
            previewImageItem = null
        } else {
            onBack()
        }
    }

    // Root full-screen surface with native insets handling
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_standalone_pdf_to_jpg"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("pdf_to_jpg_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PDF to JPG",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Export pages to high-quality JPG",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("pdf_to_jpg_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Entire Screen is fully vertically scrollable from top to bottom
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("pdf_to_jpg_scrollable_list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Document Overview Card
                item(key = "document_overview_card") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ToolOrange.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = CrimsonPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = record.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${record.pageCount} page(s) • Original Document",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("change_pdf_button")
                                ) {
                                    Text("Change", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Size & Byte details
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Original PDF Size (Exact Bytes):",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${formatNumber(originalExactBytes)} bytes (${formatBytesReadable(originalExactBytes)})",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CrimsonPrimary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Base Target",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = CrimsonPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Conversion Status or Progress
                if (isConverting) {
                    item(key = "converting_status_card") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, ToolOrange.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp,
                                        color = ToolOrange
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Converting PDF to JPG...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = conversionStatusMessage,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                LinearProgressIndicator(
                                    progress = { conversionProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ToolOrange,
                                    trackColor = ToolOrange.copy(alpha = 0.2f)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Verifying high resolution so JPG byte size is >= original PDF size...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Error Card
                if (conversionError != null) {
                    item(key = "conversion_error_card") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Conversion Issue",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = conversionError ?: "Unknown error occurred",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = startConversion,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("retry_conversion_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Conversion")
                                }
                            }
                        }
                    }
                }

                // Conversion Results
                conversionResult?.let { result ->
                    // 1. Verification Banner
                    item(key = "verification_banner_card") {
                        val isRequirementSatisfied = result.totalJpgBytes >= result.originalPdfBytes

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRequirementSatisfied) ToolGreen.copy(alpha = 0.10f)
                                else ToolOrange.copy(alpha = 0.10f)
                            ),
                            border = BorderStroke(
                                1.2.dp,
                                if (isRequirementSatisfied) ToolGreen.copy(alpha = 0.4f)
                                else ToolOrange.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isRequirementSatisfied) Icons.Default.CheckCircle else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isRequirementSatisfied) ToolGreen else ToolOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isRequirementSatisfied) "Byte Size Verified: JPG >= Original PDF" else "Conversion Completed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isRequirementSatisfied) ToolGreen else ToolOrange
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "The rendered JPG output meets the strict file size mandate. Page content, aspect ratio, text, and visual appearance have been preserved with pristine high resolution.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Side-by-side comparison metrics
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Original PDF Box
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "ORIGINAL PDF",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CrimsonPrimary,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${formatNumber(result.originalPdfBytes)} B",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = formatBytesReadable(result.originalPdfBytes),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Converted JPG Box
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, ToolGreen.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "FINAL JPG OUTPUT",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ToolGreen,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${formatNumber(result.totalJpgBytes)} B",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = ToolGreen
                                            )
                                            Text(
                                                text = formatBytesReadable(result.totalJpgBytes),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Saved Location Banner (Clear confirmation when saved)
                    if (isSavedAll || allSavedLocation != null) {
                        item(key = "saved_location_confirmation_banner") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = ToolGreen.copy(alpha = 0.12f)),
                                border = BorderStroke(1.2.dp, ToolGreen.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(ToolGreen.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = ToolGreen,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "JPG Pages Saved Successfully",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = ToolGreen
                                            )
                                            Text(
                                                text = "Single file per page saved to storage and registered in Recent Files",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Folder, contentDescription = null, tint = ToolGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = allSavedLocation ?: "Documents / PDF_Master",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Primary Action Buttons Card (Save All / Share All)
                    item(key = "primary_action_buttons_card") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Save All Button with full debounce & state protection
                                Button(
                                    onClick = performSaveAll,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("save_all_jpg_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSavedAll) ToolGreen else ToolOrange,
                                        disabledContainerColor = if (isSavedAll) ToolGreen.copy(alpha = 0.7f) else ToolOrange.copy(alpha = 0.5f)
                                    ),
                                    enabled = !isSavingAll && !isSavedAll
                                ) {
                                    if (isSavingAll) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Saving...", fontWeight = FontWeight.Bold)
                                    } else if (isSavedAll) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Saved", fontWeight = FontWeight.Bold, color = Color.White)
                                    } else {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (result.pages.size == 1) "Save JPG" else "Save All JPGs (${result.pages.size})",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Share All Button
                                OutlinedButton(
                                    onClick = {
                                        shareMultipleJpgs(context, result.pages.map { it.file })
                                    },
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("share_all_jpg_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share All", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // 4. Converted Pages Section Header
                    item(key = "converted_pages_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONVERTED JPG PAGES (${result.pages.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (result.pages.size == 1) "1 JPG File" else "${result.pages.size} JPG Files",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 5. Individual Page Cards
                    itemsIndexed(
                        items = result.pages,
                        key = { index, page -> "page_${index}_${page.fileName}" }
                    ) { index, pageItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail with clickable preview
                                Box(
                                    modifier = Modifier
                                        .size(72.dp, 96.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { previewImageItem = pageItem },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = pageItem.file,
                                        contentDescription = "Page ${index + 1} Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    // Quick view icon overlay
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.BottomEnd)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(topStart = 6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "View",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pageItem.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "Page ${index + 1} of ${result.pages.size} • ${pageItem.width} × ${pageItem.height} px",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${formatNumber(pageItem.byteSize)} bytes (${formatBytesReadable(pageItem.byteSize)})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = ToolGreen
                                    )

                                    if (pageItem.isSaved || isSavedAll) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ToolGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Saved to storage",
                                                fontSize = 11.sp,
                                                color = ToolGreen,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Single page share action
                                Column(horizontalAlignment = Alignment.End) {
                                    IconButton(
                                        onClick = {
                                            shareSingleJpg(context, pageItem.file)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Secondary Bottom Action Card for multi-page documents (so user scrolling to end has actions right there)
                    if (result.pages.size > 1) {
                        item(key = "bottom_action_buttons_card") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = performSaveAll,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("save_all_jpg_button_bottom"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSavedAll) ToolGreen else ToolOrange,
                                            disabledContainerColor = if (isSavedAll) ToolGreen.copy(alpha = 0.7f) else ToolOrange.copy(alpha = 0.5f)
                                        ),
                                        enabled = !isSavingAll && !isSavedAll
                                    ) {
                                        if (isSavingAll) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Saving...", fontWeight = FontWeight.Bold)
                                        } else if (isSavedAll) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Saved", fontWeight = FontWeight.Bold, color = Color.White)
                                        } else {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Save All JPGs (${result.pages.size})",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            shareMultipleJpgs(context, result.pages.map { it.file })
                                        },
                                        modifier = Modifier
                                            .height(50.dp)
                                            .testTag("share_all_jpg_button_bottom"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share All", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                } ?: run {
                    // If conversion not started or in progress
                    if (!isConverting && conversionError == null) {
                        item(key = "convert_cta_card") {
                            Button(
                                onClick = startConversion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp)
                                    .height(50.dp)
                                    .testTag("convert_to_jpg_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                                enabled = !isConverting
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Convert to JPG",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // Generous bottom spacer with navigationBarsPadding to guarantee NO clipping behind navigation area
                item(key = "bottom_safe_navigation_spacer") {
                    Spacer(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(32.dp)
                    )
                }
            }
        }
    }

    // Fullscreen Image Preview Dialog (clean, non-scrollable gesture viewing area)
    previewImageItem?.let { item ->
        Dialog(
            onDismissRequest = { previewImageItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = item.file,
                    contentDescription = item.fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Header Overlay with details and close
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = item.fileName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${item.width} × ${item.height} px • ${formatNumber(item.byteSize)} bytes",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }

                        IconButton(onClick = { previewImageItem = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Saves all converted JPG pages to user storage exactly once via GlobalSaveManager.
 * Registers each page in Room database and triggers Android MediaScanner indexing.
 * Ensures only 1 JPG file per page is created on disk without duplicate clones.
 */
private suspend fun saveAllJpgs(
    context: Context,
    viewModel: PdfMasterViewModel,
    pages: List<JpgPageItem>
): String = withContext(Dispatchers.IO) {
    var lastSavedPath = "Documents / PDF_Master"
    for (page in pages) {
        // Unified single save via GlobalSaveManager - saves exactly once, scans with MediaScanner
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
        lastSavedPath = saveRes.displayLocation

        // Register in Room DB once
        viewModel.addRecentFile(saveRes.fileRecord)
    }
    lastSavedPath
}

private fun shareSingleJpg(context: Context, file: File) {
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
        context.startActivity(Intent.createChooser(intent, "Share JPG"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun shareMultipleJpgs(context: Context, files: List<File>) {
    try {
        val uris = ArrayList<Uri>()
        for (file in files) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            uris.add(uri)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share All JPGs"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatNumber(bytes: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(bytes)
}

private fun formatBytesReadable(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}
