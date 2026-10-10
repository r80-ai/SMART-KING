package com.example.calendar.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

class BanglaCalendarWidgetProvider : AppWidgetProvider() {

    private val TAG = "BanglaCalendarWidget"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        try {
            CalendarWidgetUpdater.updateBanglaWidgets(context, appWidgetManager, appWidgetIds)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in onUpdate", e)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        try {
            CalendarWidgetUpdater.updateBanglaWidget(context, appWidgetManager, appWidgetId, newOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in onAppWidgetOptionsChanged", e)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        try {
            CalendarWidgetUpdater.scheduleMidnightUpdate(context)
            CalendarWidgetUpdater.updateAllWidgets(context)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in onEnabled", e)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        try {
            val action = intent.action
            if (action == CalendarWidgetUpdater.ACTION_MIDNIGHT_UPDATE ||
                action == Intent.ACTION_DATE_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED ||
                action == Intent.ACTION_TIME_CHANGED ||
                action == "android.intent.action.TIME_SET" ||
                action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_LOCALE_CHANGED
            ) {
                CalendarWidgetUpdater.updateAllWidgets(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in onReceive", e)
        }
    }
}
