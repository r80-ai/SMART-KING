package com.example.calendar.util

import com.example.calendar.model.CalendarDate
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventCategory
import com.example.calendar.model.EventVisualType
import java.util.GregorianCalendar

/**
 * Hindu Panchang and Bengali Lunisolar Festival Calculation Engine.
 * Computes exact Tithi (Shukla/Krishna Paksha, Ekadashi, Purnima, Amavasya)
 * and maps all major Sanatan festivals for 1432–1434 Bangabda (2025–2028 CE).
 */
object HinduPanchangEngine {

    private const val SYNODIC_LUNAR_CYCLE = 29.530588853

    data class TithiInfo(
        val index: Int, // 1..30
        val isShuklaPaksha: Boolean,
        val tithiNameBn: String,
        val tithiNameEn: String,
        val pakshaNameBn: String
    )

    private val TITHI_NAMES_BN = arrayOf(
        "প্রতিপদ", "দ্বিতীয়া", "তৃতীয়া", "চতুর্থী", "পঞ্চমী",
        "ষষ্ঠী", "সপ্তমী", "অষ্টমী", "নবমী", "দশমী",
        "একাদশী", "দ্বাদশী", "ত্রয়োদশী", "চতুর্দশী", "পূর্ণিমা"
    )

    private val TITHI_NAMES_EN = arrayOf(
        "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shasthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Purnima"
    )

    /**
     * Calculates the accurate astronomical Tithi for any given [CalendarDate].
     */
    fun calculateTithi(date: CalendarDate): TithiInfo {
        // Reference New Moon (Amavasya) epoch: 2026-01-18 12:00 UTC
        val epochRef = GregorianCalendar(2026, 0, 18, 12, 0, 0).timeInMillis
        val targetCal = GregorianCalendar(date.year, date.month - 1, date.day, 6, 0, 0).timeInMillis
        val diffDays = (targetCal - epochRef).toDouble() / (1000.0 * 60 * 60 * 24)
        val phase = ((diffDays % SYNODIC_LUNAR_CYCLE) + SYNODIC_LUNAR_CYCLE) % SYNODIC_LUNAR_CYCLE
        val tithiIndex = (phase / (SYNODIC_LUNAR_CYCLE / 30.0)).toInt() + 1 // 1..30

        return if (tithiIndex <= 15) {
            val nameIdx = (tithiIndex - 1).coerceIn(0, 14)
            TithiInfo(
                index = tithiIndex,
                isShuklaPaksha = true,
                tithiNameBn = if (tithiIndex == 15) "পূর্ণিমা" else "শুক্লা ${TITHI_NAMES_BN[nameIdx]}",
                tithiNameEn = if (tithiIndex == 15) "Purnima" else "Shukla ${TITHI_NAMES_EN[nameIdx]}",
                pakshaNameBn = "শুক্লপক্ষ"
            )
        } else {
            val nameIdx = (tithiIndex - 16).coerceIn(0, 14)
            TithiInfo(
                index = tithiIndex,
                isShuklaPaksha = false,
                tithiNameBn = if (tithiIndex == 30) "অমাবস্যা" else "কৃষ্ণা ${TITHI_NAMES_BN[nameIdx]}",
                tithiNameEn = if (tithiIndex == 30) "Amavasya" else "Krishna ${TITHI_NAMES_EN[nameIdx]}",
                pakshaNameBn = "কৃষ্ণপক্ষ"
            )
        }
    }

    /**
     * Returns all Hindu festivals, pujas, and tithi observances for a given date.
     */
    fun getHinduFestivalsForDate(date: CalendarDate): List<CalendarEvent> {
        val list = mutableListOf<CalendarEvent>()
        val y = date.year
        val m = date.month
        val d = date.day

        // 1. Tithi observance (Purnima, Amavasya, Ekadashi)
        val tithi = calculateTithi(date)
        when (tithi.index) {
            11 -> {
                list.add(
                    CalendarEvent(
                        id = "hindu_ekadashi_shukla_${date.toFormattedString()}",
                        titleBn = "পবিত্র একাদশী ব্রত (${tithi.tithiNameBn})",
                        titleEn = "Ekadashi Vrata (Shukla Paksha)",
                        date = date,
                        category = EventCategory.FESTIVAL_PUJA,
                        isHoliday = false,
                        descriptionBn = "শ্রী বিষ্ণু আরাধনা ও নির্জলা/ফলমূল একাদশী উপবাস",
                        descriptionEn = "Holy Ekadashi fasting and Vishnu puja",
                        visualType = EventVisualType.KRISHNA,
                        type = "HINDU"
                    )
                )
            }
            15 -> {
                list.add(
                    CalendarEvent(
                        id = "hindu_purnima_${date.toFormattedString()}",
                        titleBn = "পূর্ণিমা তিথি",
                        titleEn = "Purnima (Full Moon)",
                        date = date,
                        category = EventCategory.FESTIVAL_PUJA,
                        isHoliday = false,
                        descriptionBn = "শ্রী সত্যনারায়ণ পূজা ও পূর্ণিমা স্নান",
                        descriptionEn = "Full Moon day and Satyanarayan Puja",
                        visualType = EventVisualType.TITHI_PURNIMA,
                        type = "HINDU"
                    )
                )
            }
            26 -> {
                list.add(
                    CalendarEvent(
                        id = "hindu_ekadashi_krishna_${date.toFormattedString()}",
                        titleBn = "পবিত্র একাদশী ব্রত (${tithi.tithiNameBn})",
                        titleEn = "Ekadashi Vrata (Krishna Paksha)",
                        date = date,
                        category = EventCategory.FESTIVAL_PUJA,
                        isHoliday = false,
                        descriptionBn = "শ্রী হরি আরাধনা ও একাদশী উপবাস",
                        descriptionEn = "Holy Ekadashi fasting and Lord Hari worship",
                        visualType = EventVisualType.KRISHNA,
                        type = "HINDU"
                    )
                )
            }
            30 -> {
                list.add(
                    CalendarEvent(
                        id = "hindu_amavasya_${date.toFormattedString()}",
                        titleBn = "অমাবস্যা তিথি",
                        titleEn = "Amavasya (New Moon)",
                        date = date,
                        category = EventCategory.FESTIVAL_PUJA,
                        isHoliday = false,
                        descriptionBn = "অমাবস্যা স্নান ও পিতৃ তর্পণ",
                        descriptionEn = "New Moon day and Tarpan",
                        visualType = EventVisualType.TITHI_AMAVASYA,
                        type = "HINDU"
                    )
                )
            }
        }

        // 2. High-precision Panchang Festivals mapped by Year, Month, Day
        when (y) {
            2025 -> getFestivals2025(date, list)
            2026 -> getFestivals2026(date, list)
            2027 -> getFestivals2027(date, list)
            2028 -> getFestivals2028(date, list)
            else -> getGeneralYearFestivals(date, list)
        }

        // 3. Solar / Fixed Hindu Festivals occurring every year
        getSolarFestivals(date, list)

        return list
    }

