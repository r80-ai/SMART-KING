package com.example.calendar.model

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
    CUSTOM_PHOTO
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
    val moonSightingAdjustable: Boolean = false // ±1 day indicator
)
