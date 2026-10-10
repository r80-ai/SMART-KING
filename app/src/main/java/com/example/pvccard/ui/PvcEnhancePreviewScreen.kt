package com.example.pvccard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvccard.model.PVC_CARD_HEIGHT_PX
import com.example.pvccard.model.PVC_CARD_WIDTH_PX
import com.example.pvccard.model.PvcCardTemplate
import com.example.pvccard.viewmodel.PvcCardUiState
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue

/**
 * Step 3: Card Enhancements & Preview (Text Sharpness, Brightness, Border, Copies)
 */
@Composable
fun EnhanceAndPreviewStep(
    uiState: PvcCardUiState,
    onBrightnessChanged: (Float) -> Unit,
    onContrastChanged: (Float) -> Unit,
    onToggleTextDarken: () -> Unit,
    onToggleBorder: () -> Unit,
    onCopiesChanged: (Int) -> Unit,
    onNameChanged: (String) -> Unit,
    onSwapSides: () -> Unit,
    onAddToQueue: () -> Unit,
    onGoToSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic real-time ColorFilter reflecting live brightness and contrast slider adjustments
    val liveColorFilter = remember(uiState.brightness, uiState.contrast) {
        val scale = (1.0f + (uiState.contrast / 100.0f)).coerceIn(0.1f, 3.0f)
        val translate = (uiState.brightness * 2.55f).coerceIn(-255f, 255f)
        val cm = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        ColorFilter.colorMatrix(cm)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High-Definition Preview Cards
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Front Card Preview (Header "CR80 PVC Card Preview" completely removed)
                    Text(
                        text = "Front Side",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val front = uiState.frontBitmap
                    if (front != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(PVC_CARD_WIDTH_PX.toFloat() / PVC_CARD_HEIGHT_PX.toFloat())
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (uiState.hasPerimeterBorder) 2.dp else 1.dp,
                                    color = if (uiState.hasPerimeterBorder) Color.DarkGray else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Image(
                                bitmap = front.asImageBitmap(),
                                contentDescription = "Front card preview",
                                colorFilter = liveColorFilter,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.LightGray.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("No front side cropped yet", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Back Side Header Row: Left "Back Side", Right compact "Swap Sides" button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Back Side",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ToolBlue
                        )
                        TextButton(
                            onClick = onSwapSides,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("btn_swap_sides")
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Swap Sides",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Swap", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    val back = uiState.backBitmap
                    if (back != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(PVC_CARD_WIDTH_PX.toFloat() / PVC_CARD_HEIGHT_PX.toFloat())
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (uiState.hasPerimeterBorder) 2.dp else 1.dp,
                                    color = if (uiState.hasPerimeterBorder) Color.DarkGray else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Image(
                                bitmap = back.asImageBitmap(),
                                contentDescription = "Back card preview",
                                colorFilter = liveColorFilter,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.LightGray.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Single side card (Back empty)", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Enhancement Controls Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Card Enhancements", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Compact Single-Row Switches: Text Sharpness / Darken & Border
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Switch 1: Text Sharpness / Darken
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Text Sharpness / Darken",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Switch(
                                checked = uiState.isTextDarkened,
                                onCheckedChange = { onToggleTextDarken() },
                                colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary),
                                modifier = Modifier.testTag("switch_text_darken")
                            )
                        }

                        // Switch 2: Border
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Border",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Switch(
                                checked = uiState.hasPerimeterBorder,
                                onCheckedChange = { onToggleBorder() },
                                colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary),
                                modifier = Modifier.testTag("switch_perimeter_border")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Brightness slider
                    Text("Brightness: ${uiState.brightness.toInt()}", fontSize = 12.sp)
                    Slider(
                        value = uiState.brightness,
                        onValueChange = onBrightnessChanged,
                        valueRange = -50f..50f,
                        colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary),
                        modifier = Modifier.testTag("slider_brightness")
                    )

                    // Contrast slider
                    Text("Contrast: ${uiState.contrast.toInt()}", fontSize = 12.sp)
                    Slider(
                        value = uiState.contrast,
                        onValueChange = onContrastChanged,
                        valueRange = -50f..50f,
                        colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary),
                        modifier = Modifier.testTag("slider_contrast")
                    )
                }
            }
        }

        // Copies & Add to Queue Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val isCustom = uiState.selectedTemplate == PvcCardTemplate.CUSTOM
                    Text(
                        if (isCustom) "Card Copies & Label" else "Card Copies",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Conditional "Card Label" field: ONLY for Custom / Scanned Card mode
                    if (isCustom) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.cardName,
                            onValueChange = onNameChanged,
                            label = { Text("Card Label") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_card_name")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Number of Copies: permanently visible across all card types
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Number of Copies:", fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onCopiesChanged(uiState.cardCopies - 1) },
                                modifier = Modifier.testTag("btn_copies_minus")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                "${uiState.cardCopies}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { onCopiesChanged(uiState.cardCopies + 1) },
                                modifier = Modifier.testTag("btn_copies_plus")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddToQueue,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_add_queue"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add to Queue", fontSize = 13.sp)
                        }

                        Button(
                            onClick = onGoToSheet,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_go_to_sheet"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Print Sheet Layout", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
