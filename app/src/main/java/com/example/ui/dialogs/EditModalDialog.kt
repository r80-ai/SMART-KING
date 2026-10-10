package com.example.ui.dialogs

import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PdfFileRecord
import com.example.pdf.PdfEngine
import com.example.ui.PdfMasterViewModel

@Composable
fun EditModalDialog(
    file: PdfFileRecord,
    viewModel: PdfMasterViewModel,
    onDismiss: () -> Unit
) {
    val pages by viewModel.renderedPages.collectAsState()
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var outputName by remember { mutableStateOf("${file.nameWithoutExtension()}_edited.pdf") }

    // Active edit tool: "TEXT", "STAMP", "PAGE"
    var activeTool by remember { mutableStateOf("TEXT") }

    // Text tool state
    var textToAdd by remember { mutableStateOf("") }
    var selectedTextColor by remember { mutableIntStateOf(AndroidColor.BLACK) }
    var selectedFontSize by remember { mutableFloatStateOf(16f) }

    // Stamp tool state
    var selectedStamp by remember { mutableStateOf("APPROVED") }

    // Accumulated annotations
    val annotations = remember { mutableStateListOf<PdfEngine.EditItem>() }
    val deletedPages = remember { mutableStateListOf<Int>() }
    val pageRotations = remember { mutableStateMapOf<Int, Int>() }

    val colorsList = listOf(
        Pair("Black", AndroidColor.BLACK),
        Pair("Blue", AndroidColor.rgb(37, 99, 235)),
        Pair("Red", AndroidColor.rgb(220, 38, 38)),
        Pair("Green", AndroidColor.rgb(22, 163, 74)),
        Pair("Purple", AndroidColor.rgb(147, 51, 234))
    )

    val stampsList = listOf("APPROVED", "CONFIDENTIAL", "DRAFT", "VERIFIED", "PAID", "URGENT")

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
                // Top App Bar
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Edit PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.saveEditedPdf(
                                file,
                                annotations.toList(),
                                deletedPages.toSet(),
                                pageRotations.toMap(),
                                outputName
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }
                }

                HorizontalDivider()

                // Tool Switcher (Text, Stamp, Page Operations)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeTool == "TEXT",
                        onClick = { activeTool = "TEXT" },
                        leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Add Text") }
                    )
                    FilterChip(
                        selected = activeTool == "STAMP",
                        onClick = { activeTool = "STAMP" },
                        leadingIcon = { Icon(Icons.Default.Approval, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Stamps") }
                    )
                    FilterChip(
                        selected = activeTool == "PAGE",
                        onClick = { activeTool = "PAGE" },
                        leadingIcon = { Icon(Icons.Default.Pages, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Page Tools") }
                    )
                }

                // Middle: Document Page Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F172A))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (pages.isEmpty()) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        val currentBmp = pages.getOrNull(currentPageIndex)
                        if (currentBmp != null) {
                            val isDeleted = deletedPages.contains(currentPageIndex)
                            val rot = pageRotations[currentPageIndex] ?: 0

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box {
                                        Image(
                                            bitmap = currentBmp.asImageBitmap(),
                                            contentDescription = "Page ${currentPageIndex + 1}",
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        )

                                        if (isDeleted) {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(Color(0xCCEF4444)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "PAGE MARKED FOR DELETION",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                        enabled = currentPageIndex > 0
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Page", tint = Color.White)
                                    }
                                    Text(
                                        text = "Page ${currentPageIndex + 1} of ${pages.size} ${if (rot != 0) "($rot°)" else ""}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    IconButton(
                                        onClick = { if (currentPageIndex < pages.size - 1) currentPageIndex++ },
                                        enabled = currentPageIndex < pages.size - 1
                                    ) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom: Active Tool Controls Panel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(16.dp)
                    ) {
                        when (activeTool) {
                            "TEXT" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = textToAdd,
                                        onValueChange = { textToAdd = it },
                                        placeholder = { Text("Type text to insert...") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (textToAdd.isNotBlank()) {
                                                annotations.add(
                                                    PdfEngine.EditItem(
                                                        pageIndex = currentPageIndex,
                                                        text = textToAdd,
                                                        posX = 50f,
                                                        posY = 120f + (annotations.count { it.pageIndex == currentPageIndex } * 30f),
                                                        colorHex = selectedTextColor,
                                                        fontSize = selectedFontSize
                                                    )
                                                )
                                                textToAdd = ""
                                            }
                                        },
                                        enabled = textToAdd.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                    ) {
                                        Text("Add")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Color:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    colorsList.forEach { (name, col) ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(col), CircleShape)
                                                .border(
                                                    if (selectedTextColor == col) 2.dp else 0.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                )
                                                .clickable { selectedTextColor = col }
                                        )
                                    }
                                }
                            }
                            "STAMP" -> {
                                Text("Select Official Stamp to Stamp on Page:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stampsList.take(3).forEach { stamp ->
                                        OutlinedButton(
                                            onClick = {
                                                annotations.add(
                                                    PdfEngine.EditItem(
                                                        pageIndex = currentPageIndex,
                                                        isStamp = true,
                                                        stampText = stamp,
                                                        posX = 60f,
                                                        posY = 180f
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stamp, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stampsList.drop(3).forEach { stamp ->
                                        OutlinedButton(
                                            onClick = {
                                                annotations.add(
                                                    PdfEngine.EditItem(
                                                        pageIndex = currentPageIndex,
                                                        isStamp = true,
                                                        stampText = stamp,
                                                        posX = 60f,
                                                        posY = 180f
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stamp, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            "PAGE" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val currentRot = pageRotations[currentPageIndex] ?: 0
                                            pageRotations[currentPageIndex] = (currentRot + 90) % 360
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rotate 90°", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            if (deletedPages.contains(currentPageIndex)) {
                                                deletedPages.remove(currentPageIndex)
                                            } else {
                                                deletedPages.add(currentPageIndex)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (deletedPages.contains(currentPageIndex)) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            if (deletedPages.contains(currentPageIndex)) Icons.Default.Restore else Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (deletedPages.contains(currentPageIndex)) "Restore" else "Delete Page", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            label = { Text("Output PDF Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
