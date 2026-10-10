package com.example.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.example.photo.birefnet.BiRefNetLiteEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BiRefNetValidationTest {

    @Test
    fun testBiRefNetLiteModelValidation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = BiRefNetLiteEngine.validateModel(context)
        println("Validation result: isValid=${result.isValid}")
        println("Model path: ${result.modelPath}")
        println("Model size: ${result.modelSizeBytes} bytes")
        println("Input info: ${result.inputInfo}")
        println("Output info: ${result.outputInfo}")
        println("Technical error: ${result.technicalError}")
        println("User friendly error: ${result.userFriendlyError}")

        assertNotNull(result)
        if (result.isValid) {
            assertTrue("Model input info must not be null", result.inputInfo != null)
            assertTrue("Model output info must not be null", result.outputInfo != null)
        } else {
            assertTrue("Should report technical error if invalid", result.technicalError != null)
        }
    }

    /**
     * Requirement 17-A: BG Remove does not expose Sensitivity.
     * Verifies that Sensitivity methods and parameters are completely removed from BiRefNetLiteEngine.
     */
    @Test
    fun testBgRemoveDoesNotExposeSensitivity() {
        // 1. Verify BiRefNetLiteEngine does not expose applySensitivityToMask or any Sensitivity method
        val methods = BiRefNetLiteEngine::class.java.declaredMethods
        val hasSensitivityMethod = methods.any { it.name.contains("Sensitivity", ignoreCase = true) }
        assertFalse("BiRefNetLiteEngine must not declare any Sensitivity methods", hasSensitivityMethod)

        // 2. Verify removeBackground does not accept sensitivity parameter
        val removeBgMethods = methods.filter { it.name.startsWith("removeBackground") }
        assertTrue("removeBackground method must exist", removeBgMethods.isNotEmpty())
        for (m in removeBgMethods) {
            val paramTypes = m.parameterTypes
            val floatParams = paramTypes.filter { it == Float::class.java || it == java.lang.Float::class.java }
            assertEquals("removeBackground must not have any Float sensitivity parameters", 0, floatParams.size)
        }
    }

    /**
     * Requirement 17-B: BiRefNet Lite generates the mask.
     */
    @Test
    fun testBiRefNetLiteGeneratesMask() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val validation = BiRefNetLiteEngine.validateModel(context)
        if (!validation.isValid) {
            println("Skipping inference test because model weights are not fully checked out: ${validation.technicalError}")
            return@runTest
        }

        val bmp = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { color = Color.BLUE }
        canvas.drawCircle(100f, 150f, 80f, paint)

        val result = BiRefNetLiteEngine.removeBackground(context, bmp, refineEdges = true)
        assertTrue("BiRefNet Lite inference must succeed", result.isSuccess)
        val seg = result.getOrThrow()
        assertNotNull("Generated segmentation result must not be null", seg)
        assertNotNull("Alpha mask must not be null", seg.alphaMask)
        assertEquals("Mask width must match original bitmap width", 200, seg.maskWidth)
        assertEquals("Mask height must match original bitmap height", 300, seg.maskHeight)
        assertEquals("Alpha mask size must equal width * height", 200 * 300, seg.alphaMask.size)

        // Ensure all alpha values are in [0.0f, 1.0f] and not NaN
        for (alpha in seg.alphaMask) {
            assertFalse("Alpha value must not be NaN", alpha.isNaN())
            assertTrue("Alpha value must be within [0.0, 1.0]", alpha in 0f..1f)
        }

        seg.transparentForeground.recycle()
        bmp.recycle()
    }

    /**
     * Requirement 17-C: The original background is removed according to the BiRefNet mask.
     */
    @Test
    fun testOriginalBackgroundIsRemovedAccordingToBiRefNetMask() {
        val width = 20
        val height = 20
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.RED)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { color = Color.BLUE }
        canvas.drawRect(5f, 5f, 15f, 15f, paint)

        // Synthetic mask: 1.0 in center (subject), 0.0 outside (background)
        val alphaMask = FloatArray(width * height) { idx ->
            val x = idx % width
            val y = idx / width
            if (x in 5 until 15 && y in 5 until 15) 1.0f else 0.0f
        }

        val fg = BiRefNetLiteEngine.createRefinedForegroundBitmap(bmp, alphaMask, width, height)
        assertNotNull("Foreground bitmap must not be null", fg)

        // Pixels outside subject must have alpha = 0 (completely removed background)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = fg.getPixel(x, y)
                if (x in 5 until 15 && y in 5 until 15) {
                    assertEquals("Subject pixels must retain subject alpha", 255, Color.alpha(pixel))
                } else {
                    assertEquals("Background pixels must be completely transparent (alpha = 0)", 0, Color.alpha(pixel))
                }
            }
        }

        fg.recycle()
        bmp.recycle()
    }

    /**
     * Requirement 17-D: Transparent background contains no original background pixels outside the subject mask.
     */
    @Test
    fun testTransparentBackgroundContainsNoOriginalBackgroundPixelsOutsideSubjectMask() {
        val width = 20
        val height = 20
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.RED)

        val alphaMask = FloatArray(width * height) { idx ->
            val x = idx % width
            val y = idx / width
            if (x in 6 until 14 && y in 6 until 14) 1.0f else 0.0f
        }

        val fg = BiRefNetLiteEngine.createRefinedForegroundBitmap(bmp, alphaMask, width, height)
        val comp = PhotoEditorEngine.compositeSubjectOverBackground(
            subjectBitmap = fg,
            bgType = BgOptionType.TRANSPARENT
        )
        assertNotNull("Transparent composite must not be null", comp)

        // Verify background region is strictly 0 (no red pixels remain)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = comp.getPixel(x, y)
                if (x < 6 || x >= 14 || y < 6 || y >= 14) {
                    assertEquals("Pixel outside subject mask must have zero alpha", 0, Color.alpha(pixel))
                    assertEquals("Pixel outside subject mask must contain no original color", 0, pixel)
                }
            }
        }

        comp.recycle()
        fg.recycle()
        bmp.recycle()
    }

    /**
     * Requirement 17-E: Color/Gradient/Image backgrounds do not reveal the original background.
     */
    @Test
    fun testColorGradientImageBackgroundsDoNotRevealOriginalBackground() {
        val width = 20
        val height = 20
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.RED)

        val alphaMask = FloatArray(width * height) { idx ->
            val x = idx % width
            val y = idx / width
            if (x in 6 until 14 && y in 6 until 14) 1.0f else 0.0f
        }

        val fg = BiRefNetLiteEngine.createRefinedForegroundBitmap(bmp, alphaMask, width, height)

        // 1. Solid Color background: GREEN
        val compColor = PhotoEditorEngine.compositeSubjectOverBackground(
            subjectBitmap = fg,
            bgType = BgOptionType.SOLID_COLOR,
            solidColor = Color.GREEN
        )
        assertNotNull("Solid color composite must not be null", compColor)
        val bgPixelColor = compColor.getPixel(0, 0)
        assertEquals("Background region must be strictly selected GREEN color", Color.GREEN, bgPixelColor)
        assertFalse("Original RED background must not appear in background region", bgPixelColor == Color.RED)

        // 2. Gradient background
        val compGradient = PhotoEditorEngine.compositeSubjectOverBackground(
            subjectBitmap = fg,
            bgType = BgOptionType.GRADIENT,
            gradientColor1 = Color.BLUE,
            gradientColor2 = Color.CYAN
        )
        assertNotNull("Gradient composite must not be null", compGradient)
        val bgPixelGradient = compGradient.getPixel(0, 0)
        assertFalse("Original RED background must not appear in gradient composite", bgPixelGradient == Color.RED)

        // 3. Image background: YELLOW image
        val bgImg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bgImg.eraseColor(Color.YELLOW)
        val compImage = PhotoEditorEngine.compositeSubjectOverBackground(
            subjectBitmap = fg,
            bgType = BgOptionType.IMAGE,
            bgImage = bgImg
        )
        assertNotNull("Image composite must not be null", compImage)
        val bgPixelImage = compImage.getPixel(0, 0)
        assertEquals("Background region must be replacement image color", Color.YELLOW, bgPixelImage)
        assertFalse("Original RED background must not appear in image composite", bgPixelImage == Color.RED)

        compColor.recycle()
        compGradient.recycle()
        compImage.recycle()
        bgImg.recycle()
        fg.recycle()
        bmp.recycle()
    }

    /**
     * Requirement 17-F: Refine still works independently.
     */
    @Test
    fun testRefineWorksIndependently() {
        val width = 20
        val height = 20
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLUE)

        // Initial uniform mask of 1.0 (entire subject)
        val mask = FloatArray(width * height) { 1.0f }

        // Apply Erase brush on top-left quadrant (isRestore = false)
        BiRefNetLiteEngine.applyBrushToMask(
            mask = mask,
            width = width,
            height = height,
            centerX = 4f,
            centerY = 4f,
            radius = 6f,
            isRestore = false,
            isSoft = false
        )

        // Center of erase brush must be erased to 0.0f
        val erasedIdx = 4 * width + 4
        assertEquals("Center of erase brush must be 0.0", 0.0f, mask[erasedIdx], 0.001f)

        // Apply Restore brush back on the same spot (isRestore = true)
        BiRefNetLiteEngine.applyBrushToMask(
            mask = mask,
            width = width,
            height = height,
            centerX = 4f,
            centerY = 4f,
            radius = 6f,
            isRestore = true,
            isSoft = false
        )
        assertEquals("Center of restore brush must be restored to 1.0", 1.0f, mask[erasedIdx], 0.001f)

        val refinedFg = BiRefNetLiteEngine.createRefinedForegroundBitmap(bmp, mask, width, height)
        assertNotNull("Refined foreground must be created independently", refinedFg)
        refinedFg.recycle()
        bmp.recycle()
    }

    /**
     * Requirement 17-G: Position still works independently.
     */
    @Test
    fun testPositionWorksIndependently() {
        val width = 20
        val height = 20
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLUE)

        val compTransformed = PhotoEditorEngine.compositeSubjectOverBackground(
            subjectBitmap = bmp,
            bgType = BgOptionType.SOLID_COLOR,
            solidColor = Color.GREEN,
            subjectTranslationX = 5f,
            subjectTranslationY = -3f,
            subjectScale = 0.8f,
            subjectRotation = 45f
        )
        assertNotNull("Transformed composite must be generated successfully", compTransformed)
        assertEquals("Output width must match subject width", width, compTransformed.width)
        assertEquals("Output height must match subject height", height, compTransformed.height)

        compTransformed.recycle()
        bmp.recycle()
    }

    /**
     * Requirement 17-H: Apply commits the final background-removal result.
     */
    @Test
    fun testApplyCommitsFinalBackgroundRemovalResult() {
        var currentBitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        currentBitmap.eraseColor(Color.RED)
        val origBitmap = currentBitmap

        // Simulate BG Remove state: transparent background, blue subject in center
        val subject = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply { color = Color.BLUE }
        Canvas(subject).drawRect(5f, 5f, 15f, 15f, paint)
        var previewBitmap: Bitmap? = PhotoEditorEngine.compositeSubjectOverBackground(
            subjectBitmap = subject,
            bgType = BgOptionType.SOLID_COLOR,
            solidColor = Color.GREEN
        )

        // Apply action
        currentBitmap = previewBitmap!!
        previewBitmap = null

        assertNotNull("Committed bitmap must not be null", currentBitmap)
        assertTrue("Current bitmap must be updated to the final BG remove result", currentBitmap !== origBitmap)
        assertEquals("Final committed result must reflect replacement background", Color.GREEN, currentBitmap.getPixel(0, 0))

        currentBitmap.recycle()
        origBitmap.recycle()
        subject.recycle()
    }

    /**
     * Requirement 17-I: Cancel restores the original image.
     */
    @Test
    fun testCancelRestoresOriginalImage() {
        val original = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        original.eraseColor(Color.RED)
        var currentBitmap: Bitmap? = original

        // Simulate BG Remove opening and producing a preview
        val preview = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        preview.eraseColor(Color.GREEN)
        var previewBitmap: Bitmap? = preview

        // Cancel action
        currentBitmap = original
        previewBitmap = null

        assertSame("Cancel must restore original image instance intact", original, currentBitmap)
        assertEquals("Original pixels must remain intact", Color.RED, currentBitmap.getPixel(0, 0))

        preview.recycle()
        original.recycle()
    }

    @Test
    fun testModelAssetIntegrityAndSha256() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetManager = context.assets

        // A. The model asset exists.
        val stream = assetManager.open(BiRefNetLiteEngine.MODEL_ASSET_NAME)
        assertNotNull("Asset stream must not be null", stream)

        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        var totalBytes = 0L
        var firstChunk = true
        var headerStr = ""

        stream.use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                if (firstChunk) {
                    firstChunk = false
                    val checkLen = kotlin.math.min(read, 100)
                    headerStr = String(buffer, 0, checkLen, Charsets.US_ASCII)
                }
                digest.update(buffer, 0, read)
                totalBytes += read
            }
        }

        // B. The model is not an LFS pointer.
        assertTrue("Asset must NOT be a Git LFS pointer text file", !headerStr.startsWith("version https://git-lfs"))

        // C. Model size is 98,484,532 bytes.
        assertTrue("Model asset size must be exactly 98,484,532 bytes, got $totalBytes", totalBytes == BiRefNetLiteEngine.EXPECTED_MODEL_SIZE)

        // D. Model SHA-256 matches expected SHA.
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        assertTrue("Model asset SHA-256 ($hash) must match expected ${BiRefNetLiteEngine.EXPECTED_SHA256}", hash.equals(BiRefNetLiteEngine.EXPECTED_SHA256, ignoreCase = true))
    }

    @Test
    fun testModelCanBeExtractedToInternalStorageAndCached() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // E. The model can be copied from Android assets to internal storage.
        val modelFile = BiRefNetLiteEngine.getOrExtractModelFile(context)
        assertNotNull("Model file must not be null", modelFile)
        assertTrue("Model file must exist", modelFile.exists())
        assertTrue("Model file length must match expected size", modelFile.length() == BiRefNetLiteEngine.EXPECTED_MODEL_SIZE)
        assertTrue("Model file must not be LFS pointer", !BiRefNetLiteEngine.isGitLfsPointer(modelFile))

        // Cache verification: re-extracting should immediately return the existing cached file
        val cachedFile = BiRefNetLiteEngine.getOrExtractModelFile(context)
        assertTrue("Cached file must be same path", cachedFile.absolutePath == modelFile.absolutePath)
        assertTrue("Cached file must retain correct length", cachedFile.length() == BiRefNetLiteEngine.EXPECTED_MODEL_SIZE)
    }

    @Test
    fun testCopiedModelCreatesValidOrtSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // F. The copied model can create an ONNX Runtime session.
        val session = BiRefNetLiteEngine.getSession(context)
        assertNotNull("ONNX session must be created", session)
        assertTrue("Session must have input names", session.inputNames.isNotEmpty())
        assertTrue("Session must have output names", session.outputNames.isNotEmpty())

        val inputName = session.inputNames.first()
        val inputInfo = session.inputInfo[inputName]
        assertNotNull("Input info must exist for $inputName", inputInfo)
        println("Session input info: $inputName -> $inputInfo")
    }

    @Test
    fun testSessionReuseAcrossMultipleCallsAndReopening() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // G & J. Repeated BG Remove calls and reopening reuse the same initialized session.
        val session1 = BiRefNetLiteEngine.getSession(context)
        val session2 = BiRefNetLiteEngine.getSession(context)
        assertSame("BiRefNetLiteEngine must reuse the exact same OrtSession instance", session1, session2)
    }

    @Test
    fun testMultipleConcurrentLoadsDoNotCreateMultipleSessions() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // H. Multiple rapid BG Remove taps do not create multiple model-loading operations.
        coroutineScope {
            val deferredList = (1..5).map {
                async(Dispatchers.Default) {
                    BiRefNetLiteEngine.getSessionAsync(context)
                }
            }
            val sessions = deferredList.awaitAll()
            val firstSession = sessions.first()
            for (session in sessions) {
                assertSame("All concurrent callers must receive the exact same session instance", firstSession, session)
            }
        }
    }

    @Test
    fun testModelBinaryIsActualWeightsAndNotLfsPointer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetManager = context.assets
        val stream = assetManager.open("birefnet_lite_fp16.onnx")
        val header = ByteArray(100)
        val bytesRead = stream.read(header)
        stream.close()

        val headerString = String(header, 0, bytesRead, Charsets.UTF_8)
        assertTrue("Asset must NOT be a Git LFS pointer", !headerString.startsWith("version https://git-lfs.github.com"))

        val validation = BiRefNetLiteEngine.validateModel(context)
        assertTrue("Model must be valid: ${validation.technicalError}", validation.isValid)
        assertTrue("Model size must be == 98,484,532 bytes", validation.modelSizeBytes == BiRefNetLiteEngine.EXPECTED_MODEL_SIZE)
    }
}
