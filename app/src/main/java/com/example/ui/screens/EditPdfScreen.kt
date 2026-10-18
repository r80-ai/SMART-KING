package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfFileRecord
import com.example.ui.PdfMasterViewModel
import com.example.ui.dialogs.PdfWorkspaceDialog
import com.example.ui.theme.CrimsonPrimary

/**
 * EditPdfScreen provides full document editing capabilities:
 * - Direct click-to-edit on detected text (OCR green boxes)
 * - All core PDF Tools integrated directly in the bottom tools bar:
 *   Crop, Rotate, Watermark, Page Numbering, Organize Pages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPdfScreen(
    initialFile: PdfFileRecord? = null,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    var activeFile by remember { mutableStateOf(initialFile) }
    val allFiles by viewModel.allFiles.collectAsState()
    val context = LocalContext.current

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.importPdfFromUri(uri) { record ->
                activeFile = record
            }
        }
    }

    if (activeFile != null) {
        PdfEditorMainView(
            fileRecord = activeFile!!,
            viewModel = viewModel,
            onBack = {
                activeFile = null
                if (initialFile != null) {
                    onBack()
                }
            }
        )
    } else {
        BackHandler { onBack() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("PDF Edit", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Feature overview hero card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonPrimary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Full PDF Editor & Tools", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CrimsonPrimary)
                        }
                        Text(
                            "Select any PDF to click & edit detected text, insert stamps, annotate, crop, rotate, watermark, insert page numbers, and organize pages.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { openDocLauncher.launch(arrayOf("application/pdf")) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_select_pdf_to_edit")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select PDF File", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("Or choose from recent files:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (allFiles.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No recent PDF files found. Tap above to select one.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(allFiles) { file ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeFile = file }
                                    .testTag("recent_file_${file.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                                        Text("${file.pageCount} pages • ${(file.fileSize / 1024).coerceAtLeast(1)} KB", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
