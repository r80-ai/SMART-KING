package com.example.updater

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages configuration for the remote update manifest endpoint and release repositories.
 * The manifest endpoint is completely decoupled and can be updated remotely or migrated to
 * a different GitHub user, repository, or private/public server without breaking the update client.
 */
class UpdateConfig(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var manifestUrl: String
        get() = prefs.getString(KEY_MANIFEST_URL, DEFAULT_MANIFEST_URL) ?: DEFAULT_MANIFEST_URL
        set(value) {
            val sanitized = value.trim()
            if (sanitized.isNotBlank()) {
                prefs.edit().putString(KEY_MANIFEST_URL, sanitized).apply()
            }
        }

    var fallbackManifestUrl: String
        get() = prefs.getString(KEY_FALLBACK_MANIFEST_URL, DEFAULT_FALLBACK_URL) ?: DEFAULT_FALLBACK_URL
        set(value) {
            val sanitized = value.trim()
            if (sanitized.isNotBlank()) {
                prefs.edit().putString(KEY_FALLBACK_MANIFEST_URL, sanitized).apply()
            }
        }

    var lastCachedManifestJson: String?
        get() = prefs.getString(KEY_CACHED_MANIFEST, null)
        set(value) {
            prefs.edit().putString(KEY_CACHED_MANIFEST, value).apply()
        }

    var lastCheckedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHECKED, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_CHECKED, value).apply()
        }

    /**
     * Resets the manifest URL back to default.
     */
    fun resetToDefault() {
        prefs.edit()
            .putString(KEY_MANIFEST_URL, DEFAULT_MANIFEST_URL)
            .putString(KEY_FALLBACK_MANIFEST_URL, DEFAULT_FALLBACK_URL)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "smart_king_update_config"
        private const val KEY_MANIFEST_URL = "remote_manifest_url"
        private const val KEY_FALLBACK_MANIFEST_URL = "remote_fallback_manifest_url"
        private const val KEY_CACHED_MANIFEST = "cached_manifest_json"
        private const val KEY_LAST_CHECKED = "last_checked_timestamp"

        /**
         * Default remote configuration manifest URL hosted in the repository.
         * Raw GitHub Content is served directly over HTTPS with standard caching headers.
         */
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/r80-ai/SMART-KING/main/updates/smart_king_update_manifest.json"

        /**
         * Fallback secondary URL (e.g. GitHub Pages or alternate CDN).
         */
        const val DEFAULT_FALLBACK_URL =
            "https://r80-ai.github.io/SMART-KING/updates/smart_king_update_manifest.json"

        /**
         * Fallback release page if direct APK download cannot be completed.
         */
        const val DEFAULT_RELEASES_PAGE =
            "https://github.com/r80-ai/SMART-KING/releases"
    }
}
