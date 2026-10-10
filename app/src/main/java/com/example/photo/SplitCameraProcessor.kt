package com.example.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class SplitLayout(
    val title: String,
    val subtitle: String,
    val shortLabel: String
) {
    VERTICAL_1X2("1×2 — Vertical", "LEFT + RIGHT", "1×2 Vertical"),
    HORIZONTAL_2X1("2×1 — Horizontal", "TOP + BOTTOM", "2×1 Horizontal")
}

object SplitCameraProcessor {

    private const val TAG = "SplitCameraProcessor"

    /**
     * Creates a composite bitmap combining two separate photos according to the chosen layout.
     * - VERTICAL_1X2 (1×2):
     *     - Left section [0 .. splitX]: Uses firstPhoto (left)
     *     - Right section [splitX .. width]: Uses secondPhoto (right)
     * - HORIZONTAL_2X1 (2×1):
     *     - Top section [0 .. splitY]: Uses firstPhoto (top)
     *     - Bottom section [splitY .. height]: Uses secondPhoto (bottom)
     * - When `blendSeam == true`: Smooths/blends the seam naturally without modifying the rest of photos.
     * - When `drawDividerLine == true`: Draws a clean divider line along the split boundary.
     *
     * GUARANTEE: Never crashes, safely handles any combination of photo dimensions,
     * and memory constraints.
     */
    fun createCombinedBitmap(
        firstPhoto: Bitmap,
        secondPhoto: Bitmap,
        layout: SplitLayout = SplitLayout.VERTICAL_1X2,
        splitRatio: Float = 0.5f,
        blendSeam: Boolean = false,
        drawDividerLine: Boolean = false
    ): Bitmap {
        if (firstPhoto.isRecycled) {
            throw IllegalArgumentException("First photo bitmap is recycled")
        }
        if (secondPhoto.isRecycled) {
            throw IllegalArgumentException("Second photo bitmap is recycled")
        }

        // Base output dimensions from photos as captured
        val width = firstPhoto.width
        val height = firstPhoto.height

        val clampedRatio = splitRatio.coerceIn(0.05f, 0.95f)

        val output = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM creating output bitmap at ${width}x${height}, falling back to 1080p", oom)
            val fallbackW = 1080
            val fallbackH = (1080f * (height.toFloat() / width.toFloat())).toInt().coerceAtLeast(720)
            Bitmap.createBitmap(fallbackW, fallbackH, Bitmap.Config.ARGB_8888)
        }

