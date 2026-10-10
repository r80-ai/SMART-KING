package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PdfFileRecord
import com.example.pdf.PdfEngine
import com.example.ui.AppScreen
import com.example.ui.PdfMasterViewModel
import com.example.ui.dialogs.PdfInputSelectionScreen
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RemovePagesScreen - Full interactive visual PDF page removal tool.
 * Allows users to preview document pages in a responsive grid, multi-select pages
 * for deletion via direct touch or quick filter chips (All, Clear, Odd, Even, Custom Range),
 * and export the sanitized PDF.
 */
@Composable
fun RemovePagesScreen(
    initialFile: PdfFileRecord? = null,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    var currentFile by remember(initialFile) { mutableStateOf(initialFile) }

    // If no document is selected yet, display the full upload/selection UI
    if (currentFile == null) {
        PdfInputSelectionScreen(
            toolTitle = "Remove Pages",
            toolSubtitle = "Delete unwanted or blank pages instantly from your PDF",
            toolIcon = Icons.Default.DeleteSweep,
            badgeColor = CrimsonPrimary,
            allowMultiple = false,
            viewModel = viewModel,
            onBack = onBack,
            onSingleFileSelected = { selected ->
                currentFile = selected
            }
        )
        return
    }

    val activeFile = currentFile!!
    val context = LocalContext.current
    val totalPages = activeFile.pageCount.coerceAtLeast(1)

    // Set of 0-based page indices selected for removal
    var pagesToRemove by remember(activeFile.filePath) { mutableStateOf(setOf<Int>()) }
    var outputName by remember(activeFile.filePath) {
        mutableStateOf("${activeFile.nameWithoutExtension()}_pages_removed.pdf")
    }

    // Page thumbnails cache
    val thumbnailMap = remember(activeFile.filePath) { mutableStateMapOf<Int, Bitmap>() }
    var isLoadingThumbnails by remember(activeFile.filePath) { mutableStateOf(true) }
    var customRangeText by remember { mutableStateOf("") }
    var selectedPreviewPage by remember { mutableStateOf<Pair<Int, Bitmap?>?>(null) }

    // System Document Picker for swapping source document
    val swapFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPdfFromUri(uri) { record ->
                currentFile = record
            }
        }
    }

    // Load crisp page thumbnails in background coroutines
    LaunchedEffect(activeFile.filePath) {
        isLoadingThumbnails = true
        withContext(Dispatchers.IO) {
            try {
                val file = File(activeFile.filePath)
                var pfd: ParcelFileDescriptor? = null
                if (file.exists() && file.canRead()) {
                    pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                } else if (!activeFile.uriString.isNullOrBlank()) {
                    pfd = context.contentResolver.openFileDescriptor(android.net.Uri.parse(activeFile.uriString), "r")
                }

                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    val count = renderer.pageCount
                    for (i in 0 until count) {
                        try {
                            val page = renderer.openPage(i)
                            val scale = 1.2f
                            val w = (page.width * scale).toInt().coerceAtLeast(1)
                            val h = (page.height * scale).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            thumbnailMap[i] = bmp
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    renderer.close()
                    pfd.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoadingThumbnails = false
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
                    .navigationBarsPadding()
                    .testTag("remove_pages_screen"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp, top = 8.dp)
            ) {
                // Top Header Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("remove_pages_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Remove Pages",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${activeFile.pageCount} Total Pages",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { swapFileLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.testTag("remove_pages_change_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Change PDF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("remove_pages_close_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }
                }

                // Compact File Banner
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = CrimsonPrimary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeFile.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CrimsonPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${activeFile.pageCount} Pages",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Pinned Quick Selection Chips Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = pagesToRemove.size == totalPages,
                            onClick = {
                                pagesToRemove = (0 until totalPages).toSet()
                            },
                            label = { Text("Select All", fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_select_all"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonPrimary.copy(alpha = 0.18f),
                                selectedLabelColor = CrimsonPrimary
                            )
                        )

                        FilterChip(
                            selected = pagesToRemove.isEmpty(),
                            onClick = {
                                pagesToRemove = emptySet()
                            },
                            label = { Text("Clear Selection", fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_clear_selection")
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                val oddIndices = (0 until totalPages).filter { (it + 1) % 2 != 0 }.toSet()
                                pagesToRemove = pagesToRemove + oddIndices
                            },
                            label = { Text("Odd Pages", fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_odd_pages")
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                val evenIndices = (0 until totalPages).filter { (it + 1) % 2 == 0 }.toSet()
                                pagesToRemove = pagesToRemove + evenIndices
                            },
                            label = { Text("Even Pages", fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_even_pages")
                        )
                    }
                }

                // Visual Page Grid
                if (isLoadingThumbnails && thumbnailMap.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = CrimsonPrimary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Rendering pages...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val chunks = (0 until totalPages).chunked(2)
                    items(chunks.size) { rowIndex ->
                        val chunk = chunks[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            chunk.forEach { pageIndex ->
                                val isSelectedForRemoval = pagesToRemove.contains(pageIndex)
                                val pageBitmap = thumbnailMap[pageIndex]

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedPreviewPage = Pair(pageIndex, pageBitmap)
                                        }
                                        .testTag("page_card_$pageIndex"),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isSelectedForRemoval) 1.dp else 4.dp
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelectedForRemoval) 2.dp else 1.dp,
                                        color = if (isSelectedForRemoval) CrimsonPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (pageBitmap != null) {
                                            Image(
                                                bitmap = pageBitmap.asImageBitmap(),
                                                contentDescription = "Page ${pageIndex + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = CrimsonPrimary
                                                )
                                            }
                                        }

                                        if (isSelectedForRemoval) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFFDC2626).copy(alpha = 0.38f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFFDC2626),
                                                        shadowElevation = 6.dp
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Marked for removal",
                                                            tint = Color.White,
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .padding(6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            color = if (isSelectedForRemoval) Color(0xFFDC2626) else Color(0xCC0F172A),
                                            shape = RoundedCornerShape(bottomEnd = 8.dp),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = "Page ${pageIndex + 1}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }

                                        if (isSelectedForRemoval) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFDC2626),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Deleted",
                                                        tint = Color.White,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Selection toggle button (since card click now previews)
                                        IconButton(
                                            onClick = {
                                                pagesToRemove = if (isSelectedForRemoval) {
                                                    pagesToRemove - pageIndex
                                                } else {
                                                    pagesToRemove + pageIndex
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(32.dp)
                                                .background(
                                                    if (isSelectedForRemoval) CrimsonPrimary else Color.Black.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = if (isSelectedForRemoval) Icons.Default.Delete else Icons.Default.CheckCircle,
                                                contentDescription = "Select for removal",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (chunk.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // BOTTOM CONTROLS & ACTION BAR
                item {
                    val remainingCount = (totalPages - pagesToRemove.size).coerceAtLeast(0)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customRangeText,
                                    onValueChange = { customRangeText = it },
                                    placeholder = { Text("Custom range: e.g. 1-3, 5", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_custom_range"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CrimsonPrimary
                                    )
                                )

                                OutlinedButton(
                                    onClick = {
                                        if (customRangeText.isNotBlank()) {
                                            val parsed = PdfEngine.parsePageRange(customRangeText, totalPages)
                                            pagesToRemove = pagesToRemove + parsed.toSet()
                                        }
                                    },
                                    modifier = Modifier.testTag("btn_apply_range"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Remove: ${pagesToRemove.size} pages",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (pagesToRemove.isNotEmpty()) CrimsonPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Remaining: $remainingCount pages",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (remainingCount == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            if (remainingCount == 0) {
                                Text(
                                    text = "⚠️ Cannot remove all pages! At least 1 page must remain.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedTextField(
                                value = outputName,
                                onValueChange = { outputName = it },
                                label = { Text("Output File Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("remove_pages_output_name"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CrimsonPrimary
                                )
                            )

                            val isButtonEnabled = pagesToRemove.isNotEmpty() && remainingCount > 0

                            Button(
                                onClick = {
                                    val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                                    viewModel.removePagesFromPdf(
                                        sourceRecord = activeFile,
                                        pagesToRemove = pagesToRemove,
                                        outputName = sanitized
                                    )
                                },
                                enabled = isButtonEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("btn_remove_pages_save"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonPrimary,
                                    disabledContainerColor = CrimsonPrimary.copy(alpha = 0.35f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pagesToRemove.isEmpty()) "Select Pages to Remove"
                                    else "Remove ${pagesToRemove.size} Pages & Save PDF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }

    if (selectedPreviewPage != null) {
        com.example.ui.components.FullPageZoomDialog(
            bitmap = selectedPreviewPage!!.second,
            title = "Preview: ${activeFile.name}",
            subtitle = "Page ${selectedPreviewPage!!.first + 1} of ${activeFile.pageCount}",
            onDismiss = { selectedPreviewPage = null }
        )
    }
}
