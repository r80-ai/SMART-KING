package com.example.ui.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.hypot

enum class ImageCropHandle {
    NONE,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    LEFT_EDGE, RIGHT_EDGE, TOP_EDGE, BOTTOM_EDGE,
    BODY
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    ORIGINAL("Original", -1f),
    SQUARE("1:1", 1f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_3_2("3:2", 3f / 2f)
}

@Composable
fun ImageCropDialog(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onApplyCrop: (croppedBitmap: Bitmap, cropLeftPct: Float, cropTopPct: Float, cropRightPct: Float, cropBottomPct: Float) -> Unit
) {
    // Normalized coordinates [0f..1f] within the source bitmap
    var cropLeft by remember { mutableFloatStateOf(0f) }
    var cropTop by remember { mutableFloatStateOf(0f) }
    var cropRight by remember { mutableFloatStateOf(1f) }
    var cropBottom by remember { mutableFloatStateOf(1f) }
    var activeHandle by remember { mutableStateOf(ImageCropHandle.NONE) }
    var selectedAspect by remember { mutableStateOf(CropAspectRatio.FREE) }

    fun applyAspect(aspect: CropAspectRatio) {
        selectedAspect = aspect
        val targetRatio = when (aspect) {
            CropAspectRatio.FREE -> return
            CropAspectRatio.ORIGINAL -> sourceBitmap.width.toFloat() / sourceBitmap.height.toFloat()
            else -> aspect.ratio ?: 1f
        }
        val imgRatio = sourceBitmap.width.toFloat() / sourceBitmap.height.toFloat()

        if (targetRatio > imgRatio) {
            // Target is wider than image: fit full width, crop height
            val normH = (imgRatio / targetRatio).coerceIn(0.1f, 1f)
            cropLeft = 0f
            cropRight = 1f
            cropTop = (1f - normH) / 2f
            cropBottom = cropTop + normH
        } else {
            // Target is taller: fit full height, crop width
            val normW = (targetRatio / imgRatio).coerceIn(0.1f, 1f)
            cropTop = 0f
            cropBottom = 1f
            cropLeft = (1f - normW) / 2f
            cropRight = cropLeft + normW
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("crop_dialog_close")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Crop Image",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${sourceBitmap.width} × ${sourceBitmap.height} px",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Reset button
                    OutlinedButton(
                        onClick = {
                            cropLeft = 0f
                            cropTop = 0f
                            cropRight = 1f
                            cropBottom = 1f
                            selectedAspect = CropAspectRatio.FREE
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("crop_dialog_reset")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 12.sp)
                    }
                }

                // 2. Aspect Ratio Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CropAspectRatio.values().forEach { aspect ->
                        val isSelected = selectedAspect == aspect
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B))
                                .clickable { applyAspect(aspect) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = aspect.label,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // 3. Central Interactive Cropping Viewport
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val containerW = constraints.maxWidth.toFloat()
                    val containerH = constraints.maxHeight.toFloat()
                    val bmpRatio = sourceBitmap.width.toFloat() / sourceBitmap.height.toFloat().coerceAtLeast(1f)

                    val (fitW, fitH) = if (containerW / containerH > bmpRatio) {
                        Pair(containerH * bmpRatio, containerH)
                    } else {
                        Pair(containerW, containerW / bmpRatio)
                    }

                    val imgLeft = (containerW - fitW) / 2f
                    val imgTop = (containerH - fitH) / 2f

                    val rectLeft = imgLeft + cropLeft * fitW
                    val rectTop = imgTop + cropTop * fitH
                    val rectRight = imgLeft + cropRight * fitW
                    val rectBottom = imgTop + cropBottom * fitH

                    val touchDist = with(density) { 32.dp.toPx() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(fitW, fitH, imgLeft, imgTop, cropLeft, cropRight, cropTop, cropBottom) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val pos = down.position

                                    val dTL = hypot((pos.x - rectLeft).toDouble(), (pos.y - rectTop).toDouble()).toFloat()
                                    val dTR = hypot((pos.x - rectRight).toDouble(), (pos.y - rectTop).toDouble()).toFloat()
                                    val dBL = hypot((pos.x - rectLeft).toDouble(), (pos.y - rectBottom).toDouble()).toFloat()
                                    val dBR = hypot((pos.x - rectRight).toDouble(), (pos.y - rectBottom).toDouble()).toFloat()

                                    activeHandle = when {
                                        dTL <= touchDist -> ImageCropHandle.TOP_LEFT
                                        dTR <= touchDist -> ImageCropHandle.TOP_RIGHT
                                        dBL <= touchDist -> ImageCropHandle.BOTTOM_LEFT
                                        dBR <= touchDist -> ImageCropHandle.BOTTOM_RIGHT
                                        abs(pos.x - rectLeft) <= touchDist && pos.y in rectTop..rectBottom -> ImageCropHandle.LEFT_EDGE
                                        abs(pos.x - rectRight) <= touchDist && pos.y in rectTop..rectBottom -> ImageCropHandle.RIGHT_EDGE
                                        abs(pos.y - rectTop) <= touchDist && pos.x in rectLeft..rectRight -> ImageCropHandle.TOP_EDGE
                                        abs(pos.y - rectBottom) <= touchDist && pos.x in rectLeft..rectRight -> ImageCropHandle.BOTTOM_EDGE
                                        pos.x in rectLeft..rectRight && pos.y in rectTop..rectBottom -> ImageCropHandle.BODY
                                        else -> ImageCropHandle.NONE
                                    }

                                    if (activeHandle != ImageCropHandle.NONE) {
                                        down.consume()
                                    }

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break

                                        val d = change.positionChange()
                                        if (d.x != 0f || d.y != 0f) {
                                            change.consume()
                                            val dxNorm = d.x / fitW
                                            val dyNorm = d.y / fitH
                                            val minSize = 0.05f

                                            when (activeHandle) {
                                                ImageCropHandle.TOP_LEFT -> {
                                                    cropLeft = (cropLeft + dxNorm).coerceIn(0f, cropRight - minSize)
                                                    cropTop = (cropTop + dyNorm).coerceIn(0f, cropBottom - minSize)
                                                }
                                                ImageCropHandle.TOP_RIGHT -> {
                                                    cropRight = (cropRight + dxNorm).coerceIn(cropLeft + minSize, 1f)
                                                    cropTop = (cropTop + dyNorm).coerceIn(0f, cropBottom - minSize)
                                                }
                                                ImageCropHandle.BOTTOM_LEFT -> {
                                                    cropLeft = (cropLeft + dxNorm).coerceIn(0f, cropRight - minSize)
                                                    cropBottom = (cropBottom + dyNorm).coerceIn(cropTop + minSize, 1f)
                                                }
                                                ImageCropHandle.BOTTOM_RIGHT -> {
                                                    cropRight = (cropRight + dxNorm).coerceIn(cropLeft + minSize, 1f)
                                                    cropBottom = (cropBottom + dyNorm).coerceIn(cropTop + minSize, 1f)
                                                }
                                                ImageCropHandle.LEFT_EDGE -> {
                                                    cropLeft = (cropLeft + dxNorm).coerceIn(0f, cropRight - minSize)
                                                }
                                                ImageCropHandle.RIGHT_EDGE -> {
                                                    cropRight = (cropRight + dxNorm).coerceIn(cropLeft + minSize, 1f)
                                                }
                                                ImageCropHandle.TOP_EDGE -> {
                                                    cropTop = (cropTop + dyNorm).coerceIn(0f, cropBottom - minSize)
                                                }
                                                ImageCropHandle.BOTTOM_EDGE -> {
                                                    cropBottom = (cropBottom + dyNorm).coerceIn(cropTop + minSize, 1f)
                                                }
                                                ImageCropHandle.BODY -> {
                                                    val w = cropRight - cropLeft
                                                    val h = cropBottom - cropTop
                                                    val newL = (cropLeft + dxNorm).coerceIn(0f, 1f - w)
                                                    val newT = (cropTop + dyNorm).coerceIn(0f, 1f - h)
                                                    cropLeft = newL
                                                    cropRight = newL + w
                                                    cropTop = newT
                                                    cropBottom = newT + h
                                                }
                                                ImageCropHandle.NONE -> {}
                                            }
                                        }
                                    }
                                    activeHandle = ImageCropHandle.NONE
                                }
                            }
                    ) {
                        // 1. Draw original image fitted in viewport
                        drawImage(
                            image = sourceBitmap.asImageBitmap(),
                            dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt()),
                            dstSize = IntSize(fitW.toInt(), fitH.toInt()),
                            filterQuality = FilterQuality.High
                        )

                        // 2. Dark scrim over areas outside the crop box
                        val scrimColor = Color.Black.copy(alpha = 0.65f)
                        // Top scrim
                        drawRect(scrimColor, topLeft = Offset(imgLeft, imgTop), size = Size(fitW, (rectTop - imgTop).coerceAtLeast(0f)))
                        // Bottom scrim
                        drawRect(scrimColor, topLeft = Offset(imgLeft, rectBottom), size = Size(fitW, (imgTop + fitH - rectBottom).coerceAtLeast(0f)))
                        // Left scrim
                        drawRect(scrimColor, topLeft = Offset(imgLeft, rectTop), size = Size((rectLeft - imgLeft).coerceAtLeast(0f), rectBottom - rectTop))
                        // Right scrim
                        drawRect(scrimColor, topLeft = Offset(rectRight, rectTop), size = Size((imgLeft + fitW - rectRight).coerceAtLeast(0f), rectBottom - rectTop))

                        // 3. Crop outline
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(rectLeft, rectTop),
                            size = Size(rectRight - rectLeft, rectBottom - rectTop),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 4. Rule-of-thirds grid lines inside crop box
                        val rw = rectRight - rectLeft
                        val rh = rectBottom - rectTop
                        val gridColor = Color.White.copy(alpha = 0.35f)
                        val gridWidth = 1.dp.toPx()

                        // Vertical grid lines
                        drawLine(gridColor, Offset(rectLeft + rw / 3f, rectTop), Offset(rectLeft + rw / 3f, rectBottom), strokeWidth = gridWidth)
                        drawLine(gridColor, Offset(rectLeft + rw * 2f / 3f, rectTop), Offset(rectLeft + rw * 2f / 3f, rectBottom), strokeWidth = gridWidth)
                        // Horizontal grid lines
                        drawLine(gridColor, Offset(rectLeft, rectTop + rh / 3f), Offset(rectRight, rectTop + rh / 3f), strokeWidth = gridWidth)
                        drawLine(gridColor, Offset(rectLeft, rectTop + rh * 2f / 3f), Offset(rectRight, rectTop + rh * 2f / 3f), strokeWidth = gridWidth)

                        // 5. Four L-shaped corner brackets
                        val bracketLen = 22.dp.toPx().coerceAtMost(rw / 3f).coerceAtMost(rh / 3f)
                        val bracketStroke = 3.5.dp.toPx()
                        val bracketColor = Color.White

                        // Top-Left
                        val pathTL = Path().apply {
                            moveTo(rectLeft, rectTop + bracketLen)
                            lineTo(rectLeft, rectTop)
                            lineTo(rectLeft + bracketLen, rectTop)
                        }
                        drawPath(pathTL, bracketColor, style = Stroke(width = bracketStroke))

                        // Top-Right
                        val pathTR = Path().apply {
                            moveTo(rectRight - bracketLen, rectTop)
                            lineTo(rectRight, rectTop)
                            lineTo(rectRight, rectTop + bracketLen)
                        }
                        drawPath(pathTR, bracketColor, style = Stroke(width = bracketStroke))

                        // Bottom-Left
                        val pathBL = Path().apply {
                            moveTo(rectLeft, rectBottom - bracketLen)
                            lineTo(rectLeft, rectBottom)
                            lineTo(rectLeft + bracketLen, rectBottom)
                        }
                        drawPath(pathBL, bracketColor, style = Stroke(width = bracketStroke))

                        // Bottom-Right
                        val pathBR = Path().apply {
                            moveTo(rectRight - bracketLen, rectBottom)
                            lineTo(rectRight, rectBottom)
                            lineTo(rectRight, rectBottom - bracketLen)
                        }
                        drawPath(pathBR, bracketColor, style = Stroke(width = bracketStroke))

                        // 6. Edge handles (pills)
                        val pillColor = Color(0xFF2563EB)
                        val pillW = 20.dp.toPx()
                        val pillH = 4.5.dp.toPx()
                        // Top edge pill
                        drawRoundRect(
                            color = pillColor,
                            topLeft = Offset((rectLeft + rectRight) / 2f - pillW / 2f, rectTop - pillH / 2f),
                            size = Size(pillW, pillH),
                            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                        )
                        // Bottom edge pill
                        drawRoundRect(
                            color = pillColor,
                            topLeft = Offset((rectLeft + rectRight) / 2f - pillW / 2f, rectBottom - pillH / 2f),
                            size = Size(pillW, pillH),
                            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                        )
                        // Left edge pill
                        drawRoundRect(
                            color = pillColor,
                            topLeft = Offset(rectLeft - pillH / 2f, (rectTop + rectBottom) / 2f - pillW / 2f),
                            size = Size(pillH, pillW),
                            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                        )
                        // Right edge pill
                        drawRoundRect(
                            color = pillColor,
                            topLeft = Offset(rectRight - pillH / 2f, (rectTop + rectBottom) / 2f - pillW / 2f),
                            size = Size(pillH, pillW),
                            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                        )
                    }

                    // Live Dimensions Badge (Floating inside or below crop box)
                    val croppedW = ((cropRight - cropLeft) * sourceBitmap.width).toInt().coerceAtLeast(1)
                    val croppedH = ((cropBottom - cropTop) * sourceBitmap.height).toInt().coerceAtLeast(1)

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$croppedW × $croppedH px",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 4. Bottom Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("crop_dialog_cancel")
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }

                    Button(
                        onClick = {
                            val px = (cropLeft * sourceBitmap.width).toInt().coerceIn(0, sourceBitmap.width - 1)
                            val py = (cropTop * sourceBitmap.height).toInt().coerceIn(0, sourceBitmap.height - 1)
                            val pw = ((cropRight - cropLeft) * sourceBitmap.width).toInt().coerceIn(1, sourceBitmap.width - px)
                            val ph = ((cropBottom - cropTop) * sourceBitmap.height).toInt().coerceIn(1, sourceBitmap.height - py)

                            val cropped = Bitmap.createBitmap(sourceBitmap, px, py, pw, ph)
                            onApplyCrop(cropped, cropLeft, cropTop, cropRight, cropBottom)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("crop_dialog_apply")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Apply", modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Crop", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
