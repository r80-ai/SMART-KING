package com.example.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.pdf.PdfEngine
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Annotation tools available under the Pen feature.
 */
enum class AnnotationTool {
    NONE,
    PEN,
    HIGHLIGHT,
    SHAPE_RECTANGLE,
    SHAPE_CIRCLE,
    SHAPE_LINE,
    SHAPE_ARROW,
    ERASER
}

/**
 * Geometry-based eraser sizes.
 */
enum class EraserSize(val displayName: String, val radiusDp: Float) {
    SMALL("Small", 10f),
    MEDIUM("Medium", 22f),
    LARGE("Large", 38f)
}

/**
 * Serializable coordinate point for smooth Bézier strokes.
 */
data class AnnotationPoint(val x: Float, val y: Float)

/**
 * Independent editable annotation object model per page.
 */
data class AnnotationObject(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val tool: AnnotationTool,
    val color: Int = Color.BLACK,
    val strokeWidth: Float = 4f,
    val opacity: Float = 1.0f,
    val points: List<AnnotationPoint> = emptyList(),
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val fillColor: Int? = null,
    val baseWidth: Float = 0f,
    val baseHeight: Float = 0f,
    val isVisible: Boolean = true
) {
    /**
     * Converts to the core PdfEngine.EditItem model so that saving and exporting
     * seamlessly renders these vector annotations onto the output PDF at original coordinate scale.
     */
    fun toEditItem(): PdfEngine.EditItem {
        val annotType = when (tool) {
            AnnotationTool.PEN -> PdfEngine.AnnotationType.PEN
            AnnotationTool.HIGHLIGHT -> PdfEngine.AnnotationType.HIGHLIGHTER
            AnnotationTool.SHAPE_RECTANGLE -> PdfEngine.AnnotationType.RECTANGLE
            AnnotationTool.SHAPE_CIRCLE -> PdfEngine.AnnotationType.CIRCLE
            AnnotationTool.SHAPE_LINE -> PdfEngine.AnnotationType.LINE
            AnnotationTool.SHAPE_ARROW -> PdfEngine.AnnotationType.ARROW
            else -> PdfEngine.AnnotationType.PEN
        }
        return PdfEngine.EditItem(
            id = id,
            pageIndex = pageIndex,
            type = annotType,
            colorHex = color,
            strokeWidth = strokeWidth,
            opacity = opacity,
            posX = startX,
            posY = startY,
            endX = endX,
            endY = endY,
            points = points.map { Pair(it.x, it.y) },
            fillColorHex = fillColor,
            baseWidth = baseWidth,
            baseHeight = baseHeight,
            isVisible = isVisible
        )
    }

    companion object {
        fun fromEditItem(item: PdfEngine.EditItem): AnnotationObject? {
            val tool = when (item.type) {
                PdfEngine.AnnotationType.PEN, PdfEngine.AnnotationType.DRAW -> AnnotationTool.PEN
                PdfEngine.AnnotationType.HIGHLIGHTER -> AnnotationTool.HIGHLIGHT
                PdfEngine.AnnotationType.RECTANGLE -> AnnotationTool.SHAPE_RECTANGLE
                PdfEngine.AnnotationType.CIRCLE -> AnnotationTool.SHAPE_CIRCLE
                PdfEngine.AnnotationType.LINE -> AnnotationTool.SHAPE_LINE
                PdfEngine.AnnotationType.ARROW -> AnnotationTool.SHAPE_ARROW
                else -> return null
            }
            return AnnotationObject(
                id = item.id,
                pageIndex = item.pageIndex,
                tool = tool,
                color = item.colorHex,
                strokeWidth = item.strokeWidth,
                opacity = item.opacity,
                points = item.points.map { AnnotationPoint(it.first, it.second) },
                startX = item.posX,
                startY = item.posY,
                endX = item.endX,
                endY = item.endY,
                fillColor = item.fillColorHex,
                baseWidth = item.baseWidth,
                baseHeight = item.baseHeight,
                isVisible = item.isVisible
            )
        }
    }
}

