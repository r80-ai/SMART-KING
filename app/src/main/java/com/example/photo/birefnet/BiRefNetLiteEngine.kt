package com.example.photo.birefnet

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * BiRefNetLiteEngine - High-performance, lightweight on-device AI background removal
 * using the optimized BiRefNet Lite model (birefnet_lite_fp16.onnx) via ONNX Runtime.
 *
 * Runs 100% offline with zero external dependencies, preserving fine edge details
 * (hair, clothing, contours) with fast on-device inference and memory safety.
 */
object BiRefNetLiteEngine {
    private const val TAG = "BiRefNetLiteEngine"
    const val MODEL_ASSET_NAME = "birefnet_lite_fp16.onnx"
    const val EXPECTED_MODEL_SIZE = 98484532L
    const val EXPECTED_SHA256 = "eff9216bb2f9d3f023d9c2b7196845a7485739ab1f231593633e4d2344ffc516"
    private const val MODEL_INPUT_SIZE = 512
    private const val MIN_PLAUSIBLE_MODEL_SIZE = 90_000_000L // Expected ~98.5 MB

    // ImageNet normalization constants used by BiRefNet
    private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    @Volatile
    private var ortEnvironment: OrtEnvironment? = null

    @Volatile
    private var ortSession: OrtSession? = null

    private val initMutex = Mutex()
    private val syncLock = Any()

    /**
     * Model validation result data class.
     */
    data class ModelValidationResult(
        val isValid: Boolean,
        val modelPath: String? = null,
        val modelSizeBytes: Long = 0,
        val sha256: String? = null,
        val inputInfo: String? = null,
        val outputInfo: String? = null,
        val technicalError: String? = null,
        val userFriendlyError: String? = null
    )

    /**
     * Result of background removal containing the refined transparent subject
     * and the continuous alpha mask matrix for further manual brush editing.
     */
    data class SegmentationResult(
        val transparentForeground: Bitmap,
        val alphaMask: FloatArray, // Width x Height values in range [0f..1f]
        val maskWidth: Int,
        val maskHeight: Int
    )

    /**
     * Checks whether a file is a Git LFS pointer rather than a real binary ONNX model.
     */
    fun isGitLfsPointer(file: File): Boolean {
        try {
            if (file.length() < 1000) {
                val content = file.readText()
                return content.contains("git-lfs") || content.contains("oid sha256:")
            }
            file.inputStream().use { stream ->
                val header = ByteArray(100)
                val read = stream.read(header)
                if (read > 0) {
                    val headerStr = String(header, 0, read, Charsets.US_ASCII)
                    return headerStr.startsWith("version https://git-lfs")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "[BiRefNetLite] Error checking LFS pointer for ${file.name}", e)
        }
        return false
    }

    /**
     * Computes the SHA-256 checksum of a file.
     */
    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(65536)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Resolves or extracts the BiRefNet Lite model file from assets to local app storage.
     * Uses a chunked 1MB stream buffer to safely extract large ONNX models without memory exhaustion.
     * Caches the extracted model permanently in filesDir and reuses it on subsequent calls.
     */
    fun getOrExtractModelFile(context: Context): File {
        // 1. Target file on device storage (cached)
        val targetDir = context.noBackupFilesDir ?: context.filesDir
        val targetFile = File(targetDir, MODEL_ASSET_NAME)

        // Validate if existing target file is valid (not corrupt, exact size or > MIN_PLAUSIBLE_MODEL_SIZE, not Git LFS)
        if (targetFile.exists()) {
            val len = targetFile.length()
            if (len == EXPECTED_MODEL_SIZE && !isGitLfsPointer(targetFile)) {
                Log.i(TAG, "[BiRefNetLite] Model file verified from cache at: ${targetFile.absolutePath} ($len bytes)")
                return targetFile
            } else if (len > MIN_PLAUSIBLE_MODEL_SIZE && !isGitLfsPointer(targetFile)) {
                Log.i(TAG, "[BiRefNetLite] Model file verified from cache at: ${targetFile.absolutePath} ($len bytes)")
                return targetFile
            } else {
                Log.w(TAG, "[BiRefNetLite] Existing cached model file is invalid or incomplete (len=$len). Re-extracting from assets...")
                targetFile.delete()
            }
        }

        // 2. Check local asset files (development / Robolectric / unit test environment)
        val devCandidates = listOf(
            File("src/main/assets", MODEL_ASSET_NAME),
            File("app/src/main/assets", MODEL_ASSET_NAME),
            File("../app/src/main/assets", MODEL_ASSET_NAME)
        )
        for (candidate in devCandidates) {
            if (candidate.exists() && candidate.length() >= MIN_PLAUSIBLE_MODEL_SIZE && !isGitLfsPointer(candidate)) {
                Log.i(TAG, "[BiRefNetLite] Found verified dev model file at: ${candidate.absolutePath} (${candidate.length()} bytes)")
                return candidate
            }
        }

        // 3. Extract uncompressed asset from APK safely using chunked streaming
        Log.i(TAG, "[BiRefNetLite] Extracting $MODEL_ASSET_NAME from APK assets to ${targetFile.absolutePath}...")
        val tempFile = File(targetDir, "${MODEL_ASSET_NAME}.tmp_${System.currentTimeMillis()}")
        val startTime = System.currentTimeMillis()
        try {
            val assetStream: InputStream = context.assets.open(MODEL_ASSET_NAME)
            assetStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(1024 * 1024) // 1MB chunked buffer for streaming
                    var read: Int
                    var totalBytes = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalBytes += read
                    }
                    output.flush()
                    try {
                        output.fd.sync()
                    } catch (_: Throwable) {}
                    Log.i(TAG, "[BiRefNetLite] Streamed $totalBytes bytes from assets in ${System.currentTimeMillis() - startTime}ms")
                }
            }

