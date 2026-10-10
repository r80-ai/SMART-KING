package com.example.calendar.util

import android.content.Context
import com.example.calendar.model.CalendarDate
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventCategory
import com.example.calendar.model.EventVisualType
import com.example.data.AppDatabase
import com.example.model.UserBirthday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.GregorianCalendar

/**
 * Repository providing festivals, pujas, government holidays, national observances,
 * notable birthdays, astronomical tithis (Ekadashi, Purnima, Amavasya), and user birthdays.
 */
class CalendarEventsRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val birthdayDao = db.userBirthdayDao()

    fun getAllUserBirthdays(): Flow<List<UserBirthday>> = birthdayDao.getAllBirthdays()

    suspend fun addUserBirthday(birthday: UserBirthday): Long = withContext(Dispatchers.IO) {
        birthdayDao.insertBirthday(birthday)
    }

    suspend fun deleteUserBirthday(birthday: UserBirthday) = withContext(Dispatchers.IO) {
        birthdayDao.deleteBirthday(birthday)
    }

    /**
     * Returns all events, holidays, tithis, and birthdays for a specific [CalendarDate].
     */
    suspend fun getEventsForDate(date: CalendarDate): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CalendarEvent>()

        // 1. Fixed & annual holidays / festivals
        list.addAll(getAnnualEventsForDate(date))

        // 2. Astronomical Tithi (Ekadashi, Purnima, Amavasya)
        val tithiEvent = calculateTithi(date)
        if (tithiEvent != null) {
            list.add(tithiEvent)
        }

        // 3. User custom birthdays
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
                    descriptionEn = b.note ?: "Birthday celebration"
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
         * Computes astronomical tithi (Purnima, Amavasya, Ekadashi).
         * Synodic lunar month = 29.530588 days. Reference New Moon = 2026-01-18.
         */
        fun calculateTithi(date: CalendarDate): CalendarEvent? {
            val epochRef = GregorianCalendar(2026, 0, 18).timeInMillis
            val dateCal = GregorianCalendar(date.year, date.month - 1, date.day).timeInMillis
            val diffDays = (dateCal - epochRef).toDouble() / (1000.0 * 60 * 60 * 24)
            val lunarCycle = 29.530588853
            val phase = ((diffDays % lunarCycle) + lunarCycle) % lunarCycle
            val tithiIndex = (phase / (lunarCycle / 30.0)).toInt() + 1 // 1..30

            return when (tithiIndex) {
                15 -> CalendarEvent(
                    id = "tithi_purnima_${date.toFormattedString()}",
                    titleBn = "পূর্ণিমা",
                    titleEn = "Purnima (Full Moon)",
                    date = date,
                    category = EventCategory.FESTIVAL_PUJA,
                    isHoliday = false,
                    descriptionBn = "শ্রী সত্যনারায়ণ পূজা ও পূর্ণিমা স্নান",
                    descriptionEn = "Full Moon day and Satyanarayan Puja",
                    visualType = EventVisualType.TITHI_PURNIMA
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
                    visualType = EventVisualType.TITHI_AMAVASYA
                )
                else -> null
            }
        }

        /**
         * Comprehensive dataset of annual Bengali festivals, government holidays,
         * national days, and notable birthdays.
         */
        fun getAnnualEventsForDate(date: CalendarDate): List<CalendarEvent> {
            val list = mutableListOf<CalendarEvent>()
            val m = date.month
            val d = date.day
            val y = date.year

            when (m) {
                1 -> when (d) {
                    1 -> list.add(CalendarEvent("h_jan_1", "ইংরেজি নববর্ষ", "New Year's Day", date, EventCategory.GOVERNMENT_HOLIDAY, true, "ইংরেজি বছরের প্রথম দিন", "First day of the year"))
                    12 -> list.add(CalendarEvent("h_jan_12", "স্বামী বিবেকানন্দের জন্মজয়ন্তী / জাতীয় যুব দিবস", "Swami Vivekananda Birthday / National Youth Day", date, EventCategory.NATIONAL_DAY, true, "স্বামী বিবেকানন্দের শুভ জন্মদিন", "Birthday of Swami Vivekananda", visualType = EventVisualType.VIVEKANANDA))
                    14 -> list.add(CalendarEvent("h_jan_14", "মকর সংক্রান্তি ও পৌষ পার্বণ", "Makar Sankranti & Poush Parbon", date, EventCategory.FESTIVAL_PUJA, true, "গঙ্গাসাগর মেলা ও পিঠেপুলি উৎসব", "Harvest festival and holy dip"))
                    15 -> list.add(CalendarEvent("h_jan_15", "মাঘি বিহু ও পোঙ্গল", "Pongal / Magh Bihu", date, EventCategory.FESTIVAL_PUJA, false))
                    23 -> list.add(CalendarEvent("h_jan_23", "নেতাজি সুভাষচন্দ্র বসুর জন্মজয়ন্তী / পরাক্রম দিবস", "Netaji Subhas Chandra Bose Jayanti", date, EventCategory.NATIONAL_DAY, true, "দেশনায়ক নেতাজির জন্মদিন", "Birthday of Netaji Subhas Chandra Bose", visualType = EventVisualType.NETAJI))
                    26 -> list.add(CalendarEvent("h_jan_26", "প্রজাতন্ত্র দিবস", "Republic Day", date, EventCategory.GOVERNMENT_HOLIDAY, true, "ভারতের জাতীয় প্রজাতন্ত্র দিবস", "National Republic Day of India"))
                    30 -> list.add(CalendarEvent("h_jan_30", "মহাত্মা গান্ধী শহীদ দিবস", "Martyrs' Day (Mahatma Gandhi)", date, EventCategory.OBSERVANCE, false, visualType = EventVisualType.GANDHI))
                }
                2 -> when (d) {
                    14 -> list.add(CalendarEvent("h_feb_14", "সরস্বতী পূজা ও বসন্ত পঞ্চমী", "Saraswati Puja / Vasant Panchami", date, EventCategory.FESTIVAL_PUJA, true, "বাগদেবী মা সরস্বতীর আরাধনা", "Worship of Goddess Saraswati", visualType = EventVisualType.SARASWATI))
                    18 -> list.add(CalendarEvent("h_feb_18", "শ্রী রামকৃষ্ণ পরমহংসদেবের জন্মতিথি", "Sri Ramakrishna Paramahamsa Jayanti", date, EventCategory.FESTIVAL_PUJA, false, "যুগাবতার শ্রী রামকৃষ্ণের আবির্ভাব তিথি", "Birth anniversary of Sri Ramakrishna", visualType = EventVisualType.VIVEKANANDA))
                    21 -> list.add(CalendarEvent("h_feb_21", "আন্তর্জাতিক মাতৃভাষা দিবস ও শহীদ দিবস", "International Mother Language Day", date, EventCategory.OBSERVANCE, false, "ভাষা শহীদদের প্রতি বিনম্র শ্রদ্ধা", "Honoring language martyrs"))
                    28 -> list.add(CalendarEvent("h_feb_28", "জাতীয় বিজ্ঞান দিবস", "National Science Day", date, EventCategory.OBSERVANCE, false))
                }
                3 -> when (d) {
                    8 -> list.add(CalendarEvent("h_mar_8", "আন্তর্জাতিক নারী দিবস", "International Women's Day", date, EventCategory.OBSERVANCE, false))
                    14 -> list.add(CalendarEvent("h_mar_14", "মহা শিবরাত্রি ব্রত", "Maha Shivaratri", date, EventCategory.FESTIVAL_PUJA, true, "শিব চতুর্দশী ব্রত ও শিবপূজা", "Great night of Lord Shiva", visualType = EventVisualType.SHIVA))
                    25 -> list.add(CalendarEvent("h_mar_25", "দোলযাত্রা ও গৌর পূর্ণিমা", "Dol Yatra / Holi", date, EventCategory.FESTIVAL_PUJA, true, "বসন্তোৎসব ও রঙের উৎসব", "Festival of colors", visualType = EventVisualType.KRISHNA))
                    26 -> list.add(CalendarEvent("h_mar_26", "হোলি উৎসব", "Holi", date, EventCategory.GOVERNMENT_HOLIDAY, true))
                    29 -> list.add(CalendarEvent("h_mar_29", "গুড ফ্রাইডে", "Good Friday", date, EventCategory.GOVERNMENT_HOLIDAY, true, visualType = EventVisualType.CHRISTMAS))
                }
                4 -> when (d) {
                    1 -> list.add(CalendarEvent("h_apr_1", "ইস্টার সানডে", "Easter Sunday", date, EventCategory.FESTIVAL_PUJA, false, visualType = EventVisualType.CHRISTMAS))
                    10 -> list.add(CalendarEvent("h_apr_10", "ঈদ-উল-ফিতর", "Eid-ul-Fitr", date, EventCategory.GOVERNMENT_HOLIDAY, true, "পবিত্র রমজান পরবর্তী ঈদের আনন্দ", "Celebration marking end of Ramadan", visualType = EventVisualType.EID))
                    13 -> list.add(CalendarEvent("h_apr_13", "নীলপূজা ও চড়ক পূজা", "Neel Puja & Charak", date, EventCategory.FESTIVAL_PUJA, false, "শিবের গাজন ও চড়ক মেলা", "Chaitra Sankranti and Charak", visualType = EventVisualType.SHIVA))
                    14 -> list.add(CalendarEvent("h_apr_14", "ড. বি আর আম্বেদকর জয়ন্তী", "Dr. B.R. Ambedkar Jayanti", date, EventCategory.GOVERNMENT_HOLIDAY, true, "সংবিধান প্রণেতা বাবাসাহেবের জন্মদিন", "Birthday of Dr. B.R. Ambedkar", visualType = EventVisualType.AMBEDKAR))
                    15 -> list.add(CalendarEvent("h_apr_15", "পয়লা বৈশাখ / বাংলা নববর্ষ", "Pohela Boishakh (Bengali New Year)", date, EventCategory.GOVERNMENT_HOLIDAY, true, "শুভ নববর্ষ ও হালখাতা উৎসব", "First day of the Bengali Calendar"))
                    21 -> list.add(CalendarEvent("h_apr_21", "মহাবীর জয়ন্তী", "Mahavir Jayanti", date, EventCategory.GOVERNMENT_HOLIDAY, true))
                }
                5 -> when (d) {
                    1 -> list.add(CalendarEvent("h_may_1", "আন্তর্জাতিক শ্রমিক দিবস / মে দিবস", "International Workers' Day (May Day)", date, EventCategory.GOVERNMENT_HOLIDAY, true, "শ্রমিকদের অধিকার ও সংহতি দিবস", "May Day celebration"))
                    8 -> list.add(CalendarEvent("h_may_8", "কবিগুরু রবীন্দ্রনাথ ঠাকুরের জন্মজয়ন্তী (২৫ বৈশাখ)", "Rabindranath Tagore Jayanti", date, EventCategory.GOVERNMENT_HOLIDAY, true, "বিশ্বকবি রবীন্দ্রনাথ ঠাকুরের জন্মদিন", "Birthday of Nobel Laureate Rabindranath Tagore", visualType = EventVisualType.RABINDRANATH))
                    23 -> list.add(CalendarEvent("h_may_23", "বুদ্ধ পূর্ণিমা", "Buddha Purnima", date, EventCategory.GOVERNMENT_HOLIDAY, true, "গৌতম বুদ্ধের জন্ম ও নির্বাণ তিথি", "Birth of Gautama Buddha", visualType = EventVisualType.BUDDHA))
                    25 -> list.add(CalendarEvent("h_may_25", "জাতীয় কবি কাজী নজরুল ইসলামের জন্মজয়ন্তী (১১ জ্যৈষ্ঠ)", "Kazi Nazrul Islam Jayanti", date, EventCategory.OBSERVANCE, false, "বিদ্রোহী কবির শুভ জন্মদিন", "Birthday of National Poet Kazi Nazrul Islam", visualType = EventVisualType.NAZRUL))
                }
                6 -> when (d) {
                    5 -> list.add(CalendarEvent("h_jun_5", "বিশ্ব পরিবেশ দিবস", "World Environment Day", date, EventCategory.OBSERVANCE, false))
                    12 -> list.add(CalendarEvent("h_jun_12", "জামাই ষষ্ঠী ব্রত", "Jamai Sasthi", date, EventCategory.FESTIVAL_PUJA, false, "বাঙালির ঐতিহ্যবাহী জামাই আপ্যায়ন", "Traditional Bengali festival"))
                    17 -> list.add(CalendarEvent("h_jun_17", "ঈদ-উল-আযহা (বকরি ঈদ)", "Eid-ul-Adha", date, EventCategory.GOVERNMENT_HOLIDAY, true, "ত্যাগের মহিমায় পবিত্র বকরি ঈদ", "Feast of the Sacrifice", visualType = EventVisualType.EID))
                    21 -> list.add(CalendarEvent("h_jun_21", "আন্তর্জাতিক যোগ দিবস", "International Yoga Day", date, EventCategory.OBSERVANCE, false))
                }
                7 -> when (d) {
                    1 -> list.add(CalendarEvent("h_jul_1", "জাতীয় চিকিৎসক দিবস ও ড. বিধানচন্দ্র রায়ের জন্মদিবস", "National Doctors' Day / Dr. B.C. Roy Birthday", date, EventCategory.OBSERVANCE, false))
                    7 -> list.add(CalendarEvent("h_jul_7", "জগন্নাথদেবের রথযাত্রা", "Rath Yatra", date, EventCategory.FESTIVAL_PUJA, true, "পুরী ও বাংলার ঐতিহ্যবাহী রথযাত্রা", "Chariot festival of Lord Jagannath", visualType = EventVisualType.JAGANNATH))
                    15 -> list.add(CalendarEvent("h_jul_15", "উল্টোরথ উৎসব", "Ulto Rath Yatra", date, EventCategory.FESTIVAL_PUJA, false))
                    17 -> list.add(CalendarEvent("h_jul_17", "পবিত্র মহরম", "Muharram", date, EventCategory.GOVERNMENT_HOLIDAY, true))
                }
                8 -> when (d) {
                    15 -> list.add(CalendarEvent("h_aug_15", "স্বাধীনতা দিবস", "Independence Day", date, EventCategory.GOVERNMENT_HOLIDAY, true, "ভারতের জাতীয় স্বাধীনতা দিবস", "National Independence Day of India", visualType = EventVisualType.NETAJI))
                    19 -> list.add(CalendarEvent("h_aug_19", "রাখীবন্ধন উৎসব ও ঝুলন পূর্ণিমা", "Raksha Bandhan", date, EventCategory.FESTIVAL_PUJA, false, "ভাই ও বোনের প্রীতির বন্ধন", "Celebration of sibling bond"))
                    26 -> list.add(CalendarEvent("h_aug_26", "শ্রীকৃষ্ণ জন্মাষ্টমী", "Janmashtami", date, EventCategory.GOVERNMENT_HOLIDAY, true, "ভগবান শ্রীকৃষ্ণের আবির্ভাব তিথি", "Birth anniversary of Lord Krishna", visualType = EventVisualType.KRISHNA))
                    29 -> list.add(CalendarEvent("h_aug_29", "জাতীয় ক্রীড়া দিবস (ধ্যানচাঁদ জন্মজয়ন্তী)", "National Sports Day", date, EventCategory.OBSERVANCE, false))
                }
                9 -> {
                    when (d) {
                        3 -> list.add(CalendarEvent("h_sep_3", "মহানায়ক উত্তম কুমারের জন্মজয়ন্তী", "Mahanayak Uttam Kumar Jayanti", date, EventCategory.BIRTHDAY, false, "বাংলা চলচ্চিত্রের চিরসবুজ মহানায়ক", "Birth anniversary of Uttam Kumar", visualType = EventVisualType.UTTAM_KUMAR))
                        5 -> list.add(CalendarEvent("h_sep_5", "শিক্ষক দিবস (ড. সর্বপল্লী রাধাকৃষ্ণন জন্মজয়ন্তী)", "Teachers' Day (Dr. S. Radhakrishnan)", date, EventCategory.OBSERVANCE, false, "শ্রদ্ধেয় শিক্ষকদের প্রতি সম্মাননা", "Honoring teachers", visualType = EventVisualType.RADHAKRISHNAN))
                        8 -> list.add(CalendarEvent("h_sep_8", "শ্রী কৃষ্ণ জন্মাষ্টমী", "Sri Krishna Janmashtami", date, EventCategory.FESTIVAL_PUJA, true, "ভগবান শ্রীকৃষ্ণের শুভ আবির্ভাব তিথি", "Janmashtami celebration", visualType = EventVisualType.KRISHNA))
                        11 -> list.add(CalendarEvent("h_sep_11", "গণেশ চতুর্থী", "Ganesh Chaturthi", date, EventCategory.FESTIVAL_PUJA, true, "সিদ্ধিদাতা শ্রী গণেশ পূজা", "Ganesh Chaturthi", visualType = EventVisualType.GANESHA))
                        16 -> list.add(CalendarEvent("h_sep_16", "মহালয়া", "Mahalaya", date, EventCategory.FESTIVAL_PUJA, false, "দেবীপক্ষের শুভ সূচনা", "Beginning of Devi Paksha", visualType = null))
                        18 -> list.add(CalendarEvent("h_sep_18", "বিশ্বকর্মা পূজা", "Vishwakarma Puja", date, EventCategory.FESTIVAL_PUJA, true, "বিশ্বকর্মা পূজার মাহাত্ম্য ও শুভেচ্ছা", "Vishwakarma celebration", visualType = EventVisualType.VISHWAKARMA))
                        25 -> list.add(CalendarEvent("h_sep_25", "একাদশী", "Ekadashi", date, EventCategory.FESTIVAL_PUJA, false, "পবিত্র একাদশী ব্রত", "Ekadashi fasting", visualType = EventVisualType.TITHI_PURNIMA))
                        26 -> list.add(CalendarEvent("h_sep_26", "ঈশ্বরচন্দ্র বিদ্যাসাগরের জন্মজয়ন্তী", "Ishwar Chandra Vidyasagar Jayanti", date, EventCategory.OBSERVANCE, false, "সমাজ সংস্কারক বিদ্যাসাগরের জন্মদিন", "Birth anniversary of Vidyasagar", visualType = EventVisualType.VIDYASAGAR))
                        27 -> list.add(CalendarEvent("h_sep_27", "অমাবস্যা", "Amavasya", date, EventCategory.FESTIVAL_PUJA, true, "মহালয়া অমাবস্যা তিথি ও পিতৃ তর্পণ", "Amavasya day and Tarpan", visualType = EventVisualType.TITHI_AMAVASYA))
                    }
                }
                10 -> when (d) {
                    2 -> list.add(CalendarEvent("h_oct_2", "গান্ধী জয়ন্তী ও লাল বাহাদুর শাস্ত্রী জন্মজয়ন্তী", "Gandhi Jayanti", date, EventCategory.GOVERNMENT_HOLIDAY, true, "জাতির জনক মহাত্মা গান্ধীর জন্মদিন", "Birth anniversary of Mahatma Gandhi", visualType = EventVisualType.GANDHI))
                    11 -> list.add(CalendarEvent("h_oct_11", "মহালয়া ও পিতৃ তর্পণ", "Mahalaya", date, EventCategory.FESTIVAL_PUJA, true, "দেবীপক্ষের সূচনা ও বীরেন্দ্রকৃষ্ণ ভদ্রের চণ্ডীপাঠ", "Beginning of Devi Paksha and Pitru Tarpan", visualType = EventVisualType.DURGA))
                    15 -> list.add(CalendarEvent("h_oct_15", "ড. এ পি জে আবদুল কালাম জন্মজয়ন্তী / বিশ্ব ছাত্র দিবস", "Dr. A.P.J. Abdul Kalam Birthday / World Students' Day", date, EventCategory.OBSERVANCE, false))
                    19 -> list.add(CalendarEvent("h_oct_19", "দুর্গাপূজা - মহা ষষ্ঠী", "Durga Puja - Maha Sasthi", date, EventCategory.FESTIVAL_PUJA, true, "মা দুর্গার বোধন ও আমন্ত্রণ", "Beginning of Durga Puja", visualType = EventVisualType.DURGA))
                    20 -> list.add(CalendarEvent("h_oct_20", "দুর্গাপূজা - মহা সপ্তমী", "Durga Puja - Maha Saptami", date, EventCategory.GOVERNMENT_HOLIDAY, true, "নবপত্রিকা প্রবেশ ও সপ্তমী বিহিত পূজা", "Second day of Durga Puja", visualType = EventVisualType.DURGA))
                    21 -> list.add(CalendarEvent("h_oct_21", "দুর্গাপূজা - মহা অষ্টমী ও সন্ধিপূজা", "Durga Puja - Maha Ashtami & Sandhi Puja", date, EventCategory.GOVERNMENT_HOLIDAY, true, "অষ্টমী পুষ্পাঞ্জলি ও কুমারী পূজা", "Maha Ashtami and Sandhi Puja", visualType = EventVisualType.DURGA))
                    22 -> list.add(CalendarEvent("h_oct_22", "দুর্গাপূজা - মহা নবমী", "Durga Puja - Maha Navami", date, EventCategory.GOVERNMENT_HOLIDAY, true, "নবমী যজ্ঞ ও আরতি", "Ninth day of Durga Puja", visualType = EventVisualType.DURGA))
                    23 -> list.add(CalendarEvent("h_oct_23", "বিজয়া দশমী ও সিঁদুর খেলা", "Bijoya Dashami / Dussehra", date, EventCategory.GOVERNMENT_HOLIDAY, true, "মা দুর্গার বিসর্জন ও শুভ বিজয়া", "Immersion and Bijoya Dashami", visualType = EventVisualType.DURGA))
                    28 -> list.add(CalendarEvent("h_oct_28", "কোজাগরী লক্ষ্মী পূজা", "Kojagari Lakshmi Puja", date, EventCategory.FESTIVAL_PUJA, true, "ধনধান্যের দেবী মা লক্ষ্মীর পূজা", "Worship of Goddess Lakshmi", visualType = EventVisualType.SARASWATI))
                    31 -> list.add(CalendarEvent("h_oct_31", "কালীপূজা ও দীপাবলি", "Kali Puja & Diwali", date, EventCategory.GOVERNMENT_HOLIDAY, true, "শ্যামাপূজা ও আলোর উৎসব দীপাবলি", "Festival of lights and Kali Puja", visualType = EventVisualType.KALI))
                }
                11 -> when (d) {
                    2 -> list.add(CalendarEvent("h_nov_2", "ভাইফোঁটা ও ভ্রাতৃদ্বিতীয়া", "Bhai Phota", date, EventCategory.FESTIVAL_PUJA, true, "ভাই ও বোনের শুভ কামনায় ফোঁটা", "Bhai Dooj / traditional sibling blessing"))
                    7 -> list.add(CalendarEvent("h_nov_7", "ছট পূজা (সূর্য অর্ঘ্য)", "Chhath Puja", date, EventCategory.GOVERNMENT_HOLIDAY, true, "সূর্য দেবতার মহা আরাধনা", "Chhath Puja sun worship"))
                    10 -> list.add(CalendarEvent("h_nov_10", "জগদ্ধাত্রী পূজা", "Jagaddhatri Puja", date, EventCategory.FESTIVAL_PUJA, false, "চন্দননগর ও শান্তিপুরের বিখ্যাত পূজা", "Worship of Goddess Jagaddhatri", visualType = EventVisualType.DURGA))
                    14 -> list.add(CalendarEvent("h_nov_14", "শিশু দিবস ও জওহরলাল নেহেরু জন্মজয়ন্তী", "Children's Day (Jawaharlal Nehru)", date, EventCategory.OBSERVANCE, false, "চাচা নেহেরুর শুভ জন্মদিন", "Children's Day in India"))
                    15 -> list.add(CalendarEvent("h_nov_15", "গুরু নানক জয়ন্তী", "Guru Nanak Jayanti", date, EventCategory.GOVERNMENT_HOLIDAY, true, "শিখ ধর্মের প্রবক্তা গুরু নানকের জন্মদিন", "Birth of Guru Nanak Dev"))
                    26 -> list.add(CalendarEvent("h_nov_26", "সংবিধান দিবস", "Constitution Day (Samvidhan Divas)", date, EventCategory.OBSERVANCE, false))
                }
                12 -> when (d) {
                    1 -> list.add(CalendarEvent("h_dec_1", "বিশ্ব এইডস দিবস", "World AIDS Day", date, EventCategory.OBSERVANCE, false))
                    16 -> list.add(CalendarEvent("h_dec_16", "বিজয় দিবস", "Vijay Diwas", date, EventCategory.NATIONAL_DAY, false))
                    25 -> list.add(CalendarEvent("h_dec_25", "বড়দিন (ক্রিসমাস ডে)", "Christmas Day", date, EventCategory.GOVERNMENT_HOLIDAY, true, "যীশু খ্রিস্টের শুভ জন্মদিন", "Christmas celebration", visualType = EventVisualType.CHRISTMAS))
                    31 -> list.add(CalendarEvent("h_dec_31", "বছরের শেষ দিন", "New Year's Eve", date, EventCategory.OBSERVANCE, false))
                }
            }

            return list
        }
    }
}
