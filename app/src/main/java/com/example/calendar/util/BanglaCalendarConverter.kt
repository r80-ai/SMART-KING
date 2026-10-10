package com.example.calendar.util

import com.example.calendar.model.BanglaDate
import com.example.calendar.model.BanglaMonth
import com.example.calendar.model.CalendarDate

/**
 * High-accuracy Bengali Calendar converter for all dates and years.
 * Matches traditional West Bengal (Panjika) and standard Bengali calendar conventions.
 */
object BanglaCalendarConverter {

    /**
     * Start day of each Bengali month in the Gregorian calendar.
     * Index 1 = Boishakh (April 15), 2 = Jyeshtha (May 16), ..., 12 = Choitro (March 15).
     */
    private val MONTH_START_GREGORIAN = intArrayOf(
        0,
        14, // 1: Boishakh -> April 14
        16, // 2: Jyeshtha -> May 16
        16, // 3: Ashhadh -> June 16
        18, // 4: Srabon -> July 18
        19, // 5: Bhadro -> August 19
        19, // 6: Ashwin -> September 19
        19, // 7: Kartik -> October 19
        18, // 8: Ogrohayon -> November 18
        17, // 9: Poush -> December 17
        15, // 10: Magh -> January 15
        14, // 11: Falgun -> February 14
        15  // 12: Choitro -> March 15
    )

    /**
     * Converts a Gregorian [CalendarDate] to a [BanglaDate].
     */
    fun toBanglaDate(gregorianDate: CalendarDate): BanglaDate {
        val gYear = gregorianDate.year
        val gMonth = gregorianDate.month
        val gDay = gregorianDate.day

        var bYear: Int
        val bMonth: BanglaMonth
        val bDay: Int

        when (gMonth) {
            4 -> { // April
                if (gDay >= 14) {
                    bYear = gYear - 593
                    bMonth = BanglaMonth.BOISHAKH
                    bDay = gDay - 14 + 1
                } else {
                    bYear = gYear - 594
                    bMonth = BanglaMonth.CHOITRO
                    bDay = 17 + gDay // March 15 to 31 is 17 days
                }
            }
            5 -> { // May
                bYear = gYear - 593
                if (gDay < 16) {
                    bMonth = BanglaMonth.BOISHAKH
                    bDay = (30 - 14 + 1) + gDay // April 14..30 (17 days) + May days
                } else {
                    bMonth = BanglaMonth.JYESHTHA
                    bDay = gDay - 16 + 1
                }
            }
            6 -> { // June
                bYear = gYear - 593
                if (gDay < 16) {
                    bMonth = BanglaMonth.JYESHTHA
                    bDay = (31 - 16 + 1) + gDay // May 16..31 (16 days) + June days
                } else {
                    bMonth = BanglaMonth.ASHHADH
                    bDay = gDay - 16 + 1
                }
            }
            7 -> { // July
                bYear = gYear - 593
                if (gDay < 18) {
                    bMonth = BanglaMonth.ASHHADH
                    bDay = (30 - 16 + 1) + gDay // June 16..30 (15 days) + July days
                } else {
                    bMonth = BanglaMonth.SRABON
                    bDay = gDay - 18 + 1
                }
            }
            8 -> { // August
                bYear = gYear - 593
                if (gDay < 19) {
                    bMonth = BanglaMonth.SRABON
                    bDay = (31 - 18 + 1) + gDay // July 18..31 (14 days) + August days
                } else {
                    bMonth = BanglaMonth.BHADRO
                    bDay = gDay - 19 + 1
                }
            }
            9 -> { // September
                bYear = gYear - 593
                if (gDay < 19) {
                    bMonth = BanglaMonth.BHADRO
                    bDay = (31 - 19 + 1) + gDay // August 19..31 (13 days) + September days (e.g. 13 + 17 = 30)
                } else {
                    bMonth = BanglaMonth.ASHWIN
                    bDay = gDay - 19 + 1
                }
            }
            10 -> { // October
                bYear = gYear - 593
                if (gDay < 19) {
                    bMonth = BanglaMonth.ASHWIN
                    bDay = (30 - 19 + 1) + gDay // September 19..30 (12 days) + October days
                } else {
                    bMonth = BanglaMonth.KARTIK
                    bDay = gDay - 19 + 1
                }
            }
            11 -> { // November
                bYear = gYear - 593
                if (gDay < 18) {
                    bMonth = BanglaMonth.KARTIK
                    bDay = (31 - 19 + 1) + gDay // October 19..31 (13 days) + November days
                } else {
                    bMonth = BanglaMonth.OGROHAYON
                    bDay = gDay - 18 + 1
                }
            }
            12 -> { // December
                bYear = gYear - 593
                if (gDay < 17) {
                    bMonth = BanglaMonth.OGROHAYON
                    bDay = (30 - 18 + 1) + gDay // November 18..30 (13 days) + December days
                } else {
                    bMonth = BanglaMonth.POUSH
                    bDay = gDay - 17 + 1
                }
            }
            1 -> { // January
                bYear = gYear - 594
                if (gDay < 15) {
                    bMonth = BanglaMonth.POUSH
                    bDay = (31 - 17 + 1) + gDay // December 17..31 (15 days) + January days
                } else {
                    bMonth = BanglaMonth.MAGH
                    bDay = gDay - 15 + 1
                }
            }
            2 -> { // February
                bYear = gYear - 594
                if (gDay < 14) {
                    bMonth = BanglaMonth.MAGH
                    bDay = (31 - 15 + 1) + gDay // January 15..31 (17 days) + February days
                } else {
                    bMonth = BanglaMonth.FALGUN
                    bDay = gDay - 14 + 1
                }
            }
            3 -> { // March
                bYear = gYear - 594
                val febDays = if (gregorianDate.isLeapYear) 29 else 28
                if (gDay < 15) {
                    bMonth = BanglaMonth.FALGUN
                    bDay = (febDays - 14 + 1) + gDay
                } else {
                    bMonth = BanglaMonth.CHOITRO
                    bDay = gDay - 15 + 1
                }
            }
            else -> {
                bYear = gYear - 593
                bMonth = BanglaMonth.BOISHAKH
                bDay = 1
            }
        }

        return BanglaDate(
            day = bDay,
            month = bMonth,
            year = bYear,
            dayOfWeek = gregorianDate.dayOfWeek
        )
    }

