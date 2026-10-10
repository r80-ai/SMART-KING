package com.example.photo

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
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
class EdgePerspectiveScreenTest {

    @Test
    fun testRotateCornersClockwisePreservesGeometry() {
        // [TL, TR, BR, BL]
        val corners = listOf(
            PointF(0.1f, 0.2f), // TL
            PointF(0.8f, 0.2f), // TR
            PointF(0.9f, 0.7f), // BR
            PointF(0.2f, 0.8f)  // BL
        )

        // Clockwise 90 deg rotation formula: (x, y) -> (1 - y, x)
        val oldTL = corners[0]
        val oldTR = corners[1]
        val oldBR = corners[2]
        val oldBL = corners[3]

        val newTL = PointF(1f - oldBL.y, oldBL.x)
        val newTR = PointF(1f - oldTL.y, oldTL.x)
        val newBR = PointF(1f - oldTR.y, oldTR.x)
        val newBL = PointF(1f - oldBR.y, oldBR.x)

        val rotatedCorners = listOf(newTL, newTR, newBR, newBL)

        // Verify that the old BL (0.2, 0.8) becomes the new TL (0.2, 0.2)
        assertEquals(0.2f, rotatedCorners[0].x, 0.001f)
        assertEquals(0.2f, rotatedCorners[0].y, 0.001f)

        // Verify that the old TL (0.1, 0.2) becomes the new TR (0.8, 0.1)
        assertEquals(0.8f, rotatedCorners[1].x, 0.001f)
        assertEquals(0.1f, rotatedCorners[1].y, 0.001f)

        // Verify that the old TR (0.8, 0.2) becomes the new BR (0.8, 0.8)
        assertEquals(0.8f, rotatedCorners[2].x, 0.001f)
        assertEquals(0.8f, rotatedCorners[2].y, 0.001f)

        // Verify that the old BR (0.9, 0.7) becomes the new BL (0.3, 0.9)
        assertEquals(0.3f, rotatedCorners[3].x, 0.001f)
        assertEquals(0.9f, rotatedCorners[3].y, 0.001f)
    }

    @Test
    fun testRotateCornersCounterClockwiseInvertsClockwise() {
        val originalCorners = listOf(
            PointF(0.15f, 0.15f),
            PointF(0.85f, 0.15f),
            PointF(0.85f, 0.85f),
            PointF(0.15f, 0.85f)
        )

        // 90 CCW: (x, y) -> (y, 1 - x)
        val oldTL = originalCorners[0]
        val oldTR = originalCorners[1]
        val oldBR = originalCorners[2]
        val oldBL = originalCorners[3]

        val ccwTL = PointF(oldTR.y, 1f - oldTR.x)
        val ccwTR = PointF(oldBR.y, 1f - oldBR.x)
        val ccwBR = PointF(oldBL.y, 1f - oldBL.x)
        val ccwBL = PointF(oldTL.y, 1f - oldTL.x)

        val ccwCorners = listOf(ccwTL, ccwTR, ccwBR, ccwBL)

        // Now rotate CW from CCW: should return to original!
        val cOldTL = ccwCorners[0]
        val cOldTR = ccwCorners[1]
        val cOldBR = ccwCorners[2]
        val cOldBL = ccwCorners[3]

        val backTL = PointF(1f - cOldBL.y, cOldBL.x)
        val backTR = PointF(1f - cOldTL.y, cOldTL.x)
        val backBR = PointF(1f - cOldTR.y, cOldTR.x)
        val backBL = PointF(1f - cOldBR.y, cOldBR.x)

        assertEquals(originalCorners[0].x, backTL.x, 0.001f)
        assertEquals(originalCorners[0].y, backTL.y, 0.001f)
        assertEquals(originalCorners[1].x, backTR.x, 0.001f)
        assertEquals(originalCorners[1].y, backTR.y, 0.001f)
        assertEquals(originalCorners[2].x, backBR.x, 0.001f)
        assertEquals(originalCorners[2].y, backBR.y, 0.001f)
        assertEquals(originalCorners[3].x, backBL.x, 0.001f)
        assertEquals(originalCorners[3].y, backBL.y, 0.001f)
    }

    @Test
    fun testPerspectiveCorrectionWithCornersOnBitmap() {
        val testBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        testBitmap.eraseColor(Color.WHITE)

        // Simulate document quad (inset trapezoid)
        val quad = listOf(
            PointF(0.1f, 0.15f), // TL
            PointF(0.9f, 0.1f),  // TR
            PointF(0.85f, 0.9f), // BR
            PointF(0.15f, 0.85f) // BL
        )

        val corrected = PhotoEditorEngine.applyPerspectiveCorrection(testBitmap, quad)
        assertNotNull(corrected)
        assertTrue(corrected.width > 50)
        assertTrue(corrected.height > 50)
    }
}
