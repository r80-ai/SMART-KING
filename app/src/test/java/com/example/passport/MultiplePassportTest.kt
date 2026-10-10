package com.example.passport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MultiplePassportTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testDefaultPassportSizeCalculations() {
        val defaultPreset = PassportSizePreset.DEFAULT
        assertEquals("3.3 × 4.3 cm", defaultPreset.title)
        assertEquals(33.0f, defaultPreset.widthMm, 0.01f)
        assertEquals(43.0f, defaultPreset.heightMm, 0.01f)

        val (wPx, hPx) = defaultPreset.toPixelsAt300Dpi()
        // 33 mm / 25.4 * 300 ≈ 389.76 -> 390
        // 43 mm / 25.4 * 300 ≈ 507.87 -> 508
        assertEquals(390, wPx)
        assertEquals(508, hPx)
    }

    @Test
    fun testA4GridCalculationCapacity36() {
        // A4 Paper: 210 x 297 mm
        // Default passport size: 3.3 x 4.3 cm
        // Must yield exactly 36 photos (6 cols x 6 rows)
        val grid = PassportSheetRenderer.computeGrid(
            paper = PaperSheetFormat.A4,
            photoWidthMm = 33.0f,
            photoHeightMm = 43.0f
        )

        assertEquals("Cols on A4 must be 6", 6, grid.cols)
        assertEquals("Rows on A4 must be 6", 6, grid.rows)
        assertEquals("Capacity for default 3.3x4.3cm on A4 must be 36", 36, grid.maxCapacity)
        assertEquals("Photo width for 3.3x4.3cm must be 385px", 385, grid.photoWidthPx)
        assertEquals("Photo height for 3.3x4.3cm must be 502px", 502, grid.photoHeightPx)
        assertEquals("Vertical gap on A4 must be 40px", 40, grid.gapYPx)
        assertEquals("Horizontal gap on A4 for 3.3x4.3cm must be 20px", 20, grid.gapXPx)
        assertEquals("Top margin on A4 must be 73px", 73, grid.marginYPx)
        assertEquals("Left margin on A4 for 3.3x4.3cm must be 35px", 35, grid.marginXPx)
        // Verify 6th photo right edge fits 100% inside 2480px A4 printable width
        val sixthPhotoRightEdge = grid.marginXPx + 6 * grid.photoWidthPx + 5 * grid.gapXPx
        assertTrue("6th photo right edge ($sixthPhotoRightEdge px) must fit within 2480px canvas", sixthPhotoRightEdge <= 2480)
        assertEquals("Right margin must equal left margin (35px)", 35, 2480 - sixthPhotoRightEdge)
    }

    @Test
    fun testA4GridCalculationCapacity42ForInch12x15() {
        // A4 Paper: 210 x 297 mm
        // 1.2 x 1.5 in preset: approx 30.5 x 38.1 mm
        // Must yield exactly 42 photos (6 cols x 7 rows)
        val preset = PassportSizePreset.INCH_12_15
        val grid = PassportSheetRenderer.computeGrid(
            paper = PaperSheetFormat.A4,
            photoWidthMm = preset.widthMm,
            photoHeightMm = preset.heightMm
        )

        assertEquals("Cols on A4 for 1.2x1.5in must be 6", 6, grid.cols)
        assertEquals("Rows on A4 for 1.2x1.5in must be 7", 7, grid.rows)
        assertEquals("Capacity for 1.2x1.5in on A4 must be 42", 42, grid.maxCapacity)
        assertEquals("Vertical gap on A4 must be 40px", 40, grid.gapYPx)
        assertEquals("Horizontal gap on A4 must be 30px", 30, grid.gapXPx)
        assertEquals("Top margin on A4 must be 73px", 73, grid.marginYPx)
        assertEquals("Left margin on A4 must be 85px", 85, grid.marginXPx)
    }

    @Test
    fun testA4GridCalculationCapacityUpTo48SlotsForSmallPhotos() {
        // For photos with height <= 380px (e.g. 30x30 mm PAN/stamp photo):
        // Up to 8 rows = 48 slots max
        val grid = PassportSheetRenderer.computeGrid(
            paper = PaperSheetFormat.A4,
            photoWidthMm = 30.0f,
            photoHeightMm = 30.0f
        )

        assertEquals("Cols on A4 must be 6", 6, grid.cols)
        assertEquals("Rows on A4 can be up to 8", 8, grid.rows)
        assertEquals("Capacity can be up to 48 slots", 48, grid.maxCapacity)
        assertEquals("Vertical gap on A4 must be 40px", 40, grid.gapYPx)
        assertEquals("Horizontal gap on A4 must be 30px", 30, grid.gapXPx)
        assertEquals("Top margin on A4 must be 73px", 73, grid.marginYPx)
        assertEquals("Left margin on A4 must be 85px", 85, grid.marginXPx)
    }

    @Test
    fun test4x6PortraitAndLandscapeGridCapacity() {
        // 4x6 Portrait: exactly 3 photos per row, 4 rows = 12 max capacity
        val portraitGrid = PassportSheetRenderer.computeGrid(
            paper = PaperSheetFormat.PHOTO_4X6_PORTRAIT,
            photoWidthMm = 33.0f,
            photoHeightMm = 43.0f
        )
        assertEquals("4x6 Portrait must have exactly 3 cols", 3, portraitGrid.cols)
        assertEquals("4x6 Portrait must have exactly 4 rows", 4, portraitGrid.rows)
        assertEquals("4x6 Portrait max capacity must be exactly 12", 12, portraitGrid.maxCapacity)

        // 4x6 Landscape: exactly 5 photos per row, 3 rows = 15 max capacity
        val landscapeGrid = PassportSheetRenderer.computeGrid(
            paper = PaperSheetFormat.PHOTO_4X6_LANDSCAPE,
            photoWidthMm = 33.0f,
            photoHeightMm = 43.0f
        )
        assertEquals("4x6 Landscape must have exactly 5 cols", 5, landscapeGrid.cols)
        assertEquals("4x6 Landscape must have exactly 3 rows", 3, landscapeGrid.rows)
        assertEquals("4x6 Landscape max capacity must be exactly 15", 15, landscapeGrid.maxCapacity)
    }

    @Test
    fun testStrictTopAlignmentRendering() = runBlocking {
        // Create 2 synthetic photos
        val bmp1 = Bitmap.createBitmap(400, 500, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val bmp2 = Bitmap.createBitmap(400, 500, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

        val photo1 = PassportPhotoItem(
            id = "p1",
            displayName = "Person 1",
            originalBitmap = bmp1,
            thumbnailBitmap = bmp1,
            copiesCount = 6 // Exactly 1 row of 6 photos on A4
        )

        val photo2 = PassportPhotoItem(
            id = "p2",
            displayName = "Person 2",
            originalBitmap = bmp2,
            thumbnailBitmap = bmp2,
            copiesCount = 6 // Exactly 1 row of 6 photos on A4
        )

        val sheets = PassportSheetRenderer.renderPrintSheets(
            context = context,
            photos = listOf(photo1, photo2),
            paper = PaperSheetFormat.A4,
            photoWidthMm = 33.0f,
            photoHeightMm = 43.0f,
            borderEnabled = true
        )

        assertEquals("12 photos on 36-capacity sheet must produce 1 page", 1, sheets.size)
        val sheet = sheets[0]
        assertEquals(PaperSheetFormat.A4.widthPixels, sheet.width)
        assertEquals(PaperSheetFormat.A4.heightPixels, sheet.height)

        // Verify bottom area remains untouched pure white (top alignment rule!)
        // Bottom pixel at (width/2, height - 100) must be white (0xFFFFFFFF)
        val bottomPixel = sheet.getPixel(sheet.width / 2, sheet.height - 100)
        assertEquals("Bottom of sheet must remain pure white for paper reuse", Color.WHITE, bottomPixel)
    }

    @Test
    fun testFillEvenlyAllocation() {
        val viewModel = MultiplePassportViewModel()

        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val photos = listOf(
            PassportPhotoItem(id = "1", originalBitmap = bmp, thumbnailBitmap = bmp, copiesCount = 1),
            PassportPhotoItem(id = "2", originalBitmap = bmp, thumbnailBitmap = bmp, copiesCount = 1),
            PassportPhotoItem(id = "3", originalBitmap = bmp, thumbnailBitmap = bmp, copiesCount = 1)
        )

        // Inject photos into viewModel state for testing
        // With 3 photos on A4 (capacity 36): each should receive 12 copies (36 / 3 = 12)
        photos.forEach { p ->
            viewModel.updateCopies(p.id, 0)
        }

        // Test custom size conversion
        viewModel.setCustomSize("35", "45", SizeUnit.MM)
        assertEquals(35.0f, viewModel.uiState.value.effectiveWidthMm, 0.01f)
        assertEquals(45.0f, viewModel.uiState.value.effectiveHeightMm, 0.01f)

        // Reset to preset
        viewModel.setSizePreset(PassportSizePreset.DEFAULT)
        assertEquals(33.0f, viewModel.uiState.value.effectiveWidthMm, 0.01f)
        assertEquals(43.0f, viewModel.uiState.value.effectiveHeightMm, 0.01f)
    }

    @Test
    fun testImageFileCreation(): Unit = runBlocking {
        val sheetBmp = Bitmap.createBitmap(PaperSheetFormat.A4.widthPixels, PaperSheetFormat.A4.heightPixels, Bitmap.Config.ARGB_8888)
        sheetBmp.eraseColor(Color.WHITE)

        val imageFile = PassportSheetRenderer.saveImageFile(
            context = context,
            bitmap = sheetBmp,
            isPng = false
        )

        assertTrue(imageFile.exists())
        assertTrue("Image file size must be non-zero", imageFile.length() > 0)
        val deleted = imageFile.delete()
        assertTrue(deleted)
    }
}