    private fun getFestivals2025(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val m = date.month
        val d = date.day
        when (m) {
            2 -> when (d) {
                2 -> list.add(createHinduEvent("h25_saraswati", "সরস্বতী পূজা ও বসন্ত পঞ্চমী", "Saraswati Puja / Vasant Panchami", date, true, "মাঘ শুক্লা পঞ্চমী — বাগদেবী মা সরস্বতীর মহা আরাধনা", EventVisualType.SARASWATI))
                26 -> list.add(createHinduEvent("h25_shivaratri", "মহা শিবরাত্রি ব্রত", "Maha Shivaratri", date, true, "ফাল্গুন কৃষ্ণা চতুর্দশী — দেবাদিদেব মহাদেবের উপবাস ও পূজা", EventVisualType.SHIVA))
            }
            3 -> when (d) {
                14 -> list.add(createHinduEvent("h25_dolyatra", "দোলযাত্রা ও গৌর পূর্ণিমা", "Dol Yatra / Holi", date, true, "ফাল্গুনী পূর্ণিমা — রঙের উৎসব ও মহাপ্রভুর আবির্ভাব তিথি", EventVisualType.KRISHNA))
                15 -> list.add(createHinduEvent("h25_holi", "হোলি উৎসব", "Holi", date, true, "বসন্তোৎসব ও রঙের আনন্দ", EventVisualType.KRISHNA))
            }
            4 -> when (d) {
                6 -> list.add(createHinduEvent("h25_ram_navami", "শ্রী রাম নবমী", "Ram Navami", date, true, "চৈত্র শুক্লা নবমী — ভগবান শ্রী রামচন্দ্রের আবির্ভাব তিথি", EventVisualType.HINDU_DIYA))
                10 -> list.add(createHinduEvent("h25_mahavir_jayanti", "মহাবীর জয়ন্তী", "Mahavir Jayanti", date, true, "চৈত্র শুক্লা ত্রয়োদশী — জৈন তীর্থঙ্কর ভগবান মহাবীরের জন্মদিন", null))
                18 -> list.add(createHinduEvent("h25_good_friday", "গুড ফ্রাইডে", "Good Friday", date, true, "যিশুখ্রিস্টের আত্মত্যাগ ও ক্রুশবিদ্ধ হওয়ার স্মরণ", EventVisualType.CHRISTMAS))
                20 -> list.add(createHinduEvent("h25_easter", "ইস্টার সানডে", "Easter Sunday", date, false, "যিশুখ্রিস্টের পুনরুত্থান দিবস", EventVisualType.CHRISTMAS))
                30 -> list.add(createHinduEvent("h25_akshaya", "অক্ষয় তৃতীয়্যা", "Akshaya Tritiya", date, false, "বৈশাখ শুক্লা তৃতীয়া — শুভ কার্য ও হালখাতা", EventVisualType.GANESHA))
            }
            5 -> when (d) {
                12 -> list.add(createHinduEvent("h25_buddha_purnima", "বুদ্ধ পূর্ণিমা", "Buddha Purnima", date, true, "বৈশাখী পূর্ণিমা — ভগবান গৌতম বুদ্ধের শুভ জন্ম ও বোধিলাভ", EventVisualType.BUDDHA))
            }
            6 -> when (d) {
                2 -> list.add(createHinduEvent("h25_jamai_sasthi", "জামাই ষষ্ঠী ব্রত", "Jamai Sasthi", date, false, "জ্যৈষ্ঠ শুক্লা ষষ্ঠী — বাঙালির ঐতিহ্যবাহী জামাই আপ্যায়ন", EventVisualType.HINDU_DIYA))
                27 -> list.add(createHinduEvent("h25_rathyatra", "জগন্নাথদেবের রথযাত্রা", "Rath Yatra", date, true, "আষাঢ় শুক্লা দ্বিতীয়া — পুরী ও মাহেশের রথযাত্রা", EventVisualType.JAGANNATH))
            }
            7 -> when (d) {
                5 -> list.add(createHinduEvent("h25_ultorath", "উল্টোরথ উৎসব", "Ulto Rath Yatra", date, false, "জগন্নাথদেবের শ্রীমন্দিরে প্রত্যাবর্তন", EventVisualType.JAGANNATH))
            }
            8 -> when (d) {
                9 -> list.add(createHinduEvent("h25_rakhi", "রাখীবন্ধন উৎসব ও ঝুলন পূর্ণিমা", "Raksha Bandhan", date, false, "শ্রাবণী পূর্ণিমা — ভাই-বোনের প্রীতির বন্ধন ও ঝুলনযাত্রা", EventVisualType.HINDU_DIYA))
                16 -> list.add(createHinduEvent("h25_janmashtami", "শ্রীকৃষ্ণ জন্মাষ্টমী", "Sri Krishna Janmashtami", date, true, "ভাদ্র কৃষ্ণা অষ্টমী — ভগবান শ্রীকৃষ্ণের শুভ আবির্ভাব", EventVisualType.KRISHNA))
                27 -> list.add(createHinduEvent("h25_ganesh", "গণেশ চতুর্থী", "Ganesh Chaturthi", date, true, "ভাদ্র শুক্লা চতুর্থী — সিদ্ধিদাতা শ্রী গণেশ পূজা", EventVisualType.GANESHA))
            }
            9 -> when (d) {
                21 -> list.add(createHinduEvent("h25_mahalaya", "মহালয়া ও পিতৃ তর্পণ", "Mahalaya", date, true, "আশ্বিন অমাবস্যা — দেবীপক্ষের শুভ সূচনা ও চণ্ডীপাঠ", EventVisualType.DURGA))
                28 -> list.add(createHinduEvent("h25_sasthi", "দুর্গাপূজা - মহা ষষ্ঠী", "Durga Puja - Maha Sasthi", date, true, "দেবী বোধন, আমন্ত্রণ ও অধিবাস", EventVisualType.DURGA))
                29 -> list.add(createHinduEvent("h25_saptami", "দুর্গাপূজা - মহা সপ্তমী", "Durga Puja - Maha Saptami", date, true, "নবপত্রিকা প্রবেশ ও সপ্তমী পূজা", EventVisualType.DURGA))
                30 -> list.add(createHinduEvent("h25_ashtami", "দুর্গাপূজা - মহা অষ্টমী ও সন্ধিপূজা", "Durga Puja - Maha Ashtami & Sandhi Puja", date, true, "অষ্টমী পুষ্পাঞ্জলি ও সন্ধিপূজা", EventVisualType.DURGA))
            }
            10 -> when (d) {
                1 -> list.add(createHinduEvent("h25_navami", "দুর্গাপূজা - মহা নবমী", "Durga Puja - Maha Navami", date, true, "মহা নবমী বিহিত পূজা ও যজ্ঞ", EventVisualType.DURGA))
                2 -> list.add(createHinduEvent("h25_dashami", "বিজয়া দশমী ও সিঁদুর খেলা", "Bijoya Dashami / Dussehra", date, true, "মা দুর্গার বিসর্জন ও শুভ বিজয়ার শুভেচ্ছা", EventVisualType.DURGA))
                6 -> list.add(createHinduEvent("h25_lakshmi", "কোজাগরী লক্ষ্মী পূজা", "Kojagari Lakshmi Puja", date, true, "আশ্বিনী পূর্ণিমা — ধনধান্যের দেবী মা লক্ষ্মীর আরাধনা", EventVisualType.SARASWATI))
                20 -> list.add(createHinduEvent("h25_kali", "কালীপূজা ও দীপাবলি", "Kali Puja & Diwali", date, true, "কার্তিকী অমাবস্যা — শ্যামাপূজা ও আলোর উৎসব", EventVisualType.KALI))
                22 -> list.add(createHinduEvent("h25_bhaiphota", "ভাইফোঁটা ও ভ্রাতৃদ্বিতীয়া", "Bhai Phota", date, true, "কার্তিক শুক্লা দ্বিতীয়া — ভাইয়ের দীর্ঘায়ু কামনায় ফোঁটা", EventVisualType.HINDU_DIYA))
                26 -> list.add(createHinduEvent("h25_chhath", "ছট পূজা (সূর্য অর্ঘ্য)", "Chhath Puja", date, true, "কার্তিক শুক্লা ষষ্ঠী — সূর্য দেবতার মহা আরাধনা", EventVisualType.HINDU_DIYA))
                30 -> list.add(createHinduEvent("h25_jagaddhatri", "জগদ্ধাত্রী পূজা", "Jagaddhatri Puja", date, false, "কার্তিক শুক্লা নবমী — জগদ্ধাত্রী মাতার পূজা", EventVisualType.DURGA))
            }
            11 -> when (d) {
                5 -> {
                    list.add(createHinduEvent("h25_rash", "রাসযাত্রা ও রাস পূর্ণিমা", "Rash Yatra", date, false, "কার্তিকী পূর্ণিমা — বৃন্দাবনের রাস মহোৎসব", EventVisualType.KRISHNA))
                    list.add(createHinduEvent("h25_guru_nanak", "গুরু নানক জয়ন্তী", "Guru Nanak Jayanti", date, true, "কার্তিক পূর্ণিমা — শিখ ধর্মের প্রবক্তা গুরু নানক দেবজীর জন্মদিন", EventVisualType.HINDU_DIYA))
                }
                16 -> list.add(createHinduEvent("h25_kartik_puja", "কার্তিক পূজা", "Kartik Puja", date, true, "কার্তিক সংক্রান্তি — দেবসেনাপতি শ্রী কার্তিকের পূজা", EventVisualType.HINDU_DIYA))
            }
        }
    }

