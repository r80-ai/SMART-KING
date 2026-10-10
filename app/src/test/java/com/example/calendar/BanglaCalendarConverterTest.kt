package com.example.calendar

import com.example.calendar.model.BanglaDate
import com.example.calendar.model.BanglaMonth
import com.example.calendar.model.CalendarDate
import com.example.calendar.util.BanglaCalendarConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BanglaCalendarConverterTest {

    @Test
    fun testPohelaBoishakhConversion() {
        // 14 April 2026 is Pohela Boishakh 1433
        val date = CalendarDate(2026, 4, 14)
        val banglaDate = BanglaCalendarConverter.toBanglaDate(date)

        assertEquals(1, banglaDate.day)
        assertEquals(BanglaMonth.BOISHAKH, banglaDate.month)
        assertEquals(1433, banglaDate.year)
        assertEquals("১ বৈশাখ ১৪৩৩", banglaDate.formatShort())
    }

    @Test
    fun testBhadroDateConversion() {
        // 17 September 2026 is 30 Bhadro 1433
        val date = CalendarDate(2026, 9, 17)
        val banglaDate = BanglaCalendarConverter.toBanglaDate(date)

        assertEquals(30, banglaDate.day)
        assertEquals(BanglaMonth.BHADRO, banglaDate.month)
        assertEquals(1433, banglaDate.year)
        assertEquals("৩০ ভাদ্র ১৪৩৩", banglaDate.formatShort())
    }

    @Test
    fun testBengaliDigitsConversion() {
        assertEquals("১", BanglaDate.toBanglaDigits(1))
        assertEquals("১৫", BanglaDate.toBanglaDigits(15))
        assertEquals("২০২৬", BanglaDate.toBanglaDigits(2026))
        assertEquals("১৪৩৩", BanglaDate.toBanglaDigits(1433))
    }

    @Test
    fun testDateNavigation() {
        val date = CalendarDate(2026, 9, 17)
        val nextDay = date.plusDays(1)
        assertEquals(18, nextDay.day)
        assertEquals(9, nextDay.month)

        val prevDay = date.minusDays(1)
        assertEquals(16, prevDay.day)

        val nextMonth = date.plusMonths(1)
        assertEquals(10, nextMonth.month)
        assertEquals(17, nextMonth.day)
    }
}
