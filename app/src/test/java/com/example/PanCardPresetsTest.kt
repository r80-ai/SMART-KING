package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.pdf.PanDocumentPdfBuilder
import com.example.ui.screens.PanAssetType
import com.example.ui.screens.PanProvider
import com.example.ui.screens.ProteanRoute
import com.example.ui.screens.getPanSpec
import com.example.ui.screens.injectJfifDpi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PanCardPresetsTest {

    @Test
    fun testNsdlPhotoOnlinePreset() {
        val spec = getPanSpec(PanProvider.NSDL, PanAssetType.PHOTO, ProteanRoute.STANDARD)
        assertEquals(197, spec.widthPx)
        assertEquals(276, spec.heightPx)
        assertEquals(200, spec.dpi)
        assertEquals(50, spec.maxKb)
        assertEquals("JPEG", spec.format)
        assertEquals("2.5:3.5", spec.aspectDescription)
    }

    @Test
    fun testNsdlSignatureOnlinePreset() {
        val spec = getPanSpec(PanProvider.NSDL, PanAssetType.SIGNATURE, ProteanRoute.STANDARD)
        assertEquals(354, spec.widthPx)
        assertEquals(157, spec.heightPx)
        assertEquals(200, spec.dpi)
        assertEquals(50, spec.maxKb)
        assertEquals("JPEG", spec.format)
        assertEquals("4.5:2", spec.aspectDescription)
    }

    @Test
    fun testProteanDscAlternativePreset() {
        val photoSpec = getPanSpec(PanProvider.NSDL, PanAssetType.PHOTO, ProteanRoute.DSC_ALTERNATIVE)
        assertEquals(20, photoSpec.maxKb)
        assertEquals(197, photoSpec.widthPx)
        assertEquals(276, photoSpec.heightPx)
        assertEquals(200, photoSpec.dpi)

        val signSpec = getPanSpec(PanProvider.NSDL, PanAssetType.SIGNATURE, ProteanRoute.DSC_ALTERNATIVE)
        assertEquals(10, signSpec.maxKb)
        assertEquals(354, signSpec.widthPx)
        assertEquals(157, signSpec.heightPx)
        assertEquals(200, signSpec.dpi)
    }

    @Test
    fun testUtiitslPhotoOnlinePreset() {
        val spec = getPanSpec(PanProvider.UTIITSL, PanAssetType.PHOTO, ProteanRoute.STANDARD)
        assertEquals(213, spec.widthPx)
        assertEquals(213, spec.heightPx)
        assertEquals(300, spec.dpi)
        assertEquals(30, spec.maxKb)
        assertEquals("JPEG", spec.format)
        assertEquals("1:1", spec.aspectDescription)
    }

    @Test
    fun testUtiitslSignatureOnlinePreset() {
        val spec = getPanSpec(PanProvider.UTIITSL, PanAssetType.SIGNATURE, ProteanRoute.STANDARD)
        assertEquals(400, spec.widthPx)
        assertEquals(200, spec.heightPx)
        assertEquals(600, spec.dpi)
        assertEquals(60, spec.maxKb)
        assertEquals("JPEG", spec.format)
        assertEquals("2:1", spec.aspectDescription)
    }

    @Test
    fun testInjectJfifDpiHeader() {
        // Minimum mock JPEG SOI + EOI
        val mockJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val withDpi200 = injectJfifDpi(mockJpeg, 200)
        
        // Should have inserted 18-byte JFIF APP0 header
        assertTrue(withDpi200.size >= 22)
        assertEquals(0xFF.toByte(), withDpi200[0])
        assertEquals(0xD8.toByte(), withDpi200[1])
        assertEquals(0xFF.toByte(), withDpi200[2])
        assertEquals(0xE0.toByte(), withDpi200[3])
        assertEquals('J'.code.toByte(), withDpi200[6])
        assertEquals('F'.code.toByte(), withDpi200[7])
        assertEquals('I'.code.toByte(), withDpi200[8])
        assertEquals('F'.code.toByte(), withDpi200[9])
        assertEquals(1.toByte(), withDpi200[13]) // units = DPI
        
        val xDensity = ((withDpi200[14].toInt() and 0xFF) shl 8) or (withDpi200[15].toInt() and 0xFF)
        assertEquals(200, xDensity)

        // Test 600 DPI for UTI Signature
        val withDpi600 = injectJfifDpi(mockJpeg, 600)
        val xDensity600 = ((withDpi600[14].toInt() and 0xFF) shl 8) or (withDpi600[15].toInt() and 0xFF)
        assertEquals(600, xDensity600)
    }

    @Test
    fun testPanDocumentPdfSinglePageStrict300KbLimit() {
        // Create sample A4 page bitmap (800x1131 or 1654x2338)
        val bmp = Bitmap.createBitmap(800, 1131, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.LTGRAY)

        val pdfBytes = PanDocumentPdfBuilder.buildPanDocumentPdf(
            pages = listOf(bmp),
            maxAllowedBytes = 300 * 1024L
        )

        // Must be non-empty and strictly <= 300 KB
        assertTrue("PDF must not be empty", pdfBytes.isNotEmpty())
        assertTrue(
            "PDF size (${pdfBytes.size} bytes) must be <= 300 KB (307200 bytes)",
            pdfBytes.size <= 300 * 1024L
        )

        // Header must start with %PDF-1.4
        val header = String(pdfBytes.copyOfRange(0, 8), StandardCharsets.US_ASCII)
        assertTrue("Must have PDF header, got $header", header.startsWith("%PDF-"))

        // Trailer must end with %%EOF
        val trailer = String(pdfBytes.copyOfRange(pdfBytes.size - 20, pdfBytes.size), StandardCharsets.US_ASCII)
        assertTrue("Must end with %%EOF, got $trailer", trailer.contains("%%EOF"))
    }

    @Test
    fun testPanDocumentPdfMultiPageCombinedUnder300KbLimit() {
        // Create 3 document pages
        val page1 = Bitmap.createBitmap(600, 850, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val page2 = Bitmap.createBitmap(600, 850, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.LTGRAY) }
        val page3 = Bitmap.createBitmap(600, 850, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.DKGRAY) }

        val multiPagePdfBytes = PanDocumentPdfBuilder.buildPanDocumentPdf(
            pages = listOf(page1, page2, page3),
            maxAllowedBytes = 300 * 1024L
        )

        assertTrue("Multi-page PDF must not be empty", multiPagePdfBytes.isNotEmpty())
        assertTrue(
            "Multi-page PDF total size (${multiPagePdfBytes.size} bytes) must be <= 300 KB (307200 bytes)",
            multiPagePdfBytes.size <= 300 * 1024L
        )

        val pdfStr = String(multiPagePdfBytes, StandardCharsets.US_ASCII)
        assertTrue("Must contain /Count 3", pdfStr.contains("/Count 3"))
        assertTrue("Must contain %%EOF", pdfStr.contains("%%EOF"))
    }
}