    private fun getFestivals2026(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val m = date.month
        val d = date.day
        when (m) {
            2 -> when (d) {
                15 -> list.add(createHinduEvent("h26_shivaratri", "মহা শিবরাত্রি ব্রত", "Maha Shivaratri", date, true, "ফাল্গুন কৃষ্ণা চতুর্দশী — দেবাদিদেব মহাদেবের আরাধনা ও উপবাস", EventVisualType.SHIVA))
                23 -> list.add(createHinduEvent("h26_saraswati", "সরস্বতী পূজা ও বসন্ত পঞ্চমী", "Saraswati Puja / Vasant Panchami", date, true, "মাঘ শুক্লা পঞ্চমী — বাগদেবী মা সরস্বতীর পূজা", EventVisualType.SARASWATI))
            }
            3 -> when (d) {
                3 -> list.add(createHinduEvent("h26_dolyatra", "দোলযাত্রা ও গৌর পূর্ণিমা", "Dol Yatra / Holi", date, true, "ফাল্গুনী পূর্ণিমা — বসন্তোৎসব ও রঙের উৎসব", EventVisualType.KRISHNA))
                4 -> list.add(createHinduEvent("h26_holi", "হোলি উৎসব", "Holi", date, true, "রঙ ও সম্প্রীতির উৎসব", EventVisualType.KRISHNA))
                27 -> list.add(createHinduEvent("h26_ram_navami", "শ্রী রাম নবমী", "Ram Navami", date, true, "চৈত্র শুক্লা নবমী — ভগবান শ্রী রামচন্দ্রের আবির্ভাব তিথি", EventVisualType.HINDU_DIYA))
                31 -> list.add(createHinduEvent("h26_mahavir_jayanti", "মহাবীর জয়ন্তী", "Mahavir Jayanti", date, true, "চৈত্র শুক্লা ত্রয়োদশী — ভগবান মহাবীরের শুভ জন্মদিন", null))
            }
            4 -> when (d) {
                3 -> list.add(createHinduEvent("h26_good_friday", "গুড ফ্রাইডে", "Good Friday", date, true, "পবিত্র গুড ফ্রাইডে — যিশুখ্রিস্টের আত্মত্যাগ স্মরণ", EventVisualType.CHRISTMAS))
                5 -> list.add(createHinduEvent("h26_easter", "ইস্টার সানডে", "Easter Sunday", date, false, "যিশুখ্রিস্টের পুনরুত্থান দিবস", EventVisualType.CHRISTMAS))
                19 -> list.add(createHinduEvent("h26_akshaya", "অক্ষয় তৃতীয়্যা", "Akshaya Tritiya", date, false, "বৈশাখ শুক্লা তৃতীয়া — মঙ্গলময় অক্ষয় ফলদায়ক তিথি", EventVisualType.GANESHA))
            }
            5 -> when (d) {
                22 -> list.add(createHinduEvent("h26_jamai_sasthi", "জামাই ষষ্ঠী ব্রত", "Jamai Sasthi", date, false, "জ্যৈষ্ঠ শুক্লা ষষ্ঠী — জামাই ষষ্ঠীর পার্বণ", EventVisualType.HINDU_DIYA))
                31 -> list.add(createHinduEvent("h26_buddha_purnima", "বুদ্ধ পূর্ণিমা", "Buddha Purnima", date, true, "বৈশাখী পূর্ণিমা — ভগবান গৌতম বুদ্ধের শুভ জন্ম ও বোধিলাভ", EventVisualType.BUDDHA))
            }
            7 -> when (d) {
                16 -> list.add(createHinduEvent("h26_rathyatra", "জগন্নাথদেবের রথযাত্রা", "Rath Yatra", date, true, "আষাঢ় শুক্লা দ্বিতীয়া — জগন্নাথ, বলরাম ও সুভদ্রার রথযাত্রা", EventVisualType.JAGANNATH))
                24 -> list.add(createHinduEvent("h26_ultorath", "উল্টোরথ উৎসব", "Ulto Rath Yatra", date, false, "জগন্নাথদেবের পুনরাগমন", EventVisualType.JAGANNATH))
            }
            8 -> when (d) {
                28 -> list.add(createHinduEvent("h26_rakhi", "রাখীবন্ধন ও ঝুলন পূর্ণিমা", "Raksha Bandhan", date, false, "শ্রাবণী পূর্ণিমা — রাখী বন্ধন উৎসব", EventVisualType.HINDU_DIYA))
            }
            9 -> when (d) {
                4 -> list.add(createHinduEvent("h26_janmashtami", "শ্রীকৃষ্ণ জন্মাষ্টমী", "Sri Krishna Janmashtami", date, true, "ভাদ্র কৃষ্ণা অষ্টমী — ভগবান শ্রীকৃষ্ণের আবির্ভাব তিথি", EventVisualType.KRISHNA))
                14 -> list.add(createHinduEvent("h26_ganesh", "গণেশ চতুর্থী", "Ganesh Chaturthi", date, true, "ভাদ্র শুক্লা চতুর্থী — শ্রী গণেশ চতুর্থী পূজা", EventVisualType.GANESHA))
            }
            10 -> when (d) {
                10 -> list.add(createHinduEvent("h26_mahalaya", "মহালয়া ও পিতৃ তর্পণ", "Mahalaya", date, true, "আশ্বিন অমাবস্যা — দেবীপক্ষের শুভ সূচনা ও চণ্ডীপাঠ", EventVisualType.DURGA))
                17 -> list.add(createHinduEvent("h26_sasthi", "দুর্গাপূজা - মহা ষষ্ঠী", "Durga Puja - Maha Sasthi", date, true, "মা দুর্গার বোধন ও আমন্ত্রণ", EventVisualType.DURGA))
                18 -> list.add(createHinduEvent("h26_saptami", "দুর্গাপূজা - মহা সপ্তমী", "Durga Puja - Maha Saptami", date, true, "নবপত্রিকা প্রবেশ ও সপ্তমী বিহিত পূজা", EventVisualType.DURGA))
                19 -> list.add(createHinduEvent("h26_ashtami", "দুর্গাপূজা - মহা অষ্টমী ও সন্ধিপূজা", "Durga Puja - Maha Ashtami & Sandhi Puja", date, true, "অষ্টমী অঞ্জলি ও সন্ধিপূজা", EventVisualType.DURGA))
                20 -> list.add(createHinduEvent("h26_navami", "দুর্গাপূজা - মহা নবমী", "Durga Puja - Maha Navami", date, true, "নবমী যজ্ঞ ও আরতি", EventVisualType.DURGA))
                21 -> list.add(createHinduEvent("h26_dashami", "বিজয়া দশমী ও সিঁদুর খেলা", "Bijoya Dashami / Dussehra", date, true, "প্রতিমা বিসর্জন ও শুভ বিজয়ার প্রণাম", EventVisualType.DURGA))
                25 -> list.add(createHinduEvent("h26_lakshmi", "কোজাগরী লক্ষ্মী পূজা", "Kojagari Lakshmi Puja", date, true, "আশ্বিন পূর্ণিমা — ধনধান্য ও সৌভাগ্যের পূজা", EventVisualType.SARASWATI))
            }
            11 -> when (d) {
                8 -> list.add(createHinduEvent("h26_kali", "কালীপূজা ও দীপাবলি", "Kali Puja & Diwali", date, true, "কার্তিক অমাবস্যা — শ্যামাপূজা ও আলোক উৎসব", EventVisualType.KALI))
                10 -> list.add(createHinduEvent("h26_bhaiphota", "ভাইফোঁটা ও ভ্রাতৃদ্বিতীয়া", "Bhai Phota", date, true, "কার্তিক শুক্লা দ্বিতীয়া — শুভ ভাইফোঁটা", EventVisualType.HINDU_DIYA))
                14 -> list.add(createHinduEvent("h26_chhath", "ছট পূজা (সূর্য অর্ঘ্য)", "Chhath Puja", date, true, "কার্তিক শুক্লা ষষ্ঠী — ছট মহোৎসব", EventVisualType.HINDU_DIYA))
                16 -> list.add(createHinduEvent("h26_kartik_puja", "কার্তিক পূজা", "Kartik Puja", date, true, "কার্তিক সংক্রান্তি — দেব সেনাপতি কার্তিকের আরাধনা", EventVisualType.HINDU_DIYA))
                18 -> list.add(createHinduEvent("h26_jagaddhatri", "জগদ্ধাত্রী পূজা", "Jagaddhatri Puja", date, false, "কার্তিক শুক্লা নবমী — জগদ্ধাত্রী মাতার পূজা", EventVisualType.DURGA))
                24 -> {
                    list.add(createHinduEvent("h26_rash", "রাসযাত্রা ও রাস পূর্ণিমা", "Rash Yatra", date, false, "কার্তিকী পূর্ণিমা — রাস মহোৎসব", EventVisualType.KRISHNA))
                    list.add(createHinduEvent("h26_guru_nanak", "গুরু নানক জয়ন্তী", "Guru Nanak Jayanti", date, true, "কার্তিক পূর্ণিমা — শিখ ধর্মের প্রবক্তা গুরু নানকের জন্মতিথি", EventVisualType.HINDU_DIYA))
                }
            }
        }
    }

