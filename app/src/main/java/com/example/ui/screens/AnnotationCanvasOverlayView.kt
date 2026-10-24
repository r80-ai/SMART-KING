package com.example.ui.screens

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.pdf.PdfEngine
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Supported annotation sub-tools under the Pen master tool.
 */
enum class AnnotationTool {
    NONE,
    PEN,
    HIGHLIGHT,
    RECTANGLE,
    CIRCLE,
    LINE,
    ARROW,
    ERASER
}

/**
 * Eraser size presets (radius in dp).
 */
enum class EraserSizePreset(val radiusDp: Float) {
    SMALL(12f),
    MEDIUM(24f),
    LARGE(48f)
}

/**
 * Pure vector annotation object model representing user annotations.
 * Each object belongs to an independent page index.
 */
data class AnnotationObject(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val tool: AnnotationTool,
    val color: Int,
    val strokeWidth: Float,
    val opacity: Float = 1.0f,
    val fillColor: Int? = null,
    val points: List<PointF> = emptyList(),
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val baseWidth: Float = 0f,
    val baseHeight: Float = 0f
) {
    /**
     * Converts to PdfEngine.EditItem for PDF vector save & export at exact coordinate scale.
     */
    fun toEditItem(): PdfEngine.EditItem {
        val annotType = when (tool) {
            AnnotationTool.PEN -> PdfEngine.AnnotationType.PEN
            AnnotationTool.HIGHLIGHT -> PdfEngine.AnnotationType.HIGHLIGHTER
            AnnotationTool.RECTANGLE -> PdfEngine.AnnotationType.RECTANGLE
            AnnotationTool.CIRCLE -> PdfEngine.AnnotationType.CIRCLE
            AnnotationTool.LINE -> PdfEngine.AnnotationType.LINE
            AnnotationTool.ARROW -> PdfEngine.AnnotationType.ARROW
            else -> PdfEngine.AnnotationType.PEN
        }
        return PdfEngine.EditItem(
            id = id,
            pageIndex = pageIndex,
            type = annotType,
            colorHex = color,
            strokeWidth = strokeWidth,
            opacity = opacity,
            fillColorHex = fillColor,
            posX = startX,
            posY = startY,
            endX = endX,
            endY = endY,
            points = points.map { Pair(it.x, it.y) },
            baseWidth = baseWidth,
            baseHeight = baseHeight
        )
    }

    companion object {
        fun fromEditItem(item: PdfEngine.EditItem): AnnotationObject? {
            val tool = when (item.type) {
                PdfEngine.AnnotationType.PEN, PdfEngine.AnnotationType.DRAW -> AnnotationTool.PEN
                PdfEngine.AnnotationType.HIGHLIGHTER -> AnnotationTool.HIGHLIGHT
                PdfEngine.AnnotationType.RECTANGLE -> AnnotationTool.RECTANGLE
                PdfEngine.AnnotationType.CIRCLE -> AnnotationTool.CIRCLE
                PdfEngine.AnnotationType.LINE -> AnnotationTool.LINE
                PdfEngine.AnnotationType.ARROW -> AnnotationTool.ARROW
                else -> return null
            }
            return AnnotationObject(
                id = item.id,
                pageIndex = item.pageIndex,
                tool = tool,
                color = item.colorHex,
                strokeWidth = item.strokeWidth,
                opacity = item.opacity,
                fillColor = item.fillColorHex,
                points = item.points.map { PointF(it.first, it.second) },
                startX = item.posX,
                startY = item.posY,
                endX = item.endX,
                endY = item.endY,
                baseWidth = item.baseWidth,
                baseHeight = item.baseHeight
            )
        }
    }
}

