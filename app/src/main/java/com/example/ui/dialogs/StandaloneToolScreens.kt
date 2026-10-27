package com.example.ui.dialogs

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.components.FullPageZoomDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.AppScreen
import androidx.compose.foundation.layout.PaddingValues
import coil.compose.AsyncImage
import com.example.model.PdfFileRecord
import com.example.pdf.PdfEngine
import com.example.ui.PdfMasterViewModel
import com.example.ui.ToolResultInfo
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolIndigo
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPink
import com.example.ui.theme.ToolPurple
import com.example.ui.theme.ToolTeal
import com.example.ui.components.ZoomablePdfPreviewBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.verticalScroll

/**
 * PDF INPUT SELECTION SCREEN
 * Upload PDF directly from Android phone storage via system document picker.
 */
@Composable
fun PdfInputSelectionScreen(
    toolTitle: String,
    toolSubtitle: String,
    toolIcon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color = CrimsonPrimary,
    allowMultiple: Boolean = false,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit,
    onSingleFileSelected: (PdfFileRecord) -> Unit = {},
    onMultipleFilesSelected: (List<PdfFileRecord>) -> Unit = {}
) {
    // SAF Upload Launchers
    val singlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPdfFromUri(uri) { record ->
                onSingleFileSelected(record)
            }
        }
    }

    val multiplePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultiplePdfsFromUris(uris) { records ->
                if (records.isNotEmpty()) {
                    onMultipleFilesSelected(records)
                }
            }
        }
    }

    fun launchFilePicker() {
        if (allowMultiple) {
            multiplePicker.launch(arrayOf("application/pdf"))
        } else {
            singlePicker.launch(arrayOf("application/pdf"))
        }
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreen(isScrollable = true, paddingValues = PaddingValues(24.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("tool_input_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to All PDF Tools",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = toolTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tool Header Badge
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.10f))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(badgeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(toolIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = toolTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = toolSubtitle,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section Label
                Text(
                    text = "SELECT PDF SOURCE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hero Upload PDF Card (Clean, centered, responsive)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { launchFilePicker() }
                        .testTag("option_upload_pdf"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = "Upload PDF",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (allowMultiple) "Upload PDF Files" else "Upload PDF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (allowMultiple)
                                "Select 2 or more PDF documents from phone storage"
                            else
                                "Select from phone storage (Downloads, Documents, Drive)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { launchFilePicker() },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(48.dp)
                                .testTag("btn_browse_storage"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (allowMultiple) "Browse & Select PDFs" else "Browse Phone Storage",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel / Return Button
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel & Return to All Tools", fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * UNIFIED RESULT PREVIEW DIALOG (STEP 8 & 9)
 * Shows rendered thumbnail of generated PDF, file size, pages, and "Save & View in Files"
 */
@Composable
fun ToolResultPreviewDialog(
    info: ToolResultInfo,
    onOpenFile: ((PdfFileRecord) -> Unit)? = null,
    onViewFolder: ((String, Uri?) -> Unit)? = null,
    onShareFile: ((PdfFileRecord, Uri?) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingThumb by remember { mutableStateOf(true) }

    LaunchedEffect(info.resultFile) {
        withContext(Dispatchers.IO) {
            val file = File(info.resultFile.filePath)
            val pages = PdfEngine.renderCrispPdfPages(file, scale = 1.5f, maxPages = 1)
            previewBitmap = pages.firstOrNull()
            isLoadingThumb = false
        }
    }

    Dialog(
        onDismissRequest = { /* Prevent accidental exit on compress success */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        AppScreen(isScrollable = true, paddingValues = PaddingValues(24.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Success Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ToolGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ToolGreen, modifier = Modifier.size(38.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${info.toolTitle} Completed!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "A new PDF file has been created successfully",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Real Preview Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingThumb) {
                            CircularProgressIndicator(color = CrimsonPrimary, modifier = Modifier.size(36.dp))
                        } else if (previewBitmap != null) {
                            AsyncImage(
                                model = previewBitmap,
                                contentDescription = "Generated PDF Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(64.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Metadata Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = info.resultFile.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Pages:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${info.resultFile.pageCount} pages", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "New File Size:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatFileSize(info.resultFile.fileSize), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Saved to:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = info.savedDisplayLocation.ifBlank { "Documents / PDF_Master" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (info.summaryText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ToolGreen.copy(alpha = 0.12f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = info.summaryText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ToolGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Actions: Open File & Locate / Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (onOpenFile != null) {
                                onOpenFile(info.resultFile)
                            } else {
                                com.example.storage.GlobalSaveManager.openFile(
                                    context = context,
                                    uri = info.savedUri,
                                    file = File(info.resultFile.filePath),
                                    mimeType = if (info.isPhotoOutput) "image/jpeg" else "application/pdf"
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_open_file"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Open File", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (onShareFile != null) {
                                onShareFile(info.resultFile, info.savedUri)
                            } else {
                                com.example.storage.GlobalSaveManager.shareFile(
                                    context = context,
                                    uri = info.savedUri,
                                    file = File(info.resultFile.filePath),
                                    mimeType = if (info.isPhotoOutput) "image/jpeg" else "application/pdf"
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_locate_share"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Locate / Share", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action: View Saved Folder
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        if (onViewFolder != null) {
                            onViewFolder(info.savedDisplayLocation, info.savedUri)
                        } else {
                            com.example.storage.GlobalSaveManager.openFolder(
                                context = context,
                                treeUriStr = info.savedUri?.toString(),
                                targetFile = File(info.resultFile.filePath)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_view_folder"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "View Saved Folder", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("btn_dismiss_result")
                ) {
                    Text("Done (Stay on Tools)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

data class MergePagePreview(
    val docIndex: Int,
    val pageIndex: Int,
    val fileRecord: PdfFileRecord,
    val bitmap: Bitmap? = null
)

/**
 * STANDALONE MERGE SCREEN (EXAMPLE 1)
 * Follows exact workflow:
 * - Shows ONLY Merge screen
 * - Selected PDF list with real thumbnails
 * - Reorder (move up/down) & delete
 * - Add more PDFs
 * - Merge Now -> result preview -> Save
 */
@Composable
fun StandaloneMergeScreen(
    initialFiles: List<PdfFileRecord> = emptyList(),
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    var filesToMerge by remember { mutableStateOf(initialFiles) }
    val context = LocalContext.current
    val thumbnailMap = remember { mutableStateMapOf<String, Bitmap?>() }
    var selectedPreviewPage by remember { mutableStateOf<MergePagePreview?>(null) }
    
    val allPages = remember(filesToMerge) {
        filesToMerge.flatMapIndexed { docIdx, file ->
            (0 until file.pageCount).map { pageIdx ->
                docIdx to pageIdx
            }
        }
    }

    LaunchedEffect(filesToMerge) {
        withContext(Dispatchers.IO) {
            filesToMerge.forEachIndexed { docIdx, file ->
                for (p in 0 until file.pageCount) {
                    val key = "${file.filePath}_$p"
                    if (!thumbnailMap.containsKey(key)) {
                        val bmp = PdfEngine.renderSinglePdfPage(
                            file = java.io.File(file.filePath),
                            pageIndex = p,
                            scale = 0.6f,
                            context = context,
                            uriString = file.uriString
                        )
                        thumbnailMap[key] = bmp
                    }
                }
            }
        }
    }

    var outputName by remember {
        mutableStateOf("Merged_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf")
    }
    var isPickingSource by remember { mutableStateOf(initialFiles.isEmpty()) }

    // Multi upload launcher
    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultiplePdfsFromUris(uris) { imported ->
                filesToMerge = filesToMerge + imported
            }
        }
    }

    if (isPickingSource) {
        PdfInputSelectionScreen(
            toolTitle = "Merge PDF",
            toolSubtitle = "Select 2 or more PDF documents to combine",
            toolIcon = Icons.Default.CallMerge,
            badgeColor = ToolIndigo,
            allowMultiple = true,
            viewModel = viewModel,
            onBack = onBack,
            onMultipleFilesSelected = { selected ->
                filesToMerge = selected
                isPickingSource = false
            }
        )
        return
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreen(isScrollable = false, paddingValues = PaddingValues(0.dp)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp, top = 8.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(text = "Merge PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DOCUMENTS TO MERGE (${filesToMerge.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        TextButton(
                            onClick = { uploadLauncher.launch(arrayOf("application/pdf")) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add PDF", fontSize = 12.sp)
                        }
                    }
                }

                // List of Files with Order Controls & Thumbnails
                itemsIndexed(filesToMerge) { index, file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Order Badge
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ToolIndigo.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = ToolIndigo,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${file.pageCount} pages • ${formatFileSize(file.fileSize)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Move Up
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val mutable = filesToMerge.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index - 1, item)
                                        filesToMerge = mutable
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                            }

                            // Move Down
                            IconButton(
                                onClick = {
                                    if (index < filesToMerge.size - 1) {
                                        val mutable = filesToMerge.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index + 1, item)
                                        filesToMerge = mutable
                                    }
                                },
                                enabled = index < filesToMerge.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                            }

                            // Delete
                            IconButton(
                                onClick = {
                                    val mutable = filesToMerge.toMutableList()
                                    mutable.removeAt(index)
                                    filesToMerge = mutable
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (allPages.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "SEQUENCE PREVIEW (${allPages.size} pages)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("merge_preview_reel"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(allPages) { (docIdx, pageIdx) ->
                                val file = filesToMerge[docIdx]
                                val key = "${file.filePath}_$pageIdx"
                                val bmp = thumbnailMap[key]

                                Card(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedPreviewPage = MergePagePreview(docIdx, pageIdx, file, bmp)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = ToolIndigo)
                                            }
                                        }

                                        Surface(
                                            color = ToolIndigo.copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(bottomEnd = 10.dp),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = "Doc ${docIdx + 1} • P${pageIdx + 1}",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Output File Name
                    OutlinedTextField(
                        value = outputName,
                        onValueChange = { outputName = it },
                        label = { Text("Output File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ToolIndigo
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Merge Button
                    Button(
                        onClick = {
                            val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                            viewModel.mergePdfs(filesToMerge, sanitized) {
                                // viewModel already calls commitAndShowResult
                            }
                        },
                        enabled = filesToMerge.size >= 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_merge_now"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ToolIndigo)
                    ) {
                        Icon(Icons.Default.CallMerge, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Merge ${filesToMerge.size} PDFs Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (selectedPreviewPage != null) {
        FullPageZoomDialog(
            bitmap = selectedPreviewPage!!.bitmap,
            title = "Preview: ${selectedPreviewPage!!.fileRecord.name}",
            subtitle = "Page ${selectedPreviewPage!!.pageIndex + 1} of ${selectedPreviewPage!!.fileRecord.pageCount}",
            onDismiss = { selectedPreviewPage = null }
        )
    }
}

/**
 * STANDALONE SPLIT SCREEN (EXAMPLE 2)
 * Follows exact workflow:
 * - Shows ONLY Split screen
 * - Real PDF previews of all pages
 * - Select page/ranges to extract
 * - Split Now -> result preview -> Save
 */
@Composable
fun StandaloneSplitScreen(
    initialFile: PdfFileRecord?,
    viewModel: PdfMasterViewModel,
    toolTitle: String = "Split PDF",
    onBack: () -> Unit
) {
    if (initialFile == null) {
        PdfInputSelectionScreen(
            toolTitle = toolTitle,
            toolSubtitle = if (toolTitle.contains("Extract", ignoreCase = true)) "Select one PDF to extract specific pages" else "Select one PDF to split into sections",
            toolIcon = if (toolTitle.contains("Extract", ignoreCase = true)) Icons.Default.Description else Icons.Default.CallSplit,
            badgeColor = ToolBlue,
            allowMultiple = false,
            viewModel = viewModel,
            onBack = onBack,
            onSingleFileSelected = { file ->
                viewModel.selectFile(file)
            }
        )
        return
    }

    var outputName by remember {
        mutableStateOf(if (toolTitle.contains("Extract", ignoreCase = true)) "${initialFile.nameWithoutExtension()}_extracted.pdf" else "${initialFile.nameWithoutExtension()}_split.pdf")
    }
    val selectedPages = remember { mutableStateListOf<Int>() }
    var pageThumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoadingThumbnails by remember { mutableStateOf(true) }
    var selectedPreviewPage by remember { mutableStateOf<Pair<Int, Bitmap?>?>(null) }

    LaunchedEffect(initialFile) {
        withContext(Dispatchers.IO) {
            val file = File(initialFile.filePath)
            val pages = PdfEngine.renderCrispPdfPages(file, scale = 1.0f, maxPages = 40)
            pageThumbnails = pages
            isLoadingThumbnails = false
        }
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreen(isScrollable = false, paddingValues = PaddingValues(0.dp)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp, top = 8.dp)
            ) {
                // Top Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (toolTitle.contains("Extract", ignoreCase = true)) Icons.Default.Description else Icons.Default.CallSplit,
                                contentDescription = null,
                                tint = ToolBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = toolTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // Source File Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ToolBlue.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ToolBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = initialFile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "${initialFile.pageCount} pages • Tap pages below to select", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Page selection actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPages.size} PAGES SELECTED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row {
                            TextButton(onClick = {
                                selectedPages.clear()
                                for (i in 0 until initialFile.pageCount) selectedPages.add(i)
                            }) {
                                Text("Select All", fontSize = 12.sp)
                            }
                            TextButton(onClick = { selectedPages.clear() }) {
                                Text("Clear", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Grid of Page Thumbnails (Using items chunked to simulate grid in LazyColumn)
                if (isLoadingThumbnails) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ToolBlue)
                        }
                    }
                } else {
                    val chunks = (0 until initialFile.pageCount).chunked(3)
                    items(chunks.size) { rowIndex ->
                        val chunk = chunks[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            chunk.forEach { index ->
                                val isSelected = selectedPages.contains(index)
                                val bitmap = pageThumbnails.getOrNull(index)

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedPreviewPage = Pair(index, bitmap)
                                        }
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) ToolBlue else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bitmap != null) {
                                            AsyncImage(
                                                model = bitmap,
                                                contentDescription = "Page ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("P. ${index + 1}", fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Selection Badge
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(ToolBlue)
                                                    .align(Alignment.TopEnd),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        // Page Number footer
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .align(Alignment.BottomCenter)
                                                .padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Page ${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                        }

                                        // Selection toggle button
                                        IconButton(
                                            onClick = {
                                                if (isSelected) selectedPages.remove(index) else selectedPages.add(index)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(32.dp)
                                                .background(
                                                    if (isSelected) ToolBlue else Color.Black.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                                                contentDescription = "Select page",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Fill empty slots in the last row
                            repeat(3 - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Output File Name
                    OutlinedTextField(
                        value = outputName,
                        onValueChange = { outputName = it },
                        label = { Text("Output File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Button
                    Button(
                        onClick = {
                            val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                            val tag = if (toolTitle.contains("Extract", ignoreCase = true)) "Extraction" else "Split"
                            viewModel.splitPdf(initialFile, selectedPages.sorted(), sanitized, toolTitle, tag) {
                                // viewModel already calls commitAndShowResult
                            }
                        },
                        enabled = selectedPages.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_split_now"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                    ) {
                        Icon(if (toolTitle.contains("Extract", ignoreCase = true)) Icons.Default.Description else Icons.Default.CallSplit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val btnText = if (toolTitle.contains("Extract", ignoreCase = true)) "Extract ${selectedPages.size} Pages Now" else "Split ${selectedPages.size} Pages Now"
                        Text(text = btnText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (selectedPreviewPage != null) {
        FullPageZoomDialog(
            bitmap = selectedPreviewPage!!.second,
            title = "Preview: ${initialFile.name}",
            subtitle = "Page ${selectedPreviewPage!!.first + 1} of ${initialFile.pageCount}",
            onDismiss = { selectedPreviewPage = null }
        )
    }
}

/**
 * STANDALONE EXTRACT PAGES SCREEN (DEDICATED PAGE EXTRACTION FLOW)
 * Distinct from Split PDF:
 * - Dedicated Extract Pages branding & theme (ToolTeal)
 * - Interactive multi-select visual page grid
 * - Instant full-screen pinch-to-zoom page preview
 * - Direct export to new PDF with 48dp bottom clearance
 */
@Composable
fun StandaloneExtractPagesScreen(
    initialFile: PdfFileRecord?,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    if (initialFile == null) {
        PdfInputSelectionScreen(
            toolTitle = "Extract Pages",
            toolSubtitle = "Select one PDF to extract specific pages into a new document",
            toolIcon = Icons.Default.Description,
            badgeColor = ToolTeal,
            allowMultiple = false,
            viewModel = viewModel,
            onBack = onBack,
            onSingleFileSelected = { file ->
                viewModel.selectFile(file)
            }
        )
        return
    }

    var outputName by remember {
        mutableStateOf("${initialFile.nameWithoutExtension()}_extracted.pdf")
    }
    val selectedPages = remember { mutableStateListOf<Int>() }
    var pageThumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoadingThumbnails by remember { mutableStateOf(true) }
    var selectedPreviewPage by remember { mutableStateOf<Pair<Int, Bitmap?>?>(null) }

    LaunchedEffect(initialFile) {
        withContext(Dispatchers.IO) {
            val file = File(initialFile.filePath)
            val pages = PdfEngine.renderCrispPdfPages(file, scale = 1.0f, maxPages = 40)
            pageThumbnails = pages
            isLoadingThumbnails = false
        }
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreen(isScrollable = false, paddingValues = PaddingValues(0.dp)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 8.dp)
            ) {
                // Top Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = ToolTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Extract Pages", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // Source File Info Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ToolTeal.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ToolTeal)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = initialFile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "${initialFile.pageCount} pages • Tap cards to zoom • Use check to select", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Page selection actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPages.size} PAGES SELECTED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ToolTeal
                        )

                        Row {
                            TextButton(onClick = {
                                selectedPages.clear()
                                for (i in 0 until initialFile.pageCount) selectedPages.add(i)
                            }) {
                                Text("Select All", fontSize = 12.sp, color = ToolTeal)
                            }
                            TextButton(onClick = {
                                selectedPages.clear()
                                val odds = (0 until initialFile.pageCount).filter { it % 2 == 0 }
                                selectedPages.addAll(odds)
                            }) {
                                Text("Odd Pages", fontSize = 12.sp)
                            }
                            TextButton(onClick = { selectedPages.clear() }) {
                                Text("Clear", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Grid of Page Thumbnails
                if (isLoadingThumbnails) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ToolTeal)
                        }
                    }
                } else {
                    val chunks = (0 until initialFile.pageCount).chunked(3)
                    items(chunks.size) { rowIndex ->
                        val chunk = chunks[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            chunk.forEach { index ->
                                val isSelected = selectedPages.contains(index)
                                val bitmap = pageThumbnails.getOrNull(index)

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedPreviewPage = Pair(index, bitmap)
                                        }
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) ToolTeal else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bitmap != null) {
                                            AsyncImage(
                                                model = bitmap,
                                                contentDescription = "Page ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("P. ${index + 1}", fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Selection Badge
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(ToolTeal)
                                                    .align(Alignment.TopEnd),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        // Page Number footer
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .align(Alignment.BottomCenter)
                                                .padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Page ${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                        }

                                        // Selection toggle button
                                        IconButton(
                                            onClick = {
                                                if (isSelected) selectedPages.remove(index) else selectedPages.add(index)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(32.dp)
                                                .background(
                                                    if (isSelected) ToolTeal else Color.Black.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                                                contentDescription = "Select page",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            repeat(3 - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Output File Name
                    OutlinedTextField(
                        value = outputName,
                        onValueChange = { outputName = it },
                        label = { Text("Output File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Button
                    Button(
                        onClick = {
                            val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                            viewModel.splitPdf(initialFile, selectedPages.sorted(), sanitized, "Extract Pages", "Extraction") {
                                // commitAndShowResult invoked by viewModel
                            }
                        },
                        enabled = selectedPages.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_extract_pages_now"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ToolTeal)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Extract ${selectedPages.size} Pages Now", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (selectedPreviewPage != null) {
        FullPageZoomDialog(
            bitmap = selectedPreviewPage!!.second,
            title = "Preview: ${initialFile.name}",
            subtitle = "Page ${selectedPreviewPage!!.first + 1} of ${initialFile.pageCount}",
            onDismiss = { selectedPreviewPage = null }
        )
    }
}

enum class CropDragTarget { NONE, TL, TR, BL, BR, L, T, R, B, CENTER }

/**
 * STANDALONE CROP SCREEN (EXAMPLE 3)
 * Follows exact workflow:
 * - Shows ONLY Crop Pages screen
 * - Real PDF preview with draggable crop box / margin controls
 * - Page navigation
 * - Apply to All/Odd/Even/Current
 * - Crop Now -> result preview -> Save
 */
@Composable
fun StandaloneCropScreen(
    initialFile: PdfFileRecord?,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    if (initialFile == null) {
        PdfInputSelectionScreen(
            toolTitle = "Crop PDF",
            toolSubtitle = "Trim margins or adjust PDF page dimensions",
            toolIcon = Icons.Default.Crop,
            badgeColor = ToolTeal,
            allowMultiple = false,
            viewModel = viewModel,
            onBack = onBack,
            onSingleFileSelected = { file ->
                viewModel.selectFile(file)
            }
        )
        return
    }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var cropLeft by remember { mutableFloatStateOf(0.08f) }
    var cropTop by remember { mutableFloatStateOf(0.08f) }
    var cropRight by remember { mutableFloatStateOf(0.08f) }
    var cropBottom by remember { mutableFloatStateOf(0.08f) }
    var cropScope by remember { mutableStateOf("ALL") } // "ALL", "CURRENT", "ODD", "EVEN"
    var outputName by remember {
        mutableStateOf("${initialFile.nameWithoutExtension()}_cropped.pdf")
    }

    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    LaunchedEffect(currentPageIndex, initialFile) {
        isLoadingPage = true
        withContext(Dispatchers.IO) {
            val file = File(initialFile.filePath)
            val pages = PdfEngine.renderCrispPdfPages(file, scale = 1.6f, maxPages = currentPageIndex + 1)
            pageBitmap = pages.getOrNull(currentPageIndex)
            isLoadingPage = false
        }
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreen(isScrollable = false, paddingValues = PaddingValues(0.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().imePadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(text = "Crop PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Page Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(Icons.Default.NavigateBefore, contentDescription = "Prev")
                    }

                    Text(
                        text = "Page ${currentPageIndex + 1} of ${initialFile.pageCount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    IconButton(
                        onClick = { if (currentPageIndex < initialFile.pageCount - 1) currentPageIndex++ },
                        enabled = currentPageIndex < initialFile.pageCount - 1
                    ) {
                        Icon(Icons.Default.NavigateNext, contentDescription = "Next")
                    }
                }

                // Live PDF Preview with Interactive Crop Overlay
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingPage) {
                            CircularProgressIndicator(color = ToolTeal)
                        } else if (pageBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(pageBitmap!!.width.toFloat() / pageBitmap!!.height.toFloat())
                            ) {
                                var dragTarget by remember { mutableStateOf(CropDragTarget.NONE) }

                                AsyncImage(
                                    model = pageBitmap,
                                    contentDescription = "Crop preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )

                                // Visual Crop Overlay
                                Canvas(modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val w = size.width.toFloat()
                                                val h = size.height.toFloat()
                                                val l = w * cropLeft
                                                val t = h * cropTop
                                                val r = w * (1f - cropRight)
                                                val b = h * (1f - cropBottom)
                                                val touchRadius = 60f
                                                val x = offset.x
                                                val y = offset.y

                                                dragTarget = when {
                                                    x in (l-touchRadius)..(l+touchRadius) && y in (t-touchRadius)..(t+touchRadius) -> CropDragTarget.TL
                                                    x in (r-touchRadius)..(r+touchRadius) && y in (t-touchRadius)..(t+touchRadius) -> CropDragTarget.TR
                                                    x in (l-touchRadius)..(l+touchRadius) && y in (b-touchRadius)..(b+touchRadius) -> CropDragTarget.BL
                                                    x in (r-touchRadius)..(r+touchRadius) && y in (b-touchRadius)..(b+touchRadius) -> CropDragTarget.BR
                                                    x in (l-touchRadius)..(l+touchRadius) && y in t..b -> CropDragTarget.L
                                                    x in (r-touchRadius)..(r+touchRadius) && y in t..b -> CropDragTarget.R
                                                    x in l..r && y in (t-touchRadius)..(t+touchRadius) -> CropDragTarget.T
                                                    x in l..r && y in (b-touchRadius)..(b+touchRadius) -> CropDragTarget.B
                                                    x in l..r && y in t..b -> CropDragTarget.CENTER
                                                    else -> CropDragTarget.NONE
                                                }
                                            },
                                            onDragEnd = { dragTarget = CropDragTarget.NONE },
                                            onDragCancel = { dragTarget = CropDragTarget.NONE },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val w = size.width.toFloat()
                                                val h = size.height.toFloat()
                                                val dxPct = dragAmount.x / w
                                                val dyPct = dragAmount.y / h
                                                val minSize = 0.05f

                                                when (dragTarget) {
                                                    CropDragTarget.TL -> { 
                                                        cropLeft = (cropLeft + dxPct).coerceIn(0f, 1f - cropRight - minSize)
                                                        cropTop = (cropTop + dyPct).coerceIn(0f, 1f - cropBottom - minSize) 
                                                    }
                                                    CropDragTarget.TR -> { 
                                                        cropRight = (cropRight - dxPct).coerceIn(0f, 1f - cropLeft - minSize)
                                                        cropTop = (cropTop + dyPct).coerceIn(0f, 1f - cropBottom - minSize) 
                                                    }
                                                    CropDragTarget.BL -> { 
                                                        cropLeft = (cropLeft + dxPct).coerceIn(0f, 1f - cropRight - minSize)
                                                        cropBottom = (cropBottom - dyPct).coerceIn(0f, 1f - cropTop - minSize) 
                                                    }
                                                    CropDragTarget.BR -> { 
                                                        cropRight = (cropRight - dxPct).coerceIn(0f, 1f - cropLeft - minSize)
                                                        cropBottom = (cropBottom - dyPct).coerceIn(0f, 1f - cropTop - minSize) 
                                                    }
                                                    CropDragTarget.L -> { cropLeft = (cropLeft + dxPct).coerceIn(0f, 1f - cropRight - minSize) }
                                                    CropDragTarget.R -> { cropRight = (cropRight - dxPct).coerceIn(0f, 1f - cropLeft - minSize) }
                                                    CropDragTarget.T -> { cropTop = (cropTop + dyPct).coerceIn(0f, 1f - cropBottom - minSize) }
                                                    CropDragTarget.B -> { cropBottom = (cropBottom - dyPct).coerceIn(0f, 1f - cropTop - minSize) }
                                                    CropDragTarget.CENTER -> {
                                                        val cropW = 1f - cropLeft - cropRight
                                                        val cropH = 1f - cropTop - cropBottom
                                                        
                                                        var newLeft = cropLeft + dxPct
                                                        var newRight = cropRight - dxPct
                                                        if (newLeft < 0f) { newLeft = 0f; newRight = 1f - cropW }
                                                        else if (newRight < 0f) { newRight = 0f; newLeft = 1f - cropW }
                                                        cropLeft = newLeft
                                                        cropRight = newRight
                                                        
                                                        var newTop = cropTop + dyPct
                                                        var newBottom = cropBottom - dyPct
                                                        if (newTop < 0f) { newTop = 0f; newBottom = 1f - cropH }
                                                        else if (newBottom < 0f) { newBottom = 0f; newTop = 1f - cropH }
                                                        cropTop = newTop
                                                        cropBottom = newBottom
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        )
                                    }
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val l = w * cropLeft
                                    val t = h * cropTop
                                    val r = w * (1f - cropRight)
                                    val b = h * (1f - cropBottom)

                                    // Darkened cropped borders
                                    drawRect(Color.Black.copy(alpha = 0.45f), topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, t))
                                    drawRect(Color.Black.copy(alpha = 0.45f), topLeft = androidx.compose.ui.geometry.Offset(0f, b), size = androidx.compose.ui.geometry.Size(w, h - b))
                                    drawRect(Color.Black.copy(alpha = 0.45f), topLeft = androidx.compose.ui.geometry.Offset(0f, t), size = androidx.compose.ui.geometry.Size(l, b - t))
                                    drawRect(Color.Black.copy(alpha = 0.45f), topLeft = androidx.compose.ui.geometry.Offset(r, t), size = androidx.compose.ui.geometry.Size(w - r, b - t))

                                    // Crop border
                                    drawRect(
                                        color = ToolTeal,
                                        topLeft = androidx.compose.ui.geometry.Offset(l, t),
                                        size = androidx.compose.ui.geometry.Size(r - l, b - t),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                                    )
                                    
                                    // Corner handles
                                    val handleRadius = 14f
                                    val paintColor = ToolTeal
                                    drawCircle(color = paintColor, radius = handleRadius, center = androidx.compose.ui.geometry.Offset(l, t))
                                    drawCircle(color = paintColor, radius = handleRadius, center = androidx.compose.ui.geometry.Offset(r, t))
                                    drawCircle(color = paintColor, radius = handleRadius, center = androidx.compose.ui.geometry.Offset(l, b))
                                    drawCircle(color = paintColor, radius = handleRadius, center = androidx.compose.ui.geometry.Offset(r, b))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Apply Scope Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Apply to:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL" to "All Pages", "CURRENT" to "Current", "ODD" to "Odd", "EVEN" to "Even").forEach { (code, label) ->
                            FilterChip(
                                selected = cropScope == code,
                                onClick = { cropScope = code },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Output File Name
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("Output File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Crop Action Button
                Button(
                    onClick = {
                        val targetPages = when (cropScope) {
                            "CURRENT" -> setOf(currentPageIndex)
                            "ODD" -> (0 until initialFile.pageCount).filter { it % 2 == 0 }.toSet()
                            "EVEN" -> (0 until initialFile.pageCount).filter { it % 2 != 0 }.toSet()
                            else -> emptySet() // All pages
                        }
                        val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                        viewModel.cropPdf(
                            sourceRecord = initialFile,
                            leftPct = cropLeft,
                            topPct = cropTop,
                            rightPct = 1f - cropRight,
                            bottomPct = 1f - cropBottom,
                            targetPages = targetPages,
                            outputName = sanitized
                        ) { resultRecord ->
                            viewModel.showToolResult(
                                ToolResultInfo(
                                    resultFile = resultRecord,
                                    toolTitle = "Crop PDF",
                                    summaryText = "Cropped margins successfully saved to ${resultRecord.name}."
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_crop_now"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ToolTeal)
                ) {
                    Icon(Icons.Default.Crop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crop PDF Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

/**
 * STANDALONE COMPRESS SCREEN (EXAMPLE 4)
 * Follows exact workflow:
 * - Shows ONLY Compress PDF screen
 * - Shows current file size
 * - Shows ONLY Custom Target Size (KB/MB)
 * - DO NOT show other compression presets
 * - Compress Now -> result preview -> Save
 */
@Composable
fun StandaloneCompressScreen(
    initialFile: PdfFileRecord?,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    if (initialFile == null) {
        PdfInputSelectionScreen(
            toolTitle = "Compress PDF",
            toolSubtitle = "Reduce PDF file size by setting a target size",
            toolIcon = Icons.Default.Compress,
            badgeColor = ToolGreen,
            allowMultiple = false,
            viewModel = viewModel,
            onBack = onBack,
            onSingleFileSelected = { file ->
                viewModel.selectFile(file)
            }
        )
        return
    }

    val currentSizeKB = (initialFile.fileSize / 1024L).coerceAtLeast(10L)
    val defaultTarget = (currentSizeKB / 2).coerceAtLeast(50L)

    var targetUnit by remember { mutableStateOf("KB") } // "KB" or "MB"
    var targetValueText by remember { mutableStateOf(defaultTarget.toString()) }
    var outputName by remember {
        mutableStateOf("${initialFile.nameWithoutExtension()}_compressed.pdf")
    }

    val targetKB = remember(targetValueText, targetUnit) {
        val num = targetValueText.toLongOrNull() ?: defaultTarget
        if (targetUnit == "MB") num * 1024L else num
    }

    val reductionPercent = remember(targetKB, currentSizeKB) {
        if (targetKB >= currentSizeKB) 0
        else (((currentSizeKB - targetKB).toFloat() / currentSizeKB.toFloat()) * 100).toInt().coerceIn(1, 90)
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreen(isScrollable = false, paddingValues = PaddingValues(0.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(text = "Compress PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Current File Size Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = initialFile.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Current File Size:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${formatFileSize(initialFile.fileSize)} ($currentSizeKB KB)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CrimsonPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Pages:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${initialFile.pageCount} pages", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Document Preview Dialog/Card
                var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var isLoadingPreview by remember { mutableStateOf(true) }
                var showFullPreview by remember { mutableStateOf(false) }
                LaunchedEffect(initialFile) {
                    withContext(Dispatchers.IO) {
                        try {
                            val pages = PdfEngine.renderCrispPdfPages(File(initialFile.filePath), scale = 1.6f, maxPages = 1)
                            previewBitmap = pages.firstOrNull()
                        } catch (_: Exception) {}
                        isLoadingPreview = false
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { if (previewBitmap != null) showFullPreview = true }
                        .testTag("compress_document_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isLoadingPreview) {
                            CircularProgressIndicator(color = CrimsonPrimary, modifier = Modifier.size(36.dp))
                        } else if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Document Preview (Tap to Zoom)",
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentScale = ContentScale.Fit
                            )

                            // Tap to Zoom / Inspect pill
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.68f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tap to Zoom", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        }
                    }
                }

                if (showFullPreview) {
                    FullPageZoomDialog(
                        bitmap = previewBitmap,
                        title = "Verify Document: ${initialFile.name}",
                        subtitle = "${initialFile.pageCount} pages • ${formatFileSize(initialFile.fileSize)}",
                        onDismiss = { showFullPreview = false }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CUSTOM TARGET SIZE SECTION (ONLY THIS IS SHOWN)
                Text(
                    text = "CUSTOM TARGET SIZE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ToolGreen,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ToolGreen.copy(alpha = 0.08f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ToolGreen.copy(alpha = 0.5f)))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Set the desired output file size:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = targetValueText,
                                onValueChange = { targetValueText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Target Size") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_compress_target_size"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // KB / MB Selector
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(4.dp)
                            ) {
                                listOf("KB", "MB").forEach { unit ->
                                    val isSelected = targetUnit == unit
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) ToolGreen else Color.Transparent)
                                            .clickable {
                                                if (targetUnit != unit) {
                                                    targetUnit = unit
                                                    targetValueText = if (unit == "MB") {
                                                        ((targetKB / 1024L).coerceAtLeast(1L)).toString()
                                                    } else {
                                                        (targetKB).toString()
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unit,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Estimated Reduction Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ToolGreen.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimated Reduction:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (reductionPercent > 0) "~$reductionPercent% smaller" else "Same size",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ToolGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Output File Name
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("Output File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                    // Action Button
                    Button(
                        onClick = {
                            val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                            viewModel.compressPdfCustom(
                                sourceRecord = initialFile,
                                targetKB = targetKB,
                                outputName = sanitized
                            ) {
                                // viewModel already calls commitAndShowResult
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_compress_now"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ToolGreen)
                    ) {
                        Icon(Icons.Default.Compress, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compress Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

/**
 * STANDALONE ROTATE SCREEN
 */
@Composable
fun StandaloneRotateScreen(
    initialFile: PdfFileRecord?,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    if (initialFile == null) {
        PdfInputSelectionScreen(
            toolTitle = "Rotate PDF",
            toolSubtitle = "Rotate individual or all pages of a PDF document",
            toolIcon = Icons.Default.RotateRight,
            badgeColor = ToolBlue,
            allowMultiple = false,
            viewModel = viewModel,
            onBack = onBack,
            onSingleFileSelected = { file -> viewModel.selectFile(file) }
        )
        return
    }

    val context = LocalContext.current
    var rotationDeg by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var scopeOption by remember { mutableStateOf("ALL") }
    var outputName by remember {
        mutableStateOf("${initialFile.nameWithoutExtension()}_rotated.pdf")
    }

    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    LaunchedEffect(currentPageIndex, initialFile) {
        isLoadingPage = true
        withContext(Dispatchers.IO) {
            val file = File(initialFile.filePath)
            pageBitmap = PdfEngine.renderSinglePdfPage(
                file = file,
                pageIndex = currentPageIndex,
                scale = 1.8f,
                context = context,
                uriString = initialFile.uriString
            )
            isLoadingPage = false
        }
    }

    val isTarget = when (scopeOption) {
        "CURRENT" -> true
        "ODD" -> currentPageIndex % 2 == 0
        "EVEN" -> currentPageIndex % 2 != 0
        else -> true
    }
    val targetRotation = if (isTarget) rotationDeg.toFloat() else 0f
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pdf_preview_rotation"
    )

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreen(isScrollable = false, paddingValues = PaddingValues(0.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Rotate PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Page Navigation Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.testTag("rotate_prev_page_button")
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = "Previous Page")
                        }

                        Text(
                            text = "Page ${currentPageIndex + 1} of ${initialFile.pageCount}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        IconButton(
                            onClick = { if (currentPageIndex < initialFile.pageCount - 1) currentPageIndex++ },
                            enabled = currentPageIndex < initialFile.pageCount - 1,
                            modifier = Modifier.testTag("rotate_next_page_button")
                        ) {
                            Icon(Icons.Default.NavigateNext, contentDescription = "Next Page")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Center Interactive Live Preview Container
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(vertical = 4.dp)
                            .testTag("rotate_preview_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingPage) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = ToolBlue,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Rendering Page ${currentPageIndex + 1}...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (pageBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                shadowElevation = 6.dp,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .graphicsLayer {
                                        rotationZ = animatedRotation
                                    }
                                    .testTag("pdf_page_preview")
                            ) {
                                Image(
                                    bitmap = pageBitmap!!.asImageBitmap(),
                                    contentDescription = "PDF Page ${currentPageIndex + 1} Preview (Rotated ${animatedRotation.toInt()}°)",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Page preview unavailable",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Rotation Angle Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "0°", 90 to "90° CW", 180 to "180° Flip", 270 to "270° CW").forEach { (deg, label) ->
                        OutlinedButton(
                            onClick = { rotationDeg = deg },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("rotate_chip_$deg"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (rotationDeg == deg) ToolBlue.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(
                                if (rotationDeg == deg) 2.dp else 1.dp,
                                if (rotationDeg == deg) ToolBlue else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (rotationDeg == deg) ToolBlue else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (rotationDeg == deg) FontWeight.Bold else FontWeight.Normal,
                                color = if (rotationDeg == deg) ToolBlue else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Apply Scope
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Apply to:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL" to "All Pages", "CURRENT" to "Current", "ODD" to "Odd", "EVEN" to "Even").forEach { (code, label) ->
                            FilterChip(
                                selected = scopeOption == code,
                                onClick = { scopeOption = code },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("Output File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val targetPages = when (scopeOption) {
                            "CURRENT" -> setOf(currentPageIndex)
                            "ODD" -> (0 until initialFile.pageCount).filter { it % 2 == 0 }.toSet()
                            "EVEN" -> (0 until initialFile.pageCount).filter { it % 2 != 0 }.toSet()
                            else -> emptySet()
                        }
                        val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                        viewModel.rotatePdf(initialFile, rotationDeg, targetPages, sanitized) { resultRecord ->
                            viewModel.showToolResult(
                                ToolResultInfo(
                                    resultFile = resultRecord,
                                    toolTitle = "Rotate PDF",
                                    summaryText = "Rotated pages by $rotationDeg° into ${resultRecord.name}."
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("rotate_and_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                ) {
                    Icon(Icons.Default.RotateRight, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rotate & Save PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                }
            }
        }
    }
}

/**
 * STANDALONE WATERMARK SCREEN
 */
@Composable
fun StandaloneWatermarkScreen(
    initialFile: PdfFileRecord?,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    if (initialFile == null) {
        PdfInputSelectionScreen(
            toolTitle = "Add Watermark",
            toolSubtitle = "Stamp confidential or custom watermark onto your PDF",
            toolIcon = Icons.Default.Draw,
            badgeColor = ToolPink,
            allowMultiple = false,
            viewModel = viewModel,
            onBack = onBack,
            onSingleFileSelected = { file -> viewModel.selectFile(file) }
        )
        return
    }

    val context = LocalContext.current
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var rotationDeg by remember { mutableIntStateOf(-45) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var isTiled by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var sizeScale by remember { mutableFloatStateOf(1.0f) }
    var outputName by remember {
        mutableStateOf("${initialFile.nameWithoutExtension()}_watermarked.pdf")
    }

    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var pageLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPageIndex, initialFile) {
        if (initialFile.filePath.isBlank() && initialFile.uriString.isNullOrBlank()) {
            isLoadingPage = false
            pageLoadError = "Invalid or empty PDF file reference"
            return@LaunchedEffect
        }
        isLoadingPage = true
        pageLoadError = null
        withContext(Dispatchers.IO) {
            try {
                val file = File(initialFile.filePath)
                val rendered = PdfEngine.renderSinglePdfPage(
                    file = file,
                    pageIndex = currentPageIndex.coerceIn(0, (initialFile.pageCount - 1).coerceAtLeast(0)),
                    scale = 1.8f,
                    context = context,
                    uriString = initialFile.uriString
                )
                if (rendered != null) {
                    pageBitmap = rendered
                } else {
                    pageLoadError = "Failed to render PDF page bitmap"
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                pageLoadError = e.localizedMessage ?: "Error rendering PDF page (possibly out of memory)"
            } finally {
                isLoadingPage = false
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val animatedRotation by animateFloatAsState(
        targetValue = rotationDeg.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "watermark_preview_rotation"
    )

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreen(isScrollable = false, paddingValues = PaddingValues(0.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Add Watermark", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Page Navigation Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.testTag("watermark_prev_page_button")
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = "Previous Page")
                        }

                        Text(
                            text = "Page ${currentPageIndex + 1} of ${initialFile.pageCount}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        IconButton(
                            onClick = { if (currentPageIndex < initialFile.pageCount - 1) currentPageIndex++ },
                            enabled = currentPageIndex < initialFile.pageCount - 1,
                            modifier = Modifier.testTag("watermark_next_page_button")
                        ) {
                            Icon(Icons.Default.NavigateNext, contentDescription = "Next Page")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Center Page Preview Container with Live Watermark Overlay
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(vertical = 4.dp)
                            .testTag("watermark_preview_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingPage) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = ToolPink,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Rendering PDF Page...", fontSize = 12.sp)
                            }
                        } else if (pageBitmap != null) {
                            // PDF Page Base
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                shadowElevation = 6.dp,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .aspectRatio(pageBitmap!!.width.toFloat() / pageBitmap!!.height.toFloat())
                                    .pointerInput(isTiled) {
                                        if (!isTiled) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount
                                            }
                                        }
                                    }
                            ) {
                                Image(
                                    bitmap = pageBitmap!!.asImageBitmap(),
                                    contentDescription = "PDF Page Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawWithContent {
                                            drawContent() // Draw PDF Page first
                                            
                                            val wmText = watermarkText.ifBlank { "PREVIEW" }.uppercase()
                                            
                                            if (isTiled) {
                                                // Full-page repeating watermark grid
                                                val cols = 3
                                                val rows = 5
                                                val stepX = size.width / cols
                                                val stepY = size.height / rows
                                                val textSz = (size.width / 16f).coerceIn(12f, 22f) * sizeScale
                                                val textStyle = TextStyle(
                                                    color = Color.Red.copy(alpha = 0.32f),
                                                    fontSize = textSz.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                                val layoutResult = textMeasurer.measure(wmText, textStyle)

                                                for (c in 0 until cols) {
                                                    for (r in 0 until rows) {
                                                        val cellCenter = androidx.compose.ui.geometry.Offset(
                                                            x = stepX * (c + 0.5f),
                                                            y = stepY * (r + 0.5f)
                                                        )
                                                        this@drawWithContent.rotate(
                                                            degrees = animatedRotation,
                                                            pivot = cellCenter
                                                        ) {
                                                            drawText(
                                                                textLayoutResult = layoutResult,
                                                                topLeft = androidx.compose.ui.geometry.Offset(
                                                                    x = cellCenter.x - layoutResult.size.width / 2f,
                                                                    y = cellCenter.y - layoutResult.size.height / 2f
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Single-line draggable/resizable watermark
                                                val textSz = ((size.width / 8f).coerceIn(18f, 75f) * sizeScale)
                                                val textStyle = TextStyle(
                                                    color = Color.Red.copy(alpha = 0.45f),
                                                    fontSize = textSz.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    textAlign = TextAlign.Center
                                                )
                                                val textLayoutResult = textMeasurer.measure(wmText, textStyle)
                                                
                                                val targetCenter = androidx.compose.ui.geometry.Offset(
                                                    x = (center.x + dragOffset.x).coerceIn(textLayoutResult.size.width / 4f, size.width - textLayoutResult.size.width / 4f),
                                                    y = (center.y + dragOffset.y).coerceIn(textLayoutResult.size.height / 4f, size.height - textLayoutResult.size.height / 4f)
                                                )

                                                this@drawWithContent.rotate(
                                                    degrees = animatedRotation,
                                                    pivot = targetCenter
                                                ) {
                                                    drawText(
                                                        textLayoutResult = textLayoutResult,
                                                        topLeft = targetCenter.copy(
                                                            x = targetCenter.x - textLayoutResult.size.width / 2f,
                                                            y = targetCenter.y - textLayoutResult.size.height / 2f
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else if (pageLoadError != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Error",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    pageLoadError ?: "Failed to render preview",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                "No preview available",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Watermark Pattern Mode: Single Line vs Tiled Full-Page
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isTiled,
                        onClick = { isTiled = false },
                        label = { Text("Single Line (Draggable)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f).testTag("chip_watermark_single")
                    )
                    FilterChip(
                        selected = isTiled,
                        onClick = { isTiled = true },
                        label = { Text("Full-Page Tiled", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f).testTag("chip_watermark_tiled")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { watermarkText = it },
                    label = { Text("Watermark Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("watermark_text_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Watermark Size Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Watermark Size:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.75f to "S", 1.0f to "M", 1.35f to "L", 1.75f to "XL").forEach { (sc, lbl) ->
                            FilterChip(
                                selected = sizeScale == sc,
                                onClick = { sizeScale = sc },
                                label = { Text(lbl, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Rotation Angle Selector (Orientation Modes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(-45 to "45° Diagonal", 0 to "Horizontal", -90 to "Vertical").forEach { (deg, label) ->
                        FilterChip(
                            selected = rotationDeg == deg,
                            onClick = { rotationDeg = deg },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("watermark_chip_$deg")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("Output File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                        val normX = if (!isTiled && (dragOffset.x != 0f || dragOffset.y != 0f)) 0.5f + (dragOffset.x / 400f).coerceIn(-0.4f, 0.4f) else null
                        val normY = if (!isTiled && (dragOffset.x != 0f || dragOffset.y != 0f)) 0.5f + (dragOffset.y / 500f).coerceIn(-0.4f, 0.4f) else null
                        viewModel.applyWatermarkStandalone(
                            sourceRecord = initialFile,
                            text = watermarkText,
                            rotationDeg = rotationDeg,
                            outputName = sanitized,
                            isTiled = isTiled,
                            normX = normX,
                            normY = normY,
                            fontSize = 32f * sizeScale
                        ) { resultRecord ->
                            viewModel.showToolResult(
                                ToolResultInfo(
                                    resultFile = resultRecord,
                                    toolTitle = "Add Watermark",
                                    summaryText = "Watermark \"$watermarkText\" applied to ${resultRecord.name}."
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("watermark_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ToolPink)
                ) {
                    Icon(Icons.Default.Draw, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply & Save PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

/**
 * STANDALONE PAGE NUMBERS SCREEN
 */
@Composable
fun StandalonePageNumbersScreen(
    initialFile: PdfFileRecord?,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    if (initialFile == null) {
        PdfInputSelectionScreen(
            toolTitle = "Add Page Numbers",
            toolSubtitle = "Insert customized page numbering into header or footer",
            toolIcon = Icons.Default.FormatListNumbered,
            badgeColor = ToolIndigo,
            allowMultiple = false,
            viewModel = viewModel,
            onBack = onBack,
            onSingleFileSelected = { file -> viewModel.selectFile(file) }
        )
        return
    }

    var formatOption by remember { mutableStateOf("Page {X} of {Y}") }
    var positionOption by remember { mutableStateOf(PdfEngine.PageNumberPosition.BOTTOM_RIGHT) }
    var skipCover by remember { mutableStateOf(false) }
    var outputName by remember {
        mutableStateOf("${initialFile.nameWithoutExtension()}_numbered.pdf")
    }

    val context = LocalContext.current
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(currentPageIndex, initialFile) {
        isLoadingPage = true
        withContext(Dispatchers.IO) {
            val file = File(initialFile.filePath)
            pageBitmap = PdfEngine.renderSinglePdfPage(
                file = file,
                pageIndex = currentPageIndex,
                scale = 1.8f,
                context = context,
                uriString = initialFile.uriString
            )
            isLoadingPage = false
        }
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreen(isScrollable = false, paddingValues = PaddingValues(16.dp)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Add Page Numbers", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Page Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                        enabled = currentPageIndex > 0,
                        modifier = Modifier.testTag("pagenum_prev_page_button")
                    ) {
                        Icon(Icons.Default.NavigateBefore, contentDescription = "Previous Page")
                    }

                    Text(
                        text = "Page ${currentPageIndex + 1} of ${initialFile.pageCount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    IconButton(
                        onClick = { if (currentPageIndex < initialFile.pageCount - 1) currentPageIndex++ },
                        enabled = currentPageIndex < initialFile.pageCount - 1,
                        modifier = Modifier.testTag("pagenum_next_page_button")
                    ) {
                        Icon(Icons.Default.NavigateNext, contentDescription = "Next Page")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Center Page Preview Container with Live Page Number Overlay
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("pagenum_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingPage) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = ToolIndigo,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Rendering PDF Page...", fontSize = 12.sp)
                            }
                        } else if (pageBitmap != null) {
                            // PDF Page Base
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                shadowElevation = 6.dp,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .aspectRatio(pageBitmap!!.width.toFloat() / pageBitmap!!.height.toFloat())
                            ) {
                                Image(
                                    bitmap = pageBitmap!!.asImageBitmap(),
                                    contentDescription = "PDF Page Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawWithContent {
                                            drawContent() // Draw PDF Page first
                                            
                                            // Draw Live Page Number Overlay
                                            val xVal = (currentPageIndex + 1).toString()
                                            val yVal = initialFile.pageCount.toString()
                                            val displayText = formatOption.replace("{X}", xVal).replace("{Y}", yVal)
                                            
                                            val textSz = (this@drawWithContent.size.width / 35f).coerceIn(10f, 24f)
                                            val textStyle = TextStyle(
                                                color = Color(0xFF64748B), // Slate 500
                                                fontSize = textSz.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            val textLayoutResult = textMeasurer.measure(displayText, textStyle)
                                            
                                            val margin = this@drawWithContent.size.width * 0.05f
                                            val x = when (positionOption) {
                                                PdfEngine.PageNumberPosition.BOTTOM_LEFT -> margin
                                                PdfEngine.PageNumberPosition.BOTTOM_CENTER -> this@drawWithContent.size.width / 2f - textLayoutResult.size.width / 2f
                                                PdfEngine.PageNumberPosition.BOTTOM_RIGHT -> this@drawWithContent.size.width - textLayoutResult.size.width - margin
                                                else -> this@drawWithContent.size.width / 2f - textLayoutResult.size.width / 2f
                                            }
                                            val y = this@drawWithContent.size.height - textLayoutResult.size.height - margin
                                            
                                            drawText(
                                                textLayoutResult = textLayoutResult,
                                                color = Color(0xFF64748B),
                                                topLeft = androidx.compose.ui.geometry.Offset(x, y)
                                            )
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Controls in a scrollable column if needed, or just fits
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Numbering Format
                    Text("NUMBERING FORMAT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ToolIndigo)
                    Spacer(modifier = Modifier.height(4.dp))

                    listOf("Page {X} of {Y}", "Page {X}", "{X}", "- {X} -").forEach { fmt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { formatOption = fmt },
                            colors = CardDefaults.cardColors(
                                containerColor = if (formatOption == fmt) ToolIndigo.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = formatOption == fmt,
                                    onClick = { formatOption = fmt },
                                    colors = RadioButtonDefaults.colors(selectedColor = ToolIndigo),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fmt.replace("{X}", "1").replace("{Y}", initialFile.pageCount.toString()),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Position Selector
                    Text("POSITION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ToolIndigo)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PdfEngine.PageNumberPosition.BOTTOM_LEFT to "Bottom Left",
                            PdfEngine.PageNumberPosition.BOTTOM_CENTER to "Bottom Center",
                            PdfEngine.PageNumberPosition.BOTTOM_RIGHT to "Bottom Right"
                        ).forEach { (pos, label) ->
                            FilterChip(
                                selected = positionOption == pos,
                                onClick = { positionOption = pos },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Skip Cover
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Skip Cover Page (Start from page 2)", fontSize = 12.sp)
                        Checkbox(
                            checked = skipCover,
                            onCheckedChange = { skipCover = it },
                            colors = CheckboxDefaults.colors(checkedColor = ToolIndigo),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = outputName,
                        onValueChange = { outputName = it },
                        label = { Text("Output File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(fontSize = 13.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val targetPages = if (skipCover) (1 until initialFile.pageCount).toSet() else emptySet()
                            val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                            viewModel.addCustomPageNumbers(
                                sourceRecord = initialFile,
                                format = formatOption,
                                position = positionOption,
                                targetPages = targetPages,
                                outputName = sanitized
                            ) { resultRecord: PdfFileRecord ->
                                viewModel.showToolResult(
                                    ToolResultInfo(
                                        resultFile = resultRecord,
                                        toolTitle = "Add Page Numbers",
                                        summaryText = "Added page numbers to ${resultRecord.name}."
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pagenum_save_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ToolIndigo)
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Page Numbers Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}


fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}
