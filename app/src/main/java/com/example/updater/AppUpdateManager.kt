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
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _currentManifest = MutableStateFlow<UpdateManifest?>(null)
    val currentManifest: StateFlow<UpdateManifest?> = _currentManifest.asStateFlow()

    val isMandatoryUpdateActive: StateFlow<Boolean> = _updateState
        .map { state ->
            when (state) {
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
        // Load cached manifest on startup to prevent bypassing previously known mandatory updates
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
     * Checks for updates against the remote manifest.
     */
    fun checkForUpdates(isManual: Boolean = false) {
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
                // Check if we have a cached mandatory update
                val cached = _currentManifest.value
                if (cached != null && cached.isMandatory(installedVersionCode)) {
                    _updateState.value = UpdateState.Error(
                        message = "Network error: ${err.message}. A required update is pending.",
                        canRetry = true,
                        isMandatory = true,
                        manifest = cached
                    )
                } else {
                    _updateState.value = UpdateState.Error(
                        message = err.message ?: "Failed to check for updates.",
                        canRetry = true,
                        isMandatory = false,
                        manifest = cached
                    )
                }
            }
        }
    }

    /**
     * Starts downloading the update APK.
     */
    fun startDownload(manifest: UpdateManifest) {
        scope.launch {
            val isMandatory = manifest.isMandatory(installedVersionCode)
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
                // Perform complete security and integrity verification
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
                    message = "Download failed: ${downloadErr.message}",
                    canRetry = true,
                    isMandatory = isMandatory,
                    manifest = manifest
                )
            }
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
     * Checks if the app was just updated upon resume.
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
                // If the error was on an optional update, don't keep blocking
                _updateState.value = UpdateState.Available(manifest, isMandatory = false)
            }
        }
    }

    /**
     * Direct link to GitHub Releases web page if user chooses manual browser download.
     */
    fun openReleasesPage(activity: Activity) {
        val url = _currentManifest.value?.releasePageUrl?.takeIf { it.isNotBlank() }
            ?: UpdateConfig.DEFAULT_RELEASES_PAGE
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
        // Try primary URL first
        val primaryResult = fetchManifestFromUrl(config.manifestUrl)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        // Try fallback URL if primary fails and is different
        if (config.fallbackManifestUrl.isNotBlank() && config.fallbackManifestUrl != config.manifestUrl) {
            val fallbackResult = fetchManifestFromUrl(config.fallbackManifestUrl)
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
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP error ${response.code}: ${response.message}"))
            }

            val responseBody = response.body?.string() ?: return Result.failure(IOException("Empty response body"))
            val manifest = UpdateManifest.fromJson(responseBody)
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
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
