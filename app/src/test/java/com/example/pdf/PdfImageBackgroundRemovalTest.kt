package com.example.pdf

import android.graphics.Bitmap
import android.graphics.Color
import com.example.photo.birefnet.BiRefNetLiteEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PdfImageBackgroundRemovalTest {

    @Test
    fun testEditItemImagePropertiesAndAlphaMask() {
        val sampleBmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val origBmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val fakeMask = FloatArray(120 * 120) { 1.0f }

        val imageItem = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            imageBitmap = sampleBmp,
            originalImageBitmap = origBmp,
            posX = 50f,
            posY = 80f,
            imageWidth = 200f,
            imageHeight = 200f,
            alphaMask = fakeMask,
            maskWidth = 120,
            maskHeight = 120
        )

        assertEquals(PdfEngine.AnnotationType.IMAGE, imageItem.type)
        assertNotNull(imageItem.imageBitmap)
        assertNotNull(imageItem.originalImageBitmap)
        assertEquals(fakeMask, imageItem.alphaMask)
        assertEquals(120, imageItem.maskWidth)
        assertEquals(120, imageItem.maskHeight)
    }

    @Test
    fun testPreserveOriginalImageOnBackgroundRemoval() {
        val originalBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val initialItem = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            imageBitmap = originalBmp,
            originalImageBitmap = originalBmp
        )

        // Simulate AI background removal result
        val transparentCutout = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            setPixel(50, 50, Color.RED)
        }
        val newMask = FloatArray(100 * 100) { 0f }.apply {
            this[50 * 100 + 50] = 1.0f
        }

        val updatedItem = initialItem.copy(
            imageBitmap = transparentCutout,
            originalImageBitmap = initialItem.originalImageBitmap ?: initialItem.imageBitmap,
            alphaMask = newMask,
            maskWidth = 100,
            maskHeight = 100
        )

        assertEquals(transparentCutout, updatedItem.imageBitmap)
        assertEquals(originalBmp, updatedItem.originalImageBitmap)
        assertEquals(Color.TRANSPARENT, updatedItem.imageBitmap?.getPixel(0, 0))
        assertEquals(Color.RED, updatedItem.imageBitmap?.getPixel(50, 50))
        assertEquals(Color.RED, updatedItem.originalImageBitmap?.getPixel(0, 0))
    }

    @Test
    fun testBiRefNetApplyBrushToMaskEraseAndRestore() {
        val width = 50
        val height = 50
        // Initial solid mask
        val mask = FloatArray(width * height) { 1.0f }

        // Erase center circle
        BiRefNetLiteEngine.applyBrushToMask(
            mask = mask,
            width = width,
            height = height,
            centerX = 25f,
            centerY = 25f,
            radius = 10f,
            isRestore = false,
            isSoft = false
        )

        val centerIdx = 25 * width + 25
        val cornerIdx = 0
        assertEquals(0f, mask[centerIdx], 0.001f)
        assertEquals(1f, mask[cornerIdx], 0.001f)

        // Restore center circle
        BiRefNetLiteEngine.applyBrushToMask(
            mask = mask,
            width = width,
            height = height,
            centerX = 25f,
            centerY = 25f,
            radius = 10f,
            isRestore = true,
            isSoft = false
        )
        assertEquals(1f, mask[centerIdx], 0.001f)
    }

    @Test
    fun testForegroundBitmapFromRefinedMask() {
        val width = 20
        val height = 20
        val originalBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
        }

        val alphaMask = FloatArray(width * height) { 0f }
        // Set center pixel as opaque
        alphaMask[10 * width + 10] = 1.0f

        val foreground = BiRefNetLiteEngine.createRefinedForegroundBitmap(
            original = originalBmp,
            alphaMask = alphaMask,
            width = width,
            height = height
        )

        assertNotNull(foreground)
        assertEquals(width, foreground.width)
        assertEquals(height, foreground.height)
        assertEquals(0, Color.alpha(foreground.getPixel(0, 0)))
        assertTrue(Color.alpha(foreground.getPixel(10, 10)) > 200)
    }
}
