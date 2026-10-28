package com.example.calendar.util

import androidx.annotation.DrawableRes
import com.example.R
import com.example.calendar.model.CalendarDate
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventCategory
import com.example.calendar.model.EventVisualType

/**
 * Maps calendar festival and personality events to authentic high-resolution
 * drawable images in res/drawable/.
 */
object EventDrawableMapper {

    private val VISUAL_TYPE_TO_DRAWABLE = mapOf(
        EventVisualType.KRISHNA to R.drawable.photo_krishna,
        EventVisualType.RADHAKRISHNAN to R.drawable.photo_radhakrishnan,
        EventVisualType.GANESHA to R.drawable.photo_ganesha,
        EventVisualType.VISHWAKARMA to R.drawable.photo_vishwakarma,
        EventVisualType.UTTAM_KUMAR to R.drawable.photo_uttam_kumar,
        EventVisualType.VIVEKANANDA to R.drawable.photo_vivekananda,
        EventVisualType.NETAJI to R.drawable.photo_netaji,
        EventVisualType.RABINDRANATH to R.drawable.photo_tagore,
        EventVisualType.NAZRUL to R.drawable.photo_nazrul,
        EventVisualType.VIDYASAGAR to R.drawable.photo_vidyasagar,
        EventVisualType.SARASWATI to R.drawable.photo_saraswati,
        EventVisualType.DURGA to R.drawable.photo_durga,
        EventVisualType.KALI to R.drawable.photo_kali,
        EventVisualType.LAXMI to R.drawable.photo_laxmi,
        EventVisualType.APJ_ABDUL_KALAM to R.drawable.photo_dr_apj_abdul_kalam,
        EventVisualType.SHIVA to R.drawable.photo_shiva,
        EventVisualType.JAGANNATH to R.drawable.photo_jagannath,
        EventVisualType.GANDHI to R.drawable.photo_gandhi,
        EventVisualType.AMBEDKAR to R.drawable.photo_ambedkar,
        EventVisualType.TITHI_PURNIMA to R.drawable.photo_moon_full,
        EventVisualType.TITHI_AMAVASYA to R.drawable.photo_moon_dark
    )

    private val NAME_TO_DRAWABLE = mapOf(
        "photo_laxmi" to R.drawable.photo_laxmi,
        "photo_kali" to R.drawable.photo_kali,
        "photo_dr_apj_abdul_kalam" to R.drawable.photo_dr_apj_abdul_kalam,
        "photo_durga" to R.drawable.photo_durga,
        "photo_gandhi" to R.drawable.photo_gandhi,
        "photo_ganesha" to R.drawable.photo_ganesha,
        "photo_jagannath" to R.drawable.photo_jagannath,
        "photo_krishna" to R.drawable.photo_krishna,
        "photo_saraswati" to R.drawable.photo_saraswati,
        "photo_shiva" to R.drawable.photo_shiva,
        "photo_vishwakarma" to R.drawable.photo_vishwakarma,
        "photo_netaji" to R.drawable.photo_netaji,
        "photo_vivekananda" to R.drawable.photo_vivekananda,
        "photo_tagore" to R.drawable.photo_tagore,
        "photo_nazrul" to R.drawable.photo_nazrul,
        "photo_vidyasagar" to R.drawable.photo_vidyasagar,
        "photo_ambedkar" to R.drawable.photo_ambedkar,
        "photo_radhakrishnan" to R.drawable.photo_radhakrishnan,
        "photo_uttam_kumar" to R.drawable.photo_uttam_kumar,
        "photo_moon_full" to R.drawable.photo_moon_full,
        "photo_moon_dark" to R.drawable.photo_moon_dark
    )

    private val TARGET_DRAWABLE_MAP = mapOf(
        EventVisualType.MAKAR_SANKRANTI to "photo_makar_sankranti",
        EventVisualType.HOLI to "photo_holi",
        EventVisualType.RAM_NAVAMI to "photo_ram_navami",
        EventVisualType.AKSHAYA_TRITIYA to "photo_akshaya_tritiya",
        EventVisualType.BHAI_PHOTA to "photo_bhai_phota",
        EventVisualType.CHHATH_PUJA to "photo_chhath_puja",
        EventVisualType.KARTIK_PUJA to "photo_kartik_puja",
        EventVisualType.EKADASHI to "photo_ekadashi",
        EventVisualType.MAHALAYA to "photo_mahalaya",

        EventVisualType.RAMADAN to "photo_ramadan",
        EventVisualType.EID_UL_FITR to "photo_eid_ul_fitr",
        EventVisualType.EID_UL_ADHA to "photo_eid_ul_adha",
        EventVisualType.MUHARRAM to "photo_muharram",
        EventVisualType.EID_MILADUNNABI to "photo_eid_miladunnabi",
        EventVisualType.SHAB_E_BARAT to "photo_shab_e_barat",

        EventVisualType.GURU_NANAK to "photo_guru_nanak",
        EventVisualType.BAISAKHI to "photo_baisakhi",
        EventVisualType.BUDDHA_PURNIMA to "photo_buddha_purnima",
        EventVisualType.MAHAVIR_JAYANTI to "photo_mahavir_jayanti",
        EventVisualType.GOOD_FRIDAY to "photo_good_friday",
        EventVisualType.EASTER to "photo_easter",
        EventVisualType.CHRISTMAS to "photo_christmas",

        EventVisualType.NEHRU to "photo_nehru",
        EventVisualType.SHASTRI to "photo_shastri",
        EventVisualType.ABUL_KALAM_AZAD to "photo_abul_kalam_azad",
        EventVisualType.VAJPAYEE to "photo_vajpayee",
        EventVisualType.RAMAKRISHNA to "photo_ramakrishna"
    )

