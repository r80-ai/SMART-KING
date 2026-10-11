package com.example.amardokan.ui

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AmarDokanColors {
    val NavyDarkHeader = Color(0xFF0F2042)      // Dark Navy Header
    val NavyDarkHeaderLight = Color(0xFF193264) // Gradient secondary
    val ContentBg = Color(0xFFF7F9FC)           // White/Off-white content background
    val CardBg = Color(0xFFFFFFFF)              // Pure white cards
    val BorderLight = Color(0xFFE2E8F0)         // Thin clean border
    val PrimaryBlue = Color(0xFF1565C0)         // Standard blue action button
    val PrimaryBlueDark = Color(0xFF0D47A1)     // Dark blue
    val CashGreen = Color(0xFF2E7D32)           // Green cash collection indicator
    val CashGreenLight = Color(0xFFE8F5E9)      // Light green pill badge bg
    val DueRed = Color(0xFFC62828)              // Red outstanding-balance indicator
    val DueRedLight = Color(0xFFFFEBEE)         // Light red pill badge bg
    val AmberOrange = Color(0xFFEF6C00)         // Credit sales indicator
    val AmberOrangeLight = Color(0xFFFFF3E0)    // Amber pill badge bg
    val TextPrimary = Color(0xFF1E293B)         // Slate 800
    val TextSecondary = Color(0xFF64748B)       // Slate 500
    val TableRowEven = Color(0xFFFFFFFF)
    val TableRowOdd = Color(0xFFF8FAFC)
}

object BanglaNumberUtil {
    private val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBangla(numberStr: String): String {
        var result = numberStr
        for (i in 0..9) {
            result = result.replace(englishDigits[i], banglaDigits[i])
        }
        return result
    }

    fun toBangla(number: Int): String {
        return toBangla(number.toString())
    }

    fun toBangla(number: Long): String {
        return toBangla(number.toString())
    }

    fun toBangla(number: Double, decimals: Int = 0): String {
        val formatted = if (decimals > 0) {
            String.format(Locale.ENGLISH, "%.${decimals}f", number)
        } else {
            if (number % 1.0 == 0.0) {
                number.toLong().toString()
            } else {
                String.format(Locale.ENGLISH, "%.2f", number)
            }
        }
        return toBangla(formatted)
    }

    fun formatCurrency(amount: Double): String {
        val formatted = if (amount % 1.0 == 0.0) {
            NumberFormat.getIntegerInstance(Locale.ENGLISH).format(amount.toLong())
        } else {
            String.format(Locale.ENGLISH, "%.2f", amount)
        }
        return "₹${formatted}"
    }

    fun formatCurrencyBn(amount: Double): String {
        val formatted = if (amount % 1.0 == 0.0) {
            NumberFormat.getIntegerInstance(Locale.ENGLISH).format(amount.toLong())
        } else {
            String.format(Locale.ENGLISH, "%.2f", amount)
        }
        return "₹" + toBangla(formatted)
    }

    fun formatDateBn(dateMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return toBangla(sdf.format(Date(dateMillis)))
    }
}
