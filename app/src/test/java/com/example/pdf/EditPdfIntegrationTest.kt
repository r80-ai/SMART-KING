package com.example.pdf

import android.graphics.Color
import android.graphics.Rect
import com.example.ui.ActiveModal
import com.example.ui.PdfNavRoutes
import com.example.ui.routeToActiveModal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditPdfIntegrationTest {

    @Test
    fun testEditPdfNavigationRouteResolvesCorrectly() {
        assertEquals("edit_pdf", PdfNavRoutes.ROUTE_EDIT_PDF)
        val resolvedModal = routeToActiveModal(PdfNavRoutes.ROUTE_EDIT_PDF)
        assertEquals(ActiveModal.EDIT, resolvedModal)
    }

    @Test
    fun testPdfEditToolDefinition() {
        val tool = com.example.ui.screens.getToolById("edit_pdf")
        assertNotNull(tool)
        assertEquals("PDF Edit", tool?.title)
        assertEquals(ActiveModal.EDIT, tool?.modal)
    }

    @Test
    fun testOcrReplacementAnnotationItemStructure() {
        val mask = Rect(50, 100, 250, 140)
        val item = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.OCR_REPLACEMENT,
            text = "Updated Document Title",
            posX = 50f,
            posY = 135f,
            fontSize = 18f,
            colorHex = Color.BLACK,
            maskRect = mask,
            maskColor = Color.WHITE,
            isBold = true,
            isItalic = false,
            alignment = "LEFT"
        )

        assertEquals(PdfEngine.AnnotationType.OCR_REPLACEMENT, item.type)
        assertEquals("Updated Document Title", item.text)
        assertEquals(mask, item.maskRect)
        assertEquals(Color.WHITE, item.maskColor)
        assertEquals(Color.BLACK, item.colorHex)
        assertTrue(item.isBold)
        assertEquals("LEFT", item.alignment)
        assertNotNull(item.id)
    }

    @Test
    fun testWatermarkAnnotationItemStructure() {
        val item = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.TEXT,
            text = "CONFIDENTIAL",
            posX = 300f,
            posY = 400f,
            fontSize = 42f,
            colorHex = Color.RED,
            opacity = 0.35f,
            rotation = -45f,
            alignment = "CENTER",
            isBold = true
        )

        assertEquals(PdfEngine.AnnotationType.TEXT, item.type)
        assertEquals("CONFIDENTIAL", item.text)
        assertEquals(0.35f, item.opacity, 0.01f)
        assertEquals(-45f, item.rotation, 0.01f)
        assertEquals("CENTER", item.alignment)
    }

    @Test
    fun testEncryptedPdfFileNameResolutionForMultipleFiles() {
        // PDF 1: ANHARA.pdf with internal storage timestamp prefix
        val pdf1 = com.example.model.PdfFileRecord(
            id = 101,
            name = "1792324996557_ANHARA.pdf",
            filePath = "/data/user/0/com.aistudio.pdfmaster/files/imported_pdfs/1792324996557_ANHARA.pdf",
            fileSize = 1048576L,
            pageCount = 3,
            category = "downloads",
            isProtected = true
        )
        assertEquals("ANHARA.pdf", pdf1.resolveActualFileName())

        // PDF 2: CONFIDENTIAL_FINANCIALS.pdf imported directly with clean name
        val pdf2 = com.example.model.PdfFileRecord(
            id = 102,
            name = "CONFIDENTIAL_FINANCIALS.pdf",
            filePath = "/storage/emulated/0/Download/CONFIDENTIAL_FINANCIALS.pdf",
            fileSize = 2097152L,
            pageCount = 12,
            category = "downloads",
            isProtected = true
        )
        assertEquals("CONFIDENTIAL_FINANCIALS.pdf", pdf2.resolveActualFileName())

        // PDF 3: Fallback handling when name and path are blank
        val emptyPdf = com.example.model.PdfFileRecord(
            id = 103,
            name = "",
            filePath = "",
            fileSize = 0L,
            pageCount = 0,
            category = "downloads"
        )
        assertEquals("Unknown PDF", emptyPdf.resolveActualFileName())
    }
}
