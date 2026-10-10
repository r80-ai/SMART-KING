package com.example.pdf

import android.graphics.Bitmap
import com.example.ui.screens.injectJfifDpi
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class PageJpegData(
    val jpegBytes: ByteArray,
    val pixelWidth: Int,
    val pixelHeight: Int
)

/**
 * High-performance, zero-dependency ISO-standard PDF generator with native DCTDecode (JPEG).
 * Strictly guarantees that PAN Card Document PDFs never exceed 300 KB (307,200 bytes),
 * while optimizing for maximum visual clarity within the 250–299 KB sweet spot.
 */
object PanDocumentPdfBuilder {

    const val MAX_ALLOWED_BYTES = 300 * 1024L // Strict 300 KB ceiling (307,200 bytes)
    const val TARGET_SWEET_SPOT_MIN = 250 * 1024L // Target 250–299 KB range

    /**
     * Builds a compliant PDF containing 1 or more pages.
     * Guaranteed to produce actual final byte size <= maxAllowedBytes (300 KB).
     */
    fun buildPanDocumentPdf(
        pages: List<Bitmap>,
        maxAllowedBytes: Long = MAX_ALLOWED_BYTES
    ): ByteArray {
        require(pages.isNotEmpty()) { "At least one page required for PAN Document PDF" }

        var bestPdf: ByteArray? = null
        var bestSize = 0

        // Scale factors: evaluate 100% resolution down to 35%
        // A single 1654x2338 page at 200 DPI fits at ~70% JPEG quality around 260-295 KB.
        // Multi-page documents automatically scale resolution and quality to ensure the entire PDF <= 300 KB.
        val scaleFactors = floatArrayOf(1.0f, 0.88f, 0.75f, 0.65f, 0.50f, 0.38f)

        for (scale in scaleFactors) {
            val scaledBitmaps = pages.map { bmp ->
                if (scale >= 0.99f) {
                    bmp
                } else {
                    val targetW = max(240, (bmp.width * scale).roundToInt())
                    val targetH = max(340, (bmp.height * scale).roundToInt())
                    Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
                }
            }

            // Binary search JPEG quality between 10 and 95
            var lowQ = 10
            var highQ = 95
            var bestQForScale: ByteArray? = null

            while (lowQ <= highQ) {
                val midQ = (lowQ + highQ) / 2
                val candidatePdf = assemblePdfFromBitmaps(scaledBitmaps, midQ)
                val actualSize = candidatePdf.size

                if (actualSize <= maxAllowedBytes) {
                    bestQForScale = candidatePdf
                    if (actualSize > bestSize) {
                        bestPdf = candidatePdf
                        bestSize = actualSize
                    }
                    // Try higher quality to get closer to 250–299 KB
                    lowQ = midQ + 1
                } else {
                    // Exceeded 300 KB, try lower quality
                    highQ = midQ - 1
                }
            }

            // Clean up temporary bitmaps
            if (scale < 0.99f) {
                scaledBitmaps.forEach { if (it != pages[0]) it.recycle() }
            }

            // If we found a valid candidate in the sweet spot (250 KB - 300 KB) with scale >= 0.75, stop early
            if (bestPdf != null && bestSize >= TARGET_SWEET_SPOT_MIN) {
                break
            }
        }

        // Safety fallback: ensure bestPdf is strictly under 300 KB
        var finalResult = bestPdf
        if (finalResult == null || finalResult.size > maxAllowedBytes) {
            finalResult = emergencyCompress(pages, maxAllowedBytes)
        }

        // Absolute final safety assertion:
        check(finalResult.size <= maxAllowedBytes) {
            "Internal error: Final PDF size (${finalResult.size} bytes) exceeds 300 KB limit!"
        }

        return finalResult
    }

    private fun assemblePdfFromBitmaps(bitmaps: List<Bitmap>, quality: Int): ByteArray {
        val pageDataList = bitmaps.map { bmp ->
            val bos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            val jfifBytes = injectJfifDpi(bos.toByteArray(), 200)
            PageJpegData(
                jpegBytes = jfifBytes,
                pixelWidth = bmp.width,
                pixelHeight = bmp.height
            )
        }
        return createPdfWithJpegPages(pageDataList)
    }

    private fun emergencyCompress(pages: List<Bitmap>, maxAllowedBytes: Long): ByteArray {
        // Ultra-compact scale
        val scaled = pages.map { bmp ->
            val targetW = max(200, (bmp.width * 0.35f).roundToInt())
            val targetH = max(300, (bmp.height * 0.35f).roundToInt())
            Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
        }
        val result = assemblePdfFromBitmaps(scaled, 20)
        scaled.forEach { it.recycle() }
        return result
    }

