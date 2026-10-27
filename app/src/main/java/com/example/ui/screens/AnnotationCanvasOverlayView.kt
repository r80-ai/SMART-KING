package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
 * AnnotationCanvasOverlayView provides a high-performance Android View canvas
 * positioned right above the rendered PDF page.
 *
 * Capabilities:
 * - Smooth quadratic Bézier curves for Freehand Pen and Highlighter.
 * - Geometric shapes (Rectangle, Circle/Ellipse, Line, Arrow) with live preview.
 * - Dual Eraser Modes:
 *     Mode A: "Stroke Eraser" (Auto Object Eraser) - deletes whole object on touch.
 *     Mode B: "Manual Eraser" (Partial Drag Eraser) - erases only touched pixels via PorterDuff.Mode.CLEAR.
 * - Select, Drag-to-Move, and Corner Handle Resize for all drawn pen strokes, highlights, and shapes.
 * - Touch interception management (requestDisallowInterceptTouchEvent) during dragging/resizing/drawing/erasing.
 * - Undo, Redo, and Clear support.
 */
class AnnotationCanvasOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ToolMode {
        SELECT, PEN, HIGHLIGHT, SHAPE, ERASER
    }

    enum class ShapeType {
        RECTANGLE, CIRCLE, LINE, ARROW
    }

    enum class EraserType {
        STROKE, // Mode A: Auto Object Eraser
        MANUAL  // Mode B: Partial Drag Eraser
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
            if (value != ToolMode.SELECT) {
                // Keep selectedAnnotationId if manipulating, or let user deselect
            }
            invalidate()
        }

    var currentShape: ShapeType = ShapeType.RECTANGLE
        set(value) {
            field = value
            invalidate()
        }

    var eraserType: EraserType = EraserType.STROKE
        set(value) {
            field = value
            invalidate()
        }

    var eraserSize: EraserSize = EraserSize.MEDIUM
        set(value) {
            field = value
            invalidate()
        }

    var selectedAnnotationId: String? = null
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
    var onAnnotationUpdated: ((PdfEngine.EditItem) -> Unit)? = null
    var onAnnotationSelected: ((String?) -> Unit)? = null
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

    // Mode B: Manual Eraser drawing layer
    private var drawingLayerBitmap: Bitmap? = null
    private var drawingLayerCanvas: Canvas? = null
    private var manualEraserItem: PdfEngine.EditItem? = null

    // Move & Resize interaction state
    private enum class TouchInteraction {
        NONE,
        DRAW_STROKE,
        DRAW_SHAPE,
        ERASE_STROKE,
        ERASE_MANUAL,
        MOVE_ANNOTATION,
        RESIZE_ANNOTATION
    }

    private var activeInteraction = TouchInteraction.NONE
    private var activeResizeCorner = -1 // 0: TL, 1: TR, 2: BL, 3: BR
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var initialItemState: PdfEngine.EditItem? = null
    private var initialItemBounds = RectF()

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

    private val selectedBoundingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#3B82F6") // ToolBlue
        strokeWidth = 2.5f
    }

    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#3B82F6")
        strokeWidth = 2.5f
    }

    private val manualErasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val manualEraseCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL
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

    fun setEraserMode(size: EraserSize, type: EraserType = eraserType) {
        currentTool = ToolMode.ERASER
        eraserSize = size
        eraserType = type
        invalidate()
    }

    fun setSelectMode() {
        currentTool = ToolMode.SELECT
        invalidate()
    }

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
            selectedAnnotationId = null
            onAnnotationSelected?.invoke(null)
            onAnnotationsRemoved?.invoke(itemsToRemove)
            invalidate()
        }
    }

    private fun disallowParentIntercept(disallow: Boolean) {
        var p = parent
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(disallow)
            p = p.parent
        }
    }

    // --- Bounding Box & Corner Handles Calculation ---

    fun computeAnnotationBounds(item: PdfEngine.EditItem): RectF {
        val scaleX = if (item.baseWidth > 0f && width > 0) width.toFloat() / item.baseWidth else 1f
        val scaleY = if (item.baseHeight > 0f && height > 0) height.toFloat() / item.baseHeight else 1f
        val density = resources.displayMetrics.density

        return when (item.type) {
            PdfEngine.AnnotationType.PEN,
            PdfEngine.AnnotationType.DRAW,
            PdfEngine.AnnotationType.HIGHLIGHTER -> {
                if (item.points.isNotEmpty()) {
                    var minX = Float.MAX_VALUE
                    var maxX = -Float.MAX_VALUE
                    var minY = Float.MAX_VALUE
                    var maxY = -Float.MAX_VALUE
                    for (pt in item.points) {
                        val px = pt.first * scaleX
                        val py = pt.second * scaleY
                        if (px < minX) minX = px
                        if (px > maxX) maxX = px
                        if (py < minY) minY = py
                        if (py > maxY) maxY = py
                    }
                    val pad = max(16f, (item.strokeWidth * density) / 2f + 8f)
                    RectF(minX - pad, minY - pad, maxX + pad, maxY + pad)
                } else if (item.drawingBitmap != null) {
                    val l = item.posX * scaleX
                    val t = item.posY * scaleY
                    val r = l + (item.drawingBitmap.width.toFloat() * scaleX)
                    val b = t + (item.drawingBitmap.height.toFloat() * scaleY)
                    RectF(l, t, r, b)
                } else {
                    RectF(item.posX * scaleX - 20f, item.posY * scaleY - 20f, item.posX * scaleX + 20f, item.posY * scaleY + 20f)
                }
            }

            PdfEngine.AnnotationType.RECTANGLE,
            PdfEngine.AnnotationType.CIRCLE -> {
                val l = min(item.posX * scaleX, item.endX * scaleX)
                val t = min(item.posY * scaleY, item.endY * scaleY)
                val r = max(item.posX * scaleX, item.endX * scaleX)
                val b = max(item.posY * scaleY, item.endY * scaleY)
                val pad = max(12f, (item.strokeWidth * density) / 2f + 4f)
                RectF(l - pad, t - pad, r + pad, b + pad)
            }

            PdfEngine.AnnotationType.LINE,
            PdfEngine.AnnotationType.ARROW -> {
                val l = min(item.posX * scaleX, item.endX * scaleX)
                val t = min(item.posY * scaleY, item.endY * scaleY)
                val r = max(item.posX * scaleX, item.endX * scaleX)
                val b = max(item.posY * scaleY, item.endY * scaleY)
                val pad = max(20f, (item.strokeWidth * density * 2f))
                RectF(l - pad, t - pad, r + pad, b + pad)
            }

            else -> {
                val l = item.posX * scaleX
                val t = item.posY * scaleY
                val r = item.endX * scaleX
                val b = item.endY * scaleY
                RectF(min(l, r), min(t, b), max(l, r), max(t, b))
            }
        }
    }

    private fun getCornerHandles(bounds: RectF): List<Pair<Float, Float>> {
        return listOf(
            Pair(bounds.left, bounds.top),       // 0: Top-Left
            Pair(bounds.right, bounds.top),      // 1: Top-Right
            Pair(bounds.left, bounds.bottom),    // 2: Bottom-Left
            Pair(bounds.right, bounds.bottom)    // 3: Bottom-Right
        )
    }

    private fun findCornerHandleHit(bounds: RectF, touchX: Float, touchY: Float): Int {
        val touchRadius = 24f * resources.displayMetrics.density
        val handles = getCornerHandles(bounds)
        for (i in handles.indices) {
            val (hx, hy) = handles[i]
            if (hypot(touchX - hx, touchY - hy) <= touchRadius) {
                return i
            }
        }
        return -1
    }

    fun findAnnotationAt(touchX: Float, touchY: Float): PdfEngine.EditItem? {
        val currentList = pageAnnotations[currentPageIndex] ?: return null
        val density = resources.displayMetrics.density
        val searchRadius = 18f * density

        for (i in currentList.indices.reversed()) {
            val item = currentList[i]
            val isHit = when (item.type) {
                PdfEngine.AnnotationType.PEN,
                PdfEngine.AnnotationType.DRAW,
                PdfEngine.AnnotationType.HIGHLIGHTER -> {
                    checkStrokeIntersection(item, touchX, touchY, searchRadius)
                }

                PdfEngine.AnnotationType.LINE,
                PdfEngine.AnnotationType.ARROW -> {
                    val halfW = (item.strokeWidth * density) / 2f
                    val scaleX = if (item.baseWidth > 0f && width > 0) width.toFloat() / item.baseWidth else 1f
                    val scaleY = if (item.baseHeight > 0f && height > 0) height.toFloat() / item.baseHeight else 1f
                    distToSegment(touchX, touchY, item.posX * scaleX, item.posY * scaleY, item.endX * scaleX, item.endY * scaleY) <= (searchRadius + halfW)
                }

                PdfEngine.AnnotationType.RECTANGLE -> {
                    checkRectangleIntersection(item, touchX, touchY, searchRadius)
                }

                PdfEngine.AnnotationType.CIRCLE -> {
                    checkEllipseIntersection(item, touchX, touchY, searchRadius)
                }

                else -> false
            }

            if (isHit) {
                return item
            }
        }
        return null
    }

    private fun transformItem(
        initial: PdfEngine.EditItem,
        dx: Float,
        dy: Float,
        mode: TouchInteraction,
        corner: Int,
        initialBounds: RectF
    ): PdfEngine.EditItem {
        val scaleX = if (initial.baseWidth > 0f && width > 0) width.toFloat() / initial.baseWidth else 1f
        val scaleY = if (initial.baseHeight > 0f && height > 0) height.toFloat() / initial.baseHeight else 1f

        if (mode == TouchInteraction.MOVE_ANNOTATION) {
            val unscaleDx = dx / scaleX
            val unscaleDy = dy / scaleY

            return when (initial.type) {
                PdfEngine.AnnotationType.PEN,
                PdfEngine.AnnotationType.DRAW,
                PdfEngine.AnnotationType.HIGHLIGHTER -> {
                    val newPts = initial.points.map { Pair(it.first + unscaleDx, it.second + unscaleDy) }
                    initial.copy(
                        points = newPts,
                        posX = initial.posX + unscaleDx,
                        posY = initial.posY + unscaleDy,
                        endX = initial.endX + unscaleDx,
                        endY = initial.endY + unscaleDy
                    )
                }
                else -> {
                    initial.copy(
                        posX = initial.posX + unscaleDx,
                        posY = initial.posY + unscaleDy,
                        endX = initial.endX + unscaleDx,
                        endY = initial.endY + unscaleDy
                    )
                }
            }
        } else if (mode == TouchInteraction.RESIZE_ANNOTATION) {
            val origL = initialBounds.left
            val origT = initialBounds.top
            val origR = initialBounds.right
            val origB = initialBounds.bottom
            val minDim = 24f

            var newL = origL
            var newT = origT
            var newR = origR
            var newB = origB

            when (corner) {
                0 -> { // Top-Left
                    newL = min(origR - minDim, origL + dx)
                    newT = min(origB - minDim, origT + dy)
                }
                1 -> { // Top-Right
                    newR = max(origL + minDim, origR + dx)
                    newT = min(origB - minDim, origT + dy)
                }
                2 -> { // Bottom-Left
                    newL = min(origR - minDim, origL + dx)
                    newB = max(origT + minDim, origB + dy)
                }
                3 -> { // Bottom-Right
                    newR = max(origL + minDim, origR + dx)
                    newB = max(origT + minDim, origB + dy)
                }
            }

            val origW = max(1f, origR - origL)
            val origH = max(1f, origB - origT)
            val newW = max(1f, newR - newL)
            val newH = max(1f, newB - newT)

            fun mapPoint(px: Float, py: Float): Pair<Float, Float> {
                val u = (px - origL) / origW
                val v = (py - origT) / origH
                val nx = newL + u * newW
                val ny = newT + v * newH
                return Pair(nx / scaleX, ny / scaleY)
            }

            return when (initial.type) {
                PdfEngine.AnnotationType.PEN,
                PdfEngine.AnnotationType.DRAW,
                PdfEngine.AnnotationType.HIGHLIGHTER -> {
                    val newPts = initial.points.map { pt ->
                        mapPoint(pt.first * scaleX, pt.second * scaleY)
                    }
                    initial.copy(
                        points = newPts,
                        posX = newL / scaleX,
                        posY = newT / scaleY,
                        endX = newR / scaleX,
                        endY = newB / scaleY
                    )
                }
                PdfEngine.AnnotationType.RECTANGLE,
                PdfEngine.AnnotationType.CIRCLE -> {
                    initial.copy(
                        posX = newL / scaleX,
                        posY = newT / scaleY,
                        endX = newR / scaleX,
                        endY = newB / scaleY
                    )
                }
                PdfEngine.AnnotationType.LINE,
                PdfEngine.AnnotationType.ARROW -> {
                    val (spX, spY) = mapPoint(initial.posX * scaleX, initial.posY * scaleY)
                    val (epX, epY) = mapPoint(initial.endX * scaleX, initial.endY * scaleY)
                    initial.copy(
                        posX = spX,
                        posY = spY,
                        endX = epX,
                        endY = epY
                    )
                }
                else -> initial
            }
        }
        return initial
    }

    // --- Mode B: Manual Partial Pixel Eraser ---

    private var pendingManualRemovedItems: List<PdfEngine.EditItem>? = null
    private var isManualEraserLayerInitialized = false

    private fun prepareDrawingLayerForManualEraser() {
        if (width <= 0 || height <= 0) return
        val currentList = pageAnnotations.getOrPut(currentPageIndex) { mutableListOf() }

        if (drawingLayerBitmap == null || drawingLayerBitmap?.width != width || drawingLayerBitmap?.height != height) {
            drawingLayerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            drawingLayerCanvas = Canvas(drawingLayerBitmap!!)
        }
        val bmpCanvas = drawingLayerCanvas ?: return
        bmpCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (currentList.isNotEmpty()) {
            // Render current vector items into transparent drawingLayerBitmap
            renderAnnotationsToCanvas(bmpCanvas, currentList)
            pendingManualRemovedItems = currentList.toList()
        } else {
            pendingManualRemovedItems = null
        }

        // Replace individual items with the single baked drawing layer item internally
        manualEraserItem = PdfEngine.EditItem(
            pageIndex = currentPageIndex,
            type = PdfEngine.AnnotationType.DRAW,
            drawingBitmap = drawingLayerBitmap,
            baseWidth = width.toFloat(),
            baseHeight = height.toFloat()
        )
        currentList.clear()
        currentList.add(manualEraserItem!!)
        isManualEraserLayerInitialized = true
        // NOTE: Defer notifying Compose (onAnnotationsRemoved/onAnnotationAdded) until ACTION_UP
        // to avoid mid-gesture recomposition that collapses toolbars / bottom sheets.
    }

    private fun applyManualEraserAt(x: Float, y: Float, lx: Float, ly: Float) {
        val bmpCanvas = drawingLayerCanvas ?: return
        val radiusPx = eraserSize.radiusDp * resources.displayMetrics.density

        manualErasePaint.strokeWidth = radiusPx * 2f
        bmpCanvas.drawLine(lx, ly, x, y, manualErasePaint)
        bmpCanvas.drawCircle(x, y, radiusPx, manualEraseCirclePaint)
    }

    private fun finishManualEraserGesture() {
        if (manualEraserItem != null && drawingLayerBitmap != null && isManualEraserLayerInitialized) {
            onHistoryPush?.invoke()
            pendingManualRemovedItems?.let { removed ->
                if (removed.isNotEmpty()) {
                    onAnnotationsRemoved?.invoke(removed)
                }
            }
            pendingManualRemovedItems = null

            val updated = manualEraserItem!!.copy(
                drawingBitmap = drawingLayerBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            )
            val currentList = pageAnnotations[currentPageIndex]
            val idx = currentList?.indexOfFirst { it.id == manualEraserItem!!.id } ?: -1
            if (idx != -1) {
                currentList!![idx] = updated
                manualEraserItem = updated
            }
            onAnnotationAdded?.invoke(updated)
            isManualEraserLayerInitialized = false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isInteractionEnabled) {
            parent?.requestDisallowInterceptTouchEvent(true)
            disallowParentIntercept(true)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInteractionEnabled) {
            return false
        }
        parent?.requestDisallowInterceptTouchEvent(true)
        disallowParentIntercept(true)

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                disallowParentIntercept(true)
                touchStartX = x
                touchStartY = y
                lastX = x
                lastY = y

                // 1. Check if an existing selected annotation is touched on its handles or body
                val selectedItem = pageAnnotations[currentPageIndex]?.find { it.id == selectedAnnotationId }
                if (selectedItem != null) {
                    val bounds = computeAnnotationBounds(selectedItem)
                    val cornerHit = findCornerHandleHit(bounds, x, y)
                    if (cornerHit != -1) {
                        activeInteraction = TouchInteraction.RESIZE_ANNOTATION
                        activeResizeCorner = cornerHit
                        initialItemState = selectedItem
                        initialItemBounds = bounds
                        invalidate()
                        return true
                    } else if (bounds.contains(x, y)) {
                        activeInteraction = TouchInteraction.MOVE_ANNOTATION
                        initialItemState = selectedItem
                        initialItemBounds = bounds
                        invalidate()
                        return true
                    }
                }

                // 2. If in SELECT mode or user taps an annotation:
                if (currentTool == ToolMode.SELECT) {
                    val hit = findAnnotationAt(x, y)
                    if (hit != null) {
                        selectedAnnotationId = hit.id
                        onAnnotationSelected?.invoke(hit.id)
                        activeInteraction = TouchInteraction.MOVE_ANNOTATION
                        initialItemState = hit
                        initialItemBounds = computeAnnotationBounds(hit)
                    } else {
                        selectedAnnotationId = null
                        onAnnotationSelected?.invoke(null)
                        activeInteraction = TouchInteraction.NONE
                    }
                    invalidate()
                    return true
                }

                // 3. If user directly taps an annotation while not using eraser:
                if (currentTool != ToolMode.ERASER) {
                    val hit = findAnnotationAt(x, y)
                    if (hit != null && selectedAnnotationId == null) {
                        selectedAnnotationId = hit.id
                        onAnnotationSelected?.invoke(hit.id)
                        activeInteraction = TouchInteraction.MOVE_ANNOTATION
                        initialItemState = hit
                        initialItemBounds = computeAnnotationBounds(hit)
                        invalidate()
                        return true
                    }
                }

                // 4. Tool-specific actions
                when (currentTool) {
                    ToolMode.PEN, ToolMode.HIGHLIGHT -> {
                        selectedAnnotationId = null
                        onAnnotationSelected?.invoke(null)
                        activeInteraction = TouchInteraction.DRAW_STROKE
                        activePoints.clear()
                        activePoints.add(x to y)
                        activePath.reset()
                        activePath.moveTo(x, y)
                    }

                    ToolMode.SHAPE -> {
                        selectedAnnotationId = null
                        onAnnotationSelected?.invoke(null)
                        activeInteraction = TouchInteraction.DRAW_SHAPE
                        isDrawingShape = true
                        shapeStartX = x
                        shapeStartY = y
                        shapeCurrentX = x
                        shapeCurrentY = y
                    }

                    ToolMode.ERASER -> {
                        selectedAnnotationId = null
                        onAnnotationSelected?.invoke(null)
                        isErasing = true
                        eraserTouchX = x
                        eraserTouchY = y

                        if (eraserType == EraserType.STROKE) {
                            activeInteraction = TouchInteraction.ERASE_STROKE
                            removedDuringCurrentGesture.clear()
                            checkAndEraseAnnotationsAt(x, y)
                        } else {
                            activeInteraction = TouchInteraction.ERASE_MANUAL
                            prepareDrawingLayerForManualEraser()
                            applyManualEraserAt(x, y, lastX, lastY)
                        }
                    }

                    ToolMode.SELECT -> {}
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                disallowParentIntercept(true)

                when (activeInteraction) {
                    TouchInteraction.MOVE_ANNOTATION, TouchInteraction.RESIZE_ANNOTATION -> {
                        val init = initialItemState ?: return true
                        val dx = x - touchStartX
                        val dy = y - touchStartY
                        val transformed = transformItem(init, dx, dy, activeInteraction, activeResizeCorner, initialItemBounds)
                        val currentList = pageAnnotations[currentPageIndex] ?: return true
                        val idx = currentList.indexOfFirst { it.id == transformed.id }
                        if (idx != -1) {
                            currentList[idx] = transformed
                            onAnnotationUpdated?.invoke(transformed)
                        }
                        invalidate()
                        return true
                    }

                    TouchInteraction.DRAW_STROKE -> {
                        val midX = (lastX + x) / 2f
                        val midY = (lastY + y) / 2f
                        activePath.quadTo(lastX, lastY, midX, midY)
                        lastX = x
                        lastY = y
                        activePoints.add(x to y)
                        invalidate()
                        return true
                    }

                    TouchInteraction.DRAW_SHAPE -> {
                        shapeCurrentX = x
                        shapeCurrentY = y
                        invalidate()
                        return true
                    }

                    TouchInteraction.ERASE_STROKE -> {
                        eraserTouchX = x
                        eraserTouchY = y
                        checkAndEraseAnnotationsAt(x, y)
                        invalidate()
                        return true
                    }

                    TouchInteraction.ERASE_MANUAL -> {
                        eraserTouchX = x
                        eraserTouchY = y
                        applyManualEraserAt(x, y, lastX, lastY)
                        lastX = x
                        lastY = y
                        invalidate()
                        return true
                    }

                    TouchInteraction.NONE -> return false
                }
            }

            MotionEvent.ACTION_UP -> {
                disallowParentIntercept(false)

                when (activeInteraction) {
                    TouchInteraction.MOVE_ANNOTATION, TouchInteraction.RESIZE_ANNOTATION -> {
                        val init = initialItemState
                        if (init != null) {
                            val currentList = pageAnnotations[currentPageIndex]
                            val finalItem = currentList?.find { it.id == init.id }
                            if (finalItem != null && finalItem != init) {
                                onHistoryPush?.invoke()
                                onAnnotationUpdated?.invoke(finalItem)
                            }
                        }
                        initialItemState = null
                    }

                    TouchInteraction.DRAW_STROKE -> {
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

                    TouchInteraction.DRAW_SHAPE -> {
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

                    TouchInteraction.ERASE_STROKE -> {
                        isErasing = false
                        if (removedDuringCurrentGesture.isNotEmpty()) {
                            onAnnotationsRemoved?.invoke(removedDuringCurrentGesture.toList())
                        }
                        removedDuringCurrentGesture.clear()
                    }

                    TouchInteraction.ERASE_MANUAL -> {
                        isErasing = false
                        finishManualEraserGesture()
                    }

                    TouchInteraction.NONE -> {}
                }

                activeInteraction = TouchInteraction.NONE
                activeResizeCorner = -1
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                disallowParentIntercept(false)
                activePoints.clear()
                activePath.reset()
                isDrawingShape = false
                isErasing = false
                removedDuringCurrentGesture.clear()
                activeInteraction = TouchInteraction.NONE
                activeResizeCorner = -1
                initialItemState = null
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Mode A: Auto Object Eraser:
     * Erases stroke paths and shape objects intersecting the eraser touch circle.
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
                    val scaleX = if (item.baseWidth > 0f && width > 0) width.toFloat() / item.baseWidth else 1f
                    val scaleY = if (item.baseHeight > 0f && height > 0) height.toFloat() / item.baseHeight else 1f
                    distToSegment(touchX, touchY, item.posX * scaleX, item.posY * scaleY, item.endX * scaleX, item.endY * scaleY) <= (radiusPx + halfW)
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
            if (toRemove.any { it.id == selectedAnnotationId }) {
                selectedAnnotationId = null
                onAnnotationSelected?.invoke(null)
            }
        }
    }

    private fun checkStrokeIntersection(item: PdfEngine.EditItem, touchX: Float, touchY: Float, radiusPx: Float): Boolean {
        val pts = item.points
        if (pts.isEmpty()) {
            if (item.drawingBitmap != null) {
                val scaleX = if (item.baseWidth > 0f && width > 0) width.toFloat() / item.baseWidth else 1f
                val scaleY = if (item.baseHeight > 0f && height > 0) height.toFloat() / item.baseHeight else 1f
                val l = item.posX * scaleX
                val t = item.posY * scaleY
                val r = l + (item.drawingBitmap.width.toFloat() * scaleX)
                val b = t + (item.drawingBitmap.height.toFloat() * scaleY)
                return touchX in (l - radiusPx)..(r + radiusPx) && touchY in (t - radiusPx)..(b + radiusPx)
            }
            return false
        }
        val scaleX = if (item.baseWidth > 0f && width > 0) width.toFloat() / item.baseWidth else 1f
        val scaleY = if (item.baseHeight > 0f && height > 0) height.toFloat() / item.baseHeight else 1f
        val halfW = (item.strokeWidth * resources.displayMetrics.density * ((scaleX + scaleY) / 2f)) / 2f
        val hitThreshold = radiusPx + halfW

        if (pts.size == 1) {
            return hypot(touchX - pts[0].first * scaleX, touchY - pts[0].second * scaleY) <= hitThreshold
        }

        for (i in 0 until pts.size - 1) {
            val p1x = pts[i].first * scaleX
            val p1y = pts[i].second * scaleY
            val p2x = pts[i + 1].first * scaleX
            val p2y = pts[i + 1].second * scaleY
            if (distToSegment(touchX, touchY, p1x, p1y, p2x, p2y) <= hitThreshold) {
                return true
            }
        }
        return false
    }

    private fun checkRectangleIntersection(item: PdfEngine.EditItem, touchX: Float, touchY: Float, radiusPx: Float): Boolean {
        val scaleX = if (item.baseWidth > 0f && width > 0) width.toFloat() / item.baseWidth else 1f
        val scaleY = if (item.baseHeight > 0f && height > 0) height.toFloat() / item.baseHeight else 1f
        val left = min(item.posX * scaleX, item.endX * scaleX)
        val top = min(item.posY * scaleY, item.endY * scaleY)
        val right = max(item.posX * scaleX, item.endX * scaleX)
        val bottom = max(item.posY * scaleY, item.endY * scaleY)
        val halfW = (item.strokeWidth * resources.displayMetrics.density * ((scaleX + scaleY) / 2f)) / 2f
        val threshold = radiusPx + halfW

        if (item.fillColorHex != null) {
            if (touchX in (left - radiusPx)..(right + radiusPx) &&
                touchY in (top - radiusPx)..(bottom + radiusPx)
            ) {
                return true
            }
        }

        if (distToSegment(touchX, touchY, left, top, right, top) <= threshold) return true
        if (distToSegment(touchX, touchY, right, top, right, bottom) <= threshold) return true
        if (distToSegment(touchX, touchY, right, bottom, left, bottom) <= threshold) return true
        if (distToSegment(touchX, touchY, left, bottom, left, top) <= threshold) return true

        return false
    }

    private fun checkEllipseIntersection(item: PdfEngine.EditItem, touchX: Float, touchY: Float, radiusPx: Float): Boolean {
        val scaleX = if (item.baseWidth > 0f && width > 0) width.toFloat() / item.baseWidth else 1f
        val scaleY = if (item.baseHeight > 0f && height > 0) height.toFloat() / item.baseHeight else 1f
        val left = min(item.posX * scaleX, item.endX * scaleX)
        val top = min(item.posY * scaleY, item.endY * scaleY)
        val right = max(item.posX * scaleX, item.endX * scaleX)
        val bottom = max(item.posY * scaleY, item.endY * scaleY)

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
        val halfW = (item.strokeWidth * resources.displayMetrics.density * ((scaleX + scaleY) / 2f)) / 2f
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

    private fun renderAnnotationsToCanvas(canvas: Canvas, items: List<PdfEngine.EditItem>) {
        val density = resources.displayMetrics.density

        for (item in items) {
            val scaleX = if (item.baseWidth > 0f && width > 0) width.toFloat() / item.baseWidth else 1f
            val scaleY = if (item.baseHeight > 0f && height > 0) height.toFloat() / item.baseHeight else 1f
            val strokeScale = (scaleX + scaleY) / 2f

            when (item.type) {
                PdfEngine.AnnotationType.PEN,
                PdfEngine.AnnotationType.DRAW,
                PdfEngine.AnnotationType.HIGHLIGHTER -> {
                    if (item.drawingBitmap != null) {
                        canvas.drawBitmap(item.drawingBitmap, item.posX * scaleX, item.posY * scaleY, null)
                    }

                    val pts = item.points
                    if (pts.size >= 2) {
                        val path = Path()
                        path.moveTo(pts[0].first * scaleX, pts[0].second * scaleY)
                        for (i in 1 until pts.size) {
                            val prevX = pts[i - 1].first * scaleX
                            val prevY = pts[i - 1].second * scaleY
                            val currX = pts[i].first * scaleX
                            val currY = pts[i].second * scaleY
                            val midX = (prevX + currX) / 2f
                            val midY = (prevY + currY) / 2f
                            path.quadTo(prevX, prevY, midX, midY)
                        }
                        path.lineTo(pts.last().first * scaleX, pts.last().second * scaleY)

                        strokePaint.color = item.colorHex
                        strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        strokePaint.strokeWidth = item.strokeWidth * density * strokeScale
                        canvas.drawPath(path, strokePaint)
                    }
                }

                PdfEngine.AnnotationType.RECTANGLE -> {
                    val l = min(item.posX * scaleX, item.endX * scaleX)
                    val t = min(item.posY * scaleY, item.endY * scaleY)
                    val r = max(item.posX * scaleX, item.endX * scaleX)
                    val b = max(item.posY * scaleY, item.endY * scaleY)

                    if (item.fillColorHex != null) {
                        fillPaint.color = item.fillColorHex
                        fillPaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        canvas.drawRect(l, t, r, b, fillPaint)
                    }

                    strokePaint.color = item.colorHex
                    strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    strokePaint.strokeWidth = item.strokeWidth * density * strokeScale
                    canvas.drawRect(l, t, r, b, strokePaint)
                }

                PdfEngine.AnnotationType.CIRCLE -> {
                    val l = min(item.posX * scaleX, item.endX * scaleX)
                    val t = min(item.posY * scaleY, item.endY * scaleY)
                    val r = max(item.posX * scaleX, item.endX * scaleX)
                    val b = max(item.posY * scaleY, item.endY * scaleY)

                    if (item.fillColorHex != null) {
                        fillPaint.color = item.fillColorHex
                        fillPaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                        canvas.drawOval(l, t, r, b, fillPaint)
                    }

                    strokePaint.color = item.colorHex
                    strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    strokePaint.strokeWidth = item.strokeWidth * density * strokeScale
                    canvas.drawOval(l, t, r, b, strokePaint)
                }

                PdfEngine.AnnotationType.LINE -> {
                    strokePaint.color = item.colorHex
                    strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    strokePaint.strokeWidth = item.strokeWidth * density * strokeScale
                    canvas.drawLine(item.posX * scaleX, item.posY * scaleY, item.endX * scaleX, item.endY * scaleY, strokePaint)
                }

                PdfEngine.AnnotationType.ARROW -> {
                    val sX = item.posX * scaleX
                    val sY = item.posY * scaleY
                    val eX = item.endX * scaleX
                    val eY = item.endY * scaleY

                    strokePaint.color = item.colorHex
                    strokePaint.alpha = (item.opacity.coerceIn(0.01f, 1f) * 255).toInt()
                    strokePaint.strokeWidth = item.strokeWidth * density * strokeScale
                    canvas.drawLine(sX, sY, eX, eY, strokePaint)

                    val angle = atan2((eY - sY).toDouble(), (eX - sX).toDouble())
                    val arrowHeadLen = (item.strokeWidth * density * strokeScale * 3.5f).coerceIn(16f, 48f)
                    val x1 = (eX - arrowHeadLen * cos(angle - Math.PI / 6)).toFloat()
                    val y1 = (eY - arrowHeadLen * sin(angle - Math.PI / 6)).toFloat()
                    val x2 = (eX - arrowHeadLen * cos(angle + Math.PI / 6)).toFloat()
                    val y2 = (eY - arrowHeadLen * sin(angle + Math.PI / 6)).toFloat()
                    val headPath = Path().apply {
                        moveTo(x1, y1)
                        lineTo(eX, eY)
                        lineTo(x2, y2)
                    }
                    canvas.drawPath(headPath, strokePaint)
                }

                else -> {}
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density

        // 1. Render all committed annotations for the current page
        val currentList = pageAnnotations[currentPageIndex] ?: emptyList()
        renderAnnotationsToCanvas(canvas, currentList)

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

        // 5. Render Bounding Box and 4 Corner Resize Handles for selected annotation
        if (selectedAnnotationId != null) {
            val selectedItem = currentList.find { it.id == selectedAnnotationId }
            if (selectedItem != null) {
                val bounds = computeAnnotationBounds(selectedItem)
                selectedBoundingPaint.strokeWidth = 2f * density
                canvas.drawRect(bounds, selectedBoundingPaint)

                val handleRadius = 6.5f * density
                val handles = getCornerHandles(bounds)
                for ((hx, hy) in handles) {
                    canvas.drawCircle(hx, hy, handleRadius, handleFillPaint)
                    canvas.drawCircle(hx, hy, handleRadius, handleStrokePaint)
                }
            }
        }
    }
}
