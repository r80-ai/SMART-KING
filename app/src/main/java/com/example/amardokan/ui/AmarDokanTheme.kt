package com.example.amardokan.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Amar Dokan Color Palette & Design Tokens
 */
object AmarDokanColors {
    val PrimaryBlue = Color(0xFF1976D2)
    val PrimaryBlueDark = Color(0xFF0D47A1)
    val NavyDarkHeader = Color(0xFF0F172A)
    val NavyDarkHeaderLight = Color(0xFF1E293B)
    val ContentBg = Color(0xFFF8FAFC)
    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)

    val CardBg = Color(0xFFFFFFFF)
    val CardBackground = Color(0xFFFFFFFF)

    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)

    val CashGreen = Color(0xFF16A34A)
    val CashGreenLight = Color(0xFFDCFCE7)

    val AmberOrange = Color(0xFFD97706)
    val AmberOrangeLight = Color(0xFFFEF3C7)

    val DueRed = Color(0xFFDC2626)
    val DueRedLight = Color(0xFFFEE2E2)

    val PendingOrange = Color(0xFFEA580C)
    val PendingOrangeLight = Color(0xFFFFEDD5)

    val BorderLight = Color(0xFFE2E8F0)
    val BorderColor = Color(0xFFCBD5E1)
    val DividerColor = Color(0xFFE2E8F0)

    val Primary = Color(0xFF1976D2)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFDBEAFE)

    val AccentColor = Color(0xFF2563EB)
    val SuccessColor = Color(0xFF16A34A)
    val ErrorColor = Color(0xFFDC2626)
    val WarningColor = Color(0xFFD97706)

    val TableRowEven = Color(0xFFFFFFFF)
    val TableRowOdd = Color(0xFFF8FAFC)

    // High-Contrast Input Field & Form Tokens
    val InputText = Color(0xFF000000)            // Solid bold black (#000000)
    val InputPlaceholder = Color(0xFF333333)     // Clearly visible dark slate/black tint
    val InputLabel = Color(0xFF0F172A)           // Crisp dark navy/black label
    val InputBorder = Color(0xFF64748B)          // Crisp visible border
    val InputBorderFocused = Color(0xFF1976D2)   // Primary focus border
    val ChipUnselectedLabel = Color(0xFF0F172A)  // Dark and crisp unselected chip text
    val ChipUnselectedBorder = Color(0xFF64748B) // Strong unselected chip border
}

/**
 * Standard High-Contrast TextFieldColors for all Amar Dokan form inputs,
 * search bars, dialogs, and text fields.
 */
@Composable
fun amarDokanTextFieldColors(
    containerColor: Color = Color.White
): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF000000),
        unfocusedTextColor = Color(0xFF000000),
        disabledTextColor = Color(0xFF333333),
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        cursorColor = Color(0xFF000000),
        focusedBorderColor = AmarDokanColors.PrimaryBlue,
        unfocusedBorderColor = Color(0xFF64748B),
        disabledBorderColor = Color(0xFF94A3B8),
        focusedLabelColor = Color(0xFF0F172A),
        unfocusedLabelColor = Color(0xFF1E293B),
        disabledLabelColor = Color(0xFF333333),
        focusedPlaceholderColor = Color(0xFF333333),
        unfocusedPlaceholderColor = Color(0xFF444444),
        disabledPlaceholderColor = Color(0xFF555555),
        focusedLeadingIconColor = Color(0xFF0F172A),
        unfocusedLeadingIconColor = Color(0xFF1E293B),
        focusedTrailingIconColor = Color(0xFF0F172A),
        unfocusedTrailingIconColor = Color(0xFF1E293B),
        focusedSupportingTextColor = Color(0xFF333333),
        unfocusedSupportingTextColor = Color(0xFF444444)
    )
}

/**
 * Utility for formatting Bengali numerals, currencies, and dates.
 */
object BanglaNumberUtil {
    private val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val enDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    fun toBangla(number: Number): String {
        return toBangla(number.toString())
    }

    fun toBangla(str: String): String {
        val sb = java.lang.StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglish(str: String): String {
        val sb = java.lang.StringBuilder()
        for (ch in str) {
            val idx = bnDigits.indexOf(ch)
            if (idx >= 0) {
                sb.append(enDigits[idx])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatAmount(amount: Double): String {
        val formatter = DecimalFormat("#,##,##0.##")
        return toBangla(formatter.format(amount))
    }

    fun formatCurrency(amount: Double): String {
        return "₹${formatAmount(amount)}"
    }

    fun formatCurrencyBn(amount: Double): String {
        return "₹${formatAmount(amount)}"
    }

    fun formatDateBn(dateMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault())
        return toBangla(sdf.format(Date(dateMillis)))
    }
}

@Composable
fun AmarDokanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = AmarDokanColors.PrimaryBlue,
        onPrimary = Color.White,
        primaryContainer = AmarDokanColors.PrimaryContainer,
        onPrimaryContainer = Color(0xFF0D47A1),
        secondary = AmarDokanColors.NavyDarkHeader,
        onSecondary = Color.White,
        surface = Color.White,
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF1E293B),
        background = AmarDokanColors.Background,
        onBackground = Color(0xFF000000),
        outline = Color(0xFF64748B),
        outlineVariant = Color(0xFF94A3B8),
        error = AmarDokanColors.DueRed,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
