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

    @Test
    fun testEventDrawableMapping() {
        // 1. Dr. APJ Abdul Kalam on 15 October
        val oct15Events = SpecialDaysRepository.getSpecialDayEventsForDate(CalendarDate(2026, 10, 15), context)
        val kalamEvent = oct15Events.find { it.titleBn.contains("কালাম") || it.id.contains("10-15") }
        assertNotNull("Kalam event should exist on Oct 15", kalamEvent)
        assertEquals(com.example.R.drawable.photo_dr_apj_abdul_kalam, kalamEvent!!.resolveDrawableRes())

        // 2. Netaji on 23 January
        val jan23Events = SpecialDaysRepository.getSpecialDayEventsForDate(CalendarDate(2026, 1, 23), context)
        val netajiEvent = jan23Events.find { it.titleBn.contains("নেতাজি") }
        assertNotNull("Netaji event should exist", netajiEvent)
        assertEquals(com.example.R.drawable.photo_netaji, netajiEvent!!.resolveDrawableRes())

        // 3. Gandhi on 2 October
        val oct2Events = SpecialDaysRepository.getSpecialDayEventsForDate(CalendarDate(2026, 10, 2), context)
        val gandhiEvent = oct2Events.find { it.titleBn.contains("গান্ধী") }
        assertNotNull("Gandhi event should exist", gandhiEvent)
        assertEquals(com.example.R.drawable.photo_gandhi, gandhiEvent!!.resolveDrawableRes())

        // 4. Lakshmi Puja in 2026 (25 October 2026)
        val laxmiEvents = HinduPanchangEngine.getHinduFestivalsForDate(CalendarDate(2026, 10, 25))
        val laxmiEvent = laxmiEvents.find { it.titleBn.contains("লক্ষ্মী") }
        assertNotNull("Lakshmi puja should exist on Oct 25, 2026", laxmiEvent)
        assertEquals(com.example.R.drawable.photo_laxmi, laxmiEvent!!.resolveDrawableRes())

        // 5. Kali Puja in 2026 (8 November 2026)
        val kaliEvents = HinduPanchangEngine.getHinduFestivalsForDate(CalendarDate(2026, 11, 8))
        val kaliEvent = kaliEvents.find { it.titleBn.contains("কালী") }
        assertNotNull("Kali puja should exist on Nov 8, 2026", kaliEvent)
        assertEquals(com.example.R.drawable.photo_kali, kaliEvent!!.resolveDrawableRes())

        // 6. Rabindranath Tagore on 8 May
        val tagoreEvents = CalendarEventsRepository.getRegionalObservances(CalendarDate(2026, 5, 8))
        val tagoreEvent = tagoreEvents.find { it.titleBn.contains("রবীন্দ্রনাথ") }
        assertNotNull("Tagore event should exist on May 8", tagoreEvent)
        assertEquals(com.example.R.drawable.photo_tagore, tagoreEvent!!.resolveDrawableRes())
    }

    @Test
    fun testVisualFallbackEngineForRemainingFestivals() {
        // Holi
        val holiEvent = HinduPanchangEngine.getHinduFestivalsForDate(CalendarDate(2026, 3, 4))
            .find { it.titleBn.contains("হোলি") }
        assertNotNull("Holi event should exist", holiEvent)
        assertEquals(com.example.calendar.model.EventVisualType.HOLI, holiEvent!!.visualType)
        val holiFallback = com.example.calendar.util.EventVisualFallbackEngine.getFallback(holiEvent)
        assertEquals("🎨", holiFallback.symbol)
        assertTrue(holiFallback.gradientColors.isNotEmpty())

        // Ramadan
        val ramadanEvent = HijriCalendarEngine.getIslamicEventsForDate(CalendarDate(2026, 2, 18))
            .find { it.titleBn.contains("রমজান") }
        assertNotNull("Ramadan event should exist", ramadanEvent)
        assertEquals(com.example.calendar.model.EventVisualType.RAMADAN, ramadanEvent!!.visualType)
        val ramadanFallback = com.example.calendar.util.EventVisualFallbackEngine.getFallback(ramadanEvent)
        assertEquals("🌙", ramadanFallback.symbol)

        // Eid-ul-Fitr
        val eidEvent = HijriCalendarEngine.getIslamicEventsForDate(CalendarDate(2026, 3, 20))
            .find { it.titleBn.contains("ঈদুল ফিতর") }
        assertNotNull("Eid event should exist", eidEvent)
        assertEquals(com.example.calendar.model.EventVisualType.EID_UL_FITR, eidEvent!!.visualType)
        val eidFallback = com.example.calendar.util.EventVisualFallbackEngine.getFallback(eidEvent)
        assertEquals("🕌", eidFallback.symbol)

        // Ekadashi
        val ekadashiEvent = com.example.calendar.model.CalendarEvent(
            id = "test_ekadashi",
            titleBn = "একাদশী ব্রত",
            titleEn = "Ekadashi Vrata",
            date = CalendarDate(2026, 3, 1),
            category = com.example.calendar.model.EventCategory.FESTIVAL_PUJA,
            isHoliday = false,
            visualType = com.example.calendar.model.EventVisualType.EKADASHI
        )
        val ekadashiFallback = com.example.calendar.util.EventVisualFallbackEngine.getFallback(ekadashiEvent)
        assertEquals("🌿", ekadashiFallback.symbol)

        // Nehru on Nov 14
        val nov14Events = SpecialDaysRepository.getSpecialDayEventsForDate(CalendarDate(2026, 11, 14), context)
        val nehruEvent = nov14Events.find { it.titleBn.contains("নেহরু") }
        assertNotNull("Nehru event should exist on Nov 14", nehruEvent)
        assertEquals(com.example.calendar.model.EventVisualType.NEHRU, nehruEvent!!.visualType)
        val nehruFallback = com.example.calendar.util.EventVisualFallbackEngine.getFallback(nehruEvent)
        assertEquals("🌹", nehruFallback.symbol)

        // Abul Kalam Azad on Nov 11
        val nov11Events = SpecialDaysRepository.getSpecialDayEventsForDate(CalendarDate(2026, 11, 11), context)
        val azadEvent = nov11Events.find { it.titleBn.contains("আজাদ") }
        assertNotNull("Azad event should exist on Nov 11", azadEvent)
        assertEquals(com.example.calendar.model.EventVisualType.ABUL_KALAM_AZAD, azadEvent!!.visualType)
        val azadFallback = com.example.calendar.util.EventVisualFallbackEngine.getFallback(azadEvent)
        assertEquals("📖", azadFallback.symbol)
    }

    @Test
    fun testDynamicVisualAssetMapper() {
        // Test existing asset
        val laxmiAsset = com.example.calendar.util.EventDrawableMapper.getEventVisualAsset(context, "photo_laxmi", "HINDU")
        assertEquals(com.example.R.drawable.photo_laxmi, laxmiAsset)

        // Test non-uploaded asset falls back gracefully to EventVectorFallback
        val holiAsset = com.example.calendar.util.EventDrawableMapper.getEventVisualAsset(context, "photo_holi", "HINDU")
        assertTrue(holiAsset is com.example.calendar.util.EventVectorFallback)
        assertEquals("🎨", (holiAsset as com.example.calendar.util.EventVectorFallback).symbol)
    }
}
