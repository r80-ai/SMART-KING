package com.example.updater

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handles downloading APK assets from GitHub Releases or configured endpoints.
 */
class UpdateDownloader(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Directory inside app cache where downloaded update APKs reside.
     * This path is matched by `<cache-path name="cache_files" path="." />` in file_paths.xml.
     */
    val updatesDir: File
        get() = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }

    val finalApkFile: File
        get() = File(updatesDir, "smart_king_update.apk")

    val tempApkFile: File
        get() = File(updatesDir, "smart_king_update_temp.apk")

    /**
     * Downloads the APK file from the provided URL, reporting progress.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        expectedSha256: String = "",
        onProgress: (bytesDownloaded: Long, totalBytes: Long, percentage: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Check if final APK already exists and matches expected SHA-256
            if (finalApkFile.exists() && expectedSha256.isNotBlank()) {
                val currentSha256 = UpdateSecurityVerifier.calculateFileSha256(finalApkFile)
                if (currentSha256.equals(expectedSha256.replace(":", "").trim(), ignoreCase = true)) {
                    onProgress(finalApkFile.length(), finalApkFile.length(), 100)
                    return@withContext Result.success(finalApkFile)
                }
            }

            if (tempApkFile.exists()) {
                tempApkFile.delete()
            }
            if (finalApkFile.exists()) {
                finalApkFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "SMART-KING-Android-Updater")
                .header("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("HTTP download error: ${response.code} ${response.message}")
                )
            }

            val body = response.body ?: return@withContext Result.failure(
                IOException("Response body is empty")
            )

            val contentLength = body.contentLength()
            var bytesReadTotal = 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(tempApkFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var lastPercent = -1

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        currentCoroutineContext().ensureActive()
                        outputStream.write(buffer, 0, read)
                        bytesReadTotal += read

                        val percent = if (contentLength > 0L) {
                            ((bytesReadTotal * 100) / contentLength).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }

                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(bytesReadTotal, contentLength, percent)
                        }
                    }
                    outputStream.flush()
                }
            }

            // Atomically rename temp file to final APK file
            if (finalApkFile.exists()) {
                finalApkFile.delete()
            }
            val renamed = tempApkFile.renameTo(finalApkFile)
            val resultFile = if (renamed) finalApkFile else tempApkFile

            onProgress(bytesReadTotal, bytesReadTotal, 100)
            Result.success(resultFile)
        } catch (e: Exception) {
            if (tempApkFile.exists()) {
                tempApkFile.delete()
            }
            Result.failure(e)
        }
    }

    /**
     * Cleans up any cached update files.
     */
    fun clearCache() {
        try {
            if (tempApkFile.exists()) tempApkFile.delete()
            if (finalApkFile.exists()) finalApkFile.delete()
        } catch (_: Exception) {}
    }
}
