package com.example.calendar.util

import androidx.annotation.DrawableRes
import com.example.R
import com.example.calendar.model.CalendarEvent
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

    @DrawableRes
    fun getDrawableResForVisualType(type: EventVisualType?): Int? {
        if (type == null) return null
        return VISUAL_TYPE_TO_DRAWABLE[type]
    }

    @DrawableRes
    fun getDrawableResForName(name: String?): Int? {
        if (name.isNullOrBlank()) return null
        val cleanName = name.removeSuffix(".png").trim()
        return NAME_TO_DRAWABLE[cleanName]
    }

    @DrawableRes
    fun getDrawableResForEvent(event: CalendarEvent): Int? {
        // 1. Direct explicit imageResId
        if (event.imageResId != null && event.imageResId != 0) {
            return event.imageResId
        }

        // 2. Direct explicit imageDrawableName
        if (!event.imageDrawableName.isNullOrBlank()) {
            val res = getDrawableResForName(event.imageDrawableName)
            if (res != null) return res
        }

        // 3. By visualType
        if (event.visualType != null) {
            val res = getDrawableResForVisualType(event.visualType)
            if (res != null) return res
        }

        // 4. Safe keyword matching on titleBn / id
        val text = "${event.id} ${event.titleBn} ${event.titleEn}"
        return when {
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
    }
}
