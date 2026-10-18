package com.example.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.test.core.app.ApplicationProvider
import com.example.model.PdfFileRecord
import com.example.storage.GlobalSaveManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfSavePipelineTest {

    private lateinit var context: Context
    private lateinit var sampleOriginalPdf: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GlobalSaveManager.initialize(context)

        // Create a valid 2-page sample PDF file
        val tempDir = File(context.cacheDir, "test_pdfs").apply { if (!exists()) mkdirs() }
        sampleOriginalPdf = File(tempDir, "ABDU2004.pdf")

        val doc = PdfDocument()
        for (p in 1..2) {
            val pageInfo = PdfDocument.PageInfo.Builder(612, 792, p).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
            }
            canvas.drawText("Page $p of ABDU2004 Original Document", 50f, 100f, paint)
            doc.finishPage(page)
        }
        FileOutputStream(sampleOriginalPdf).use { doc.writeTo(it) }
        doc.close()

        assertTrue("Sample PDF must exist", sampleOriginalPdf.exists())
        assertTrue("Sample PDF must have non-zero length", sampleOriginalPdf.length() > 0)
    }

    @Test
    fun testSaveEditedPdfWithoutEperm() = runBlocking {
        // SCENARIO 1 & 2: Save edited PDF, ensure no EPERM and file exists with non-zero size
        val originalLength = sampleOriginalPdf.length()
        val originalBytes = sampleOriginalPdf.readBytes()

        val textAnnotation = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.TEXT,
            text = "Official Approval Stamp",
            posX = 80f,
            posY = 200f,
            fontSize = 20f,
            colorHex = Color.RED,
            isBold = true
        )

        val outputName = "ABDU2004_edited.pdf"
        val savedFile = PdfEngine.saveEditedPdf(
            context = context,
            sourceFile = sampleOriginalPdf,
            items = listOf(textAnnotation),
            deletedPages = emptySet(),
            pageRotations = emptyMap(),
            outputFileName = outputName
        )

        // 1. Confirm file exists at destination
        assertNotNull(savedFile)
        assertTrue("Saved file must exist on disk without EPERM", savedFile.exists())

        // 2. Confirm non-zero size
        assertTrue("Saved file must have non-zero size", savedFile.length() > 0)

        // 3. Confirm path does not crash with raw /storage/emulated/0
        assertFalse("Path should not be directly under unpermitted root", savedFile.absolutePath == "/storage/emulated/0/Smart King/$outputName")
        assertTrue("Saved file should be in app writable directory", savedFile.canWrite() && savedFile.canRead())

        // 4. Confirm original PDF is preserved and unchanged
        assertEquals("Original PDF length must not change", originalLength, sampleOriginalPdf.length())
        assertTrue("Original PDF content must remain identical", originalBytes.contentEquals(sampleOriginalPdf.readBytes()))
    }

    @Test
    fun testReopenSavedPdfAndVerifyPages() = runBlocking {
        // SCENARIO 3: Reopen the saved PDF and verify it is a valid PDF
        val drawItem = PdfEngine.EditItem(
            pageIndex = 1,
            type = PdfEngine.AnnotationType.DRAW,
            points = listOf(Pair(100f, 100f), Pair(200f, 200f)),
            colorHex = Color.BLUE,
            strokeWidth = 4f
        )

        val savedFile = PdfEngine.saveEditedPdf(
            context = context,
            sourceFile = sampleOriginalPdf,
            items = listOf(drawItem),
            deletedPages = emptySet(),
            pageRotations = emptyMap(),
            outputFileName = "ABDU2004_draw_edited.pdf"
        )

        assertTrue(savedFile.exists())
        // Verify page count using PdfEngine
        val pageCount = PdfEngine.getPageCount(savedFile)
        assertEquals(2, pageCount)
    }

    @Test
    fun testUserSelectedDestinationViaSaf() = runBlocking {
        // SCENARIO 5: Test saving to user-selected destination
        val safDestinationFile = File(context.cacheDir, "user_selected_ABDU2004.pdf")
        if (safDestinationFile.exists()) safDestinationFile.delete()

        val item = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.STAMP,
            stampText = "VERIFIED",
            posX = 150f,
            posY = 300f
        )

        // Write edited PDF directly to destination file (simulating SAF target URI stream)
        val resultFile = PdfEngine.writeEditedPdf(
            context = context,
            sourceFile = sampleOriginalPdf,
            items = listOf(item),
            deletedPages = emptySet(),
            pageRotations = emptyMap(),
            outputFile = safDestinationFile
        )

        assertTrue("SAF destination file must exist", resultFile.exists())
        assertTrue("SAF destination file must have content", resultFile.length() > 0)
        assertEquals(safDestinationFile.absolutePath, resultFile.absolutePath)
    }

    @Test
    fun testDuplicateFilenamesPreservedSafely() = runBlocking {
        // SCENARIO 7: Duplicate filenames must not overwrite existing file
        val targetDir = GlobalSaveManager.getLocalActiveDirectory(context)

        val file1 = PdfEngine.generateUniquePdfFile(targetDir, "ABDU2004_edited.pdf")
        file1.writeText("first content")

        val file2 = PdfEngine.generateUniquePdfFile(targetDir, "ABDU2004_edited.pdf")
        assertNotEquals("Second duplicate file must have unique path", file1.name, file2.name)
        assertEquals("ABDU2004_edited_(1).pdf", file2.name)

        file2.writeText("second content")
        val file3 = PdfEngine.generateUniquePdfFile(targetDir, "ABDU2004_edited.pdf")
        assertEquals("ABDU2004_edited_(2).pdf", file3.name)

        // Original duplicate file remained untouched
        assertEquals("first content", file1.readText())
        assertEquals("second content", file2.readText())
    }

    @Test
    fun testSpacesAndUnicodePreservedInFilename() {
        // SCENARIO 7: Filenames with spaces and Unicode (Bengali, Arabic, etc.)
        val nameWithSpaces = "ABDU 2004 edited.pdf"
        val cleanSpaces = PdfEngine.sanitizeFileName(nameWithSpaces)
        assertEquals("ABDU 2004 edited.pdf", cleanSpaces)

        val nameWithUnicode = "বাংলা নথি 2026.pdf"
        val cleanUnicode = PdfEngine.sanitizeFileName(nameWithUnicode)
        assertEquals("বাংলা নথি 2026.pdf", cleanUnicode)

        val uniqueWithUnicode = GlobalSaveManager.generateUniqueName("বাংলা নথি", "pdf") { false }
        assertEquals("বাংলা নথি.pdf", uniqueWithUnicode)
    }

    @Test
    fun testLocalActiveDirectoryNeverReturnsForbiddenRoot() {
        val activeDir = GlobalSaveManager.getLocalActiveDirectory(context)
        assertNotNull(activeDir)
        assertTrue("Local active directory must exist", activeDir.exists())
        assertTrue("Local active directory must be writable without EPERM", activeDir.canWrite())
        assertFalse("Directory cannot be raw unpermitted root", activeDir.absolutePath == "/storage/emulated/0/Smart King")
    }

    @Test
    fun testResolveActualFileNameFromRecord() {
        val record = PdfFileRecord(
            name = "1792324996557_ABDU2004.pdf",
            filePath = "/data/user/0/com.aistudio.pdfmaster/files/imported_pdfs/1792324996557_ABDU2004.pdf",
            fileSize = 51200L,
            pageCount = 2,
            category = "imported"
        )
        val resolvedName = record.resolveActualFileName(context)
        assertEquals("ABDU2004.pdf", resolvedName)
        assertEquals("ABDU2004", resolvedName.removeSuffix(".pdf"))
    }
}