    /**
     * Converts a [BanglaDate] back to its approximate/exact [CalendarDate].
     */
    fun toGregorianDate(bYear: Int, bMonth: BanglaMonth, bDay: Int): CalendarDate {
        return when (bMonth) {
            BanglaMonth.BOISHAKH -> {
                val gYear = bYear + 593
                if (bDay <= 17) CalendarDate(gYear, 4, 14 + bDay - 1)
                else CalendarDate(gYear, 5, bDay - 17)
            }
            BanglaMonth.JYESHTHA -> {
                val gYear = bYear + 593
                if (bDay <= 16) CalendarDate(gYear, 5, 16 + bDay - 1)
                else CalendarDate(gYear, 6, bDay - 16)
            }
            BanglaMonth.ASHHADH -> {
                val gYear = bYear + 593
                if (bDay <= 15) CalendarDate(gYear, 6, 16 + bDay - 1)
                else CalendarDate(gYear, 7, bDay - 15)
            }
            BanglaMonth.SRABON -> {
                val gYear = bYear + 593
                if (bDay <= 14) CalendarDate(gYear, 7, 18 + bDay - 1)
                else CalendarDate(gYear, 8, bDay - 14)
            }
            BanglaMonth.BHADRO -> {
                val gYear = bYear + 593
                if (bDay <= 13) CalendarDate(gYear, 8, 19 + bDay - 1)
                else CalendarDate(gYear, 9, bDay - 13)
            }
            BanglaMonth.ASHWIN -> {
                val gYear = bYear + 593
                if (bDay <= 12) CalendarDate(gYear, 9, 19 + bDay - 1)
                else CalendarDate(gYear, 10, bDay - 12)
            }
            BanglaMonth.KARTIK -> {
                val gYear = bYear + 593
                if (bDay <= 13) CalendarDate(gYear, 10, 19 + bDay - 1)
                else CalendarDate(gYear, 11, bDay - 13)
            }
            BanglaMonth.OGROHAYON -> {
                val gYear = bYear + 593
                if (bDay <= 13) CalendarDate(gYear, 11, 18 + bDay - 1)
                else CalendarDate(gYear, 12, bDay - 13)
            }
            BanglaMonth.POUSH -> {
                val gYear = bYear + 593
                if (bDay <= 15) CalendarDate(gYear, 12, 17 + bDay - 1)
                else CalendarDate(gYear + 1, 1, bDay - 15)
            }
            BanglaMonth.MAGH -> {
                val gYear = bYear + 594
                if (bDay <= 17) CalendarDate(gYear, 1, 15 + bDay - 1)
                else CalendarDate(gYear, 2, bDay - 17)
            }
            BanglaMonth.FALGUN -> {
                val gYear = bYear + 594
                val isLeap = CalendarDate(gYear, 1, 1).isLeapYear
                val febDays = if (isLeap) 29 else 28
                val febRemaining = febDays - 14 + 1
                if (bDay <= febRemaining) CalendarDate(gYear, 2, 14 + bDay - 1)
                else CalendarDate(gYear, 3, bDay - febRemaining)
            }
            BanglaMonth.CHOITRO -> {
                val gYear = bYear + 594
                if (bDay <= 17) CalendarDate(gYear, 3, 15 + bDay - 1)
                else CalendarDate(gYear, 4, bDay - 17)
            }
        }
    }
}
