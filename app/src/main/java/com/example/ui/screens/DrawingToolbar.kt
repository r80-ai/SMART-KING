package com.example.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photo.DrawingViewModel
import com.example.ui.theme.CrimsonPrimary
import kotlin.math.roundToInt

private val ToolBlue = Color(0xFF2563EB)
private val DarkSurfaceBg = Color(0xFF0F172A)
private val DarkCardBg = Color(0xFF1E293B)

private val PenColorPalette = listOf(
    AndroidColor.rgb(37, 99, 235),  // Royal Blue
    AndroidColor.rgb(220, 38, 38),  // Crimson Red
    AndroidColor.rgb(22, 163, 74),  // Emerald Green
    AndroidColor.rgb(234, 88, 12),  // Vivid Orange
    AndroidColor.rgb(147, 51, 234), // Rich Purple
    AndroidColor.rgb(202, 138, 4),  // Amber Yellow
    AndroidColor.BLACK,
    AndroidColor.WHITE
)

private val HighlighterColorPalette = listOf(
    AndroidColor.rgb(250, 204, 21), // Fluorescent Yellow
    AndroidColor.rgb(74, 222, 128), // Neon Green
    AndroidColor.rgb(56, 189, 248), // Sky Blue
    AndroidColor.rgb(244, 114, 182),// Soft Pink
    AndroidColor.rgb(251, 146, 60), // Light Orange
    AndroidColor.rgb(192, 132, 252) // Lavender
)

private val ShapeFillPalette = listOf(
    null,                           // Transparent / Outline Only
    AndroidColor.WHITE,
    AndroidColor.rgb(220, 38, 38),  // Crimson Red
    AndroidColor.rgb(37, 99, 235),  // Royal Blue
    AndroidColor.rgb(22, 163, 74),  // Emerald Green
    AndroidColor.rgb(250, 204, 21), // Amber Yellow
    AndroidColor.BLACK
)

/**
 * Fixed bottom control toolbar for Photo Editor Drawing, ported directly from PDF Edit -> Pen.
 * Layout is fixed and resilient to prevent collapsing or jumping during canvas gestures.
 */
