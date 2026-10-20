package com.example.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.example.pdf.PdfEngine
import kotlin.math.abs
import kotlin.math.max

/**
 * TextOverlayView provides a high-performance Android View canvas that sits
 * on top of the PDF render canvas / viewer.
 *
 * Capabilities:
 * - Natural top placement via layout elevation and bringToFront()
 * - Hardware-accelerated onDraw execution of text glyphs and bounding box
 * - Robust touch handling with requestDisallowInterceptTouchEvent
 * - Drag & Move freely within PDF page
 * - Corner Resizing with free stretch (independent width/height and font scaling)
 * - Matrix / Zoom synchronization via Matrix.invert()
 */
class TextOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val layers = mutableListOf<PdfEngine.EditItem>()
    var selectedLayerId: String? = null
        private set

    var onLayerSelected: ((String?) -> Unit)? = null
    var onLayerUpdated: ((PdfEngine.EditItem) -> Unit)? = null
    var onLayerEditRequested: ((PdfEngine.EditItem) -> Unit)? = null
    var onHistoryPush: (() -> Unit)? = null

    // Transformation Matrix for Zoom / Pan Synchronization
    var viewerMatrix: Matrix = Matrix()
        private set
    private var inverseMatrix: Matrix = Matrix()

    // Paint instances
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val boundingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#3B82F6")
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Touch interaction state
    private var activeMode = TouchMode.NONE
    private var activeResizeCorner = -1
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var initialItem: PdfEngine.EditItem? = null
    private var initialBoxW = 0f
    private var initialBoxH = 0f
    private var initialFontSize = 28f

    private enum class TouchMode {
        NONE, DRAG, RESIZE
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val pt = mapTouchCoordinates(e.x, e.y)
            val touched = findLayerAt(pt.x, pt.y)
            if (touched != null && !touched.isLocked) {
                onLayerEditRequested?.invoke(touched)
                return true
            }
            return super.onDoubleTap(e)
        }
    })

    init {
        isFocusable = true
        isClickable = true
        elevation = 16f * resources.displayMetrics.density
    }

    fun setTransformationMatrix(matrix: Matrix) {
        viewerMatrix.set(matrix)
        if (!viewerMatrix.invert(inverseMatrix)) {
            inverseMatrix.reset()
        }
        invalidate()
    }

    fun setZoomAndPan(scale: Float, panX: Float = 0f, panY: Float = 0f) {
        val m = Matrix()
        m.postScale(scale, scale)
        m.postTranslate(panX, panY)
        setTransformationMatrix(m)
    }

    fun mapTouchCoordinates(rawX: Float, rawY: Float): PointF {
        if (inverseMatrix.isIdentity) {
            return PointF(rawX, rawY)
        }
        val pts = floatArrayOf(rawX, rawY)
        inverseMatrix.mapPoints(pts)
        return PointF(pts[0], pts[1])
    }

    fun addOrSelectLayer(item: PdfEngine.EditItem) {
        visibility = VISIBLE
        bringToFront()
        val existingIndex = layers.indexOfFirst { it.id == item.id }
        if (existingIndex != -1) {
            layers[existingIndex] = item
        } else {
            layers.add(item)
        }
        selectedLayerId = item.id
        postInvalidate()
    }

    fun updateLayers(items: List<PdfEngine.EditItem>, selectedId: String?) {
        layers.clear()
        layers.addAll(items)
        selectedLayerId = selectedId
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (layers.isEmpty()) return

        val density = resources.displayMetrics.density
        val scaledDensity = resources.displayMetrics.scaledDensity
        val stroke3dp = 3f * density
        val handleRadius = 7f * density

        for (layer in layers) {
            if (!layer.isVisible) continue

            val isSelected = layer.id == selectedLayerId

            // Dynamic font measurement
            val fontSizePx = if (layer.fontSize < 60f) {
                layer.fontSize * scaledDensity
            } else {
                layer.fontSize
            }.coerceAtLeast(18f)

            val tf = when {
                layer.customFontPath != null -> try {
                    Typeface.createFromFile(layer.customFontPath)
                } catch (_: Exception) { Typeface.DEFAULT }
                layer.fontFamily.equals("SERIF", ignoreCase = true) -> Typeface.SERIF
                layer.fontFamily.equals("MONOSPACE", ignoreCase = true) -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }

            val style = when {
                layer.isBold && layer.isItalic -> Typeface.BOLD_ITALIC
                layer.isBold -> Typeface.BOLD
                layer.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            textPaint.apply {
                this.typeface = Typeface.create(tf, style)
                this.textSize = fontSizePx
                this.color = if (layer.colorHex != 0) layer.colorHex else Color.RED
                this.alpha = (layer.opacity.coerceIn(0.05f, 1f) * 255).toInt().coerceIn(0, 255)
                this.isUnderlineText = layer.isUnderline
                this.isStrikeThruText = layer.isStrikethrough
                this.style = Paint.Style.FILL
                this.isAntiAlias = true
                this.textScaleX = 1f
            }

            val lines = (if (layer.text.isEmpty()) " " else layer.text).split("\n")
            var maxLineW = 0f
            for (l in lines) {
                val w = textPaint.measureText(l)
                if (w > maxLineW) maxLineW = w
            }

            val fm = textPaint.fontMetrics
            val lineHeight = (fm.descent - fm.ascent) * if (layer.lineSpacingMultiplier > 0f) layer.lineSpacingMultiplier else 1.15f
            val natW = maxLineW + layer.backgroundPadding * 2f + 24f
            val natH = lines.size * lineHeight + layer.backgroundPadding * 2f + 18f
            val boxW = if (layer.boxWidth > 0f) layer.boxWidth else natW
            val boxH = if (layer.boxHeight > 0f) layer.boxHeight else natH

            // Allow independent width scaling (stretching/narrowing text)
            val availW = boxW - layer.backgroundPadding * 2f - 24f
            if (availW > 0f && maxLineW > 0f) {
                val stretch = (availW / maxLineW).coerceIn(0.25f, 4.0f)
                textPaint.textScaleX = stretch
            }

            canvas.save()
            val centerX = layer.posX + boxW / 2f
            val centerY = layer.posY + boxH / 2f
            val rot = layer.getRotationAngle()
            if (rot != 0f) {
                canvas.rotate(rot, centerX, centerY)
            }

            // Optional background fill
            if (layer.backgroundColorHex != 0) {
                bgPaint.color = layer.backgroundColorHex
                bgPaint.alpha = (layer.opacity * Color.alpha(layer.backgroundColorHex)).toInt().coerceIn(0, 255)
                if (layer.backgroundCornerRadius > 0f) {
                    canvas.drawRoundRect(
                        layer.posX, layer.posY, layer.posX + boxW, layer.posY + boxH,
                        layer.backgroundCornerRadius, layer.backgroundCornerRadius, bgPaint
                    )
                } else {
                    canvas.drawRect(layer.posX, layer.posY, layer.posX + boxW, layer.posY + boxH, bgPaint)
                }
            }

            // Draw text lines
            var curY = layer.posY + layer.backgroundPadding - fm.ascent + 8f
            for (line in lines) {
                val lineW = textPaint.measureText(line)
                val curX = when (layer.alignment.uppercase()) {
                    "CENTER" -> layer.posX + (boxW - lineW) / 2f
                    "RIGHT" -> layer.posX + boxW - lineW - layer.backgroundPadding - 12f
                    else -> layer.posX + layer.backgroundPadding + 12f
                }
                canvas.drawText(line, curX, curY, textPaint)
                curY += lineHeight
            }

            // Bounding box display and interactive handles
            if (isSelected) {
                boundingPaint.strokeWidth = stroke3dp
                boundingPaint.color = Color.parseColor("#3B82F6") // ToolBlue Accent
                canvas.drawRect(layer.posX, layer.posY, layer.posX + boxW, layer.posY + boxH, boundingPaint)

                // 4 corner handles
                val corners = listOf(
                    Pair(layer.posX, layer.posY),
                    Pair(layer.posX + boxW, layer.posY),
                    Pair(layer.posX, layer.posY + boxH),
                    Pair(layer.posX + boxW, layer.posY + boxH)
                )
                handleStrokePaint.strokeWidth = 2f * density
                for (corner in corners) {
                    canvas.drawCircle(corner.first, corner.second, handleRadius, handlePaint)
                    canvas.drawCircle(corner.first, corner.second, handleRadius, handleStrokePaint)
                }
            }

            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        val pt = mapTouchCoordinates(event.x, event.y)
        val x = pt.x
        val y = pt.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y

                // 1. Check if touch intersects any of the 4 corner resize handles on the active layer
                val selItem = layers.find { it.id == selectedLayerId }
                if (selItem != null && !selItem.isLocked) {
                    val cornerIdx = getHitCorner(selItem, x, y)
                    if (cornerIdx != -1) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        onHistoryPush?.invoke()
                        activeMode = TouchMode.RESIZE
                        activeResizeCorner = cornerIdx
                        initialItem = selItem
                        initialBoxW = getLayerBoxWidth(selItem)
                        initialBoxH = getLayerBoxHeight(selItem)
                        initialFontSize = selItem.fontSize
                        invalidate()
                        return true
                    }
                }

                // 2. Check if touch intersects the text bounding box of any layer
                val hit = findLayerAt(x, y)
                if (hit != null) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    onHistoryPush?.invoke()
                    selectedLayerId = hit.id
                    onLayerSelected?.invoke(hit.id)
                    activeMode = if (!hit.isLocked) TouchMode.DRAG else TouchMode.NONE
                    initialItem = hit
                    initialBoxW = getLayerBoxWidth(hit)
                    initialBoxH = getLayerBoxHeight(hit)
                    initialFontSize = hit.fontSize
                    invalidate()
                    return true
                } else {
                    if (selectedLayerId != null) {
                        selectedLayerId = null
                        onLayerSelected?.invoke(null)
                        invalidate()
                    }
                    activeMode = TouchMode.NONE
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastTouchX
                val dy = y - lastTouchY
                lastTouchX = x
                lastTouchY = y

                val curItem = layers.find { it.id == selectedLayerId }
                if (curItem != null && !curItem.isLocked) {
                    when (activeMode) {
                        TouchMode.DRAG -> {
                            val updated = curItem.copy(
                                posX = curItem.posX + dx,
                                posY = curItem.posY + dy
                            )
                            updateSingleLayer(updated)
                            onLayerUpdated?.invoke(updated)
                            invalidate()
                            return true
                        }

                        TouchMode.RESIZE -> {
                            val initItm = initialItem ?: curItem
                            val initW = initialBoxW.coerceAtLeast(30f)
                            val initH = initialBoxH.coerceAtLeast(20f)
                            val initFSize = initialFontSize.coerceAtLeast(10f)

                            val curW = if (curItem.boxWidth > 0f) curItem.boxWidth else initW
                            val curH = if (curItem.boxHeight > 0f) curItem.boxHeight else initH
                            var newX = curItem.posX
                            var newY = curItem.posY
                            var newW = curW
                            var newH = curH

                            when (activeResizeCorner) {
                                0 -> { // Top-Left
                                    newW = max(30f, curW - dx)
                                    newH = max(20f, curH - dy)
                                    newX = curItem.posX + (curW - newW)
                                    newY = curItem.posY + (curH - newH)
                                }
                                1 -> { // Top-Right
                                    newW = max(30f, curW + dx)
                                    newH = max(20f, curH - dy)
                                    newY = curItem.posY + (curH - newH)
                                }
                                2 -> { // Bottom-Left
                                    newW = max(30f, curW - dx)
                                    newH = max(20f, curH + dy)
                                    newX = curItem.posX + (curW - newW)
                                }
                                3 -> { // Bottom-Right
                                    newW = max(30f, curW + dx)
                                    newH = max(20f, curH + dy)
                                }
                            }

                            // Dynamic proportional font size scaling based on handle drag distance
                            val scaleRatioW = newW / initW
                            val scaleRatioH = newH / initH
                            val avgScale = (scaleRatioW + scaleRatioH) / 2f
                            val newFontSize = (initFSize * avgScale).coerceIn(10f, 200f)

                            val updated = curItem.copy(
                                posX = newX,
                                posY = newY,
                                boxWidth = newW,
                                boxHeight = newH,
                                fontSize = newFontSize
                            )
                            updateSingleLayer(updated)
                            onLayerUpdated?.invoke(updated)
                            invalidate()
                            return true
                        }

                        TouchMode.NONE -> {}
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                activeMode = TouchMode.NONE
                activeResizeCorner = -1
                initialItem = null
            }
        }

        return super.onTouchEvent(event)
    }

    private fun updateSingleLayer(updated: PdfEngine.EditItem) {
        val idx = layers.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            layers[idx] = updated
        }
    }

    private fun getLayerBoxWidth(layer: PdfEngine.EditItem): Float {
        val scaledDensity = resources.displayMetrics.scaledDensity
        val fontSizePx = if (layer.fontSize < 60f) layer.fontSize * scaledDensity else layer.fontSize
        textPaint.textSize = fontSizePx.coerceAtLeast(18f)
        val lines = (if (layer.text.isEmpty()) " " else layer.text).split("\n")
        var maxLineW = 0f
        for (l in lines) {
            val w = textPaint.measureText(l)
            if (w > maxLineW) maxLineW = w
        }
        val natW = maxLineW + layer.backgroundPadding * 2f + 24f
        return if (layer.boxWidth > 0f) layer.boxWidth else natW
    }

    private fun getLayerBoxHeight(layer: PdfEngine.EditItem): Float {
        val scaledDensity = resources.displayMetrics.scaledDensity
        val fontSizePx = if (layer.fontSize < 60f) layer.fontSize * scaledDensity else layer.fontSize
        textPaint.textSize = fontSizePx.coerceAtLeast(18f)
        val lines = (if (layer.text.isEmpty()) " " else layer.text).split("\n")
        val fm = textPaint.fontMetrics
        val lineHeight = (fm.descent - fm.ascent) * if (layer.lineSpacingMultiplier > 0f) layer.lineSpacingMultiplier else 1.15f
        val natH = lines.size * lineHeight + layer.backgroundPadding * 2f + 18f
        return if (layer.boxHeight > 0f) layer.boxHeight else natH
    }

    private fun findLayerAt(x: Float, y: Float): PdfEngine.EditItem? {
        for (i in layers.indices.reversed()) {
            val item = layers[i]
            if (!item.isVisible) continue
            val w = getLayerBoxWidth(item)
            val h = getLayerBoxHeight(item)
            val cx = item.posX + w / 2f
            val cy = item.posY + h / 2f
            val rot = item.getRotationAngle()
            val checkPt = if (rot != 0f) unrotatePoint(x, y, cx, cy, rot) else PointF(x, y)
            val rect = RectF(item.posX, item.posY, item.posX + w, item.posY + h)
            if (rect.contains(checkPt.x, checkPt.y)) {
                return item
            }
        }
        return null
    }

    private fun getHitCorner(item: PdfEngine.EditItem, x: Float, y: Float): Int {
        val w = getLayerBoxWidth(item)
        val h = getLayerBoxHeight(item)
        val hitRadius = max(36f * resources.displayMetrics.density, 54f)

        val cx = item.posX + w / 2f
        val cy = item.posY + h / 2f
        val rot = item.getRotationAngle()
        val checkPt = if (rot != 0f) unrotatePoint(x, y, cx, cy, rot) else PointF(x, y)

        val corners = listOf(
            Pair(item.posX, item.posY),
            Pair(item.posX + w, item.posY),
            Pair(item.posX, item.posY + h),
            Pair(item.posX + w, item.posY + h)
        )
        for (i in corners.indices) {
            val c = corners[i]
            if (abs(checkPt.x - c.first) <= hitRadius && abs(checkPt.y - c.second) <= hitRadius) {
                return i
            }
        }
        return -1
    }

    private fun unrotatePoint(px: Float, py: Float, cx: Float, cy: Float, angleDeg: Float): PointF {
        val rad = Math.toRadians(-angleDeg.toDouble())
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        val dx = px - cx
        val dy = py - cy
        val rx = (dx * cos - dy * sin).toFloat() + cx
        val ry = (dx * sin + dy * cos).toFloat() + cy
        return PointF(rx, ry)
    }
}
