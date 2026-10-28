package com.example.calendar.util

import androidx.compose.ui.graphics.Color
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventCategory
import com.example.calendar.model.EventVisualType

/**
 * Data model for themed Vector Graphic / Icon fallback badges.
 */
data class EventVectorFallback(
    val symbol: String,
    val gradientColors: List<Color>,
    val borderColor: Color,
    val textColor: Color = Color.White,
    val description: String = ""
)

/**
 * High-quality Vector Graphic and Icon Fallback Engine.
 * Provides authentic religious symbols, national emblems, and festival motifs
 * with themed radial gradients and accent borders when dedicated PNG photos
 * are not present in res/drawable/.
 */
object EventVisualFallbackEngine {

    fun getFallback(event: CalendarEvent): EventVectorFallback {
        // 1. By EventVisualType
        when (event.visualType) {
            // Hindu Festivals
            EventVisualType.MAKAR_SANKRANTI -> return EventVectorFallback(
                symbol = "🪁",
                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
                borderColor = Color(0xFFFDE047),
                description = "মকর সংক্রান্তি ঘুড়ি ও সূর্য পার্বণ"
            )
            EventVisualType.HOLI -> return EventVectorFallback(
                symbol = "🎨",
                gradientColors = listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
                borderColor = Color(0xFFF472B6),
                description = "দোলযাত্রা ও আবীর উৎসব"
            )
            EventVisualType.RAM_NAVAMI -> return EventVectorFallback(
                symbol = "🏹",
                gradientColors = listOf(Color(0xFFEA580C), Color(0xFF9A3412)),
                borderColor = Color(0xFFFBBF24),
                description = "শ্রীরামের ধনুর্ধর প্রতীক"
            )
            EventVisualType.AKSHAYA_TRITIYA -> return EventVectorFallback(
                symbol = "🪙",
                gradientColors = listOf(Color(0xFFFBBF24), Color(0xFFB45309)),
                borderColor = Color(0xFFFEF08A),
                description = "শুভ অক্ষয় তৃতীয়া স্বর্ণ ও সমৃদ্ধি"
            )
            EventVisualType.BHAI_PHOTA -> return EventVectorFallback(
                symbol = "🪔",
                gradientColors = listOf(Color(0xFFDC2626), Color(0xFF991B1B)),
                borderColor = Color(0xFFFCA5A5),
                description = "ভ্রাতৃদ্বিতীয়া প্রদীপ ও চন্দনের ফোঁটা"
            )
            EventVisualType.CHHATH_PUJA -> return EventVectorFallback(
                symbol = "🌅",
                gradientColors = listOf(Color(0xFFF97316), Color(0xFFC2410C)),
                borderColor = Color(0xFFFED7AA),
                description = "ছট পূজা সূর্য অর্ঘ্য"
            )
            EventVisualType.KARTIK_PUJA -> return EventVectorFallback(
                symbol = "🦚",
                gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF0E7490)),
                borderColor = Color(0xFF67E8F9),
                description = "ময়ূরবাহন দেবসেনাপতি কার্তিক"
            )
            EventVisualType.EKADASHI -> return EventVectorFallback(
                symbol = "🌿",
                gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)),
                borderColor = Color(0xFF93C5FD),
                description = "পবিত্র একাদশী তুলসী ও শঙ্খ"
            )
            EventVisualType.MAHALAYA -> return EventVectorFallback(
                symbol = "🔱",
                gradientColors = listOf(Color(0xFF475569), Color(0xFF1E293B)),
                borderColor = Color(0xFFE2E8F0),
                description = "দেবীপক্ষের শুভ সূচনা ও তর্পণ"
            )

            // Muslim Festivals (Hijri Calendar)
            EventVisualType.RAMADAN -> return EventVectorFallback(
                symbol = "🌙",
                gradientColors = listOf(Color(0xFF059669), Color(0xFF064E3B)),
                borderColor = Color(0xFF34D399),
                description = "পবিত্র মাহে রমজান"
            )
            EventVisualType.EID_UL_FITR, EventVisualType.EID -> return EventVectorFallback(
                symbol = "🕌",
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857)),
                borderColor = Color(0xFF6EE7B7),
                description = "পবিত্র ঈদুল ফিতর"
            )
            EventVisualType.EID_UL_ADHA -> return EventVectorFallback(
                symbol = "🕋",
                gradientColors = listOf(Color(0xFF047857), Color(0xFF064E3B)),
                borderColor = Color(0xFFFBBF24),
                description = "পবিত্র ঈদুল আজহা কোরবানি"
            )
            EventVisualType.MUHARRAM -> return EventVectorFallback(
                symbol = "☪",
                gradientColors = listOf(Color(0xFF0F766E), Color(0xFF134E4A)),
                borderColor = Color(0xFF5EEAD4),
                description = "পবিত্র আশুরা ও মহররম"
            )
            EventVisualType.EID_MILADUNNABI -> return EventVectorFallback(
                symbol = "🕌",
                gradientColors = listOf(Color(0xFF16A34A), Color(0xFF14532D)),
                borderColor = Color(0xFF86EFAC),
                description = "পবিত্র ঈদে মিলাদুন্নবী"
            )
            EventVisualType.SHAB_E_BARAT -> return EventVectorFallback(
                symbol = "🏮",
                gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF064E3B)),
                borderColor = Color(0xFF67E8F9),
                description = "পবিত্র শবে বরাত"
            )

            // Sikh, Buddhist, Jain & Christian Festivals
            EventVisualType.GURU_NANAK -> return EventVectorFallback(
                symbol = "☬",
                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
                borderColor = Color(0xFFFDE68A),
                description = "পবিত্র খাণ্ডা ও গুরু নানক জয়ন্তী"
            )
            EventVisualType.BAISAKHI -> return EventVectorFallback(
                symbol = "🌾",
                gradientColors = listOf(Color(0xFFEAB308), Color(0xFF854D0E)),
                borderColor = Color(0xFFFEF08A),
                description = "বৈশাখী ফসল উৎসব"
            )
            EventVisualType.BUDDHA_PURNIMA, EventVisualType.BUDDHA -> return EventVectorFallback(
                symbol = "☸",
                gradientColors = listOf(Color(0xFFFBBF24), Color(0xFF92400E)),
                borderColor = Color(0xFFFDE047),
                description = "বুদ্ধ পূর্ণিমা ধর্মচক্র"
            )
            EventVisualType.MAHAVIR_JAYANTI -> return EventVectorFallback(
                symbol = "✋",
                gradientColors = listOf(Color(0xFFEAB308), Color(0xFFA16207)),
                borderColor = Color(0xFFFEF9C3),
                description = "মহাবীর জয়ন্তী অহিংসা প্রতীক"
            )
            EventVisualType.GOOD_FRIDAY -> return EventVectorFallback(
                symbol = "✝",
                gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF4C1D95)),
                borderColor = Color(0xFFC4B5FD),
                description = "পবিত্র গুড ফ্রাইডে ক্রুশ"
            )
            EventVisualType.EASTER -> return EventVectorFallback(
                symbol = "🕊️",
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFD97706)),
                borderColor = Color(0xFFDDD6FE),
                description = "ইস্টার সানডে পুনরুত্থান"
            )
            EventVisualType.CHRISTMAS -> return EventVectorFallback(
                symbol = "🎄",
                gradientColors = listOf(Color(0xFFDC2626), Color(0xFF15803D)),
                borderColor = Color(0xFFFDE047),
                description = "বড়দিন ও ক্রিসমাস ট্রি"
            )

            // Major National Figures
            EventVisualType.NEHRU -> return EventVectorFallback(
                symbol = "🌹",
                gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF991B1B)),
                borderColor = Color(0xFF93C5FD),
                description = "জওহরলাল নেহরু ও লাল গোলাপ"
            )
            EventVisualType.SHASTRI -> return EventVectorFallback(
                symbol = "🇮🇳",
                gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF14532D)),
                borderColor = Color(0xFFFBBF24),
                description = "লালবাহাদুর শাস্ত্রী জয় জওয়ান জয় কিষাণ"
            )
            EventVisualType.ABUL_KALAM_AZAD -> return EventVectorFallback(
                symbol = "📖",
                gradientColors = listOf(Color(0xFF0F766E), Color(0xFF1E3A8A)),
                borderColor = Color(0xFF67E8F9),
                description = "মৌলানা আবুল কালাম আজাদ জাতীয় শিক্ষা"
            )
            EventVisualType.VAJPAYEE -> return EventVectorFallback(
                symbol = "🪷",
                gradientColors = listOf(Color(0xFFEA580C), Color(0xFF1E3A8A)),
                borderColor = Color(0xFFFED7AA),
                description = "অটল বিহারী বাজপেয়ী সুশাসন"
            )
            EventVisualType.RAMAKRISHNA -> return EventVectorFallback(
                symbol = "🕉️",
                gradientColors = listOf(Color(0xFFEA580C), Color(0xFF7C2D12)),
                borderColor = Color(0xFFFDE047),
                description = "শ্রী শ্রী রামকৃষ্ণ পরমহংসদেব"
            )
            EventVisualType.BIRTHDAY_CAKE -> return EventVectorFallback(
                symbol = "🎂",
                gradientColors = listOf(Color(0xFF9333EA), Color(0xFF581C87)),
                borderColor = Color(0xFFD8B4FE),
                description = "জন্মদিন"
            )
            EventVisualType.HINDU_DIYA -> return EventVectorFallback(
                symbol = "🪔",
                gradientColors = listOf(Color(0xFFEA580C), Color(0xFF7C2D12)),
                borderColor = Color(0xFFFBBF24),
                description = "সনাতন প্রদীপ"
            )
            EventVisualType.ISLAMIC_CRESCENT -> return EventVectorFallback(
                symbol = "☪",
                gradientColors = listOf(Color(0xFF059669), Color(0xFF064E3B)),
                borderColor = Color(0xFF34D399),
                description = "ইসলামিক চাঁদ-তারা"
            )
            else -> {}
        }

        // 2. Keyword fallback on titleBn / titleEn / id
        val text = "${event.id} ${event.titleBn} ${event.titleEn}"
        if (text.contains("মকর") || text.contains("পৌষ সংক্রান্তি")) {
            return EventVectorFallback("🪁", listOf(Color(0xFFF59E0B), Color(0xFFB45309)), Color(0xFFFDE047), description = "মকর সংক্রান্তি")
        }
        if (text.contains("হোলি") || text.contains("দোলযাত্রা")) {
            return EventVectorFallback("🎨", listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)), Color(0xFFF472B6), description = "হোলি উৎসব")
        }
        if (text.contains("রাম নবমী")) {
            return EventVectorFallback("🏹", listOf(Color(0xFFEA580C), Color(0xFF9A3412)), Color(0xFFFBBF24), description = "রাম নবমী")
        }
        if (text.contains("অক্ষয়")) {
            return EventVectorFallback("🪙", listOf(Color(0xFFFBBF24), Color(0xFFB45309)), Color(0xFFFEF08A), description = "অক্ষয় তৃতীয়া")
        }
        if (text.contains("ভাইফোঁটা") || text.contains("ভ্রাতৃদ্বিতীয়া")) {
            return EventVectorFallback("🪔", listOf(Color(0xFFDC2626), Color(0xFF991B1B)), Color(0xFFFCA5A5), description = "ভাইফোঁটা")
        }
        if (text.contains("ছট পূজা") || text.contains("ছট্ পূজা")) {
            return EventVectorFallback("🌅", listOf(Color(0xFFF97316), Color(0xFFC2410C)), Color(0xFFFED7AA), description = "ছট পূজা")
        }
        if (text.contains("কার্তিক পূজা")) {
            return EventVectorFallback("🦚", listOf(Color(0xFF06B6D4), Color(0xFF0E7490)), Color(0xFF67E8F9), description = "কার্তিক পূজা")
        }
        if (text.contains("একাদশী")) {
            return EventVectorFallback("🌿", listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)), Color(0xFF93C5FD), description = "একাদশী ব্রত")
        }
        if (text.contains("মহালয়া")) {
            return EventVectorFallback("🔱", listOf(Color(0xFF475569), Color(0xFF1E293B)), Color(0xFFE2E8F0), description = "মহালয়া")
        }
        if (text.contains("রমজান")) {
            return EventVectorFallback("🌙", listOf(Color(0xFF059669), Color(0xFF064E3B)), Color(0xFF34D399), description = "মাহে রমজান")
        }
        if (text.contains("ঈদুল ফিতর")) {
            return EventVectorFallback("🕌", listOf(Color(0xFF10B981), Color(0xFF047857)), Color(0xFF6EE7B7), description = "ঈদুল ফিতর")
        }
        if (text.contains("ঈদুল আজহা") || text.contains("কোরবানি")) {
            return EventVectorFallback("🕋", listOf(Color(0xFF047857), Color(0xFF064E3B)), Color(0xFFFBBF24), description = "ঈদুল আজহা")
        }
        if (text.contains("মুহররম") || text.contains("আশুরা")) {
            return EventVectorFallback("☪", listOf(Color(0xFF0F766E), Color(0xFF134E4A)), Color(0xFF5EEAD4), description = "পবিত্র আশুরা")
        }
        if (text.contains("মিলাদুন্নবী")) {
            return EventVectorFallback("🕌", listOf(Color(0xFF16A34A), Color(0xFF14532D)), Color(0xFF86EFAC), description = "ঈদে মিলাদুন্নবী")
        }
        if (text.contains("শবে বরাত")) {
            return EventVectorFallback("🏮", listOf(Color(0xFF1E3A8A), Color(0xFF064E3B)), Color(0xFF67E8F9), description = "শবে বরাত")
        }
        if (text.contains("গুরু নানক")) {
            return EventVectorFallback("☬", listOf(Color(0xFFF59E0B), Color(0xFFB45309)), Color(0xFFFDE68A), description = "গুরু নানক জয়ন্তী")
        }
        if (text.contains("বৈশাখী")) {
            return EventVectorFallback("🌾", listOf(Color(0xFFEAB308), Color(0xFF854D0E)), Color(0xFFFEF08A), description = "বৈশাখী উৎসব")
        }
        if (text.contains("বুদ্ধ")) {
            return EventVectorFallback("☸", listOf(Color(0xFFFBBF24), Color(0xFF92400E)), Color(0xFFFDE047), description = "বুদ্ধ পূর্ণিমা")
        }
        if (text.contains("মহাবীর")) {
            return EventVectorFallback("✋", listOf(Color(0xFFEAB308), Color(0xFFA16207)), Color(0xFFFEF9C3), description = "মহাবীর জয়ন্তী")
        }
        if (text.contains("গুড ফ্রাইডে")) {
            return EventVectorFallback("✝", listOf(Color(0xFF7C3AED), Color(0xFF4C1D95)), Color(0xFFC4B5FD), description = "গুড ফ্রাইডে")
        }
        if (text.contains("ইস্টার")) {
            return EventVectorFallback("🕊️", listOf(Color(0xFF8B5CF6), Color(0xFFD97706)), Color(0xFFDDD6FE), description = "ইস্টার সানডে")
        }
        if (text.contains("বড়দিন") || text.contains("ক্রিসমাস")) {
            return EventVectorFallback("🎄", listOf(Color(0xFFDC2626), Color(0xFF15803D)), Color(0xFFFDE047), description = "বড়দিন")
        }
        if (text.contains("নেহরু")) {
            return EventVectorFallback("🌹", listOf(Color(0xFF1E3A8A), Color(0xFF991B1B)), Color(0xFF93C5FD), description = "নেহরু জয়ন্তী")
        }
        if (text.contains("শাস্ত্রী")) {
            return EventVectorFallback("🇮🇳", listOf(Color(0xFF1E3A8A), Color(0xFF14532D)), Color(0xFFFBBF24), description = "শাস্ত্রী জয়ন্তী")
        }
        if (text.contains("আজাদ")) {
            return EventVectorFallback("📖", listOf(Color(0xFF0F766E), Color(0xFF1E3A8A)), Color(0xFF67E8F9), description = "মৌলানা আজাদ জয়ন্তী")
        }
        if (text.contains("বাজপেয়ী")) {
            return EventVectorFallback("🪷", listOf(Color(0xFFEA580C), Color(0xFF1E3A8A)), Color(0xFFFED7AA), description = "বাজপেয়ী জয়ন্তী")
        }
        if (text.contains("রামকৃষ্ণ")) {
            return EventVectorFallback("🕉️", listOf(Color(0xFFEA580C), Color(0xFF7C2D12)), Color(0xFFFDE047), description = "শ্রীরামকৃষ্ণ জন্মতিথি")
        }

        // 3. Category Fallback
        return getFallbackByCategory(event.type, event.category)
    }

    fun getFallbackByCategory(type: String, category: EventCategory = EventCategory.OBSERVANCE): EventVectorFallback {
        return when (type) {
            "ISLAMIC" -> EventVectorFallback(
                symbol = "☪",
                gradientColors = listOf(Color(0xFF059669), Color(0xFF064E3B)),
                borderColor = Color(0xFF34D399),
                description = "ইসলামিক পর্ব"
            )
            "HINDU" -> EventVectorFallback(
                symbol = "ॐ",
                gradientColors = listOf(Color(0xFFEA580C), Color(0xFF7C2D12)),
                borderColor = Color(0xFFFBBF24),
                description = "সনাতন পর্ব"
            )
            "BIRTHDAY" -> EventVectorFallback(
                symbol = "🎂",
                gradientColors = listOf(Color(0xFF9333EA), Color(0xFF581C87)),
                borderColor = Color(0xFFD8B4FE),
                description = "জন্মদিন"
            )
            else -> when (category) {
                EventCategory.NATIONAL_DAY -> EventVectorFallback(
                    symbol = "🇮🇳",
                    gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)),
                    borderColor = Color(0xFF38BDF8),
                    description = "জাতীয় দিবস"
                )
                EventCategory.GOVERNMENT_HOLIDAY -> EventVectorFallback(
                    symbol = "🏛️",
                    gradientColors = listOf(Color(0xFFDC2626), Color(0xFF7F1D1D)),
                    borderColor = Color(0xFFF87171),
                    description = "সরকারি ছুটি"
                )
                EventCategory.FESTIVAL_PUJA -> EventVectorFallback(
                    symbol = "🪔",
                    gradientColors = listOf(Color(0xFFD97706), Color(0xFF78350F)),
                    borderColor = Color(0xFFFCD34D),
                    description = "উৎসব"
                )
                else -> EventVectorFallback(
                    symbol = "🌐",
                    gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)),
                    borderColor = Color(0xFF93C5FD),
                    description = "বিশেষ দিবস"
                )
            }
        }
    }
}