/**
 * Custom Annotation Canvas Overlay View.
 *
 * Sits directly on top of the PDF page renderer in pdf_editor_canvas.xml.
 * Features:
 * - Freehand Pen with smooth quadratic Bézier curves
 * - Highlight marker with semi-transparency and smooth curves
 * - Manual drag-to-create Shapes (Rectangle, Circle/Ellipse, Line, Arrow)
 * - Dedicated Geometry-based Eraser that NEVER whitens or alters the background PDF bitmap
 * - Parent touch intercept disallowing during active gestures (requestDisallowInterceptTouchEvent)
 * - Multi-page annotation model storage mapped by page index
 */
class AnnotationCanvasOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentPageIndex: Int = 0
        set(value) {
            field = value
            postInvalidate()
        }

    var activeTool: AnnotationTool = AnnotationTool.NONE
        set(value) {
            field = value
            isFocusable = value != AnnotationTool.NONE
            isClickable = value != AnnotationTool.NONE
            postInvalidate()
        }

    // Pen attributes
    var brushColor: Int = Color.parseColor("#2563EB")
    var brushStrokeWidthDp: Float = 4f
    var brushOpacity: Float = 1.0f

    // Highlighter attributes
    var highlightColor: Int = Color.parseColor("#FACC15")
    var highlightStrokeWidthDp: Float = 22f
    var highlightOpacity: Float = 0.4f

    // Shape attributes
    var shapeStrokeWidthDp: Float = 3f

    // Eraser attributes
    var eraserSize: EraserSize = EraserSize.MEDIUM

    // Independent editable annotation model per page (pageIndex -> list of annotations)
    val annotationsByPage: MutableMap<Int, MutableList<AnnotationObject>> = mutableMapOf()

    // Callbacks
    var onAnnotationAdded: ((AnnotationObject) -> Unit)? = null
    var onAnnotationsRemoved: ((List<String>) -> Unit)? = null
    var onAnnotationCleared: ((Int) -> Unit)? = null

    // Touch interaction state
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private val inProgressPoints = mutableListOf<AnnotationPoint>()
    private val inProgressPath = Path()

    // In-progress shape drag
    private var isDraggingShape = false
    private var currentShapeX = 0f
    private var currentShapeY = 0f

    // In-progress eraser drag
    private var isErasing = false
    private var eraserTouchX = 0f
    private var eraserTouchY = 0f
    private val erasedIdsInCurrentGesture = mutableSetOf<String>()

    // Paints
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val previewGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#60A5FA")
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val eraserCursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#EF4444")
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    private val eraserFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#33EF4444") // 20% alpha red
    }

    init {
        elevation = 8f * resources.displayMetrics.density
    }

    /**
     * Synchronizes annotations from external state (e.g. ViewModel or editItems).
     */
    fun syncFromEditItems(items: List<PdfEngine.EditItem>) {
        annotationsByPage.clear()
        for (item in items) {
            val obj = AnnotationObject.fromEditItem(item) ?: continue
            annotationsByPage.getOrPut(obj.pageIndex) { mutableListOf() }.add(obj)
        }
        postInvalidate()
    }

    /**
     * Clears all annotations on the specified page.
     */
    fun clearPage(pageIndex: Int) {
        val list = annotationsByPage[pageIndex]
        if (!list.isNullOrEmpty()) {
            list.clear()
            onAnnotationCleared?.invoke(pageIndex)
            postInvalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (activeTool == AnnotationTool.NONE) {
            return false
        }

        val density = resources.displayMetrics.density
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Strictly disable PDF page scrolling/paging intercept while actively drawing, shaping, or erasing
                parent?.requestDisallowInterceptTouchEvent(true)

                touchDownX = x
                touchDownY = y
                lastX = x
                lastY = y

                when (activeTool) {
                    AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                        inProgressPoints.clear()
                        inProgressPoints.add(AnnotationPoint(x, y))
                        inProgressPath.reset()
                        inProgressPath.moveTo(x, y)
                    }

                    AnnotationTool.SHAPE_RECTANGLE,
                    AnnotationTool.SHAPE_CIRCLE,
                    AnnotationTool.SHAPE_LINE,
                    AnnotationTool.SHAPE_ARROW -> {
                        isDraggingShape = true
                        currentShapeX = x
                        currentShapeY = y
                    }

                    AnnotationTool.ERASER -> {
                        isErasing = true
                        eraserTouchX = x
                        eraserTouchY = y
                        erasedIdsInCurrentGesture.clear()
                        checkEraserIntersection(x, y, eraserSize.radiusDp * density)
                    }

                    AnnotationTool.NONE -> {}
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                when (activeTool) {
                    AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                        // Smooth quadratic Bézier curves
                        val midX = (lastX + x) / 2f
                        val midY = (lastY + y) / 2f
                        inProgressPath.quadTo(lastX, lastY, midX, midY)
                        lastX = x
                        lastY = y
                        inProgressPoints.add(AnnotationPoint(x, y))
                    }

                    AnnotationTool.SHAPE_RECTANGLE,
                    AnnotationTool.SHAPE_CIRCLE,
                    AnnotationTool.SHAPE_LINE,
                    AnnotationTool.SHAPE_ARROW -> {
                        currentShapeX = x
                        currentShapeY = y
                    }

                    AnnotationTool.ERASER -> {
                        eraserTouchX = x
                        eraserTouchY = y
                        checkEraserIntersection(x, y, eraserSize.radiusDp * density)
                    }

                    AnnotationTool.NONE -> {}
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)

                when (activeTool) {
                    AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                        inProgressPath.lineTo(x, y)
                        inProgressPoints.add(AnnotationPoint(x, y))

                        if (inProgressPoints.size >= 2) {
                            val isHighlight = activeTool == AnnotationTool.HIGHLIGHT
                            val color = if (isHighlight) highlightColor else brushColor
                            val widthPx = if (isHighlight) highlightStrokeWidthDp * density else brushStrokeWidthDp * density
                            val opacity = if (isHighlight) highlightOpacity else brushOpacity

                            val newObj = AnnotationObject(
                                id = UUID.randomUUID().toString(),
                                pageIndex = currentPageIndex,
                                tool = activeTool,
                                color = color,
                                strokeWidth = widthPx,
                                opacity = opacity,
                                points = inProgressPoints.toList(),
                                baseWidth = width.toFloat().coerceAtLeast(1f),
                                baseHeight = height.toFloat().coerceAtLeast(1f)
                            )
                            annotationsByPage.getOrPut(currentPageIndex) { mutableListOf() }.add(newObj)
                            onAnnotationAdded?.invoke(newObj)
                        }
                        inProgressPoints.clear()
                        inProgressPath.reset()
                    }

                    AnnotationTool.SHAPE_RECTANGLE,
                    AnnotationTool.SHAPE_CIRCLE,
                    AnnotationTool.SHAPE_LINE,
                    AnnotationTool.SHAPE_ARROW -> {
                        isDraggingShape = false
                        val dist = hypot(x - touchDownX, y - touchDownY)
                        // Strictly created via user drag (ACTION_DOWN to ACTION_UP); never auto-place shapes on tap
                        if (dist > 8f * density) {
                            val newObj = AnnotationObject(
                                id = UUID.randomUUID().toString(),
                                pageIndex = currentPageIndex,
                                tool = activeTool,
                                color = brushColor,
                                strokeWidth = shapeStrokeWidthDp * density,
                                opacity = brushOpacity,
                                startX = touchDownX,
                                startY = touchDownY,
                                endX = x,
                                endY = y,
                                baseWidth = width.toFloat().coerceAtLeast(1f),
                                baseHeight = height.toFloat().coerceAtLeast(1f)
                            )
                            annotationsByPage.getOrPut(currentPageIndex) { mutableListOf() }.add(newObj)
                            onAnnotationAdded?.invoke(newObj)
                        }
                    }

                    AnnotationTool.ERASER -> {
                        isErasing = false
                        if (erasedIdsInCurrentGesture.isNotEmpty()) {
                            onAnnotationsRemoved?.invoke(erasedIdsInCurrentGesture.toList())
                            erasedIdsInCurrentGesture.clear()
                        }
                    }

                    AnnotationTool.NONE -> {}
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isDraggingShape = false
                isErasing = false
                inProgressPoints.clear()
                inProgressPath.reset()
                erasedIdsInCurrentGesture.clear()
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Geometry-based annotation eraser.
     * Operates continuously on touch-drag by intersecting and removing annotation path segments or shape objects.
     * It strictly ONLY deletes canvas annotation objects and NEVER whitens or alters the background PDF.
     */
    private fun checkEraserIntersection(cx: Float, cy: Float, radius: Float) {
        val list = annotationsByPage[currentPageIndex] ?: return
        val toRemove = mutableListOf<AnnotationObject>()

        for (item in list) {
            if (!item.isVisible) continue
            var hit = false

            when (item.tool) {
                AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                    val pts = item.points
                    val effectiveRadius = radius + (item.strokeWidth / 2f)
                    for (i in 0 until pts.size - 1) {
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        if (distanceToSegment(cx, cy, p1.x, p1.y, p2.x, p2.y) <= effectiveRadius) {
                            hit = true
                            break
                        }
                    }
                }

                AnnotationTool.SHAPE_LINE, AnnotationTool.SHAPE_ARROW -> {
                    val effectiveRadius = radius + (item.strokeWidth / 2f)
                    if (distanceToSegment(cx, cy, item.startX, item.startY, item.endX, item.endY) <= effectiveRadius) {
                        hit = true
                    }
                }

                AnnotationTool.SHAPE_RECTANGLE -> {
                    val effectiveRadius = radius + (item.strokeWidth / 2f)
                    val left = min(item.startX, item.endX)
                    val right = max(item.startX, item.endX)
                    val top = min(item.startY, item.endY)
                    val bottom = max(item.startY, item.endY)

                    // Check distance to four borders
                    if (distanceToSegment(cx, cy, left, top, right, top) <= effectiveRadius ||
                        distanceToSegment(cx, cy, right, top, right, bottom) <= effectiveRadius ||
                        distanceToSegment(cx, cy, left, bottom, right, bottom) <= effectiveRadius ||
                        distanceToSegment(cx, cy, left, top, left, bottom) <= effectiveRadius
                    ) {
                        hit = true
                    } else if (item.fillColor != null && cx in left..right && cy in top..bottom) {
                        hit = true
                    }
                }

                AnnotationTool.SHAPE_CIRCLE -> {
                    val effectiveRadius = radius + (item.strokeWidth / 2f)
                    val left = min(item.startX, item.endX)
                    val right = max(item.startX, item.endX)
                    val top = min(item.startY, item.endY)
                    val bottom = max(item.startY, item.endY)
                    val centerX = (left + right) / 2f
                    val centerY = (top + bottom) / 2f
                    val rx = max((right - left) / 2f, 1f)
                    val ry = max((bottom - top) / 2f, 1f)

                    val normDist = sqrt(((cx - centerX) / rx) * ((cx - centerX) / rx) + ((cy - centerY) / ry) * ((cy - centerY) / ry))
                    val distFromPerimeter = kotlin.math.abs(normDist - 1f) * min(rx, ry)
                    if (distFromPerimeter <= effectiveRadius || (item.fillColor != null && normDist <= 1f)) {
                        hit = true
                    }
                }

                AnnotationTool.NONE, AnnotationTool.ERASER -> {}
            }

            if (hit) {
                toRemove.add(item)
            }
        }

        if (toRemove.isNotEmpty()) {
            list.removeAll(toRemove)
            toRemove.forEach { erasedIdsInCurrentGesture.add(it.id) }
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = resources.displayMetrics.density

        // 1. Draw existing annotations for current page
        val pageItems = annotationsByPage[currentPageIndex] ?: emptyList()
        for (item in pageItems) {
            if (!item.isVisible) continue
            drawAnnotationObject(canvas, item)
        }

        // 2. Draw active in-progress stroke (smooth Bézier path)
        if (inProgressPoints.size >= 2) {
            val isHighlight = activeTool == AnnotationTool.HIGHLIGHT
            val color = if (isHighlight) highlightColor else brushColor
            val widthPx = if (isHighlight) highlightStrokeWidthDp * density else brushStrokeWidthDp * density
            val opacity = if (isHighlight) highlightOpacity else brushOpacity

            strokePaint.apply {
                this.color = color
                this.alpha = (opacity.coerceIn(0.01f, 1f) * 255).toInt()
                this.strokeWidth = widthPx
            }
            canvas.drawPath(inProgressPath, strokePaint)
        }

        // 3. Draw active in-progress shape preview while dragging
        if (isDraggingShape) {
            val previewObj = AnnotationObject(
                pageIndex = currentPageIndex,
                tool = activeTool,
                color = brushColor,
                strokeWidth = shapeStrokeWidthDp * density,
                opacity = brushOpacity,
                startX = touchDownX,
                startY = touchDownY,
                endX = currentShapeX,
                endY = currentShapeY
            )
            drawAnnotationObject(canvas, previewObj)
        }

        // 4. Draw continuous active eraser cursor indicator
        if (isErasing) {
            val r = eraserSize.radiusDp * density
            canvas.drawCircle(eraserTouchX, eraserTouchY, r, eraserFillPaint)
            canvas.drawCircle(eraserTouchX, eraserTouchY, r, eraserCursorPaint)
        }
    }

    private fun drawAnnotationObject(canvas: Canvas, item: AnnotationObject) {
        strokePaint.apply {
            color = item.color
            alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
            strokeWidth = item.strokeWidth.coerceAtLeast(1f)
            style = Paint.Style.STROKE
        }

        when (item.tool) {
            AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                val pts = item.points
                if (pts.size >= 2) {
                    val path = Path()
                    path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]
                        val curr = pts[i]
                        val midX = (prev.x + curr.x) / 2f
                        val midY = (prev.y + curr.y) / 2f
                        path.quadTo(prev.x, prev.y, midX, midY)
                    }
                    path.lineTo(pts.last().x, pts.last().y)
                    canvas.drawPath(path, strokePaint)
                }
            }

            AnnotationTool.SHAPE_LINE -> {
                canvas.drawLine(item.startX, item.startY, item.endX, item.endY, strokePaint)
            }

            AnnotationTool.SHAPE_ARROW -> {
                canvas.drawLine(item.startX, item.startY, item.endX, item.endY, strokePaint)
                val angle = atan2((item.endY - item.startY).toDouble(), (item.endX - item.startX).toDouble())
                val arrowHeadLen = (item.strokeWidth * 4f).coerceIn(16f, 48f)
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

            AnnotationTool.SHAPE_RECTANGLE -> {
                val left = min(item.startX, item.endX)
                val right = max(item.startX, item.endX)
                val top = min(item.startY, item.endY)
                val bottom = max(item.startY, item.endY)
                if (item.fillColor != null) {
                    fillPaint.apply {
                        color = item.fillColor
                        alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    }
                    canvas.drawRect(left, top, right, bottom, fillPaint)
                }
                canvas.drawRect(left, top, right, bottom, strokePaint)
            }

            AnnotationTool.SHAPE_CIRCLE -> {
                val left = min(item.startX, item.endX)
                val right = max(item.startX, item.endX)
                val top = min(item.startY, item.endY)
                val bottom = max(item.startY, item.endY)
                val rectF = RectF(left, top, right, bottom)
                if (item.fillColor != null) {
                    fillPaint.apply {
                        color = item.fillColor
                        alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    }
                    canvas.drawOval(rectF, fillPaint)
                }
                canvas.drawOval(rectF, strokePaint)
            }

            AnnotationTool.NONE, AnnotationTool.ERASER -> {}
        }
    }

    private fun distanceToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) {
            val ex = px - x1
            val ey = py - y1
            return sqrt(ex * ex + ey * ey)
        }
        val t = ((px - x1) * dx + (py - y1) * dy) / lenSq
        val clampedT = t.coerceIn(0f, 1f)
        val projX = x1 + clampedT * dx
        val projY = y1 + clampedT * dy
        val rx = px - projX
        val ry = py - projY
        return sqrt(rx * rx + ry * ry)
    }
}
