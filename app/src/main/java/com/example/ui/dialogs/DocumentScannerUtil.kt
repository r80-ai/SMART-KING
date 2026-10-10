package com.example.ui.dialogs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF

object DocumentScannerUtil {

    // Simple auto-detect based on brightness threshold and extreme points
    fun detectCorners(bitmap: Bitmap): List<PointF> {
        val width = bitmap.width
        val height = bitmap.height
        
        // Default corners if detection fails (10% padding)
        val defaultCorners = listOf(
            PointF(width * 0.1f, height * 0.1f), // TL
            PointF(width * 0.9f, height * 0.1f), // TR
            PointF(width * 0.9f, height * 0.9f), // BR
            PointF(width * 0.1f, height * 0.9f)  // BL
        )

        try {
            // Scale down for processing speed
            val scale = 256f / Math.max(width, height)
            val sw = Math.round(width * scale)
            val sh = Math.round(height * scale)
            if (sw <= 0 || sh <= 0) return defaultCorners

            val smallBmp = Bitmap.createScaledBitmap(bitmap, sw, sh, true)
            val pixels = IntArray(sw * sh)
            smallBmp.getPixels(pixels, 0, sw, 0, 0, sw, sh)

            // 1. Convert to grayscale and find adaptive threshold
            var sum = 0L
            val grays = IntArray(sw * sh)
            for (i in pixels.indices) {
                val c = pixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val gray = (r * 77 + g * 150 + b * 29) shr 8
                grays[i] = gray
                sum += gray
            }
            val avg = (sum / pixels.size).toInt()

            // 2. Multi-threshold approach to find the most likely document boundary
            // We'll try both "light object on dark background" and "dark object on light background"
            fun findExtremes(threshold: Int, isLighter: Boolean): List<PointF>? {
                var minSum = Int.MAX_VALUE; var tl = PointF(0f, 0f)
                var maxSum = Int.MIN_VALUE; var br = PointF(0f, 0f)
                var maxDiff = Int.MIN_VALUE; var tr = PointF(0f, 0f)
                var minDiff = Int.MAX_VALUE; var bl = PointF(0f, 0f)
                var count = 0

                for (y in 0 until sh) {
                    for (x in 0 until sw) {
                        val g = grays[y * sw + x]
                        val match = if (isLighter) g > threshold else g < threshold
                        if (match) {
                            count++
                            val s = x + y; val d = x - y
                            if (s < minSum) { minSum = s; tl = PointF(x.toFloat(), y.toFloat()) }
                            if (s > maxSum) { maxSum = s; br = PointF(x.toFloat(), y.toFloat()) }
                            if (d > maxDiff) { maxDiff = d; tr = PointF(x.toFloat(), y.toFloat()) }
                            if (d < minDiff) { minDiff = d; bl = PointF(x.toFloat(), y.toFloat()) }
                        }
                    }
                }
                
                // If the "detected" object is too small or too large, it might be noise
                val ratio = count.toFloat() / (sw * sh)
                if (ratio < 0.05f || ratio > 0.95f) return null
                
                return listOf(tl, tr, br, bl)
            }

            val resultLighter = findExtremes(avg + 30, true)
            val resultDarker = findExtremes(avg - 30, false)
            
            val bestSmallResult = resultLighter ?: resultDarker ?: defaultCorners
            
            smallBmp.recycle()

            // Scale back to original coordinates
            val invScale = 1f / scale
            val finalCorners = if (bestSmallResult === defaultCorners) {
                defaultCorners
            } else {
                bestSmallResult.map { PointF(it.x * invScale, it.y * invScale) }
            }
            return finalCorners

        } catch (e: Exception) {
            e.printStackTrace()
            return defaultCorners
        }
    }

    // Perspective transformation using Android Matrix
    fun applyPerspective(
        srcBitmap: Bitmap,
        tl: PointF, tr: PointF, br: PointF, bl: PointF
    ): Bitmap {
        // Calculate the dimension of the new image
        val widthTop = Math.hypot((tr.x - tl.x).toDouble(), (tr.y - tl.y).toDouble())
        val widthBottom = Math.hypot((br.x - bl.x).toDouble(), (br.y - bl.y).toDouble())
        val dstWidth = Math.max(widthTop, widthBottom).toInt().coerceAtLeast(1)

        val heightLeft = Math.hypot((bl.x - tl.x).toDouble(), (bl.y - tl.y).toDouble())
        val heightRight = Math.hypot((br.x - tr.x).toDouble(), (br.y - tr.y).toDouble())
        val dstHeight = Math.max(heightLeft, heightRight).toInt().coerceAtLeast(1)

        val srcPoints = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            dstWidth.toFloat(), 0f,
            dstWidth.toFloat(), dstHeight.toFloat(),
            0f, dstHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        // For large bitmaps, we can just use Bitmap.createBitmap with Matrix!
        // Wait, Bitmap.createBitmap(src, 0, 0, src.w, src.h, matrix, true) applies affine, not polyToPoly properly.
        // We MUST draw onto a new Canvas for polyToPoly to take effect correctly.
        val outBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        
        // Note: Canvas.concat() with a perspective matrix ONLY works when hardware accelerated
        // or on specific API levels. In software (Canvas(bitmap)), it might fallback to affine.
        // To be 100% robust on all devices, we can use BitmapShader or manually sample.
        // Actually, canvas.concat(matrix) and canvas.drawBitmap() handles perspective correctly in Android 10+ (API 29+), but for a robust solution we can write a simple manual sampler or use RenderScript. Since we want no crashes, let's try drawBitmapMesh.
        // Let's use a dense mesh to do the perspective transform!
        
        val meshW = 40
        val meshH = 40
        val verts = FloatArray((meshW + 1) * (meshH + 1) * 2)
        
        var idx = 0
        for (y in 0..meshH) {
            val fy = y * srcBitmap.height.toFloat() / meshH
            for (x in 0..meshW) {
                val fx = x * srcBitmap.width.toFloat() / meshW
                val pt = floatArrayOf(fx, fy)
                matrix.mapPoints(pt)
                verts[idx++] = pt[0]
                verts[idx++] = pt[1]
            }
        }
        
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmapMesh(srcBitmap, meshW, meshH, verts, 0, null, 0, paint)

        return outBitmap
    }
    
    fun applyFilter(bitmap: Bitmap, mode: String): Bitmap {
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        
        when (mode) {
            "Original" -> paint.colorFilter = null
            "Grayscale" -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            "B&W" -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                // Increase contrast
                val scale = 2f
                val translate = -255f * (scale - 1f) / 2f
                cm.postConcat(ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )))
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            "Color" -> {
                val cm = ColorMatrix()
                // Boost saturation and contrast slightly
                cm.setSaturation(1.5f)
                val scale = 1.2f
                val translate = -255f * (scale - 1f) / 2f
                cm.postConcat(ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )))
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return out
    }
}
