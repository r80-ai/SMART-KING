package com.example.updater

import org.json.JSONObject

/**
 * Representation of the remote update manifest hosted on GitHub Releases / GitHub Pages / custom HTTPS server.
 */
data class UpdateManifest(
    val manifestSchemaVersion: Int = 1,
    val appId: String = "com.aistudio.pdfmaster.qvrx",
    val latestVersionCode: Long,
    val latestVersionName: String,
    val minimumSupportedVersionCode: Long = 1,
    val mandatoryUpdate: Boolean = false,
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val releasePageUrl: String = "",
    val apkSha256: String = "",
    val signingCertificateSha256: String? = null,
    val publishedAt: String = "",
    val fileSizeBytes: Long = 0L,
    val nextManifestUrl: String? = null
) {
    /**
     * Checks if this manifest represents a newer version than currently installed.
     */
    fun isUpdateAvailable(installedVersionCode: Long): Boolean {
        return latestVersionCode > installedVersionCode
    }

    /**
     * Checks if the update is strictly mandatory for the currently installed version.
     * Update is mandatory if:
     * 1. A newer version is available AND
     * 2. Either mandatoryUpdate flag is true OR installedVersionCode is below minimumSupportedVersionCode
     */
    fun isMandatory(installedVersionCode: Long): Boolean {
        return isUpdateAvailable(installedVersionCode) &&
                (mandatoryUpdate || installedVersionCode < minimumSupportedVersionCode)
    }

    /**
     * Serializes this manifest to a JSON string for caching.
     */
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("manifestSchemaVersion", manifestSchemaVersion)
        obj.put("appId", appId)
        obj.put("latestVersionCode", latestVersionCode)
        obj.put("latestVersionName", latestVersionName)
        obj.put("minimumSupportedVersionCode", minimumSupportedVersionCode)
        obj.put("mandatoryUpdate", mandatoryUpdate)
        obj.put("releaseTitle", releaseTitle)
        obj.put("releaseNotes", releaseNotes)
        obj.put("downloadUrl", downloadUrl)
        obj.put("releasePageUrl", releasePageUrl)
        obj.put("apkSha256", apkSha256)
        if (!signingCertificateSha256.isNullOrBlank()) {
            obj.put("signingCertificateSha256", signingCertificateSha256)
        }
        obj.put("publishedAt", publishedAt)
        obj.put("fileSizeBytes", fileSizeBytes)
        if (!nextManifestUrl.isNullOrBlank()) {
            obj.put("nextManifestUrl", nextManifestUrl)
        }
        return obj.toString(2)
    }

    companion object {
        /**
         * Parses an UpdateManifest from a JSON string.
         * Throws IllegalArgumentException if required fields are missing or invalid.
         */
        fun fromJson(jsonStr: String): UpdateManifest {
            val obj = JSONObject(jsonStr)

            val appId = obj.optString("appId", "com.aistudio.pdfmaster.qvrx")
            val latestVersionCode = obj.optLong("latestVersionCode", -1L)
            if (latestVersionCode <= 0) {
                throw IllegalArgumentException("Invalid or missing 'latestVersionCode' in update manifest.")
            }

            val latestVersionName = obj.optString("latestVersionName", "1.0")
            val minimumSupportedVersionCode = obj.optLong("minimumSupportedVersionCode", 1L)
            val mandatoryUpdate = obj.optBoolean("mandatoryUpdate", false)
            val releaseTitle = obj.optString("releaseTitle", "New SMART KING Release")
            val releaseNotes = obj.optString("releaseNotes", "")
            val downloadUrl = obj.optString("downloadUrl", "")
            val releasePageUrl = obj.optString("releasePageUrl", "")
            val apkSha256 = obj.optString("apkSha256", "").trim()
            val signingCertificateSha256 = if (obj.has("signingCertificateSha256")) {
                obj.optString("signingCertificateSha256").trim().takeIf { it.isNotBlank() }
            } else null
            val publishedAt = obj.optString("publishedAt", "")
            val fileSizeBytes = obj.optLong("fileSizeBytes", 0L)
            val nextManifestUrl = if (obj.has("nextManifestUrl")) {
                obj.optString("nextManifestUrl").trim().takeIf { it.isNotBlank() }
            } else null
            val schemaVersion = obj.optInt("manifestSchemaVersion", 1)

            return UpdateManifest(
                manifestSchemaVersion = schemaVersion,
                appId = appId,
                latestVersionCode = latestVersionCode,
                latestVersionName = latestVersionName,
                minimumSupportedVersionCode = minimumSupportedVersionCode,
                mandatoryUpdate = mandatoryUpdate,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                releasePageUrl = releasePageUrl,
                apkSha256 = apkSha256,
                signingCertificateSha256 = signingCertificateSha256,
                publishedAt = publishedAt,
                fileSizeBytes = fileSizeBytes,
                nextManifestUrl = nextManifestUrl
            )
        }
    }
}
