package com.example.photo

import com.example.pdf.PdfEngine
import com.example.ui.screens.AnnotationCanvasOverlayView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DrawingViewModelTest {

    private lateinit var viewModel: DrawingViewModel

    @Before
    fun setUp() {
        viewModel = DrawingViewModel()
    }

    @Test
    fun testInitialDefaultState() {
        assertEquals(AnnotationCanvasOverlayView.ToolMode.PEN, viewModel.activeSubTool)
        assertEquals(6f, viewModel.penStrokeThickness, 0.01f)
        assertEquals(1.0f, viewModel.penOpacity, 0.01f)
        assertFalse(viewModel.isColorPickerActive)
        assertFalse(viewModel.showEmojiPicker)
        assertTrue(viewModel.annotations.isEmpty())
        assertFalse(viewModel.canUndo)
        assertFalse(viewModel.canRedo)
    }

    @Test
    fun testAnnotationAddAndUndoRedo() {
        val item1 = PdfEngine.EditItem(
            id = "item-1",
            pageIndex = 0,
            type = PdfEngine.AnnotationType.PEN,
            posX = 10f,
            posY = 10f,
            colorHex = 0xFF2563EB.toInt()
        )
        val item2 = PdfEngine.EditItem(
            id = "item-2",
            pageIndex = 0,
            type = PdfEngine.AnnotationType.RECTANGLE,
            posX = 20f,
            posY = 20f,
            endX = 100f,
            endY = 100f
        )

        viewModel.pushHistory()
        viewModel.addAnnotation(item1)
        assertEquals(1, viewModel.annotations.size)
        assertTrue(viewModel.canUndo)
        assertFalse(viewModel.canRedo)

        viewModel.pushHistory()
        viewModel.addAnnotation(item2)
        assertEquals(2, viewModel.annotations.size)

        // Undo 1
        viewModel.undo()
        assertEquals(1, viewModel.annotations.size)
        assertEquals("item-1", viewModel.annotations[0].id)
        assertTrue(viewModel.canUndo)
        assertTrue(viewModel.canRedo)

        // Redo 1
        viewModel.redo()
        assertEquals(2, viewModel.annotations.size)
        assertEquals("item-2", viewModel.annotations[1].id)
        assertFalse(viewModel.canRedo)
    }

    @Test
    fun testToolSwitchingAndReset() {
        viewModel.activeSubTool = AnnotationCanvasOverlayView.ToolMode.SHAPE
        viewModel.shapeType = AnnotationCanvasOverlayView.ShapeType.CIRCLE
        viewModel.arrowStyle = AnnotationCanvasOverlayView.ArrowStyle.DOUBLE_ENDED
        viewModel.eraserType = AnnotationCanvasOverlayView.EraserType.STROKE

        assertEquals(AnnotationCanvasOverlayView.ToolMode.SHAPE, viewModel.activeSubTool)
        assertEquals(AnnotationCanvasOverlayView.ShapeType.CIRCLE, viewModel.shapeType)
        assertEquals(AnnotationCanvasOverlayView.ArrowStyle.DOUBLE_ENDED, viewModel.arrowStyle)
        assertEquals(AnnotationCanvasOverlayView.EraserType.STROKE, viewModel.eraserType)

        val item = PdfEngine.EditItem(
            id = "item-test",
            pageIndex = 0,
            type = PdfEngine.AnnotationType.ARROW,
            posX = 0f,
            posY = 0f,
            endX = 50f,
            endY = 50f
        )
        viewModel.addAnnotation(item)
        assertEquals(1, viewModel.annotations.size)

        viewModel.reset()
        assertTrue(viewModel.annotations.isEmpty())
        assertFalse(viewModel.canUndo)
        assertFalse(viewModel.canRedo)
        assertNull(viewModel.selectedAnnotationId)
        assertFalse(viewModel.isColorPickerActive)
        assertFalse(viewModel.showEmojiPicker)
    }

    @Test
    fun testRemoveAnnotations() {
        val item1 = PdfEngine.EditItem(id = "1", pageIndex = 0, type = PdfEngine.AnnotationType.PEN)
        val item2 = PdfEngine.EditItem(id = "2", pageIndex = 0, type = PdfEngine.AnnotationType.HIGHLIGHTER)
        viewModel.addAnnotation(item1)
        viewModel.addAnnotation(item2)
        assertEquals(2, viewModel.annotations.size)

        viewModel.removeAnnotations(listOf(item1))
        assertEquals(1, viewModel.annotations.size)
        assertEquals("2", viewModel.annotations[0].id)
    }
}
