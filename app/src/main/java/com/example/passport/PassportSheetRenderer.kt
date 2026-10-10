package com.example.passport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.photo.PhotoEditorEngine
import com.example.photo.birefnet.BiRefNetLiteEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * PassportSheetRenderer provides mathematically calibrated rendering for
 * Multiple Passport Photo Maker sheets and individual cropped photos.
 *
 * Strictly adheres to the TOP-ALIGNMENT rule:
 * - Photos always align to the TOP-LEFT of the sheet.
 * - Rows fill sequentially from row 0 down.
 * - Below the last photo row is 100% untouched pure white space for clean cutting and paper reuse.
 *
 * Memory Safeguards:
 * - Preview sheets render at downscaled resolution using RGB_565 (1.5 - 2MB vs 35MB).
 * - Full 300 DPI is generated on background threads during PDF/Image export.
 * - Safe guards against zero-dimensions, recycled bitmaps, and OutOfMemoryError.
 */
object PassportSheetRenderer {

    private const val TAG = "PassportSheetRenderer"

    /**
     * Compute grid layout parameters for the selected paper format, photo dimensions, and DPI.
     */
    fun computeGrid(
        paper: PaperSheetFormat,
        photoWidthMm: Float,
        photoHeightMm: Float,
        targetDpi: Int = 300
    ): SheetGridCalculation {
        val scaleFactor = targetDpi.toFloat() / 300.0f
        val sheetWidthPx = Math.round(paper.widthPixels * scaleFactor).coerceAtLeast(200)
        val sheetHeightPx = Math.round(paper.heightPixels * scaleFactor).coerceAtLeast(200)

        val photoWidthPx = Math.round((photoWidthMm / 25.4f) * targetDpi).coerceAtLeast(20)
        val photoHeightPx = Math.round((photoHeightMm / 25.4f) * targetDpi).coerceAtLeast(20)

        return when (paper) {
            PaperSheetFormat.A4 -> {
                // 1.2 × 1.5 in (approx 30.5 × 38.1 mm): exactly 6 columns × 7 rows = 42 capacity!
                val isInch12x15 = (photoWidthMm in 29.0f..31.8f && photoHeightMm in 36.5f..39.5f)
                // 3.3 × 4.3 cm photo: exactly 6 columns × 6 rows = 36 capacity!
                val isDefault33x43 = (photoWidthMm in 32.0f..35.0f && photoHeightMm in 41.0f..45.0f)

                // 1. Photo Dimensions:
                // For 3.3 x 4.3 cm: Calibrated 385 x 502 px to fit 6 columns within 2480px A4 printable width without clipping
                // For 1.2 x 1.5 in: Strictly untouched original dimensions (360 x 450 px @ 300 DPI)
                val finalPhotoWidthPx = if (isDefault33x43) {
                    Math.round(385 * scaleFactor).coerceAtLeast(20)
                } else {
                    photoWidthPx
                }
                val finalPhotoHeightPx = if (isDefault33x43) {
                    Math.round(502 * scaleFactor).coerceAtLeast(20)
                } else {
                    photoHeightPx
                }

                // 2. Horizontal Gap (between columns):
                // For 3.3 x 4.3 cm: 20 px so all 6 columns fit inside canvas with balanced margins
                // For 1.2 x 1.5 in: Strictly untouched at 30 px
                val gapX = if (isDefault33x43) {
                    Math.round(20 * scaleFactor).coerceAtLeast(1)
                } else {
                    Math.round(30 * scaleFactor).coerceAtLeast(1)
                }

                // 3. Vertical Gap (between rows): exactly 40 px
                val gapY = Math.round(40 * scaleFactor).coerceAtLeast(1)

                // 4. Top Margin: exactly 73 px
                val marginY = Math.round(73 * scaleFactor).coerceAtLeast(1)

                val cols = 6
                val rows = when {
                    isInch12x15 -> 7
                    isDefault33x43 -> 6
                    else -> {
                        val maxAvailableH = sheetHeightPx - marginY - Math.round(47 * scaleFactor)
                        max(1, (maxAvailableH + gapY) / (finalPhotoHeightPx + gapY)).coerceAtMost(8)
                    }
                }

                // 5. Left Margin:
                // For 3.3 x 4.3 cm: Horizontally centered (leftMargin = rightMargin = 35 px) preventing 6th photo clipping
                // For 1.2 x 1.5 in: Strictly untouched at 85 px
                val marginX = if (isDefault33x43) {
                    val totalRowWidth = cols * finalPhotoWidthPx + (cols - 1) * gapX
                    max(0, (sheetWidthPx - totalRowWidth) / 2)
                } else {
                    Math.round(85 * scaleFactor).coerceAtLeast(1)
                }

                SheetGridCalculation(
                    paper = paper,
                    photoWidthPx = finalPhotoWidthPx,
                    photoHeightPx = finalPhotoHeightPx,
                    cols = cols,
                    rows = rows,
                    maxCapacity = cols * rows,
                    marginXPx = marginX,
                    marginYPx = marginY,
                    gapXPx = gapX,
                    gapYPx = gapY
                )
            }

            PaperSheetFormat.PHOTO_4X6_PORTRAIT -> {
                // STRICT 4×6 PORTRAIT:
                // Fixed: exactly 3 columns across, 4 rows down = 12 photos max capacity
                val cols = 3
                val rows = 4
                val maxCapacity = 12

                // Fixed Margins: Top = exactly 20 px, Bottom = exactly 30 px
                val topMargin = Math.round(20 * scaleFactor).coerceAtLeast(1)
                val bottomMargin = Math.round(30 * scaleFactor).coerceAtLeast(1)
                val availableHeight = sheetHeightPx - topMargin - bottomMargin

                val baseGap = Math.round(24 * scaleFactor).coerceAtLeast(2)
                val totalGapsY = (rows - 1) * baseGap
                val maxH = (availableHeight - totalGapsY) / rows

                val minSideMargin = Math.round(24 * scaleFactor).coerceAtLeast(2)
                val totalGapsX = (cols - 1) * baseGap
                val maxW = (sheetWidthPx - 2 * minSideMargin - totalGapsX) / cols

                val photoAspect = photoWidthMm / photoHeightMm
                val finalPhotoHeightPx: Int
                val finalPhotoWidthPx: Int
                if (maxW / photoAspect <= maxH) {
                    finalPhotoWidthPx = maxW
                    finalPhotoHeightPx = Math.round(finalPhotoWidthPx / photoAspect).coerceAtLeast(10)
                } else {
                    finalPhotoHeightPx = maxH
                    finalPhotoWidthPx = Math.round(finalPhotoHeightPx * photoAspect).coerceAtLeast(10)
                }

                val gapX = baseGap
                val gapY = baseGap
                val totalRowWidth = cols * finalPhotoWidthPx + (cols - 1) * gapX
                val marginX = max(0, (sheetWidthPx - totalRowWidth) / 2)

                SheetGridCalculation(
                    paper = paper,
                    photoWidthPx = finalPhotoWidthPx,
                    photoHeightPx = finalPhotoHeightPx,
                    cols = cols,
                    rows = rows,
                    maxCapacity = maxCapacity,
                    marginXPx = marginX,
                    marginYPx = topMargin,
                    gapXPx = gapX,
                    gapYPx = gapY
                )
            }

            PaperSheetFormat.PHOTO_4X6_LANDSCAPE -> {
                // STRICT 4×6 LANDSCAPE:
                // Fixed: exactly 5 columns across, 3 rows down = 15 photos max capacity
                val cols = 5
                val rows = 3
                val maxCapacity = 15

                // Fixed Margins: Top = exactly 20 px, Bottom = exactly 30 px
                val topMargin = Math.round(20 * scaleFactor).coerceAtLeast(1)
                val bottomMargin = Math.round(30 * scaleFactor).coerceAtLeast(1)
                val availableHeight = sheetHeightPx - topMargin - bottomMargin

                val baseGap = Math.round(24 * scaleFactor).coerceAtLeast(2)
                val totalGapsY = (rows - 1) * baseGap
                val maxH = (availableHeight - totalGapsY) / rows

                val minSideMargin = Math.round(24 * scaleFactor).coerceAtLeast(2)
                val totalGapsX = (cols - 1) * baseGap
                val maxW = (sheetWidthPx - 2 * minSideMargin - totalGapsX) / cols

                val photoAspect = photoWidthMm / photoHeightMm
                val finalPhotoHeightPx: Int
                val finalPhotoWidthPx: Int
                if (maxW / photoAspect <= maxH) {
                    finalPhotoWidthPx = maxW
                    finalPhotoHeightPx = Math.round(finalPhotoWidthPx / photoAspect).coerceAtLeast(10)
                } else {
                    finalPhotoHeightPx = maxH
                    finalPhotoWidthPx = Math.round(finalPhotoHeightPx * photoAspect).coerceAtLeast(10)
                }

                val gapX = baseGap
                val gapY = baseGap
                val totalRowWidth = cols * finalPhotoWidthPx + (cols - 1) * gapX
                val marginX = max(0, (sheetWidthPx - totalRowWidth) / 2)

                SheetGridCalculation(
                    paper = paper,
                    photoWidthPx = finalPhotoWidthPx,
                    photoHeightPx = finalPhotoHeightPx,
                    cols = cols,
                    rows = rows,
                    maxCapacity = maxCapacity,
                    marginXPx = marginX,
                    marginYPx = topMargin,
                    gapXPx = gapX,
                    gapYPx = gapY
                )
            }
        }
    }

