package com.example.pvccard.model

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import java.util.UUID

/**
 * Standard CR80 PVC Card Dimensions:
 * 85.60 mm × 53.98 mm (approx 86 × 54 mm)
 * At 300 DPI:
 * Width = (85.60 / 25.4) * 300 ≈ 1012 px
 * Height = (53.98 / 25.4) * 300 ≈ 638 px
 * Aspect ratio = 1012 / 638 ≈ 1.58577
 */
const val PVC_CARD_WIDTH_PX = 1012
const val PVC_CARD_HEIGHT_PX = 638
const val PVC_CARD_DPI = 300
const val PVC_CORNER_RADIUS_PX = 24f // approx 2 mm corner radius for standard CR80

/**
 * Supported print sheet layouts.
 */
enum class PvcSheetLayout(
    val id: String,
    val title: String,
    val subtitle: String,
    val widthPx: Int,
    val heightPx: Int,
    val maxCards: Int
) {
    A4_FIVE_CARDS(
        id = "a4_5_cards",
        title = "A4 (5 Cards Side-by-Side)",
        subtitle = "Front & Back side-by-side, 5 rows, strict top-alignment",
        widthPx = 2480,
        heightPx = 3508,
        maxCards = 5
    ),
    PHOTO_4X6_ONE_CARD(
        id = "4x6_one_card",
        title = "4×6 Photo Paper (1 Card)",
        subtitle = "Front & Back with center fold line",
        widthPx = 1200,
        heightPx = 1800,
        maxCards = 1
    )
}

/**
 * Official card preset types including factory calibrated government ID cards.
 */
enum class PvcCardType(val displayName: String) {
    AADHAAR("e-Aadhaar"),
    PAN("PAN Card"),
    VOTER("Voter ID (e-EPIC)"),
    RATION("Ration Card"),
    AYUSHMAN("Ayushman Bharat"),
    ABHA("ABHA / Health Card"),
    UDID("UDID Card"),
    CUSTOM("Custom / Scanned Card");

    val id: String get() = name.lowercase()
    val defaultFrontNorm: RectFNorm get() = PvcFactoryPresets.getPreset(this)?.frontNorm ?: RectFNorm(0.070f, 0.400f, 0.408f, 0.182f)
    val defaultBackNorm: RectFNorm get() = PvcFactoryPresets.getPreset(this)?.backNorm ?: RectFNorm(0.515f, 0.400f, 0.408f, 0.182f)
}

typealias PvcCardTemplate = PvcCardType

data class CropCoordinates(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun toRectF(): RectF = RectF(x, y, x + width, y + height)

    fun toNorm(docW: Float, docH: Float): RectFNorm = RectFNorm(
        left = x / docW,
        top = y / docH,
        width = width / docW,
        height = height / docH
    )
}

data class PvcCardPreset(
    val cardType: PvcCardType,
    val sourceDocWidth: Float,
    val sourceDocHeight: Float,
    val front: CropCoordinates,
    val back: CropCoordinates
) {
    val frontNorm: RectFNorm get() = front.toNorm(sourceDocWidth, sourceDocHeight)
    val backNorm: RectFNorm get() = back.toNorm(sourceDocWidth, sourceDocHeight)
}

object PvcFactoryPresets {
    // 1. e-Aadhaar (Source: 2550 x 3300)
    val AADHAAR = PvcCardPreset(
        cardType = PvcCardType.AADHAAR,
        sourceDocWidth = 2550f, sourceDocHeight = 3300f,
        front = CropCoordinates(x = 208.7f, y = 2392.5f, width = 1034.6f, height = 659.7f),
        back = CropCoordinates(x = 1300.6f, y = 2392.5f, width = 1040.4f, height = 659.7f)
    )

    // 2. PAN Card (Source: 2479 x 3508)
    val PAN = PvcCardPreset(
        cardType = PvcCardType.PAN,
        sourceDocWidth = 2479f, sourceDocHeight = 3508f,
        front = CropCoordinates(x = 286.1f, y = 2716.5f, width = 968.1f, height = 608.6f),
        back = CropCoordinates(x = 1284.1f, y = 2716.5f, width = 968.1f, height = 602.4f)
    )

