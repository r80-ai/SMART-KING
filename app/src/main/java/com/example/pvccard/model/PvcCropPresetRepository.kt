package com.example.pvccard.model

import android.content.Context
import android.content.SharedPreferences

object PvcCropPresetRepository {
    private const val PREF_NAME = "pvc_crop_presets_pref"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun savePreset(context: Context, templateId: String, front: RectFNorm, back: RectFNorm) {
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putFloat("${templateId}_f_left", front.left)
            putFloat("${templateId}_f_top", front.top)
            putFloat("${templateId}_f_width", front.width)
            putFloat("${templateId}_f_height", front.height)

            putFloat("${templateId}_b_left", back.left)
            putFloat("${templateId}_b_top", back.top)
            putFloat("${templateId}_b_width", back.width)
            putFloat("${templateId}_b_height", back.height)
            apply()
        }
    }

    fun loadPreset(context: Context, templateId: String, defaultFront: RectFNorm, defaultBack: RectFNorm): Pair<RectFNorm, RectFNorm>? {
        val prefs = getPrefs(context)
        val fLeft = prefs.getFloat("${templateId}_f_left", -1f)
        if (fLeft == -1f) return null

        val front = RectFNorm(
            left = fLeft,
            top = prefs.getFloat("${templateId}_f_top", defaultFront.top),
            width = prefs.getFloat("${templateId}_f_width", defaultFront.width),
            height = prefs.getFloat("${templateId}_f_height", defaultFront.height)
        )
        val back = RectFNorm(
            left = prefs.getFloat("${templateId}_b_left", defaultBack.left),
            top = prefs.getFloat("${templateId}_b_top", defaultBack.top),
            width = prefs.getFloat("${templateId}_b_width", defaultBack.width),
            height = prefs.getFloat("${templateId}_b_height", defaultBack.height)
        )
        return Pair(front, back)
    }

    fun clearPreset(context: Context, templateId: String) {
        val prefs = getPrefs(context)
        prefs.edit().apply {
            remove("${templateId}_f_left")
            remove("${templateId}_f_top")
            remove("${templateId}_f_width")
            remove("${templateId}_f_height")
            remove("${templateId}_b_left")
            remove("${templateId}_b_top")
            remove("${templateId}_b_width")
            remove("${templateId}_b_height")
            apply()
        }
    }
}