    /**
     * Render an individual photo with zoom, pan, brightness, contrast, saturation,
     * and background settings applied at target pixel resolution.
     */
    suspend fun renderIndividualPhoto(
        context: Context,
        item: PassportPhotoItem,
        targetWidthPx: Int,
        targetHeightPx: Int,
        useRgb565: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        val safeW = targetWidthPx.coerceIn(20, 3000)
        val safeH = targetHeightPx.coerceIn(20, 3000)
        val config = if (useRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888

        val outBitmap = try {
            Bitmap.createBitmap(safeW, safeH, config)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            Bitmap.createBitmap(safeW, safeH, Bitmap.Config.RGB_565)
        }
        val canvas = Canvas(outBitmap)

        // 1. Prepare background
        when (item.backgroundOption.type) {
            BackgroundStyleType.ORIGINAL -> {
                canvas.drawColor(AndroidColor.WHITE)
            }
            BackgroundStyleType.SOLID -> {
                canvas.drawColor(item.backgroundOption.solidColor.toArgb())
            }
            BackgroundStyleType.GRADIENT -> {
                val shader = LinearGradient(
                    0f, 0f, 0f, safeH.toFloat(),
                    item.backgroundOption.gradientColors.first.toArgb(),
                    item.backgroundOption.gradientColors.second.toArgb(),
                    Shader.TileMode.CLAMP
                )
                val bgPaint = Paint().apply { this.shader = shader }
                canvas.drawRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), bgPaint)
            }
        }

