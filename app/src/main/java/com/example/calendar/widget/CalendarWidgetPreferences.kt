package com.example.calendar.widget

import android.content.Context
import android.content.SharedPreferences

enum class WidgetStyle(val key: String, val titleBn: String, val titleEn: String) {
    GRADIENT("gradient", "পার্পল-ব্লু গ্রেডিয়েন্ট (ডিফল্ট)", "Purple/Blue Gradient (Default)"),
    DARK("dark", "ডার্ক স্লেট", "Dark Slate"),
    LIGHT("light", "লাইট মিনিমাল", "Light Minimal"),
    SIMPLE("simple", "সিম্পল গ্লাস", "Simple Glass");

    companion object {
        fun fromKey(key: String): WidgetStyle {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: GRADIENT
        }
    }
}

object CalendarWidgetPreferences {
    private const val PREF_NAME = "calendar_widget_prefs"
    private const val KEY_WIDGET_STYLE = "key_widget_style"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getWidgetStyle(context: Context): WidgetStyle {
        val key = getPrefs(context).getString(KEY_WIDGET_STYLE, WidgetStyle.GRADIENT.key) ?: WidgetStyle.GRADIENT.key
        return WidgetStyle.fromKey(key)
    }

    fun setWidgetStyle(context: Context, style: WidgetStyle) {
        getPrefs(context).edit().putString(KEY_WIDGET_STYLE, style.key).apply()
        CalendarWidgetUpdater.updateAllWidgets(context)
    }
}
