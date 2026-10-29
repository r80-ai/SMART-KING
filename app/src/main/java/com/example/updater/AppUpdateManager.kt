package com.example.updater

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class CheckingRelease(val message: String) : UpdateState()
    data class UpToDate(val installedVersionCode: Long, val lastChecked: Long) : UpdateState()
    data class Available(val manifest: UpdateManifest, val isMandatory: Boolean) : UpdateState()
    data class Downloading(
        val manifest: UpdateManifest,
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val isMandatory: Boolean
    ) : UpdateState()
    data class Downloaded(
        val manifest: UpdateManifest,
        val apkFile: File,
        val isMandatory: Boolean
    ) : UpdateState()
    data class Installing(
        val manifest: UpdateManifest,
        val apkFile: File,
        val isMandatory: Boolean
    ) : UpdateState()
    data class Error(
        val message: String,
        val canRetry: Boolean,
        val isMandatory: Boolean,
        val manifest: UpdateManifest? = null
    ) : UpdateState()
}

class AppUpdateManager private constructor(private val appContext: Context) {

    val config = UpdateConfig(appContext)
    val downloader = UpdateDownloader(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _currentManifest = MutableStateFlow<UpdateManifest?>(null)
    val currentManifest: StateFlow<UpdateManifest?> = _currentManifest.asStateFlow()

    val isMandatoryUpdateActive: StateFlow<Boolean> = _updateState
        .map { state ->
            when (state) {
                is UpdateState.CheckingRelease -> {
                    // Retain mandatory lock if current manifest is mandatory
                    _currentManifest.value?.isMandatory(installedVersionCode) ?: false
                }
                is UpdateState.Available -> state.isMandatory
                is UpdateState.Downloading -> state.isMandatory
                is UpdateState.Downloaded -> state.isMandatory
                is UpdateState.Installing -> state.isMandatory
                is UpdateState.Error -> state.isMandatory
                else -> false
            }
        }.stateIn(scope, SharingStarted.Eagerly, false)

    val installedVersionCode: Long
        get() {
            return try {
                val pInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                PackageInfoCompat.getLongVersionCode(pInfo)
            } catch (e: Exception) {
                1L
            }
        }

    val installedVersionName: String
        get() {
            return try {
                val pInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                pInfo.versionName ?: "1.0"
            } catch (e: Exception) {
                "1.0"
            }
        }

    init {
        // Load cached manifest on startup to prevent bypassing previously verified mandatory updates
        config.lastCachedManifestJson?.let { cachedJson ->
            try {
                val cached = UpdateManifest.fromJson(cachedJson)
                _currentManifest.value = cached
                if (cached.isMandatory(installedVersionCode)) {
                    _updateState.value = UpdateState.Available(cached, isMandatory = true)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Checks for updates against the configured remote manifest URL.
     */
    fun checkForUpdates(isManual: Boolean = false) {
        if (_updateState.value is UpdateState.Downloading || _updateState.value is UpdateState.Installing) {
            return // Prevent interrupting ongoing operations
        }

        scope.launch {
            _updateState.value = UpdateState.Checking
            val result = fetchRemoteManifest()

            result.onSuccess { manifest ->
                _currentManifest.value = manifest
                config.lastCachedManifestJson = manifest.toJson()
                config.lastCheckedTimestamp = System.currentTimeMillis()

                // If remote manifest provides a nextManifestUrl, apply it for future checks
                manifest.nextManifestUrl?.let { nextUrl ->
                    if (nextUrl.isNotBlank() && nextUrl != config.manifestUrl) {
                        config.manifestUrl = nextUrl
                    }
                }

                val currentCode = installedVersionCode
                if (manifest.isUpdateAvailable(currentCode)) {
                    val mandatory = manifest.isMandatory(currentCode)
                    _updateState.value = UpdateState.Available(manifest, isMandatory = mandatory)
                } else {
                    _updateState.value = UpdateState.UpToDate(currentCode, config.lastCheckedTimestamp)
                    downloader.clearCache()
                }
            }.onFailure { err ->
                val userErrorMessage = formatErrorMessage(err)
                val cached = _currentManifest.value
                val isMandatory = cached?.isMandatory(installedVersionCode) ?: false

                _updateState.value = UpdateState.Error(
                    message = userErrorMessage,
                    canRetry = true,
                    isMandatory = isMandatory,
                    manifest = cached
                )
            }
        }
    }

    /**
     * Directly queries GitHub Releases API for the latest published release, identifies the APK asset,
     * and downloads it in-app with streaming progress without launching Chrome.
     */
    fun fetchAndDownloadLatestReleaseApk(activity: Activity? = null) {
        val currentState = _updateState.value
        if (currentState is UpdateState.Downloading || currentState is UpdateState.Installing) {
            return // Prevent duplicate downloads when tapped repeatedly
        }

        val wasMandatory = isMandatoryUpdateActive.value ||
                (_currentManifest.value?.isMandatory(installedVersionCode) ?: false)

        scope.launch {
            _updateState.value = UpdateState.CheckingRelease(
                message = "GitHub থেকে সর্বশেষ রিলিজ খোঁজা হচ্ছে..."
            )

            val releaseResult = withContext(Dispatchers.IO) {
                GitHubReleaseHelper.fetchLatestRelease(httpClient, config.githubOwner, config.githubRepo)
            }

            releaseResult.onSuccess { release ->
                val assetResult = GitHubReleaseHelper.selectApkAsset(release, config.preferredApkAssetName)

                assetResult.onSuccess { asset ->
                    // Build manifest representation from GitHub Release API response
                    val parsedVersionCode = GitHubReleaseHelper.extractVersionCode(release.tagName, release.body)
                        ?: (installedVersionCode + 1)
                    val parsedSha256 = GitHubReleaseHelper.extractSha256FromBody(release.body) ?: ""

                    val synthesizedManifest = UpdateManifest(
                        manifestSchemaVersion = 1,
                        appId = appContext.packageName,
                        latestVersionCode = parsedVersionCode,
                        latestVersionName = release.tagName.removePrefix("v"),
                        minimumSupportedVersionCode = installedVersionCode,
                        mandatoryUpdate = wasMandatory,
                        releaseTitle = release.name.ifBlank { "SMART KING ${release.tagName}" },
                        releaseNotes = release.body.ifBlank { "Direct update from GitHub Releases (${release.tagName})" },
                        downloadUrl = asset.downloadUrl,
                        releasePageUrl = "https://github.com/${config.githubOwner}/${config.githubRepo}/releases/tag/${release.tagName}",
                        apkSha256 = parsedSha256,
                        fileSizeBytes = asset.size
                    )

                    _currentManifest.value = synthesizedManifest

                    // Immediately start downloading the APK with progress tracking
                    startDownloadInternal(synthesizedManifest, wasMandatory, activity)
                }.onFailure { assetErr ->
                    _updateState.value = UpdateState.Error(
                        message = assetErr.message ?: "No compatible APK asset found in release.",
                        canRetry = true,
                        isMandatory = wasMandatory,
                        manifest = _currentManifest.value
                    )
                }
            }.onFailure { releaseErr ->
                _updateState.value = UpdateState.Error(
                    message = formatErrorMessage(releaseErr),
                    canRetry = true,
                    isMandatory = wasMandatory,
                    manifest = _currentManifest.value
                )
            }
        }
    }

    /**
     * Starts downloading the update APK based on an existing verified manifest.
     */
    fun startDownload(manifest: UpdateManifest) {
        val currentState = _updateState.value
        if (currentState is UpdateState.Downloading || currentState is UpdateState.Installing) {
            return
        }

        val isMandatory = manifest.isMandatory(installedVersionCode)
        scope.launch {
            startDownloadInternal(manifest, isMandatory, null)
        }
    }

    private suspend fun startDownloadInternal(
        manifest: UpdateManifest,
        isMandatory: Boolean,
        activity: Activity?
    ) {
        _updateState.value = UpdateState.Downloading(
            manifest = manifest,
            progressPercent = 0,
            bytesDownloaded = 0L,
            totalBytes = manifest.fileSizeBytes,
            isMandatory = isMandatory
        )

        val downloadResult = downloader.downloadApk(
            downloadUrl = manifest.downloadUrl,
            expectedSha256 = manifest.apkSha256
        ) { bytesDownloaded, totalBytes, percent ->
            _updateState.value = UpdateState.Downloading(
                manifest = manifest,
                progressPercent = percent,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                isMandatory = isMandatory
            )
        }

        downloadResult.onSuccess { apkFile ->
            // Perform security, package identity, version, and signature verification
            val verification = UpdateSecurityVerifier.verifyApk(
                context = appContext,
                apkFile = apkFile,
                manifest = manifest,
                installedVersionCode = installedVersionCode
            )

            when (verification) {
                is VerificationResult.Success -> {
                    _updateState.value = UpdateState.Downloaded(
                        manifest = manifest,
                        apkFile = apkFile,
                        isMandatory = isMandatory
                    )
                    // If activity was provided, can prompt for installation immediately
                    if (activity != null) {
                        installApk(activity, apkFile)
                    }
                }
                is VerificationResult.Failure -> {
                    val errorDetail = verification.detailedError ?: ""
                    _updateState.value = UpdateState.Error(
                        message = "${verification.reason} $errorDetail".trim(),
                        canRetry = true,
                        isMandatory = isMandatory,
                        manifest = manifest
                    )
                    apkFile.delete()
                }
            }
        }.onFailure { downloadErr ->
            _updateState.value = UpdateState.Error(
                message = "APK Download failed: ${downloadErr.message}",
                canRetry = true,
                isMandatory = isMandatory,
                manifest = manifest
            )
        }
    }

    /**
     * Installs the downloaded APK via standard system Package Installer.
     */
    fun installApk(activity: Activity, apkFile: File): Boolean {
        return try {
            val isMandatory = _currentManifest.value?.isMandatory(installedVersionCode) ?: false
            _updateState.value = UpdateState.Installing(
                manifest = _currentManifest.value ?: return false,
                apkFile = apkFile,
                isMandatory = isMandatory
            )

            // Check Unknown Sources permission on Android 8.0+ (Oreo+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${activity.packageName}")
                    )
                    activity.startActivity(permissionIntent)
                    return false
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            activity.startActivity(installIntent)
            true
        } catch (e: Exception) {
            val manifest = _currentManifest.value
            _updateState.value = UpdateState.Error(
                message = "Installation failed to start: ${e.message}",
                canRetry = true,
                isMandatory = manifest?.isMandatory(installedVersionCode) ?: false,
                manifest = manifest
            )
            false
        }
    }

    /**
     * Checks if the app was updated upon resume.
     * If installed version >= manifest's latest version or minimum required version,
     * the lock is lifted and the cache is cleared.
     */
    fun onAppResumed() {
        val currentCode = installedVersionCode
        val manifest = _currentManifest.value
        if (manifest != null) {
            if (currentCode >= manifest.latestVersionCode) {
                _updateState.value = UpdateState.UpToDate(currentCode, System.currentTimeMillis())
                downloader.clearCache()
                config.lastCachedManifestJson = null
            } else if (!manifest.isMandatory(currentCode) && _updateState.value is UpdateState.Error) {
                _updateState.value = UpdateState.Available(manifest, isMandatory = false)
            }
        }
    }

    /**
     * Opens GitHub Releases web page if user explicitly requests manual browser viewing.
     */
    fun openReleasesPage(activity: Activity) {
        val url = _currentManifest.value?.releasePageUrl?.takeIf { it.isNotBlank() }
            ?: config.getReleasesWebUrl()
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(browserIntent)
        } catch (_: Exception) {}
    }

    fun dismissOptionalDialog() {
        if (!isMandatoryUpdateActive.value) {
            val currentCode = installedVersionCode
            _updateState.value = UpdateState.UpToDate(currentCode, config.lastCheckedTimestamp)
        }
    }

    private suspend fun fetchRemoteManifest(): Result<UpdateManifest> = withContext(Dispatchers.IO) {
        val configuredUrl = config.manifestUrl.trim()

        // 1. Try primary URL
        val primaryResult = fetchManifestFromUrl(configuredUrl)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        // 2. If configured URL gave 404 and does not end in .json, try appending .json
        if (primaryResult.isFailure && !configuredUrl.endsWith(".json", ignoreCase = true)) {
            val jsonUrl = "$configuredUrl.json"
            val altResult = fetchManifestFromUrl(jsonUrl)
            if (altResult.isSuccess) {
                // Update config to valid URL with .json
                config.manifestUrl = jsonUrl
                return@withContext altResult
            }
        }

        // 3. Try fallback URL if primary fails and fallback is distinct
        val fallbackUrl = config.fallbackManifestUrl.trim()
        if (fallbackUrl.isNotBlank() && fallbackUrl != configuredUrl) {
            val fallbackResult = fetchManifestFromUrl(fallbackUrl)
            if (fallbackResult.isSuccess) {
                return@withContext fallbackResult
            }
        }

        primaryResult
    }

    private fun fetchManifestFromUrl(url: String): Result<UpdateManifest> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SMART-KING-Android-Updater")
                .header("Accept", "application/json, text/plain, */*")
                .header("Cache-Control", "no-cache")
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code

            when {
                code == 200 -> {
                    val responseBody = response.body?.string()
                        ?: return Result.failure(IOException("Empty response body from update server."))
                    val manifest = UpdateManifest.fromJson(responseBody)
                    Result.success(manifest)
                }
                code == 404 -> {
                    Result.failure(
                        IOException("Update manifest not found (HTTP 404). Check endpoint URL or ensure updates/smart_king_update.json is published on GitHub.")
                    )
                }
                code == 403 || code == 429 -> {
                    Result.failure(
                        IOException("Update server access rate limited (HTTP $code). Please try again later.")
                    )
                }
                code in 500..599 -> {
                    Result.failure(
                        IOException("Update server encountered an error (HTTP $code). Please try again later.")
                    )
                }
                else -> {
                    Result.failure(
                        IOException("HTTP error $code: ${response.message}")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "No internet connection or DNS failed. Please check network."
            is SocketTimeoutException -> "Network timeout while checking for updates. Please retry."
            is JSONException, is IllegalArgumentException -> "Corrupted or invalid update manifest: ${throwable.message}"
            else -> throwable.message ?: "Failed to check for updates."
        }
    }

    companion object {
        @Volatile
        private var instance: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