            if (!tempFile.exists()) {
                throw IllegalStateException("Failed to extract model asset: temporary file does not exist")
            }

            val extractedLen = tempFile.length()
            if (isGitLfsPointer(tempFile)) {
                val header = try { tempFile.readText().take(120) } catch (_: Throwable) { "" }
                tempFile.delete()
                throw IllegalStateException("Model asset in APK is a Git LFS pointer text file ($extractedLen bytes: '$header'). Real binary ONNX weights must be packaged into the APK.")
            }

            if (extractedLen < MIN_PLAUSIBLE_MODEL_SIZE) {
                tempFile.delete()
                throw IllegalStateException("Extracted model file is incomplete ($extractedLen bytes, expected $EXPECTED_MODEL_SIZE bytes).")
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }

            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                Log.i(TAG, "[BiRefNetLite] Direct rename returned false; copying temp file to target...")
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            Log.i(TAG, "[BiRefNetLite] Model extracted and cached successfully: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
        } catch (e: Throwable) {
            Log.e(TAG, "[BiRefNetLite] Failed to extract model from assets: ${e.message}", e)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            throw e
        }

        return targetFile
    }

    /**
     * Initializes or gets the cached ONNX Runtime session in a thread-safe asynchronous manner.
     * Ensures only ONE session is created across multiple concurrent or repeated calls.
     */
    suspend fun getSessionAsync(context: Context): OrtSession {
        ortSession?.let { return it }

        return initMutex.withLock {
            ortSession?.let { return@withLock it }

            withContext(Dispatchers.IO) {
                Log.i(TAG, "[BiRefNetLite] Initializing ONNX Runtime environment...")
                if (ortEnvironment == null) {
                    ortEnvironment = OrtEnvironment.getEnvironment()
                }
                val env = ortEnvironment ?: throw IllegalStateException("Failed to initialize OrtEnvironment")
                Log.i(TAG, "[BiRefNetLite] ONNX Runtime environment ready (version=${env.version})")

                val modelFile = getOrExtractModelFile(context)
                Log.i(TAG, "[BiRefNetLite] Model file ready at: ${modelFile.absolutePath} (${modelFile.length()} bytes)")

                val availableCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
                val sessionOptions = OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(availableCores)
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                }

                val sessionStartTime = System.currentTimeMillis()
                val session = env.createSession(modelFile.absolutePath, sessionOptions)
                ortSession = session

                Log.i(TAG, "[BiRefNetLite] Session created in ${System.currentTimeMillis() - sessionStartTime}ms!")
                Log.i(TAG, "[BiRefNetLite] Input names: ${session.inputNames}, Output names: ${session.outputNames}")
                for (name in session.inputNames) {
                    Log.i(TAG, "[BiRefNetLite] Input tensor: name=$name, info=${session.inputInfo[name]}")
                }
                for (name in session.outputNames) {
                    Log.i(TAG, "[BiRefNetLite] Output tensor: name=$name, info=${session.outputInfo[name]}")
                }

                session
            }
        }
    }

    /**
     * Synchronous session getter for non-coroutine contexts and backward compatibility.
     */
    fun getSession(context: Context): OrtSession {
        ortSession?.let { return it }

        synchronized(syncLock) {
            ortSession?.let { return it }

            Log.i(TAG, "[BiRefNetLite] Initializing ONNX Runtime environment synchronously...")
            if (ortEnvironment == null) {
                ortEnvironment = OrtEnvironment.getEnvironment()
            }
            val env = ortEnvironment ?: throw IllegalStateException("Failed to initialize OrtEnvironment")

            val modelFile = getOrExtractModelFile(context)
            val availableCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(availableCores)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }

            val session = env.createSession(modelFile.absolutePath, sessionOptions)
            ortSession = session
            return session
        }
    }

    /**
     * Asynchronously preloads the BiRefNet Lite model in the background so the first
     * background removal click responds instantly.
     */
    suspend fun preloadModelAsync(context: Context) {
        if (ortSession != null) return
        withContext(Dispatchers.IO) {
            try {
                getSessionAsync(context)
                Log.i(TAG, "[BiRefNetLite] Model successfully preloaded in background.")
            } catch (e: Throwable) {
                Log.w(TAG, "[BiRefNetLite] Background model preload warning: ${e.message}")
            }
        }
    }

    /**
     * Performs a comprehensive validation of the BiRefNet Lite model.
     */
    fun validateModel(context: Context): ModelValidationResult {
        Log.i(TAG, "[BiRefNetLite] Model validation: Starting checks...")
        try {
            val modelFile = try {
                getOrExtractModelFile(context)
            } catch (e: Throwable) {
                val err = "Failed to extract or resolve model file: ${e.message}"
                Log.e(TAG, "[BiRefNetLite] Model validation: FAILED - $err", e)
                return ModelValidationResult(
                    isValid = false,
                    technicalError = err,
                    userFriendlyError = "BiRefNet Lite model extraction failed: ${e.localizedMessage ?: e.message}"
                )
            }

            if (!modelFile.exists()) {
                val err = "Model file does not exist at ${modelFile.absolutePath}"
                Log.e(TAG, "[BiRefNetLite] Model validation: FAILED - $err")
                return ModelValidationResult(
                    isValid = false,
                    modelPath = modelFile.absolutePath,
                    technicalError = err,
                    userFriendlyError = "BiRefNet Lite model file not found on device storage."
                )
            }

            val size = modelFile.length()
            if (size <= 0) {
                val err = "Model file is empty (0 bytes)"
                Log.e(TAG, "[BiRefNetLite] Model validation: FAILED - $err")
                return ModelValidationResult(
                    isValid = false,
                    modelPath = modelFile.absolutePath,
                    modelSizeBytes = 0,
                    technicalError = err,
                    userFriendlyError = "BiRefNet Lite model file is empty."
                )
            }

            if (isGitLfsPointer(modelFile)) {
                val err = "Model file is a Git LFS pointer text file, not valid binary ONNX weights"
                Log.e(TAG, "[BiRefNetLite] Model validation: FAILED - $err")
                return ModelValidationResult(
                    isValid = false,
                    modelPath = modelFile.absolutePath,
                    modelSizeBytes = size,
                    technicalError = err,
                    userFriendlyError = "BiRefNet Lite model is a Git LFS pointer instead of binary model weights."
                )
            }

            if (size < MIN_PLAUSIBLE_MODEL_SIZE) {
                val err = "Model file size ($size bytes) is too small for BiRefNet Lite (expected $EXPECTED_MODEL_SIZE)"
                Log.e(TAG, "[BiRefNetLite] Model validation: FAILED - $err")
                return ModelValidationResult(
                    isValid = false,
                    modelPath = modelFile.absolutePath,
                    modelSizeBytes = size,
                    technicalError = err,
                    userFriendlyError = "BiRefNet Lite model file is incomplete ($size bytes)."
                )
            }

            val session = try {
                getSession(context)
            } catch (e: Throwable) {
                val err = "Failed to create ONNX Runtime InferenceSession: ${e.message}"
                Log.e(TAG, "[BiRefNetLite] Model validation: FAILED - $err", e)
                return ModelValidationResult(
                    isValid = false,
                    modelPath = modelFile.absolutePath,
                    modelSizeBytes = size,
                    technicalError = err,
                    userFriendlyError = "BiRefNet Lite inference engine could not be initialized: ${e.message}"
                )
            }

            val inputName = session.inputNames.firstOrNull()
            val inputInfo = inputName?.let { session.inputInfo[it] }
            if (inputName == null || inputInfo == null) {
                val err = "Cannot read input tensor metadata from session"
                Log.e(TAG, "[BiRefNetLite] Model validation: FAILED - $err")
                return ModelValidationResult(
                    isValid = false,
                    modelPath = modelFile.absolutePath,
                    modelSizeBytes = size,
                    technicalError = err,
                    userFriendlyError = "Invalid model input configuration."
                )
            }

            val outputName = session.outputNames.firstOrNull()
            val outputInfo = outputName?.let { session.outputInfo[it] }
            if (outputName == null || outputInfo == null) {
                val err = "Cannot read output tensor metadata from session"
                Log.e(TAG, "[BiRefNetLite] Model validation: FAILED - $err")
                return ModelValidationResult(
                    isValid = false,
                    modelPath = modelFile.absolutePath,
                    modelSizeBytes = size,
                    inputInfo = inputInfo.toString(),
                    technicalError = err,
                    userFriendlyError = "Invalid model output configuration."
                )
            }

            Log.i(TAG, "[BiRefNetLite] Model validation: PASSED! Path: ${modelFile.absolutePath}, Size: $size bytes, Input: $inputName ($inputInfo), Output: $outputName ($outputInfo)")
            return ModelValidationResult(
                isValid = true,
                modelPath = modelFile.absolutePath,
                modelSizeBytes = size,
                inputInfo = inputInfo.toString(),
                outputInfo = outputInfo.toString()
            )
        } catch (e: Throwable) {
            val err = "Unexpected error during validation: ${e.message}"
            Log.e(TAG, "[BiRefNetLite] Model validation: FAILED - $err", e)
            return ModelValidationResult(
                isValid = false,
                technicalError = err,
                userFriendlyError = "BiRefNet Lite model validation error: ${e.message}"
            )
        }
    }

    /**
     * Numerically stable sigmoid activation function.
     */
    private fun sigmoid(x: Float): Float {
        if (x.isNaN()) return 0.5f
        return if (x >= 0f) {
            1.0f / (1.0f + exp(-x))
        } else {
            val z = exp(x)
            z / (1.0f + z)
        }
    }

    /**
     * Runs BiRefNet Lite segmentation on [bitmap] asynchronously without blocking the UI.
     */
    suspend fun removeBackground(
        context: Context,
        bitmap: Bitmap,
        refineEdges: Boolean = true
    ): Result<SegmentationResult> = withContext(Dispatchers.Default) {
        try {
            if (bitmap.isRecycled) {
                return@withContext Result.failure(IllegalStateException("Input bitmap is recycled"))
            }

            val session = getSessionAsync(context)
            val env = ortEnvironment ?: OrtEnvironment.getEnvironment().also { ortEnvironment = it }

            val origW = bitmap.width
            val origH = bitmap.height
            Log.i(TAG, "[BiRefNetLite] Inference start: input image dimensions ${origW}x${origH}")

            // 1. Prepare 512x512 scaled input bitmap for ONNX inference
            val inputBmp = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
            val pixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
            inputBmp.getPixels(pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)
            if (inputBmp !== bitmap) {
                inputBmp.recycle()
            }

            // 2. Normalize and convert to Float32 tensor [1, 3, 512, 512] (NCHW)
            val channelSize = MODEL_INPUT_SIZE * MODEL_INPUT_SIZE
            val totalElements = 3 * channelSize
            val byteBuffer = ByteBuffer.allocateDirect(totalElements * 4)
                .order(ByteOrder.nativeOrder())
            val floatBuffer = byteBuffer.asFloatBuffer()

            val rPlane = FloatArray(channelSize)
            val gPlane = FloatArray(channelSize)
            val bPlane = FloatArray(channelSize)

            val inv255 = 1.0f / 255.0f
            val meanR = MEAN[0]
            val meanG = MEAN[1]
            val meanB = MEAN[2]
            val invStdR = 1.0f / STD[0]
            val invStdG = 1.0f / STD[1]
            val invStdB = 1.0f / STD[2]

            for (i in 0 until channelSize) {
                val p = pixels[i]
                val r = (((p shr 16) and 0xFF) * inv255 - meanR) * invStdR
                val g = (((p shr 8) and 0xFF) * inv255 - meanG) * invStdG
                val b = ((p and 0xFF) * inv255 - meanB) * invStdB

                rPlane[i] = r
                gPlane[i] = g
                bPlane[i] = b
            }

            floatBuffer.put(rPlane)
            floatBuffer.put(gPlane)
            floatBuffer.put(bPlane)
            byteBuffer.rewind()

            // 3. Create input tensor and run inference
            val inputName = session.inputNames.first()
            val outputName = session.outputNames.first()

            val shape = longArrayOf(1, 3, MODEL_INPUT_SIZE.toLong(), MODEL_INPUT_SIZE.toLong())
            val tensor = OnnxTensor.createTensor(
                env,
                byteBuffer,
                shape,
                OnnxJavaType.FLOAT
            )

            val inferenceStartTime = System.currentTimeMillis()
            val results = session.run(mapOf(inputName to tensor))
            val inferenceDuration = System.currentTimeMillis() - inferenceStartTime
            Log.i(TAG, "[BiRefNetLite] ONNX inference completed in ${inferenceDuration}ms")

            val outputTensor = results.get(outputName).get() as OnnxTensor
            val outFloatBuffer = outputTensor.floatBuffer

            // 4. Extract raw 512x512 logits and apply standard sigmoid for full pure segmentation mask
            val rawAlpha = FloatArray(channelSize)
            for (i in 0 until channelSize) {
                val logit = outFloatBuffer.get(i)
                val rawVal = sigmoid(logit)
                rawAlpha[i] = if (rawVal.isNaN()) 0f else rawVal.coerceIn(0f, 1f)
            }

            tensor.close()
            results.close()

            // 5. Determine working resolution: preserve high resolution for photos up to 2048px
            val maxWorkingDim = 2048
            val maxOrigDim = max(origW, origH)
            val (workingW, workingH) = if (maxOrigDim > maxWorkingDim) {
                val scale = maxWorkingDim.toFloat() / maxOrigDim
                Pair((origW * scale).roundToInt(), (origH * scale).roundToInt())
            } else {
                Pair(origW, origH)
            }

            // Bilinear upsampling of mask to working resolution
            Log.i(TAG, "[BiRefNetLite] Mask generation: resizing 512x512 to ${workingW}x${workingH}")
            val fullAlphaMask = upsampleBilinear(rawAlpha, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, workingW, workingH)

            // 6. Mask refinement: edge smoothing, hole filling, anti-aliasing
            val refinedMask = if (refineEdges) {
                refineAlphaMask(fullAlphaMask, workingW, workingH)
            } else {
                fullAlphaMask
            }

            // 7. Apply refined alpha mask to original bitmap with fringe suppression
            Log.i(TAG, "[BiRefNetLite] Alpha compositing: applying continuous matte with background-fringe suppression")
            val foreground = createRefinedForegroundBitmap(bitmap, refinedMask, workingW, workingH)
            Log.i(TAG, "[BiRefNetLite] Final output: ${workingW}x${workingH} transparent foreground bitmap created successfully")

            Result.success(
                SegmentationResult(
                    transparentForeground = foreground,
                    alphaMask = refinedMask,
                    maskWidth = workingW,
                    maskHeight = workingH
                )
            )
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "[BiRefNetLite] OutOfMemoryError during background removal", e)
            System.gc()
            Result.failure(IllegalStateException("Device ran out of memory during background removal. Please try a smaller photo.", e))
        } catch (e: Throwable) {
            Log.e(TAG, "[BiRefNetLite] Error running BiRefNet Lite inference: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Bilinear upsampling of a float mask from (srcW x srcH) to (dstW x dstH).
     */
    private fun upsampleBilinear(
        src: FloatArray,
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int
    ): FloatArray {
        val dst = FloatArray(dstW * dstH)
        val xRatio = (srcW - 1).toFloat() / max(1, dstW - 1)
        val yRatio = (srcH - 1).toFloat() / max(1, dstH - 1)

        for (y in 0 until dstH) {
            val srcY = y * yRatio
            val yLow = srcY.toInt().coerceIn(0, srcH - 1)
            val yHigh = min(srcH - 1, yLow + 1)
            val yWeight = srcY - yLow

            val rowLow = yLow * srcW
            val rowHigh = yHigh * srcW
            val dstRow = y * dstW

            for (x in 0 until dstW) {
                val srcX = x * xRatio
                val xLow = srcX.toInt().coerceIn(0, srcW - 1)
                val xHigh = min(srcW - 1, xLow + 1)
                val xWeight = srcX - xLow

                val a = src[rowLow + xLow]
                val b = src[rowLow + xHigh]
                val c = src[rowHigh + xLow]
                val d = src[rowHigh + xHigh]

                val interp = a * (1 - xWeight) * (1 - yWeight) +
                        b * xWeight * (1 - yWeight) +
                        c * (1 - xWeight) * yWeight +
                        d * xWeight * yWeight

                dst[dstRow + x] = if (interp.isNaN()) 0f else interp.coerceIn(0f, 1f)
            }
        }
        return dst
    }

    /**
     * Refines alpha mask transitions to eliminate aliasing and stair-stepping,
     * while preserving solid interior regions and razor-sharp true silhouettes.
     */
    private fun refineAlphaMask(
        mask: FloatArray,
        width: Int,
        height: Int
    ): FloatArray {
        val size = width * height
        val out = FloatArray(size)

        for (y in 0 until height) {
            val yPrev = max(0, y - 1)
            val yNext = min(height - 1, y + 1)
            val row = y * width
            val rowPrev = yPrev * width
            val rowNext = yNext * width

            for (x in 0 until width) {
                val idx = row + x
                val center = mask[idx]

                if (center <= 0.01f || center >= 0.99f) {
                    out[idx] = center
                    continue
                }

                val xPrev = max(0, x - 1)
                val xNext = min(width - 1, x + 1)

                val sum = (
                    mask[rowPrev + xPrev] + mask[rowPrev + x] * 2f + mask[rowPrev + xNext] +
                    mask[row + xPrev] * 2f + center * 4f + mask[row + xNext] * 2f +
                    mask[rowNext + xPrev] + mask[rowNext + x] * 2f + mask[rowNext + xNext]
                ) / 16f

                out[idx] = sum.coerceIn(0f, 1f)
            }
        }
        return out
    }

    /**
     * Creates the transparent foreground bitmap by applying the refined alpha mask to original pixels,
     * including background-fringe suppression (defringing) along hair strands and boundary contours.
     */
    fun createRefinedForegroundBitmap(
        original: Bitmap,
        alphaMask: FloatArray,
        width: Int,
        height: Int
    ): Bitmap {
        val srcBmp = if (original.width != width || original.height != height) {
            Bitmap.createScaledBitmap(original, width, height, true)
        } else {
            original
        }

        val origPixels = IntArray(width * height)
        srcBmp.getPixels(origPixels, 0, width, 0, 0, width, height)

        if (srcBmp !== original) {
            srcBmp.recycle()
        }

        val outPixels = IntArray(width * height)
        val outBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                val idx = row + x
                val rawAlpha = alphaMask[idx]
                val alpha = if (rawAlpha.isNaN()) 0f else rawAlpha.coerceIn(0f, 1f)
                val aInt = ((alpha * 255f) + 0.5f).toInt().coerceIn(0, 255)

                if (aInt == 0) {
                    outPixels[idx] = 0
                    continue
                }

                val p = origPixels[idx]
                var r = (p shr 16) and 0xFF
                var g = (p shr 8) and 0xFF
                var b = p and 0xFF

                if (alpha in 0.03f..0.88f) {
                    var fgR = 0
                    var fgG = 0
                    var fgB = 0
                    var fgCount = 0

                    if (x > 0 && alphaMask[idx - 1] >= 0.85f) {
                        val np = origPixels[idx - 1]
                        fgR += (np shr 16) and 0xFF
                        fgG += (np shr 8) and 0xFF
                        fgB += np and 0xFF
                        fgCount++
                    }
                    if (x < width - 1 && alphaMask[idx + 1] >= 0.85f) {
                        val np = origPixels[idx + 1]
                        fgR += (np shr 16) and 0xFF
                        fgG += (np shr 8) and 0xFF
                        fgB += np and 0xFF
                        fgCount++
                    }
                    if (y > 0 && alphaMask[idx - width] >= 0.85f) {
                        val np = origPixels[idx - width]
                        fgR += (np shr 16) and 0xFF
                        fgG += (np shr 8) and 0xFF
                        fgB += np and 0xFF
                        fgCount++
                    }
                    if (y < height - 1 && alphaMask[idx + width] >= 0.85f) {
                        val np = origPixels[idx + width]
                        fgR += (np shr 16) and 0xFF
                        fgG += (np shr 8) and 0xFF
                        fgB += np and 0xFF
                        fgCount++
                    }
                    if (fgCount == 0) {
                        if (x > 1 && alphaMask[idx - 2] >= 0.85f) {
                            val np = origPixels[idx - 2]
                            fgR += (np shr 16) and 0xFF
                            fgG += (np shr 8) and 0xFF
                            fgB += np and 0xFF
                            fgCount++
                        }
                        if (x < width - 2 && alphaMask[idx + 2] >= 0.85f) {
                            val np = origPixels[idx + 2]
                            fgR += (np shr 16) and 0xFF
                            fgG += (np shr 8) and 0xFF
                            fgB += np and 0xFF
                            fgCount++
                        }
                        if (y > 1 && alphaMask[idx - 2 * width] >= 0.85f) {
                            val np = origPixels[idx - 2 * width]
                            fgR += (np shr 16) and 0xFF
                            fgG += (np shr 8) and 0xFF
                            fgB += np and 0xFF
                            fgCount++
                        }
                        if (y < height - 2 && alphaMask[idx + 2 * width] >= 0.85f) {
                            val np = origPixels[idx + 2 * width]
                            fgR += (np shr 16) and 0xFF
                            fgG += (np shr 8) and 0xFF
                            fgB += np and 0xFF
                            fgCount++
                        }
                    }

                    if (fgCount > 0) {
                        val avgR = fgR.toFloat() / fgCount
                        val avgG = fgG.toFloat() / fgCount
                        val avgB = fgB.toFloat() / fgCount
                        val blend = ((1f - alpha) * 0.45f).coerceIn(0f, 0.70f)
                        val nr = ((1f - blend) * r + blend * avgR + 0.5f).toInt()
                        val ng = ((1f - blend) * g + blend * avgG + 0.5f).toInt()
                        val nb = ((1f - blend) * b + blend * avgB + 0.5f).toInt()
                        r = nr.coerceIn(0, 255)
                        g = ng.coerceIn(0, 255)
                        b = nb.coerceIn(0, 255)
                    }
                }

                outPixels[idx] = (aInt shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        outBmp.setPixels(outPixels, 0, width, 0, 0, width, height)
        return outBmp
    }

    /**
     * Applies manual erase or restore brush stroke to an existing alpha mask.
     * - Erase: sets mask alpha towards 0
     * - Restore: restores mask alpha towards 1
     */
    fun applyBrushToMask(
        mask: FloatArray,
        width: Int,
        height: Int,
        centerX: Float,
        centerY: Float,
        radius: Float,
        isRestore: Boolean,
        isSoft: Boolean
    ) {
        val r = radius.coerceAtLeast(1f)
        val minX = max(0, (centerX - r).toInt())
        val maxX = min(width - 1, (centerX + r).toInt())
        val minY = max(0, (centerY - r).toInt())
        val maxY = min(height - 1, (centerY + r).toInt())

        val rSq = r * r

        for (y in minY..maxY) {
            val dy = y - centerY
            val dySq = dy * dy
            val row = y * width
            for (x in minX..maxX) {
                val dx = x - centerX
                val distSq = dx * dx + dySq
                if (distSq <= rSq) {
                    val idx = row + x
                    val currentVal = mask[idx]
                    val factor = if (isSoft) {
                        val dist = kotlin.math.sqrt(distSq)
                        (1f - dist / r).coerceIn(0f, 1f)
                    } else {
                        1f
                    }

                    if (isRestore) {
                        mask[idx] = max(currentVal, currentVal + (1f - currentVal) * factor).coerceIn(0f, 1f)
                    } else {
                        mask[idx] = min(currentVal, currentVal * (1f - factor)).coerceIn(0f, 1f)
                    }
                }
            }
        }
    }
}
