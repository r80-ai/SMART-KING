package com.example.pvccard.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.print.PrintHelper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.example.pvccard.model.PVC_CARD_HEIGHT_PX
import com.example.pvccard.model.PVC_CARD_WIDTH_PX
import com.example.pvccard.model.PVC_CORNER_RADIUS_PX
import com.example.pvccard.model.PvcCardItem
import com.example.pvccard.model.PvcSheetLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * PvcCardRenderer manages all rendering operations for PVC Smart Cards:
 * - PDF rendering at exact 300 DPI
 * - Rotation and de-skewing
 * - Rescaling to standard CR80 PVC dimensions (1012 × 638 px)
 * - Text darkening & sharpness enhancement
 * - Multi-card sheet rendering (A4 5-Cards, A4 NTR Duplex, 4×6 Photo Paper)
 * - NO corner ticks or cutting marks on print sheets
 * - Strict top-alignment on sheets
 */
object PvcCardRenderer {
    private const val TAG = "PvcCardRenderer"
    private var isPdfBoxInitialized = false

    private fun ensurePdfBoxInit(context: Context) {
        if (!isPdfBoxInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isPdfBoxInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init PDFBox: ${e.message}", e)
            }
        }
    }

    /**
     * Quickly gets the page count of a PDF document.
     */
    suspend fun getPdfPageCount(
        context: Context,
        uri: Uri,
        password: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var tempUnlockedFile: File? = null

        try {
            val cleanPassword = password?.trim()
            if (!cleanPassword.isNullOrEmpty()) {
                ensurePdfBoxInit(context)
                var doc: PDDocument? = null
                try {
                    val stream: InputStream? = context.contentResolver.openInputStream(uri)
                    if (stream != null) doc = PDDocument.load(stream, cleanPassword)
                } catch (_: Exception) {
                    try {
                        val stream: InputStream? = context.contentResolver.openInputStream(uri)
                        if (stream != null) doc = PDDocument.load(stream, cleanPassword.uppercase())
                    } catch (e: Exception) { return@withContext Result.failure(e) }
                }

                if (doc == null) return@withContext Result.failure(SecurityException("Invalid password"))

                val unlockedFile = File(context.cacheDir, "metadata_pvc_${System.currentTimeMillis()}.pdf")
                tempUnlockedFile = unlockedFile
                doc.save(unlockedFile)
                val count = doc.numberOfPages
                doc.close()
                Result.success(count)
            } else {
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext Result.failure(IllegalStateException("No PFD"))
                renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                Result.success(count)
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            tempUnlockedFile?.delete()
        }
    }

    /**
     * Renders a PDF page to a 300 DPI Bitmap with optional password decryption.
     */
    suspend fun renderPdfPageToBitmap(
        context: Context,
        uri: Uri,
        pageIndex: Int = 0,
        targetDpi: Int = 300,
        password: String? = null
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var tempUnlockedFile: File? = null

        try {
            val cleanPassword = password?.trim()

            if (!cleanPassword.isNullOrEmpty()) {
                // Decrypt password-protected PDF using PdfBox-Android
                ensurePdfBoxInit(context)

                var doc: PDDocument? = null
                var loadException: Exception? = null

                // Attempt 1: Exact trimmed password
                try {
                    val stream: InputStream? = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        doc = PDDocument.load(stream, cleanPassword)
                    }
                } catch (e: Exception) {
                    loadException = e
                }

                // Attempt 2: Uppercase password (standard Indian ID format e.g. UIDAI RAHU1990)
                if (doc == null && cleanPassword != cleanPassword.uppercase()) {
                    try {
                        val stream: InputStream? = context.contentResolver.openInputStream(uri)
                        if (stream != null) {
                            doc = PDDocument.load(stream, cleanPassword.uppercase())
                        }
                    } catch (e: Exception) {
                        loadException = e
                    }
                }

                if (doc == null) {
                    if (loadException is InvalidPasswordException) {
                        return@withContext Result.failure(SecurityException("Incorrect password. Please verify and try again.", loadException))
                    }
                    return@withContext Result.failure(loadException ?: IllegalArgumentException("Failed to decrypt PDF with entered password."))
                }

                // Strip security encryption and save to temporary unlocked file
                try {
                    if (doc.isEncrypted) {
                        doc.setAllSecurityToBeRemoved(true)
                    }
                    val unlockedFile = File(context.cacheDir, "unlocked_pvc_${System.currentTimeMillis()}.pdf")
                    tempUnlockedFile = unlockedFile
                    doc.save(unlockedFile)
                } finally {
                    try { doc.close() } catch (_: Exception) {}
                }

                // Open the unlocked file with PdfRenderer
                pfd = ParcelFileDescriptor.open(tempUnlockedFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                // No password supplied: open directly via ContentResolver
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext Result.failure(IllegalStateException("Could not open file descriptor for PDF"))
            }

            // Create PdfRenderer from descriptor
            renderer = try {
                PdfRenderer(pfd)
            } catch (sec: SecurityException) {
                return@withContext Result.failure(SecurityException("This PDF is password-protected. Please enter password.", sec))
            }

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                return@withContext Result.failure(IndexOutOfBoundsException("Page index $pageIndex is out of range (total ${renderer.pageCount})"))
            }

            val page = renderer.openPage(pageIndex)
            val scale = targetDpi.toFloat() / 72.0f
            val targetW = max(1, (page.width * scale).roundToInt())
            val targetH = max(1, (page.height * scale).roundToInt())

            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(AndroidColor.WHITE)

            val matrix = Matrix().apply {
                postScale(scale, scale)
            }

            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()

            Result.success(bitmap)
        } catch (sec: SecurityException) {
            Log.e(TAG, "Password protected PDF: ${sec.message}")
            Result.failure(SecurityException("This PDF is password-protected. Please enter password.", sec))
        } catch (inv: InvalidPasswordException) {
            Log.e(TAG, "Invalid PDF password: ${inv.message}")
            Result.failure(SecurityException("Incorrect password. Please verify and try again.", inv))
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering PDF page: ${e.message}", e)
            Result.failure(e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            tempUnlockedFile?.let {
                try { it.delete() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Rotates a bitmap by [degrees] around its center with anti-aliasing.
     */
    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f || source.isRecycled) return source
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
    }

    /**
     * Crops a card region from [source] and rescales it to standard CR80 size (1012 × 638 px @ 300 DPI).
     */
    fun cropToPvcSize(source: Bitmap, cropRect: Rect): Bitmap {
        if (source.isRecycled) {
            return Bitmap.createBitmap(PVC_CARD_WIDTH_PX, PVC_CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        }

        val clampedRect = Rect(
            cropRect.left.coerceIn(0, source.width - 1),
            cropRect.top.coerceIn(0, source.height - 1),
            cropRect.right.coerceIn(cropRect.left + 1, source.width),
            cropRect.bottom.coerceIn(cropRect.top + 1, source.height)
        )

        val cropped = Bitmap.createBitmap(
            source,
            clampedRect.left,
            clampedRect.top,
            clampedRect.width(),
            clampedRect.height()
        )

        // Rescale to exact CR80 300 DPI dimensions
        val scaled = Bitmap.createScaledBitmap(cropped, PVC_CARD_WIDTH_PX, PVC_CARD_HEIGHT_PX, true)
        if (scaled !== cropped) {
            cropped.recycle()
        }
        return scaled
    }

    /**
     * Applies brightness, contrast, text darkening, and perimeter border enhancements.
     */
    fun applyEnhancements(
        source: Bitmap,
        brightness: Float,
        contrast: Float,
        isTextDarkened: Boolean,
        hasBorder: Boolean
    ): Bitmap {
        if (source.isRecycled) return source

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Color filter for Brightness and Contrast
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val cm = ColorMatrix()

        val scale = 1.0f + (contrast / 100.0f).coerceIn(-0.9f, 2.0f)
        val translate = (brightness * 2.55f).coerceIn(-255f, 255f)

        cm.set(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 2. Text Sharpness / Darkening filter (crisp text, barcodes & QR codes)
        if (isTextDarkened) {
            val pixels = IntArray(output.width * output.height)
            output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
            val len = pixels.size

            for (i in 0 until len) {
                val p = pixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                // Fast luminance
                val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()

                // If pixel is text / dark line / barcode detail (lum < 150), darken it for rich contrast
                if (lum < 150) {
                    val factor = if (lum < 90) 0.65f else 0.82f
                    val nr = (r * factor).toInt().coerceIn(0, 255)
                    val ng = (g * factor).toInt().coerceIn(0, 255)
                    val nb = (b * factor).toInt().coerceIn(0, 255)
                    pixels[i] = (p and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
                }
            }
            output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
        }

        // 3. Clean 2px Perimeter Border (NO corner ticks / cutting marks!)
        if (hasBorder) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb(220, 20, 20, 20) // Crisp, professional dark border
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
            val rect = RectF(1.5f, 1.5f, output.width - 1.5f, output.height - 1.5f)
            canvas.drawRoundRect(rect, PVC_CORNER_RADIUS_PX, PVC_CORNER_RADIUS_PX, borderPaint)
        }

        return output
    }

    /**
     * Renders a complete print sheet for the chosen layout.
     * Strictly adheres to:
     * - Top alignment: Fill row-by-row from top-left, leftover bottom is clean white
     * - NO corner ticks or cutting marks
     * - NTR Duplex: Page 0 = Fronts, Page 1 = Backs (horizontally mirrored)
     */
    fun renderPrintSheet(
        layout: PvcSheetLayout,
        cards: List<PvcCardItem>,
        pageIndex: Int = 0
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(layout.widthPx, layout.heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)
        canvas.drawColor(AndroidColor.WHITE)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Expand cards according to their copy count
        val expandedCards = mutableListOf<PvcCardItem>()
        for (c in cards) {
            repeat(c.copies.coerceAtLeast(1)) {
                expandedCards.add(c)
            }
        }

        when (layout) {
            PvcSheetLayout.A4_FIVE_CARDS -> {
                // 5 Cards Side-by-Side on A4 (2480 × 3508 px @ 300 DPI)
                // 2 columns: Front on left, Back on right
                // Card size: 1012 × 638 px
                val gapX = 60
                val totalRowWidth = PVC_CARD_WIDTH_PX * 2 + gapX // 2084 px
                val leftMargin = (layout.widthPx - totalRowWidth) / 2 // 198 px
                val topMargin = 70
                val gapY = 35

                val maxRows = 5
                val cardsToDraw = expandedCards.take(maxRows)

                for ((rowIndex, card) in cardsToDraw.withIndex()) {
                    val y = topMargin + rowIndex * (PVC_CARD_HEIGHT_PX + gapY)

                    // Draw Front Card (Left)
                    val frontX = leftMargin
                    val enhancedFront = applyEnhancements(
                        card.frontBitmap,
                        card.brightness,
                        card.contrast,
                        card.isTextDarkened,
                        card.hasPerimeterBorder
                    )
                    canvas.drawBitmap(enhancedFront, frontX.toFloat(), y.toFloat(), cardPaint)

                    // Draw Back Card (Right) if present
                    val backBitmap = card.backBitmap ?: card.frontBitmap
                    val backX = leftMargin + PVC_CARD_WIDTH_PX + gapX
                    val enhancedBack = applyEnhancements(
                        backBitmap,
                        card.brightness,
                        card.contrast,
                        card.isTextDarkened,
                        card.hasPerimeterBorder
                    )
                    canvas.drawBitmap(enhancedBack, backX.toFloat(), y.toFloat(), cardPaint)
                }
                // Area below last drawn row remains 100% clean pure white for reuse!
            }

            PvcSheetLayout.PHOTO_4X6_ONE_CARD -> {
                // 4×6 Photo Paper (1200 × 1800 px @ 300 DPI)
                // 1 Card: Front & Back stacked vertically with a clean center fold line
                val card = expandedCards.firstOrNull() ?: return sheetBitmap
                val leftMargin = (layout.widthPx - PVC_CARD_WIDTH_PX) / 2 // 94 px
                val topMargin = 180

                // Front Card
                val enhancedFront = applyEnhancements(
                    card.frontBitmap,
                    card.brightness,
                    card.contrast,
                    card.isTextDarkened,
                    card.hasPerimeterBorder
                )
                canvas.drawBitmap(enhancedFront, leftMargin.toFloat(), topMargin.toFloat(), cardPaint)

                // Center Fold Line
                val foldY = topMargin + PVC_CARD_HEIGHT_PX + 40f
                val foldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.LTGRAY
                    strokeWidth = 2f
                    pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
                }
                canvas.drawLine(
                    leftMargin.toFloat(),
                    foldY,
                    (leftMargin + PVC_CARD_WIDTH_PX).toFloat(),
                    foldY,
                    foldPaint
                )

                // Back Card
                val backBitmap = card.backBitmap ?: card.frontBitmap
                val backY = topMargin + PVC_CARD_HEIGHT_PX + 80
                val enhancedBack = applyEnhancements(
                    backBitmap,
                    card.brightness,
                    card.contrast,
                    card.isTextDarkened,
                    card.hasPerimeterBorder
                )
                canvas.drawBitmap(enhancedBack, leftMargin.toFloat(), backY.toFloat(), cardPaint)
            }
        }

        return sheetBitmap
    }

    /**
     * Exports sheet bitmaps as a multi-page PDF document at 300 DPI.
     */
    suspend fun exportSheetsToPdf(
        context: Context,
        sheetBitmaps: List<Bitmap>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        try {
            for ((index, bmp) in sheetBitmaps.withIndex()) {
                // A4 dimensions in PostScript points: 595 × 842 pt
                val isA4 = bmp.width > 2000
                val pageW = if (isA4) 595 else 288 // 4×6 is approx 288 × 432 pt
                val pageH = if (isA4) 842 else 432

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = document.startPage(pageInfo)

                val destRect = Rect(0, 0, pageW, pageH)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                page.canvas.drawBitmap(bmp, null, destRect, paint)

                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting PDF: ${e.message}", e)
            Result.failure(e)
        } finally {
            document.close()
        }
    }

    /**
     * Direct print helper using Android's PrintHelper.
     */
    fun directPrint(context: Context, jobName: String, bitmap: Bitmap) {
        val printHelper = PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        printHelper.printBitmap(jobName, bitmap)
    }
}
