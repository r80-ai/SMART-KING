package com.example.updater

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

/**
 * Metadata for an asset uploaded to a GitHub Release.
 */
data class GitHubReleaseAsset(
    val id: Long,
    val name: String,
    val size: Long,
    val downloadUrl: String,
    val contentType: String
)

/**
 * Metadata for a release published on GitHub.
 */
data class GitHubReleaseInfo(
    val id: Long,
    val tagName: String,
    val name: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val body: String,
    val publishedAt: String,
    val assets: List<GitHubReleaseAsset>
)

/**
 * Helper to fetch, parse, and select assets from the GitHub Releases API.
 */
object GitHubReleaseHelper {

    private val SHA256_PATTERN = Pattern.compile("(?i)(?:sha-?256|checksum)[:\\s]+([a-f0-9]{64})")
    private val VERSION_CODE_PATTERN = Pattern.compile("(?i)(?:version-?code|build)[:\\s]+(\\d+)")

    /**
     * Fetches the latest published release from the GitHub Releases API for the specified repository.
     */
    fun fetchLatestRelease(
        client: OkHttpClient,
        owner: String,
        repo: String
    ): Result<GitHubReleaseInfo> {
        val cleanOwner = owner.trim()
        val cleanRepo = repo.trim()

        if (cleanOwner.isBlank() || cleanRepo.isBlank()) {
            return Result.failure(IllegalArgumentException("GitHub owner and repository name cannot be empty."))
        }

        // 1. Try /releases/latest endpoint
        val latestUrl = "https://api.github.com/repos/$cleanOwner/$cleanRepo/releases/latest"
        val request = Request.Builder()
            .url(latestUrl)
            .header("User-Agent", "SMART-KING-Android-Updater")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        try {
            val response = client.newCall(request).execute()
            when (response.code) {
                200 -> {
                    val bodyString = response.body?.string()
                        ?: return Result.failure(IOException("Empty response body from GitHub API."))
                    val releaseObj = JSONObject(bodyString)
                    return Result.success(parseRelease(releaseObj))
                }
                404 -> {
                    // /releases/latest may 404 if only prereleases exist or none marked "latest".
                    // Fall back to querying /releases list and picking first non-draft release.
                    return fetchFirstValidReleaseFromList(client, cleanOwner, cleanRepo)
                }
                403, 429 -> {
                    return Result.failure(
                        IOException("GitHub API rate limit reached (HTTP ${response.code}). Please wait a few minutes or retry later.")
                    )
                }
                in 500..599 -> {
                    return Result.failure(
                        IOException("GitHub server error (HTTP ${response.code}). Please retry later.")
                    )
                }
                else -> {
                    return Result.failure(
                        IOException("Failed to query GitHub Releases (HTTP ${response.code}: ${response.message}).")
                    )
                }
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun fetchFirstValidReleaseFromList(
        client: OkHttpClient,
        owner: String,
        repo: String
    ): Result<GitHubReleaseInfo> {
        val listUrl = "https://api.github.com/repos/$owner/$repo/releases?per_page=10"
        val request = Request.Builder()
            .url(listUrl)
            .header("User-Agent", "SMART-KING-Android-Updater")
            .header("Accept", "application/vnd.github+json")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(
                    IOException("GitHub repository '$owner/$repo' releases not found (HTTP ${response.code}). Check repository name and visibility.")
                )
            }

            val body = response.body?.string()
                ?: return Result.failure(IOException("Empty response body from GitHub API."))
            val array = JSONArray(body)

            for (i in 0 until array.length()) {
                val relObj = array.getJSONObject(i)
                val release = parseRelease(relObj)
                // Filter out draft releases
                if (!release.draft) {
                    return Result.success(release)
                }
            }

            Result.failure(
                IOException("No published releases found in repository '$owner/$repo'.")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Selects the most appropriate APK asset from the release.
     */
    fun selectApkAsset(
        release: GitHubReleaseInfo,
        preferredAssetName: String = "app-debug.apk"
    ): Result<GitHubReleaseAsset> {
        val apkAssets = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }

        if (apkAssets.isEmpty()) {
            val allAssetNames = release.assets.map { it.name }.joinToString(", ")
            return Result.failure(
                IllegalStateException(
                    "No APK asset found in release '${release.name}' (tag: ${release.tagName}). Available assets: [${allAssetNames.ifEmpty { "None" }}]."
                )
            )
        }

        // Priority 1: Exact case-insensitive match for preferred asset (e.g. app-debug.apk)
        val preferredMatch = apkAssets.firstOrNull { it.name.equals(preferredAssetName.trim(), ignoreCase = true) }
        if (preferredMatch != null) {
            return Result.success(preferredMatch)
        }

        // Priority 2: Release APK (e.g. smart-king-release.apk or app-release.apk)
        val releaseMatch = apkAssets.firstOrNull { it.name.contains("release", ignoreCase = true) }
        if (releaseMatch != null) {
            return Result.success(releaseMatch)
        }

        // Priority 3: Debug APK (e.g. app-debug.apk)
        val debugMatch = apkAssets.firstOrNull { it.name.contains("debug", ignoreCase = true) }
        if (debugMatch != null) {
            return Result.success(debugMatch)
        }

        // Priority 4: First available APK
        return Result.success(apkAssets.first())
    }

    /**
     * Extracts optional SHA-256 checksum from the release notes body if published there.
     */
    fun extractSha256FromBody(body: String): String? {
        val matcher = SHA256_PATTERN.matcher(body)
        return if (matcher.find()) {
            matcher.group(1)?.lowercase()
        } else null
    }

    /**
     * Extracts optional versionCode from tag or release notes body (e.g. build-42 -> 42).
     */
    fun extractVersionCode(tagName: String, body: String): Long? {
        val bodyMatcher = VERSION_CODE_PATTERN.matcher(body)
        if (bodyMatcher.find()) {
            bodyMatcher.group(1)?.toLongOrNull()?.let { return it }
        }

        val tagDigits = tagName.filter { it.isDigit() }
        if (tagDigits.isNotBlank()) {
            return tagDigits.toLongOrNull()
        }

        return null
    }

    private fun parseRelease(obj: JSONObject): GitHubReleaseInfo {
        val id = obj.optLong("id", 0L)
        val tagName = obj.optString("tag_name", "latest")
        val name = obj.optString("name", tagName)
        val draft = obj.optBoolean("draft", false)
        val prerelease = obj.optBoolean("prerelease", false)
        val body = obj.optString("body", "")
        val publishedAt = obj.optString("published_at", "")

        val assetsList = mutableListOf<GitHubReleaseAsset>()
        val assetsArray = obj.optJSONArray("assets")
        if (assetsArray != null) {
            for (i in 0 until assetsArray.length()) {
                val aObj = assetsArray.getJSONObject(i)
                assetsList.add(
                    GitHubReleaseAsset(
                        id = aObj.optLong("id", 0L),
                        name = aObj.optString("name", ""),
                        size = aObj.optLong("size", 0L),
                        downloadUrl = aObj.optString("browser_download_url", ""),
                        contentType = aObj.optString("content_type", "")
                    )
                )
            }
        }

        return GitHubReleaseInfo(
            id = id,
            tagName = tagName,
            name = name,
            draft = draft,
            prerelease = prerelease,
            body = body,
            publishedAt = publishedAt,
            assets = assetsList
        )
    }
}
