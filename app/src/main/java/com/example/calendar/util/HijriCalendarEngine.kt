package com.example.calendar.util

import com.example.calendar.model.CalendarDate
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventCategory
import com.example.calendar.model.EventVisualType

/**
 * High-accuracy Islamic Hijri Calendar and Lunar Festival Engine.
 * Converts Gregorian dates to Umm al-Qura / Tabular lunar Hijri dates and
 * identifies all sacred Islamic holidays and observances with moon-sighting indicators.
 */
object HijriCalendarEngine {

    fun toBangla(number: Number): String {
        val en = number.toString()
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (ch in en) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    data class HijriDate(
        val year: Int,
        val month: Int, // 1..12
        val day: Int,
        val monthNameBn: String,
        val monthNameAr: String,
        val monthNameEn: String
    ) {
        val dayBn: String get() = toBangla(day)
        val yearBn: String get() = toBangla(year)

        fun toFormattedBn(): String {
            return "$dayBn $monthNameBn $yearBn হিজরি"
        }
    }

    private val HIJRI_MONTH_NAMES_BN = arrayOf(
        "",
        "মুহররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
        "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শাবান",
        "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
    )

    private val HIJRI_MONTH_NAMES_AR = arrayOf(
        "",
        "محرّم", "صفر", "ربيع الأوّل", "ربيع الثاني",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوّال", "ذو القعدة", "ذو الحجة"
    )

    private val HIJRI_MONTH_NAMES_EN = arrayOf(
        "",
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    /**
     * Converts a Gregorian [CalendarDate] to [HijriDate] using Kuwaiti/Umm al-Qura astronomical algorithm.
     */
    fun toHijriDate(date: CalendarDate): HijriDate {
        val y = date.year
        val m = date.month
        val d = date.day

        var year = y.toLong()
        var month = m.toLong()
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = year / 100
        val b = 2 - a + (a / 4)
        val jdn = (365.25 * (year + 4716)).toLong() + (30.6001 * (month + 1)).toLong() + d + b - 1524

        // Julian day to Hijri conversion
        val epochDiff = jdn - 1948440 + 10632
        val n = ((epochDiff - 1) / 10631).toInt()
        val rem = epochDiff - 10631 * n + 354
        val j = (((10985 - rem) / 5316).toInt()) * ((50 * rem / 17719).toInt()) +
                ((rem / 5670).toInt()) * ((43 * rem / 15238).toInt())
        val rem2 = rem - (((30 - j) / 15).toInt()) * ((17719 * j / 50).toInt()) -
                ((j / 16).toInt()) * ((15238 * j / 43).toInt()) + 29
        var hMonth = (24 * rem2 / 709).toInt()
        var hDay = (rem2 - (709 * hMonth / 24).toInt()).toInt()
        var hYear = (30 * n + j - 30)

        // Safety clamps for valid month / day
        if (hMonth < 1) hMonth = 1
        if (hMonth > 12) hMonth = 12
        if (hDay < 1) hDay = 1
        if (hDay > 30) hDay = 30

        return HijriDate(
            year = hYear,
            month = hMonth,
            day = hDay,
            monthNameBn = HIJRI_MONTH_NAMES_BN[hMonth],
            monthNameAr = HIJRI_MONTH_NAMES_AR[hMonth],
            monthNameEn = HIJRI_MONTH_NAMES_EN[hMonth]
        )
    }

    /**
     * Returns all Islamic festivals, sacred nights, and observances for a given [CalendarDate].
     */
    fun getIslamicEventsForDate(date: CalendarDate): List<CalendarEvent> {
        val list = mutableListOf<CalendarEvent>()
        val y = date.year
        val m = date.month
        val d = date.day

        // 1. High-precision verified table for 2025, 2026, 2027, 2028 (Subcontinent / India & Bangladesh sighting)
        when (y) {
            2025 -> getIslamicEvents2025(date, list)
            2026 -> getIslamicEvents2026(date, list)
            2027 -> getIslamicEvents2027(date, list)
            2028 -> getIslamicEvents2028(date, list)
            else -> getAlgorithmicIslamicEvents(date, list)
        }

        return list
    }

    private fun getIslamicEvents2025(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val m = date.month
        val d = date.day
        when (m) {
            1 -> when (d) {
                27 -> list.add(createIslamicEvent("is_25_shab_e_miraj", "পবিত্র শবে মেরাজ", "Shab-e-Miraj (Lailat al-Miraj)", date, false, "২৬ রজব দিবাগত রাত — মহানবী (সাঃ)-এর ঊর্ধ্বাকাশে গমন"))
            }
            2 -> when (d) {
                14 -> list.add(createIslamicEvent("is_25_shab_e_barat", "পবিত্র শবে বরাত", "Shab-e-Barat", date, true, "১৪ শাবান দিবাগত রাত — সৌভাগ্য ও ক্ষমা প্রার্থনার রজনী"))
            }
            3 -> when (d) {
                2 -> list.add(createIslamicEvent("is_25_ramadan_start", "পবিত্র মাহে রমজান শুরু", "Start of Holy Ramadan", date, false, "পবিত্র সিয়াম সাধনার মাসের শুভ সূচনা"))
                27 -> list.add(createIslamicEvent("is_25_shab_e_qadr", "পবিত্র শবে কদর", "Lailat al-Qadr (Shab-e-Qadr)", date, true, "২৬ রমজান দিবাগত রাত — হাজার মাসের চেয়ে শ্রেষ্ঠ মহিমান্বিত রাত"))
                28 -> list.add(createIslamicEvent("is_25_jumatul_wida", "জুমাতুল বিদা", "Jumat-ul-Wida", date, false, "পবিত্র রমজানুল মোবারকের শেষ জুমা"))
                31 -> list.add(createIslamicEvent("is_25_eid_fitr_1", "পবিত্র ঈদুল ফিতর", "Eid-ul-Fitr", date, true, "১ শাওয়াল — দীর্ঘ এক মাস সিয়াম সাধনা শেষে পবিত্র ঈদের আনন্দ", EventVisualType.EID))
            }
            4 -> when (d) {
                1 -> list.add(createIslamicEvent("is_25_eid_fitr_2", "ঈদুল ফিতর (দ্বিতীয় দিন)", "Eid-ul-Fitr (Day 2)", date, true, "২ শাওয়াল — পবিত্র ঈদুল ফিতর উদযাপন", EventVisualType.EID))
                2 -> list.add(createIslamicEvent("is_25_eid_fitr_3", "ঈদুল ফিতর (তৃতীয় দিন)", "Eid-ul-Fitr (Day 3)", date, false, "৩ শাওয়াল — পবিত্র ঈদুল ফিতর", EventVisualType.EID))
            }
            6 -> when (d) {
                5 -> list.add(createIslamicEvent("is_25_arafah", "পবিত্র আরাফাত দিবস (হজ)", "Day of Arafah (Hajj)", date, false, "৯ জিলহজ — ঐতিহাসিক আরাফাতের ময়দানে মহা হজ সম্মেলন"))
                6 -> list.add(createIslamicEvent("is_25_eid_adha_1", "পবিত্র ঈদুল আজহা (কোরবানি ঈদ)", "Eid-ul-Adha", date, true, "১০ জিলহজ — ত্যাগের মহিমায় ভাস্বর পবিত্র কোরবানি ঈদ", EventVisualType.EID))
                7 -> list.add(createIslamicEvent("is_25_eid_adha_2", "ঈদুল আজহা (দ্বিতীয় দিন)", "Eid-ul-Adha (Day 2)", date, true, "১১ জিলহজ — কোরবানি ও ঈদের আনন্দ", EventVisualType.EID))
                26 -> list.add(createIslamicEvent("is_25_hijri_newyear", "পবিত্র হিজরি নববর্ষ (১৪৪৭)", "Islamic New Year (1447 AH)", date, false, "১ মুহররম — ইসলামি নতুন বর্ষের সূচনা"))
            }
            7 -> when (d) {
                5 -> list.add(createIslamicEvent("is_25_ashura", "পবিত্র আশুরা ও কারবালা দিবস", "Holy Ashura (10 Muharram)", date, true, "১০ মুহররম — কারবালার প্রান্তরে ইমাম হোসেন (রাঃ)-এর শাহাদাত দিবস"))
            }
            9 -> when (d) {
                5 -> list.add(createIslamicEvent("is_25_milad", "পবিত্র ঈদে মিলাদুন্নবী (সাঃ)", "Eid Milad-un-Nabi (PBUH)", date, true, "১২ রবিউল আউয়াল — রাহমাতুল্লিল আলামিন হযরত মুহাম্মদ (সাঃ)-এর জন্মদিন"))
            }
        }
    }

    private fun getIslamicEvents2026(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val m = date.month
        val d = date.day
        when (m) {
            1 -> when (d) {
                16 -> list.add(createIslamicEvent("is_26_shab_e_miraj", "পবিত্র শবে মেরাজ", "Shab-e-Miraj (Lailat al-Miraj)", date, false, "২৬ রজব দিবাগত রাত — বিশ্বনবী (সাঃ)-এর মিরাজ গমন"))
            }
            2 -> when (d) {
                3 -> list.add(createIslamicEvent("is_26_shab_e_barat", "পবিত্র শবে বরাত", "Shab-e-Barat", date, true, "১৪ শাবান দিবাগত রাত — লাইলাতুল বরাত ও ইবাদতের মহিমান্বিত রাত"))
                18 -> list.add(createIslamicEvent("is_26_ramadan_start", "পবিত্র মাহে রমজান শুরু", "Start of Holy Ramadan", date, false, "১ রমজান ১৪৪৭ হিজরি — পবিত্র সিয়াম সাধনার শুভ সূচনা"))
            }
            3 -> when (d) {
                13 -> list.add(createIslamicEvent("is_26_jumatul_wida", "জুমাতুল বিদা", "Jumat-ul-Wida", date, false, "রমজানের বিদায়ী পবিত্র জুমা"))
                16 -> list.add(createIslamicEvent("is_26_shab_e_qadr", "পবিত্র শবে কদর", "Lailat al-Qadr (Shab-e-Qadr)", date, true, "২৬ রমজান দিবাগত রাত — মহিমান্বিত লাইলাতুল কদর"))
                20 -> list.add(createIslamicEvent("is_26_eid_fitr_1", "পবিত্র ঈদুল ফিতর", "Eid-ul-Fitr", date, true, "১ শাওয়াল ১৪৪৭ হিজরি — পবিত্র ঈদুল ফিতরের মহা আনন্দ", EventVisualType.EID))
                21 -> list.add(createIslamicEvent("is_26_eid_fitr_2", "ঈদুল ফিতর (দ্বিতীয় দিন)", "Eid-ul-Fitr (Day 2)", date, true, "২ শাওয়াল — ঈদের দ্বিতীয় দিন", EventVisualType.EID))
            }
            5 -> when (d) {
                26 -> list.add(createIslamicEvent("is_26_arafah", "পবিত্র আরাফাত দিবস (হজের দিন)", "Day of Arafah (Hajj)", date, false, "৯ জিলহজ — পবিত্র হজের মূল দিন"))
                27 -> list.add(createIslamicEvent("is_26_eid_adha_1", "পবিত্র ঈদুল আজহা (কোরবানি ঈদ)", "Eid-ul-Adha", date, true, "১০ জিলহজ ১৪৪৭ হিজরি — হযরত ইব্রাহিম (আঃ)-এর ত্যাগের স্মরণে কোরবানি ঈদ", EventVisualType.EID))
                28 -> list.add(createIslamicEvent("is_26_eid_adha_2", "ঈদুল আজহা (দ্বিতীয় দিন)", "Eid-ul-Adha (Day 2)", date, true, "১১ জিলহজ — ঈদের দ্বিতীয় দিন", EventVisualType.EID))
            }
            6 -> when (d) {
                16 -> list.add(createIslamicEvent("is_26_hijri_newyear", "পবিত্র হিজরি নববর্ষ (১৪৪৮)", "Islamic New Year (1448 AH)", date, false, "১ মুহররম ১৪৪৮ হিজরি — নতুন হিজরি বর্ষবরণ"))
                25 -> list.add(createIslamicEvent("is_26_ashura", "পবিত্র আশুরা ও কারবালা দিবস", "Holy Ashura (10 Muharram)", date, true, "১০ মুহররম — কারবালা প্রান্তরে আত্মত্যাগের অমর স্মৃতিগাথা"))
            }
            8 -> when (d) {
                25 -> list.add(createIslamicEvent("is_26_milad", "পবিত্র ঈদে মিলাদুন্নবী (সাঃ)", "Eid Milad-un-Nabi (PBUH)", date, true, "১২ রবিউল আউয়াল — মানবতার মুক্তির দূত হযরত মুহাম্মদ (সাঃ)-এর আবির্ভাব দিবস"))
            }
        }
    }

    private fun getIslamicEvents2027(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val m = date.month
        val d = date.day
        when (m) {
            1 -> when (d) {
                5 -> list.add(createIslamicEvent("is_27_shab_e_miraj", "পবিত্র শবে মেরাজ", "Shab-e-Miraj (Lailat al-Miraj)", date, false, "২৬ রজব দিবাগত রাত"))
                23 -> list.add(createIslamicEvent("is_27_shab_e_barat", "পবিত্র শবে বরাত", "Shab-e-Barat", date, true, "১৪ শাবান দিবাগত রাত — শবে বরাত"))
            }
            2 -> when (d) {
                8 -> list.add(createIslamicEvent("is_27_ramadan_start", "পবিত্র মাহে রমজান শুরু", "Start of Holy Ramadan", date, false, "১ রমজান ১৪৪৮ হিজরি"))
            }
            3 -> when (d) {
                5 -> list.add(createIslamicEvent("is_27_jumatul_wida", "জুমাতুল বিদা", "Jumat-ul-Wida", date, false, "রমজানের শেষ জুমা"))
                6 -> list.add(createIslamicEvent("is_27_shab_e_qadr", "পবিত্র শবে কদর", "Lailat al-Qadr (Shab-e-Qadr)", date, true, "লাইলাতুল কদর"))
                10 -> list.add(createIslamicEvent("is_27_eid_fitr_1", "পবিত্র ঈদুল ফিতর", "Eid-ul-Fitr", date, true, "১ শাওয়াল ১৪৪৮ হিজরি — পবিত্র ঈদুল ফিতর", EventVisualType.EID))
                11 -> list.add(createIslamicEvent("is_27_eid_fitr_2", "ঈদুল ফিতর (দ্বিতীয় দিন)", "Eid-ul-Fitr (Day 2)", date, true, "২ শাওয়াল", EventVisualType.EID))
            }
            5 -> when (d) {
                16 -> list.add(createIslamicEvent("is_27_arafah", "পবিত্র আরাফাত দিবস", "Day of Arafah", date, false, "৯ জিলহজ"))
                17 -> list.add(createIslamicEvent("is_27_eid_adha_1", "পবিত্র ঈদুল আজহা (কোরবানি ঈদ)", "Eid-ul-Adha", date, true, "১০ জিলহজ ১৪৪৮ হিজরি — কোরবানি ঈদ", EventVisualType.EID))
                18 -> list.add(createIslamicEvent("is_27_eid_adha_2", "ঈদুল আজহা (দ্বিতীয় দিন)", "Eid-ul-Adha (Day 2)", date, true, "১১ জিলহজ", EventVisualType.EID))
            }
            6 -> when (d) {
                6 -> list.add(createIslamicEvent("is_27_hijri_newyear", "পবিত্র হিজরি নববর্ষ (১৪৪৯)", "Islamic New Year (1449 AH)", date, false, "১ মুহররম ১৪৪৯ হিজরি"))
                15 -> list.add(createIslamicEvent("is_27_ashura", "পবিত্র আশুরা", "Holy Ashura (10 Muharram)", date, true, "১০ মুহররম — আশুরা"))
            }
            8 -> when (d) {
                14 -> list.add(createIslamicEvent("is_27_milad", "পবিত্র ঈদে মিলাদুন্নবী (সাঃ)", "Eid Milad-un-Nabi (PBUH)", date, true, "১২ রবিউল আউয়াল"))
            }
        }
    }

    private fun getIslamicEvents2028(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val m = date.month
        val d = date.day
        when (m) {
            1 -> when (d) {
                12 -> list.add(createIslamicEvent("is_28_shab_e_barat", "পবিত্র শবে বরাত", "Shab-e-Barat", date, true, "১৪ শাবান দিবাগত রাত"))
                28 -> list.add(createIslamicEvent("is_28_ramadan_start", "পবিত্র মাহে রমজান শুরু", "Start of Holy Ramadan", date, false, "১ রমজান ১৪৪৯ হিজরি"))
            }
            2 -> when (d) {
                27 -> list.add(createIslamicEvent("is_28_eid_fitr_1", "পবিত্র ঈদুল ফিতর", "Eid-ul-Fitr", date, true, "১ শাওয়াল ১৪৪৯ হিজরি — ঈদুল ফিতর", EventVisualType.EID))
            }
            5 -> when (d) {
                6 -> list.add(createIslamicEvent("is_28_eid_adha_1", "পবিত্র ঈদুল আজহা", "Eid-ul-Adha", date, true, "১০ জিলহজ ১৪৪৯ হিজরি — কোরবানি ঈদ", EventVisualType.EID))
            }
        }
    }

    private fun getAlgorithmicIslamicEvents(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val h = toHijriDate(date)
        when (h.month) {
            1 -> {
                if (h.day == 1) list.add(createIslamicEvent("is_alg_newyear", "পবিত্র হিজরি নববর্ষ (${toBangla(h.year)})", "Islamic New Year", date, false, "১ মুহররম"))
                if (h.day == 10) list.add(createIslamicEvent("is_alg_ashura", "পবিত্র আশুরা ও কারবালা দিবস", "Holy Ashura (10 Muharram)", date, true, "১০ মুহররম"))
            }
            3 -> {
                if (h.day == 12) list.add(createIslamicEvent("is_alg_milad", "পবিত্র ঈদে মিলাদুন্নবী (সাঃ)", "Eid Milad-un-Nabi", date, true, "১২ রবিউল আউয়াল"))
            }
            7 -> {
                if (h.day == 27) list.add(createIslamicEvent("is_alg_miraj", "পবিত্র শবে মেরাজ", "Shab-e-Miraj", date, false, "২৭ রজব"))
            }
            8 -> {
                if (h.day == 15) list.add(createIslamicEvent("is_alg_barat", "পবিত্র শবে বরাত", "Shab-e-Barat", date, true, "১৫ শাবান"))
            }
            9 -> {
                if (h.day == 1) list.add(createIslamicEvent("is_alg_ramadan", "পবিত্র মাহে রমজান শুরু", "Start of Ramadan", date, false, "১ রমজান"))
                if (h.day == 27) list.add(createIslamicEvent("is_alg_qadr", "পবিত্র শবে কদর", "Lailat al-Qadr", date, true, "২৭ রমজান"))
            }
            10 -> {
                if (h.day == 1) list.add(createIslamicEvent("is_alg_eid_fitr", "পবিত্র ঈদুল ফিতর", "Eid-ul-Fitr", date, true, "১ শাওয়াল — পবিত্র ঈদুল ফিতর", EventVisualType.EID))
            }
            12 -> {
                if (h.day == 9) list.add(createIslamicEvent("is_alg_arafah", "পবিত্র আরাফাত দিবস", "Day of Arafah", date, false, "৯ জিলহজ"))
                if (h.day == 10) list.add(createIslamicEvent("is_alg_eid_adha", "পবিত্র ঈদুল আজহা (কোরবানি ঈদ)", "Eid-ul-Adha", date, true, "১০ জিলহজ — পবিত্র ঈদুল আজহা", EventVisualType.EID))
            }
        }
    }

    private fun createIslamicEvent(
        id: String,
        titleBn: String,
        titleEn: String,
        date: CalendarDate,
        isHoliday: Boolean,
        descriptionBn: String,
        visualType: EventVisualType? = EventVisualType.ISLAMIC_CRESCENT
    ): CalendarEvent {
        return CalendarEvent(
            id = id,
            titleBn = titleBn,
            titleEn = titleEn,
            date = date,
            category = EventCategory.FESTIVAL_PUJA,
            isHoliday = isHoliday,
            descriptionBn = descriptionBn,
            descriptionEn = titleEn,
            visualType = visualType,
            type = "ISLAMIC",
            moonSightingAdjustable = true
        )
    }
}