    // 3. Voter ID e-EPIC (Source: 2479 x 3508)
    val VOTER = PvcCardPreset(
        cardType = PvcCardType.VOTER,
        sourceDocWidth = 2479f, sourceDocHeight = 3508f,
        front = CropCoordinates(x = 141.5f, y = 399.0f, width = 1011.4f, height = 638.5f),
        back = CropCoordinates(x = 1363.1f, y = 397.9f, width = 1017.6f, height = 639.5f)
    )

    // 4. Ration Card (Source: 2479 x 3508)
    val RATION = PvcCardPreset(
        cardType = PvcCardType.RATION,
        sourceDocWidth = 2479f, sourceDocHeight = 3508f,
        front = CropCoordinates(x = 224.7f, y = 2778.8f, width = 980.7f, height = 592.4f),
        back = CropCoordinates(x = 1285.8f, y = 2782.1f, width = 980.8f, height = 582.2f)
    )

    // 5. Ayushman Bharat (Source: 1275 x 642)
    val AYUSHMAN = PvcCardPreset(
        cardType = PvcCardType.AYUSHMAN,
        sourceDocWidth = 1275f, sourceDocHeight = 642f,
        front = CropCoordinates(x = 42.5f, y = 21.5f, width = 1211.3f, height = 609.9f),
        back = CropCoordinates(x = 38.3f, y = 21.5f, width = 1211.3f, height = 609.9f)
    )

    // 6. ABHA / Health Card (Source: 1937 x 2400)
    val ABHA = PvcCardPreset(
        cardType = PvcCardType.ABHA,
        sourceDocWidth = 1937f, sourceDocHeight = 2400f,
        front = CropCoordinates(x = 51.5f, y = 43.5f, width = 1833.0f, height = 1124.5f),
        back = CropCoordinates(x = 48.0f, y = 1228.0f, width = 1836.5f, height = 1114.0f)
    )

    // 7. UDID Card (Unique Disability ID) (Source: 2479 x 3504)
    val UDID = PvcCardPreset(
        cardType = PvcCardType.UDID,
        sourceDocWidth = 2479f, sourceDocHeight = 3504f,
        front = CropCoordinates(x = 219.5f, y = 206.4f, width = 1031.9f, height = 637.7f),
        back = CropCoordinates(x = 234.7f, y = 1146.2f, width = 1026.8f, height = 658.2f)
    )

    fun getPreset(type: PvcCardType): PvcCardPreset? = when(type) {
        PvcCardType.AADHAAR -> AADHAAR
        PvcCardType.PAN -> PAN
        PvcCardType.VOTER -> VOTER
        PvcCardType.RATION -> RATION
        PvcCardType.AYUSHMAN -> AYUSHMAN
        PvcCardType.ABHA -> ABHA
        PvcCardType.UDID -> UDID
        PvcCardType.CUSTOM -> null
    }
}

/**
 * Normalized rectangle coordinates in 0f..1f relative to source document.
 */
data class RectFNorm(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height

    fun toPixelRect(imageW: Int, imageH: Int): Rect {
        val l = (left * imageW).toInt().coerceIn(0, imageW - 1)
        val t = (top * imageH).toInt().coerceIn(0, imageH - 1)
        val r = (right * imageW).toInt().coerceIn(l + 1, imageW)
        val b = (bottom * imageH).toInt().coerceIn(t + 1, imageH)
        return Rect(l, t, r, b)
    }
}

/**
 * Represents a completed, print-ready PVC Smart Card.
 */
data class PvcCardItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Smart Card",
    val template: PvcCardTemplate = PvcCardTemplate.AADHAAR,
    val frontBitmap: Bitmap,
    val backBitmap: Bitmap? = null,
    val copies: Int = 1,
    // Enhancements
    val brightness: Float = 0f, // -100 to 100
    val contrast: Float = 0f,   // -100 to 100
    val isTextDarkened: Boolean = false, // Sharp text & barcode contrast
    val hasPerimeterBorder: Boolean = true // Clean 2px border around card perimeter
)

/**
 * Active workflow step inside the PVC Card Maker.
 */
enum class PvcWorkflowStep(val title: String) {
    IMPORT("1. Import & Auto-Crop"),
    MANUAL_CROP("2. De-Skew & Crop"),
    ENHANCE_PREVIEW("3. Enhance & Preview"),
    PRINT_SHEET("4. Print Sheet")
}