/**
 * Custom Annotation Canvas Overlay View.
 *
 * Sits directly on top of the PDF ImageView in pdf_editor_canvas.
 *
 * Key Capabilities:
 * 1. Independent per-page editable annotation models: Map<Int, MutableList<AnnotationObject>>
 * 2. Smooth Bézier curves for freehand Pen and Highlighter (quadTo midpoint interpolation)
 * 3. Manual drag-to-create shapes (Rectangle, Circle/Ellipse, Line, Arrow) with live drag preview
 * 4. Geometry-based Eraser: ONLY erases vector annotation objects and segments; NEVER whitens,
 *    cuts, or damages underlying PDF page content, images, or text
 * 5. Touch event disallow-intercept on drawing/shaping/erasing to prevent PDF scrolling conflicts
 * 6. Full Undo, Redo, and Cancel support
 * 7. Accurate vector export synchronization with PdfEngine
 */
class AnnotationCanvasOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Current active tool under the Pen master tab
    var activeTool: AnnotationTool = AnnotationTool.NONE
        set(value) {
            field = value
            cancelCurrentGesture()
            invalidate()
        }

    // Active configuration for Pen / Shapes
    var activeColor: Int = Color.rgb(37, 99, 235) // Modern Blue default
    var activeStrokeWidth: Float = 4f // In dp
    var activeOpacity: Float = 1.0f // 0f..1f
    var activeFillColor: Int? = null // Optional shape fill

    // Highlighter configuration
    var highlighterColor: Int = Color.rgb(250, 204, 21) // High-visibility fluorescent yellow
    var highlighterWidth: Float = 22f // In dp
    var highlighterOpacity: Float = 0.35f // Semi-transparent freehand marker

    // Eraser configuration
    var eraserSizePreset: EraserSizePreset = EraserSizePreset.MEDIUM

    // Current page tracking
    var currentPageIndex: Int = 0
        set(value) {
            if (field != value) {
                cancelCurrentGesture()
                field = value
                invalidate()
            }
        }

    // Per-page annotation storage: PageIndex -> List of AnnotationObjects
    private val annotationsByPage = mutableMapOf<Int, MutableList<AnnotationObject>>()

    // Undo / Redo history stacks (storing snapshots of annotationsByPage)
    private val undoStack = mutableListOf<Map<Int, List<AnnotationObject>>>()
    private val redoStack = mutableListOf<Map<Int, List<AnnotationObject>>>()

    // Callbacks
    var onAnnotationsChanged: ((List<AnnotationObject>) -> Unit)? = null
    var onUndoRedoStateChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null

    // In-progress Freehand Pen / Highlight stroke tracking
    private val currentStrokePoints = mutableListOf<PointF>()
    private val currentStrokePath = Path()
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // In-progress Shape drag tracking
    private var isDraggingShape = false
    private var shapeStartX = 0f
    private var shapeStartY = 0f
    private var shapeCurrentX = 0f
    private var shapeCurrentY = 0f

    // In-progress Eraser drag tracking
    private var isErasing = false
    private var eraserTouchX = 0f
    private var eraserTouchY = 0f
    private var didEraseInCurrentGesture = false

    // Paints
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val eraserCursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#EF4444") // Red outline for eraser cursor
        strokeWidth = 2f * resources.displayMetrics.density
        pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
    }

    private val eraserCursorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(40, 239, 68, 68)
    }

    init {
        setWillNotDraw(false)
        isFocusable = false
        isClickable = false
    }

    /**
     * Returns true if user can interact with the annotation canvas.
     */
    fun isToolActive(): Boolean = activeTool != AnnotationTool.NONE

    /**
     * Saves current state to Undo history before modifications.
     */
    private fun pushHistory() {
        val snapshot = annotationsByPage.mapValues { entry -> entry.value.map { it.copy() } }
        undoStack.add(snapshot)
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        notifyUndoRedoState()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentSnapshot = annotationsByPage.mapValues { entry -> entry.value.map { it.copy() } }
        redoStack.add(currentSnapshot)

        val previous = undoStack.removeAt(undoStack.size - 1)
        annotationsByPage.clear()
        previous.forEach { (page, list) ->
            annotationsByPage[page] = list.toMutableList()
        }
        notifyUndoRedoState()
        notifyAnnotationsChanged()
        invalidate()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentSnapshot = annotationsByPage.mapValues { entry -> entry.value.map { it.copy() } }
        undoStack.add(currentSnapshot)

        val next = redoStack.removeAt(redoStack.size - 1)
        annotationsByPage.clear()
        next.forEach { (page, list) ->
            annotationsByPage[page] = list.toMutableList()
        }
        notifyUndoRedoState()
        notifyAnnotationsChanged()
        invalidate()
    }

    /**
     * Clears all annotations on current page with undo support.
     */
    fun clearCurrentPageAnnotations() {
        val pageList = annotationsByPage[currentPageIndex]
        if (!pageList.isNullOrEmpty()) {
            pushHistory()
            pageList.clear()
            notifyAnnotationsChanged()
            invalidate()
        }
    }

    /**
     * Cancels any active in-progress gesture.
     */
    fun cancelCurrentGesture() {
        currentStrokePoints.clear()
        currentStrokePath.reset()
        isDraggingShape = false
        isErasing = false
        didEraseInCurrentGesture = false
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    /**
     * Updates annotations from external source (e.g. initial load or ViewModel).
     */
    fun loadAnnotations(items: List<AnnotationObject>) {
        annotationsByPage.clear()
        for (item in items) {
            val list = annotationsByPage.getOrPut(item.pageIndex) { mutableListOf() }
            list.add(item)
        }
        undoStack.clear()
        redoStack.clear()
        notifyUndoRedoState()
        invalidate()
    }

    /**
     * Returns all annotations across all pages.
     */
    fun getAllAnnotations(): List<AnnotationObject> {
        val all = mutableListOf<AnnotationObject>()
        annotationsByPage.values.forEach { all.addAll(it) }
        return all
    }

    /**
     * Returns annotations for current page.
     */
    fun getCurrentPageAnnotations(): List<AnnotationObject> {
        return annotationsByPage[currentPageIndex]?.toList() ?: emptyList()
    }

    private fun notifyAnnotationsChanged() {
        onAnnotationsChanged?.invoke(getAllAnnotations())
    }

    private fun notifyUndoRedoState() {
        onUndoRedoStateChanged?.invoke(canUndo(), canRedo())
    }

    // ====================================================
    // TOUCH EVENT HANDLING & GESTURES
    // ====================================================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isToolActive()) {
            return false // Pass events through when Pen tool is inactive
        }

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // RULE 4: Disable PDF page scrolling/paging intercept while drawing, shaping, or erasing
                parent?.requestDisallowInterceptTouchEvent(true)

                when (activeTool) {
                    AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                        currentStrokePoints.clear()
                        currentStrokePoints.add(PointF(x, y))
                        currentStrokePath.reset()
                        currentStrokePath.moveTo(x, y)
                        lastTouchX = x
                        lastTouchY = y
                        invalidate()
                        return true
                    }
                    AnnotationTool.RECTANGLE, AnnotationTool.CIRCLE, AnnotationTool.LINE, AnnotationTool.ARROW -> {
                        // Shapes strictly created via user drag; never auto-placed
                        isDraggingShape = true
                        shapeStartX = x
                        shapeStartY = y
                        shapeCurrentX = x
                        shapeCurrentY = y
                        invalidate()
                        return true
                    }
                    AnnotationTool.ERASER -> {
                        isErasing = true
                        eraserTouchX = x
                        eraserTouchY = y
                        didEraseInCurrentGesture = false
                        performEraserIntersection(x, y)
                        invalidate()
                        return true
                    }
                    else -> return false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (activeTool) {
                    AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                        currentStrokePoints.add(PointF(x, y))
                        // Smooth Bézier curve (quadTo through midpoints)
                        val midX = (lastTouchX + x) / 2f
                        val midY = (lastTouchY + y) / 2f
                        currentStrokePath.quadTo(lastTouchX, lastTouchY, midX, midY)
                        lastTouchX = x
                        lastTouchY = y
                        invalidate()
                        return true
                    }
                    AnnotationTool.RECTANGLE, AnnotationTool.CIRCLE, AnnotationTool.LINE, AnnotationTool.ARROW -> {
                        if (isDraggingShape) {
                            shapeCurrentX = x
                            shapeCurrentY = y
                            invalidate()
                            return true
                        }
                    }
                    AnnotationTool.ERASER -> {
                        if (isErasing) {
                            eraserTouchX = x
                            eraserTouchY = y
                            performEraserIntersection(x, y)
                            invalidate()
                            return true
                        }
                    }
                    else -> {}
                }
            }

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)

                when (activeTool) {
                    AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                        currentStrokePoints.add(PointF(x, y))
                        currentStrokePath.lineTo(x, y)

                        if (currentStrokePoints.size >= 2) {
                            val isHighlighter = activeTool == AnnotationTool.HIGHLIGHT
                            val color = if (isHighlighter) highlighterColor else activeColor
                            val strokeWidth = if (isHighlighter) highlighterWidth else activeStrokeWidth
                            val opacity = if (isHighlighter) highlighterOpacity else activeOpacity

                            val annot = AnnotationObject(
                                pageIndex = currentPageIndex,
                                tool = activeTool,
                                color = color,
                                strokeWidth = strokeWidth,
                                opacity = opacity,
                                points = currentStrokePoints.toList(),
                                baseWidth = width.toFloat().coerceAtLeast(1f),
                                baseHeight = height.toFloat().coerceAtLeast(1f)
                            )
                            pushHistory()
                            val pageList = annotationsByPage.getOrPut(currentPageIndex) { mutableListOf() }
                            pageList.add(annot)
                            notifyAnnotationsChanged()
                        }
                        currentStrokePoints.clear()
                        currentStrokePath.reset()
                        invalidate()
                        return true
                    }

                    AnnotationTool.RECTANGLE, AnnotationTool.CIRCLE, AnnotationTool.LINE, AnnotationTool.ARROW -> {
                        if (isDraggingShape) {
                            shapeCurrentX = x
                            shapeCurrentY = y
                            isDraggingShape = false

                            val dragDist = hypot(shapeCurrentX - shapeStartX, shapeCurrentY - shapeStartY)
                            // Require at least 8px drag so accidental taps do not spawn empty shapes
                            if (dragDist >= 8f) {
                                val annot = AnnotationObject(
                                    pageIndex = currentPageIndex,
                                    tool = activeTool,
                                    color = activeColor,
                                    strokeWidth = activeStrokeWidth,
                                    opacity = activeOpacity,
                                    fillColor = activeFillColor,
                                    startX = shapeStartX,
                                    startY = shapeStartY,
                                    endX = shapeCurrentX,
                                    endY = shapeCurrentY,
                                    baseWidth = width.toFloat().coerceAtLeast(1f),
                                    baseHeight = height.toFloat().coerceAtLeast(1f)
                                )
                                pushHistory()
                                val pageList = annotationsByPage.getOrPut(currentPageIndex) { mutableListOf() }
                                pageList.add(annot)
                                notifyAnnotationsChanged()
                            }
                            invalidate()
                            return true
                        }
                    }

                    AnnotationTool.ERASER -> {
                        isErasing = false
                        if (didEraseInCurrentGesture) {
                            notifyAnnotationsChanged()
                        }
                        invalidate()
                        return true
                    }

                    else -> {}
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelCurrentGesture()
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    // ====================================================
    // GEOMETRIC ERASER INTERSECTION ALGORITHM
    // ====================================================
    /**
     * RULE 3: ERASER & CANVAS INTEGRITY RULES:
     * - The Eraser must ONLY erase annotation strokes and shapes created on the canvas.
     * - It must NEVER whiten, erase, cut, or damage original PDF page content, background text, or images.
     * - Operates continuously on touch-drag by intersecting and removing annotation objects.
     */
    private fun performEraserIntersection(ex: Float, ey: Float) {
        val pageList = annotationsByPage[currentPageIndex] ?: return
        if (pageList.isEmpty()) return

        val density = resources.displayMetrics.density
        val eraserRadius = eraserSizePreset.radiusDp * density

        val toRemove = mutableListOf<AnnotationObject>()

        for (item in pageList) {
            val strokeRadius = (item.strokeWidth * density) / 2f
            val hitThreshold = eraserRadius + strokeRadius

            val isHit = when (item.tool) {
                AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                    // Check intersection with all segments of the freehand stroke
                    var hit = false
                    val pts = item.points
                    for (i in 0 until pts.size - 1) {
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        if (distanceToSegment(ex, ey, p1.x, p1.y, p2.x, p2.y) <= hitThreshold) {
                            hit = true
                            break
                        }
                    }
                    hit
                }

                AnnotationTool.LINE, AnnotationTool.ARROW -> {
                    distanceToSegment(ex, ey, item.startX, item.startY, item.endX, item.endY) <= hitThreshold
                }

                AnnotationTool.RECTANGLE -> {
                    val left = min(item.startX, item.endX)
                    val right = max(item.startX, item.endX)
                    val top = min(item.startY, item.endY)
                    val bottom = max(item.startY, item.endY)

                    // If filled, check inside
                    val inside = item.fillColor != null && (ex in left..right && ey in top..bottom)
                    // Check edges
                    val nearEdge = distanceToSegment(ex, ey, left, top, right, top) <= hitThreshold ||
                            distanceToSegment(ex, ey, right, top, right, bottom) <= hitThreshold ||
                            distanceToSegment(ex, ey, right, bottom, left, bottom) <= hitThreshold ||
                            distanceToSegment(ex, ey, left, bottom, left, top) <= hitThreshold
                    inside || nearEdge
                }

                AnnotationTool.CIRCLE -> {
                    val left = min(item.startX, item.endX)
                    val right = max(item.startX, item.endX)
                    val top = min(item.startY, item.endY)
                    val bottom = max(item.startY, item.endY)
                    val cx = (left + right) / 2f
                    val cy = (top + bottom) / 2f
                    val rx = max((right - left) / 2f, 1f)
                    val ry = max((bottom - top) / 2f, 1f)

                    // Normalized distance from center
                    val dx = (ex - cx) / rx
                    val dy = (ey - cy) / ry
                    val normDist = hypot(dx, dy)

                    if (item.fillColor != null && normDist <= 1.0f) {
                        true
                    } else {
                        // Check distance to ellipse boundary
                        abs(normDist - 1.0f) * min(rx, ry) <= hitThreshold
                    }
                }

                else -> false
            }

            if (isHit) {
                toRemove.add(item)
            }
        }

        if (toRemove.isNotEmpty()) {
            if (!didEraseInCurrentGesture) {
                pushHistory()
                didEraseInCurrentGesture = true
            }
            pageList.removeAll(toRemove)
        }
    }

    /**
     * Calculates distance from point (px, py) to line segment (x1, y1) - (x2, y2).
     */
    private fun distanceToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return hypot(px - x1, py - y1)

        val t = (((px - x1) * dx + (py - y1) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = x1 + t * dx
        val projY = y1 + t * dy
        return hypot(px - projX, py - projY)
    }

    // ====================================================
    // ON DRAW RENDERING
    // ====================================================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = resources.displayMetrics.density

        // 1. Draw all committed annotations for the current page
        val pageList = annotationsByPage[currentPageIndex]
        if (!pageList.isNullOrEmpty()) {
            for (item in pageList) {
                drawAnnotation(canvas, item, density)
            }
        }

        // 2. Draw active in-progress Freehand Pen or Highlight stroke
        if (currentStrokePoints.size >= 2) {
            val isHighlighter = activeTool == AnnotationTool.HIGHLIGHT
            val color = if (isHighlighter) highlighterColor else activeColor
            val widthDp = if (isHighlighter) highlighterWidth else activeStrokeWidth
            val opacity = if (isHighlighter) highlighterOpacity else activeOpacity

            strokePaint.color = color
            strokePaint.alpha = (opacity.coerceIn(0.01f, 1.0f) * 255).toInt()
            strokePaint.strokeWidth = widthDp * density
            canvas.drawPath(currentStrokePath, strokePaint)
        }

        // 3. Draw active in-progress Shape drag preview
        if (isDraggingShape) {
            val previewAnnot = AnnotationObject(
                pageIndex = currentPageIndex,
                tool = activeTool,
                color = activeColor,
                strokeWidth = activeStrokeWidth,
                opacity = activeOpacity,
                fillColor = activeFillColor,
                startX = shapeStartX,
                startY = shapeStartY,
                endX = shapeCurrentX,
                endY = shapeCurrentY
            )
            drawAnnotation(canvas, previewAnnot, density)
        }

        // 4. Draw Eraser touch cursor (visual ring indicator)
        if (isErasing) {
            val eraserRadius = eraserSizePreset.radiusDp * density
            canvas.drawCircle(eraserTouchX, eraserTouchY, eraserRadius, eraserCursorFillPaint)
            canvas.drawCircle(eraserTouchX, eraserTouchY, eraserRadius, eraserCursorPaint)
        }
    }

    private fun drawAnnotation(canvas: Canvas, item: AnnotationObject, density: Float) {
        val strokeW = item.strokeWidth * density
        val alphaVal = (item.opacity.coerceIn(0.01f, 1.0f) * 255).toInt()

        strokePaint.color = item.color
        strokePaint.alpha = alphaVal
        strokePaint.strokeWidth = strokeW

        when (item.tool) {
            AnnotationTool.PEN, AnnotationTool.HIGHLIGHT -> {
                val pts = item.points
                if (pts.size >= 2) {
                    val path = Path()
                    path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]
                        val cur = pts[i]
                        val midX = (prev.x + cur.x) / 2f
                        val midY = (prev.y + cur.y) / 2f
                        path.quadTo(prev.x, prev.y, midX, midY)
                    }
                    path.lineTo(pts.last().x, pts.last().y)
                    canvas.drawPath(path, strokePaint)
                }
            }

            AnnotationTool.RECTANGLE -> {
                val left = min(item.startX, item.endX)
                val right = max(item.startX, item.endX)
                val top = min(item.startY, item.endY)
                val bottom = max(item.startY, item.endY)

                if (item.fillColor != null) {
                    fillPaint.color = item.fillColor
                    fillPaint.alpha = alphaVal
                    canvas.drawRect(left, top, right, bottom, fillPaint)
                }
                canvas.drawRect(left, top, right, bottom, strokePaint)
            }

            AnnotationTool.CIRCLE -> {
                val left = min(item.startX, item.endX)
                val right = max(item.startX, item.endX)
                val top = min(item.startY, item.endY)
                val bottom = max(item.startY, item.endY)
                val rectF = RectF(left, top, right, bottom)

                if (item.fillColor != null) {
                    fillPaint.color = item.fillColor
                    fillPaint.alpha = alphaVal
                    canvas.drawOval(rectF, fillPaint)
                }
                canvas.drawOval(rectF, strokePaint)
            }

            AnnotationTool.LINE -> {
                canvas.drawLine(item.startX, item.startY, item.endX, item.endY, strokePaint)
            }

            AnnotationTool.ARROW -> {
                // Draw base line
                canvas.drawLine(item.startX, item.startY, item.endX, item.endY, strokePaint)

                // Draw arrowhead
                val angle = atan2((item.endY - item.startY).toDouble(), (item.endX - item.startX).toDouble())
                val arrowHeadLen = (strokeW * 3.5f).coerceIn(16f, 48f)
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
}