@Composable
fun DrawingToolbar(
    viewModel: DrawingViewModel,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceBg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("photo_drawing_toolbar")
    ) {
        // TOP CONTROL HEADER: Title, Undo, Redo, Clear, Cancel, Apply
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pen & Drawing",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Undo
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = viewModel.canUndo,
                    modifier = Modifier.size(28.dp).testTag("drawing_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (viewModel.canUndo) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = viewModel.canRedo,
                    modifier = Modifier.size(28.dp).testTag("drawing_redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (viewModel.canRedo) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear All
                TextButton(
                    onClick = { viewModel.clearAll() },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("drawing_clear_button")
                ) {
                    Text("Clear", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Cancel
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(28.dp).testTag("drawing_cancel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Apply
                IconButton(
                    onClick = onApply,
                    modifier = Modifier.size(28.dp).testTag("drawing_apply_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // SUBTOOL SWITCHER TABS: Select, Pen, Highlight, Shapes, Icons, Emoji, Eraser
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = viewModel.activeSubTool == AnnotationCanvasOverlayView.ToolMode.SELECT,
                onClick = { viewModel.activeSubTool = AnnotationCanvasOverlayView.ToolMode.SELECT },
                label = { Text("Select", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_select")
            )
            FilterChip(
                selected = viewModel.activeSubTool == AnnotationCanvasOverlayView.ToolMode.PEN,
                onClick = { viewModel.activeSubTool = AnnotationCanvasOverlayView.ToolMode.PEN },
                label = { Text("Pen", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_pen")
            )
            FilterChip(
                selected = viewModel.activeSubTool == AnnotationCanvasOverlayView.ToolMode.HIGHLIGHT,
                onClick = { viewModel.activeSubTool = AnnotationCanvasOverlayView.ToolMode.HIGHLIGHT },
                label = { Text("Highlight", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_highlight")
            )
            FilterChip(
                selected = viewModel.activeSubTool == AnnotationCanvasOverlayView.ToolMode.SHAPE,
                onClick = { viewModel.activeSubTool = AnnotationCanvasOverlayView.ToolMode.SHAPE },
                label = { Text("Shapes", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.CropSquare, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_shapes")
            )
            FilterChip(
                selected = viewModel.activeSubTool == AnnotationCanvasOverlayView.ToolMode.ICON,
                onClick = { viewModel.activeSubTool = AnnotationCanvasOverlayView.ToolMode.ICON },
                label = { Text("Icons", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_icons")
            )
            FilterChip(
                selected = false,
                onClick = { viewModel.showEmojiPicker = true },
                label = { Text("Emoji", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Mood, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_emoji")
            )
            FilterChip(
                selected = viewModel.activeSubTool == AnnotationCanvasOverlayView.ToolMode.ERASER,
                onClick = { viewModel.activeSubTool = AnnotationCanvasOverlayView.ToolMode.ERASER },
                label = { Text("Eraser", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp)) },
                modifier = Modifier.height(30.dp).testTag("chip_tool_eraser")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // TOOL-SPECIFIC CONTROL PANELS
        when (viewModel.activeSubTool) {
            AnnotationCanvasOverlayView.ToolMode.SELECT -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (viewModel.selectedAnnotationId != null) "Object Selected (Drag to move, corner handles to scale)" else "Tap any drawing or shape to select & resize",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    if (viewModel.selectedAnnotationId != null) {
                        TextButton(
                            onClick = { viewModel.deleteSelected() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("btn_delete_selected_annotation")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnnotationCanvasOverlayView.ToolMode.PEN -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Size & Opacity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Size: ${viewModel.penStrokeThickness.roundToInt()}dp", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(62.dp))
                        Slider(
                            value = viewModel.penStrokeThickness,
                            onValueChange = { viewModel.penStrokeThickness = it },
                            valueRange = 2f..40f,
                            modifier = Modifier.weight(1f).height(24.dp).testTag("slider_pen_thickness")
                        )
                        Text("Alpha: ${(viewModel.penOpacity * 100).roundToInt()}%", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(62.dp))
                        Slider(
                            value = viewModel.penOpacity,
                            onValueChange = { viewModel.penOpacity = it },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.weight(1f).height(24.dp).testTag("slider_pen_opacity")
                        )
                    }

                    // Colors + Real-time Eyedropper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Current swatch
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(viewModel.penColor))
                                .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                        )

                        // Eyedropper button
                        FilledTonalButton(
                            onClick = {
                                viewModel.colorPickerTarget = "PEN"
                                viewModel.isColorPickerActive = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("btn_eyedropper_pen"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (viewModel.isColorPickerActive && viewModel.colorPickerTarget == "PEN") CrimsonPrimary else DarkCardBg
                            )
                        ) {
                            Icon(Icons.Default.Colorize, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eyedropper", fontSize = 10.5.sp, color = Color.White)
                        }

                        // Preset palette chips
                        Row(
                            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (c in PenColorPalette) {
                                ColorDot(
                                    color = c,
                                    isSelected = viewModel.penColor == c,
                                    onClick = { viewModel.penColor = c }
                                )
                            }
                        }
                    }
                }
            }

            AnnotationCanvasOverlayView.ToolMode.HIGHLIGHT -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Width: ${viewModel.highlightWidth.roundToInt()}dp", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(65.dp))
                        Slider(
                            value = viewModel.highlightWidth,
                            onValueChange = { viewModel.highlightWidth = it },
                            valueRange = 10f..60f,
                            modifier = Modifier.weight(1f).height(24.dp).testTag("slider_highlight_width")
                        )
                        Text("Alpha: ${(viewModel.highlightOpacity * 100).roundToInt()}%", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(62.dp))
                        Slider(
                            value = viewModel.highlightOpacity,
                            onValueChange = { viewModel.highlightOpacity = it },
                            valueRange = 0.1f..0.8f,
                            modifier = Modifier.weight(1f).height(24.dp).testTag("slider_highlight_opacity")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(viewModel.highlightColor))
                                .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                        )

                        FilledTonalButton(
                            onClick = {
                                viewModel.colorPickerTarget = "HIGHLIGHT"
                                viewModel.isColorPickerActive = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("btn_eyedropper_highlight"),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = DarkCardBg)
                        ) {
                            Icon(Icons.Default.Colorize, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eyedropper", fontSize = 10.5.sp, color = Color.White)
                        }

                        Row(
                            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (c in HighlighterColorPalette) {
                                ColorDot(
                                    color = c,
                                    isSelected = viewModel.highlightColor == c,
                                    onClick = { viewModel.highlightColor = c }
                                )
                            }
                        }
                    }
                }
            }

            AnnotationCanvasOverlayView.ToolMode.SHAPE -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Shape Type Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = viewModel.shapeType == AnnotationCanvasOverlayView.ShapeType.RECTANGLE,
                            onClick = { viewModel.shapeType = AnnotationCanvasOverlayView.ShapeType.RECTANGLE },
                            label = { Text("Rectangle", fontSize = 10.5.sp) },
                            modifier = Modifier.height(28.dp).testTag("chip_shape_rect")
                        )
                        FilterChip(
                            selected = viewModel.shapeType == AnnotationCanvasOverlayView.ShapeType.CIRCLE,
                            onClick = { viewModel.shapeType = AnnotationCanvasOverlayView.ShapeType.CIRCLE },
                            label = { Text("Circle / Oval", fontSize = 10.5.sp) },
                            modifier = Modifier.height(28.dp).testTag("chip_shape_circle")
                        )
                        FilterChip(
                            selected = viewModel.shapeType == AnnotationCanvasOverlayView.ShapeType.LINE,
                            onClick = { viewModel.shapeType = AnnotationCanvasOverlayView.ShapeType.LINE },
                            label = { Text("Line", fontSize = 10.5.sp) },
                            modifier = Modifier.height(28.dp).testTag("chip_shape_line")
                        )
                        FilterChip(
                            selected = viewModel.shapeType == AnnotationCanvasOverlayView.ShapeType.ARROW,
                            onClick = { viewModel.shapeType = AnnotationCanvasOverlayView.ShapeType.ARROW },
                            label = { Text("Arrow", fontSize = 10.5.sp) },
                            modifier = Modifier.height(28.dp).testTag("chip_shape_arrow")
                        )
                    }

                    // Arrow Styles (if Arrow selected)
                    if (viewModel.shapeType == AnnotationCanvasOverlayView.ShapeType.ARROW) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (style in AnnotationCanvasOverlayView.ArrowStyle.entries) {
                                FilterChip(
                                    selected = viewModel.arrowStyle == style,
                                    onClick = { viewModel.arrowStyle = style },
                                    label = { Text(style.label, fontSize = 10.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }

                    // Stroke width, color, and fill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Border: ${viewModel.shapeStrokeWidth.roundToInt()}dp", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(68.dp))
                        Slider(
                            value = viewModel.shapeStrokeWidth,
                            onValueChange = { viewModel.shapeStrokeWidth = it },
                            valueRange = 1f..24f,
                            modifier = Modifier.weight(1f).height(24.dp)
                        )

                        FilledTonalButton(
                            onClick = {
                                viewModel.colorPickerTarget = "SHAPE"
                                viewModel.isColorPickerActive = true
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = DarkCardBg)
                        ) {
                            Icon(Icons.Default.Colorize, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Picker", fontSize = 10.sp, color = Color.White)
                        }

                        // Fill Color toggle
                        FilterChip(
                            selected = viewModel.shapeFillColor != null,
                            onClick = {
                                viewModel.shapeFillColor = if (viewModel.shapeFillColor == null) viewModel.shapeColor else null
                            },
                            label = { Text(if (viewModel.shapeFillColor != null) "Filled" else "Outline", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                    }

                    // Color swatches for Shape Border & Fill
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in PenColorPalette) {
                            ColorDot(
                                color = c,
                                isSelected = viewModel.shapeColor == c,
                                onClick = {
                                    viewModel.shapeColor = c
                                    if (viewModel.shapeFillColor != null) {
                                        viewModel.shapeFillColor = c
                                    }
                                }
                            )
                        }
                    }
                }
            }

            AnnotationCanvasOverlayView.ToolMode.ICON -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val icons = listOf(
                        "TICK_CLEAN" to "✓ Clean",
                        "TICK_HAND" to "✓ Pen",
                        "CROSS_CLEAN" to "✗ Clean",
                        "CROSS_HAND" to "✗ Pen",
                        "STAR" to "★ Star",
                        "HEART" to "♥ Heart",
                        "WARNING" to "⚠ Warn",
                        "THUMBS_UP" to "👍 Thumb",
                        "SMILEY" to "😊 Smile",
                        "BADGE" to "🏷 Badge"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for ((key, label) in icons) {
                            FilterChip(
                                selected = viewModel.iconType == key,
                                onClick = { viewModel.iconType = key },
                                label = { Text(label, fontSize = 10.5.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Thickness: ${viewModel.iconStrokeWidth.roundToInt()}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(72.dp))
                        Slider(
                            value = viewModel.iconStrokeWidth,
                            onValueChange = { viewModel.iconStrokeWidth = it },
                            valueRange = 1f..16f,
                            modifier = Modifier.weight(1f).height(24.dp)
                        )

                        FilledTonalButton(
                            onClick = {
                                viewModel.colorPickerTarget = "ICON"
                                viewModel.isColorPickerActive = true
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = DarkCardBg)
                        ) {
                            Icon(Icons.Default.Colorize, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Picker", fontSize = 10.sp, color = Color.White)
                        }

                        // Colors
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (c in PenColorPalette) {
                                ColorDot(
                                    color = c,
                                    isSelected = viewModel.iconColor == c,
                                    onClick = { viewModel.iconColor = c }
                                )
                            }
                        }
                    }
                }
            }

            AnnotationCanvasOverlayView.ToolMode.ERASER -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Eraser Mode toggle
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = viewModel.eraserType == AnnotationCanvasOverlayView.EraserType.MANUAL,
                                onClick = { viewModel.eraserType = AnnotationCanvasOverlayView.EraserType.MANUAL },
                                label = { Text("CLEAR Stroke Eraser", fontSize = 10.5.sp) },
                                modifier = Modifier.height(28.dp).testTag("chip_eraser_stroke")
                            )
                            FilterChip(
                                selected = viewModel.eraserType == AnnotationCanvasOverlayView.EraserType.STROKE,
                                onClick = { viewModel.eraserType = AnnotationCanvasOverlayView.EraserType.STROKE },
                                label = { Text("Object Eraser", fontSize = 10.5.sp) },
                                modifier = Modifier.height(28.dp).testTag("chip_eraser_object")
                            )
                        }

                        // Size presets
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = viewModel.eraserSize == AnnotationCanvasOverlayView.EraserSize.SMALL,
                                onClick = {
                                    viewModel.eraserSize = AnnotationCanvasOverlayView.EraserSize.SMALL
                                    viewModel.eraserStrokeWidth = 20f
                                },
                                label = { Text("S", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                            FilterChip(
                                selected = viewModel.eraserSize == AnnotationCanvasOverlayView.EraserSize.MEDIUM,
                                onClick = {
                                    viewModel.eraserSize = AnnotationCanvasOverlayView.EraserSize.MEDIUM
                                    viewModel.eraserStrokeWidth = 44f
                                },
                                label = { Text("M", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                            FilterChip(
                                selected = viewModel.eraserSize == AnnotationCanvasOverlayView.EraserSize.LARGE,
                                onClick = {
                                    viewModel.eraserSize = AnnotationCanvasOverlayView.EraserSize.LARGE
                                    viewModel.eraserStrokeWidth = 80f
                                },
                                label = { Text("L", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Eraser Radius: ${viewModel.eraserStrokeWidth.roundToInt() / 2}dp", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = viewModel.eraserStrokeWidth,
                            onValueChange = { viewModel.eraserStrokeWidth = it },
                            valueRange = 8f..100f,
                            modifier = Modifier.weight(1f).height(24.dp).testTag("slider_eraser_radius")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorDot(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) CrimsonPrimary else Color.White.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
