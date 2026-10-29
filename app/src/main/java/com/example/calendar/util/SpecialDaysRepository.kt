package com.example.calendar.util

import android.content.Context
import com.example.calendar.model.CalendarDate
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventCategory
import com.example.calendar.model.EventVisualType
import com.example.data.AppDatabase
import com.example.model.FestivalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

data class SpecialDayItem(
    val key: String,         // "MM-dd" e.g. "01-01"
    val dayMonthKey: String, // "dd-MM" e.g. "01-01"
    val month: Int,
    val day: Int,
    val titleBn: String,
    val titleEn: String,
    val whyBn: String,
    val categoryType: String,
    val isHoliday: Boolean,
    val category: String,
    val visualType: String?
)

/**
 * Repository for the 146 Special Days expanded dataset loaded from assets/special_days.json.
 * Provides query methods by day-month key ("MM-dd" or "dd-MM") and date.
 */
object SpecialDaysRepository {

    @Volatile
    private var isLoaded = false
    private val eventsByKey = mutableMapOf<String, MutableList<SpecialDayItem>>()
    private val allItems = mutableListOf<SpecialDayItem>()

    fun ensureLoaded(context: Context? = null) {
        if (isLoaded) return
        val ctx = context ?: try {
            com.example.AmarDokanApp.instance
        } catch (_: Exception) {
            null
        } ?: return

        synchronized(this) {
            if (isLoaded) return
            try {
                val inputStream = ctx.assets.open("special_days.json")
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val jsonString = reader.use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val key = obj.optString("key", "")
                    val dayMonthKey = obj.optString("dayMonthKey", "")
                    val month = obj.optInt("month", 1)
                    val day = obj.optInt("day", 1)
                    val titleBn = obj.optString("titleBn", "")
                    val titleEn = obj.optString("titleEn", titleBn)
                    val whyBn = obj.optString("whyBn", "")
                    val categoryType = obj.optString("categoryType", "")
                    val isHoliday = obj.optBoolean("isHoliday", false)
                    val category = obj.optString("category", "OBSERVANCE")
                    val visualType = if (obj.isNull("visualType")) null else obj.optString("visualType")

                    val item = SpecialDayItem(
                        key = key,
                        dayMonthKey = dayMonthKey,
                        month = month,
                        day = day,
                        titleBn = titleBn,
                        titleEn = titleEn,
                        whyBn = whyBn,
                        categoryType = categoryType,
                        isHoliday = isHoliday,
                        category = category,
                        visualType = visualType
                    )
                    allItems.add(item)

                    // Index by MM-dd
                    eventsByKey.computeIfAbsent(key) { mutableListOf() }.add(item)
                    // Also index by dd-MM if distinct
                    if (dayMonthKey.isNotBlank() && dayMonthKey != key) {
                        eventsByKey.computeIfAbsent(dayMonthKey) { mutableListOf() }.add(item)
                    }
                }
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Query special day items by key (supports "MM-dd" e.g. "01-26" and "dd-MM" e.g. "26-01").
     */
    fun getSpecialDaysByKey(key: String, context: Context? = null): List<SpecialDayItem> {
        if (!isLoaded && context != null) {
            ensureLoaded(context)
        }
        return eventsByKey[key] ?: emptyList()
    }

    /**
     * Query CalendarEvents by day-month key for a given date.
     */
    fun getEventsByDayMonthKey(key: String, date: CalendarDate, context: Context? = null): List<CalendarEvent> {
        val items = getSpecialDaysByKey(key, context)
        return items.map { item ->
            val vType = try {
                if (!item.visualType.isNullOrBlank()) EventVisualType.valueOf(item.visualType) else null
            } catch (_: Exception) { null }

            val cat = when (item.category) {
                "GOVERNMENT_HOLIDAY" -> EventCategory.GOVERNMENT_HOLIDAY
                "NATIONAL_DAY" -> EventCategory.NATIONAL_DAY
                else -> EventCategory.OBSERVANCE
            }

            CalendarEvent(
                id = "sd_${item.key}_${date.year}",
                titleBn = item.titleBn,
                titleEn = item.titleEn,
                date = date,
                category = cat,
                isHoliday = item.isHoliday,
                descriptionBn = "কেন: ${item.whyBn} | ধরন: ${item.categoryType}",
                descriptionEn = "Reason: ${item.whyBn} | Type: ${item.categoryType}",
                visualType = vType,
                type = if (item.isHoliday) "GOVERNMENT_HOLIDAY" else if (cat == EventCategory.NATIONAL_DAY) "NATIONAL" else "OBSERVANCE",
                moonSightingAdjustable = false
            )
        }
    }

    /**
     * Query CalendarEvents for a specific Gregorian [CalendarDate].
     */
    fun getSpecialDayEventsForDate(date: CalendarDate, context: Context? = null): List<CalendarEvent> {
        val mmDd = String.format("%02d-%02d", date.month, date.day)
        return getEventsByDayMonthKey(mmDd, date, context)
    }

    /**
     * Pre-populates Room Database with all special days if not already present.
     */
    suspend fun prePopulateRoomDatabase(db: AppDatabase, context: Context) = withContext(Dispatchers.IO) {
        ensureLoaded(context)
        try {
            val festivalDao = db.festivalDao()
            val existing = festivalDao.getFestivalsByType("SPECIAL")
            if (existing.isEmpty()) {
                val entities = mutableListOf<FestivalEntity>()
                val years = 2025..2028
                for (item in allItems) {
                    for (yr in years) {
                        entities.add(
                            FestivalEntity(
                                id = "fest_sd_${item.key}_$yr",
                                titleBn = item.titleBn,
                                titleEn = item.titleEn,
                                year = yr,
                                month = item.month,
                                day = item.day,
                                type = if (item.isHoliday) "NATIONAL" else "SPECIAL",
                                isHoliday = item.isHoliday,
                                descriptionBn = "কেন: ${item.whyBn} | ধরন: ${item.categoryType}",
                                descriptionEn = item.whyBn,
                                visualType = item.visualType,
                                moonSightingAdjustable = false
                            )
                        )
                    }
                }
                festivalDao.insertFestivals(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllItems(context: Context? = null): List<SpecialDayItem> {
        if (!isLoaded && context != null) {
            ensureLoaded(context)
        }
        return allItems
    }
}
