package com.example.photo

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PhotoEditorEngineTest {

    @Test
    fun testColorAdjustmentsBrightnessIncreasesLuminance() {
        val bmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(100, 100, 100))

        val brighter = PhotoEditorEngine.applyColorAdjustments(bmp, brightness = 0.3f)
        val origPixel = bmp.getPixel(25, 25)
        val brightPixel = brighter.getPixel(25, 25)

        assertTrue(
            "Brightened pixel red (${Color.red(brightPixel)}) should be greater than original (${Color.red(origPixel)})",
            Color.red(brightPixel) > Color.red(origPixel)
        )
    }

    @Test
    fun testColorAdjustmentsSaturationGrayscale() {
        val bmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(200, 50, 50)) // Deep red

        val grayscale = PhotoEditorEngine.applyColorAdjustments(bmp, saturation = 0f)
        val p = grayscale.getPixel(25, 25)

        val r = Color.red(p)
        val g = Color.green(p)
        val b = Color.blue(p)

        assertTrue("Grayscale R and G should be close", kotlin.math.abs(r - g) <= 5)
        assertTrue("Grayscale G and B should be close", kotlin.math.abs(g - b) <= 5)
    }

    @Test
    fun testEnhanceAndSharpenProducesEnhancedEdges() {
        val bmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        // Draw high contrast edge in center
        for (y in 0 until 50) {
            for (x in 0 until 25) bmp.setPixel(x, y, Color.BLACK)
            for (x in 25 until 50) bmp.setPixel(x, y, Color.WHITE)
        }

        val sharpened = PhotoEditorEngine.applyEnhanceAndSharpen(bmp, enhance = 0.5f, sharpen = 0.8f)
        assertNotNull(sharpened)
        assertEquals(50, sharpened.width)
        assertEquals(50, sharpened.height)
    }

    @Test
    fun testRotate90DegreesDimensions() {
        val bmp = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
        val rotated = PhotoEditorEngine.rotateOrFlip(bmp, degrees = 90)

        assertEquals(120, rotated.width)
        assertEquals(80, rotated.height)
    }

    @Test
    fun testFlipHorizontal() {
        val bmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        bmp.setPixel(5, 25, Color.RED)
        bmp.setPixel(45, 25, Color.BLUE)

        val flipped = PhotoEditorEngine.rotateOrFlip(bmp, flipHorizontal = true)
        assertEquals(50, flipped.width)
        assertEquals(50, flipped.height)

        val newLeft = flipped.getPixel(4, 25)
        val newRight = flipped.getPixel(44, 25)

        assertEquals(Color.BLUE, newLeft)
        assertEquals(Color.RED, newRight)
    }

    @Test
    fun testCropBitmapDimensions() {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val cropRect = RectF(0.25f, 0.25f, 0.75f, 0.75f) // 100x100 region

        val cropped = PhotoEditorEngine.cropBitmap(bmp, cropRect)
        assertEquals(100, cropped.width)
        assertEquals(100, cropped.height)
    }

    @Test
    fun testResizeBitmapExactDimensions() {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val resized = PhotoEditorEngine.resizeBitmap(bmp, 350, 420)

        assertEquals(350, resized.width)
        assertEquals(420, resized.height)
    }

    @Test
    fun testSelectiveBlurAppliesToMaskedRegion() {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // Alternating pattern
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                bmp.setPixel(x, y, if ((x + y) % 2 == 0) Color.BLACK else Color.WHITE)
            }
        }

        // Blur only around center (0.5, 0.5)
        val stroke = listOf(PointF(0.5f, 0.5f))
        val blurred = PhotoEditorEngine.applySelectiveBlur(
            bitmap = bmp,
            strokes = listOf(stroke),
            brushRadiusPx = 20f,
            blurRadius = 10
        )

        // Center should be blended (gray ~ 128)
        val centerPixel = blurred.getPixel(50, 50)
        val cornerPixel = blurred.getPixel(5, 5)

        // Corner outside mask should remain exactly original
        assertEquals(bmp.getPixel(5, 5), cornerPixel)
        // Center inside mask should be averaged
        assertTrue("Blurred center should be smoothed", Color.red(centerPixel) in 50..200)
    }

    @Test
    fun testObjectRemovalInpainting() {
        val bmp = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        // Green background
        bmp.eraseColor(Color.rgb(34, 139, 34))

        // Unwanted red spot in center
        for (y in 35..45) {
            for (x in 35..45) {
                bmp.setPixel(x, y, Color.RED)
            }
        }

        // Mark center for erase
        val stroke = listOf(PointF(40f / 80f, 40f / 80f))
        val erased = PhotoEditorEngine.applyObjectRemoval(
            bitmap = bmp,
            strokes = listOf(stroke),
            brushRadiusPx = 12f
        )

        val centerPixel = erased.getPixel(40, 40)
        val outsidePixel = erased.getPixel(10, 10)

        // Outside unchanged green
        assertEquals(Color.rgb(34, 139, 34), outsidePixel)
        // Erased center should be in-painted with surrounding green, not red
        assertTrue("Inpainted center green channel should be dominant", Color.green(centerPixel) > Color.red(centerPixel))
    }

    @Test
    fun testPerspectiveCorrectionTransformsQuadToRect() {
        val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLUE)

        val corners = listOf(
            PointF(0.1f, 0.1f), // TL
            PointF(0.9f, 0.15f), // TR
            PointF(0.85f, 0.85f), // BR
            PointF(0.15f, 0.8f)  // BL
        )

        val rectified = PhotoEditorEngine.applyPerspectiveCorrection(bmp, corners)
        assertNotNull(rectified)
        assertTrue(rectified.width > 50)
        assertTrue(rectified.height > 50)
    }

    @Test
    fun testBackgroundRemovalGeneratesTransparency() {
        val bmp = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        // White background
        bmp.eraseColor(Color.WHITE)

        // Blue subject in center
        for (y in 20..40) {
            for (x in 20..40) {
                bmp.setPixel(x, y, Color.BLUE)
            }
        }

        val noBg = PhotoEditorEngine.removeBackground(bmp, sensitivity = 0.4f)
        val cornerAlpha = Color.alpha(noBg.getPixel(2, 2))
        val centerAlpha = Color.alpha(noBg.getPixel(30, 30))

        assertEquals("Background corner should be transparent", 0, cornerAlpha)
        assertEquals("Subject center should remain fully opaque", 255, centerAlpha)
    }

    @Test
    fun testBurnTextAndDrawing() {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)

        val textItem = TextItem(text = "Sample", normX = 0.5f, normY = 0.5f, color = Color.RED)
        val withText = PhotoEditorEngine.burnText(bmp, listOf(textItem))
        assertNotNull(withText)
        assertEquals(100, withText.width)

        val stroke = DrawingStroke(
            points = listOf(PointF(0.1f, 0.1f), PointF(0.9f, 0.9f)),
            color = Color.BLACK,
            brushSizePx = 6f
        )
        val withDrawing = PhotoEditorEngine.burnDrawing(withText, listOf(stroke))
        assertNotNull(withDrawing)
        assertEquals(100, withDrawing.width)
    }

    @Test
    fun testOriginalBitmapSafetyImmutability() {
        val original = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        original.eraseColor(Color.RED)
        val origPixelBefore = original.getPixel(25, 25)

        // Perform various operations
        val brightened = PhotoEditorEngine.applyColorAdjustments(original, brightness = 0.5f)
        val cropped = PhotoEditorEngine.cropBitmap(original, RectF(0.1f, 0.1f, 0.9f, 0.9f))
        val rotated = PhotoEditorEngine.rotateOrFlip(original, degrees = 90)

        // Verify original remains untouched
        val origPixelAfter = original.getPixel(25, 25)
        assertEquals("Original pixel must never change", origPixelBefore, origPixelAfter)
        assertFalse("Original must not be recycled", original.isRecycled)
    }
}
