package com.example.calendar.model

import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Universal date representation (Year, Month 1..12, Day 1..31).
 * Completely independent of Android API levels and desugaring.
 */
data class CalendarDate(
    val year: Int,
    val month: Int, // 1 = January, 12 = December
    val day: Int
) : Comparable<CalendarDate> {

    init {
        require(month in 1..12) { "Month must be between 1 and 12: $month" }
        require(day in 1..31) { "Day must be between 1 and 31: $day" }
    }

    /**
     * Day of week: 1 = Sunday, 2 = Monday, ..., 7 = Saturday
     */
    val dayOfWeek: Int
        get() {
            val cal = GregorianCalendar(year, month - 1, day)
            return cal.get(Calendar.DAY_OF_WEEK)
        }

    val isLeapYear: Boolean
        get() = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    val daysInMonth: Int
        get() = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear) 29 else 28
            else -> 30
        }

    val dayOfYear: Int
        get() {
            val cal = GregorianCalendar(year, month - 1, day)
            return cal.get(Calendar.DAY_OF_YEAR)
        }

    fun plusDays(days: Int): CalendarDate {
        if (days == 0) return this
        val cal = GregorianCalendar(year, month - 1, day)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return CalendarDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun minusDays(days: Int): CalendarDate = plusDays(-days)

    fun plusMonths(months: Int): CalendarDate {
        val cal = GregorianCalendar(year, month - 1, 1)
        cal.add(Calendar.MONTH, months)
        val newYear = cal.get(Calendar.YEAR)
        val newMonth = cal.get(Calendar.MONTH) + 1
        val maxDays = CalendarDate(newYear, newMonth, 1).daysInMonth
        return CalendarDate(newYear, newMonth, day.coerceAtMost(maxDays))
    }

    fun minusMonths(months: Int): CalendarDate = plusMonths(-months)

    fun plusYears(years: Int): CalendarDate {
        val newYear = year + years
        val maxDays = CalendarDate(newYear, month, 1).daysInMonth
        return CalendarDate(newYear, month, day.coerceAtMost(maxDays))
    }

    fun minusYears(years: Int): CalendarDate = plusYears(-years)

    fun daysBetween(other: CalendarDate): Long {
        val cal1 = GregorianCalendar(year, month - 1, day)
        val cal2 = GregorianCalendar(other.year, other.month - 1, other.day)
        val diffMs = cal2.timeInMillis - cal1.timeInMillis
        return diffMs / (1000L * 60 * 60 * 24)
    }

    override fun compareTo(other: CalendarDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toFormattedString(): String {
        return "%04d-%02d-%02d".format(year, month, day)
    }

    companion object {
        fun today(): CalendarDate {
            val cal = Calendar.getInstance()
            return CalendarDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun of(year: Int, month: Int, day: Int): CalendarDate = CalendarDate(year, month, day)
    }
}
