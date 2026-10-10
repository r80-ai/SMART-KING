package com.example.passport

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Size units for passport photo custom sizing.
 */
enum class SizeUnit(val label: String, val toMmMultiplier: Float) {
    CM("cm", 10.0f),
    MM("mm", 1.0f),
    INCH("in", 25.4f)
}

/**
 * Standard preset photo sizes.
 */
data class PassportSizePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val widthMm: Float,
    val heightMm: Float,
    val isDefault: Boolean = false
) {
    val aspectRatio: Float
        get() = widthMm / heightMm

    /**
     * Compute exact integer pixel dimensions at 300 DPI.
     * Formula: (mm / 25.4) * 300
     */
    fun toPixelsAt300Dpi(): Pair<Int, Int> {
        val w = Math.round((widthMm / 25.4f) * 300)
        val h = Math.round((heightMm / 25.4f) * 300)
        return Pair(w, h)
    }

    companion object {
        val DEFAULT = PassportSizePreset(
            id = "default_33_43",
            title = "3.3 × 4.3 cm",
            subtitle = "1.2 × 1.5 in @ 300 DPI (Default)",
            widthMm = 33.0f,
            heightMm = 43.0f,
            isDefault = true
        )

        val STANDARD_PASSPORT = PassportSizePreset(
            id = "passport_35_45",
            title = "3.5 × 4.5 cm",
            subtitle = "India / EU / UK Standard",
            widthMm = 35.0f,
            heightMm = 45.0f
        )

        val INCH_12_15 = PassportSizePreset(
            id = "preset_12_15_in",
            title = "1.2 × 1.5 in",
            subtitle = "1.2 × 1.5 in (Stamp / Universal)",
            widthMm = 30.48f,
            heightMm = 38.10f
        )

        val US_VISA = PassportSizePreset(
            id = "visa_51_51",
            title = "5.1 × 5.1 cm",
            subtitle = "2 × 2 in (US / Schengen Visa)",
            widthMm = 50.8f,
            heightMm = 50.8f
        )

        val PAN_CARD = PassportSizePreset(
            id = "pan_35_35",
            title = "3.5 × 3.5 cm",
            subtitle = "NSDL / UTI PAN Card Photo",
            widthMm = 35.0f,
            heightMm = 35.0f
        )

        val ALL_PRESETS = listOf(DEFAULT, STANDARD_PASSPORT, INCH_12_15, US_VISA, PAN_CARD)
    }
}

/**
 * Background style selection.
 */
enum class BackgroundStyleType {
    ORIGINAL,
    SOLID,
    GRADIENT
}

/**
 * Background configuration for passport photo.
 */
data class PassportBackgroundOption(
    val id: String,
    val name: String,
    val type: BackgroundStyleType,
    val solidColor: Color = Color.White,
    val gradientColors: Pair<Color, Color> = Pair(Color.White, Color.LightGray)
) {
    companion object {
        val ORIGINAL = PassportBackgroundOption(
            id = "bg_original",
            name = "Original",
            type = BackgroundStyleType.ORIGINAL
        )

        val WHITE = PassportBackgroundOption(
            id = "bg_white",
            name = "White",
            type = BackgroundStyleType.SOLID,
            solidColor = Color.White
        )

        val LIGHT_BLUE = PassportBackgroundOption(
            id = "bg_light_blue",
            name = "Light Blue",
            type = BackgroundStyleType.SOLID,
            solidColor = Color(0xFFA2C8EC) // Official passport photo light blue
        )

        val SOFT_GRAY = PassportBackgroundOption(
            id = "bg_soft_gray",
            name = "Soft Gray",
            type = BackgroundStyleType.SOLID,
            solidColor = Color(0xFFE8ECEF)
        )

        val GRADIENT_STUDIO = PassportBackgroundOption(
            id = "bg_grad_studio",
            name = "Studio Light",
            type = BackgroundStyleType.GRADIENT,
            gradientColors = Pair(Color(0xFFFFFFFF), Color(0xFFD4D8DD))
        )

        val GRADIENT_BLUE = PassportBackgroundOption(
            id = "bg_grad_blue",
            name = "Soft Blue Grad",
            type = BackgroundStyleType.GRADIENT,
            gradientColors = Pair(Color(0xFFE3F2FD), Color(0xFF90CAF9))
        )

        val GRADIENT_SLATE = PassportBackgroundOption(
            id = "bg_grad_slate",
            name = "Slate Grad",
            type = BackgroundStyleType.GRADIENT,
            gradientColors = Pair(Color(0xFFECEFF1), Color(0xFFB0BEC5))
        )

        val ALL_STANDARD_PRESETS = listOf(ORIGINAL, WHITE, LIGHT_BLUE, SOFT_GRAY, GRADIENT_STUDIO, GRADIENT_BLUE, GRADIENT_SLATE)
    }
}