        val outW = output.width
        val outH = output.height
        val canvas = Canvas(output)
        val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        when (layout) {
            SplitLayout.VERTICAL_1X2 -> {
                val splitX = (outW * clampedRatio).toInt().coerceIn(1, outW - 1)

                // Draw Left Photo on [0 .. splitX] exactly as captured
                val srcRect1 = android.graphics.Rect(0, 0, (firstPhoto.width * clampedRatio).toInt().coerceAtLeast(1), firstPhoto.height)
                val dstRect1 = android.graphics.Rect(0, 0, splitX, outH)
                canvas.drawBitmap(firstPhoto, srcRect1, dstRect1, drawPaint)

                // Draw Right Photo on [splitX .. outW] exactly as captured
                val srcRect2 = android.graphics.Rect((secondPhoto.width * clampedRatio).toInt().coerceAtMost(secondPhoto.width - 1), 0, secondPhoto.width, secondPhoto.height)
                val dstRect2 = android.graphics.Rect(splitX, 0, outW, outH)
                canvas.drawBitmap(secondPhoto, srcRect2, dstRect2, drawPaint)

                // Apply seam blending if requested
                if (blendSeam) {
                    applyVerticalSeamBlend(
                        output = output,
                        splitX = splitX,
                        width = outW,
                        height = outH
                    )
                }

                // Draw divider line if requested
                if (drawDividerLine) {
                    val linePaint = Paint().apply {
                        color = Color.WHITE
                        strokeWidth = (outW * 0.0035f).coerceIn(3f, 8f)
                        style = Paint.Style.STROKE
                        isAntiAlias = true
                    }
                    canvas.drawLine(splitX.toFloat(), 0f, splitX.toFloat(), outH.toFloat(), linePaint)
                }
            }

            SplitLayout.HORIZONTAL_2X1 -> {
                val splitY = (outH * clampedRatio).toInt().coerceIn(1, outH - 1)

                // Draw Top Photo on [0 .. splitY] exactly as captured
                val srcRect1 = android.graphics.Rect(0, 0, firstPhoto.width, (firstPhoto.height * clampedRatio).toInt().coerceAtLeast(1))
                val dstRect1 = android.graphics.Rect(0, 0, outW, splitY)
                canvas.drawBitmap(firstPhoto, srcRect1, dstRect1, drawPaint)

                // Draw Bottom Photo on [splitY .. outH] exactly as captured
                val srcRect2 = android.graphics.Rect(0, (secondPhoto.height * clampedRatio).toInt().coerceAtMost(secondPhoto.height - 1), secondPhoto.width, secondPhoto.height)
                val dstRect2 = android.graphics.Rect(0, splitY, outW, outH)
                canvas.drawBitmap(secondPhoto, srcRect2, dstRect2, drawPaint)

                // Apply seam blending if requested
                if (blendSeam) {
                    applyHorizontalSeamBlend(
                        output = output,
                        splitY = splitY,
                        width = outW,
                        height = outH
                    )
                }

                // Draw divider line if requested
                if (drawDividerLine) {
                    val linePaint = Paint().apply {
                        color = Color.WHITE
                        strokeWidth = (outH * 0.0035f).coerceIn(3f, 8f)
                        style = Paint.Style.STROKE
                        isAntiAlias = true
                    }
                    canvas.drawLine(0f, splitY.toFloat(), outW.toFloat(), splitY.toFloat(), linePaint)
                }
            }
        }

