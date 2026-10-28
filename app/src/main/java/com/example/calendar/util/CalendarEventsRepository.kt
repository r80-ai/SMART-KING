package com.example.calendar.util

import android.content.Context
import com.example.calendar.model.CalendarDate
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventCategory
import com.example.calendar.model.EventVisualType
import com.example.data.AppDatabase
import com.example.model.FestivalEntity
import com.example.model.UserBirthday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central Festival & Calendar Events Repository.
 * Integrates:
 *  - Complete Date-by-Date Special Days Dataset (146 dates from special_days.json)
 *  - Hindu Lunisolar & Tithi Engine (type: "HINDU") with shifting religious festivals
 *  - Islamic Hijri Lunar Engine (type: "ISLAMIC")
 *  - Room Database pre-populated festivals and custom events
 *  - User custom birthdays
 */
class CalendarEventsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val birthdayDao = db.userBirthdayDao()
    private val festivalDao = db.festivalDao()

    init {
        SpecialDaysRepository.ensureLoaded(appContext)
        // Pre-populate Room SQLite database in background
        CoroutineScope(Dispatchers.IO).launch {
            SpecialDaysRepository.prePopulateRoomDatabase(db, appContext)
        }
    }

    fun getAllUserBirthdays(): Flow<List<UserBirthday>> = birthdayDao.getAllBirthdays()

    suspend fun addUserBirthday(birthday: UserBirthday): Long = withContext(Dispatchers.IO) {
        birthdayDao.insertBirthday(birthday)
    }

    suspend fun deleteUserBirthday(birthday: UserBirthday) = withContext(Dispatchers.IO) {
        birthdayDao.deleteBirthday(birthday)
    }

    suspend fun addCustomFestival(festival: FestivalEntity) = withContext(Dispatchers.IO) {
        festivalDao.insertFestival(festival)
    }

    suspend fun getFestivalsByType(type: String): List<FestivalEntity> = withContext(Dispatchers.IO) {
        festivalDao.getFestivalsByType(type)
    }

    /**
     * Query events by day-month key ("MM-dd" or "dd-MM").
     */
    fun getEventsByDayMonthKey(key: String, date: CalendarDate = CalendarDate.today()): List<CalendarEvent> {
        return SpecialDaysRepository.getEventsByDayMonthKey(key, date, appContext)
    }

    /**
     * Returns all events, Hindu pujas/tithis, Islamic holidays, 146 special days,
     * and user birthdays for a specific [CalendarDate].
     */
    suspend fun getEventsForDate(date: CalendarDate): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CalendarEvent>()

        // 1. Hindu / Panchang Lunisolar Festivals & Tithis
        list.addAll(HinduPanchangEngine.getHinduFestivalsForDate(date))

        // 2. Islamic / Hijri Lunar Festivals & Observances
        list.addAll(HijriCalendarEngine.getIslamicEventsForDate(date))

        // 3. Fixed Gregorian Special Days (146 dates from expanded dataset)
        list.addAll(SpecialDaysRepository.getSpecialDayEventsForDate(date, appContext))

        // 4. Cultural / Regional Observances (e.g. Rabindranath Tagore, Nazrul Islam, Uttam Kumar, Vidyasagar)
        list.addAll(getRegionalObservances(date))

        // 5. Room Database Custom Festivals
        try {
            val dbFestivals = festivalDao.getFestivalsForDate(date.year, date.month, date.day)
            for (f in dbFestivals) {
                if (f.id.startsWith("fest_sd_")) {
                    // Already loaded from SpecialDaysRepository above
                    continue
                }
                val vType = try {
                    if (f.visualType != null) EventVisualType.valueOf(f.visualType) else null
                } catch (_: Exception) { null }

                list.add(
                    CalendarEvent(
                        id = f.id,
                        titleBn = f.titleBn,
                        titleEn = f.titleEn,
                        date = date,
                        category = if (f.isHoliday) EventCategory.GOVERNMENT_HOLIDAY else EventCategory.FESTIVAL_PUJA,
                        isHoliday = f.isHoliday,
                        descriptionBn = f.descriptionBn,
                        descriptionEn = f.descriptionEn,
                        visualType = vType,
                        type = f.type,
                        moonSightingAdjustable = f.moonSightingAdjustable
                    )
                )
            }
        } catch (_: Exception) {}

        // 6. User custom birthdays
        val userBdays = birthdayDao.getBirthdaysForDate(date.month, date.day)
        for (b in userBdays) {
            val ageStr = if (b.birthYear != null && date.year >= b.birthYear) {
                val age = date.year - b.birthYear
                " ($age বছর)"
            } else ""

            list.add(
                CalendarEvent(
                    id = "user_bday_${b.id}",
                    titleBn = "আজ ${b.name}-এর জন্মদিন$ageStr",
                    titleEn = "Today is ${b.name}'s Birthday",
                    date = date,
                    category = EventCategory.BIRTHDAY,
                    isHoliday = false,
                    descriptionBn = b.note ?: "জন্মদিন উদযাপন",
                    descriptionEn = b.note ?: "Birthday celebration",
                    type = "BIRTHDAY",
                    visualType = EventVisualType.BIRTHDAY_CAKE
                )
            )
        }

        // Deduplicate while preserving multiple distinct events on the same date
        val seen = mutableSetOf<String>()
        val result = mutableListOf<CalendarEvent>()
        for (ev in list) {
            val normKey = ev.titleBn.trim().replace("\\s+".toRegex(), " ")
            if (seen.add(normKey)) {
                result.add(ev)
            }
        }
        result
    }

    /**
     * Returns all events for a given Gregorian month.
     */
    suspend fun getEventsForMonth(year: Int, month: Int): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val daysInMonth = CalendarDate(year, month, 1).daysInMonth
        val allEvents = mutableListOf<CalendarEvent>()
        for (d in 1..daysInMonth) {
            allEvents.addAll(getEventsForDate(CalendarDate(year, month, d)))
        }
        allEvents
    }

    companion object {

        /**
         * Fast synchronous lookup of all festivals and events for widget and background tasks.
         */
        fun getAllEventsForDateSync(date: CalendarDate): List<CalendarEvent> {
            val list = mutableListOf<CalendarEvent>()
            list.addAll(HinduPanchangEngine.getHinduFestivalsForDate(date))
            list.addAll(HijriCalendarEngine.getIslamicEventsForDate(date))
            list.addAll(SpecialDaysRepository.getSpecialDayEventsForDate(date))
            list.addAll(getRegionalObservances(date))

            val seen = mutableSetOf<String>()
            val result = mutableListOf<CalendarEvent>()
            for (ev in list) {
                val normKey = ev.titleBn.trim().replace("\\s+".toRegex(), " ")
                if (seen.add(normKey)) {
                    result.add(ev)
                }
            }
            return result
        }

        /**
         * Computes astronomical Tithi event for widgets and legacy callers.
         */
        fun calculateTithi(date: CalendarDate): CalendarEvent? {
            val tithi = HinduPanchangEngine.calculateTithi(date)
            return when (tithi.index) {
                15 -> CalendarEvent(
                    id = "tithi_purnima_${date.toFormattedString()}",
                    titleBn = "পূর্ণিমা",
                    titleEn = "Purnima (Full Moon)",
                    date = date,
                    category = EventCategory.FESTIVAL_PUJA,
                    isHoliday = false,
                    descriptionBn = "শ্রী সত্যনারায়ণ পূজা ও পূর্ণিমা স্নান",
                    descriptionEn = "Full Moon day and Satyanarayan Puja",
                    visualType = EventVisualType.TITHI_PURNIMA,
                    type = "HINDU"
                )
                30 -> CalendarEvent(
                    id = "tithi_amavasya_${date.toFormattedString()}",
                    titleBn = "অমাবস্যা",
                    titleEn = "Amavasya (New Moon)",
                    date = date,
                    category = EventCategory.FESTIVAL_PUJA,
                    isHoliday = true,
                    descriptionBn = "অমাবস্যা তিথি ও পিতৃ তর্পণ",
                    descriptionEn = "New Moon day and Tarpan",
                    visualType = EventVisualType.TITHI_AMAVASYA,
                    type = "HINDU"
                )
                else -> null
            }
        }

        /**
         * Cultural and regional icons celebration days.
         */
        fun getRegionalObservances(date: CalendarDate): List<CalendarEvent> {
            val list = mutableListOf<CalendarEvent>()
            val m = date.month
            val d = date.day
            when (m) {
                5 -> when (d) {
                    8, 9 -> list.add(CalendarEvent("h_may_8", "কবিগুরু রবীন্দ্রনাথ ঠাকুরের জন্মজয়ন্তী (২৫ বৈশাখ)", "Rabindranath Tagore Jayanti", date, EventCategory.NATIONAL_DAY, true, "বিশ্বকবি রবীন্দ্রনাথ ঠাকুরের শুভ জন্মদিন", "Rabindranath Tagore Jayanti", EventVisualType.RABINDRANATH, null, "NATIONAL", false))
                    25, 26 -> list.add(CalendarEvent("h_may_25", "জাতীয় কবি কাজী নজরুল ইসলামের জন্মজয়ন্তী (১১ জ্যৈষ্ঠ)", "Kazi Nazrul Islam Jayanti", date, EventCategory.OBSERVANCE, false, "বিদ্রোহী কবি কাজী নজরুল ইসলামের শুভ জন্মদিন", "Kazi Nazrul Islam Jayanti", EventVisualType.NAZRUL, null, "NATIONAL", false))
                }
                9 -> when (d) {
                    3 -> list.add(CalendarEvent("h_sep_3", "মহানায়ক উত্তম কুমারের জন্মজয়ন্তী", "Mahanayak Uttam Kumar Jayanti", date, EventCategory.OBSERVANCE, false, "বাংলা চলচ্চিত্রের মহানায়ক উত্তম কুমারের জন্মদিন", "Mahanayak Uttam Kumar Jayanti", EventVisualType.UTTAM_KUMAR, null, "NATIONAL", false))
                    26 -> list.add(CalendarEvent("h_sep_26", "ঈশ্বরচন্দ্র বিদ্যাসাগরের জন্মজয়ন্তী", "Ishwar Chandra Vidyasagar Jayanti", date, EventCategory.OBSERVANCE, false, "সমাজ সংস্কারক ঈশ্বরচন্দ্র বিদ্যাসাগরের জন্মদিন", "Ishwar Chandra Vidyasagar Jayanti", EventVisualType.VIDYASAGAR, null, "NATIONAL", false))
                }
            }
            return list
        }

        fun getAnnualEventsForDate(date: CalendarDate): List<CalendarEvent> = getAllEventsForDateSync(date)
    }
}
