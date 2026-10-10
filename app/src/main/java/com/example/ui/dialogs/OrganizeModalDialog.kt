package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PdfFileRecord
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple

enum class OrganizeMode {
    ORGANIZE,
    REMOVE_PAGES,
    REORDER
}

@Composable
fun OrganizeModalDialog(
    file: PdfFileRecord,
    viewModel: PdfMasterViewModel,
    mode: OrganizeMode = OrganizeMode.ORGANIZE,
    onDismiss: () -> Unit
) {
    val pages by viewModel.renderedPages.collectAsState()
    val totalCount = pages.size.coerceAtLeast(file.pageCount).coerceAtLeast(1)

    // Current page order: list of original page indices
    val pageOrder = remember { mutableStateListOf<Int>().apply { addAll(0 until totalCount) } }
    val pageRotations = remember { mutableStateMapOf<Int, Int>() }
    val selectedForRemoval = remember { mutableStateListOf<Int>() }

    var outputName by remember {
        mutableStateOf(
            when (mode) {
                OrganizeMode.REMOVE_PAGES -> "${file.nameWithoutExtension()}_trimmed.pdf"
                OrganizeMode.REORDER -> "${file.nameWithoutExtension()}_reordered.pdf"
                else -> "${file.nameWithoutExtension()}_organized.pdf"
            }
        )
    }

    LaunchedEffect(file) {
        viewModel.loadPagesForViewer(file)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val title = when (mode) {
                                OrganizeMode.REMOVE_PAGES -> "Remove Pages"
                                OrganizeMode.REORDER -> "Reorder Pages"
                                else -> "Organize PDF"
                            }
                            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "${pageOrder.size} pages • ${file.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (mode == OrganizeMode.REMOVE_PAGES) {
                                viewModel.removePagesFromPdf(file, selectedForRemoval.toSet(), outputName)
                            } else {
                                viewModel.organizePdf(file, pageOrder.toList(), pageRotations.toMap(), outputName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_organized_pdf_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }
                }

                // Instructions Banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (mode) {
                            OrganizeMode.REMOVE_PAGES -> "Tap pages to select them for removal. Selected pages will be deleted."
                            OrganizeMode.REORDER -> "Use arrow buttons on cards to move pages left or right in sequence."
                            else -> "Drag, reorder, rotate or delete pages. Preview updates immediately."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Pages Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(pageOrder) { index, origPageIdx ->
                        val isMarkedForDelete = selectedForRemoval.contains(origPageIdx)
                        val rotation = pageRotations[origPageIdx] ?: 0
                        val bmp by produceState<android.graphics.Bitmap?>(initialValue = null, origPageIdx, pages) {
                            val cached = pages.getOrNull(origPageIdx)
                            if (cached != null) {
                                value = cached
                            } else {
                                value = viewModel.getPageBitmap(origPageIdx, 1.0f)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isMarkedForDelete) 2.dp else 1.dp,
                                    color = if (isMarkedForDelete) CrimsonPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (mode == OrganizeMode.REMOVE_PAGES) {
                                        if (isMarkedForDelete) selectedForRemoval.remove(origPageIdx)
                                        else selectedForRemoval.add(origPageIdx)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMarkedForDelete) CrimsonPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Badge & Index
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isMarkedForDelete) CrimsonPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                            .size(22.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMarkedForDelete) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (mode == OrganizeMode.REMOVE_PAGES) {
                                        Icon(
                                            imageVector = if (isMarkedForDelete) Icons.Default.Delete else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            tint = if (isMarkedForDelete) CrimsonPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text("P.${origPageIdx + 1}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Page Preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp!!.asImageBitmap(),
                                            contentDescription = "Page ${index + 1}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(rotation.toFloat()),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ToolBlue, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Loading...", fontSize = 11.sp)
                                        }
                                    }

                                    if (isMarkedForDelete) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(CrimsonPrimary.copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Controls for Organize / Reorder
                                if (mode != OrganizeMode.REMOVE_PAGES) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Move left
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val temp = pageOrder[index]
                                                    pageOrder[index] = pageOrder[index - 1]
                                                    pageOrder[index - 1] = temp
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(16.dp))
                                        }

                                        // Rotate 90
                                        IconButton(
                                            onClick = {
                                                val curr = pageRotations[origPageIdx] ?: 0
                                                pageRotations[origPageIdx] = (curr + 90) % 360
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(16.dp), tint = ToolBlue)
                                        }

                                        // Move right
                                        IconButton(
                                            onClick = {
                                                if (index < pageOrder.size - 1) {
                                                    val temp = pageOrder[index]
                                                    pageOrder[index] = pageOrder[index + 1]
                                                    pageOrder[index + 1] = temp
                                                }
                                            },
                                            enabled = index < pageOrder.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", modifier = Modifier.size(16.dp))
                                        }

                                        // Delete from order
                                        IconButton(
                                            onClick = {
                                                if (pageOrder.size > 1) {
                                                    pageOrder.removeAt(index)
                                                }
                                            },
                                            enabled = pageOrder.size > 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = CrimsonPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Output Name Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            label = { Text("Output PDF Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
