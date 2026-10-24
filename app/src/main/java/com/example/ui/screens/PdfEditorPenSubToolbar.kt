package com.example.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPrimary

enum class PenSubCategory {
    PEN,
    HIGHLIGHT,
    SHAPES,
    ERASER
}

/**
 * Dedicated Sub-Toolbar / Bottom-Sheet Control Panel for Pen Annotations:
 * 1. Freehand Pen (Smooth drawing, brush size, color picker, opacity)
 * 2. Highlight (Semi-transparent freehand marker, adjustable width and opacity)
 * 3. Shapes (Manual drag-to-create: Rectangle, Circle/Ellipse, Line, Arrow)
 * 4. Dedicated Eraser (Geometry-based annotation eraser, adjustable sizes: Small/Medium/Large, and Clear option)
 */
@Composable
fun PdfEditorPenSubToolbar(
    activeCategory: PenSubCategory,
    onCategoryChange: (PenSubCategory) -> Unit,
    // Pen options
    penColor: Int,
    onPenColorChange: (Int) -> Unit,
    penStrokeWidth: Float,
    onPenStrokeWidthChange: (Float) -> Unit,
    penOpacity: Float,
    onPenOpacityChange: (Float) -> Unit,
    // Highlighter options
    highlighterColor: Int,
    onHighlighterColorChange: (Int) -> Unit,
    highlighterWidth: Float,
    onHighlighterWidthChange: (Float) -> Unit,
    highlighterOpacity: Float,
    onHighlighterOpacityChange: (Float) -> Unit,
    // Shapes options
    selectedShapeTool: AnnotationTool,
    onShapeToolChange: (AnnotationTool) -> Unit,
    shapeFillColor: Int?,
    onShapeFillColorChange: (Int?) -> Unit,
    // Eraser options
    eraserSize: EraserSizePreset,
    onEraserSizeChange: (EraserSizePreset) -> Unit,
    onClearPage: () -> Unit,
    // History & Actions
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val penColors = listOf(
        AndroidColor.rgb(37, 99, 235),  // Royal Blue
        AndroidColor.rgb(220, 38, 38),  // Crimson Red
        AndroidColor.rgb(22, 163, 74),  // Emerald Green
        AndroidColor.rgb(234, 88, 12),  // Vivid Orange
        AndroidColor.rgb(147, 51, 234), // Rich Purple
        AndroidColor.rgb(202, 138, 4),  // Amber Yellow
        AndroidColor.BLACK,
        AndroidColor.WHITE
    )

    val highlighterColors = listOf(
        AndroidColor.rgb(250, 204, 21), // Fluorescent Yellow
        AndroidColor.rgb(74, 222, 128), // Neon Green
        AndroidColor.rgb(56, 189, 248), // Sky Blue
        AndroidColor.rgb(244, 114, 182),// Soft Pink
        AndroidColor.rgb(251, 146, 60), // Light Orange
        AndroidColor.rgb(192, 132, 252) // Lavender
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("pen_subtoolbar_panel")
    ) {
        // TOP ROW: Primary Sub-Tool Selector Tabs + Quick Action Buttons (Undo, Redo, Close)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 4 Sub-Tools: Pen | Highlight | Shapes | Eraser
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubToolTabItem(
                    label = "Pen",
                    icon = Icons.Default.BorderColor,
                    isSelected = activeCategory == PenSubCategory.PEN,
                    onClick = { onCategoryChange(PenSubCategory.PEN) },
                    testTag = "subtool_tab_pen"
                )
                SubToolTabItem(
                    label = "Highlight",
                    icon = Icons.Default.Highlight,
                    isSelected = activeCategory == PenSubCategory.HIGHLIGHT,
                    onClick = { onCategoryChange(PenSubCategory.HIGHLIGHT) },
                    testTag = "subtool_tab_highlight"
                )
                SubToolTabItem(
                    label = "Shapes",
                    icon = Icons.Default.Category,
                    isSelected = activeCategory == PenSubCategory.SHAPES,
                    onClick = { onCategoryChange(PenSubCategory.SHAPES) },
                    testTag = "subtool_tab_shapes"
                )
                SubToolTabItem(
                    label = "Eraser",
                    icon = Icons.Default.AutoFixNormal,
                    isSelected = activeCategory == PenSubCategory.ERASER,
                    onClick = { onCategoryChange(PenSubCategory.ERASER) },
                    testTag = "subtool_tab_eraser"
                )
            }

            // Right side: Undo, Redo, Done
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(32.dp).testTag("pen_btn_undo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(32.dp).testTag("pen_btn_redo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("pen_btn_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Done",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // DYNAMIC CONTROL BODY BASED ON ACTIVE SUB-TOOL
        when (activeCategory) {
            // ====================================================
            // 1. FREEHAND PEN: Brush size, Color, Opacity
            // ====================================================
            PenSubCategory.PEN -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Brush Size & Opacity Slider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brush Size Slider (1dp - 30dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Size", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("${penStrokeWidth.toInt()} dp", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = penStrokeWidth,
                                onValueChange = onPenStrokeWidthChange,
                                valueRange = 1f..30f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(penColor),
                                    activeTrackColor = Color(penColor),
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        // Opacity Slider (10% - 100%)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Opacity", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("${(penOpacity * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = penOpacity,
                                onValueChange = onPenOpacityChange,
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = CrimsonPrimary,
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Color Palette Swatches
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        penColors.forEach { c ->
                            ColorSwatch(
                                colorHex = c,
                                isSelected = penColor == c,
                                onClick = { onPenColorChange(c) }
                            )
                        }
                    }
                }
            }

            // ====================================================
            // 2. HIGHLIGHT: Marker width, Fluorescent colors, Opacity
            // ====================================================
            PenSubCategory.HIGHLIGHT -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Marker Width Slider (10dp - 40dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Marker Width", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("${highlighterWidth.toInt()} dp", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = highlighterWidth,
                                onValueChange = onHighlighterWidthChange,
                                valueRange = 10f..40f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(highlighterColor),
                                    activeTrackColor = Color(highlighterColor),
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        // Opacity Slider (15% - 70%)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Transparency", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("${(highlighterOpacity * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = highlighterOpacity,
                                onValueChange = onHighlighterOpacityChange,
                                valueRange = 0.15f..0.70f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = CrimsonPrimary,
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Highlighter Colors Swatches
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        highlighterColors.forEach { c ->
                            ColorSwatch(
                                colorHex = c,
                                isSelected = highlighterColor == c,
                                onClick = { onHighlighterColorChange(c) }
                            )
                        }
                    }
                }
            }

            // ====================================================
            // 3. SHAPES: Rectangle, Circle/Ellipse, Line, Arrow
            // ====================================================
            PenSubCategory.SHAPES -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Shapes Selection Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShapeSelectorChip(
                            label = "Rectangle",
                            icon = Icons.Default.CropSquare,
                            isSelected = selectedShapeTool == AnnotationTool.RECTANGLE,
                            onClick = { onShapeToolChange(AnnotationTool.RECTANGLE) }
                        )
                        ShapeSelectorChip(
                            label = "Circle",
                            icon = Icons.Default.Circle,
                            isSelected = selectedShapeTool == AnnotationTool.CIRCLE,
                            onClick = { onShapeToolChange(AnnotationTool.CIRCLE) }
                        )
                        ShapeSelectorChip(
                            label = "Line",
                            icon = Icons.Default.HorizontalRule,
                            isSelected = selectedShapeTool == AnnotationTool.LINE,
                            onClick = { onShapeToolChange(AnnotationTool.LINE) }
                        )
                        ShapeSelectorChip(
                            label = "Arrow",
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            isSelected = selectedShapeTool == AnnotationTool.ARROW,
                            onClick = { onShapeToolChange(AnnotationTool.ARROW) }
                        )

                        // Fill Toggle for Rectangle / Circle
                        if (selectedShapeTool == AnnotationTool.RECTANGLE || selectedShapeTool == AnnotationTool.CIRCLE) {
                            val isFilled = shapeFillColor != null
                            FilterChip(
                                selected = isFilled,
                                onClick = {
                                    if (isFilled) onShapeFillColorChange(null) else onShapeFillColorChange(penColor)
                                },
                                label = { Text(if (isFilled) "Filled" else "Outline Only", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        if (isFilled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stroke Width and Color Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Stroke Width", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("${penStrokeWidth.toInt()} dp", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = penStrokeWidth,
                                onValueChange = onPenStrokeWidthChange,
                                valueRange = 1f..20f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(penColor),
                                    activeTrackColor = Color(penColor),
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        // Compact Color swatches
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            penColors.take(5).forEach { c ->
                                ColorSwatch(
                                    colorHex = c,
                                    isSelected = penColor == c,
                                    sizeDp = 22,
                                    onClick = {
                                        onPenColorChange(c)
                                        if (shapeFillColor != null) onShapeFillColorChange(c)
                                    }
                                )
                            }
                        }
                    }

                    // Gesture Hint
                    Text(
                        text = "Drag finger on page from start to end to create shape",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // ====================================================
            // 4. DEDICATED ERASER: Small, Medium, Large, Clear Page
            // ====================================================
            PenSubCategory.ERASER -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Eraser Sizes: Small | Medium | Large
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Eraser Size:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            EraserSizeChip(
                                label = "Small",
                                isSelected = eraserSize == EraserSizePreset.SMALL,
                                onClick = { onEraserSizeChange(EraserSizePreset.SMALL) }
                            )
                            EraserSizeChip(
                                label = "Medium",
                                isSelected = eraserSize == EraserSizePreset.MEDIUM,
                                onClick = { onEraserSizeChange(EraserSizePreset.MEDIUM) }
                            )
                            EraserSizeChip(
                                label = "Large",
                                isSelected = eraserSize == EraserSizePreset.LARGE,
                                onClick = { onEraserSizeChange(EraserSizePreset.LARGE) }
                            )
                        }

                        // Clear All Page Annotations Button
                        OutlinedButton(
                            onClick = onClearPage,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF87171)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Page", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Integrity Guarantee Banner
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Geometry-based eraser: Removes annotation strokes without altering original PDF text or graphics",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubToolTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (isSelected) CrimsonPrimary else Color(0xFF0F172A),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CrimsonPrimary else Color(0xFF334155)
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ShapeSelectorChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CrimsonPrimary,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White,
            containerColor = Color(0xFF0F172A),
            labelColor = Color(0xFF94A3B8),
            iconColor = Color(0xFF94A3B8)
        ),
        modifier = Modifier.height(30.dp)
    )
}

@Composable
private fun EraserSizeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) CrimsonPrimary else Color(0xFF0F172A),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CrimsonPrimary else Color(0xFF334155)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ColorSwatch(
    colorHex: Int,
    isSelected: Boolean,
    sizeDp: Int = 24,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Color(colorHex))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) Color.White else Color(0xFF475569),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