    @DrawableRes
    fun getDrawableResForVisualType(type: EventVisualType?): Int? {
        if (type == null) return null
        return VISUAL_TYPE_TO_DRAWABLE[type]
    }

    fun getTargetDrawableNameForType(type: EventVisualType?): String? {
        if (type == null) return null
        return TARGET_DRAWABLE_MAP[type]
    }

    @DrawableRes
    fun getDrawableResForName(name: String?, context: android.content.Context? = null): Int? {
        if (name.isNullOrBlank()) return null
        val cleanName = name.removeSuffix(".png").trim()
        val staticRes = NAME_TO_DRAWABLE[cleanName]
        if (staticRes != null) return staticRes

        // Dynamic lookup if custom PNG was added to res/drawable/
        if (context != null) {
            try {
                val id = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
                if (id != 0) return id
            } catch (_: Exception) {}
        }
        return null
    }

    @DrawableRes
    fun getDrawableResForEvent(event: CalendarEvent, context: android.content.Context? = null): Int? {
        // 1. Direct explicit imageResId
        if (event.imageResId != null && event.imageResId != 0) {
            return event.imageResId
        }

        // 2. Direct explicit imageDrawableName
        if (!event.imageDrawableName.isNullOrBlank()) {
            val res = getDrawableResForName(event.imageDrawableName, context)
            if (res != null) return res
        }

        // 3. By visualType
        if (event.visualType != null) {
            val res = getDrawableResForVisualType(event.visualType)
            if (res != null) return res

            // Check if target drawable name exists in resources dynamically
            val targetName = TARGET_DRAWABLE_MAP[event.visualType]
            if (targetName != null) {
                val resFromTarget = getDrawableResForName(targetName, context)
                if (resFromTarget != null) return resFromTarget
            }
        }

        // 4. Safe keyword matching on titleBn / id
        val text = "${event.id} ${event.titleBn} ${event.titleEn}"
        val directRes = when {
            text.contains("কালাম") || text.contains("Kalam") || text.contains("abdul_kalam") -> R.drawable.photo_dr_apj_abdul_kalam
            text.contains("লক্ষ্মী") || text.contains("লক্ষী") || text.contains("Laxmi") || text.contains("Lakshmi") -> R.drawable.photo_laxmi
            text.contains("কালী") || text.contains("শ্যামা") || text.contains("Kali") -> R.drawable.photo_kali
            text.contains("দুর্গা") || text.contains("Durga") || text.contains("মহিষাসুরমর্দিনী") -> R.drawable.photo_durga
            text.contains("সরস্বতী") || text.contains("Saraswati") || text.contains("বসন্ত পঞ্চমী") -> R.drawable.photo_saraswati
            text.contains("শিব") || text.contains("মহাদেব") || text.contains("Shiv") || text.contains("নীলপূজা") || text.contains("চড়ক") -> R.drawable.photo_shiva
            text.contains("জগন্নাথ") || text.contains("রথযাত্রা") || text.contains("উল্টোরথ") || text.contains("Jagannath") -> R.drawable.photo_jagannath
            text.contains("গণেশ") || text.contains("Ganesh") -> R.drawable.photo_ganesha
            text.contains("শ্রীকৃষ্ণ") || text.contains("জন্মাষ্টমী") || text.contains("দোলযাত্রা") || text.contains("হোলি") || text.contains("রাস") || text.contains("Krishna") -> R.drawable.photo_krishna
            text.contains("বিশ্বকর্মা") || text.contains("Vishwakarma") -> R.drawable.photo_vishwakarma
            text.contains("নেতাজি") || text.contains("সুভাষ") || text.contains("Netaji") -> R.drawable.photo_netaji
            text.contains("বিবেকানন্দ") || text.contains("রামকৃষ্ণ") || text.contains("Vivekananda") -> R.drawable.photo_vivekananda
            text.contains("গান্ধী") || text.contains("Gandhi") -> R.drawable.photo_gandhi
            text.contains("আম্বেদকর") || text.contains("Ambedkar") -> R.drawable.photo_ambedkar
            text.contains("রবীন্দ্রনাথ") || text.contains("Tagore") || text.contains("Rabindranath") -> R.drawable.photo_tagore
            text.contains("নজরুল") || text.contains("Nazrul") -> R.drawable.photo_nazrul
            text.contains("বিদ্যাসাগর") || text.contains("Vidyasagar") -> R.drawable.photo_vidyasagar
            text.contains("রাধাকৃষ্ণন") || text.contains("Radhakrishnan") -> R.drawable.photo_radhakrishnan
            text.contains("উত্তম কুমার") || text.contains("Uttam Kumar") -> R.drawable.photo_uttam_kumar
            text.contains("পূর্ণিমা") || text.contains("Purnima") -> R.drawable.photo_moon_full
            text.contains("অমাবস্যা") || text.contains("Amavasya") -> R.drawable.photo_moon_dark
            else -> null
        }
        if (directRes != null) return directRes

        // 5. Dynamic target check based on remaining keywords
        if (context != null) {
            val potentialName = when {
                text.contains("মকর") || text.contains("পৌষ সংক্রান্তি") -> "photo_makar_sankranti"
                text.contains("হোলি") -> "photo_holi"
                text.contains("রাম নবমী") -> "photo_ram_navami"
                text.contains("অক্ষয়") -> "photo_akshaya_tritiya"
                text.contains("ভাইফোঁটা") || text.contains("ভ্রাতৃদ্বিতীয়া") -> "photo_bhai_phota"
                text.contains("ছট পূজা") -> "photo_chhath_puja"
                text.contains("কার্তিক পূজা") -> "photo_kartik_puja"
                text.contains("একাদশী") -> "photo_ekadashi"
                text.contains("মহালয়া") -> "photo_mahalaya"
                text.contains("রমজান") -> "photo_ramadan"
                text.contains("ঈদুল ফিতর") -> "photo_eid_ul_fitr"
                text.contains("ঈদুল আজহা") || text.contains("কোরবানি") -> "photo_eid_ul_adha"
                text.contains("মুহররম") || text.contains("আশুরা") -> "photo_muharram"
                text.contains("মিলাদুন্নবী") -> "photo_eid_miladunnabi"
                text.contains("শবে বরাত") -> "photo_shab_e_barat"
                text.contains("গুরু নানক") -> "photo_guru_nanak"
                text.contains("বৈশাখী") -> "photo_baisakhi"
                text.contains("বুদ্ধ পূর্ণিমা") -> "photo_buddha_purnima"
                text.contains("মহাবীর") -> "photo_mahavir_jayanti"
                text.contains("গুড ফ্রাইডে") -> "photo_good_friday"
                text.contains("ইস্টার") -> "photo_easter"
                text.contains("বড়দিন") || text.contains("ক্রিসমাস") -> "photo_christmas"
                text.contains("নেহরু") -> "photo_nehru"
                text.contains("শাস্ত্রী") -> "photo_shastri"
                text.contains("আজাদ") -> "photo_abul_kalam_azad"
                text.contains("বাজপেয়ী") -> "photo_vajpayee"
                text.contains("রামকৃষ্ণ") -> "photo_ramakrishna"
                else -> null
            }
            if (potentialName != null) {
                val dynId = getDrawableResForName(potentialName, context)
                if (dynId != null) return dynId
            }
        }

        return null
    }

    /**
     * Dynamic visual asset resolver with vector fallback as requested.
     */
    fun getEventVisualAsset(context: android.content.Context, resName: String?, category: String): Any {
        if (!resName.isNullOrEmpty()) {
            val drawableId = getDrawableResForName(resName, context)
            if (drawableId != null && drawableId != 0) return drawableId

            val cleanName = resName.removePrefix("R.drawable.").removePrefix("res/drawable/").removeSuffix(".png").trim()
            val visualType = TARGET_DRAWABLE_MAP.entries.find { it.value == cleanName }?.key
            if (visualType != null) {
                return EventVisualFallbackEngine.getFallback(
                    CalendarEvent(
                        id = "temp_$cleanName",
                        titleBn = cleanName,
                        titleEn = cleanName,
                        date = CalendarDate(2026, 1, 1),
                        category = EventCategory.FESTIVAL_PUJA,
                        isHoliday = false,
                        visualType = visualType
                    )
                )
            }
        }
        return EventVisualFallbackEngine.getFallbackByCategory(category)
    }
}