        // 2. Safe Subject Bitmap
        val origBmp = item.originalBitmap
        if (origBmp.isRecycled) {
            return@withContext outBitmap
        }

        val sourceBitmap: Bitmap = if (item.backgroundOption.type != BackgroundStyleType.ORIGINAL) {
            item.segmentedForeground?.takeIf { !it.isRecycled } ?: try {
                val res = BiRefNetLiteEngine.removeBackground(context, origBmp)
                res.getOrNull()?.transparentForeground?.takeIf { !it.isRecycled }
                    ?: PhotoEditorEngine.removeBackground(origBmp).takeIf { !it.isRecycled }
                    ?: origBmp
            } catch (t: Throwable) {
                Log.w(TAG, "BiRefNet fallback to fast removal: ${t.message}")
                try {
                    PhotoEditorEngine.removeBackground(origBmp).takeIf { !it.isRecycled } ?: origBmp
                } catch (_: Throwable) {
                    origBmp
                }
            }
        } else {
            origBmp
        }

        if (sourceBitmap.isRecycled || sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
            return@withContext outBitmap
        }

        // 3. Setup color filter for Brightness, Contrast, Saturation
        val filterPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val cm = ColorMatrix()
        val satScale = 1.0f + (item.saturation / 100f)
        cm.setSaturation(satScale.coerceAtLeast(0f))

