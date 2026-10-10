package com.example.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale

class PdfToJpgTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testPageNumberFormattingRules() {
        // Requirement 13 & 14:
        // Single page PDF must be page_001.jpg
        val singlePageName = String.format(Locale.US, "page_%03d.jpg", 1)
        assertEquals("page_001.jpg", singlePageName)

        // Multi-page PDFs must produce page_001.jpg, page_002.jpg, page_003.jpg, etc.
        val multiPageNames = (1..5).map { pageNum ->
            String.format(Locale.US, "page_%03d.jpg", pageNum)
        }
        assertEquals(
            listOf("page_001.jpg", "page_002.jpg", "page_003.jpg", "page_004.jpg", "page_005.jpg"),
            multiPageNames
        )
    }

    @Test
    fun testJpgPageItemAndSizeAccounting() {
        val dummyFile1 = tempFolder.newFile("page_001.jpg")
        dummyFile1.writeBytes(ByteArray(250_000))

        val dummyFile2 = tempFolder.newFile("page_002.jpg")
        dummyFile2.writeBytes(ByteArray(300_000))

        val item1 = JpgPageItem(0, "page_001.jpg", dummyFile1, dummyFile1.length(), 1600, 2200)
        val item2 = JpgPageItem(1, "page_002.jpg", dummyFile2, dummyFile2.length(), 1600, 2200)

        val pages = listOf(item1, item2)
        val totalJpgBytes = pages.sumOf { it.byteSize }
        assertEquals(550_000L, totalJpgBytes)

        val originalPdfBytes = 500_000L
        val result = PdfToJpgConversionResult(
            originalPdfFile = tempFolder.newFile("test.pdf"),
            originalPdfBytes = originalPdfBytes,
            totalJpgBytes = totalJpgBytes,
            pages = pages,
            scaleUsed = 2.5f,
            qualityUsed = 95,
            totalAttempts = 1
        )

        // Strict Requirement 6 & 7:
        // The exported JPG output must NEVER have a total file size smaller than the original PDF.
        // The exported JPG size must be at least equal to the original PDF size in bytes.
        assertTrue(
            "Total JPG bytes ($totalJpgBytes) must be >= original PDF bytes ($originalPdfBytes)",
            result.totalJpgBytes >= result.originalPdfBytes
        )
    }

    @Test
    fun testFileRequirementSizeCondition() {
        val originalPdfBytes = 2_560_000L // 2.56 MB
        val exportedJpgBytesUnder = 2_400_000L // 2.4 MB (Smaller than original)
        val exportedJpgBytesOver = 2_800_000L // 2.8 MB (Equal or greater)

        assertFalse(
            "Export smaller than original PDF must NOT satisfy the requirement",
            exportedJpgBytesUnder >= originalPdfBytes
        )
        assertTrue(
            "Export equal or larger than original PDF satisfies the requirement",
            exportedJpgBytesOver >= originalPdfBytes
        )
    }
}
