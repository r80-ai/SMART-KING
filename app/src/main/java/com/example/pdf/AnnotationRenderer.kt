package com.example.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.example.pdf.PdfEngine.AnnotationType
import com.example.pdf.PdfEngine.EditItem

/**
 * High-fidelity vector annotation and drawing rendering engine shared across
 * PDF Editor and Photo Editor markup tools.
 * Supports:
 * - Smooth quadratic Bézier curves (quadTo) for Pen and Highlighter
 * - Translucent blending for Highlighters
 * - Geometric shapes (Rectangle, Circle, Line) with custom fill & stroke
 * - All 7 Arrow styles (Single, Double-ended, Dashed, Curved, Block, Chevron, Filled-head)
 * - Vector Icons & Symbols (Checkmarks, Crosses, Stars, Hearts, Badges, etc.)
 * - Scalable Text & Emoji Stamps
 * - PorterDuff.Mode.CLEAR stroke erasers with layer isolation
 */
object AnnotationRenderer {

    fun renderItemsToCanvas(canvas: Canvas, items: List<EditItem>, finalW: Float, finalH: Float) {
        if (items.isEmpty()) return
        val layerSave = canvas.saveLayer(0f, 0f, finalW, finalH, null)
        for (item in items) {
            if (!item.isVisible) continue
            val sx = if (item.baseWidth > 0f) finalW / item.baseWidth else 1f
            val sy = if (item.baseHeight > 0f) finalH / item.baseHeight else 1f
            val strokeScale = (sx + sy) / 2f

            val posX = item.posX * sx
            val posY = item.posY * sy
            val endX = item.endX * sx
            val endY = item.endY * sy
            val strokeW = item.strokeWidth * strokeScale
            val scaledPoints = if (item.points.isNotEmpty()) {
                item.points.map { Pair(it.first * sx, it.second * sy) }
            } else emptyList()

            if (item.drawingBitmap != null) {
                canvas.drawBitmap(item.drawingBitmap, 0f, 0f, null)
            }

            val itemRot = item.getRotationAngle()

            // Image annotation
            if (item.imageBitmap != null) {
                canvas.save()
                val cx = posX + (item.imageWidth * sx) / 2f
                val cy = posY + (item.imageHeight * sy) / 2f
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, cx, cy)
                }
                val dstRect = Rect(posX.toInt(), posY.toInt(), (posX + item.imageWidth * sx).toInt(), (posY + item.imageHeight * sy).toInt())
                val imgPaint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                    isDither = true
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                }
                canvas.drawBitmap(item.imageBitmap, null, dstRect, imgPaint)
                canvas.restore()
            }

            // Stamp annotation
            if (item.isStamp && item.stampText.isNotBlank()) {
                canvas.save()
                val boxW = 160f * sx
                val boxH = 45f * sy
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, posX + boxW / 2f, posY + boxH / 2f)
                }
                val stampPaint = Paint().apply {
                    color = Color.rgb(220, 38, 38)
                    style = Paint.Style.STROKE
                    strokeWidth = (3f * strokeScale).coerceAtLeast(1.5f)
                    isAntiAlias = true
                }
                val textPaint = Paint().apply {
                    color = Color.rgb(220, 38, 38)
                    textSize = (22f * strokeScale).coerceAtLeast(12f)
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawRoundRect(posX, posY, posX + boxW, posY + boxH, 8f * strokeScale, 8f * strokeScale, stampPaint)
                canvas.drawText(item.stampText, posX + 12f * sx, posY + 30f * sy, textPaint)
                canvas.restore()
            } else if (item.type == AnnotationType.DRAW || item.type == AnnotationType.PEN) {
                if (scaledPoints.size >= 2) {
                    canvas.save()
                    if (itemRot != 0f) {
                        var minX = Float.MAX_VALUE
                        var maxX = -Float.MAX_VALUE
                        var minY = Float.MAX_VALUE
                        var maxY = -Float.MAX_VALUE
                        for (pt in scaledPoints) {
                            if (pt.first < minX) minX = pt.first
                            if (pt.first > maxX) maxX = pt.first
                            if (pt.second < minY) minY = pt.second
                            if (pt.second > maxY) maxY = pt.second
                        }
                        canvas.rotate(itemRot, (minX + maxX) / 2f, (minY + maxY) / 2f)
                    }
                    val drawPaint = Paint().apply {
                        color = item.colorHex
                        alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = strokeW.coerceAtLeast(1f)
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    val path = Path()
                    path.moveTo(scaledPoints[0].first, scaledPoints[0].second)
                    for (pIdx in 1 until scaledPoints.size) {
                        val prevX = scaledPoints[pIdx - 1].first
                        val prevY = scaledPoints[pIdx - 1].second
                        val currX = scaledPoints[pIdx].first
                        val currY = scaledPoints[pIdx].second
                        val midX = (prevX + currX) / 2f
                        val midY = (prevY + currY) / 2f
                        path.quadTo(prevX, prevY, midX, midY)
                    }
                    path.lineTo(scaledPoints.last().first, scaledPoints.last().second)
                    canvas.drawPath(path, drawPaint)
                    canvas.restore()
                } else if (scaledPoints.size == 1) {
                    val dotPaint = Paint().apply {
                        color = item.colorHex
                        alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(scaledPoints[0].first, scaledPoints[0].second, strokeW / 2f, dotPaint)
                }
            } else if (item.type == AnnotationType.HIGHLIGHTER) {
                canvas.save()
                val hlAlpha = (item.opacity.coerceIn(0.1f, 1f) * 255).toInt()
                if (scaledPoints.size >= 2) {
                    if (itemRot != 0f) {
                        var minX = Float.MAX_VALUE
                        var maxX = -Float.MAX_VALUE
                        var minY = Float.MAX_VALUE
                        var maxY = -Float.MAX_VALUE
                        for (pt in scaledPoints) {
                            if (pt.first < minX) minX = pt.first
                            if (pt.first > maxX) maxX = pt.first
                            if (pt.second < minY) minY = pt.second
                            if (pt.second > maxY) maxY = pt.second
                        }
                        canvas.rotate(itemRot, (minX + maxX) / 2f, (minY + maxY) / 2f)
                    }
                    val hlPaint = Paint().apply {
                        color = item.colorHex
                        alpha = hlAlpha
                        style = Paint.Style.STROKE
                        strokeWidth = strokeW.coerceAtLeast(10f)
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    val path = Path()
                    path.moveTo(scaledPoints[0].first, scaledPoints[0].second)
                    for (pIdx in 1 until scaledPoints.size) {
                        val prevX = scaledPoints[pIdx - 1].first
                        val prevY = scaledPoints[pIdx - 1].second
                        val currX = scaledPoints[pIdx].first
                        val currY = scaledPoints[pIdx].second
                        val midX = (prevX + currX) / 2f
                        val midY = (prevY + currY) / 2f
                        path.quadTo(prevX, prevY, midX, midY)
                    }
                    path.lineTo(scaledPoints.last().first, scaledPoints.last().second)
                    canvas.drawPath(path, hlPaint)
                } else if (scaledPoints.size == 1) {
                    val hlDot = Paint().apply {
                        color = item.colorHex
                        alpha = hlAlpha
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(scaledPoints[0].first, scaledPoints[0].second, strokeW / 2f, hlDot)
                } else {
                    if (itemRot != 0f) {
                        canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                    }
                    val hlPaint = Paint().apply {
                        color = item.colorHex
                        alpha = hlAlpha
                        style = Paint.Style.STROKE
                        strokeWidth = strokeW.coerceAtLeast(14f)
                        strokeCap = Paint.Cap.SQUARE
                        isAntiAlias = true
                    }
                    canvas.drawLine(posX, posY, endX, endY, hlPaint)
                }
                canvas.restore()
            } else if (item.type == AnnotationType.LINE || item.type == AnnotationType.UNDERLINE || item.type == AnnotationType.STRIKETHROUGH) {
                canvas.save()
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                }
                val linePaint = Paint().apply {
                    color = item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW.coerceAtLeast(1f)
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }
                canvas.drawLine(posX, posY, endX, endY, linePaint)
                canvas.restore()
            } else if (item.type == AnnotationType.ARROW) {
                canvas.save()
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                }
                val arrowStyle = item.stampText.ifBlank { "SINGLE" }.uppercase()
                val arrowPaint = Paint().apply {
                    color = item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW.coerceAtLeast(2.0f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                val dx = endX - posX
                val dy = endY - posY
                val angle = Math.atan2(dy.toDouble(), dx.toDouble())
                val arrowHeadLen = (strokeW * 3.8f).coerceIn(16f, 52f)

                when (arrowStyle) {
                    "DOUBLE_ENDED" -> {
                        canvas.drawLine(posX, posY, endX, endY, arrowPaint)
                        // End head
                        val x1 = (endX - arrowHeadLen * Math.cos(angle - Math.PI / 6)).toFloat()
                        val y1 = (endY - arrowHeadLen * Math.sin(angle - Math.PI / 6)).toFloat()
                        val x2 = (endX - arrowHeadLen * Math.cos(angle + Math.PI / 6)).toFloat()
                        val y2 = (endY - arrowHeadLen * Math.sin(angle + Math.PI / 6)).toFloat()
                        val headPath = Path().apply { moveTo(x1, y1); lineTo(endX, endY); lineTo(x2, y2) }
                        canvas.drawPath(headPath, arrowPaint)
                        // Start head
                        val sx1 = (posX + arrowHeadLen * Math.cos(angle - Math.PI / 6)).toFloat()
                        val sy1 = (posY + arrowHeadLen * Math.sin(angle - Math.PI / 6)).toFloat()
                        val sx2 = (posX + arrowHeadLen * Math.cos(angle + Math.PI / 6)).toFloat()
                        val sy2 = (posY + arrowHeadLen * Math.sin(angle + Math.PI / 6)).toFloat()
                        val startHeadPath = Path().apply { moveTo(sx1, sy1); lineTo(posX, posY); lineTo(sx2, sy2) }
                        canvas.drawPath(startHeadPath, arrowPaint)
                    }
                    "DASHED" -> {
                        val dashPaint = Paint(arrowPaint).apply {
                            pathEffect = DashPathEffect(floatArrayOf(16f * strokeScale, 10f * strokeScale), 0f)
                        }
                        canvas.drawLine(posX, posY, endX, endY, dashPaint)
                        val x1 = (endX - arrowHeadLen * Math.cos(angle - Math.PI / 6)).toFloat()
                        val y1 = (endY - arrowHeadLen * Math.sin(angle - Math.PI / 6)).toFloat()
                        val x2 = (endX - arrowHeadLen * Math.cos(angle + Math.PI / 6)).toFloat()
                        val y2 = (endY - arrowHeadLen * Math.sin(angle + Math.PI / 6)).toFloat()
                        val headPath = Path().apply { moveTo(x1, y1); lineTo(endX, endY); lineTo(x2, y2) }
                        canvas.drawPath(headPath, arrowPaint)
                    }
                    "CURVED" -> {
                        val midX = (posX + endX) / 2f - dy * 0.25f
                        val midY = (posY + endY) / 2f + dx * 0.25f
                        val arcPath = Path().apply {
                            moveTo(posX, posY)
                            quadTo(midX, midY, endX, endY)
                        }
                        canvas.drawPath(arcPath, arrowPaint)
                        val tangentAngle = Math.atan2((endY - midY).toDouble(), (endX - midX).toDouble())
                        val x1 = (endX - arrowHeadLen * Math.cos(tangentAngle - Math.PI / 6)).toFloat()
                        val y1 = (endY - arrowHeadLen * Math.sin(tangentAngle - Math.PI / 6)).toFloat()
                        val x2 = (endX - arrowHeadLen * Math.cos(tangentAngle + Math.PI / 6)).toFloat()
                        val y2 = (endY - arrowHeadLen * Math.sin(tangentAngle + Math.PI / 6)).toFloat()
                        val headPath = Path().apply { moveTo(x1, y1); lineTo(endX, endY); lineTo(x2, y2) }
                        canvas.drawPath(headPath, arrowPaint)
                    }
                    "BLOCK" -> {
                        val len = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        val shaftW = (strokeW * 2.2f).coerceIn(8f, 32f)
                        val headW = (strokeW * 5.0f).coerceIn(20f, 65f)
                        val headL = arrowHeadLen.coerceAtMost(len * 0.5f)
                        val blockPath = Path()
                        canvas.save()
                        canvas.translate(posX, posY)
                        canvas.rotate(Math.toDegrees(angle).toFloat())
                        blockPath.moveTo(0f, -shaftW / 2f)
                        blockPath.lineTo(len - headL, -shaftW / 2f)
                        blockPath.lineTo(len - headL, -headW / 2f)
                        blockPath.lineTo(len, 0f)
                        blockPath.lineTo(len - headL, headW / 2f)
                        blockPath.lineTo(len - headL, shaftW / 2f)
                        blockPath.lineTo(0f, shaftW / 2f)
                        blockPath.close()
                        if (item.fillColorHex != null) {
                            val fillP = Paint().apply {
                                color = item.fillColorHex
                                alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                                style = Paint.Style.FILL
                                isAntiAlias = true
                            }
                            canvas.drawPath(blockPath, fillP)
                        }
                        canvas.drawPath(blockPath, arrowPaint)
                        canvas.restore()
                    }
                    "CHEVRON" -> {
                        canvas.drawLine(posX, posY, endX, endY, arrowPaint)
                        // Outer chevron
                        val x1 = (endX - arrowHeadLen * Math.cos(angle - Math.PI / 6)).toFloat()
                        val y1 = (endY - arrowHeadLen * Math.sin(angle - Math.PI / 6)).toFloat()
                        val x2 = (endX - arrowHeadLen * Math.cos(angle + Math.PI / 6)).toFloat()
                        val y2 = (endY - arrowHeadLen * Math.sin(angle + Math.PI / 6)).toFloat()
                        val headPath = Path().apply { moveTo(x1, y1); lineTo(endX, endY); lineTo(x2, y2) }
                        canvas.drawPath(headPath, arrowPaint)
                        // Inner chevron offset
                        val offX = (arrowHeadLen * 0.45f * Math.cos(angle)).toFloat()
                        val offY = (arrowHeadLen * 0.45f * Math.sin(angle)).toFloat()
                        val headPath2 = Path().apply { moveTo(x1 - offX, y1 - offY); lineTo(endX - offX, endY - offY); lineTo(x2 - offX, y2 - offY) }
                        canvas.drawPath(headPath2, arrowPaint)
                    }
                    "FILLED_HEAD" -> {
                        val lineEndX = (endX - arrowHeadLen * 0.7f * Math.cos(angle)).toFloat()
                        val lineEndY = (endY - arrowHeadLen * 0.7f * Math.sin(angle)).toFloat()
                        canvas.drawLine(posX, posY, lineEndX, lineEndY, arrowPaint)
                        val x1 = (endX - arrowHeadLen * Math.cos(angle - Math.PI / 6)).toFloat()
                        val y1 = (endY - arrowHeadLen * Math.sin(angle - Math.PI / 6)).toFloat()
                        val x2 = (endX - arrowHeadLen * Math.cos(angle + Math.PI / 6)).toFloat()
                        val y2 = (endY - arrowHeadLen * Math.sin(angle + Math.PI / 6)).toFloat()
                        val headPath = Path().apply {
                            moveTo(x1, y1)
                            lineTo(endX, endY)
                            lineTo(x2, y2)
                            close()
                        }
                        val solidHeadPaint = Paint().apply {
                            color = item.colorHex
                            alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                            style = Paint.Style.FILL
                            isAntiAlias = true
                        }
                        canvas.drawPath(headPath, solidHeadPaint)
                    }
                    else -> { // "SINGLE" default
                        canvas.drawLine(posX, posY, endX, endY, arrowPaint)
                        val x1 = (endX - arrowHeadLen * Math.cos(angle - Math.PI / 6)).toFloat()
                        val y1 = (endY - arrowHeadLen * Math.sin(angle - Math.PI / 6)).toFloat()
                        val x2 = (endX - arrowHeadLen * Math.cos(angle + Math.PI / 6)).toFloat()
                        val y2 = (endY - arrowHeadLen * Math.sin(angle + Math.PI / 6)).toFloat()
                        val headPath = Path().apply {
                            moveTo(x1, y1)
                            lineTo(endX, endY)
                            lineTo(x2, y2)
                        }
                        canvas.drawPath(headPath, arrowPaint)
                    }
                }
                canvas.restore()
            } else if (item.type == AnnotationType.RECTANGLE) {
                canvas.save()
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                }
                val left = Math.min(posX, endX)
                val right = Math.max(posX, endX)
                val top = Math.min(posY, endY)
                val bottom = Math.max(posY, endY)
                if (item.fillColorHex != null) {
                    val fillPaint = Paint().apply {
                        color = item.fillColorHex
                        alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawRect(left, top, right, bottom, fillPaint)
                }
                val rectPaint = Paint().apply {
                    color = item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW.coerceAtLeast(1f)
                    isAntiAlias = true
                }
                canvas.drawRect(left, top, right, bottom, rectPaint)
                canvas.restore()
            } else if (item.type == AnnotationType.CIRCLE) {
                canvas.save()
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                }
                val left = Math.min(posX, endX)
                val right = Math.max(posX, endX)
                val top = Math.min(posY, endY)
                val bottom = Math.max(posY, endY)
                if (item.fillColorHex != null) {
                    val fillPaint = Paint().apply {
                        color = item.fillColorHex
                        alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawOval(left, top, right, bottom, fillPaint)
                }
                val ovalPaint = Paint().apply {
                    color = item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW.coerceAtLeast(1f)
                    isAntiAlias = true
                }
                canvas.drawOval(left, top, right, bottom, ovalPaint)
                canvas.restore()
            } else if (item.type == AnnotationType.CHECKMARK) {
                canvas.save()
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                }
                val left = Math.min(posX, endX)
                val right = Math.max(posX, endX)
                val top = Math.min(posY, endY)
                val bottom = Math.max(posY, endY)
                val w = (right - left).coerceAtLeast(20f * sx)
                val h = (bottom - top).coerceAtLeast(20f * sy)
                val tickPath = Path().apply {
                    moveTo(left + 0.12f * w, top + 0.52f * h)
                    cubicTo(
                        left + 0.22f * w, top + 0.68f * h,
                        left + 0.32f * w, top + 0.94f * h,
                        left + 0.42f * w, top + 0.90f * h
                    )
                    cubicTo(
                        left + 0.55f * w, top + 0.75f * h,
                        left + 0.80f * w, top + 0.28f * h,
                        left + 0.95f * w, top + 0.08f * h
                    )
                }
                val tickPaint = Paint().apply {
                    color = item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = (strokeW * 1.5f).coerceAtLeast(3.2f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                canvas.drawPath(tickPath, tickPaint)
                canvas.restore()
            } else if (item.type == AnnotationType.STAR) {
                canvas.save()
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                }
                val left = Math.min(posX, endX)
                val right = Math.max(posX, endX)
                val top = Math.min(posY, endY)
                val bottom = Math.max(posY, endY)
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val outerR = Math.min(right - left, bottom - top) / 2f
                val innerR = outerR * 0.42f
                val starPath = Path()
                for (i in 0 until 10) {
                    val angle = (Math.PI / 5.0) * i - Math.PI / 2.0
                    val r = if (i % 2 == 0) outerR else innerR
                    val px = (cx + r * Math.cos(angle)).toFloat()
                    val py = (cy + r * Math.sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()
                if (item.fillColorHex != null) {
                    val fillPaint = Paint().apply {
                        color = item.fillColorHex
                        alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawPath(starPath, fillPaint)
                }
                val starPaint = Paint().apply {
                    color = item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW.coerceAtLeast(1.5f)
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                canvas.drawPath(starPath, starPaint)
                canvas.restore()
            } else if (item.type == AnnotationType.HEART) {
                canvas.save()
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                }
                val left = Math.min(posX, endX)
                val right = Math.max(posX, endX)
                val top = Math.min(posY, endY)
                val bottom = Math.max(posY, endY)
                val w = right - left
                val h = bottom - top
                val heartPath = Path().apply {
                    moveTo(left + w * 0.5f, top + h * 0.85f)
                    cubicTo(left + w * 0.1f, top + h * 0.55f, left, top + h * 0.25f, left + w * 0.25f, top + h * 0.15f)
                    cubicTo(left + w * 0.45f, top + h * 0.08f, left + w * 0.5f, top + h * 0.35f, left + w * 0.5f, top + h * 0.35f)
                    cubicTo(left + w * 0.5f, top + h * 0.35f, left + w * 0.55f, top + h * 0.08f, left + w * 0.75f, top + h * 0.15f)
                    cubicTo(left + w * 1.0f, top + h * 0.25f, left + w * 0.9f, top + h * 0.55f, left + w * 0.5f, top + h * 0.85f)
                    close()
                }
                if (item.fillColorHex != null) {
                    val fillPaint = Paint().apply {
                        color = item.fillColorHex
                        alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawPath(heartPath, fillPaint)
                }
                val heartPaint = Paint().apply {
                    color = item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW.coerceAtLeast(1.5f)
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                canvas.drawPath(heartPath, heartPaint)
                canvas.restore()
            } else if (item.type == AnnotationType.CROSS) {
                canvas.save()
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                }
                val left = Math.min(posX, endX)
                val right = Math.max(posX, endX)
                val top = Math.min(posY, endY)
                val bottom = Math.max(posY, endY)
                val crossPaint = Paint().apply {
                    color = item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW.coerceAtLeast(2f)
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }
                canvas.drawLine(left + 0.15f * (right - left), top + 0.15f * (bottom - top), right - 0.15f * (right - left), bottom - 0.15f * (bottom - top), crossPaint)
                canvas.drawLine(right - 0.15f * (right - left), top + 0.15f * (bottom - top), left + 0.15f * (right - left), bottom - 0.15f * (bottom - top), crossPaint)
                canvas.restore()
            } else if (item.type == AnnotationType.SMILEY) {
                canvas.save()
                if (itemRot != 0f) canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                val left = Math.min(posX, endX); val right = Math.max(posX, endX)
                val top = Math.min(posY, endY); val bottom = Math.max(posY, endY)
                val cx = (left + right) / 2f; val cy = (top + bottom) / 2f
                val r = Math.min(right - left, bottom - top) / 2f
                val p = Paint().apply { color = item.colorHex; alpha = (item.opacity * 255).toInt(); style = Paint.Style.STROKE; strokeWidth = strokeW.coerceAtLeast(1.5f); isAntiAlias = true }
                canvas.drawCircle(cx, cy, r, p)
                canvas.drawCircle(cx - r * 0.35f, cy - r * 0.3f, r * 0.1f, p)
                canvas.drawCircle(cx + r * 0.35f, cy - r * 0.3f, r * 0.1f, p)
                val mouth = RectF(cx - r * 0.5f, cy - r * 0.1f, cx + r * 0.5f, cy + r * 0.5f)
                canvas.drawArc(mouth, 0f, 180f, false, p)
                canvas.restore()
            } else if (item.type == AnnotationType.THUMBS_UP) {
                canvas.save()
                if (itemRot != 0f) canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                val left = Math.min(posX, endX); val w = Math.abs(endX - posX); val top = Math.min(posY, endY); val h = Math.abs(endY - posY)
                val p = Paint().apply { color = item.colorHex; alpha = (item.opacity * 255).toInt(); style = Paint.Style.STROKE; strokeWidth = strokeW.coerceAtLeast(1.5f); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true }
                val path = Path().apply {
                    moveTo(left + w * 0.3f, top + h * 0.4f); lineTo(left + w * 0.3f, top + h * 0.9f)
                    lineTo(left + w * 0.1f, top + h * 0.9f); lineTo(left + w * 0.1f, top + h * 0.4f); close()
                    moveTo(left + w * 0.3f, top + h * 0.4f); lineTo(left + w * 0.45f, top + h * 0.1f)
                    cubicTo(left + w * 0.6f, top, left + w * 0.7f, top + h * 0.1f, left + w * 0.6f, top + h * 0.3f)
                    lineTo(left + w * 0.9f, top + h * 0.3f); cubicTo(left + w, top + h * 0.3f, left + w, top + h * 0.5f, left + w * 0.9f, top + h * 0.5f)
                    lineTo(left + w * 0.85f, top + h * 0.5f); cubicTo(left + w * 0.95f, top + h * 0.5f, left + w * 0.95f, top + h * 0.7f, left + w * 0.85f, top + h * 0.7f)
                    lineTo(left + w * 0.8f, top + h * 0.7f); cubicTo(left + w * 0.9f, top + h * 0.7f, left + w * 0.9f, top + h * 0.9f, left + w * 0.8f, top + h * 0.9f)
                    lineTo(left + w * 0.3f, top + h * 0.9f)
                }
                canvas.drawPath(path, p)
                canvas.restore()
            } else if (item.type == AnnotationType.WARNING) {
                canvas.save()
                if (itemRot != 0f) canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                val left = Math.min(posX, endX); val w = Math.abs(endX - posX); val top = Math.min(posY, endY); val h = Math.abs(endY - posY)
                val p = Paint().apply { color = item.colorHex; alpha = (item.opacity * 255).toInt(); style = Paint.Style.STROKE; strokeWidth = strokeW.coerceAtLeast(2f); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; isAntiAlias = true }
                val path = Path().apply { moveTo(left + w * 0.5f, top + h * 0.1f); lineTo(left + w * 0.9f, top + h * 0.9f); lineTo(left + w * 0.1f, top + h * 0.9f); close() }
                canvas.drawPath(path, p)
                canvas.drawLine(left + w * 0.5f, top + h * 0.4f, left + w * 0.5f, top + h * 0.65f, p)
                canvas.drawPoint(left + w * 0.5f, top + h * 0.8f, p)
                canvas.restore()
            } else if (item.type == AnnotationType.OCR_REPLACEMENT && item.maskRect != null) {
                val paint = Paint().apply {
                    color = item.maskColor
                    style = Paint.Style.FILL
                }
                canvas.drawRect(item.maskRect, paint)
                
                val p = Paint().apply {
                    color = item.colorHex
                    textSize = item.fontSize
                    val baseTypeface = when (item.fontFamily) {
                        "HANDWRITING", "Handwriting", "CURSIVE", "Cursive" -> try { Typeface.create("casual", Typeface.NORMAL) } catch (_: Exception) { Typeface.create("cursive", Typeface.NORMAL) }
                        "SERIF", "Serif" -> Typeface.SERIF
                        "MONOSPACE", "Monospace" -> Typeface.MONOSPACE
                        "BENGALI", "Bengali" -> Typeface.SANS_SERIF
                        else -> Typeface.DEFAULT
                    }
                    val style = when {
                        item.isBold && item.isItalic -> Typeface.BOLD_ITALIC
                        item.isBold -> Typeface.BOLD
                        item.isItalic -> Typeface.ITALIC
                        else -> Typeface.NORMAL
                    }
                    typeface = Typeface.create(baseTypeface, style)
                    textAlign = when (item.alignment) {
                        "CENTER" -> Paint.Align.CENTER
                        "RIGHT" -> Paint.Align.RIGHT
                        else -> Paint.Align.LEFT
                    }
                }
                canvas.drawText(item.text, item.posX, item.posY, p)
            } else if (item.type == AnnotationType.WATERMARK) {
                val fontSz = (item.fontSize * strokeScale).coerceAtLeast(12f)
                val wmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorHex
                    alpha = (item.opacity * 255).toInt().coerceIn(0, 255)
                    textSize = fontSz
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }

                if (item.isTiled) {
                    val stepX = (finalW / 3f).coerceAtLeast(150f)
                    val stepY = (finalH / 4f).coerceAtLeast(150f)
                    for (col in 0..3) {
                        for (row in 0..5) {
                            canvas.save()
                            val tx = col * stepX
                            val ty = row * stepY
                            canvas.rotate(itemRot, tx, ty)
                            canvas.drawText(item.text, tx, ty, wmPaint)
                            canvas.restore()
                        }
                    }
                } else {
                    canvas.save()
                    canvas.rotate(itemRot, posX, posY)
                    canvas.drawText(item.text, posX, posY, wmPaint)
                    canvas.restore()
                }
            } else if (item.type == AnnotationType.ERASER) {
                if (scaledPoints.size >= 2) {
                    val erasePaint = Paint().apply {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        style = Paint.Style.STROKE
                        strokeWidth = strokeW.coerceAtLeast(4f)
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    val path = Path()
                    path.moveTo(scaledPoints[0].first, scaledPoints[0].second)
                    for (pIdx in 1 until scaledPoints.size) {
                        val prevX = scaledPoints[pIdx - 1].first
                        val prevY = scaledPoints[pIdx - 1].second
                        val currX = scaledPoints[pIdx].first
                        val currY = scaledPoints[pIdx].second
                        val midX = (prevX + currX) / 2f
                        val midY = (prevY + currY) / 2f
                        path.quadTo(prevX, prevY, midX, midY)
                    }
                    path.lineTo(scaledPoints.last().first, scaledPoints.last().second)
                    canvas.drawPath(path, erasePaint)
                } else if (scaledPoints.size == 1) {
                    val eraseCircle = Paint().apply {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(scaledPoints[0].first, scaledPoints[0].second, strokeW / 2f, eraseCircle)
                }
            } else if (item.type == AnnotationType.ICON) {
                canvas.save()
                if (itemRot != 0f) {
                    canvas.rotate(itemRot, (posX + endX) / 2f, (posY + endY) / 2f)
                }
                val left = Math.min(posX, endX)
                val right = Math.max(posX, endX)
                val top = Math.min(posY, endY)
                val bottom = Math.max(posY, endY)
                val w = (right - left).coerceAtLeast(16f * strokeScale)
                val h = (bottom - top).coerceAtLeast(16f * strokeScale)
                val iconPaint = Paint().apply {
                    color = item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW.coerceAtLeast(2f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                val fillPaint = Paint().apply {
                    color = item.fillColorHex ?: item.colorHex
                    alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                PdfEngine.drawVectorIconOnCanvas(canvas, item.stampText.ifBlank { "TICK_CLEAN" }, left, top, w, h, iconPaint, if (item.fillColorHex != null) fillPaint else null)
                canvas.restore()
            } else if (item.type == AnnotationType.TEXT || item.text.isNotBlank()) {
                val scaledItem = item.copy(
                    posX = posX,
                    posY = posY,
                    fontSize = item.fontSize * strokeScale,
                    boxWidth = if (item.boxWidth > 0f) item.boxWidth * strokeScale else 0f,
                    boxHeight = if (item.boxHeight > 0f) item.boxHeight * strokeScale else 0f
                )
                com.example.ui.screens.TextEffectRenderer.drawPdfEditItem(canvas, scaledItem, strokeScale)
            }
        }
        canvas.restoreToCount(layerSave)
    }
}
