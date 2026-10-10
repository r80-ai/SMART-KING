package com.example.calendar.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.calendar.model.CalendarDate
import com.example.calendar.util.BanglaCalendarConverter
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BanglaCalendarWidgetTest {

    @Test
    fun testWidgetLayoutCanBeInflatedByRemoteViews() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_bangla_calendar)
        val inflated = views.apply(context, null)
        assertNotNull("Inflated view must not be null", inflated)
    }

    @Test
    fun testWidgetPreviewLayoutCanBeInflatedByRemoteViews() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_bangla_calendar_preview)
        val inflated = views.apply(context, null)
        assertNotNull("Inflated preview view must not be null", inflated)
    }

    @Test
    fun testUpdateBanglaWidgetLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 65)
        }

        CalendarWidgetUpdater.updateBanglaWidget(context, appWidgetManager, 1001, options)
        CalendarWidgetUpdater.updateBanglaWidgets(context, appWidgetManager, intArrayOf(1001, 1002, 1003))
    }

    @Test
    fun testWidgetBuildViewsWithAndWithoutFestival() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val today = CalendarDate.today()
        val banglaDate = BanglaCalendarConverter.toBanglaDate(today)

        // Without festival
        val viewsNoEvent = CalendarWidgetUpdater.buildBanglaWidgetViews(
            context, today, banglaDate, null, WidgetStyle.GRADIENT, null
        )
        val inflatedNoEvent = viewsNoEvent.apply(context, null)
        assertNotNull(inflatedNoEvent)

        // With festival
        val viewsWithEvent = CalendarWidgetUpdater.buildBanglaWidgetViews(
            context, today, banglaDate, "বিশ্বকর্মা পূজা", WidgetStyle.GRADIENT, null
        )
        val inflatedWithEvent = viewsWithEvent.apply(context, null)
        assertNotNull(inflatedWithEvent)
    }

    @Test
    fun testBanglaCalendarConversionForToday() {
        val today = CalendarDate.today()
        val banglaDate = BanglaCalendarConverter.toBanglaDate(today)
        val shortStr = banglaDate.formatShort()
        val fullGregorian = banglaDate.formatGregorianFullInBangla(today)
        assertNotNull(shortStr)
        assertNotNull(fullGregorian)
    }
}
