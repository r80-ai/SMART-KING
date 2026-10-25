package com.example

import android.graphics.Color
import com.example.pdf.PdfEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEffectsTest {

    @Test
    fun testEditItemTextEffectProperties() {
        val item = PdfEngine.EditItem(
            pageIndex = 0,
            text = "Test PDF Text",
            letterSpacing = 4.5f,
            lineSpacingMultiplier = 1.3f,
            textStrokeWidth = 3f,
            strokeColorHex = Color.RED,
            shadowRadius = 8f,
            shadowDx = 4f,
            shadowDy = 4f,
            shadowColorHex = Color.BLACK,
            innerShadowRadius = 6f,
            innerShadowDx = 2f,
            innerShadowDy = 2f,
            innerShadowColorHex = Color.DKGRAY,
            isEmboss = true,
            perspectiveSkewX = 15f,
            perspectiveSkewY = -10f,
            rotate3dX = 20f,
            rotate3dY = -25f,
            is3dText = true,
            depth3d = 12f,
            isReflection = true
        )

        assertEquals(4.5f, item.letterSpacing, 0.001f)
        assertEquals(1.3f, item.lineSpacingMultiplier, 0.001f)
        assertEquals(3f, item.textStrokeWidth, 0.001f)
        assertEquals(Color.RED, item.strokeColor)
        assertEquals(8f, item.shadowRadius, 0.001f)
        assertEquals(4f, item.shadowDx, 0.001f)
        assertEquals(4f, item.shadowDy, 0.001f)
        assertEquals(Color.BLACK, item.shadowColor)
        assertEquals(6f, item.innerShadowRadius, 0.001f)
        assertEquals(2f, item.innerShadowDx, 0.001f)
        assertEquals(2f, item.innerShadowDy, 0.001f)
        assertEquals(Color.DKGRAY, item.innerShadowColor)
        assertTrue(item.isEmbossEnabled)
        assertEquals(15f, item.perspectiveSkewX, 0.001f)
        assertEquals(-10f, item.perspectiveSkewY, 0.001f)
        assertEquals(20f, item.rotationX, 0.001f)
        assertEquals(-25f, item.rotationY, 0.001f)
        assertTrue(item.is3DTextEnabled)
        assertEquals(12f, item.extrusionDepth, 0.001f)
        assertTrue(item.isReflectionEnabled)
    }

    @Test
    fun testEditItemDefaultValues() {
        val defaultItem = PdfEngine.EditItem(pageIndex = 0, text = "Default")
        assertEquals(0f, defaultItem.letterSpacing, 0.001f)
        assertEquals(1.15f, defaultItem.lineSpacingMultiplier, 0.001f)
        assertEquals(0f, defaultItem.textStrokeWidth, 0.001f)
        assertEquals(0, defaultItem.strokeColor)
        assertEquals(0f, defaultItem.shadowRadius, 0.001f)
        assertEquals(0f, defaultItem.innerShadowRadius, 0.001f)
        assertFalse(defaultItem.isEmbossEnabled)
        assertEquals(0f, defaultItem.perspectiveSkewX, 0.001f)
        assertEquals(0f, defaultItem.perspectiveSkewY, 0.001f)
        assertEquals(0f, defaultItem.rotationX, 0.001f)
        assertEquals(0f, defaultItem.rotationY, 0.001f)
        assertFalse(defaultItem.is3DTextEnabled)
        assertEquals(0f, defaultItem.extrusionDepth, 0.001f)
        assertFalse(defaultItem.isReflectionEnabled)
    }
}