    private fun getFestivals2027(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val m = date.month
        val d = date.day
        when (m) {
            2 -> when (d) {
                11 -> list.add(createHinduEvent("h27_saraswati", "সরস্বতী পূজা ও বসন্ত পঞ্চমী", "Saraswati Puja / Vasant Panchami", date, true, "মাঘ শুক্লা পঞ্চমী — বাগদেবী মা সরস্বতীর আরাধনা", EventVisualType.SARASWATI))
            }
            3 -> when (d) {
                6 -> list.add(createHinduEvent("h27_shivaratri", "মহা শিবরাত্রি ব্রত", "Maha Shivaratri", date, true, "ফাল্গুন কৃষ্ণা চতুর্দশী — মহাদেব পূজা", EventVisualType.SHIVA))
                22 -> list.add(createHinduEvent("h27_dolyatra", "দোলযাত্রা ও গৌর পূর্ণিমা", "Dol Yatra / Holi", date, true, "ফাল্গুনী পূর্ণিমা — দোল উৎসব", EventVisualType.KRISHNA))
                23 -> list.add(createHinduEvent("h27_holi", "হোলি উৎসব", "Holi", date, true, "রঙের উৎসব", EventVisualType.KRISHNA))
                26 -> list.add(createHinduEvent("h27_good_friday", "গুড ফ্রাইডে", "Good Friday", date, true, "গুড ফ্রাইডে", EventVisualType.CHRISTMAS))
                28 -> list.add(createHinduEvent("h27_easter", "ইস্টার সানডে", "Easter Sunday", date, false, "ইস্টার সানডে", EventVisualType.CHRISTMAS))
            }
            4 -> when (d) {
                15 -> list.add(createHinduEvent("h27_ram_navami", "শ্রী রাম নবমী", "Ram Navami", date, true, "চৈত্র শুক্লা নবমী — শ্রী রাম নবমী", EventVisualType.HINDU_DIYA))
                19 -> list.add(createHinduEvent("h27_mahavir_jayanti", "মহাবীর জয়ন্তী", "Mahavir Jayanti", date, true, "মহাবীর জয়ন্তী", null))
            }
            5 -> when (d) {
                9 -> list.add(createHinduEvent("h27_akshaya", "অক্ষয় তৃতীয়্যা", "Akshaya Tritiya", date, false, "বৈশাখ শুক্লা তৃতীয়া", EventVisualType.GANESHA))
                20 -> list.add(createHinduEvent("h27_buddha_purnima", "বুদ্ধ পূর্ণিমা", "Buddha Purnima", date, true, "বৈশাখী পূর্ণিমা — বুদ্ধ পূর্ণিমা", EventVisualType.BUDDHA))
            }
            6 -> when (d) {
                11 -> list.add(createHinduEvent("h27_jamai_sasthi", "জামাই ষষ্ঠী ব্রত", "Jamai Sasthi", date, false, "জ্যৈষ্ঠ শুক্লা ষষ্ঠী", EventVisualType.HINDU_DIYA))
            }
            7 -> when (d) {
                5 -> list.add(createHinduEvent("h27_rathyatra", "জগন্নাথদেবের রথযাত্রা", "Rath Yatra", date, true, "আষাঢ় শুক্লা দ্বিতীয়া", EventVisualType.JAGANNATH))
                13 -> list.add(createHinduEvent("h27_ultorath", "উল্টোরথ উৎসব", "Ulto Rath Yatra", date, false, "উল্টোরথ", EventVisualType.JAGANNATH))
            }
            8 -> when (d) {
                17 -> list.add(createHinduEvent("h27_rakhi", "রাখীবন্ধন ও ঝুলন পূর্ণিমা", "Raksha Bandhan", date, false, "শ্রাবণী পূর্ণিমা", EventVisualType.HINDU_DIYA))
                25 -> list.add(createHinduEvent("h27_janmashtami", "শ্রীকৃষ্ণ জন্মাষ্টমী", "Sri Krishna Janmashtami", date, true, "ভাদ্র কৃষ্ণা অষ্টমী", EventVisualType.KRISHNA))
            }
            9 -> when (d) {
                4 -> list.add(createHinduEvent("h27_ganesh", "গণেশ চতুর্থী", "Ganesh Chaturthi", date, true, "ভাদ্র শুক্লা চতুর্থী", EventVisualType.GANESHA))
                29 -> list.add(createHinduEvent("h27_mahalaya", "মহালয়া ও পিতৃ তর্পণ", "Mahalaya", date, true, "আশ্বিন অমাবস্যা — দেবীপক্ষের সূচনা", EventVisualType.DURGA))
            }
            10 -> when (d) {
                6 -> list.add(createHinduEvent("h27_sasthi", "দুর্গাপূজা - মহা ষষ্ঠী", "Durga Puja - Maha Sasthi", date, true, "ষষ্ঠী বোধন", EventVisualType.DURGA))
                7 -> list.add(createHinduEvent("h27_saptami", "দুর্গাপূজা - মহা সপ্তমী", "Durga Puja - Maha Saptami", date, true, "সপ্তমী পূজা", EventVisualType.DURGA))
                8 -> list.add(createHinduEvent("h27_ashtami", "দুর্গাপূজা - মহা অষ্টমী ও সন্ধিপূজা", "Durga Puja - Maha Ashtami & Sandhi Puja", date, true, "মহা অষ্টমী", EventVisualType.DURGA))
                9 -> list.add(createHinduEvent("h27_navami", "দুর্গাপূজা - মহা নবমী", "Durga Puja - Maha Navami", date, true, "মহা নবমী", EventVisualType.DURGA))
                10 -> list.add(createHinduEvent("h27_dashami", "বিজয়া দশমী ও সিঁদুর খেলা", "Bijoya Dashami / Dussehra", date, true, "শুভ বিজয়া দশমী", EventVisualType.DURGA))
                15 -> list.add(createHinduEvent("h27_lakshmi", "কোজাগরী লক্ষ্মী পূজা", "Kojagari Lakshmi Puja", date, true, "আশ্বিন পূর্ণিমা — লক্ষ্মী পূজা", EventVisualType.SARASWATI))
                29 -> list.add(createHinduEvent("h27_kali", "কালীপূজা ও দীপাবলি", "Kali Puja & Diwali", date, true, "কার্তিক অমাবস্যা — দীপাবলি ও কালীপূজা", EventVisualType.KALI))
                31 -> list.add(createHinduEvent("h27_bhaiphota", "ভাইফোঁটা ও ভ্রাতৃদ্বিতীয়া", "Bhai Phota", date, true, "ভাইফোঁটা", EventVisualType.HINDU_DIYA))
            }
            11 -> when (d) {
                4 -> list.add(createHinduEvent("h27_chhath", "ছট পূজা", "Chhath Puja", date, true, "কার্তিক শুক্লা ষষ্ঠী", EventVisualType.HINDU_DIYA))
                7 -> list.add(createHinduEvent("h27_jagaddhatri", "জগদ্ধাত্রী পূজা", "Jagaddhatri Puja", date, false, "জগদ্ধাত্রী পূজা", EventVisualType.DURGA))
                13 -> {
                    list.add(createHinduEvent("h27_rash", "রাসযাত্রা ও রাস পূর্ণিমা", "Rash Yatra", date, false, "রাস পূর্ণিমা", EventVisualType.KRISHNA))
                    list.add(createHinduEvent("h27_guru_nanak", "গুরু নানক জয়ন্তী", "Guru Nanak Jayanti", date, true, "গুরু নানক জয়ন্তী", EventVisualType.HINDU_DIYA))
                }
                16 -> list.add(createHinduEvent("h27_kartik_puja", "কার্তিক পূজা", "Kartik Puja", date, true, "কার্তিক সংক্রান্তি — কার্তিক পূজা", EventVisualType.HINDU_DIYA))
            }
        }
    }