        val contrastFactor = if (item.contrast >= 0) {
            1.0f + (item.contrast / 100f) * 1.5f
        } else {
            1.0f + (item.contrast / 100f) * 0.7f
        }
        val brightnessOffset = (item.brightness / 100f) * 128f

        val cmContrast = ColorMatrix(
            floatArrayOf(
                contrastFactor, 0f, 0f, 0f, brightnessOffset,
                0f, contrastFactor, 0f, 0f, brightnessOffset,
                0f, 0f, contrastFactor, 0f, brightnessOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(cmContrast)
        filterPaint.colorFilter = ColorMatrixColorFilter(cm)

        // 4. Calculate crop, zoom, and pan placement
        val srcW = sourceBitmap.width.toFloat()
        val srcH = sourceBitmap.height.toFloat()

        val scaleX = safeW.toFloat() / srcW
        val scaleY = safeH.toFloat() / srcH
        val baseScale = max(scaleX, scaleY)
        val finalScale = baseScale * item.zoomScale.coerceAtLeast(1.0f)

        val scaledW = srcW * finalScale
        val scaledH = srcH * finalScale

        val drawLeft = (safeW - scaledW) / 2f + item.panOffsetX
        val drawTop = (safeH - scaledH) / 2f + item.panOffsetY

        val destRect = RectF(drawLeft, drawTop, drawLeft + scaledW, drawTop + scaledH)
        val srcRect = Rect(0, 0, sourceBitmap.width, sourceBitmap.height)
        try {
            canvas.drawBitmap(sourceBitmap, srcRect, destRect, filterPaint)
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing photo bitmap: ${e.message}")
        }

        outBitmap
    }

    /**
     * Renders multi-page or single-page print sheets.
     * When [isPreview] is true, uses a calibrated 100 DPI target resolution and RGB_565
     * to avoid OutOfMemoryError while displaying perfectly crisp in Compose UI.
     */
    suspend fun renderPrintSheets(
        context: Context,
        photos: List<PassportPhotoItem>,
        paper: PaperSheetFormat,
        photoWidthMm: Float,
        photoHeightMm: Float,
        borderEnabled: Boolean = true,
        borderColor: Color = Color.Black,
        isPreview: Boolean = false
    ): List<Bitmap> = withContext(Dispatchers.Default) {
        // 300 DPI canvas resolution across A4 and 4x6 ensures exact pixel margins and crisp preview
        val targetDpi = 300
        val grid = computeGrid(paper, photoWidthMm, photoHeightMm, targetDpi)

        val scaleFactor = targetDpi.toFloat() / 300.0f
        val sheetW = Math.round(paper.widthPixels * scaleFactor).coerceAtLeast(200)
        val sheetH = Math.round(paper.heightPixels * scaleFactor).coerceAtLeast(200)

        // Expand photos by their copy count into a flattened list of items
        val flattenedPhotos = mutableListOf<PassportPhotoItem>()
        for (item in photos) {
            if (!item.originalBitmap.isRecycled) {
                val copies = item.copiesCount.coerceAtLeast(0)
                repeat(copies) {
                    flattenedPhotos.add(item)
                }
            }
        }

        if (flattenedPhotos.isEmpty()) {
            val emptySheet = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.RGB_565).apply {
                eraseColor(AndroidColor.WHITE)
            }
            return@withContext listOf(emptySheet)
        }

        // Cache pre-rendered photo bitmaps by photo item ID to avoid re-rendering duplicates
        val renderedCache = mutableMapOf<String, Bitmap>()
        for (item in photos) {
            if (item.copiesCount > 0 && !item.originalBitmap.isRecycled && !renderedCache.containsKey(item.id)) {
                val bmp = renderIndividualPhoto(context, item, grid.photoWidthPx, grid.photoHeightPx, useRgb565 = true)
                renderedCache[item.id] = bmp
            }
        }

        val totalCapacity = max(1, grid.maxCapacity)
        val totalSheets = max(1, (flattenedPhotos.size + totalCapacity - 1) / totalCapacity)
        val sheetBitmaps = mutableListOf<Bitmap>()

        val borderPaint = Paint().apply {
            color = borderColor.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = (2f * scaleFactor).coerceAtLeast(1f) // Crisp fixed 2px border at 300 DPI
            isAntiAlias = true
        }

        for (sheetIdx in 0 until totalSheets) {
            val sheetBitmap = try {
                Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.RGB_565).apply {
                    eraseColor(AndroidColor.WHITE)
                }
            } catch (oom: OutOfMemoryError) {
                System.gc()
                Bitmap.createBitmap(sheetW / 2, sheetH / 2, Bitmap.Config.RGB_565).apply {
                    eraseColor(AndroidColor.WHITE)
                }
            }

            val canvas = Canvas(sheetBitmap)
            canvas.drawColor(AndroidColor.WHITE)

            val startIndex = sheetIdx * totalCapacity
            val endIndex = min(startIndex + totalCapacity, flattenedPhotos.size)
            val photosOnThisSheet = flattenedPhotos.subList(startIndex, endIndex)

            var currentPhotoIdx = 0
            val placedRects = mutableListOf<RectF>()

            // Fill row-by-row strictly top-aligned
            for (r in 0 until grid.rows) {
                if (currentPhotoIdx >= photosOnThisSheet.size) break

                for (c in 0 until grid.cols) {
                    if (currentPhotoIdx >= photosOnThisSheet.size) break

                    val photoItem = photosOnThisSheet[currentPhotoIdx]
                    val cachedBmp = renderedCache[photoItem.id]

                    val left = grid.marginXPx + c * (grid.photoWidthPx + grid.gapXPx).toFloat()
                    val top = grid.marginYPx + r * (grid.photoHeightPx + grid.gapYPx).toFloat()
                    val right = left + grid.photoWidthPx
                    val bottom = top + grid.photoHeightPx

                    val rect = RectF(left, top, right, bottom)
                    placedRects.add(rect)

                    // Draw photo bitmap
                    if (cachedBmp != null && !cachedBmp.isRecycled) {
                        try {
                            canvas.drawBitmap(cachedBmp, null, rect, null)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed drawing cached photo: ${e.message}")
                        }
                    }

                    // Draw border if requested
                    if (borderEnabled) {
                        canvas.drawRect(rect, borderPaint)
                    }

                    currentPhotoIdx++
                }
            }

            sheetBitmaps.add(sheetBitmap)
        }