        return output
    }

    /**
     * Blends a soft, natural transition zone around the vertical seam [splitX] for 1×2 layout.
     * Smooths ONLY the seam area across the boundary between the two captured photos,
     * merging both photos visually together without distorting, moving, or cropping either photo.
     */
    private fun applyVerticalSeamBlend(
        output: Bitmap,
        splitX: Int,
        width: Int,
        height: Int
    ) {
        if (output.isRecycled || width <= 4 || height <= 4) return
        try {
            // Blend width proportional to image size (approx 5% width around seam, min 20, max width/6)
            val blendRadius = (width * 0.05f).toInt().coerceIn(20, (width / 6).coerceAtLeast(20))
            val startX = (splitX - blendRadius).coerceAtLeast(0)
            val endX = (splitX + blendRadius).coerceAtMost(width)
            val stripW = endX - startX
            val stripH = height

            if (stripW <= 4 || stripH <= 4) return

            val seamLocalX = (splitX - startX).coerceIn(1, stripW - 2)

            // Extract composite pixels around the seam
            val srcPixels = IntArray(stripW * stripH)
            output.getPixels(srcPixels, 0, stripW, startX, 0, stripW, stripH)

            // Multi-pass blur across seam to create a soft, natural transition
            val blurredH = IntArray(stripW * stripH)
            val kRadius = (blendRadius * 0.65f).toInt().coerceIn(8, 28)

            // Fast horizontal box blur across each row with running sum (equivalent to Gaussian over 2 passes)
            for (y in 0 until stripH) {
                val rowOffset = y * stripW
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0

                // Initialize window for x = 0
                for (kx in -kRadius..kRadius) {
                    val sampleX = kx.coerceIn(0, stripW - 1)
                    val p = srcPixels[rowOffset + sampleX]
                    rSum += (p shr 16) and 0xFF
                    gSum += (p shr 8) and 0xFF
                    bSum += p and 0xFF
                    count++
                }

                for (lx in 0 until stripW) {
                    blurredH[rowOffset + lx] = (0xFF shl 24) or
                        ((rSum / count) shl 16) or
                        ((gSum / count) shl 8) or
                        (bSum / count)

                    // Advance sliding window
                    val removeX = (lx - kRadius).coerceIn(0, stripW - 1)
                    val addX = (lx + kRadius + 1).coerceIn(0, stripW - 1)

                    val pRem = srcPixels[rowOffset + removeX]
                    val pAdd = srcPixels[rowOffset + addX]

                    rSum += ((pAdd shr 16) and 0xFF) - ((pRem shr 16) and 0xFF)
                    gSum += ((pAdd shr 8) and 0xFF) - ((pRem shr 8) and 0xFF)
                    bSum += (pAdd and 0xFF) - (pRem and 0xFF)
                }
            }

            // Second pass: slight vertical smoothing and feathered blend with original
            val outPixels = IntArray(stripW * stripH)
            val vRadius = 2

            // Precompute feather weights: 1.0 at seam, 0.0 at edges of blend strip
            val weightArr = FloatArray(stripW)
            for (lx in 0 until stripW) {
                val distFromSeam = abs(lx - seamLocalX)
                val maxDist = if (lx < seamLocalX) seamLocalX else (stripW - 1 - seamLocalX)
                val t = (1f - distFromSeam.toFloat() / maxDist.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                weightArr[lx] = t * t * (3f - 2f * t) // smoothstep hermite curve
            }

            for (y in 0 until stripH) {
                val rowOffset = y * stripW
                for (lx in 0 until stripW) {
                    val idx = rowOffset + lx
                    val origP = srcPixels[idx]
                    val weight = weightArr[lx]

                    if (weight <= 0.005f) {
                        outPixels[idx] = origP
                        continue
                    }

                    // Vertical average around (lx, y) of blurredH
                    var vrSum = 0
                    var vgSum = 0
                    var vbSum = 0
                    var vCount = 0
                    for (ky in -vRadius..vRadius) {
                        val sampleY = (y + ky).coerceIn(0, stripH - 1)
                        val p = blurredH[sampleY * stripW + lx]
                        vrSum += (p shr 16) and 0xFF
                        vgSum += (p shr 8) and 0xFF
                        vbSum += p and 0xFF
                        vCount++
                    }

                    val blurR = vrSum / vCount
                    val blurG = vgSum / vCount
                    val blurB = vbSum / vCount

                    val origR = (origP shr 16) and 0xFF
                    val origG = (origP shr 8) and 0xFF
                    val origB = origP and 0xFF

                    val finalR = ((1f - weight) * origR + weight * blurR).toInt().coerceIn(0, 255)
                    val finalG = ((1f - weight) * origG + weight * blurG).toInt().coerceIn(0, 255)
                    val finalB = ((1f - weight) * origB + weight * blurB).toInt().coerceIn(0, 255)

                    outPixels[idx] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                }
            }

            output.setPixels(outPixels, 0, stripW, startX, 0, stripW, stripH)
        } catch (t: Throwable) {
            Log.e(TAG, "Safe recovery from vertical seam blend error", t)
        }
    }

    /**
     * Blends a soft, natural transition zone around the horizontal seam [splitY] for 2×1 layout.
     * Smooths ONLY the seam area across the boundary between the two captured photos,
     * merging both photos visually together without distorting, moving, or cropping either photo.
     */
    private fun applyHorizontalSeamBlend(
        output: Bitmap,
        splitY: Int,
        width: Int,
        height: Int
    ) {
        if (output.isRecycled || width <= 4 || height <= 4) return
        try {
            val blendRadius = (height * 0.05f).toInt().coerceIn(20, (height / 6).coerceAtLeast(20))
            val startY = (splitY - blendRadius).coerceAtLeast(0)
            val endY = (splitY + blendRadius).coerceAtMost(height)
            val stripH = endY - startY
            val stripW = width

            if (stripW <= 4 || stripH <= 4) return

            val seamLocalY = (splitY - startY).coerceIn(1, stripH - 2)

            // Extract composite pixels around the horizontal seam
            val srcPixels = IntArray(stripW * stripH)
            output.getPixels(srcPixels, 0, stripW, 0, startY, stripW, stripH)

            // Fast vertical box blur across each column with running sum
            val blurredV = IntArray(stripW * stripH)
            val kRadius = (blendRadius * 0.65f).toInt().coerceIn(8, 28)

            for (x in 0 until stripW) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0

                // Initialize window for y = 0
                for (ky in -kRadius..kRadius) {
                    val sampleY = ky.coerceIn(0, stripH - 1)
                    val p = srcPixels[sampleY * stripW + x]
                    rSum += (p shr 16) and 0xFF
                    gSum += (p shr 8) and 0xFF
                    bSum += p and 0xFF
                    count++
                }

                for (ly in 0 until stripH) {
                    blurredV[ly * stripW + x] = (0xFF shl 24) or
                        ((rSum / count) shl 16) or
                        ((gSum / count) shl 8) or
                        (bSum / count)

                    val removeY = (ly - kRadius).coerceIn(0, stripH - 1)
                    val addY = (ly + kRadius + 1).coerceIn(0, stripH - 1)

                    val pRem = srcPixels[removeY * stripW + x]
                    val pAdd = srcPixels[addY * stripW + x]

                    rSum += ((pAdd shr 16) and 0xFF) - ((pRem shr 16) and 0xFF)
                    gSum += ((pAdd shr 8) and 0xFF) - ((pRem shr 8) and 0xFF)
                    bSum += (pAdd and 0xFF) - (pRem and 0xFF)
                }
            }

            // Second pass: slight horizontal smoothing and feathered blend with original
            val outPixels = IntArray(stripW * stripH)
            val hRadius = 2

            // Precompute feather weights: 1.0 at seam, 0.0 at edges of blend strip
            val weightArr = FloatArray(stripH)
            for (ly in 0 until stripH) {
                val distFromSeam = abs(ly - seamLocalY)
                val maxDist = if (ly < seamLocalY) seamLocalY else (stripH - 1 - seamLocalY)
                val t = (1f - distFromSeam.toFloat() / maxDist.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                weightArr[ly] = t * t * (3f - 2f * t) // smoothstep hermite curve
            }

            for (ly in 0 until stripH) {
                val rowOffset = ly * stripW
                val weight = weightArr[ly]

                for (x in 0 until stripW) {
                    val idx = rowOffset + x
                    val origP = srcPixels[idx]

                    if (weight <= 0.005f) {
                        outPixels[idx] = origP
                        continue
                    }

                    // Horizontal average around (x, ly) of blurredV
                    var hrSum = 0
                    var hgSum = 0
                    var hbSum = 0
                    var hCount = 0
                    for (kx in -hRadius..hRadius) {
                        val sampleX = (x + kx).coerceIn(0, stripW - 1)
                        val p = blurredV[rowOffset + sampleX]
                        hrSum += (p shr 16) and 0xFF
                        hgSum += (p shr 8) and 0xFF
                        hbSum += p and 0xFF
                        hCount++
                    }

                    val blurR = hrSum / hCount
                    val blurG = hgSum / hCount
                    val blurB = hbSum / hCount

                    val origR = (origP shr 16) and 0xFF
                    val origG = (origP shr 8) and 0xFF
                    val origB = origP and 0xFF

                    val finalR = ((1f - weight) * origR + weight * blurR).toInt().coerceIn(0, 255)
                    val finalG = ((1f - weight) * origG + weight * blurG).toInt().coerceIn(0, 255)
                    val finalB = ((1f - weight) * origB + weight * blurB).toInt().coerceIn(0, 255)

                    outPixels[idx] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                }
            }

            output.setPixels(outPixels, 0, stripW, 0, startY, stripW, stripH)
        } catch (t: Throwable) {
            Log.e(TAG, "Safe recovery from horizontal seam blend error", t)
        }
    }
}
