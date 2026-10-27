package com.example.photo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
class FaceRelightEngineTest {

    @Test
    fun testBlendRelitAtZeroIntensityReturnsOriginal() {
        val orig = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
        val relit = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }

        val blended = FaceRelightEngine.blendRelit(orig, relit, 0f)
        assertEquals("Blended at 0 intensity should match original", Color.BLACK, blended.getPixel(10, 10))
    }

    @Test
    fun testBlendRelitAtFullIntensityReturnsRelit() {
        val orig = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
        val relit = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }

        val blended = FaceRelightEngine.blendRelit(orig, relit, 1f)
        assertEquals("Blended at 1.0 intensity should match relit", Color.WHITE, blended.getPixel(10, 10))
    }

    @Test
    fun testBlendRelitAtMidIntensityInterpolatesCorrectly() {
        val orig = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(50, 50, 50))
        }
        val relit = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(150, 150, 150))
        }

        val blended = FaceRelightEngine.blendRelit(orig, relit, 0.5f)
        val p = blended.getPixel(10, 10)
        val r = Color.red(p)

        assertTrue("Blended pixel value should be between orig and relit ($r)", r in 85..115)
    }

    @Test
    fun testGenerateFullRelitBitmapProducesValidIlluminatedBitmap() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val orig = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(60, 50, 40))
        }

        val relit = FaceRelightEngine.generateFullRelitBitmap(context, orig)
        assertNotNull(relit)
        assertEquals(orig.width, relit.width)
        assertEquals(orig.height, relit.height)

        val origPx = orig.getPixel(20, 20)
        val relitPx = relit.getPixel(20, 20)

        assertTrue(
            "Relit pixel (${Color.red(relitPx)}) should be brighter than original (${Color.red(origPx)})",
            Color.red(relitPx) > Color.red(origPx)
        )
    }
}