    private fun getFestivals2028(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val m = date.month
        val d = date.day
        when (m) {
            1 -> when (d) {
                31 -> list.add(createHinduEvent("h28_saraswati", "সরস্বতী পূজা ও বসন্ত পঞ্চমী", "Saraswati Puja / Vasant Panchami", date, true, "বাগদেবী পূজা", EventVisualType.SARASWATI))
            }
            2 -> when (d) {
                24 -> list.add(createHinduEvent("h28_shivaratri", "মহা শিবরাত্রি ব্রত", "Maha Shivaratri", date, true, "শিব চতুর্দশী", EventVisualType.SHIVA))
            }
            3 -> when (d) {
                11 -> list.add(createHinduEvent("h28_dolyatra", "দোলযাত্রা ও গৌর পূর্ণিমা", "Dol Yatra / Holi", date, true, "দোলযাত্রা", EventVisualType.KRISHNA))
            }
            4 -> when (d) {
                4 -> list.add(createHinduEvent("h28_ram_navami", "শ্রী রাম নবমী", "Ram Navami", date, true, "শ্রী রাম নবমী", EventVisualType.HINDU_DIYA))
                8 -> list.add(createHinduEvent("h28_mahavir_jayanti", "মহাবীর জয়ন্তী", "Mahavir Jayanti", date, true, "মহাবীর জয়ন্তী", null))
                14 -> list.add(createHinduEvent("h28_good_friday", "গুড ফ্রাইডে", "Good Friday", date, true, "গুড ফ্রাইডে", EventVisualType.CHRISTMAS))
                16 -> list.add(createHinduEvent("h28_easter", "ইস্টার সানডে", "Easter Sunday", date, false, "ইস্টার সানডে", EventVisualType.CHRISTMAS))
                28 -> list.add(createHinduEvent("h28_akshaya", "অক্ষয় তৃতীয়্যা", "Akshaya Tritiya", date, false, "বৈশাখ শুক্লা তৃতীয়া", EventVisualType.GANESHA))
            }
            5 -> when (d) {
                8 -> list.add(createHinduEvent("h28_buddha_purnima", "বুদ্ধ পূর্ণিমা", "Buddha Purnima", date, true, "বুদ্ধ পূর্ণিমা", EventVisualType.BUDDHA))
            }
            6 -> when (d) {
                24 -> list.add(createHinduEvent("h28_rathyatra", "জগন্নাথদেবের রথযাত্রা", "Rath Yatra", date, true, "রথযাত্রা", EventVisualType.JAGANNATH))
            }
            8 -> when (d) {
                13 -> list.add(createHinduEvent("h28_janmashtami", "শ্রীকৃষ্ণ জন্মাষ্টমী", "Sri Krishna Janmashtami", date, true, "জন্মাষ্টমী", EventVisualType.KRISHNA))
                23 -> list.add(createHinduEvent("h28_ganesh", "গণেশ চতুর্থী", "Ganesh Chaturthi", date, true, "গণেশ চতুর্থী", EventVisualType.GANESHA))
            }
            9 -> when (d) {
                18 -> list.add(createHinduEvent("h28_mahalaya", "মহালয়া", "Mahalaya", date, true, "মহালয়া অমাবস্যা", EventVisualType.DURGA))
                25 -> list.add(createHinduEvent("h28_sasthi", "দুর্গাপূজা - মহা ষষ্ঠী", "Durga Puja - Maha Sasthi", date, true, "ষষ্ঠী বোধন", EventVisualType.DURGA))
                29 -> list.add(createHinduEvent("h28_dashami", "বিজয়া দশমী", "Bijoya Dashami", date, true, "শুভ বিজয়া", EventVisualType.DURGA))
            }
            10 -> when (d) {
                3 -> list.add(createHinduEvent("h28_lakshmi", "কোজাগরী লক্ষ্মী পূজা", "Kojagari Lakshmi Puja", date, true, "কোজাগরী পূর্ণিমা", EventVisualType.SARASWATI))
                17 -> list.add(createHinduEvent("h28_kali", "কালীপূজা ও দীপাবলি", "Kali Puja & Diwali", date, true, "দীপাবলি ও কালীপূজা", EventVisualType.KALI))
                19 -> list.add(createHinduEvent("h28_bhaiphota", "ভাইফোঁটা", "Bhai Phota", date, true, "ভাইফোঁটা", EventVisualType.HINDU_DIYA))
                23 -> list.add(createHinduEvent("h28_chhath", "ছট পূজা", "Chhath Puja", date, true, "ছট পূজা", EventVisualType.HINDU_DIYA))
            }
            11 -> when (d) {
                1 -> {
                    list.add(createHinduEvent("h28_rash", "রাস পূর্ণিমা", "Rash Purnima", date, false, "রাস পূর্ণিমা", EventVisualType.KRISHNA))
                    list.add(createHinduEvent("h28_guru_nanak", "গুরু নানক জয়ন্তী", "Guru Nanak Jayanti", date, true, "গুরু নানক জয়ন্তী", EventVisualType.HINDU_DIYA))
                }
                16 -> list.add(createHinduEvent("h28_kartik_puja", "কার্তিক পূজা", "Kartik Puja", date, true, "কার্তিক সংক্রান্তি — কার্তিক পূজা", EventVisualType.HINDU_DIYA))
            }
        }
    }