/**
 * Paper format specifications for Step 4.
 */
enum class PaperSheetFormat(
    val id: String,
    val title: String,
    val subtitle: String,
    val widthMm: Float,
    val heightMm: Float,
    val isLandscape: Boolean = false
) {
    A4(
        id = "a4",
        title = "A4 Paper",
        subtitle = "210 × 297 mm (2480 × 3508 px @ 300 DPI)",
        widthMm = 210.0f,
        heightMm = 297.0f,
        isLandscape = false
    ),
    PHOTO_4X6_PORTRAIT(
        id = "4x6_portrait",
        title = "4×6 Photo (Portrait)",
        subtitle = "101.6 × 152.4 mm (3 × 4 = 12 photos)",
        widthMm = 101.6f,
        heightMm = 152.4f,
        isLandscape = false
    ),
    PHOTO_4X6_LANDSCAPE(
        id = "4x6_landscape",
        title = "4×6 Photo (Landscape)",
        subtitle = "152.4 × 101.6 mm (5 × 3 = 15 photos)",
        widthMm = 152.4f,
        heightMm = 101.6f,
        isLandscape = true
    );

    /**
     * Exact pixel dimensions at 300 DPI.
     */
    val widthPixels: Int
        get() = Math.round((widthMm / 25.4f) * 300)

    val heightPixels: Int
        get() = Math.round((heightMm / 25.4f) * 300)

    val aspectRatio: Float
        get() = widthPixels.toFloat() / heightPixels.toFloat()
}

/**
 * Model representing an individual photo in the multiple passport maker.
 */
data class PassportPhotoItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri? = null,
    val displayName: String = "Photo",
    val originalBitmap: Bitmap,
    val thumbnailBitmap: Bitmap,
    // Crop & Pan State
    val zoomScale: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    // Adjustment filters (-100 to +100)
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    // Background style
    val backgroundOption: PassportBackgroundOption = PassportBackgroundOption.ORIGINAL,
    // Cached segmented foreground (transparent background) if background removal applied
    val segmentedForeground: Bitmap? = null,
    // Copy count for print sheet
    val copiesCount: Int = 1
)

/**
 * 4 UI Steps tracked by the top step indicator.
 */
enum class PassportFlowStep(val stepNumber: Int, val title: String, val shortLabel: String) {
    STEP_1_UPLOAD(1, "Upload Photos", "1. Upload"),
    STEP_2_CROP_ADJUST(2, "Crop, Size & Adjust", "2. Crop"),
    STEP_3_BACKGROUND(3, "Background Settings", "3. Background"),
    STEP_4_LAYOUT_EXPORT(4, "Print Sheet Layout", "4. Print Sheet")
}

/**
 * Studio tool tabs for editing selected photos and configuring sheets.
 */
enum class PassportStudioTab(val title: String, val shortLabel: String) {
    CROP_SIZE("Crop, Size & Adjust", "Crop & Size"),
    BACKGROUND("Background Settings", "Background"),
    PRINT_SHEET("Print Sheet & Export", "Print Sheet")
}

/**
 * Calculation result for placing passport photos on a sheet.
 */
data class SheetGridCalculation(
    val paper: PaperSheetFormat,
    val photoWidthPx: Int,
    val photoHeightPx: Int,
    val cols: Int,
    val rows: Int,
    val maxCapacity: Int,
    val marginXPx: Int,
    val marginYPx: Int,
    val gapXPx: Int,
    val gapYPx: Int
)
