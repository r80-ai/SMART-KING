package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photo.CropPreset
import com.example.photo.DrawingStroke
import com.example.photo.PhotoFonts
import com.example.photo.TextAlignment
import com.example.photo.TextItem
import com.example.photo.MaskStroke
import com.example.photo.BgRefineMode
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.example.ui.theme.CrimsonPrimary
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Interactive draggable crop overlay with 1:1 responsive corner & edge handles and aspect ratio presets.
 */
@Composable
fun CropOverlay(
    cropRect: android.graphics.RectF,
    onCropRectChange: (android.graphics.RectF) -> Unit,
    aspectPreset: CropPreset,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val currentCropRectState = rememberUpdatedState(cropRect)
        val currentAspectPresetState = rememberUpdatedState(aspectPreset)
        val onCropRectChangeState = rememberUpdatedState(onCropRectChange)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthPx, heightPx) {
                    val cornerTouchRadiusPx = 52.dp.toPx()
                    val edgeTouchThicknessPx = 42.dp.toPx()

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position

                        val currRect = currentCropRectState.value
                        val leftPx = currRect.left * widthPx
                        val topPx = currRect.top * heightPx
                        val rightPx = currRect.right * widthPx
                        val bottomPx = currRect.bottom * heightPx

                        // 1. Check corner proximity
                        val dTL = hypot(downPos.x - leftPx, downPos.y - topPx)
                        val dTR = hypot(downPos.x - rightPx, downPos.y - topPx)
                        val dBR = hypot(downPos.x - rightPx, downPos.y - bottomPx)
                        val dBL = hypot(downPos.x - leftPx, downPos.y - bottomPx)

                        var activeHandle = -1 // 0: TL, 1: TR, 2: BR, 3: BL, 4: Top, 5: Right, 6: Bottom, 7: Left, 8: Center

                        if (dTL <= cornerTouchRadiusPx || dTR <= cornerTouchRadiusPx || dBR <= cornerTouchRadiusPx || dBL <= cornerTouchRadiusPx) {
                            val minD = minOf(dTL, dTR, dBR, dBL)
                            activeHandle = when (minD) {
                                dTL -> 0
                                dTR -> 1
                                dBR -> 2
                                else -> 3
                            }
                        } else {
                            // 2. Check edges with generous hit strips
                            val nearTop = abs(downPos.y - topPx) <= edgeTouchThicknessPx && downPos.x in (leftPx - 16f)..(rightPx + 16f)
                            val nearBottom = abs(downPos.y - bottomPx) <= edgeTouchThicknessPx && downPos.x in (leftPx - 16f)..(rightPx + 16f)
                            val nearLeft = abs(downPos.x - leftPx) <= edgeTouchThicknessPx && downPos.y in (topPx - 16f)..(bottomPx + 16f)
                            val nearRight = abs(downPos.x - rightPx) <= edgeTouchThicknessPx && downPos.y in (topPx - 16f)..(bottomPx + 16f)

                            activeHandle = when {
                                nearTop -> 4
                                nearRight -> 5
                                nearBottom -> 6
                                nearLeft -> 7
                                downPos.x in leftPx..rightPx && downPos.y in topPx..bottomPx -> 8
                                else -> -1
                            }
                        }

                        if (activeHandle != -1) {
                            down.consume()
                            var lastTouch = downPos

                            drag(down.id) { change ->
                                change.consume()
                                val currentPos = change.position
                                val ratio = currentAspectPresetState.value.ratio
                                val r = currentCropRectState.value

                                when (activeHandle) {
                                    0 -> { // Top-Left corner: 1:1 finger tracking
                                        val newNormX = (currentPos.x / widthPx).coerceIn(0f, r.right - 0.04f)
                                        val newNormY = (currentPos.y / heightPx).coerceIn(0f, r.bottom - 0.04f)
                                        if (ratio == null) {
                                            onCropRectChangeState.value(android.graphics.RectF(newNormX, newNormY, r.right, r.bottom))
                                        } else {
                                            val reqW = (r.bottom - newNormY) * (heightPx / widthPx) * ratio
                                            val adjL = (r.right - reqW).coerceIn(0f, r.right - 0.02f)
                                            onCropRectChangeState.value(android.graphics.RectF(adjL, newNormY, r.right, r.bottom))
                                        }
                                    }
                                    1 -> { // Top-Right corner: 1:1 finger tracking
                                        val newNormX = (currentPos.x / widthPx).coerceIn(r.left + 0.04f, 1f)
                                        val newNormY = (currentPos.y / heightPx).coerceIn(0f, r.bottom - 0.04f)
                                        if (ratio == null) {
                                            onCropRectChangeState.value(android.graphics.RectF(r.left, newNormY, newNormX, r.bottom))
                                        } else {
                                            val reqW = (r.bottom - newNormY) * (heightPx / widthPx) * ratio
                                            val adjR = (r.left + reqW).coerceIn(r.left + 0.02f, 1f)
                                            onCropRectChangeState.value(android.graphics.RectF(r.left, newNormY, adjR, r.bottom))
                                        }
                                    }
                                    2 -> { // Bottom-Right corner: 1:1 finger tracking
                                        val newNormX = (currentPos.x / widthPx).coerceIn(r.left + 0.04f, 1f)
                                        val newNormY = (currentPos.y / heightPx).coerceIn(r.top + 0.04f, 1f)
                                        if (ratio == null) {
                                            onCropRectChangeState.value(android.graphics.RectF(r.left, r.top, newNormX, newNormY))
                                        } else {
                                            val reqW = (newNormY - r.top) * (heightPx / widthPx) * ratio
                                            val adjR = (r.left + reqW).coerceIn(r.left + 0.02f, 1f)
                                            onCropRectChangeState.value(android.graphics.RectF(r.left, r.top, adjR, newNormY))
                                        }
                                    }
                                    3 -> { // Bottom-Left corner: 1:1 finger tracking
                                        val newNormX = (currentPos.x / widthPx).coerceIn(0f, r.right - 0.04f)
                                        val newNormY = (currentPos.y / heightPx).coerceIn(r.top + 0.04f, 1f)
                                        if (ratio == null) {
                                            onCropRectChangeState.value(android.graphics.RectF(newNormX, r.top, r.right, newNormY))
                                        } else {
                                            val reqW = (newNormY - r.top) * (heightPx / widthPx) * ratio
                                            val adjL = (r.right - reqW).coerceIn(0f, r.right - 0.02f)
                                            onCropRectChangeState.value(android.graphics.RectF(adjL, r.top, r.right, newNormY))
                                        }
                                    }
                                    4 -> { // Top edge: 1:1 finger tracking Y
                                        val newNormY = (currentPos.y / heightPx).coerceIn(0f, r.bottom - 0.04f)
                                        if (ratio == null) {
                                            onCropRectChangeState.value(android.graphics.RectF(r.left, newNormY, r.right, r.bottom))
                                        } else {
                                            val reqW = (r.bottom - newNormY) * (heightPx / widthPx) * ratio
                                            val center = (r.left + r.right) / 2f
                                            val halfW = reqW / 2f
                                            val adjL = (center - halfW).coerceIn(0f, r.right - 0.02f)
                                            val adjR = (center + halfW).coerceIn(adjL + 0.02f, 1f)
                                            onCropRectChangeState.value(android.graphics.RectF(adjL, newNormY, adjR, r.bottom))
                                        }
                                    }
                                    5 -> { // Right edge: 1:1 finger tracking X
                                        val newNormX = (currentPos.x / widthPx).coerceIn(r.left + 0.04f, 1f)
                                        if (ratio == null) {
                                            onCropRectChangeState.value(android.graphics.RectF(r.left, r.top, newNormX, r.bottom))
                                        } else {
                                            val reqH = (newNormX - r.left) * (widthPx / heightPx) / ratio
                                            val center = (r.top + r.bottom) / 2f
                                            val halfH = reqH / 2f
                                            val adjT = (center - halfH).coerceIn(0f, r.bottom - 0.02f)
                                            val adjB = (center + halfH).coerceIn(adjT + 0.02f, 1f)
                                            onCropRectChangeState.value(android.graphics.RectF(r.left, adjT, newNormX, adjB))
                                        }
                                    }
                                    6 -> { // Bottom edge: 1:1 finger tracking Y
                                        val newNormY = (currentPos.y / heightPx).coerceIn(r.top + 0.04f, 1f)
                                        if (ratio == null) {
                                            onCropRectChangeState.value(android.graphics.RectF(r.left, r.top, r.right, newNormY))
                                        } else {
                                            val reqW = (newNormY - r.top) * (heightPx / widthPx) * ratio
                                            val center = (r.left + r.right) / 2f
                                            val halfW = reqW / 2f
                                            val adjL = (center - halfW).coerceIn(0f, r.right - 0.02f)
                                            val adjR = (center + halfW).coerceIn(adjL + 0.02f, 1f)
                                            onCropRectChangeState.value(android.graphics.RectF(adjL, r.top, adjR, newNormY))
                                        }
                                    }
                                    7 -> { // Left edge: 1:1 finger tracking X
                                        val newNormX = (currentPos.x / widthPx).coerceIn(0f, r.right - 0.04f)
                                        if (ratio == null) {
                                            onCropRectChangeState.value(android.graphics.RectF(newNormX, r.top, r.right, r.bottom))
                                        } else {
                                            val reqH = (r.right - newNormX) * (widthPx / heightPx) / ratio
                                            val center = (r.top + r.bottom) / 2f
                                            val halfH = reqH / 2f
                                            val adjT = (center - halfH).coerceIn(0f, r.bottom - 0.02f)
                                            val adjB = (center + halfH).coerceIn(adjT + 0.02f, 1f)
                                            onCropRectChangeState.value(android.graphics.RectF(newNormX, adjT, r.right, adjB))
                                        }
                                    }
                                    8 -> { // Center Pan: moves entire crop box smoothly
                                        val dx = (currentPos.x - lastTouch.x) / widthPx
                                        val dy = (currentPos.y - lastTouch.y) / heightPx
                                        val boxW = r.width()
                                        val boxH = r.height()
                                        val newL = (r.left + dx).coerceIn(0f, 1f - boxW)
                                        val newT = (r.top + dy).coerceIn(0f, 1f - boxH)
                                        onCropRectChangeState.value(android.graphics.RectF(newL, newT, newL + boxW, newT + boxH))
                                        lastTouch = currentPos
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val left = cropRect.left * size.width
            val top = cropRect.top * size.height
            val right = cropRect.right * size.width
            val bottom = cropRect.bottom * size.height
            val cropW = right - left
            val cropH = bottom - top

            // 1. Darken outside region
            drawRect(color = Color(0x99000000), topLeft = Offset.Zero, size = Size(size.width, top))
            drawRect(color = Color(0x99000000), topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
            drawRect(color = Color(0x99000000), topLeft = Offset(0f, top), size = Size(left, cropH))
            drawRect(color = Color(0x99000000), topLeft = Offset(right, top), size = Size(size.width - right, cropH))

            // 2. Crop border
            drawRect(color = Color.White, topLeft = Offset(left, top), size = Size(cropW, cropH), style = Stroke(width = 2.dp.toPx()))

            // 3. Rule of thirds grid
            val thirdW = cropW / 3f
            val thirdH = cropH / 3f
            val gridColor = Color.White.copy(alpha = 0.45f)
            val gridStroke = Stroke(width = 1.dp.toPx())

            drawLine(gridColor, Offset(left + thirdW, top), Offset(left + thirdW, bottom), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(left + 2 * thirdW, top), Offset(left + 2 * thirdW, bottom), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(left, top + thirdH), Offset(right, top + thirdH), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(left, top + 2 * thirdH), Offset(right, top + 2 * thirdH), strokeWidth = gridStroke.width)

            // 4. Edge handles (visual pill indicators on midpoints)
            val edgePillLen = 28.dp.toPx()
            val edgePillThick = 4.5.dp.toPx()
            val edgePillColor = CrimsonPrimary

            // Top Edge midpoint handle
            drawRoundRect(
                color = edgePillColor,
                topLeft = Offset((left + right) / 2f - edgePillLen / 2f, top - edgePillThick / 2f),
                size = Size(edgePillLen, edgePillThick),
                cornerRadius = CornerRadius(edgePillThick / 2f, edgePillThick / 2f)
            )
            // Bottom Edge midpoint handle
            drawRoundRect(
                color = edgePillColor,
                topLeft = Offset((left + right) / 2f - edgePillLen / 2f, bottom - edgePillThick / 2f),
                size = Size(edgePillLen, edgePillThick),
                cornerRadius = CornerRadius(edgePillThick / 2f, edgePillThick / 2f)
            )
            // Left Edge midpoint handle
            drawRoundRect(
                color = edgePillColor,
                topLeft = Offset(left - edgePillThick / 2f, (top + bottom) / 2f - edgePillLen / 2f),
                size = Size(edgePillThick, edgePillLen),
                cornerRadius = CornerRadius(edgePillThick / 2f, edgePillThick / 2f)
            )
            // Right Edge midpoint handle
            drawRoundRect(
                color = edgePillColor,
                topLeft = Offset(right - edgePillThick / 2f, (top + bottom) / 2f - edgePillLen / 2f),
                size = Size(edgePillThick, edgePillLen),
                cornerRadius = CornerRadius(edgePillThick / 2f, edgePillThick / 2f)
            )

            // 5. Corner bracket handles (bold L-brackets)
            val handleLen = 22.dp.toPx()
            val handleThick = 4.5.dp.toPx()
            val handleColor = Color.White

            // Top-Left
            drawLine(handleColor, Offset(left - 2f, top), Offset(left + handleLen, top), strokeWidth = handleThick)
            drawLine(handleColor, Offset(left, top - 2f), Offset(left, top + handleLen), strokeWidth = handleThick)

            // Top-Right
            drawLine(handleColor, Offset(right - handleLen, top), Offset(right + 2f, top), strokeWidth = handleThick)
            drawLine(handleColor, Offset(right, top - 2f), Offset(right, top + handleLen), strokeWidth = handleThick)

            // Bottom-Right
            drawLine(handleColor, Offset(right - handleLen, bottom), Offset(right + 2f, bottom), strokeWidth = handleThick)
            drawLine(handleColor, Offset(right, bottom - handleLen), Offset(right, bottom + 2f), strokeWidth = handleThick)

            // Bottom-Left
            drawLine(handleColor, Offset(left - 2f, bottom), Offset(left + handleLen, bottom), strokeWidth = handleThick)
            drawLine(handleColor, Offset(left, bottom - handleLen), Offset(left, bottom + 2f), strokeWidth = handleThick)
        }
    }
}

/**
 * Draggable 4-corner perspective adjustment pins overlay.
 * corners: [TL, TR, BR, BL] in normalized coordinates [0f..1f].
 */
@Composable
fun PerspectiveOverlay(
    corners: List<PointF>,
    onCornersChange: (List<PointF>) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val cornersState = rememberUpdatedState(corners)
        val onCornersChangeState = rememberUpdatedState(onCornersChange)
        var activeCornerIndex by remember { mutableIntStateOf(-1) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthPx, heightPx) {
                    val touchHitRadiusPx = 56.dp.toPx() // Generous invisible touch target

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val currCorners = cornersState.value
                        var closestIdx = -1
                        var minDist = Float.MAX_VALUE

                        currCorners.forEachIndexed { i, pt ->
                            val pxX = pt.x * widthPx
                            val pxY = pt.y * heightPx
                            val dist = hypot(down.position.x - pxX, down.position.y - pxY)
                            if (dist <= touchHitRadiusPx && dist < minDist) {
                                minDist = dist
                                closestIdx = i
                            }
                        }

                        activeCornerIndex = closestIdx

                        if (closestIdx != -1) {
                            down.consume()
                            drag(down.id) { change ->
                                change.consume()
                                val newX = (change.position.x / widthPx).coerceIn(0f, 1f)
                                val newY = (change.position.y / heightPx).coerceIn(0f, 1f)

                                val updated = cornersState.value.toMutableList()
                                updated[closestIdx] = PointF(newX, newY)
                                onCornersChangeState.value(updated)
                            }
                            activeCornerIndex = -1
                        }
                    }
                }
        ) {
            if (corners.size == 4) {
                val pts = corners.map { Offset(it.x * size.width, it.y * size.height) }

                // Draw quad outline
                val quadPath = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    lineTo(pts[1].x, pts[1].y)
                    lineTo(pts[2].x, pts[2].y)
                    lineTo(pts[3].x, pts[3].y)
                    close()
                }

                // Semi-transparent quad fill
                drawPath(quadPath, color = Color(0x33E11D48))
                drawPath(quadPath, color = CrimsonPrimary, style = Stroke(width = 2.5.dp.toPx()))

                // Draw corner handles with large visible feedback and clean labels
                pts.forEachIndexed { index, offset ->
                    val isActive = index == activeCornerIndex
                    val outerRadius = if (isActive) 20.dp.toPx() else 15.dp.toPx()
                    val innerRadius = if (isActive) 12.dp.toPx() else 9.dp.toPx()

                    // Outer white glow/ring
                    drawCircle(color = Color.White, radius = outerRadius, center = offset)
                    // Inner colored pin
                    drawCircle(color = CrimsonPrimary, radius = innerRadius, center = offset)
                    // Center white dot
                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = offset)
                }
            }
        }
    }
}