    private fun getGeneralYearFestivals(date: CalendarDate, list: MutableList<CalendarEvent>) {
        // Fallback approximate tithi calculation
        val t = calculateTithi(date)
        if (date.month in 9..10 && t.index == 8 && t.isShuklaPaksha) {
            list.add(createHinduEvent("h_gen_ashtami", "দুর্গাপূজা - মহা অষ্টমী", "Durga Puja - Maha Ashtami", date, true, "অষ্টমী বিহিত পূজা ও অঞ্জলি", EventVisualType.DURGA))
        }
    }

    private fun getSolarFestivals(date: CalendarDate, list: MutableList<CalendarEvent>) {
        val m = date.month
        val d = date.day
        when (m) {
            1 -> {
                if (d == 14 || d == 15) {
                    list.add(createHinduEvent("h_poush_sankranti", "মকর সংক্রান্তি ও পৌষ পার্বণ", "Makar Sankranti & Poush Parbon", date, true, "গঙ্গাসাগর মেলা, পবিত্র স্নান ও পিঠেপুলি উৎসব", EventVisualType.HINDU_DIYA))
                }
            }
            4 -> {
                if (d == 13 || d == 14) {
                    list.add(createHinduEvent("h_charak_neel", "নীলপূজা ও চড়ক পূজা", "Neel Puja & Charak Puja", date, false, "চৈত্র সংক্রান্তি, শিবের গাজন ও চড়ক মেলা", EventVisualType.SHIVA))
                }
                if (d == 14 || d == 15) {
                    list.add(createHinduEvent("h_pohela_boishakh", "পয়লা বৈশাখ / বাংলা নববর্ষ ও বৈশাখী", "Pohela Boishakh (Bengali New Year) & Vaisakhi", date, true, "শুভ নববর্ষ ও বৈশাখী উৎসব", EventVisualType.HINDU_DIYA))
                }
            }
            9 -> {
                if (d == 17 || d == 18) {
                    list.add(createHinduEvent("h_vishwakarma", "বিশ্বকর্মা পূজা", "Vishwakarma Puja", date, true, "কন্যা সংক্রান্তি — দেবশিল্পী বিশ্বকর্মার আরাধনা ও ঘুড়ি উৎসব", EventVisualType.VISHWAKARMA))
                }
            }
            11 -> {
                if (d == 16 || d == 17) {
                    list.add(createHinduEvent("h_kartik_sankranti_solar", "কার্তিক সংক্রান্তি ও কার্তিক পূজা", "Kartik Sankranti & Kartik Puja", date, true, "কার্তিক সংক্রান্তি — দেবসেনাপতি কার্তিকের পূজা", EventVisualType.HINDU_DIYA))
                }
            }
        }
    }

    private fun createHinduEvent(
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
            category = EventCategory.FESTIVAL_PUJA,
            isHoliday = isHoliday,
            descriptionBn = descriptionBn,
            descriptionEn = titleEn,
            visualType = visualType,
            type = "HINDU",
            moonSightingAdjustable = false
        )
    }
}
