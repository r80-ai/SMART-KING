package com.example.photo

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pdf.PdfEngine
import com.example.ui.screens.AnnotationCanvasOverlayView
import java.util.UUID

/**
 * State holder and ViewModel for the advanced Drawing & Pen markup engine in Photo Editing.
 * Completely ports and aligns with the PDF Edit -> Pen feature set:
 * - Pen (smooth quadTo bezier curves, customizable thickness & opacity)
 * - Highlighter (translucent blending, custom colors & width)
 * - Shapes & Arrows (Rectangle, Circle, Line, all 7 Arrow Styles, stroke/fill options)
 * - Vector Icons & Symbols (Checkmarks, Crossmarks, Stars, Badges, etc.)
 * - Emoji Stamps (scalable, movable, rotatable)
 * - Advanced Eraser (smooth PorterDuff.Mode.CLEAR stroke eraser & object eraser)
 * - Real-time Eyedropper Color Picker
 * - Full Undo / Redo history
 */
class DrawingViewModel : ViewModel() {
    var activeSubTool by mutableStateOf(AnnotationCanvasOverlayView.ToolMode.PEN)

    // PEN TOOL OPTIONS
    var penColor by mutableIntStateOf(AndroidColor.rgb(37, 99, 235)) // Royal Blue
    var penStrokeThickness by mutableFloatStateOf(6f)
    var penOpacity by mutableFloatStateOf(1.0f)

    // HIGHLIGHTER TOOL OPTIONS
    var highlightColor by mutableIntStateOf(AndroidColor.rgb(250, 204, 21)) // Fluorescent Yellow
    var highlightWidth by mutableFloatStateOf(24f)
    var highlightOpacity by mutableFloatStateOf(0.40f)

    // SHAPES & ARROWS OPTIONS
    var shapeType by mutableStateOf(AnnotationCanvasOverlayView.ShapeType.RECTANGLE)
    var arrowStyle by mutableStateOf(AnnotationCanvasOverlayView.ArrowStyle.SINGLE)
    var shapeColor by mutableIntStateOf(AndroidColor.rgb(220, 38, 38)) // Crimson Red
    var shapeStrokeWidth by mutableFloatStateOf(4f)
    var shapeFillColor by mutableStateOf<Int?>(null)

    // VECTOR ICONS & SYMBOLS OPTIONS
    var iconType by mutableStateOf("TICK_CLEAN")
    var iconColor by mutableIntStateOf(AndroidColor.rgb(22, 163, 74)) // Emerald Green
    var iconStrokeWidth by mutableFloatStateOf(4f)
    var iconFillColor by mutableStateOf<Int?>(null)

    // ERASER OPTIONS
    var eraserSize by mutableStateOf(AnnotationCanvasOverlayView.EraserSize.MEDIUM)
    var eraserStrokeWidth by mutableFloatStateOf(44f)
    var eraserType by mutableStateOf(AnnotationCanvasOverlayView.EraserType.MANUAL) // Smooth CLEAR Stroke Eraser

    // EYEDROPPER STATE
    var isColorPickerActive by mutableStateOf(false)
    var colorPickerTarget by mutableStateOf("PEN") // "PEN", "HIGHLIGHT", "SHAPE", "ICON"

    // EMOJI STATE
    var showEmojiPicker by mutableStateOf(false)

    // SELECTION & ANNOTATIONS
    var selectedAnnotationId by mutableStateOf<String?>(null)
    val annotations = mutableStateListOf<PdfEngine.EditItem>()

    // UNDO / REDO HISTORY
    private val undoStack = mutableListOf<List<PdfEngine.EditItem>>()
    private val redoStack = mutableListOf<List<PdfEngine.EditItem>>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private fun updateUndoRedoState() {
        canUndo = undoStack.isNotEmpty() || annotations.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun pushHistory() {
        undoStack.add(annotations.map { it.copy() })
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoState()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(annotations.map { it.copy() })
            val previous = undoStack.removeLast()
            annotations.clear()
            annotations.addAll(previous)
            selectedAnnotationId = null
            updateUndoRedoState()
        } else if (annotations.isNotEmpty()) {
            redoStack.add(annotations.map { it.copy() })
            annotations.clear()
            selectedAnnotationId = null
            updateUndoRedoState()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(annotations.map { it.copy() })
            val next = redoStack.removeLast()
            annotations.clear()
            annotations.addAll(next)
            selectedAnnotationId = null
            updateUndoRedoState()
        }
    }

    fun clearAll() {
        if (annotations.isNotEmpty()) {
            pushHistory()
            annotations.clear()
            selectedAnnotationId = null
            updateUndoRedoState()
        }
    }

    fun deleteSelected() {
        val id = selectedAnnotationId ?: return
        pushHistory()
        annotations.removeAll { it.id == id }
        selectedAnnotationId = null
        updateUndoRedoState()
    }

    fun addAnnotation(item: PdfEngine.EditItem) {
        annotations.add(item)
        updateUndoRedoState()
    }

    fun updateAnnotation(item: PdfEngine.EditItem) {
        val idx = annotations.indexOfFirst { it.id == item.id }
        if (idx != -1) {
            annotations[idx] = item
        }
        updateUndoRedoState()
    }

    fun removeAnnotations(items: List<PdfEngine.EditItem>) {
        annotations.removeAll(items)
        if (items.any { it.id == selectedAnnotationId }) {
            selectedAnnotationId = null
        }
        updateUndoRedoState()
    }

    fun addEmoji(emoji: String, viewWidth: Float, viewHeight: Float, baseW: Float, baseH: Float) {
        pushHistory()
        val centerX = if (viewWidth > 0f) viewWidth / 2f else 300f
        val centerY = if (viewHeight > 0f) viewHeight / 2f else 300f
        val emojiItem = PdfEngine.EditItem(
            id = UUID.randomUUID().toString(),
            pageIndex = 0,
            type = PdfEngine.AnnotationType.TEXT,
            text = emoji,
            posX = centerX,
            posY = centerY,
            fontSize = 48f,
            colorHex = AndroidColor.BLACK,
            alignment = "CENTER",
            baseWidth = if (baseW > 0f) baseW else viewWidth,
            baseHeight = if (baseH > 0f) baseH else viewHeight
        )
        annotations.add(emojiItem)
        selectedAnnotationId = emojiItem.id
        updateUndoRedoState()
    }

    fun reset(clearHistory: Boolean = true) {
        annotations.clear()
        if (clearHistory) {
            undoStack.clear()
            redoStack.clear()
        }
        selectedAnnotationId = null
        activeSubTool = AnnotationCanvasOverlayView.ToolMode.PEN
        isColorPickerActive = false
        showEmojiPicker = false
        updateUndoRedoState()
    }
}
