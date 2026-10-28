package com.example.calendar.util

import android.content.Context
import com.example.calendar.model.CalendarDate
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventCategory
import com.example.calendar.model.EventVisualType
import com.example.data.AppDatabase
import com.example.model.FestivalEntity
import com.example.model.UserBirthday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Central Festival & Calendar Events Repository.
 * Integrates dual-calendar engines:
 *  - Hindu Lunisolar & Tithi Engine (type: "HINDU")
 *  - Islamic Hijri Lunar Engine (type: "ISLAMIC")
 *  - National & Public Holidays (type: "NATIONAL")
 *  - Room Database persistent custom festivals and user birthdays
 */
class CalendarEventsRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val birthdayDao = db.userBirthdayDao()
    private val festivalDao = db.festivalDao()

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
     * Returns all events, Hindu pujas/tithis, Islamic holidays, national days,
     * and user birthdays for a specific [CalendarDate].
     */
    suspend fun getEventsForDate(date: CalendarDate): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CalendarEvent>()

        // 1. Hindu / Panchang Lunisolar Festivals & Tithis
        list.addAll(HinduPanchangEngine.getHinduFestivalsForDate(date))

        // 2. Islamic / Hijri Lunar Festivals & Observances
        list.addAll(HijriCalendarEngine.getIslamicEventsForDate(date))

        // 3. National Days & Government Holidays
        list.addAll(getNationalAndPublicHolidays(date))

        // 4. Room Database Custom Festivals
        try {
            val dbFestivals = festivalDao.getFestivalsForDate(date.year, date.month, date.day)
            for (f in dbFestivals) {
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

        // 5. User custom birthdays
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
                    type = "NATIONAL",
                    visualType = EventVisualType.BIRTHDAY_CAKE
                )
            )
        }

        list
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
            list.addAll(getNationalAndPublicHolidays(date))
            return list
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
         * National days, public holidays, and notable historical birthdays.
         */
        fun getNationalAndPublicHolidays(date: CalendarDate): List<CalendarEvent> {
            val list = mutableListOf<CalendarEvent>()
            val m = date.month
            val d = date.day

            when (m) {
                1 -> when (d) {
                    1 -> list.add(createNationalEvent("h_jan_1", "ইংরেজি নববর্ষ", "New Year's Day", date, true, "ইংরেজি বছরের প্রথম দিন", null))
                    12 -> list.add(createNationalEvent("h_jan_12", "স্বামী বিবেকানন্দের জন্মজয়ন্তী / জাতীয় যুব দিবস", "Swami Vivekananda Birthday / National Youth Day", date, true, "স্বামী বিবেকানন্দের শুভ জন্মদিন", EventVisualType.VIVEKANANDA))
                    23 -> list.add(createNationalEvent("h_jan_23", "নেতাজি সুভাষচন্দ্র বসুর জন্মজয়ন্তী / পরাক্রম দিবস", "Netaji Subhas Chandra Bose Jayanti", date, true, "দেশনায়ক নেতাজির জন্মদিন", EventVisualType.NETAJI))
                    26 -> list.add(createNationalEvent("h_jan_26", "প্রজাতন্ত্র দিবস", "Republic Day", date, true, "ভারতের জাতীয় প্রজাতন্ত্র দিবস", null))
                    30 -> list.add(createNationalEvent("h_jan_30", "মহাত্মা গান্ধী শহীদ দিবস", "Martyrs' Day (Mahatma Gandhi)", date, false, "জাতির জনক মহাত্মা গান্ধীর প্রয়াণ দিবস", EventVisualType.GANDHI))
                }
                2 -> when (d) {
                    18 -> list.add(createNationalEvent("h_feb_18", "শ্রী রামকৃষ্ণ পরমহংসদেবের জন্মতিথি", "Sri Ramakrishna Paramahamsa Jayanti", date, false, "যুগাবতার শ্রী রামকৃষ্ণের আবির্ভাব তিথি", EventVisualType.VIVEKANANDA))
                    21 -> list.add(createNationalEvent("h_feb_21", "আন্তর্জাতিক মাতৃভাষা দিবস ও শহীদ দিবস", "International Mother Language Day", date, false, "ভাষা শহীদদের প্রতি বিনম্র শ্রদ্ধা", null))
                    28 -> list.add(createNationalEvent("h_feb_28", "জাতীয় বিজ্ঞান দিবস", "National Science Day", date, false, "বিজ্ঞানী সি. ভি. রমনের রমন এফেক্ট আবিষ্কারের স্মরণে", null))
                }
                3 -> when (d) {
                    8 -> list.add(createNationalEvent("h_mar_8", "আন্তর্জাতিক নারী দিবস", "International Women's Day", date, false, "বিশ্ব নারী অধিকার দিবস", null))
                }
                4 -> when (d) {
                    14 -> list.add(createNationalEvent("h_apr_14", "ড. বি আর আম্বেদকর জয়ন্তী", "Dr. B.R. Ambedkar Jayanti", date, true, "সংবিধান প্রণেতা বাবাসাহেবের জন্মদিন", EventVisualType.AMBEDKAR))
                    21 -> list.add(createNationalEvent("h_apr_21", "মহাবীর জয়ন্তী", "Mahavir Jayanti", date, true, "জৈন তীর্থঙ্কর ভগবান মহাবীরের জন্মদিন", null))
                }
                5 -> when (d) {
                    1 -> list.add(createNationalEvent("h_may_1", "আন্তর্জাতিক শ্রমিক দিবস / মে দিবস", "International Workers' Day (May Day)", date, true, "শ্রমিকদের অধিকার ও সংহতি দিবস", null))
                    8 -> list.add(createNationalEvent("h_may_8", "কবিগুরু রবীন্দ্রনাথ ঠাকুরের জন্মজয়ন্তী (২৫ বৈশাখ)", "Rabindranath Tagore Jayanti", date, true, "বিশ্বকবি রবীন্দ্রনাথ ঠাকুরের জন্মদিন", EventVisualType.RABINDRANATH))
                    23 -> list.add(createNationalEvent("h_may_23", "বুদ্ধ পূর্ণিমা", "Buddha Purnima", date, true, "গৌতম বুদ্ধের জন্ম ও নির্বাণ তিথি", EventVisualType.BUDDHA))
                    25 -> list.add(createNationalEvent("h_may_25", "জাতীয় কবি কাজী নজরুল ইসলামের জন্মজয়ন্তী (১১ জ্যৈষ্ঠ)", "Kazi Nazrul Islam Jayanti", date, false, "বিদ্রোহী কবির শুভ জন্মদিন", EventVisualType.NAZRUL))
                }
                6 -> when (d) {
                    5 -> list.add(createNationalEvent("h_jun_5", "বিশ্ব পরিবেশ দিবস", "World Environment Day", date, false, "পরিবেশ সংরক্ষণ ও বৃক্ষরোপণ দিবস", null))
                    21 -> list.add(createNationalEvent("h_jun_21", "আন্তর্জাতিক যোগ দিবস", "International Yoga Day", date, false, "সুস্বাস্থ্যের জন্য যোগচর্চা দিবস", null))
                }
                7 -> when (d) {
                    1 -> list.add(createNationalEvent("h_jul_1", "জাতীয় চিকিৎসক দিবস ও ড. বিধানচন্দ্র রায়ের জন্মদিবস", "National Doctors' Day", date, false, "চিকিৎসকদের প্রতি শ্রদ্ধা ও সম্মাননা", null))
                }
                8 -> when (d) {
                    15 -> list.add(createNationalEvent("h_aug_15", "স্বাধীনতা দিবস", "Independence Day", date, true, "ভারতের জাতীয় স্বাধীনতা দিবস", EventVisualType.NETAJI))
                    29 -> list.add(createNationalEvent("h_aug_29", "জাতীয় ক্রীড়া দিবস (ধ্যানচাঁদ জন্মজয়ন্তী)", "National Sports Day", date, false, "হকি জাদুকর ধ্যানচাঁদের জন্মদিন", null))
                }
                9 -> when (d) {
                    3 -> list.add(createNationalEvent("h_sep_3", "মহানায়ক উত্তম কুমারের জন্মজয়ন্তী", "Mahanayak Uttam Kumar Jayanti", date, false, "বাংলা চলচ্চিত্রের চিরসবুজ মহানায়ক", EventVisualType.UTTAM_KUMAR))
                    5 -> list.add(createNationalEvent("h_sep_5", "শিক্ষক দিবস (ড. সর্বপল্লী রাধাকৃষ্ণন জন্মজয়ন্তী)", "Teachers' Day (Dr. S. Radhakrishnan)", date, false, "শ্রদ্ধেয় শিক্ষকদের প্রতি সম্মাননা", EventVisualType.RADHAKRISHNAN))
                    26 -> list.add(createNationalEvent("h_sep_26", "ঈশ্বরচন্দ্র বিদ্যাসাগরের জন্মজয়ন্তী", "Ishwar Chandra Vidyasagar Jayanti", date, false, "সমাজ সংস্কারক বিদ্যাসাগরের জন্মদিন", EventVisualType.VIDYASAGAR))
                }
                10 -> when (d) {
                    2 -> list.add(createNationalEvent("h_oct_2", "গান্ধী জয়ন্তী ও লাল বাহাদুর শাস্ত্রী জন্মজয়ন্তী", "Gandhi Jayanti", date, true, "জাতির জনক মহাত্মা গান্ধীর জন্মদিন", EventVisualType.GANDHI))
                    15 -> list.add(createNationalEvent("h_oct_15", "ড. এ পি জে আবদুল কালাম জন্মজয়ন্তী / বিশ্ব ছাত্র দিবস", "Dr. A.P.J. Abdul Kalam Birthday", date, false, "মিসাইল ম্যান আবদুল কালামের জন্মদিন", null))
                }
                11 -> when (d) {
                    14 -> list.add(createNationalEvent("h_nov_14", "শিশু দিবস ও জওহরলাল নেহেরু জন্মজয়ন্তী", "Children's Day (Jawaharlal Nehru)", date, false, "চাচা নেহেরুর শুভ জন্মদিন", null))
                    15 -> list.add(createNationalEvent("h_nov_15", "গুরু নানক জয়ন্তী", "Guru Nanak Jayanti", date, true, "শিখ ধর্মের প্রবক্তা গুরু নানকের জন্মদিন", null))
                    26 -> list.add(createNationalEvent("h_nov_26", "সংবিধান দিবস", "Constitution Day (Samvidhan Divas)", date, false, "ভারতীয় সংবিধান গৃহীত হওয়ার ঐতিহাসিক দিন", null))
                }
                12 -> when (d) {
                    1 -> list.add(createNationalEvent("h_dec_1", "বিশ্ব এইডস দিবস", "World AIDS Day", date, false, "সচেতনতা দিবস", null))
                    16 -> list.add(createNationalEvent("h_dec_16", "বিজয় দিবস", "Vijay Diwas", date, false, "ঐতিহাসিক বিজয় দিবস", null))
                    25 -> list.add(createNationalEvent("h_dec_25", "বড়দিন (ক্রিসমাস ডে)", "Christmas Day", date, true, "যীশু খ্রিস্টের শুভ জন্মদিন", EventVisualType.CHRISTMAS))
                    31 -> list.add(createNationalEvent("h_dec_31", "বছরের শেষ দিন", "New Year's Eve", date, false, "ইংরেজি বছরের সমাপনী দিন", null))
                }
            }

            return list
        }

        fun getAnnualEventsForDate(date: CalendarDate): List<CalendarEvent> = getAllEventsForDateSync(date)

        private fun createNationalEvent(
            id: String,
            titleBn: String,
            titleEn: String,
            date: CalendarDate,
            isHoliday: Boolean,
            descriptionBn: String,
            visualType: EventVisualType?
        ): CalendarEvent {
            return CalendarEvent(
                id = id,
                titleBn = titleBn,
                titleEn = titleEn,
                date = date,
                category = if (isHoliday) EventCategory.GOVERNMENT_HOLIDAY else EventCategory.NATIONAL_DAY,
                isHoliday = isHoliday,
                descriptionBn = descriptionBn,
                descriptionEn = titleEn,
                visualType = visualType,
                type = "NATIONAL",
                moonSightingAdjustable = false
            )
        }
    }
}
