package com.example.pvccard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvccard.model.PvcSheetLayout
import com.example.pvccard.viewmodel.PvcCardUiState
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue

/**
 * Step 4: Print Sheet Layout (A4 5-Cards, 4×6 Photo Paper) & Export / Print
 *
 * Layout Structure:
 * 1. Top Section: Sheet Preview anchored strictly at the top with natural A4 / 4×6 aspect ratio and ZERO empty black void.
 * 2. Middle Section: Paper Select Options directly underneath the preview.
 * 3. Bottom Section: Action buttons (Save PDF, Save JPG, Print).
 */
@Composable
fun PrintSheetStep(
    uiState: PvcCardUiState,
    onLayoutChanged: (PvcSheetLayout) -> Unit,
    onPageChanged: (Int) -> Unit,
    onRemoveCard: (String) -> Unit,
    onQueueCopiesDelta: (String, Int) -> Unit,
    onExportPdf: () -> Unit,
    onExportImage: () -> Unit,
    onDirectPrint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isA4 = uiState.sheetLayout == PvcSheetLayout.A4_FIVE_CARDS
    val sheetAspectRatio = if (isA4) 2480f / 3508f else 1200f / 1800f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. TOP SECTION: SHEET PREVIEW (AT THE VERY TOP)
        // Wrapped strictly around natural page aspect ratio (2480:3508 for A4, 1200:1800 for 4×6)
        // Pure white background, zero black void or excessive height
        item {
            val sheetBmp = uiState.sheetPreviewBitmap
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(sheetAspectRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .testTag("sheet_preview_canvas"),
                color = Color.White,
                shadowElevation = 3.dp
            ) {
                if (sheetBmp != null) {
                    Image(
                        bitmap = sheetBmp.asImageBitmap(),
                        contentDescription = "Print sheet layout preview",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CrimsonPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "Generating 300 DPI sheet preview...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. MIDDLE SECTION: PAGE SELECT OPTIONS (BELOW PREVIEW)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paper_selector_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isA4Selected = uiState.sheetLayout == PvcSheetLayout.A4_FIVE_CARDS
                val is4x6Selected = uiState.sheetLayout == PvcSheetLayout.PHOTO_4X6_ONE_CARD

                FilterChip(
                    selected = isA4Selected,
                    onClick = { onLayoutChanged(PvcSheetLayout.A4_FIVE_CARDS) },
                    label = {
                        Text(
                            "A4 Sheet (5 Cards Side-by-Side)",
                            fontSize = 11.sp,
                            fontWeight = if (isA4Selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = if (isA4Selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_layout_a4")
                )

                FilterChip(
                    selected = is4x6Selected,
                    onClick = { onLayoutChanged(PvcSheetLayout.PHOTO_4X6_ONE_CARD) },
                    label = {
                        Text(
                            "4×6 Photo Paper (1 Card)",
                            fontSize = 11.sp,
                            fontWeight = if (is4x6Selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = if (is4x6Selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_layout_4x6")
                )
            }
        }

        // Multi-Card Queue List (if queue has multiple items)
        if (uiState.cardQueue.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Card Queue (${uiState.cardQueue.size} items, ${uiState.cardQueue.sumOf { it.copies }} cards total)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        uiState.cardQueue.forEach { card ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Image(
                                        bitmap = card.frontBitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp, 23.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column {
                                        Text(card.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("${card.copies} copies", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onQueueCopiesDelta(card.id, -1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { onQueueCopiesDelta(card.id, 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { onRemoveCard(card.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. BOTTOM SECTION: EXPORT & PRINT BUTTONS
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExportPdf,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_pdf"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onExportImage,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_image"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save JPG", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = onDirectPrint,
                            colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_direct_print"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
