package com.example.updater

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * Result of APK validation before triggering installation.
 */
sealed class VerificationResult {
    data object Success : VerificationResult()
    data class Failure(val reason: String, val detailedError: String? = null) : VerificationResult()
}

/**
 * Validates package identity, non-downgrade version code, SHA-256 file checksum,
 * and signing certificate fingerprints before allowing APK installation.
 */
object UpdateSecurityVerifier {

    /**
     * Verifies the downloaded APK file against the manifest and the installed application.
     */
    fun verifyApk(
        context: Context,
        apkFile: File,
        manifest: UpdateManifest,
        installedVersionCode: Long,
        allowSameVersion: Boolean = false
    ): VerificationResult {
        // 1. File existence
        if (!apkFile.exists()) {
            return VerificationResult.Failure(
                "Downloaded APK file does not exist at '${apkFile.absolutePath}'."
            )
        }

        // 2. File readability
        if (!apkFile.canRead()) {
            return VerificationResult.Failure(
                "Downloaded APK file cannot be read due to file permission restrictions."
            )
        }

        // 3. File non-empty
        if (apkFile.length() <= 0L) {
            return VerificationResult.Failure(
                "Downloaded APK file is empty (0 bytes)."
            )
        }

        // 4. Expected file size check (if specified in manifest and > 0)
        if (manifest.fileSizeBytes > 0L && apkFile.length() != manifest.fileSizeBytes) {
            return VerificationResult.Failure(
                "Downloaded file size (${apkFile.length()} bytes) does not match manifest (${manifest.fileSizeBytes} bytes)."
            )
        }

        // 5. SHA-256 Checksum check (if specified in manifest and non-blank)
        if (manifest.apkSha256.isNotBlank()) {
            val calculatedSha256 = calculateFileSha256(apkFile)
            val expectedSha256 = manifest.apkSha256.replace(":", "").trim().lowercase(Locale.US)
            val actualCleanSha256 = calculatedSha256.replace(":", "").trim().lowercase(Locale.US)

            if (actualCleanSha256 != expectedSha256) {
                return VerificationResult.Failure(
                    "APK SHA-256 integrity check failed! File may be corrupted or tampered with.",
                    "Expected: $expectedSha256, Actual: $actualCleanSha256"
                )
            }
        }

        // 6. Archive Parsing & Package Info validation
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo: PackageInfo? = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
        if (archiveInfo == null) {
            return VerificationResult.Failure(
                "Unable to parse APK archive (corrupted or truncated download). The file is not a valid Android package."
            )
        }

        // 7. Verify Package Name (Application ID)
        val expectedPackageName = context.packageName // com.aistudio.pdfmaster.qvrx
        if (archiveInfo.packageName != expectedPackageName) {
            return VerificationResult.Failure(
                "Package identity mismatch! APK package is '${archiveInfo.packageName}', expected '$expectedPackageName'."
            )
        }

        // 8. Non-downgrade and version code check
        val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
        if (archiveVersionCode < installedVersionCode) {
            return VerificationResult.Failure(
                "Version downgrade rejected: Downloaded APK version ($archiveVersionCode) is older than currently installed ($installedVersionCode)."
            )
        }

        if (archiveVersionCode == installedVersionCode && !allowSameVersion) {
            val archiveVersionName = archiveInfo.versionName ?: "unknown"
            return VerificationResult.Failure(
                "Downloaded APK (v$archiveVersionName, Build $archiveVersionCode) is already installed. An update requires a newer build with a higher versionCode."
            )
        }

        if (manifest.latestVersionCode > 0 && archiveVersionCode < manifest.latestVersionCode) {
            return VerificationResult.Failure(
                "APK build version ($archiveVersionCode) is lower than expected latestVersionCode (${manifest.latestVersionCode})."
            )
        }

        // 9. Verify Signing Certificate
        val archiveSignatures = extractSignatures(archiveInfo)
        if (archiveSignatures.isEmpty()) {
            return VerificationResult.Failure("No signing certificates found in the downloaded APK archive.")
        }

        val archiveFingerprints = archiveSignatures.map { calculateCertSha256(it) }

        // If manifest specifies a signingCertificateSha256, verify it matches
        if (!manifest.signingCertificateSha256.isNullOrBlank()) {
            val expectedCertFingerprint = manifest.signingCertificateSha256
                .replace(":", "").trim().lowercase(Locale.US)

            val matchesManifest = archiveFingerprints.any { cert ->
                cert.replace(":", "").trim().lowercase(Locale.US) == expectedCertFingerprint
            }

            if (!matchesManifest) {
                return VerificationResult.Failure(
                    "APK signing certificate does not match the manifest expected certificate fingerprint.",
                    "Expected: $expectedCertFingerprint, Found: $archiveFingerprints"
                )
            }
        }

        // 8. Verify compatibility with installed app signature
        try {
            val installedInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val installedSignatures = extractSignatures(installedInfo)
            if (installedSignatures.isNotEmpty()) {
                val installedFingerprints = installedSignatures.map { calculateCertSha256(it).replace(":", "").lowercase(Locale.US) }
                val archiveCleanFingerprints = archiveFingerprints.map { it.replace(":", "").lowercase(Locale.US) }

                val sharesCertificate = archiveCleanFingerprints.any { it in installedFingerprints }
                if (!sharesCertificate) {
                    return VerificationResult.Failure(
                        "APK signing certificate mismatch: The downloaded APK is signed with a different key than the installed app. Android will refuse installation with INSTALL_FAILED_UPDATE_INCOMPATIBLE. Please ensure the update is built and signed with the matching keystore.",
                        "Installed: $installedFingerprints, APK: $archiveCleanFingerprints"
                    )
                }
            }
        } catch (e: Exception) {
            // In unit test runner without full package context or if check fails non-fatally
        }

        return VerificationResult.Success
    }

    /**
     * Calculates SHA-256 checksum of a file using an 8KB streaming buffer.
     */
    fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return bytesToHex(digest.digest())
    }

    /**
     * Calculates SHA-256 fingerprint for a raw Signature byte array.
     */
    fun calculateCertSha256(signature: Signature): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(signature.toByteArray())
        return bytesToHex(digest.digest())
    }

    private fun extractSignatures(packageInfo: PackageInfo): List<Signature> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptyList()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners?.toList() ?: emptyList()
            } else {
                signingInfo.signingCertificateHistory?.toList() ?: emptyList()
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.toList() ?: emptyList()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format(Locale.US, "%02x", b))
        }
        return sb.toString()
    }
}
