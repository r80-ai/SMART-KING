package com.example.calendar.model

/**
 * Representation of a date in the Bengali Calendar (বঙ্গাব্দ).
 */
data class BanglaDate(
    val day: Int,
    val month: BanglaMonth,
    val year: Int,
    val dayOfWeek: Int // 1 = Sunday (রবিবার) ... 7 = Saturday (শনিবার)
) {
    val dayNameBn: String
        get() = getBanglaDayName(dayOfWeek)

    val dayNameEn: String
        get() = getEnglishDayName(dayOfWeek)

    val monthNameBn: String
        get() = month.nameBn

    val monthNameEn: String
        get() = month.nameEn

    val dayBn: String
        get() = toBanglaDigits(day)

    val yearBn: String
        get() = toBanglaDigits(year)

    /**
     * E.g. "৩০ ভাদ্র ১৪৩৩"
     */
    fun formatShort(): String = "$dayBn $monthNameBn $yearBn"

    /**
     * E.g. "৩০ ভাদ্র ১৪৩৩ বঙ্গাব্দ"
     */
    fun formatFull(): String = "$dayBn $monthNameBn $yearBn বঙ্গাব্দ"

    /**
     * Formats corresponding Gregorian date in Bengali,
     * e.g. "বৃহস্পতিবার, ১৭ সেপ্টেম্বর"
     */
    fun formatGregorianInBangla(gregorianDate: CalendarDate): String {
        val gregDayBn = toBanglaDigits(gregorianDate.day)
        val gregMonthBn = getGregorianMonthNameBn(gregorianDate.month)
        return "$dayNameBn, $gregDayBn $gregMonthBn"
    }

    /**
     * Formats Gregorian date with year in Bengali,
     * e.g. "বৃহস্পতিবার, ১৭ সেপ্টেম্বর ২০২৬"
     */
    fun formatGregorianFullInBangla(gregorianDate: CalendarDate): String {
        val gregDayBn = toBanglaDigits(gregorianDate.day)
        val gregMonthBn = getGregorianMonthNameBn(gregorianDate.month)
        val gregYearBn = toBanglaDigits(gregorianDate.year)
        return "$dayNameBn, $gregDayBn $gregMonthBn $gregYearBn"
    }

    companion object {
        fun toBanglaDigits(number: Int): String = toBanglaDigits(number.toString())

        fun toBanglaDigits(text: String): String {
            val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            val sb = java.lang.StringBuilder()
            for (ch in text) {
                if (ch in '0'..'9') {
                    sb.append(banglaDigits[ch - '0'])
                } else {
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        fun getBanglaDayName(dayOfWeek: Int): String = when (dayOfWeek) {
            1 -> "রবিবার"
            2 -> "সোমবার"
            3 -> "মঙ্গলবার"
            4 -> "বুধবার"
            5 -> "বৃহস্পতিবার"
            6 -> "শুক্রবার"
            7 -> "শনিবার"
            else -> "রবিবার"
        }

        fun getBanglaDayShortName(dayOfWeek: Int): String = when (dayOfWeek) {
            1 -> "রবি"
            2 -> "সোম"
            3 -> "মঙ্গল"
            4 -> "বুধ"
            5 -> "বৃহস্পতি"
            6 -> "শুক্র"
            7 -> "শনি"
            else -> "রবি"
        }

        fun getEnglishDayName(dayOfWeek: Int): String = when (dayOfWeek) {
            1 -> "Sunday"
            2 -> "Monday"
            3 -> "Tuesday"
            4 -> "Wednesday"
            5 -> "Thursday"
            6 -> "Friday"
            7 -> "Saturday"
            else -> "Sunday"
        }

        fun getEnglishDayShortName(dayOfWeek: Int): String = when (dayOfWeek) {
            1 -> "Sun"
            2 -> "Mon"
            3 -> "Tue"
            4 -> "Wed"
            5 -> "Thu"
            6 -> "Fri"
            7 -> "Sat"
            else -> "Sun"
        }

        fun getGregorianMonthNameBn(month: Int): String = when (month) {
            1 -> "জানুয়ারি"
            2 -> "ফেব্রুয়ারি"
            3 -> "মার্চ"
            4 -> "এপ্রিল"
            5 -> "মে"
            6 -> "জুন"
            7 -> "জুলাই"
            8 -> "আগস্ট"
            9 -> "সেপ্টেম্বর"
            10 -> "অক্টোবর"
            11 -> "নভেম্বর"
            12 -> "ডিসেম্বর"
            else -> ""
        }

        fun getGregorianMonthNameEn(month: Int): String = when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> ""
        }
    }
}