    /**
     * Creates an ISO 32000-1 compliant PDF document embedding JPEG images directly
     * with DCTDecode filter. This produces a compact, high-performance PDF with minimal
     * wrapper overhead (~1 KB per page).
     */
    fun createPdfWithJpegPages(pages: List<PageJpegData>): ByteArray {
        val out = ByteArrayOutputStream()
        val offsets = mutableMapOf<Int, Long>()
        var currentOffset = 0L

        fun writeAscii(str: String) {
            val bytes = str.toByteArray(StandardCharsets.US_ASCII)
            out.write(bytes)
            currentOffset += bytes.size
        }

        fun writeBytes(bytes: ByteArray) {
            out.write(bytes)
            currentOffset += bytes.size
        }

        // 1. PDF Header (PDF-1.4 with binary comment to flag binary content)
        writeAscii("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")

        val pageCount = pages.size
        val catalogObj = 1
        val pagesObj = 2
        val totalObjects = 2 + (pageCount * 3)

        // 2. Catalog Object (1 0 obj)
        offsets[catalogObj] = currentOffset
        writeAscii("1 0 obj\n<<\n  /Type /Catalog\n  /Pages 2 0 R\n>>\nendobj\n")

        // 3. Pages Object (2 0 obj)
        offsets[pagesObj] = currentOffset
        val kidsStr = (0 until pageCount).joinToString(" ") { i -> "${3 + (i * 3)} 0 R" }
        writeAscii("2 0 obj\n<<\n  /Type /Pages\n  /Kids [$kidsStr]\n  /Count $pageCount\n>>\nendobj\n")

        // 4. Page, Content, and Image Objects for each page
        for (i in 0 until pageCount) {
            val pageData = pages[i]
            val pageObj = 3 + (i * 3)
            val contentObj = pageObj + 1
            val imageObj = pageObj + 2

            // Standard A4 dimensions in points: 595.28 x 841.89 pt
            val pageW = 595.28f
            val pageH = 841.89f

            // Page Object
            offsets[pageObj] = currentOffset
            writeAscii(
                "$pageObj 0 obj\n<<\n" +
                "  /Type /Page\n" +
                "  /Parent 2 0 R\n" +
                "  /MediaBox [0 0 ${String.format(Locale.US, "%.2f %.2f", pageW, pageH)}]\n" +
                "  /Contents $contentObj 0 R\n" +
                "  /Resources <<\n" +
                "    /ProcSet [/PDF /ImageC]\n" +
                "    /XObject <<\n" +
                "      /Im${i + 1} $imageObj 0 R\n" +
                "    >>\n" +
                "  >>\n>>\nendobj\n"
            )

            // Content Stream Object
            val contentStreamStr = "q\n${String.format(Locale.US, "%.2f 0 0 %.2f 0 0 cm", pageW, pageH)}\n/Im${i + 1} Do\nQ\n"
            val contentStreamBytes = contentStreamStr.toByteArray(StandardCharsets.US_ASCII)

            offsets[contentObj] = currentOffset
            writeAscii(
                "$contentObj 0 obj\n<<\n" +
                "  /Length ${contentStreamBytes.size}\n>>\nstream\n"
            )
            writeBytes(contentStreamBytes)
            writeAscii("endstream\nendobj\n")

            // Image XObject with DCTDecode (JPEG)
            offsets[imageObj] = currentOffset
            writeAscii(
                "$imageObj 0 obj\n<<\n" +
                "  /Type /XObject\n" +
                "  /Subtype /Image\n" +
                "  /Width ${pageData.pixelWidth}\n" +
                "  /Height ${pageData.pixelHeight}\n" +
                "  /ColorSpace /DeviceRGB\n" +
                "  /BitsPerComponent 8\n" +
                "  /Filter /DCTDecode\n" +
                "  /Length ${pageData.jpegBytes.size}\n>>\nstream\n"
            )
            writeBytes(pageData.jpegBytes)
            writeAscii("\nendstream\nendobj\n")
        }

        // 5. Cross-Reference Table (xref)
        val xrefOffset = currentOffset
        writeAscii("xref\n0 ${totalObjects + 1}\n")
        writeAscii("0000000000 65535 f \n")
        for (obj in 1..totalObjects) {
            val offset = offsets[obj] ?: 0L
            writeAscii(String.format(Locale.US, "%010d 00000 n \n", offset))
        }

        // 6. Trailer and EOF
        writeAscii(
            "trailer\n<<\n" +
            "  /Size ${totalObjects + 1}\n" +
            "  /Root 1 0 R\n>>\n" +
            "startxref\n$xrefOffset\n%%EOF\n"
        )

        return out.toByteArray()
    }
}