        // Clean up intermediate cropped bitmaps in renderedCache
        renderedCache.values.forEach {
            try { it.recycle() } catch (_: Exception) {}
        }

        sheetBitmaps
    }

    /**
     * Create print-ready PDF file from high-res sheet bitmaps.
     */
    suspend fun createPdf(
        context: Context,
        sheets: List<Bitmap>,
        paper: PaperSheetFormat
    ): File = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()

        val (pageWidthPt, pageHeightPt) = when (paper) {
            PaperSheetFormat.A4 -> Pair(595, 842)
            PaperSheetFormat.PHOTO_4X6_PORTRAIT -> Pair(288, 432)
            PaperSheetFormat.PHOTO_4X6_LANDSCAPE -> Pair(432, 288)
        }

        sheets.forEachIndexed { index, sheetBmp ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, index + 1).create()
            val page = pdfDoc.startPage(pageInfo)

            val destRect = Rect(0, 0, pageWidthPt, pageHeightPt)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            if (!sheetBmp.isRecycled) {
                page.canvas.drawBitmap(sheetBmp, null, destRect, paint)
            }

            pdfDoc.finishPage(page)
        }

        val tempDir = File(context.cacheDir, "passport_export").apply { mkdirs() }
        val pdfFile = File(tempDir, "Passport_Sheet_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        pdfFile
    }

    /**
     * Save high-resolution bitmap as JPEG/PNG file.
     */
    suspend fun saveImageFile(
        context: Context,
        bitmap: Bitmap,
        isPng: Boolean = false
    ): File = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "passport_export").apply { mkdirs() }
        val ext = if (isPng) "png" else "jpg"
        val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val quality = if (isPng) 100 else 98

        val file = File(tempDir, "Passport_Sheet_${System.currentTimeMillis()}.$ext")
        FileOutputStream(file).use { out ->
            bitmap.compress(format, quality, out)
        }
        file
    }
}
