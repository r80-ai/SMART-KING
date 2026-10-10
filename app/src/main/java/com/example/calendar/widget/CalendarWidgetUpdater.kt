package com.example.calendar.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.calendar.model.BanglaDate
import com.example.calendar.model.CalendarDate
import com.example.calendar.model.CalendarEvent
import com.example.calendar.util.BanglaCalendarConverter
import com.example.calendar.util.CalendarEventsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object CalendarWidgetUpdater {

    private const val TAG = "BanglaCalendarWidget"
    const val ACTION_MIDNIGHT_UPDATE = "com.example.calendar.ACTION_MIDNIGHT_UPDATE"
    const val EXTRA_OPEN_CALENDAR = "OPEN_CALENDAR"
    const val VALUE_OPEN_BANGLA = "BANGLA"

    /**
     * Updates all active instances of Bangla calendar widgets safely.
     */
    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val banglaIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BanglaCalendarWidgetProvider::class.java)
            )
            if (banglaIds != null && banglaIds.isNotEmpty()) {
                updateBanglaWidgets(context, appWidgetManager, banglaIds)
            }
            scheduleMidnightUpdate(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating all widgets", e)
        }
    }

    /**
     * Synchronously updates the given Bangla calendar widget IDs with immediate date and time.
     */
    fun updateBanglaWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val today = CalendarDate.today()
            val banglaDate = BanglaCalendarConverter.toBanglaDate(today)
            val style = CalendarWidgetPreferences.getWidgetStyle(context)
            val topEvent = getSynchronousTopEvent(today)

            scheduleMidnightUpdate(context)

            for (id in appWidgetIds) {
                try {
                    val options = appWidgetManager.getAppWidgetOptions(id)
                    val views = buildBanglaWidgetViews(context, today, banglaDate, topEvent?.titleBn, style, options)
                    appWidgetManager.updateAppWidget(id, views)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed updating widget id: $id, attempting fallback views", e)
                    try {
                        val fallbackViews = buildFallbackWidgetViews(context)
                        appWidgetManager.updateAppWidget(id, fallbackViews)
                    } catch (fallbackEx: Exception) {
                        Log.e(TAG, "Critical: Fallback widget update also failed for id $id", fallbackEx)
                    }
                }
            }

            checkAndUpdateCustomBirthdays(context, appWidgetManager, appWidgetIds)
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in updateBanglaWidgets", e)
        }
    }

    /**
     * Updates a single Bangla widget instance (e.g. after options/resize changed).
     */
    fun updateBanglaWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        try {
            val today = CalendarDate.today()
            val banglaDate = BanglaCalendarConverter.toBanglaDate(today)
            val style = CalendarWidgetPreferences.getWidgetStyle(context)
            val topEvent = getSynchronousTopEvent(today)
            val options = newOptions ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
            val views = buildBanglaWidgetViews(context, today, banglaDate, topEvent?.titleBn, style, options)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            Log.e(TAG, "Failed updating single widget id: $appWidgetId", e)
            try {
                val fallbackViews = buildFallbackWidgetViews(context)
                appWidgetManager.updateAppWidget(appWidgetId, fallbackViews)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Critical: Fallback update failed for id $appWidgetId", fallbackEx)
            }
        }
    }

    /**
     * Minimal fallback RemoteViews to ensure the launcher never displays "Can't load widget".
     */
    private fun buildFallbackWidgetViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_bangla_calendar)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.calendar.OPEN_BANGLA"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CALENDAR, VALUE_OPEN_BANGLA)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 101, intent, flags)
        views.setOnClickPendingIntent(R.id.widget_bangla_root, pendingIntent)
        return views
    }

    /**
     * Fast synchronous lookup of annual festivals, government holidays, or tithis.
     */
    private fun getSynchronousTopEvent(today: CalendarDate): CalendarEvent? {
        return try {
            val annual = CalendarEventsRepository.getAnnualEventsForDate(today)
            val topAnnual = annual.firstOrNull { it.isHoliday } ?: annual.firstOrNull()
            val tithi = CalendarEventsRepository.calculateTithi(today)
            topAnnual ?: tithi
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up synchronous top event", e)
            null
        }
    }

    internal fun buildBanglaWidgetViews(
        context: Context,
        gregorianDate: CalendarDate,
        banglaDate: BanglaDate,
        eventTitle: String?,
        style: WidgetStyle,
        options: Bundle? = null
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_bangla_calendar)

        // PendingIntent to launch Bangla Calendar in MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.calendar.OPEN_BANGLA"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CALENDAR, VALUE_OPEN_BANGLA)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 101, intent, flags)
        views.setOnClickPendingIntent(R.id.widget_bangla_root, pendingIntent)

        // Set theme background drawable
        val bgRes = when (style) {
            WidgetStyle.GRADIENT -> R.drawable.widget_bg_gradient
            WidgetStyle.DARK -> R.drawable.widget_bg_dark
            WidgetStyle.LIGHT -> R.drawable.widget_bg_light
            WidgetStyle.SIMPLE -> R.drawable.widget_bg_simple
        }
        views.setInt(R.id.widget_bangla_root, "setBackgroundResource", bgRes)

        // Text values
        // Top line: e.g. "৩০ ভাদ্র ১৪৩৩"
        views.setTextViewText(R.id.widget_bangla_date, banglaDate.formatShort())

        // Subdate line: e.g. "বৃহস্পতিবার, ১৭ সেপ্টেম্বর ২০২৬"
        views.setTextViewText(R.id.widget_bangla_subdate, banglaDate.formatGregorianFullInBangla(gregorianDate))

        // Festival / Holiday badge
        val hasEvent = !eventTitle.isNullOrBlank()
        if (hasEvent) {
            views.setViewVisibility(R.id.widget_bangla_event_container, View.VISIBLE)
            views.setViewVisibility(R.id.widget_bangla_event, View.VISIBLE)
            // Strip any existing leading symbol if present, and set pure text with a yellow circle dot
            val cleanTitle = eventTitle.trim().removePrefix("✦").removePrefix("★").removePrefix("•").removePrefix("●").trim()
            views.setTextViewText(R.id.widget_bangla_event, "● $cleanTitle")
        } else {
            views.setViewVisibility(R.id.widget_bangla_event_container, View.GONE)
            views.setViewVisibility(R.id.widget_bangla_event, View.GONE)
        }

        // Apply responsive sizing
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 250
        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 60
        applyResponsiveSizes(views, minWidth = minWidth, minHeight = minHeight, hasEvent = hasEvent)

        // Apply theme color styling safely
        applyStyle(views, R.id.widget_bangla_clock, R.id.widget_bangla_date, R.id.widget_bangla_subdate, R.id.widget_bangla_event, style)

        return views
    }

    /**
     * Dynamically adjusts typography sizing and badge visibility based on widget dimensions
     * to prevent clipping or overflowing when resized.
     */
    private fun applyResponsiveSizes(
        views: RemoteViews,
        minWidth: Int,
        minHeight: Int,
        hasEvent: Boolean
    ) {
        val clockId = R.id.widget_bangla_clock
        val dateId = R.id.widget_bangla_date
        val subdateId = R.id.widget_bangla_subdate
        val eventId = R.id.widget_bangla_event
        val eventContainerId = R.id.widget_bangla_event_container

        // Always keep festival line visible when an event is present
        if (hasEvent) {
            views.setViewVisibility(eventContainerId, View.VISIBLE)
            views.setViewVisibility(eventId, View.VISIBLE)
        } else {
            views.setViewVisibility(eventContainerId, View.GONE)
            views.setViewVisibility(eventId, View.GONE)
        }

        if (minHeight < 52) {
            // Very compact launcher cell
            views.setTextViewTextSize(clockId, TypedValue.COMPLEX_UNIT_SP, 30f)
            views.setTextViewTextSize(dateId, TypedValue.COMPLEX_UNIT_SP, 14.5f)
            views.setTextViewTextSize(subdateId, TypedValue.COMPLEX_UNIT_SP, 10.5f)
            views.setTextViewTextSize(eventId, TypedValue.COMPLEX_UNIT_SP, 10f)
        } else if (minHeight < 75) {
            // Standard 4x1 launcher cell (reference dimensions)
            views.setTextViewTextSize(clockId, TypedValue.COMPLEX_UNIT_SP, 34f)
            views.setTextViewTextSize(dateId, TypedValue.COMPLEX_UNIT_SP, 16f)
            views.setTextViewTextSize(subdateId, TypedValue.COMPLEX_UNIT_SP, 11.5f)
            views.setTextViewTextSize(eventId, TypedValue.COMPLEX_UNIT_SP, 11f)
        } else {
            // Spacious / tall widget
            views.setTextViewTextSize(clockId, TypedValue.COMPLEX_UNIT_SP, 38f)
            views.setTextViewTextSize(dateId, TypedValue.COMPLEX_UNIT_SP, 17f)
            views.setTextViewTextSize(subdateId, TypedValue.COMPLEX_UNIT_SP, 12.5f)
            views.setTextViewTextSize(eventId, TypedValue.COMPLEX_UNIT_SP, 11.5f)
        }
    }

    /**
     * Applies safe text color styling without calling unsupported RemoteViews methods.
     */
    private fun applyStyle(
        views: RemoteViews,
        clockId: Int,
        dateId: Int,
        subdateId: Int,
        eventId: Int,
        style: WidgetStyle
    ) {
        when (style) {
            WidgetStyle.GRADIENT, WidgetStyle.SIMPLE -> {
                views.setTextColor(clockId, Color.WHITE)
                views.setTextColor(dateId, Color.WHITE)
                views.setTextColor(subdateId, Color.WHITE)
                views.setTextColor(eventId, Color.parseColor("#FFEB3B"))
            }
            WidgetStyle.DARK -> {
                views.setTextColor(clockId, Color.WHITE)
                views.setTextColor(dateId, Color.WHITE)
                views.setTextColor(subdateId, Color.parseColor("#CBD5E1"))
                views.setTextColor(eventId, Color.parseColor("#38BDF8"))
            }
            WidgetStyle.LIGHT -> {
                views.setTextColor(clockId, Color.parseColor("#0F172A"))
                views.setTextColor(dateId, Color.parseColor("#0F172A"))
                views.setTextColor(subdateId, Color.parseColor("#475569"))
                views.setTextColor(eventId, Color.parseColor("#0284C7"))
            }
        }
    }

    /**
     * Checks if there is a custom birthday in the Room database for today, and updates the widget.
     */
    private fun checkAndUpdateCustomBirthdays(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = com.example.data.AppDatabase.getDatabase(context)
                val today = CalendarDate.today()
                val userBdays = db.userBirthdayDao().getBirthdaysForDate(today.month, today.day)
                if (userBdays.isNotEmpty()) {
                    val b = userBdays.first()
                    val titleBn = "আজ ${b.name}-এর জন্মদিন"
                    val banglaDate = BanglaCalendarConverter.toBanglaDate(today)
                    val style = CalendarWidgetPreferences.getWidgetStyle(context)

                    for (id in appWidgetIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        val views = buildBanglaWidgetViews(context, today, banglaDate, titleBn, style, options)
                        appWidgetManager.updateAppWidget(id, views)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking custom birthdays for widget update", e)
            }
        }
    }

    /**
     * Schedules an alarm right at next midnight (00:00:02) to refresh the date and events.
     */
    fun scheduleMidnightUpdate(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, BanglaCalendarWidgetProvider::class.java).apply {
                action = ACTION_MIDNIGHT_UPDATE
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, 999, intent, flags)

            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 2)
                set(Calendar.MILLISECOND, 0)
            }

            alarmManager.set(
                AlarmManager.RTC,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling midnight widget alarm", e)
        }
    }
}
