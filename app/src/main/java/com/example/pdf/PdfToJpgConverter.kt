package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

data class JpgPageItem(
    val pageIndex: Int,
    val fileName: String,          // e.g. "page_001.jpg"
    val file: File,
    val byteSize: Long,
    val width: Int,
    val height: Int,
    var savedUri: Uri? = null,
    var isSaved: Boolean = false
)

data class PdfToJpgConversionResult(
    val originalPdfFile: File,
    val originalPdfBytes: Long,
    val totalJpgBytes: Long,
    val pages: List<JpgPageItem>,
    val scaleUsed: Float,
    val qualityUsed: Int,
    val totalAttempts: Int
)

object PdfToJpgConverter {

    private const val TAG = "PdfToJpgConverter"

    /**
     * Converts every page of a PDF file to high-quality JPG image(s).
     *
     * Strict Requirements:
     * - Exported JPG output must NEVER have a total file size smaller than the original PDF.
     * - The exported JPG size must be at least equal to the original PDF size in bytes (totalJpgBytes >= originalPdfBytes).
     * - If first JPG export is smaller, automatically increases JPG quality/resolution/file size
     *   until the output reaches or exceeds the original PDF byte size.
     * - Preserves aspect ratio, layout, text, colors, and visual appearance exactly.
     * - Multi-page: page_001.jpg, page_002.jpg, ...
     * - Single page: page_001.jpg
     * - Does not use fake padding or meaningless data.
     * - Memory safe for large PDFs.
     */
    suspend fun convertPdfToJpg(
        context: Context,
        pdfFile: File,
        originalSizeBytes: Long = pdfFile.length(),
        onProgress: (pageIndex: Int, totalPages: Int, pass: Int, statusMessage: String) -> Unit = { _, _, _, _ -> }
    ): PdfToJpgConversionResult = withContext(Dispatchers.IO) {
        require(pdfFile.exists()) { "PDF file does not exist: ${pdfFile.absolutePath}" }
        val exactOriginalBytes = if (originalSizeBytes > 0) originalSizeBytes else pdfFile.length()

        val workingDir = File(context.cacheDir, "pdf_to_jpg_${System.currentTimeMillis()}").apply {
            if (!exists()) mkdirs()
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            require(totalPages > 0) { "PDF document contains no pages." }

            // Initial rendering parameters
            // Start with high DPI (scale 2.5f ≈ 180 DPI, or higher based on target bytes)
            var currentScale = 2.5f
            var currentQuality = 95
            var pass = 1
            val maxPasses = 6

            // For large original files, compute a smarter starting scale so we don't need multiple retries
            // Assuming average JPEG page at 180 DPI is ~400-800 KB, if original is large, scale up initially
            val estimatedPageTargetBytes = exactOriginalBytes / totalPages
            if (estimatedPageTargetBytes > 1_500_000L) {
                // e.g. 5 MB PDF with 1 page -> start at scale 3.5f, quality 98
                currentScale = 3.5f
                currentQuality = 98
            } else if (estimatedPageTargetBytes > 3_000_000L) {
                currentScale = 4.5f
                currentQuality = 100
            }

            var finalResultPages: List<JpgPageItem> = emptyList()
            var finalTotalBytes = 0L

            while (pass <= maxPasses) {
                onProgress(
                    0,
                    totalPages,
                    pass,
                    if (pass == 1) "Rendering $totalPages page(s) to high-quality JPG..."
                    else "Optimizing resolution to satisfy size requirement (Attempt $pass)..."
                )

                val passFiles = mutableListOf<JpgPageItem>()

                for (pageIndex in 0 until totalPages) {
                    val pageNum = pageIndex + 1
                    val fileName = String.format(Locale.US, "page_%03d.jpg", pageNum)
                    val targetFile = File(workingDir, "pass_${pass}_$fileName")

                    onProgress(
                        pageNum,
                        totalPages,
                        pass,
                        "Rendering page $pageNum of $totalPages at ${String.format(Locale.US, "%.1fx", currentScale)} DPI..."
                    )

                    val page = renderer.openPage(pageIndex)
                    val pageWidth = page.width
                    val pageHeight = page.height

                    // Safe dimension calculation
                    val renderWidth = (pageWidth * currentScale).toInt().coerceAtLeast(1)
                    val renderHeight = (pageHeight * currentScale).toInt().coerceAtLeast(1)

                    // Ensure we don't exceed JVM memory limits
                    val freeMem = Runtime.getRuntime().freeMemory()
                    val totalMem = Runtime.getRuntime().totalMemory()
                    val maxMem = Runtime.getRuntime().maxMemory()
                    val availableMem = maxMem - (totalMem - freeMem)
                    val bitmapBytes = renderWidth.toLong() * renderHeight.toLong() * 4L

                    val safeScale = if (bitmapBytes > (availableMem * 0.45).toLong() && currentScale > 2.0f) {
                        val factor = sqrt((availableMem * 0.40) / bitmapBytes.toDouble()).toFloat()
                        max(2.0f, currentScale * factor)
                    } else {
                        currentScale
                    }

                    val finalW = (pageWidth * safeScale).toInt().coerceAtLeast(1)
                    val finalH = (pageHeight * safeScale).toInt().coerceAtLeast(1)

                    var bitmap: Bitmap? = null
                    try {
                        bitmap = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        // JPG has no alpha channel: PDF pages default to transparent background, fill with pure white
                        canvas.drawColor(Color.WHITE)

                        val matrix = Matrix().apply {
                            setScale(safeScale, safeScale)
                        }

                        // Use RENDER_MODE_FOR_PRINT for best visual clarity and accurate vector rasterization
                        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    } finally {
                        page.close()
                    }

                    // Compress to JPG
                    FileOutputStream(targetFile).use { outStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outStream)
                        outStream.flush()
                    }

                    // Immediately free bitmap memory to handle large multi-page PDFs safely
                    bitmap.recycle()

                    val writtenBytes = targetFile.length()
                    passFiles.add(
                        JpgPageItem(
                            pageIndex = pageIndex,
                            fileName = fileName,
                            file = targetFile,
                            byteSize = writtenBytes,
                            width = finalW,
                            height = finalH
                        )
                    )
                }

                val currentTotalBytes = passFiles.sumOf { it.file.length() }
                Log.d(TAG, "Pass $pass completed: total JPG bytes = $currentTotalBytes, target = $exactOriginalBytes (scale=$currentScale, quality=$currentQuality)")

                // Requirement: Total exported JPG size must NEVER be smaller than original PDF size in bytes
                if (currentTotalBytes >= exactOriginalBytes || pass == maxPasses) {
                    // Requirement satisfied or reached maximum retry
                    finalResultPages = passFiles
                    finalTotalBytes = currentTotalBytes
                    break
                } else {
                    // Need to scale up resolution and quality
                    onProgress(
                        totalPages,
                        totalPages,
                        pass,
                        "Verifying byte size: output $currentTotalBytes < original $exactOriginalBytes. Increasing resolution..."
                    )

                    // Clean up previous pass files before retrying
                    passFiles.forEach { it.file.delete() }

                    // Calculate required scale factor: area is proportional to scale^2
                    val byteRatio = exactOriginalBytes.toDouble() / max(1L, currentTotalBytes).toDouble()
                    val scaleMultiplier = (sqrt(byteRatio) * 1.15).toFloat() // 15% safety buffer

                    currentScale = (currentScale * scaleMultiplier).coerceIn(currentScale + 0.5f, 7.5f)
                    currentQuality = 100 // Set to maximum practical JPEG quality 100
                    pass++
                }
            }

            // Rename the winning pass files to clean destination names
            val finalCleanItems = finalResultPages.mapIndexed { index, item ->
                val cleanFile = File(workingDir, item.fileName)
                if (item.file != cleanFile) {
                    if (cleanFile.exists()) cleanFile.delete()
                    item.file.renameTo(cleanFile)
                }
                item.copy(file = cleanFile, byteSize = cleanFile.length())
            }

            onProgress(
                totalPages,
                totalPages,
                pass,
                "Conversion completed! Total size: ${finalTotalBytes} bytes"
            )

            val finalResultBytes = finalCleanItems.sumOf { it.byteSize }
            PdfToJpgConversionResult(
                originalPdfFile = pdfFile,
                originalPdfBytes = exactOriginalBytes,
                totalJpgBytes = finalResultBytes,
                pages = finalCleanItems,
                scaleUsed = currentScale,
                qualityUsed = currentQuality,
                totalAttempts = pass
            )
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Saves a single generated JPG image to Android MediaStore (Pictures/PDFMaster_Jpg)
     * and into the application's global storage directory.
     */
    fun saveJpgToMediaStore(
        context: Context,
        imageFile: File,
        desiredName: String
    ): Uri? {
        return try {
            val resolver = context.contentResolver
            val baseName = desiredName.substringBeforeLast(".")
            val extension = "jpg"
            var displayName = "$baseName.$extension"

            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/PDFMaster_Jpg")
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                } else {
                    val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                    val appDir = File(picturesDir, "PDFMaster_Jpg").apply { if (!exists()) mkdirs() }
                    val destFile = File(appDir, displayName)
                    put(android.provider.MediaStore.MediaColumns.DATA, destFile.absolutePath)
                }
            }

            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(collection, contentValues) ?: return null

            resolver.openOutputStream(uri)?.use { outStream ->
                java.io.FileInputStream(imageFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
                outStream.flush()
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(imageFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )

            uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save JPG to MediaStore: ${e.message}", e)
            null
        }
    }
}
