package com.example.pvccard

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.pvccard.engine.PvcCardRenderer
import com.example.pvccard.model.PVC_CARD_HEIGHT_PX
import com.example.pvccard.model.PVC_CARD_WIDTH_PX
import com.example.pvccard.model.PvcCardItem
import com.example.pvccard.model.PvcCardTemplate
import com.example.pvccard.model.PvcCardType
import com.example.pvccard.model.PvcFactoryPresets
import com.example.pvccard.model.PvcSheetLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PvcCardRendererTest {

    @Test
    fun testCr80PvcDimensions() {
        assertEquals("Width must be 1012 px @ 300 DPI", 1012, PVC_CARD_WIDTH_PX)
        assertEquals("Height must be 638 px @ 300 DPI", 638, PVC_CARD_HEIGHT_PX)
    }

    @Test
    fun testCropToPvcSize() {
        val sample = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888)
        val cropped = PvcCardRenderer.cropToPvcSize(sample, Rect(100, 100, 1500, 900))

        assertNotNull(cropped)
        assertEquals(PVC_CARD_WIDTH_PX, cropped.width)
        assertEquals(PVC_CARD_HEIGHT_PX, cropped.height)
    }

    @Test
    fun testApplyEnhancements() {
        val card = Bitmap.createBitmap(PVC_CARD_WIDTH_PX, PVC_CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val enhanced = PvcCardRenderer.applyEnhancements(
            source = card,
            brightness = 10f,
            contrast = 15f,
            isTextDarkened = true,
            hasBorder = true
        )

        assertNotNull(enhanced)
        assertEquals(PVC_CARD_WIDTH_PX, enhanced.width)
        assertEquals(PVC_CARD_HEIGHT_PX, enhanced.height)
    }

    @Test
    fun testA4FiveCardsSheetLayoutDimensionsAndTopAlignment() {
        val front = Bitmap.createBitmap(PVC_CARD_WIDTH_PX, PVC_CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val back = Bitmap.createBitmap(PVC_CARD_WIDTH_PX, PVC_CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888)

        val card = PvcCardItem(
            name = "Test Aadhaar",
            template = PvcCardTemplate.AADHAAR,
            frontBitmap = front,
            backBitmap = back,
            copies = 2
        )

        val sheet = PvcCardRenderer.renderPrintSheet(
            layout = PvcSheetLayout.A4_FIVE_CARDS,
            cards = listOf(card)
        )

        assertNotNull(sheet)
        assertEquals(2480, sheet.width)
        assertEquals(3508, sheet.height)
    }

    @Test
    fun testPhoto4x6SheetLayout() {
        val front = Bitmap.createBitmap(PVC_CARD_WIDTH_PX, PVC_CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val back = Bitmap.createBitmap(PVC_CARD_WIDTH_PX, PVC_CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888)

        val card = PvcCardItem(
            name = "4x6 Card",
            frontBitmap = front,
            backBitmap = back
        )

        val sheet = PvcCardRenderer.renderPrintSheet(PvcSheetLayout.PHOTO_4X6_ONE_CARD, listOf(card))

        assertNotNull(sheet)
        assertEquals(1200, sheet.width)
        assertEquals(1800, sheet.height)
    }

    @Test
    fun testAllSevenFactoryPresets() {
        val expectedOfficialTypes = listOf(
            PvcCardType.AADHAAR,
            PvcCardType.PAN,
            PvcCardType.VOTER,
            PvcCardType.RATION,
            PvcCardType.AYUSHMAN,
            PvcCardType.ABHA,
            PvcCardType.UDID
        )

        for (type in expectedOfficialTypes) {
            val preset = PvcFactoryPresets.getPreset(type)
            assertNotNull("Preset must not be null for ${type.name}", preset)
            assertEquals(type, preset!!.cardType)

            assertTrue(preset.sourceDocWidth > 0f)
            assertTrue(preset.sourceDocHeight > 0f)

            // Front bounds verification
            assertTrue(preset.front.width > 0f)
            assertTrue(preset.front.height > 0f)
            assertTrue(preset.frontNorm.left >= 0f && preset.frontNorm.left <= 1f)
            assertTrue(preset.frontNorm.top >= 0f && preset.frontNorm.top <= 1f)
            assertTrue(preset.frontNorm.right <= 1.05f) // allow minor rounding
            assertTrue(preset.frontNorm.bottom <= 1.05f)

            // Back bounds verification
            assertTrue(preset.back.width > 0f)
            assertTrue(preset.back.height > 0f)
            assertTrue(preset.backNorm.left >= 0f && preset.backNorm.left <= 1f)
            assertTrue(preset.backNorm.top >= 0f && preset.backNorm.top <= 1f)
            assertTrue(preset.backNorm.right <= 1.05f)
            assertTrue(preset.backNorm.bottom <= 1.05f)
        }
    }
}
