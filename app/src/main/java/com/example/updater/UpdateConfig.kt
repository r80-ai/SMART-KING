package com.example.updater

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages configuration for the remote update manifest endpoint, GitHub Releases repository,
 * and APK asset selection.
 *
 * The repository owner, repository name, preferred APK asset, and manifest endpoint are fully
 * configurable and decoupled. This supports seamless migration between GitHub accounts or custom
 * servers without modifying compiled application code.
 */
class UpdateConfig(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var manifestUrl: String
        get() = prefs.getString(KEY_MANIFEST_URL, DEFAULT_MANIFEST_URL) ?: DEFAULT_MANIFEST_URL
        set(value) {
            val sanitized = sanitizeManifestUrl(value)
            if (sanitized.isNotBlank()) {
                prefs.edit().putString(KEY_MANIFEST_URL, sanitized).apply()
            }
        }

    var fallbackManifestUrl: String
        get() = prefs.getString(KEY_FALLBACK_MANIFEST_URL, DEFAULT_FALLBACK_URL) ?: DEFAULT_FALLBACK_URL
        set(value) {
            val sanitized = sanitizeManifestUrl(value)
            if (sanitized.isNotBlank()) {
                prefs.edit().putString(KEY_FALLBACK_MANIFEST_URL, sanitized).apply()
            }
        }

    var githubOwner: String
        get() = prefs.getString(KEY_GITHUB_OWNER, DEFAULT_GITHUB_OWNER) ?: DEFAULT_GITHUB_OWNER
        set(value) {
            val sanitized = value.trim()
            if (sanitized.isNotBlank()) {
                prefs.edit().putString(KEY_GITHUB_OWNER, sanitized).apply()
            }
        }

    var githubRepo: String
        get() = prefs.getString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
        set(value) {
            val sanitized = value.trim()
            if (sanitized.isNotBlank()) {
                prefs.edit().putString(KEY_GITHUB_REPO, sanitized).apply()
            }
        }

    var preferredApkAssetName: String
        get() = prefs.getString(KEY_PREFERRED_APK_ASSET, DEFAULT_PREFERRED_APK_ASSET) ?: DEFAULT_PREFERRED_APK_ASSET
        set(value) {
            val sanitized = value.trim()
            if (sanitized.isNotBlank()) {
                prefs.edit().putString(KEY_PREFERRED_APK_ASSET, sanitized).apply()
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
     * URL for querying GitHub Releases API for the latest published release.
     */
    fun getLatestReleaseApiUrl(): String {
        return "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
    }

    /**
     * URL for querying GitHub Releases API list to filter non-draft, non-prerelease versions.
     */
    fun getAllReleasesApiUrl(): String {
        return "https://api.github.com/repos/$githubOwner/$githubRepo/releases"
    }

    /**
     * Web page URL for the releases tab on GitHub.
     */
    fun getReleasesWebUrl(): String {
        return "https://github.com/$githubOwner/$githubRepo/releases"
    }

    /**
     * Resets all configuration options back to documented canonical defaults.
     */
    fun resetToDefault() {
        prefs.edit()
            .putString(KEY_MANIFEST_URL, DEFAULT_MANIFEST_URL)
            .putString(KEY_FALLBACK_MANIFEST_URL, DEFAULT_FALLBACK_URL)
            .putString(KEY_GITHUB_OWNER, DEFAULT_GITHUB_OWNER)
            .putString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO)
            .putString(KEY_PREFERRED_APK_ASSET, DEFAULT_PREFERRED_APK_ASSET)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "smart_king_update_config"
        private const val KEY_MANIFEST_URL = "remote_manifest_url"
        private const val KEY_FALLBACK_MANIFEST_URL = "remote_fallback_manifest_url"
        private const val KEY_GITHUB_OWNER = "github_owner"
        private const val KEY_GITHUB_REPO = "github_repo"
        private const val KEY_PREFERRED_APK_ASSET = "preferred_apk_asset"
        private const val KEY_CACHED_MANIFEST = "cached_manifest_json"
        private const val KEY_LAST_CHECKED = "last_checked_timestamp"

        const val DEFAULT_GITHUB_OWNER = "r80-ai"
        const val DEFAULT_GITHUB_REPO = "SMART-KING"
        const val DEFAULT_PREFERRED_APK_ASSET = "app-debug.apk"

        /**
         * Primary default remote manifest URL pointing to the published raw JSON on GitHub.
         */
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/r80-ai/SMART-KING/main/updates/smart_king_update.json"

        /**
         * Fallback secondary URL in case of filename variations.
         */
        const val DEFAULT_FALLBACK_URL =
            "https://raw.githubusercontent.com/r80-ai/SMART-KING/main/updates/smart_king_update_manifest.json"

        /**
         * Fallback release page if direct APK download cannot be completed.
         */
        const val DEFAULT_RELEASES_PAGE =
            "https://github.com/r80-ai/SMART-KING/releases"

        /**
         * Sanitizes user input for manifest URL.
         */
        fun sanitizeManifestUrl(input: String): String {
            var url = input.trim()
            if (url.startsWith("http://", ignoreCase = true)) {
                // Prefer HTTPS for security
                url = "https://" + url.substring(7)
            }
            return url
        }
    }
}
