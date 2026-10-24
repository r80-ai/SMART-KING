package com.example.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.pdf.PdfEngine
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * AnnotationCanvasOverlayView provides a dedicated, high-performance Android View canvas
 * positioned right above the rendered PDF page.
 *
 * Capabilities:
 * - Smooth quadratic Bézier curves for Freehand Pen and Highlighter.
 * - Manual drag-to-create geometric shapes (Rectangle, Circle/Ellipse, Line, Arrow) with live preview.
 * - Continuous geometry-based Eraser that ONLY intersects and removes annotation strokes/shapes
 *   without EVER modifying, whitening, or cutting the original PDF page content.
 * - Independent per-page annotation data models.
 * - Touch interception management (requestDisallowInterceptTouchEvent) during drawing/erasing.
 * - Undo, Redo, and Clear support.
 */
class AnnotationCanvasOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ToolMode {
        PEN, HIGHLIGHT, SHAPE, ERASER
    }

    enum class ShapeType {
        RECTANGLE, CIRCLE, LINE, ARROW
    }

    enum class EraserSize(val radiusDp: Float) {
        SMALL(12f),
        MEDIUM(24f),
        LARGE(44f)
    }

    // Active tool state
    var isInteractionEnabled: Boolean = false
        set(value) {
            field = value
            isFocusable = value
            isClickable = value
            invalidate()
        }

    var currentTool: ToolMode = ToolMode.PEN
        set(value) {
            field = value
            invalidate()
        }

    var currentShape: ShapeType = ShapeType.RECTANGLE
        set(value) {
            field = value
            invalidate()
        }

    var eraserSize: EraserSize = EraserSize.MEDIUM
        set(value) {
            field = value
            invalidate()
        }

    // Styling parameters
    var penColor: Int = Color.rgb(37, 99, 235) // Default Royal Blue
    var penStrokeWidth: Float = 4f
    var penOpacity: Float = 1.0f

    var highlightColor: Int = Color.rgb(250, 204, 21) // Highlighting Yellow
    var highlightStrokeWidth: Float = 22f
    var highlightOpacity: Float = 0.40f

    var shapeColor: Int = Color.rgb(220, 38, 38) // Crimson
    var shapeStrokeWidth: Float = 3.5f
    var shapeOpacity: Float = 1.0f
    var shapeFillColor: Int? = null

    // Callbacks for ViewModel & history synchronization
    var onAnnotationAdded: ((PdfEngine.EditItem) -> Unit)? = null
    var onAnnotationsRemoved: ((List<PdfEngine.EditItem>) -> Unit)? = null
    var onHistoryPush: (() -> Unit)? = null

    // Per-page annotation storage
    var currentPageIndex: Int = 0
        private set
    private val pageAnnotations = mutableMapOf<Int, MutableList<PdfEngine.EditItem>>()

    // In-progress drawing gesture state
    private val activePoints = mutableListOf<Pair<Float, Float>>()
    private val activePath = Path()
    private var lastX = 0f
    private var lastY = 0f

    // In-progress shape gesture state
    private var isDrawingShape = false
    private var shapeStartX = 0f
    private var shapeStartY = 0f
    private var shapeCurrentX = 0f
    private var shapeCurrentY = 0f

    // In-progress eraser gesture state
    private var isErasing = false
    private var eraserTouchX = 0f
    private var eraserTouchY = 0f
    private val removedDuringCurrentGesture = mutableListOf<PdfEngine.EditItem>()

    // Paints
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val previewShapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val eraserIndicatorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(60, 239, 68, 68)
    }

    private val eraserIndicatorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(239, 68, 68)
        strokeWidth = 2f
    }

    init {
        elevation = 8f * resources.displayMetrics.density
    }

    /**
     * Updates the active page annotations from external state
     */
    fun setAnnotations(items: List<PdfEngine.EditItem>, pageIndex: Int) {
        currentPageIndex = pageIndex
        pageAnnotations[pageIndex] = items.toMutableList()
        invalidate()
    }

    fun getCurrentPageAnnotations(): List<PdfEngine.EditItem> {
        return pageAnnotations[currentPageIndex]?.toList() ?: emptyList()
    }

    fun setPenMode(color: Int, strokeWidth: Float, opacity: Float) {
        currentTool = ToolMode.PEN
        penColor = color
        penStrokeWidth = strokeWidth
        penOpacity = opacity.coerceIn(0.05f, 1.0f)
        invalidate()
    }

    fun setHighlightMode(color: Int, strokeWidth: Float, opacity: Float) {
        currentTool = ToolMode.HIGHLIGHT
        highlightColor = color
        highlightStrokeWidth = strokeWidth
        highlightOpacity = opacity.coerceIn(0.10f, 0.85f)
        invalidate()
    }

    fun setShapeMode(shape: ShapeType, color: Int, strokeWidth: Float, opacity: Float, fillColor: Int?) {
        currentTool = ToolMode.SHAPE
        currentShape = shape
        shapeColor = color
        shapeStrokeWidth = strokeWidth
        shapeOpacity = opacity.coerceIn(0.05f, 1.0f)
        shapeFillColor = fillColor
        invalidate()
    }

    fun setEraserMode(size: EraserSize) {
        currentTool = ToolMode.ERASER
        eraserSize = size
        invalidate()
    }

    /**
     * Clears all pen, highlight, and shape annotations on the current page.
     * Original PDF content is completely untouched.
     */
    fun clearCurrentPageAnnotations() {
        val currentList = pageAnnotations[currentPageIndex] ?: return
        val itemsToRemove = currentList.filter {
            it.type == PdfEngine.AnnotationType.PEN ||
            it.type == PdfEngine.AnnotationType.DRAW ||
            it.type == PdfEngine.AnnotationType.HIGHLIGHTER ||
            it.type == PdfEngine.AnnotationType.RECTANGLE ||
            it.type == PdfEngine.AnnotationType.CIRCLE ||
            it.type == PdfEngine.AnnotationType.LINE ||
            it.type == PdfEngine.AnnotationType.ARROW
        }
        if (itemsToRemove.isNotEmpty()) {
            onHistoryPush?.invoke()
            currentList.removeAll(itemsToRemove)
            onAnnotationsRemoved?.invoke(itemsToRemove)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInteractionEnabled) {
            return false
        }

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                when (currentTool) {
                    ToolMode.PEN, ToolMode.HIGHLIGHT -> {
                        activePoints.clear()
                        activePoints.add(x to y)
                        activePath.reset()
                        activePath.moveTo(x, y)
                        lastX = x
                        lastY = y
                    }

                    ToolMode.SHAPE -> {
                        isDrawingShape = true
                        shapeStartX = x
                        shapeStartY = y
                        shapeCurrentX = x
                        shapeCurrentY = y
                    }

                    ToolMode.ERASER -> {
                        isErasing = true
                        eraserTouchX = x
                        eraserTouchY = y
                        removedDuringCurrentGesture.clear()
                        checkAndEraseAnnotationsAt(x, y)
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                when (currentTool) {
                    ToolMode.PEN, ToolMode.HIGHLIGHT -> {
                        // Smooth Bézier curve interpolation via midpoints
                        val midX = (lastX + x) / 2f
                        val midY = (lastY + y) / 2f
                        activePath.quadTo(lastX, lastY, midX, midY)
                        lastX = x
                        lastY = y
                        activePoints.add(x to y)
                        invalidate()
                    }

                    ToolMode.SHAPE -> {
                        shapeCurrentX = x
                        shapeCurrentY = y
                        invalidate()
                    }

                    ToolMode.ERASER -> {
                        eraserTouchX = x
                        eraserTouchY = y
                        checkAndEraseAnnotationsAt(x, y)
                        invalidate()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)

                when (currentTool) {
                    ToolMode.PEN, ToolMode.HIGHLIGHT -> {
                        activePath.lineTo(x, y)
                        activePoints.add(x to y)

                        if (activePoints.size >= 2) {
                            onHistoryPush?.invoke()
                            val isHighlight = currentTool == ToolMode.HIGHLIGHT
                            val newAnnotation = PdfEngine.EditItem(
                                pageIndex = currentPageIndex,
                                type = if (isHighlight) PdfEngine.AnnotationType.HIGHLIGHTER else PdfEngine.AnnotationType.PEN,
                                colorHex = if (isHighlight) highlightColor else penColor,
                                strokeWidth = if (isHighlight) highlightStrokeWidth else penStrokeWidth,
                                opacity = if (isHighlight) highlightOpacity else penOpacity,
                                points = activePoints.toList(),
                                baseWidth = if (width > 0) width.toFloat() else 800f,
                                baseHeight = if (height > 0) height.toFloat() else 1100f
                            )
                            val currentList = pageAnnotations.getOrPut(currentPageIndex) { mutableListOf() }
                            currentList.add(newAnnotation)
                            onAnnotationAdded?.invoke(newAnnotation)
                        }

                        activePoints.clear()
                        activePath.reset()
                    }

                    ToolMode.SHAPE -> {
                        shapeCurrentX = x
                        shapeCurrentY = y
                        isDrawingShape = false

                        val dist = hypot(shapeCurrentX - shapeStartX, shapeCurrentY - shapeStartY)
                        if (dist > 12f) {
                            onHistoryPush?.invoke()
                            val annotType = when (currentShape) {
                                ShapeType.RECTANGLE -> PdfEngine.AnnotationType.RECTANGLE
                                ShapeType.CIRCLE -> PdfEngine.AnnotationType.CIRCLE
                                ShapeType.LINE -> PdfEngine.AnnotationType.LINE
                                ShapeType.ARROW -> PdfEngine.AnnotationType.ARROW
                            }

                            val (pX, pY, eX, eY) = if (currentShape == ShapeType.LINE || currentShape == ShapeType.ARROW) {
                                listOf(shapeStartX, shapeStartY, shapeCurrentX, shapeCurrentY)
                            } else {
                                listOf(
                                    min(shapeStartX, shapeCurrentX),
                                    min(shapeStartY, shapeCurrentY),
                                    max(shapeStartX, shapeCurrentX),
                                    max(shapeStartY, shapeCurrentY)
                                )
                            }

                            val newShapeAnnotation = PdfEngine.EditItem(
                                pageIndex = currentPageIndex,
                                type = annotType,
                                posX = pX,
                                posY = pY,
                                endX = eX,
                                endY = eY,
                                colorHex = shapeColor,
                                fillColorHex = shapeFillColor,
                                strokeWidth = shapeStrokeWidth,
                                opacity = shapeOpacity,
                                baseWidth = if (width > 0) width.toFloat() else 800f,
                                baseHeight = if (height > 0) height.toFloat() else 1100f
                            )
                            val currentList = pageAnnotations.getOrPut(currentPageIndex) { mutableListOf() }
                            currentList.add(newShapeAnnotation)
                            onAnnotationAdded?.invoke(newShapeAnnotation)
                        }
                    }

                    ToolMode.ERASER -> {
                        isErasing = false
                        if (removedDuringCurrentGesture.isNotEmpty()) {
                            onAnnotationsRemoved?.invoke(removedDuringCurrentGesture.toList())
                        }
                        removedDuringCurrentGesture.clear()
                    }
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                activePoints.clear()
                activePath.reset()
                isDrawingShape = false
                isErasing = false
                removedDuringCurrentGesture.clear()
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Geometry collision detection:
     * Erases stroke paths and shape objects intersecting the eraser touch circle.
     * PDF background text and bitmap are completely untouched.
     */
    private fun checkAndEraseAnnotationsAt(touchX: Float, touchY: Float) {
        val currentList = pageAnnotations[currentPageIndex] ?: return
        val radiusPx = eraserSize.radiusDp * resources.displayMetrics.density

        val toRemove = mutableListOf<PdfEngine.EditItem>()
        for (item in currentList) {
            if (removedDuringCurrentGesture.contains(item)) continue

            val isHit = when (item.type) {
                PdfEngine.AnnotationType.PEN,
                PdfEngine.AnnotationType.DRAW,
                PdfEngine.AnnotationType.HIGHLIGHTER -> {
                    checkStrokeIntersection(item, touchX, touchY, radiusPx)
                }

                PdfEngine.AnnotationType.LINE,
                PdfEngine.AnnotationType.ARROW -> {
                    val halfW = (item.strokeWidth * resources.displayMetrics.density) / 2f
                    distToSegment(touchX, touchY, item.posX, item.posY, item.endX, item.endY) <= (radiusPx + halfW)
                }

                PdfEngine.AnnotationType.RECTANGLE -> {
                    checkRectangleIntersection(item, touchX, touchY, radiusPx)
                }

                PdfEngine.AnnotationType.CIRCLE -> {
                    checkEllipseIntersection(item, touchX, touchY, radiusPx)
                }

                else -> false
            }

            if (isHit) {
                toRemove.add(item)
            }
        }

        if (toRemove.isNotEmpty()) {
            if (removedDuringCurrentGesture.isEmpty()) {
                onHistoryPush?.invoke()
            }
            currentList.removeAll(toRemove)
            removedDuringCurrentGesture.addAll(toRemove)
        }
    }

    private fun checkStrokeIntersection(item: PdfEngine.EditItem, touchX: Float, touchY: Float, radiusPx: Float): Boolean {
        val pts = item.points
        if (pts.isEmpty()) return false
        val halfW = (item.strokeWidth * resources.displayMetrics.density) / 2f
        val hitThreshold = radiusPx + halfW

        if (pts.size == 1) {
            return hypot(touchX - pts[0].first, touchY - pts[0].second) <= hitThreshold
        }

        for (i in 0 until pts.size - 1) {
            val p1 = pts[i]
            val p2 = pts[i + 1]
            if (distToSegment(touchX, touchY, p1.first, p1.second, p2.first, p2.second) <= hitThreshold) {
                return true
            }
        }
        return false
    }

    private fun checkRectangleIntersection(item: PdfEngine.EditItem, touchX: Float, touchY: Float, radiusPx: Float): Boolean {
        val left = min(item.posX, item.endX)
        val top = min(item.posY, item.endY)
        val right = max(item.posX, item.endX)
        val bottom = max(item.posY, item.endY)
        val halfW = (item.strokeWidth * resources.displayMetrics.density) / 2f
        val threshold = radiusPx + halfW

        // Check if user hit the filled interior
        if (item.fillColorHex != null) {
            if (touchX in (left - radiusPx)..(right + radiusPx) &&
                touchY in (top - radiusPx)..(bottom + radiusPx)
            ) {
                return true
            }
        }

        // Check distance to the 4 perimeter edges
        if (distToSegment(touchX, touchY, left, top, right, top) <= threshold) return true
        if (distToSegment(touchX, touchY, right, top, right, bottom) <= threshold) return true
        if (distToSegment(touchX, touchY, right, bottom, left, bottom) <= threshold) return true
        if (distToSegment(touchX, touchY, left, bottom, left, top) <= threshold) return true

        return false
    }

    private fun checkEllipseIntersection(item: PdfEngine.EditItem, touchX: Float, touchY: Float, radiusPx: Float): Boolean {
        val left = min(item.posX, item.endX)
        val top = min(item.posY, item.endY)
        val right = max(item.posX, item.endX)
        val bottom = max(item.posY, item.endY)

        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val rx = abs(right - left) / 2f
        val ry = abs(bottom - top) / 2f

        if (rx <= 0f || ry <= 0f) return false

        val normX = (touchX - cx) / rx
        val normY = (touchY - cy) / ry
        val normDist = sqrt((normX * normX + normY * normY).toDouble()).toFloat()

        if (item.fillColorHex != null && normDist <= 1.0f + (radiusPx / min(rx, ry))) {
            return true
        }

        val distToPerimeter = abs(normDist - 1.0f) * min(rx, ry)
        val halfW = (item.strokeWidth * resources.displayMetrics.density) / 2f
        return distToPerimeter <= (radiusPx + halfW)
    }

    private fun distToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return hypot(px - ax, py - ay)

        val t = (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = ax + t * dx
        val projY = ay + t * dy
        return hypot(px - projX, py - projY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density

        // 1. Render all committed annotations for the current page
        val currentList = pageAnnotations[currentPageIndex] ?: emptyList()
        for (item in currentList) {
            when (item.type) {
                PdfEngine.AnnotationType.PEN,
                PdfEngine.AnnotationType.DRAW,
                PdfEngine.AnnotationType.HIGHLIGHTER -> {
                    val pts = item.points
                    if (pts.size >= 2) {
                        val path = Path()
                        path.moveTo(pts[0].first, pts[0].second)
                        for (i in 1 until pts.size) {
                            val prev = pts[i - 1]
                            val curr = pts[i]
                            val midX = (prev.first + curr.first) / 2f
                            val midY = (prev.second + curr.second) / 2f
                            path.quadTo(prev.first, prev.second, midX, midY)
                        }
                        path.lineTo(pts.last().first, pts.last().second)

                        strokePaint.color = item.colorHex
                        strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        strokePaint.strokeWidth = item.strokeWidth * density
                        canvas.drawPath(path, strokePaint)
                    }
                }

                PdfEngine.AnnotationType.RECTANGLE -> {
                    val l = min(item.posX, item.endX)
                    val t = min(item.posY, item.endY)
                    val r = max(item.posX, item.endX)
                    val b = max(item.posY, item.endY)

                    if (item.fillColorHex != null) {
                        fillPaint.color = item.fillColorHex
                        fillPaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        canvas.drawRect(l, t, r, b, fillPaint)
                    }

                    strokePaint.color = item.colorHex
                    strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    strokePaint.strokeWidth = item.strokeWidth * density
                    canvas.drawRect(l, t, r, b, strokePaint)
                }

                PdfEngine.AnnotationType.CIRCLE -> {
                    val l = min(item.posX, item.endX)
                    val t = min(item.posY, item.endY)
                    val r = max(item.posX, item.endX)
                    val b = max(item.posY, item.endY)

                    if (item.fillColorHex != null) {
                        fillPaint.color = item.fillColorHex
                        fillPaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        canvas.drawOval(l, t, r, b, fillPaint)
                    }

                    strokePaint.color = item.colorHex
                    strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    strokePaint.strokeWidth = item.strokeWidth * density
                    canvas.drawOval(l, t, r, b, strokePaint)
                }

                PdfEngine.AnnotationType.LINE -> {
                    strokePaint.color = item.colorHex
                    strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    strokePaint.strokeWidth = item.strokeWidth * density
                    canvas.drawLine(item.posX, item.posY, item.endX, item.endY, strokePaint)
                }

                PdfEngine.AnnotationType.ARROW -> {
                    strokePaint.color = item.colorHex
                    strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    strokePaint.strokeWidth = item.strokeWidth * density
                    canvas.drawLine(item.posX, item.posY, item.endX, item.endY, strokePaint)

                    val angle = atan2((item.endY - item.posY).toDouble(), (item.endX - item.posX).toDouble())
                    val arrowHeadLen = (item.strokeWidth * density * 3.5f).coerceIn(16f, 48f)
                    val x1 = (item.endX - arrowHeadLen * cos(angle - Math.PI / 6)).toFloat()
                    val y1 = (item.endY - arrowHeadLen * sin(angle - Math.PI / 6)).toFloat()
                    val x2 = (item.endX - arrowHeadLen * cos(angle + Math.PI / 6)).toFloat()
                    val y2 = (item.endY - arrowHeadLen * sin(angle + Math.PI / 6)).toFloat()
                    val headPath = Path().apply {
                        moveTo(x1, y1)
                        lineTo(item.endX, item.endY)
                        lineTo(x2, y2)
                    }
                    canvas.drawPath(headPath, strokePaint)
                }

                else -> {}
            }
        }

        // 2. Render active in-progress stroke
        if ((currentTool == ToolMode.PEN || currentTool == ToolMode.HIGHLIGHT) && activePoints.size >= 2) {
            val isHighlight = currentTool == ToolMode.HIGHLIGHT
            strokePaint.color = if (isHighlight) highlightColor else penColor
            strokePaint.alpha = if (isHighlight) (highlightOpacity * 255).toInt() else (penOpacity * 255).toInt()
            strokePaint.strokeWidth = (if (isHighlight) highlightStrokeWidth else penStrokeWidth) * density
            canvas.drawPath(activePath, strokePaint)
        }

        // 3. Render active in-progress shape preview
        if (currentTool == ToolMode.SHAPE && isDrawingShape) {
            previewShapePaint.color = shapeColor
            previewShapePaint.alpha = (shapeOpacity * 255).toInt()
            previewShapePaint.strokeWidth = shapeStrokeWidth * density

            when (currentShape) {
                ShapeType.RECTANGLE -> {
                    val l = min(shapeStartX, shapeCurrentX)
                    val t = min(shapeStartY, shapeCurrentY)
                    val r = max(shapeStartX, shapeCurrentX)
                    val b = max(shapeStartY, shapeCurrentY)
                    if (shapeFillColor != null) {
                        fillPaint.color = shapeFillColor!!
                        fillPaint.alpha = (shapeOpacity * 0.4f * 255).toInt()
                        canvas.drawRect(l, t, r, b, fillPaint)
                    }
                    canvas.drawRect(l, t, r, b, previewShapePaint)
                }

                ShapeType.CIRCLE -> {
                    val l = min(shapeStartX, shapeCurrentX)
                    val t = min(shapeStartY, shapeCurrentY)
                    val r = max(shapeStartX, shapeCurrentX)
                    val b = max(shapeStartY, shapeCurrentY)
                    if (shapeFillColor != null) {
                        fillPaint.color = shapeFillColor!!
                        fillPaint.alpha = (shapeOpacity * 0.4f * 255).toInt()
                        canvas.drawOval(l, t, r, b, fillPaint)
                    }
                    canvas.drawOval(l, t, r, b, previewShapePaint)
                }

                ShapeType.LINE -> {
                    canvas.drawLine(shapeStartX, shapeStartY, shapeCurrentX, shapeCurrentY, previewShapePaint)
                }

                ShapeType.ARROW -> {
                    canvas.drawLine(shapeStartX, shapeStartY, shapeCurrentX, shapeCurrentY, previewShapePaint)
                    val angle = atan2((shapeCurrentY - shapeStartY).toDouble(), (shapeCurrentX - shapeStartX).toDouble())
                    val arrowHeadLen = (shapeStrokeWidth * density * 3.5f).coerceIn(16f, 48f)
                    val x1 = (shapeCurrentX - arrowHeadLen * cos(angle - Math.PI / 6)).toFloat()
                    val y1 = (shapeCurrentY - arrowHeadLen * sin(angle - Math.PI / 6)).toFloat()
                    val x2 = (shapeCurrentX - arrowHeadLen * cos(angle + Math.PI / 6)).toFloat()
                    val y2 = (shapeCurrentY - arrowHeadLen * sin(angle + Math.PI / 6)).toFloat()
                    val headPath = Path().apply {
                        moveTo(x1, y1)
                        lineTo(shapeCurrentX, shapeCurrentY)
                        lineTo(x2, y2)
                    }
                    canvas.drawPath(headPath, previewShapePaint)
                }
            }
        }

        // 4. Render eraser touch indicator
        if (currentTool == ToolMode.ERASER && isErasing) {
            val radiusPx = eraserSize.radiusDp * density
            canvas.drawCircle(eraserTouchX, eraserTouchY, radiusPx, eraserIndicatorFillPaint)
            canvas.drawCircle(eraserTouchX, eraserTouchY, radiusPx, eraserIndicatorStrokePaint)
        }
    }
}
