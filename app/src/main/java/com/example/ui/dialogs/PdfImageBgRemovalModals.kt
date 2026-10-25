package com.example.ui.dialogs

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import kotlin.math.roundToInt

enum class ManualEraserMode {
    ERASE, RESTORE
}

/**
 * Dialog prompting user to choose between Auto AI Background Removal (BiRefNet Lite)
 * and Manual Eraser / Refine brush mode.
 */
@Composable
fun PdfImageBgRemovalChoiceDialog(
    onDismiss: () -> Unit,
    onAutoAiRemove: () -> Unit,
    onManualEraser: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("dialog_bg_removal_choice")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: Compact title and close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remove Background",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Button 1: "Auto AI Remove" (Full width, clean, compact)
                Button(
                    onClick = onAutoAiRemove,
                    colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_auto_ai_remove")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto AI Remove",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }

                // Button 2: "Manual Eraser / Refine" (Full width, clean, compact)
                Button(
                    onClick = onManualEraser,
                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_manual_eraser_refine")
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Manual Eraser / Refine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Non-blocking progress indicator dialog shown while BiRefNet Lite ONNX background removal runs.
 */
@Composable
fun PdfImageBgRemovalProgressDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularProgressIndicator(
                    color = ToolGreen,
                    modifier = Modifier.size(44.dp),
                    strokeWidth = 3.5.dp
                )
                Text(
                    text = "Removing Background...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = "Running BiRefNet Lite ONNX AI on-device with high-precision alpha matting",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Interactive canvas dialog allowing the user to manually erase or restore transparent pixels
 * on the image using touch strokes, adjustable brush sizes, and undo capability.
 */
@Composable
fun PdfImageManualEraserDialog(
    imageBitmap: Bitmap,
    originalBitmap: Bitmap,
    onApply: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var workingBitmap by remember {
        mutableStateOf(imageBitmap.copy(Bitmap.Config.ARGB_8888, true))
    }
    val undoStack = remember { mutableStateListOf<Bitmap>() }

    var mode by remember { mutableStateOf(ManualEraserMode.ERASE) }
    var brushSizeDp by remember { mutableFloatStateOf(24f) }
    var drawVersion by remember { mutableIntStateOf(0) }

    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }
    var lastTouchPos by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current

    // Prepare scaled original for seamless pixel restoration
    val scaledOriginal = remember(originalBitmap, workingBitmap.width, workingBitmap.height) {
        if (originalBitmap.width != workingBitmap.width || originalBitmap.height != workingBitmap.height) {
            Bitmap.createScaledBitmap(originalBitmap, workingBitmap.width, workingBitmap.height, true)
        } else {
            originalBitmap
        }
    }

    val restoreShader = remember(scaledOriginal) {
        BitmapShader(scaledOriginal, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("dialog_manual_image_eraser"),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Top Header: Title, Undo, Reset, Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = ToolGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Manual Eraser & Refine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Undo button
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    workingBitmap = undoStack.removeLast()
                                    drawVersion++
                                }
                            },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_manual_undo")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo stroke",
                                tint = if (undoStack.isNotEmpty()) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reset to initial cutout
                        IconButton(
                            onClick = {
                                if (undoStack.size >= 10) undoStack.removeAt(0)
                                undoStack.add(workingBitmap.copy(Bitmap.Config.ARGB_8888, true))
                                workingBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
                                drawVersion++
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_manual_reset")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset image",
                                tint = ToolOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Canvas Container with transparency checkerboard
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidthPx = constraints.maxWidth.toFloat()
                    val containerHeightPx = constraints.maxHeight.toFloat()

                    val bmpW = workingBitmap.width.toFloat()
                    val bmpH = workingBitmap.height.toFloat()

                    val fitScale = minOf(
                        containerWidthPx / bmpW.coerceAtLeast(1f),
                        containerHeightPx / bmpH.coerceAtLeast(1f)
                    )
                    val displayedW = bmpW * fitScale
                    val displayedH = bmpH * fitScale
                    val offsetX = (containerWidthPx - displayedW) / 2f
                    val offsetY = (containerHeightPx - displayedH) / 2f

                    val brushRadiusBmp = (brushSizeDp * density.density / fitScale).coerceAtLeast(1f)

                    fun screenToBitmap(pos: Offset): Offset {
                        val bx = ((pos.x - offsetX) / fitScale).coerceIn(0f, bmpW)
                        val by = ((pos.y - offsetY) / fitScale).coerceIn(0f, bmpH)
                        return Offset(bx, by)
                    }

                    fun applyPoint(bx: Float, by: Float) {
                        val canvas = AndroidCanvas(workingBitmap)
                        if (mode == ManualEraserMode.ERASE) {
                            val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                                style = AndroidPaint.Style.FILL
                            }
                            canvas.drawCircle(bx, by, brushRadiusBmp, p)
                        } else {
                            val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                shader = restoreShader
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
                                style = AndroidPaint.Style.FILL
                            }
                            canvas.drawCircle(bx, by, brushRadiusBmp, p)
                        }
                    }

                    fun applyLine(x1: Float, y1: Float, x2: Float, y2: Float) {
                        val canvas = AndroidCanvas(workingBitmap)
                        if (mode == ManualEraserMode.ERASE) {
                            val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                                style = AndroidPaint.Style.STROKE
                                strokeCap = AndroidPaint.Cap.ROUND
                                strokeJoin = AndroidPaint.Join.ROUND
                                strokeWidth = brushRadiusBmp * 2f
                            }
                            canvas.drawLine(x1, y1, x2, y2, p)
                        } else {
                            val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                shader = restoreShader
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
                                style = AndroidPaint.Style.STROKE
                                strokeCap = AndroidPaint.Cap.ROUND
                                strokeJoin = AndroidPaint.Join.ROUND
                                strokeWidth = brushRadiusBmp * 2f
                            }
                            canvas.drawLine(x1, y1, x2, y2, p)
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(mode, brushSizeDp, workingBitmap) {
                                detectDragGestures(
                                    onDragStart = { startPos ->
                                        if (undoStack.size >= 10) undoStack.removeAt(0)
                                        undoStack.add(workingBitmap.copy(Bitmap.Config.ARGB_8888, true))

                                        currentTouchPos = startPos
                                        lastTouchPos = startPos

                                        val pt = screenToBitmap(startPos)
                                        applyPoint(pt.x, pt.y)
                                        drawVersion++
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val curPos = change.position
                                        currentTouchPos = curPos
                                        val prevPos = lastTouchPos ?: curPos
                                        lastTouchPos = curPos

                                        val p1 = screenToBitmap(prevPos)
                                        val p2 = screenToBitmap(curPos)
                                        applyLine(p1.x, p1.y, p2.x, p2.y)
                                        drawVersion++
                                    },
                                    onDragEnd = {
                                        currentTouchPos = null
                                        lastTouchPos = null
                                        drawVersion++
                                    },
                                    onDragCancel = {
                                        currentTouchPos = null
                                        lastTouchPos = null
                                        drawVersion++
                                    }
                                )
                            }
                    ) {
                        // 1. Transparency checkerboard pattern
                        drawCheckerboardPattern(gridSizePx = 24f)

                        // 2. Read drawVersion state to trigger redraw on strokes
                        @Suppress("UNUSED_VARIABLE")
                        val v = drawVersion

                        // 3. Draw image inside fitted rect
                        drawImage(
                            image = workingBitmap.asImageBitmap(),
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(workingBitmap.width, workingBitmap.height),
                            dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                            dstSize = IntSize(displayedW.roundToInt(), displayedH.roundToInt())
                        )

                        // 4. Brush cursor ring indicator while touching
                        currentTouchPos?.let { pos ->
                            drawCircle(
                                color = if (mode == ManualEraserMode.ERASE) Color(0xFFEF4444) else ToolGreen,
                                radius = brushSizeDp * density.density,
                                center = pos,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Controls: Mode Selector & Brush Size Slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mode Switcher: Erase vs Restore
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = mode == ManualEraserMode.ERASE,
                                onClick = { mode = ManualEraserMode.ERASE },
                                label = { Text("Erase", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("btn_manual_mode_erase")
                            )

                            FilterChip(
                                selected = mode == ManualEraserMode.RESTORE,
                                onClick = { mode = ManualEraserMode.RESTORE },
                                label = { Text("Restore", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ToolGreen,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("btn_manual_mode_restore")
                            )
                        }

                        // Brush radius visual indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Size: ${brushSizeDp.toInt()}px",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((brushSizeDp * 0.25f).coerceIn(4f, 20f).dp)
                                        .clip(CircleShape)
                                        .background(if (mode == ManualEraserMode.ERASE) CrimsonPrimary else ToolGreen)
                                )
                            }
                        }
                    }

                    // Brush Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Slider(
                            value = brushSizeDp,
                            onValueChange = { brushSizeDp = it },
                            valueRange = 8f..80f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("slider_eraser_brush_size"),
                            colors = SliderDefaults.colors(
                                thumbColor = if (mode == ManualEraserMode.ERASE) CrimsonPrimary else ToolGreen,
                                activeTrackColor = if (mode == ManualEraserMode.ERASE) CrimsonPrimary else ToolGreen
                            )
                        )
                    }

                    // Action Buttons: Cancel and Apply
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF475569))
                        ) {
                            Text("Cancel", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onApply(workingBitmap.copy(Bitmap.Config.ARGB_8888, true))
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_apply_manual_eraser"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply to PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a two-tone checkerboard pattern indicating alpha transparency.
 */
private fun DrawScope.drawCheckerboardPattern(gridSizePx: Float = 24f) {
    val cols = (size.width / gridSizePx).toInt() + 1
    val rows = (size.height / gridSizePx).toInt() + 1
    val c1 = Color(0xFF263238)
    val c2 = Color(0xFF1E262B)
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val color = if ((r + c) % 2 == 0) c1 else c2
            drawRect(
                color = color,
                topLeft = Offset(c * gridSizePx, r * gridSizePx),
                size = Size(gridSizePx, gridSizePx)
            )
        }
    }
}
