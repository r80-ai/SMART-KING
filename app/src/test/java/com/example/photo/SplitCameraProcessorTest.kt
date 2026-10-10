package com.example.photo

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SplitCameraProcessorTest {

    @Test
    fun testCreateCombinedBitmapVertical1x2Dimensions() {
        val width = 200
        val height = 300
        val photo1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val photo2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val result = SplitCameraProcessor.createCombinedBitmap(
            firstPhoto = photo1,
            secondPhoto = photo2,
            layout = SplitLayout.VERTICAL_1X2,
            splitRatio = 0.5f,
            blendSeam = false,
            drawDividerLine = false
        )

        assertNotNull(result)
        assertEquals("Width must match", width, result.width)
        assertEquals("Height must match", height, result.height)
    }

    @Test
    fun testCreateCombinedBitmapHorizontal2x1Dimensions() {
        val width = 200
        val height = 300
        val photo1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val photo2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val result = SplitCameraProcessor.createCombinedBitmap(
            firstPhoto = photo1,
            secondPhoto = photo2,
            layout = SplitLayout.HORIZONTAL_2X1,
            splitRatio = 0.5f,
            blendSeam = false,
            drawDividerLine = false
        )

        assertNotNull(result)
        assertEquals("Width must match", width, result.width)
        assertEquals("Height must match", height, result.height)
    }

    @Test
    fun testSeamBlendNeverCrashes() {
        val width = 150
        val height = 150
        val photo1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val photo2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // 1x2 Vertical with seam blend
        val resVert = SplitCameraProcessor.createCombinedBitmap(
            firstPhoto = photo1,
            secondPhoto = photo2,
            layout = SplitLayout.VERTICAL_1X2,
            splitRatio = 0.5f,
            blendSeam = true,
            drawDividerLine = false
        )
        assertNotNull(resVert)
        assertEquals(width, resVert.width)
        assertEquals(height, resVert.height)

        // 2x1 Horizontal with seam blend
        val resHoriz = SplitCameraProcessor.createCombinedBitmap(
            firstPhoto = photo1,
            secondPhoto = photo2,
            layout = SplitLayout.HORIZONTAL_2X1,
            splitRatio = 0.5f,
            blendSeam = true,
            drawDividerLine = false
        )
        assertNotNull(resHoriz)
        assertEquals(width, resHoriz.width)
        assertEquals(height, resHoriz.height)
    }

    @Test
    fun testVerticalSeamBlendInterpolatesActualPixels() {
        val width = 200
        val height = 100
        // Left photo is solid black
        val blackPhoto = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        blackPhoto.eraseColor(Color.BLACK)

        // Right photo is solid white
        val whitePhoto = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        whitePhoto.eraseColor(Color.WHITE)

        val unblended = SplitCameraProcessor.createCombinedBitmap(
            firstPhoto = blackPhoto,
            secondPhoto = whitePhoto,
            layout = SplitLayout.VERTICAL_1X2,
            splitRatio = 0.5f,
            blendSeam = false,
            drawDividerLine = false
        )
        val splitX = (width * 0.5f).toInt()

        // Unblended has a hard discontinuity
        val unblendedLeft = Color.red(unblended.getPixel(splitX - 1, 50))
        val unblendedRight = Color.red(unblended.getPixel(splitX, 50))
        assertEquals("Unblended left must be 0", 0, unblendedLeft)
        assertEquals("Unblended right must be 255", 255, unblendedRight)

        // Now blend seam
        val blended = SplitCameraProcessor.createCombinedBitmap(
            firstPhoto = blackPhoto,
            secondPhoto = whitePhoto,
            layout = SplitLayout.VERTICAL_1X2,
            splitRatio = 0.5f,
            blendSeam = true,
            drawDividerLine = false
        )

        // Blended at seam should be a smooth blend between black and white (around 128)
        val seamPixelRed = Color.red(blended.getPixel(splitX, 50))
        org.junit.Assert.assertTrue("Seam pixel must be smoothly interpolated between 50 and 205, got $seamPixelRed", seamPixelRed in 50..205)

        // Outside blend zone, original photos remain pure black and pure white
        assertEquals("Far left untouched", 0, Color.red(blended.getPixel(2, 50)))
        assertEquals("Far right untouched", 255, Color.red(blended.getPixel(width - 3, 50)))
    }

    @Test
    fun testHorizontalSeamBlendInterpolatesActualPixels() {
        val width = 100
        val height = 200
        // Top photo is solid black
        val blackPhoto = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        blackPhoto.eraseColor(Color.BLACK)

        // Bottom photo is solid white
        val whitePhoto = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        whitePhoto.eraseColor(Color.WHITE)

        val splitY = (height * 0.5f).toInt()

        // Blend seam
        val blended = SplitCameraProcessor.createCombinedBitmap(
            firstPhoto = blackPhoto,
            secondPhoto = whitePhoto,
            layout = SplitLayout.HORIZONTAL_2X1,
            splitRatio = 0.5f,
            blendSeam = true,
            drawDividerLine = false
        )

        // Blended at seam should be a smooth blend between black and white
        val seamPixelRed = Color.red(blended.getPixel(50, splitY))
        org.junit.Assert.assertTrue("Seam pixel must be smoothly interpolated between 50 and 205, got $seamPixelRed", seamPixelRed in 50..205)

        // Outside blend zone, top is pure black and bottom is pure white
        assertEquals("Far top untouched", 0, Color.red(blended.getPixel(50, 2)))
        assertEquals("Far bottom untouched", 255, Color.red(blended.getPixel(50, height - 3)))
    }
}
