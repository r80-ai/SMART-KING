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
    fun testGlobalSaveManagerLocalActiveDirectoryIsAlwaysPermitted() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = com.example.storage.GlobalSaveManager.getLocalActiveDirectory(context)
        assertNotNull(dir)
        assertTrue("Directory must exist or be creatable", dir.exists() || dir.mkdirs())
        assertTrue("Directory must be writable without EPERM", dir.canWrite())

        // Verify write and delete
        val testFile = java.io.File(dir, "write_test_${System.currentTimeMillis()}.pdf")
        testFile.writeText("test_content")
        assertTrue(testFile.exists())
        assertEquals("test_content", testFile.readText())
        testFile.delete()
    }

    @Test
    fun testFilenameDeduplicationAndUnicodeHandling() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = com.example.storage.GlobalSaveManager.getLocalActiveDirectory(context)

        // Test Unicode Bengali filename with spaces
        val unicodeName = "আমার ফাইল ২০২৬.pdf"
        val sanitized = PdfEngine.sanitizeFileName(unicodeName)
        assertTrue("Should preserve spaces and Bengali characters", sanitized.contains("আমার ফাইল ২০২৬"))
        assertTrue("Should end with .pdf", sanitized.endsWith(".pdf"))

        val file1 = PdfEngine.generateUniquePdfFile(dir, "ABDU2004_edited.pdf")
        file1.writeText("first")
        try {
            val file2 = PdfEngine.generateUniquePdfFile(dir, "ABDU2004_edited.pdf")
            assertEquals("ABDU2004_edited_(1).pdf", file2.name)
        } finally {
            file1.delete()
        }
    }

    @Test
    fun testSaveEditedPdfCreatesNonEmptyValidPdfAndLeavesOriginalUntouched() {
        kotlinx.coroutines.runBlocking {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            val originalFile = java.io.File(context.cacheDir, "original_test_doc.pdf")
            PdfEngine.generateMinimalValidPdf("Original Doc", "Do Not Overwrite", originalFile)
            assertTrue(originalFile.exists())
            val origLength = originalFile.length()
            assertTrue("Original must be non-empty", origLength > 0)
            val origBytes = originalFile.readBytes()

            // Apply edits (e.g. text annotation)
            val editItem = PdfEngine.EditItem(
                pageIndex = 0,
                type = PdfEngine.AnnotationType.TEXT,
                text = "NEW EDITED ANNOTATION",
                posX = 50f,
                posY = 50f,
                fontSize = 20f,
                colorHex = Color.BLUE
            )

            val editedFile = PdfEngine.saveEditedPdf(
                context = context,
                sourceFile = originalFile,
                items = listOf(editItem),
                deletedPages = emptySet(),
                pageRotations = emptyMap(),
                outputFileName = "ABDU2004_edited.pdf"
            )

            // 1. Confirm file exists at destination
            assertNotNull(editedFile)
            assertTrue("Edited file must exist", editedFile.exists())
            assertTrue("Edited file must have non-zero size", editedFile.length() > 0)

            // 2. Confirm original PDF remains unchanged
            assertEquals("Original file size must remain untouched", origLength, originalFile.length())
            assertTrue("Original content must be identical", java.util.Arrays.equals(origBytes, originalFile.readBytes()))

            // Cleanup
            originalFile.delete()
            editedFile.delete()
        }
    }
}

