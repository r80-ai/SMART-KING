package com.example.calendar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.calendar.model.CalendarDate
import com.example.calendar.util.CalendarEventsRepository
import com.example.calendar.util.HinduPanchangEngine
import com.example.calendar.util.HijriCalendarEngine
import com.example.calendar.util.SpecialDaysRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpecialDaysRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        SpecialDaysRepository.ensureLoaded(context)
    }

    @Test
    fun testAll146SpecialDaysLoaded() {
        val allItems = SpecialDaysRepository.getAllItems(context)
        assertEquals(146, allItems.size)
    }

    @Test
    fun testQueryByMonthDayKey() {
        // 01-01 (January 1st): ইংরেজি নববর্ষ
        val jan1 = SpecialDaysRepository.getSpecialDaysByKey("01-01", context)
        assertFalse("01-01 should exist", jan1.isEmpty())
        assertEquals("ইংরেজি নববর্ষ", jan1[0].titleBn)
        assertTrue(jan1[0].whyBn.contains("বিশ্বজুড়ে"))

        // 01-26 (January 26th): প্রজাতন্ত্র দিবস
        val jan26 = SpecialDaysRepository.getSpecialDaysByKey("01-26", context)
        assertFalse("01-26 should exist", jan26.isEmpty())
        assertTrue(jan26[0].titleBn.contains("প্রজাতন্ত্র দিবস"))

        // 12-25 (December 25th): বড়দিন
        val dec25 = SpecialDaysRepository.getSpecialDaysByKey("12-25", context)
        assertFalse("12-25 should exist", dec25.isEmpty())
        assertTrue(dec25[0].titleBn.contains("বড়দিন"))

        // 08-15 (August 15th): স্বাধীনতা দিবস
        val aug15 = SpecialDaysRepository.getSpecialDaysByKey("08-15", context)
        assertFalse("08-15 should exist", aug15.isEmpty())
        assertTrue(aug15[0].titleBn.contains("স্বাধীনতা দিবস"))
    }

    @Test
    fun testCalendarEventsRepositoryReturnsSpecialDays() {
        val repo = CalendarEventsRepository(context)
        val dateJan23 = CalendarDate(2026, 1, 23)
        val events = CalendarEventsRepository.getAllEventsForDateSync(dateJan23)
        val netajiEvent = events.find { it.titleBn.contains("নেতাজি") }
        assertNotNull("Netaji Jayanti should be present on Jan 23", netajiEvent)
        assertTrue("Description should include whyBn", netajiEvent!!.descriptionBn!!.contains("স্বাধীনতা সংগ্রামী"))
    }

    @Test
    fun testShiftingReligiousFestivalsMapped() {
        // Ram Navami in 2026 (March 27, 2026)
        val ramNavamiDate = CalendarDate(2026, 3, 27)
        val ramEvents = HinduPanchangEngine.getHinduFestivalsForDate(ramNavamiDate)
        assertTrue("Ram Navami should be mapped on March 27, 2026", ramEvents.any { it.titleBn.contains("রাম নবমী") })

        // Eid-ul-Fitr in 2026 (March 20, 2026)
        val eidFitrDate = CalendarDate(2026, 3, 20)
        val eidEvents = HijriCalendarEngine.getIslamicEventsForDate(eidFitrDate)
        assertTrue("Eid-ul-Fitr should be mapped on March 20, 2026", eidEvents.any { it.titleBn.contains("ঈদুল ফিতর") })

        // Good Friday in 2026 (April 3, 2026)
        val goodFridayDate = CalendarDate(2026, 4, 3)
        val gfEvents = HinduPanchangEngine.getHinduFestivalsForDate(goodFridayDate)
        assertTrue("Good Friday should be mapped on April 3, 2026", gfEvents.any { it.titleBn.contains("গুড ফ্রাইডে") })
    }
}
