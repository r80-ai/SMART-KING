package com.example.calendar.model

import com.example.calendar.util.EventDrawableMapper

enum class EventCategory(
    val nameBn: String,
    val nameEn: String
) {
    GOVERNMENT_HOLIDAY("সরকারি ছুটি", "Government Holiday"),
    SCHOOL_HOLIDAY("বিদ্যালয় ছুটি", "School Holiday"),
    FESTIVAL_PUJA("উৎসব ও পূজা", "Festival / Puja"),
    NATIONAL_DAY("জাতীয় দিবস", "National Day"),
    OBSERVANCE("বিশেষ দিবস", "Observance"),
    BIRTHDAY("জন্মদিন", "Birthday")
}

enum class EventVisualType {
    KRISHNA,
    RADHAKRISHNAN,
    GANESHA,
    VISHWAKARMA,
    UTTAM_KUMAR,
    VIVEKANANDA,
    NETAJI,
    RABINDRANATH,
    NAZRUL,
    VIDYASAGAR,
    SARASWATI,
    DURGA,
    KALI,
    LAXMI,
    APJ_ABDUL_KALAM,
    SHIVA,
    JAGANNATH,
    BUDDHA,
    GANDHI,
    AMBEDKAR,
    CHRISTMAS,
    EID,
    ISLAMIC_CRESCENT,
    HINDU_DIYA,
    TITHI_PURNIMA,
    TITHI_AMAVASYA,
    TITHI_SHUKLA,
    TITHI_KRISHNA,
    BIRTHDAY_CAKE,
    CUSTOM_PHOTO,

    // Hindu Festivals
    MAKAR_SANKRANTI,
    HOLI,
    RAM_NAVAMI,
    AKSHAYA_TRITIYA,
    BHAI_PHOTA,
    CHHATH_PUJA,
    KARTIK_PUJA,
    EKADASHI,
    MAHALAYA,

    // Muslim Festivals (Hijri Calendar)
    RAMADAN,
    EID_UL_FITR,
    EID_UL_ADHA,
    MUHARRAM,
    EID_MILADUNNABI,
    SHAB_E_BARAT,

    // Sikh, Buddhist, Jain & Christian Festivals
    GURU_NANAK,
    BAISAKHI,
    BUDDHA_PURNIMA,
    MAHAVIR_JAYANTI,
    GOOD_FRIDAY,
    EASTER,

    // Major National Figures
    NEHRU,
    SHASTRI,
    ABUL_KALAM_AZAD,
    VAJPAYEE,
    RAMAKRISHNA
}

data class CalendarEvent(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val date: CalendarDate,
    val category: EventCategory,
    val isHoliday: Boolean = true,
    val descriptionBn: String? = null,
    val descriptionEn: String? = null,
    val visualType: EventVisualType? = null,
    val photoUrl: String? = null,
    val type: String = "NATIONAL", // "HINDU" | "ISLAMIC" | "NATIONAL"
    val moonSightingAdjustable: Boolean = false, // ±1 day indicator
    val imageResId: Int? = null,
    val imageDrawableName: String? = null
) {
    /**
     * Resolves the drawable resource ID for this event if an authentic photo exists.
     * Supports optional Context for runtime resource identification.
     */
    fun resolveDrawableRes(context: android.content.Context? = null): Int? {
        if (imageResId != null && imageResId != 0) return imageResId
        return EventDrawableMapper.getDrawableResForEvent(this, context)
    }
}
