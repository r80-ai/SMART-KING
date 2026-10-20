package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pdf.PdfEngine
import com.example.photo.ImageLayerItem
import com.example.photo.TextItem

/**
 * Universal model for layer items in the Layer Manager.
 */
data class LayerEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val isText: Boolean,
    val thumbnailBitmap: Bitmap? = null,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f // 0f .. 1f
)

/**
 * Dedicated, full-featured Layer Manager Dialog supporting both Text & Image layers.
 * Provides:
 * - Visibility Toggle (Eye icon)
 * - Lock/Unlock toggle
 * - Opacity Slider (0-100%)
 * - Z-Index reordering (Up / Down buttons)
 * - Selection & deletion
 */
@Composable
fun FullLayerManagerDialog(
    layers: List<LayerEntry>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onChangeOpacity: (String, Float) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var expandedOpacityId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Layers",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Layer Manager",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${layers.size} layer${if (layers.size == 1) "" else "s"} (Top to Bottom)",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (layers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No layers added yet",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Add text or images to see them here",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display layers with index: index 0 is top layer (rendered last)
                        itemsIndexed(layers, key = { _, l -> l.id }) { index, layer ->
                            val isSelected = layer.id == selectedId
                            val isOpacityExpanded = expandedOpacityId == layer.id

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF334155) else Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF334155),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelect(layer.id) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Left: Thumbnail/Icon & Name
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Thumbnail / Type icon
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF0F172A)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (layer.thumbnailBitmap != null && !layer.thumbnailBitmap.isRecycled) {
                                                    Image(
                                                        bitmap = layer.thumbnailBitmap.asImageBitmap(),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = if (layer.isText) Icons.Default.TextFields else Icons.Default.Image,
                                                        contentDescription = null,
                                                        tint = if (layer.isText) Color(0xFF60A5FA) else Color(0xFF34D399),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Layer Name
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = layer.title,
                                                    color = if (layer.isVisible) Color.White else Color(0xFF64748B),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${layer.subtitle} • ${(layer.opacity * 100).toInt()}%",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        // Right: Actions: Visibility, Lock, Reorder Up/Down
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Opacity button toggle
                                            IconButton(
                                                onClick = {
                                                    expandedOpacityId = if (isOpacityExpanded) null else layer.id
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Opacity,
                                                    contentDescription = "Opacity",
                                                    tint = if (isOpacityExpanded) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Visibility Toggle
                                            IconButton(
                                                onClick = { onToggleVisibility(layer.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Visibility",
                                                    tint = if (layer.isVisible) Color(0xFF60A5FA) else Color(0xFF64748B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Lock Toggle
                                            IconButton(
                                                onClick = { onToggleLock(layer.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = "Lock",
                                                    tint = if (layer.isLocked) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Move Up (towards front)
                                            IconButton(
                                                onClick = { onMoveUp(index) },
                                                enabled = index > 0,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Move Up",
                                                    tint = if (index > 0) Color.White else Color(0xFF475569),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Move Down (towards back)
                                            IconButton(
                                                onClick = { onMoveDown(index) },
                                                enabled = index < layers.size - 1,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Move Down",
                                                    tint = if (index < layers.size - 1) Color.White else Color(0xFF475569),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Delete
                                            IconButton(
                                                onClick = { onDelete(layer.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Layer",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Expandable Opacity Slider
                                    AnimatedVisibility(visible = isOpacityExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Layer Opacity",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "${(layer.opacity * 100).toInt()}%",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Slider(
                                                value = layer.opacity,
                                                onValueChange = { onChangeOpacity(layer.id, it) },
                                                valueRange = 0.05f..1f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFF3B82F6),
                                                    activeTrackColor = Color(0xFF3B82F6),
                                                    inactiveTrackColor = Color(0xFF334155)
                                                ),
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