/**
 * Freehand touch stroke capture for Blur, Erase, and Drawing tools.
 */
@Composable
fun TouchStrokeCanvas(
    strokes: List<List<PointF>>,
    currentStroke: List<PointF>,
    strokeColor: Color,
    brushSizeDp: Float,
    onStrokePointAdded: (PointF) -> Unit,
    onStrokeCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val brushRadiusPx = with(density) { (brushSizeDp / 2f).dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthPx, heightPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val pt = PointF(
                                (offset.x / widthPx).coerceIn(0f, 1f),
                                (offset.y / heightPx).coerceIn(0f, 1f)
                            )
                            onStrokePointAdded(pt)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pt = PointF(
                                (change.position.x / widthPx).coerceIn(0f, 1f),
                                (change.position.y / heightPx).coerceIn(0f, 1f)
                            )
                            onStrokePointAdded(pt)
                        },
                        onDragEnd = { onStrokeCompleted() },
                        onDragCancel = { onStrokeCompleted() }
                    )
                }
        ) {
            val allStrokes = if (currentStroke.isNotEmpty()) strokes + listOf(currentStroke) else strokes

            for (stroke in allStrokes) {
                if (stroke.isEmpty()) continue
                if (stroke.size == 1) {
                    val pt = stroke.first()
                    drawCircle(
                        color = strokeColor,
                        radius = brushRadiusPx,
                        center = Offset(pt.x * size.width, pt.y * size.height)
                    )
                } else {
                    val path = Path().apply {
                        moveTo(stroke[0].x * size.width, stroke[0].y * size.height)
                        for (i in 1 until stroke.size) {
                            lineTo(stroke[i].x * size.width, stroke[i].y * size.height)
                        }
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = brushRadiusPx * 2f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Drawing Canvas supporting accurate per-stroke colors, brush sizes, opacities,
 * eraser strokes, seamless zoom-and-draw without panning the image, and immediate touch response.
 */
@Composable
fun DrawingOverlayCanvas(
    strokes: List<DrawingStroke>,
    currentStrokePoints: List<PointF>,
    currentColor: Int,
    currentBrushSizeDp: Float,
    currentOpacity: Float,
    isEraser: Boolean,
    isPanMode: Boolean,
    onStrokePointAdded: (PointF) -> Unit,
    onStrokeCompleted: () -> Unit,
    onStrokeCancelled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .then(
                    if (!isPanMode) {
                        Modifier.pointerInput(widthPx, heightPx, isPanMode) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val initialPt = PointF(
                                    (down.position.x / widthPx).coerceIn(0f, 1f),
                                    (down.position.y / heightPx).coerceIn(0f, 1f)
                                )
                                onStrokePointAdded(initialPt)
                                down.consume()

                                var isMultiTouch = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.isEmpty()) break

                                    // If a second finger touches down (for zooming/panning), cancel drawing stroke
                                    if (pressed.size > 1) {
                                        isMultiTouch = true
                                        break
                                    }

                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    change.consume()
                                    val pt = PointF(
                                        (change.position.x / widthPx).coerceIn(0f, 1f),
                                        (change.position.y / heightPx).coerceIn(0f, 1f)
                                    )
                                    onStrokePointAdded(pt)
                                }

                                if (isMultiTouch) {
                                    onStrokeCancelled()
                                } else {
                                    onStrokeCompleted()
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            // Draw completed strokes
            for (stroke in strokes) {
                if (stroke.points.isEmpty()) continue
                val strokeColor = if (stroke.isEraser) {
                    Color.Transparent
                } else {
                    Color(stroke.color).copy(alpha = stroke.opacity.coerceIn(0.05f, 1f))
                }
                val strokeBrushRadius = with(density) { (stroke.brushSizePx / 2f).dp.toPx() }
                val blend = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver

                if (stroke.points.size == 1) {
                    val pt = stroke.points.first()
                    drawCircle(
                        color = strokeColor,
                        radius = strokeBrushRadius,
                        center = Offset(pt.x * size.width, pt.y * size.height),
                        blendMode = blend
                    )
                } else {
                    val path = Path().apply {
                        moveTo(stroke.points[0].x * size.width, stroke.points[0].y * size.height)
                        for (i in 1 until stroke.points.size) {
                            lineTo(stroke.points[i].x * size.width, stroke.points[i].y * size.height)
                        }
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = strokeBrushRadius * 2f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = blend
                    )
                }
            }

            // Draw actively ongoing stroke
            if (currentStrokePoints.isNotEmpty()) {
                val activeColor = if (isEraser) {
                    Color.Transparent
                } else {
                    Color(currentColor).copy(alpha = currentOpacity.coerceIn(0.05f, 1f))
                }
                val activeRadius = with(density) { (currentBrushSizeDp / 2f).dp.toPx() }
                val blend = if (isEraser) BlendMode.Clear else BlendMode.SrcOver

                if (currentStrokePoints.size == 1) {
                    val pt = currentStrokePoints.first()
                    drawCircle(
                        color = activeColor,
                        radius = activeRadius,
                        center = Offset(pt.x * size.width, pt.y * size.height),
                        blendMode = blend
                    )
                } else {
                    val path = Path().apply {
                        moveTo(currentStrokePoints[0].x * size.width, currentStrokePoints[0].y * size.height)
                        for (i in 1 until currentStrokePoints.size) {
                            lineTo(currentStrokePoints[i].x * size.width, currentStrokePoints[i].y * size.height)
                        }
                    }
                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(
                            width = activeRadius * 2f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = blend
                    )
                }
            }
        }
    }
}

/**
 * Text Overlay Canvas with non-destructive, draggable, rotatable, resizable text layers.
 * Supports smooth 1:1 single-finger dragging, pinch-to-zoom scaling, two-finger rotation,
 * dedicated corner transformation handles, tap to select, and double-tap to edit.
 */
@Composable
fun TextOverlayCanvas(
    textItems: List<TextItem>,
    selectedTextId: String?,
    onSelectText: (String) -> Unit,
    onUpdateTextItem: (TextItem) -> Unit,
    onEditText: (TextItem) -> Unit,
    onDeleteText: (String) -> Unit,
    modifier: Modifier = Modifier,
    onStartTransform: (() -> Unit)? = null
) {
    val currentOnSelectState = rememberUpdatedState(onSelectText)
    val currentOnUpdateState = rememberUpdatedState(onUpdateTextItem)
    val currentOnEditTextState = rememberUpdatedState(onEditText)
    val currentOnDeleteTextState = rememberUpdatedState(onDeleteText)
    val currentOnStartTransformState = rememberUpdatedState(onStartTransform)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    currentOnSelectState.value("")
                }
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        for (item in textItems) {
            val isSelected = item.id == selectedTextId
            val centerX = item.normX * widthPx
            val centerY = item.normY * heightPx
            val font = PhotoFonts.getFont(item.fontId)
            val currentItemState = rememberUpdatedState(item)

            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            val x = (centerX - placeable.width / 2f).roundToInt()
                            val y = (centerY - placeable.height / 2f).roundToInt()
                            placeable.placeRelative(x, y)
                        }
                    }
                    .graphicsLayer {
                        rotationZ = item.rotationDeg
                        alpha = item.opacity.coerceIn(0.05f, 1f)
                    }
                    .padding(8.dp)
            ) {
                // Text Bounding Box with Smooth Drag, Pinch & Rotate
                Box(
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) CrimsonPrimary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            if (isSelected) Color(0x33E11D48) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .pointerInput(item.id, widthPx, heightPx) {
                            detectTapGestures(
                                onPress = {
                                    currentOnSelectState.value(item.id)
                                },
                                onTap = {
                                    currentOnSelectState.value(item.id)
                                },
                                onDoubleTap = {
                                    currentOnSelectState.value(item.id)
                                    currentOnEditTextState.value(currentItemState.value)
                                }
                            )
                        }
                        .pointerInput(item.id, widthPx, heightPx) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                currentOnSelectState.value(item.id)
                                currentOnStartTransformState.value?.invoke()

                                var accNormX = currentItemState.value.normX
                                var accNormY = currentItemState.value.normY
                                var accSize = currentItemState.value.fontSizeSp
                                var accRot = currentItemState.value.rotationDeg

                                var touchSlopPassed = false
                                val touchSlop = viewConfiguration.touchSlop

                                do {
                                    val event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (canceled) break

                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount == 0) break

                                    val pan = event.calculatePan()
                                    val zoom = event.calculateZoom()
                                    val rotation = event.calculateRotation()

                                    if (!touchSlopPassed) {
                                        if (pan.getDistance() > touchSlop || zoom != 1f || rotation != 0f) {
                                            touchSlopPassed = true
                                        }
                                    }

                                    if (touchSlopPassed) {
                                        accNormX = (accNormX + pan.x / widthPx)
                                        accNormY = (accNormY + pan.y / heightPx)
                                        accSize = (accSize * zoom).coerceAtLeast(1f)
                                        accRot = (accRot + rotation) % 360f

                                        currentOnUpdateState.value(
                                            currentItemState.value.copy(
                                                normX = accNormX,
                                                normY = accNormY,
                                                fontSizeSp = accSize,
                                                rotationDeg = accRot
                                            )
                                        )
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    Text(
                        text = item.text,
                        color = Color(item.color),
                        fontSize = item.fontSizeSp.sp,
                        fontFamily = font.fontFamily,
                        fontWeight = if (item.isBold || font.defaultBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (item.isItalic || font.defaultItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (item.isUnderline) TextDecoration.Underline else TextDecoration.None,
                        textAlign = when (item.alignment) {
                            TextAlignment.LEFT -> androidx.compose.ui.text.style.TextAlign.Start
                            TextAlignment.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                            TextAlignment.RIGHT -> androidx.compose.ui.text.style.TextAlign.End
                        },
                        modifier = Modifier.padding(4.dp)
                    )
                }

                // Interactive Corner Handles when selected (Touch targets >= 48dp)
                if (isSelected) {
                    // Top-Left: Edit Text Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-14).dp, y = (-14).dp)
                            .size(48.dp)
                            .clickable { currentOnEditTextState.value(currentItemState.value) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(1.5.dp, CrimsonPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Text Content",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Bottom-Left: Delete Text Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-14).dp, y = 14.dp)
                            .size(48.dp)
                            .clickable { currentOnDeleteTextState.value(item.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Text",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Top-Right: Rotate Handle (Smooth real-time drag to rotate)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 14.dp, y = (-14).dp)
                            .size(48.dp)
                            .pointerInput(item.id) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    currentOnStartTransformState.value?.invoke()
                                    var accRot = currentItemState.value.rotationDeg
                                    do {
                                        val event = awaitPointerEvent()
                                        val pan = event.calculatePan()
                                        if (pan != Offset.Zero) {
                                            val rotDelta = (pan.x + pan.y) * 0.75f
                                            accRot = (accRot + rotDelta) % 360f
                                            currentOnUpdateState.value(currentItemState.value.copy(rotationDeg = accRot))
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(1.5.dp, CrimsonPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate Text Handle",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Bottom-Right: Resize Handle (Smooth real-time drag to scale font size)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 14.dp, y = 14.dp)
                            .size(48.dp)
                            .pointerInput(item.id) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    currentOnStartTransformState.value?.invoke()
                                    var accSize = currentItemState.value.fontSizeSp
                                    do {
                                        val event = awaitPointerEvent()
                                        val pan = event.calculatePan()
                                        if (pan != Offset.Zero) {
                                            val sizeDelta = (pan.x + pan.y) * 0.35f
                                            accSize = (accSize + sizeDelta).coerceAtLeast(1f)
                                            currentOnUpdateState.value(currentItemState.value.copy(fontSizeSp = accSize))
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(1.5.dp, CrimsonPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "Resize Text Handle",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Manual Refinement Overlay for Background Removal:
 * Allows user to brush directly onto the photo to Erase remaining background or Restore subject parts.
 */
@Composable
fun RefineMaskOverlay(
    refineMode: BgRefineMode,
    brushSizeDp: Float,
    isSoftBrush: Boolean,
    onStrokeCompleted: (MaskStroke) -> Unit,
    modifier: Modifier = Modifier
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    val currentPoints = remember { mutableStateListOf<PointF>() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(refineMode, brushSizeDp, isSoftBrush) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    currentPoints.clear()
                    currentPoints.add(PointF(down.position.x / w, down.position.y / h))
                    touchPos = down.position

                    drag(down.id) { change ->
                        change.consume()
                        touchPos = change.position
                        currentPoints.add(PointF(change.position.x / w, change.position.y / h))
                    }

                    if (currentPoints.isNotEmpty()) {
                        val stroke = MaskStroke(
                            points = currentPoints.toList(),
                            radiusPx = brushSizeDp * 1.5f,
                            isRestore = refineMode == BgRefineMode.RESTORE_SUBJECT,
                            isSoft = isSoftBrush
                        )
                        onStrokeCompleted(stroke)
                        currentPoints.clear()
                    }
                    touchPos = null
                }
            }
    ) {
        val pos = touchPos
        if (pos != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = brushSizeDp.dp.toPx()
                val color = if (refineMode == BgRefineMode.RESTORE_SUBJECT) Color(0xFF10B981) else Color(0xFFE11D48)
                drawCircle(
                    color = color.copy(alpha = 0.35f),
                    radius = r,
                    center = pos
                )
                drawCircle(
                    color = Color.White,
                    radius = r,
                    center = pos,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = color,
                    radius = 3.dp.toPx(),
                    center = pos
                )
            }
        }
    }
}

/**
 * Renders a high-contrast checkerboard canvas pattern behind transparent image regions.
 */
@Composable
fun CheckerboardPattern(
    modifier: Modifier = Modifier,
    squareSizeDp: androidx.compose.ui.unit.Dp = 12.dp,
    color1: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF232B38),
    color2: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF181F2A)
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val squarePx = with(density) { squareSizeDp.toPx() }

    Canvas(modifier = modifier) {
        val numX = (size.width / squarePx).toInt() + 1
        val numY = (size.height / squarePx).toInt() + 1
        for (y in 0 until numY) {
            for (x in 0 until numX) {
                val color = if ((x + y) % 2 == 0) color1 else color2
                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(x * squarePx, y * squarePx),
                    size = androidx.compose.ui.geometry.Size(squarePx, squarePx)
                )
            }
        }
    }
}

/**
 * Handles direct on-canvas pan, scale, and rotation of the extracted foreground subject.
 */
@Composable
fun ForegroundTransformOverlay(
    translationX: Float,
    translationY: Float,
    scale: Float,
    rotation: Float,
    onTransformChange: (Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var curX by remember(translationX) { mutableFloatStateOf(translationX) }
    var curY by remember(translationY) { mutableFloatStateOf(translationY) }
    var curScale by remember(scale) { mutableFloatStateOf(scale) }
    var curRot by remember(rotation) { mutableFloatStateOf(rotation) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotate ->
                    curX += pan.x
                    curY += pan.y
                    curScale = (curScale * zoom).coerceIn(0.2f, 3.5f)
                    curRot = (curRot + rotate) % 360f
                    onTransformChange(curX, curY, curScale, curRot)
                }
            }
    )
}

/**
 * Full-spectrum Custom Color Picker Dialog with RGB sliders and presets.
 */
@Composable
fun CustomColorPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var r by remember { mutableFloatStateOf(((initialColor shr 16) and 0xFF) / 255f) }
    var g by remember { mutableFloatStateOf(((initialColor shr 8) and 0xFF) / 255f) }
    var b by remember { mutableFloatStateOf((initialColor and 0xFF) / 255f) }

    val currentColorInt = android.graphics.Color.rgb(
        (r * 255).roundToInt().coerceIn(0, 255),
        (g * 255).roundToInt().coerceIn(0, 255),
        (b * 255).roundToInt().coerceIn(0, 255)
    )

    val quickColors = listOf(
        0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFE11D48.toInt(), 0xFF3B82F6.toInt(),
        0xFF10B981.toInt(), 0xFFF59E0B.toInt(), 0xFF8B5CF6.toInt(), 0xFFEC4899.toInt(),
        0xFF06B6D4.toInt(), 0xFF64748B.toInt(), 0xFFFF5722.toInt(), 0xFF4CAF50.toInt(),
        0xFFFFEB3B.toInt(), 0xFF9C27B0.toInt(), 0xFF795548.toInt(), 0xFF0F172A.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(currentColorInt))
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Quick Presets:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickColors.forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(col))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    r = ((col shr 16) and 0xFF) / 255f
                                    g = ((col shr 8) and 0xFF) / 255f
                                    b = (col and 0xFF) / 255f
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Red: ${(r * 255).roundToInt()}", color = Color(0xFFEF4444), fontSize = 12.sp, modifier = Modifier.width(60.dp))
                    Slider(
                        value = r,
                        onValueChange = { r = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Green: ${(g * 255).roundToInt()}", color = Color(0xFF10B981), fontSize = 12.sp, modifier = Modifier.width(60.dp))
                    Slider(
                        value = g,
                        onValueChange = { g = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Blue: ${(b * 255).roundToInt()}", color = Color(0xFF3B82F6), fontSize = 12.sp, modifier = Modifier.width(60.dp))
                    Slider(
                        value = b,
                        onValueChange = { b = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentColorInt)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Select", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

/**
 * Photoshop-style Eyedropper / Color Picker overlay.
 * Samples exact pixel colors from the image bitmap upon touch / drag.
 */
@Composable
fun EyedropperOverlay(
    bitmap: Bitmap,
    currentColor: Int,
    onColorSampled: (Int) -> Unit,
    onColorConfirmed: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var activeColor by remember { mutableIntStateOf(currentColor) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bitmap) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun sampleAt(pos: Offset) {
                        touchPos = pos
                        val px = ((pos.x / size.width.toFloat()) * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                        val py = ((pos.y / size.height.toFloat()) * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                        if (!bitmap.isRecycled) {
                            val color = bitmap.getPixel(px, py)
                            activeColor = color
                            onColorSampled(color)
                        }
                    }

                    sampleAt(down.position)

                    drag(down.id) { change ->
                        change.consume()
                        sampleAt(change.position)
                    }

                    // Touch released
                    val finalPos = touchPos
                    if (finalPos != null && !bitmap.isRecycled) {
                        val px = ((finalPos.x / size.width.toFloat()) * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                        val py = ((finalPos.y / size.height.toFloat()) * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                        val finalColor = bitmap.getPixel(px, py)
                        onColorConfirmed(finalColor)
                    }
                    touchPos = null
                }
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pos = touchPos
            if (pos != null) {
                val loupeRadius = 34.dp.toPx()
                val loupeCenter = Offset(
                    x = pos.x.coerceIn(loupeRadius + 8f, widthPx - loupeRadius - 8f),
                    y = (pos.y - 65.dp.toPx()).coerceIn(loupeRadius + 8f, heightPx - loupeRadius - 8f)
                )

                // Stem line connecting touch pos to loupe
                drawLine(
                    color = Color.White,
                    start = pos,
                    end = loupeCenter,
                    strokeWidth = 2.dp.toPx()
                )

                // Crosshair at target touch position
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = pos,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawLine(
                    color = Color.White,
                    start = Offset(pos.x - 10.dp.toPx(), pos.y),
                    end = Offset(pos.x + 10.dp.toPx(), pos.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(pos.x, pos.y - 10.dp.toPx()),
                    end = Offset(pos.x, pos.y + 10.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Loupe background with sampled color
                drawCircle(
                    color = Color(activeColor),
                    radius = loupeRadius,
                    center = loupeCenter
                )

                // Inner white ring
                drawCircle(
                    color = Color.White,
                    radius = loupeRadius,
                    center = loupeCenter,
                    style = Stroke(width = 3.5.dp.toPx())
                )
                // Outer dark shadow ring
                drawCircle(
                    color = Color(0x99000000),
                    radius = loupeRadius + 2.dp.toPx(),
                    center = loupeCenter,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Loupe center crosshair
                drawLine(
                    color = Color.White,
                    start = Offset(loupeCenter.x - 8.dp.toPx(), loupeCenter.y),
                    end = Offset(loupeCenter.x + 8.dp.toPx(), loupeCenter.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(loupeCenter.x, loupeCenter.y - 8.dp.toPx()),
                    end = Offset(loupeCenter.x, loupeCenter.y + 8.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // Top guidance banner
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color(0xCC0F172A), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Colorize,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (touchPos == null) "Touch photo to pick exact color" else "Sampling color...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(activeColor))
                        .border(1.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

/**
 * Reusable color palette chips with optional custom color picker button.
 */
@Composable
fun ColorPaletteRow(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    onOpenCustomPicker: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        android.graphics.Color.WHITE,
        android.graphics.Color.BLACK,
        android.graphics.Color.RED,
        android.graphics.Color.BLUE,
        android.graphics.Color.GREEN,
        android.graphics.Color.YELLOW,
        0xFFFF6D00.toInt(), // Orange
        0xFF9C27B0.toInt(), // Purple
        0xFFE91E63.toInt(), // Pink
        0xFF00E5FF.toInt(), // Cyan
        0xFF84CC16.toInt(), // Lime
        0xFF795548.toInt()  // Brown
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onOpenCustomPicker != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.5.dp, CrimsonPrimary, CircleShape)
                    .clickable { onOpenCustomPicker() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Pick Custom Color",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        colors.forEach { col ->
            val isSelected = col == selectedColor
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(col))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) CrimsonPrimary else Color(0x44FFFFFF),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(col) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    val iconColor = if (col == android.graphics.Color.WHITE || col == android.graphics.Color.YELLOW) Color.Black else Color.White
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Aspect ratio preset selector chips for Crop tool.
 */
@Composable
fun AspectRatioChips(
    selectedPreset: CropPreset,
    onPresetSelected: (CropPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CropPreset.values().forEach { preset ->
            val isSelected = preset == selectedPreset
            FilterChip(
                selected = isSelected,
                onClick = { onPresetSelected(preset) },
                label = { Text(preset.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CrimsonPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFFCBD5E1)
                ),
                border = BorderStroke(1.dp, if (isSelected) CrimsonPrimary else Color(0xFF334155))
            )
        }
    }
}
