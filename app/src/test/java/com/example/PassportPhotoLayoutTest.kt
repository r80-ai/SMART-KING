package com.example

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import com.example.ui.screens.PaperLayoutMode
import com.example.ui.screens.PaperSpec
import com.example.ui.screens.PassportPrintLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PassportPhotoLayoutTest {

    private val testDpi = 300
    private val paper4x6 = PaperSpec("4 × 6 inch", 101.6f, 152.4f)
    private val paperA4 = PaperSpec("A4", 210f, 297f)

    private fun createTestPhoto(width: Int = 413, height: Int = 531, color: Int = Color.RED): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(color)
        return bmp
    }

    // ==================================================
    // 4 × 6 INCH + PORTRAIT (3 photos / row, 4 rows max = 12 per page)
    // Page dimensions remain strictly fixed at 4 x 6 inch (1200 x 1800 px)
    // ==================================================

    @Test
    fun test4x6PortraitThreePhotos() {
        val photo = createTestPhoto(413, 531, Color.RED)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 3, PaperLayoutMode.PORTRAIT)

        assertEquals(1, pages.size)
        val page = pages[0]
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paper4x6, PaperLayoutMode.PORTRAIT, testDpi)

        // Real paper dimensions must NEVER stretch
        assertEquals(expectedW, page.width)
        assertEquals(expectedH, page.height)

        // Row 0 has photos
        val margin = maxOf(4, (3.5f / 25.4f * testDpi).toInt())
        assertEquals(Color.RED, page.getPixel(page.width / 2, margin + 50))

        page.recycle()
        photo.recycle()
    }

    @Test
    fun test4x6PortraitSixPhotos() {
        val photo = createTestPhoto(413, 531, Color.RED)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 6, PaperLayoutMode.PORTRAIT)

        assertEquals(1, pages.size)
        val page = pages[0]
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paper4x6, PaperLayoutMode.PORTRAIT, testDpi)

        assertEquals(expectedW, page.width)
        assertEquals(expectedH, page.height)

        page.recycle()
        photo.recycle()
    }

    @Test
    fun test4x6PortraitNinePhotos() {
        val photo = createTestPhoto(413, 531, Color.RED)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 9, PaperLayoutMode.PORTRAIT)

        assertEquals(1, pages.size)
        val page = pages[0]
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paper4x6, PaperLayoutMode.PORTRAIT, testDpi)

        assertEquals(expectedW, page.width)
        assertEquals(expectedH, page.height)

        page.recycle()
        photo.recycle()
    }

    @Test
    fun test4x6PortraitTwelvePhotos() {
        val photo = createTestPhoto(413, 531, Color.RED)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 12, PaperLayoutMode.PORTRAIT)

        assertEquals(1, pages.size)
        val page = pages[0]
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paper4x6, PaperLayoutMode.PORTRAIT, testDpi)

        assertEquals(expectedW, page.width)
        assertEquals(expectedH, page.height)

        page.recycle()
        photo.recycle()
    }

    @Test
    fun test4x6PortraitOverflowCreatesNewPageNotStretched() {
        val photo = createTestPhoto(413, 531, Color.RED)
        // 15 photos on 4x6 Portrait: 12 photos fit on page 1, 3 photos on page 2
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 15, PaperLayoutMode.PORTRAIT)

        assertEquals(2, pages.size)
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paper4x6, PaperLayoutMode.PORTRAIT, testDpi)

        for (p in pages) {
            assertEquals(expectedW, p.width)
            assertEquals(expectedH, p.height)
            p.recycle()
        }
        photo.recycle()
    }

    // ==================================================
    // 4 × 6 INCH + LANDSCAPE (5 photos / row, 3 rows max = 15 per page)
    // Page dimensions remain strictly fixed at 6 x 4 inch (1800 x 1200 px)
    // ==================================================

    @Test
    fun test4x6LandscapeFivePhotos() {
        val photo = createTestPhoto(413, 531, Color.BLUE)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 5, PaperLayoutMode.LANDSCAPE)

        assertEquals(1, pages.size)
        val page = pages[0]
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paper4x6, PaperLayoutMode.LANDSCAPE, testDpi)

        assertEquals(expectedW, page.width)
        assertEquals(expectedH, page.height)

        val margin = maxOf(4, (3.5f / 25.4f * testDpi).toInt())
        assertEquals(Color.BLUE, page.getPixel(page.width / 2, margin + 50))

        page.recycle()
        photo.recycle()
    }

    @Test
    fun test4x6LandscapeTenPhotos() {
        val photo = createTestPhoto(413, 531, Color.BLUE)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 10, PaperLayoutMode.LANDSCAPE)

        assertEquals(1, pages.size)
        val page = pages[0]
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paper4x6, PaperLayoutMode.LANDSCAPE, testDpi)

        assertEquals(expectedW, page.width)
        assertEquals(expectedH, page.height)

        page.recycle()
        photo.recycle()
    }

    @Test
    fun test4x6LandscapeFifteenPhotos() {
        val photo = createTestPhoto(413, 531, Color.BLUE)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 15, PaperLayoutMode.LANDSCAPE)

        assertEquals(1, pages.size)
        val page = pages[0]
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paper4x6, PaperLayoutMode.LANDSCAPE, testDpi)

        assertEquals(expectedW, page.width)
        assertEquals(expectedH, page.height)

        page.recycle()
        photo.recycle()
    }

    @Test
    fun test4x6LandscapeOverflowCreatesNewPage() {
        val photo = createTestPhoto(413, 531, Color.BLUE)
        // 20 photos on 4x6 Landscape: 15 on page 1, 5 on page 2
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 20, PaperLayoutMode.LANDSCAPE)

        assertEquals(2, pages.size)
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paper4x6, PaperLayoutMode.LANDSCAPE, testDpi)

        for (p in pages) {
            assertEquals(expectedW, p.width)
            assertEquals(expectedH, p.height)
            p.recycle()
        }
        photo.recycle()
    }

    // ==================================================
    // A4 PAPER LAYOUT (6 photos in ONE ROW across the top)
    // Page dimensions remain strictly fixed at real A4 (210 x 297 mm, 2480 x 3507 px at 300 dpi)
    // Photos placed across upper portion, remaining lower portion is completely WHITE/EMPTY
    // ==================================================

    @Test
    fun testA4SixPhotosInOneRow() {
        val photo = createTestPhoto(413, 531, Color.MAGENTA)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paperA4, true, testDpi, 6)

        assertEquals(1, pages.size)
        val page = pages[0]
        val (expectedW, expectedH) = PassportPrintLayoutEngine.getPaperDimensionsPx(paperA4, PaperLayoutMode.PORTRAIT, testDpi)

        // Real A4 dimensions must NEVER stretch
        assertEquals(expectedW, page.width)
        assertEquals(expectedH, page.height)
        assertEquals(2480, page.width)
        assertEquals(3507, page.height)

        // Upper portion (row 0) contains the photos across the page with small white gaps between them
        val testY = 100 // inside row 0 (height is 531)
        val gapX = maxOf(2, (2f / 25.4f * testDpi).toInt())
        val photoW = 413

        // Verify each of the 6 photos has its content present
        for (i in 0 until 6) {
            val photoCenterX = i * (photoW + gapX) + photoW / 2
            if (photoCenterX < page.width) {
                assertEquals("Photo $i should be present in row 0", Color.MAGENTA, page.getPixel(photoCenterX, testY))
            }
        }

        // Verify small white gap between adjacent photos
        val gap1X = photoW + gapX / 2
        assertEquals("White gap between photo 0 and photo 1", Color.WHITE, page.getPixel(gap1X, testY))

        val gap2X = 2 * photoW + gapX + gapX / 2
        assertEquals("White gap between photo 1 and photo 2", Color.WHITE, page.getPixel(gap2X, testY))

        // Remaining lower portion must be completely WHITE / EMPTY
        assertEquals(Color.WHITE, page.getPixel(page.width / 2, 1000))
        assertEquals(Color.WHITE, page.getPixel(page.width / 2, 2000))
        assertEquals(Color.WHITE, page.getPixel(page.width / 2, 3000))

        page.recycle()
        photo.recycle()
    }

    @Test
    fun testA4TwelvePhotosTwoRows() {
        val photo = createTestPhoto(413, 531, Color.MAGENTA)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paperA4, true, testDpi, 12)

        assertEquals(1, pages.size)
        val page = pages[0]

        // Page dimensions fixed
        assertEquals(2480, page.width)
        assertEquals(3507, page.height)

        val testX = 200 // inside photo 0 (spans x = 1 to 414)
        // Row 0 has photos
        assertEquals(Color.MAGENTA, page.getPixel(testX, 100))
        // Row 1 has photos (around y = 650)
        assertEquals(Color.MAGENTA, page.getPixel(testX, 650))

        // Lower portion remains completely white/empty
        assertEquals(Color.WHITE, page.getPixel(testX, 1800))
        assertEquals(Color.WHITE, page.getPixel(testX, 3000))

        page.recycle()
        photo.recycle()
    }

    @Test
    fun testA4RowCapacityIsSix() {
        assertEquals(6, PassportPrintLayoutEngine.getPaperRowCapacity(paperA4))
        assertEquals(6, PassportPrintLayoutEngine.getPaperRowCapacity("A4"))
    }

    @Test
    fun testA4OverflowCreatesNewSheetNotStretched() {
        val photo = createTestPhoto(413, 531, Color.MAGENTA)
        // 40 photos: 36 fit on page 1 (6 rows x 6 cols), 4 photos on page 2
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paperA4, true, testDpi, 40)

        assertEquals(2, pages.size)
        for (p in pages) {
            assertEquals(2480, p.width)
            assertEquals(3507, p.height)
            p.recycle()
        }
        photo.recycle()
    }

    // ==================================================
    // 4x6 INCH ROW CAPACITY LOGIC (UNTOUCHED)
    // Portrait: +3 photos per line (3 -> 6 -> 9 -> 12)
    // Landscape: +5 photos per line (5 -> 10 -> 15 -> 20)
    // ==================================================

    @Test
    fun testAddLinePhotoRowCapacity() {
        assertEquals(3, PassportPrintLayoutEngine.getPaperRowCapacity(PaperLayoutMode.PORTRAIT))
        assertEquals(5, PassportPrintLayoutEngine.getPaperRowCapacity(PaperLayoutMode.LANDSCAPE))
        assertEquals(3, PassportPrintLayoutEngine.getPaperRowCapacity(paper4x6, PaperLayoutMode.PORTRAIT))
        assertEquals(5, PassportPrintLayoutEngine.getPaperRowCapacity(paper4x6, PaperLayoutMode.LANDSCAPE))

        // Portrait step progression
        var portraitCopies = 3
        portraitCopies += PaperLayoutMode.PORTRAIT.photosPerRow
        assertEquals(6, portraitCopies)
        portraitCopies += PaperLayoutMode.PORTRAIT.photosPerRow
        assertEquals(9, portraitCopies)
        portraitCopies += PaperLayoutMode.PORTRAIT.photosPerRow
        assertEquals(12, portraitCopies)

        // Landscape step progression
        var landscapeCopies = 5
        landscapeCopies += PaperLayoutMode.LANDSCAPE.photosPerRow
        assertEquals(10, landscapeCopies)
        landscapeCopies += PaperLayoutMode.LANDSCAPE.photosPerRow
        assertEquals(15, landscapeCopies)
        landscapeCopies += PaperLayoutMode.LANDSCAPE.photosPerRow
        assertEquals(20, landscapeCopies)
    }

    // ==================================================
    // OUTPUT CONSISTENCY (PREVIEW, JPG, PDF)
    // ==================================================

    @Test
    fun testPdfDocumentUsesExactPageBitmapDimensions() {
        val photo = createTestPhoto(413, 531, Color.GREEN)
        val pages = PassportPrintLayoutEngine.generatePrintLayouts(photo, paper4x6, true, testDpi, 12, PaperLayoutMode.PORTRAIT)
        val sheetBmp = pages.first()

        val pageInfo = PdfDocument.PageInfo.Builder(sheetBmp.width, sheetBmp.height, 1).create()
        assertEquals(sheetBmp.width, pageInfo.pageWidth)
        assertEquals(sheetBmp.height, pageInfo.pageHeight)

        pages.forEach { it.recycle() }
        photo.recycle()
    }
}
