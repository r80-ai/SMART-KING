package com.example

import com.example.hisab.ui.BillingItem
import com.example.ui.screens.DatePrefixType
import com.example.ui.screens.DocFilterMode
import com.example.ui.screens.PassportPreset
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

class CyberCafeUtilityToolsTest {

    @Test
    fun testBillingMemoCalculations() {
        val item1 = BillingItem("bw", "B&W Xerox", 20, 3.0)
        val item2 = BillingItem("color", "Color Print", 5, 10.0)
        val item3 = BillingItem("lam", "Lamination", 2, 20.0)
        val item4 = BillingItem("form", "Online Form", 1, 50.0)

        assertEquals(60.0, item1.total, 0.001)
        assertEquals(50.0, item2.total, 0.001)
        assertEquals(40.0, item3.total, 0.001)
        assertEquals(50.0, item4.total, 0.001)

        val items = listOf(item1, item2, item3, item4)
        val subtotal = items.sumOf { it.total }
        assertEquals(200.0, subtotal, 0.001)

        val discount = 15.0
        val grandTotal = (subtotal - discount).coerceAtLeast(0.0)
        assertEquals(185.0, grandTotal, 0.001)
    }

    @Test
    fun testBharatUpiPayloadFormat() {
        val upiId = "digitalcyber@okhdfcbank"
        val payeeName = "Digital Cyber Point"
        val amount = 185.0
        val note = "Bill #1042"

        val amtStr = String.format(java.util.Locale.US, "%.2f", amount)
        val cleanName = URLEncoder.encode(payeeName, "UTF-8")
        val cleanNote = URLEncoder.encode(note, "UTF-8")

        val payload = "upi://pay?pa=$upiId&pn=$cleanName&am=$amtStr&cu=INR&tn=$cleanNote"

        assertTrue(payload.startsWith("upi://pay?pa=digitalcyber@okhdfcbank"))
        assertTrue(payload.contains("&am=185.00"))
        assertTrue(payload.contains("&cu=INR"))
        assertTrue(payload.contains("Digital+Cyber+Point") || payload.contains("Digital%20Cyber%20Point"))
    }

    @Test
    fun testZxingQrCodeEncoding() {
        val testPayload = "upi://pay?pa=shop@upi&pn=Shop&am=50.00&cu=INR"
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(testPayload, BarcodeFormat.QR_CODE, 200, 200, hints)
        assertNotNull(bitMatrix)
        assertEquals(200, bitMatrix.width)
        assertEquals(200, bitMatrix.height)
        // Check that at least some pixels are true (black) and some false (white)
        var hasBlack = false
        var hasWhite = false
        for (x in 0 until 200) {
            for (y in 0 until 200) {
                if (bitMatrix.get(x, y)) hasBlack = true else hasWhite = true
            }
        }
        assertTrue(hasBlack)
        assertTrue(hasWhite)
    }

    @Test
    fun testPassportPresetsAndPrefixes() {
        val preset = PassportPreset.STANDARD_35_45
        assertEquals(413, preset.widthPx)
        assertEquals(531, preset.heightPx)

        val prefixDop = DatePrefixType.DOP
        assertEquals("D.O.P: ", prefixDop.prefix)

        val prefixDob = DatePrefixType.DOB
        assertEquals("D.O.B: ", prefixDob.prefix)

        val prefixNone = DatePrefixType.DATE_ONLY
        assertEquals("", prefixNone.prefix)
    }

    @Test
    fun testDocFilterModes() {
        assertEquals("Clean Photocopy / B&W", DocFilterMode.CLEAN_PHOTOCOPY.labelEn)
        assertEquals("Magic Color", DocFilterMode.MAGIC_COLOR.labelEn)
        assertEquals("Soft Grayscale", DocFilterMode.SOFT_GRAYSCALE.labelEn)
        assertEquals("Original", DocFilterMode.ORIGINAL.labelEn)
    }
}
