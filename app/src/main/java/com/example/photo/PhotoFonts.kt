package com.example.photo

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

data class EditorFont(
    val id: String,
    val displayName: String,
    val category: String, // "English" or "Bengali"
    val fontFamily: FontFamily,
    val previewSample: String,
    val defaultBold: Boolean = false,
    val defaultItalic: Boolean = false
)

object PhotoFonts {
    val ALL_FONTS = listOf(
        // English Fonts
        EditorFont(
            id = "sans_serif",
            displayName = "Sans Serif",
            category = "English",
            fontFamily = FontFamily.SansSerif,
            previewSample = "Photo Studio"
        ),
        EditorFont(
            id = "serif",
            displayName = "Serif",
            category = "English",
            fontFamily = FontFamily.Serif,
            previewSample = "Photo Studio"
        ),
        EditorFont(
            id = "bold",
            displayName = "Bold",
            category = "English",
            fontFamily = FontFamily.SansSerif,
            previewSample = "Photo Studio",
            defaultBold = true
        ),
        EditorFont(
            id = "modern",
            displayName = "Modern",
            category = "English",
            fontFamily = FontFamily.Monospace,
            previewSample = "Photo Studio"
        ),
        EditorFont(
            id = "clean",
            displayName = "Clean",
            category = "English",
            fontFamily = FontFamily.Default,
            previewSample = "Photo Studio"
        ),
        EditorFont(
            id = "handwriting_1",
            displayName = "Handwriting 1",
            category = "English",
            fontFamily = FontFamily.Cursive,
            previewSample = "Photo Studio"
        ),
        EditorFont(
            id = "handwriting_2",
            displayName = "Handwriting 2",
            category = "English",
            fontFamily = FontFamily.Cursive,
            previewSample = "Photo Studio",
            defaultItalic = true
        ),
        EditorFont(
            id = "handwriting_3",
            displayName = "Handwriting 3",
            category = "English",
            fontFamily = FontFamily.SansSerif,
            previewSample = "Photo Studio",
            defaultItalic = true
        ),
        EditorFont(
            id = "script",
            displayName = "Script",
            category = "English",
            fontFamily = FontFamily.Cursive,
            previewSample = "Photo Studio",
            defaultBold = true
        ),
        EditorFont(
            id = "signature",
            displayName = "Signature style",
            category = "English",
            fontFamily = FontFamily.Cursive,
            previewSample = "Photo Studio"
        ),

        // Bengali Fonts (properly supports Bengali Unicode)
        EditorFont(
            id = "bn_sans",
            displayName = "Bengali Sans",
            category = "Bengali",
            fontFamily = FontFamily.SansSerif,
            previewSample = "বাংলা লেখা"
        ),
        EditorFont(
            id = "bn_serif",
            displayName = "Bengali Serif",
            category = "Bengali",
            fontFamily = FontFamily.Serif,
            previewSample = "বাংলা লেখা"
        ),
        EditorFont(
            id = "bn_bold",
            displayName = "Bengali Bold",
            category = "Bengali",
            fontFamily = FontFamily.SansSerif,
            previewSample = "বাংলা লেখা",
            defaultBold = true
        ),
        EditorFont(
            id = "bn_handwriting_1",
            displayName = "Bengali Handwriting 1",
            category = "Bengali",
            fontFamily = FontFamily.Cursive,
            previewSample = "বাংলা লেখা"
        ),
        EditorFont(
            id = "bn_handwriting_2",
            displayName = "Bengali Handwriting 2",
            category = "Bengali",
            fontFamily = FontFamily.Cursive,
            previewSample = "বাংলা লেখা",
            defaultItalic = true
        ),
        EditorFont(
            id = "bn_clean",
            displayName = "Bengali Clean",
            category = "Bengali",
            fontFamily = FontFamily.Default,
            previewSample = "বাংলা লেখা"
        )
    )

    fun getFont(fontId: String): EditorFont {
        return ALL_FONTS.find { it.id == fontId } ?: ALL_FONTS.first()
    }

    /**
     * Converts font ID, bold, and italic flags to Android Canvas Typeface
     * ensuring proper shaping for both English and Bengali Unicode text.
     */
    fun getTypeface(fontId: String, isBold: Boolean, isItalic: Boolean): Typeface {
        val font = getFont(fontId)
        val finalBold = isBold || font.defaultBold
        val finalItalic = isItalic || font.defaultItalic

        val style = when {
            finalBold && finalItalic -> Typeface.BOLD_ITALIC
            finalBold -> Typeface.BOLD
            finalItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        return when (font.id) {
            "serif", "bn_serif" -> Typeface.create(Typeface.SERIF, style)
            "modern" -> Typeface.create(Typeface.MONOSPACE, style)
            "handwriting_1", "script", "signature", "bn_handwriting_1" -> Typeface.create("cursive", style)
            "handwriting_2", "bn_handwriting_2" -> Typeface.create("casual", style)
            "handwriting_3" -> Typeface.create("sans-serif-light", style)
            "clean", "bn_clean" -> Typeface.create("sans-serif-medium", style)
            "bold", "bn_bold" -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            else -> Typeface.create(Typeface.SANS_SERIF, style)
        }
    }
}
