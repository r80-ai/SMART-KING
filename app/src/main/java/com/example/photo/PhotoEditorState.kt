package com.example.photo

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Typeface

enum class PhotoEditCategory(val displayName: String) {
    BASIC("Basic"),
    ADJUST("Adjust"),
    TOOLS("Tools"),
    MARKUP("Markup")
}

enum class PhotoEditTool(val displayName: String, val category: PhotoEditCategory) {
    NONE("None", PhotoEditCategory.BASIC),
    // Basic
    CROP("Crop", PhotoEditCategory.BASIC),
    ROTATE_FLIP("Rotate / Flip", PhotoEditCategory.BASIC),
    RESIZE("Resize", PhotoEditCategory.BASIC),

    // Adjust
    BRIGHTNESS("Brightness", PhotoEditCategory.ADJUST),
    CONTRAST("Contrast", PhotoEditCategory.ADJUST),
    SATURATION("Saturation", PhotoEditCategory.ADJUST),
    ENHANCE("Enhance", PhotoEditCategory.ADJUST),
    SHARPEN("Sharpen", PhotoEditCategory.ADJUST),

    // Tools
    BLUR("Blur", PhotoEditCategory.TOOLS),
    ERASE("Erase / Remove", PhotoEditCategory.TOOLS),
    PERSPECTIVE("Perspective", PhotoEditCategory.TOOLS),
    BG_REMOVE("BG Remove", PhotoEditCategory.TOOLS),

    // Markup
    TEXT("Text", PhotoEditCategory.MARKUP),
    DRAWING("Drawing", PhotoEditCategory.MARKUP)
}

enum class CropPreset(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE_1_1("1:1", 1f),
    STANDARD_4_3("4:3", 4f / 3f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    STORY_9_16("9:16", 9f / 16f),
    CLASSIC_3_2("3:2", 3f / 2f)
}

data class TextItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String = "Double tap to edit",
    var normX: Float = 0.5f,
    var normY: Float = 0.5f,
    var fontSizeSp: Float = 28f,
    var color: Int = android.graphics.Color.WHITE,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isStrikethrough: Boolean = false,
    var alignment: TextAlignment = TextAlignment.CENTER,
    var opacity: Float = 1.0f,
    var rotationDeg: Float = 0f,
    var fontId: String = "sans_serif",
    var letterSpacing: Float = 0f,
    var lineSpacingMultiplier: Float = 1.0f,
    var curveArc: Float = 0f,
    var backgroundColor: Int? = null,
    var backgroundCornerRadius: Float = 0f,
    var backgroundPadding: Float = 0f,
    var strokeColor: Int? = null,
    var strokeWidth: Float = 0f,
    var shadowColor: Int? = null,
    var shadowRadius: Float = 0f,
    var shadowDx: Float = 0f,
    var shadowDy: Float = 0f,
    var innerShadowColor: Int? = null,
    var innerShadowRadius: Float = 0f,
    var isEmboss: Boolean = false,
    var perspectiveSlant: Float = 0f,
    var rotationX: Float = 0f,
    var rotationY: Float = 0f,
    var depth3D: Float = 0f,
    var depth3DColor: Int? = null,
    var shadow3D: Boolean = false,
    var hasReflection: Boolean = false,
    var textureBitmap: Bitmap? = null,
    var customTypeface: Typeface? = null,
    var paddingX: Float = 0f,
    var paddingY: Float = 0f,
    var isVisible: Boolean = true,
    var isLocked: Boolean = false,
    var boxWidth: Float = 0f,
    var boxHeight: Float = 0f,
    var depth3DShadowDistance: Float = 12f
)

data class ImageLayerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bitmap: Bitmap,
    var normX: Float = 0.5f,
    var normY: Float = 0.5f,
    var widthPx: Float = 220f,
    var heightPx: Float = 220f,
    var rotationDeg: Float = 0f,
    var opacity: Float = 1f,
    var isVisible: Boolean = true,
    var isLocked: Boolean = false
)

enum class TextAlignment {
    LEFT, CENTER, RIGHT, JUSTIFY
}

data class DrawingStroke(
    val points: List<PointF>,
    val color: Int,
    val brushSizePx: Float,
    val opacity: Float = 1f,
    val isEraser: Boolean = false
)

enum class BgOptionType(val label: String) {
    TRANSPARENT("Transparent"),
    SOLID_COLOR("Color"),
    GRADIENT("Gradient"),
    IMAGE("Image")
}

enum class GradientDirection(val label: String) {
    VERTICAL("Vertical"),
    HORIZONTAL("Horizontal"),
    DIAGONAL("Diagonal")
}

data class EditorHistoryState(
    val bitmap: android.graphics.Bitmap,
    val textItems: List<TextItem>,
    val drawStrokes: List<DrawingStroke> = emptyList()
)

enum class BgRefineMode {
    NONE,
    ERASE_BG,
    RESTORE_SUBJECT
}

data class MaskStroke(
    val points: List<PointF>,
    val radiusPx: Float,
    val isRestore: Boolean,
    val isSoft: Boolean = true
)
