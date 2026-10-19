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

    @Test
    fun testImageItemCoordinateConversionBetweenScreenAndPage() {
        val baseW = 1200f
        val baseH = 1600f
        val pageWidthPx = 600f
        val pageHeightPx = 800f

        val item = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            posX = 300f,
            posY = 400f,
            imageWidth = 600f,
            imageHeight = 800f,
            baseWidth = baseW,
            baseHeight = baseH
        )

        val toScreenScaleX = pageWidthPx / item.baseWidth
        val toScreenScaleY = pageHeightPx / item.baseHeight

        val screenX = item.posX * toScreenScaleX
        val screenY = item.posY * toScreenScaleY
        val screenW = item.imageWidth * toScreenScaleX
        val screenH = item.imageHeight * toScreenScaleY

        assertEquals(150f, screenX, 0.01f)
        assertEquals(200f, screenY, 0.01f)
        assertEquals(300f, screenW, 0.01f)
        assertEquals(400f, screenH, 0.01f)

        // Convert back
        val toPageScaleX = item.baseWidth / pageWidthPx
        val toPageScaleY = item.baseHeight / pageHeightPx

        assertEquals(300f, screenX * toPageScaleX, 0.01f)
        assertEquals(400f, screenY * toPageScaleY, 0.01f)
        assertEquals(600f, screenW * toPageScaleX, 0.01f)
        assertEquals(800f, screenH * toPageScaleY, 0.01f)
    }

    @Test
    fun testFourCornerHandlesResizeWidthAndHeightIndependently() {
        val baseW = 1000f
        val baseH = 1000f
        val pageWidthPx = 500f
        val pageHeightPx = 500f
        val toPageX = baseW / pageWidthPx // 2.0
        val toPageY = baseH / pageHeightPx // 2.0

        val initial = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            posX = 200f,
            posY = 200f,
            imageWidth = 400f,
            imageHeight = 400f,
            baseWidth = baseW,
            baseHeight = baseH
        )

        // 1. TOP-LEFT handle: drag by (-10 local px, -20 local px) -> (-20 page px, -40 page px)
        // Image should become wider (+20) and taller (+40), X becomes 180, Y becomes 160
        val deltaW_tl = -10f * toPageX // -20
        val deltaH_tl = -20f * toPageY // -40
        val newW_tl = (initial.imageWidth - deltaW_tl).coerceIn(30f, baseW * 2f) // 420
        val newX_tl = initial.posX + (initial.imageWidth - newW_tl) // 180
        val newH_tl = (initial.imageHeight - deltaH_tl).coerceIn(30f, baseH * 2f) // 440
        val newY_tl = initial.posY + (initial.imageHeight - newH_tl) // 160
        assertEquals(180f, newX_tl, 0.01f)
        assertEquals(160f, newY_tl, 0.01f)
        assertEquals(420f, newW_tl, 0.01f)
        assertEquals(440f, newH_tl, 0.01f)

        // 2. TOP-RIGHT handle: drag by (+15 local px, -10 local px) -> (+30 page px, -20 page px)
        // Image should become wider (+30), taller (+20), Y becomes 180, X stays 200
        val deltaW_tr = 15f * toPageX // +30
        val deltaH_tr = -10f * toPageY // -20
        val newW_tr = (initial.imageWidth + deltaW_tr).coerceIn(30f, baseW * 2f) // 430
        val newH_tr = (initial.imageHeight - deltaH_tr).coerceIn(30f, baseH * 2f) // 420
        val newY_tr = initial.posY + (initial.imageHeight - newH_tr) // 180
        assertEquals(200f, initial.posX, 0.01f)
        assertEquals(180f, newY_tr, 0.01f)
        assertEquals(430f, newW_tr, 0.01f)
        assertEquals(420f, newH_tr, 0.01f)

        // 3. BOTTOM-LEFT handle: drag by (-10 local px, +25 local px) -> (-20 page px, +50 page px)
        // Image should become wider (+20), taller (+50), X becomes 180, Y stays 200
        val deltaW_bl = -10f * toPageX // -20
        val deltaH_bl = 25f * toPageY // +50
        val newW_bl = (initial.imageWidth - deltaW_bl).coerceIn(30f, baseW * 2f) // 420
        val newX_bl = initial.posX + (initial.imageWidth - newW_bl) // 180
        val newH_bl = (initial.imageHeight + deltaH_bl).coerceIn(30f, baseH * 2f) // 450
        assertEquals(180f, newX_bl, 0.01f)
        assertEquals(200f, initial.posY, 0.01f)
        assertEquals(420f, newW_bl, 0.01f)
        assertEquals(450f, newH_bl, 0.01f)

        // 4. BOTTOM-RIGHT handle: drag by (+20 local px, +30 local px) -> (+40 page px, +60 page px)
        // X stays 200, Y stays 200, width becomes 440, height becomes 460
        val deltaW_br = 20f * toPageX // +40
        val deltaH_br = 30f * toPageY // +60
        val newW_br = (initial.imageWidth + deltaW_br).coerceIn(30f, baseW * 2f) // 440
        val newH_br = (initial.imageHeight + deltaH_br).coerceIn(30f, baseH * 2f) // 460
        assertEquals(200f, initial.posX, 0.01f)
        assertEquals(200f, initial.posY, 0.01f)
        assertEquals(440f, newW_br, 0.01f)
        assertEquals(460f, newH_br, 0.01f)
    }

    @Test
    fun testPdfEngineImagePlacementCoordinateMapping() {
        val finalW = 595
        val finalH = 842

        val sampleBmp = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        val imageItem = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            imageBitmap = sampleBmp,
            originalImageBitmap = sampleBmp,
            posX = 100f,
            posY = 150f,
            imageWidth = 200f,
            imageHeight = 250f,
            baseWidth = 1190f,
            baseHeight = 1684f
        )

        val sx = if (imageItem.baseWidth > 0f) finalW.toFloat() / imageItem.baseWidth else 1f
        val sy = if (imageItem.baseHeight > 0f) finalH.toFloat() / imageItem.baseHeight else 1f

        val posX = imageItem.posX * sx
        val posY = imageItem.posY * sy
        val dstRect = Rect(
            posX.toInt(),
            posY.toInt(),
            (posX + imageItem.imageWidth * sx).toInt(),
            (posY + imageItem.imageHeight * sy).toInt()
        )

        assertEquals(50f, posX, 0.01f)
        assertEquals(75f, posY, 0.01f)
        assertEquals(50, dstRect.left)
        assertEquals(75, dstRect.top)
        assertEquals(150, dstRect.right)
        assertEquals(200, dstRect.bottom)
        assertEquals(100, dstRect.width())
        assertEquals(125, dstRect.height())
    }
}


