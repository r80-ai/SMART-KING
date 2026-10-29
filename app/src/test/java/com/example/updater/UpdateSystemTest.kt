package com.example.updater

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpdateSystemTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testUpdateManifestParsing_ValidJson() {
        val json = """
            {
                "manifestSchemaVersion": 1,
                "appId": "com.aistudio.pdfmaster.qvrx",
                "latestVersionCode": 5,
                "latestVersionName": "1.2.0",
                "minimumSupportedVersionCode": 3,
                "mandatoryUpdate": false,
                "releaseTitle": "New Release 1.2.0",
                "releaseNotes": "Fixes and performance improvements",
                "downloadUrl": "https://github.com/r80-ai/SMART-KING/releases/download/v1.2.0/app.apk",
                "releasePageUrl": "https://github.com/r80-ai/SMART-KING/releases/tag/v1.2.0",
                "apkSha256": "ABCD1234EF5678",
                "publishedAt": "2026-10-29T10:00:00Z",
                "fileSizeBytes": 15000000
            }
        """.trimIndent()

        val manifest = UpdateManifest.fromJson(json)
        assertEquals(1, manifest.manifestSchemaVersion)
        assertEquals("com.aistudio.pdfmaster.qvrx", manifest.appId)
        assertEquals(5L, manifest.latestVersionCode)
        assertEquals("1.2.0", manifest.latestVersionName)
        assertEquals(3L, manifest.minimumSupportedVersionCode)
        assertFalse(manifest.mandatoryUpdate)
        assertEquals("New Release 1.2.0", manifest.releaseTitle)
        assertEquals("https://github.com/r80-ai/SMART-KING/releases/download/v1.2.0/app.apk", manifest.downloadUrl)
    }

    @Test
    fun testVersionComparison_UpdateAvailableAndMandatory() {
        val manifest = UpdateManifest(
            latestVersionCode = 10,
            latestVersionName = "2.0.0",
            minimumSupportedVersionCode = 5,
            mandatoryUpdate = false
        )

        // Case 1: Newer version, but above minimumSupportedVersionCode -> Available, Not Mandatory
        assertTrue(manifest.isUpdateAvailable(installedVersionCode = 6))
        assertFalse(manifest.isMandatory(installedVersionCode = 6))

        // Case 2: Installed version strictly below minimumSupportedVersionCode -> Mandatory!
        assertTrue(manifest.isUpdateAvailable(installedVersionCode = 4))
        assertTrue(manifest.isMandatory(installedVersionCode = 4))

        // Case 3: Installed version is latest -> Not available, Not mandatory
        assertFalse(manifest.isUpdateAvailable(installedVersionCode = 10))
        assertFalse(manifest.isMandatory(installedVersionCode = 10))

        // Case 4: Installed version is higher (future/dev build) -> Not available
        assertFalse(manifest.isUpdateAvailable(installedVersionCode = 11))
        assertFalse(manifest.isMandatory(installedVersionCode = 11))

        // Case 5: mandatoryUpdate explicitly set to true
        val mandatoryManifest = manifest.copy(mandatoryUpdate = true)
        assertTrue(mandatoryManifest.isMandatory(installedVersionCode = 9))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testManifestParsing_MissingVersionCode_ThrowsException() {
        val invalidJson = """
            {
                "appId": "com.aistudio.pdfmaster.qvrx",
                "latestVersionName": "1.0"
            }
        """.trimIndent()
        UpdateManifest.fromJson(invalidJson)
    }

    @Test
    fun testFileSha256ChecksumCalculation() {
        val testFile = File(context.cacheDir, "test_checksum_file.bin")
        FileOutputStream(testFile).use { fos ->
            fos.write("SMART_KING_TEST_PAYLOAD".toByteArray(Charsets.UTF_8))
        }

        val calculatedSha256 = UpdateSecurityVerifier.calculateFileSha256(testFile)
        assertNotNull(calculatedSha256)
        assertEquals(64, calculatedSha256.length) // 256 bits = 64 hex characters

        // Modifying one byte changes the checksum
        FileOutputStream(testFile).use { fos ->
            fos.write("SMART_KING_TEST_PAYLOAD_MODIFIED".toByteArray(Charsets.UTF_8))
        }
        val modifiedSha256 = UpdateSecurityVerifier.calculateFileSha256(testFile)
        assertTrue(calculatedSha256 != modifiedSha256)

        testFile.delete()
    }

    @Test
    fun testUpdateConfig_PersistenceAndReset() {
        val config = UpdateConfig(context)
        val originalDefault = config.manifestUrl
        assertEquals(UpdateConfig.DEFAULT_MANIFEST_URL, originalDefault)

        val customUrl = "https://custom-server.org/updates/my_manifest.json"
        config.manifestUrl = customUrl
        assertEquals(customUrl, config.manifestUrl)

        config.githubOwner = "new-owner"
        config.githubRepo = "new-repo"
        config.preferredApkAssetName = "custom-build.apk"

        assertEquals("new-owner", config.githubOwner)
        assertEquals("new-repo", config.githubRepo)
        assertEquals("custom-build.apk", config.preferredApkAssetName)
        assertEquals("https://api.github.com/repos/new-owner/new-repo/releases/latest", config.getLatestReleaseApiUrl())

        config.resetToDefault()
        assertEquals(UpdateConfig.DEFAULT_MANIFEST_URL, config.manifestUrl)
        assertEquals(UpdateConfig.DEFAULT_GITHUB_OWNER, config.githubOwner)
        assertEquals(UpdateConfig.DEFAULT_GITHUB_REPO, config.githubRepo)
        assertEquals(UpdateConfig.DEFAULT_PREFERRED_APK_ASSET, config.preferredApkAssetName)
    }

    @Test
    fun testGitHubReleaseAssetSelection_PrefersAppDebugApk() {
        val release = GitHubReleaseInfo(
            id = 101L,
            tagName = "v1.1.0",
            name = "SMART KING Debug Build #12",
            draft = false,
            prerelease = false,
            body = "Release notes for build 12",
            publishedAt = "2026-10-29T12:00:00Z",
            assets = listOf(
                GitHubReleaseAsset(1, "source-code.zip", 5000000, "https://github.com/download/source.zip", "application/zip"),
                GitHubReleaseAsset(2, "app-debug.apk", 329000000, "https://github.com/download/app-debug.apk", "application/vnd.android.package-archive"),
                GitHubReleaseAsset(3, "checksums.txt", 1024, "https://github.com/download/checksums.txt", "text/plain")
            )
        )

        val selected = GitHubReleaseHelper.selectApkAsset(release, "app-debug.apk")
        assertTrue(selected.isSuccess)
        val asset = selected.getOrThrow()
        assertEquals("app-debug.apk", asset.name)
        assertEquals("https://github.com/download/app-debug.apk", asset.downloadUrl)
    }

    @Test
    fun testGitHubReleaseAssetSelection_NoApk_FailsWithDescriptiveError() {
        val release = GitHubReleaseInfo(
            id = 102L,
            tagName = "v1.1.0",
            name = "Release Without APK",
            draft = false,
            prerelease = false,
            body = "No binaries attached",
            publishedAt = "2026-10-29T12:00:00Z",
            assets = listOf(
                GitHubReleaseAsset(1, "source-code.tar.gz", 5000000, "https://github.com/download/source.tar.gz", "application/gzip")
            )
        )

        val selected = GitHubReleaseHelper.selectApkAsset(release, "app-debug.apk")
        assertTrue(selected.isFailure)
        val err = selected.exceptionOrNull()
        assertTrue(err is IllegalStateException)
        assertTrue(err?.message?.contains("No APK asset found") == true)
    }

    @Test
    fun testGitHubReleaseHelper_ExtractSha256AndVersionCode() {
        val bodyWithSha = """
            SMART KING v1.1.0
            SHA256: d813259c995337db5d7aceb207db830f945b0a95169f2396a7f9e93e888df60d
            Build: 25
        """.trimIndent()

        val sha256 = GitHubReleaseHelper.extractSha256FromBody(bodyWithSha)
        assertEquals("d813259c995337db5d7aceb207db830f945b0a95169f2396a7f9e93e888df60d", sha256)

        val versionCode = GitHubReleaseHelper.extractVersionCode("build-25", bodyWithSha)
        assertEquals(25L, versionCode)
    }

    @Test
    fun testVerificationFailsOnCorruptedChecksum() {
        val testFile = File(context.cacheDir, "fake_app.apk")
        FileOutputStream(testFile).use { fos ->
            fos.write("NOT_A_REAL_APK".toByteArray())
        }

        val manifest = UpdateManifest(
            latestVersionCode = 2,
            latestVersionName = "1.1.0",
            apkSha256 = "1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff"
        )

        val result = UpdateSecurityVerifier.verifyApk(
            context = context,
            apkFile = testFile,
            manifest = manifest,
            installedVersionCode = 1
        )

        assertTrue(result is VerificationResult.Failure)
        val failure = result as VerificationResult.Failure
        assertTrue(failure.reason.contains("SHA-256 integrity check failed"))

        testFile.delete()
    }
}
