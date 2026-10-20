package com.example.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * High-performance, non-destructive photo editing algorithms:
 * - Crop & Resize
 * - Rotate (90 deg left/right) & Flip (H/V)
 * - Color adjustments (Brightness, Contrast, Saturation)
 * - Enhance & Sharpen
 * - Selective Blur on user-drawn area
 * - Object Removal / Erase with intelligent surrounding pixel in-painting
 * - 4-corner Perspective Correction with homography
 * - Background Removal with transparency support
 */
object PhotoEditorEngine {

    private const val TAG = "PhotoEditorEngine"

    /**
     * Non-destructively adjusts Brightness [-1.0f..1.0f], Contrast [0.2f..2.5f], and Saturation [0.0f..2.5f].
     * (Defaults: brightness = 0.0f, contrast = 1.0f, saturation = 1.0f)
     */
    fun applyColorAdjustments(
        bitmap: Bitmap,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f
    ): Bitmap {
        if (bitmap.isRecycled) return bitmap
        if (brightness == 0f && contrast == 1f && saturation == 1f) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Saturation ColorMatrix
        val satMatrix = ColorMatrix().apply {
            setSaturation(saturation.coerceIn(0f, 3f))
        }

        // 2. Contrast & Brightness ColorMatrix
        // Formula: C' = C * contrast + brightness * 255
        val scale = contrast.coerceIn(0.1f, 3f)
        val translate = (brightness.coerceIn(-1f, 1f) * 255f) + (1f - scale) * 128f / 2f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val combinedMatrix = ColorMatrix().apply {
            setConcat(contrastMatrix, satMatrix)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Enhances clarity and sharpens edges smoothly without introducing excessive noise or halo artifacts.
     * enhance: [0.0f..1.0f], sharpen: [0.0f..1.0f]
     */
    fun applyEnhanceAndSharpen(
        bitmap: Bitmap,
        enhance: Float = 0f,
        sharpen: Float = 0f
    ): Bitmap {
        if (bitmap.isRecycled) return bitmap
        if (enhance <= 0f && sharpen <= 0f) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)

        val sharpenWeight = (sharpen.coerceIn(0f, 1f) * 0.75f)
        val enhanceWeight = (enhance.coerceIn(0f, 1f) * 0.40f)

        for (y in 0 until h) {
            val rowOffset = y * w
            val yPrev = max(0, y - 1) * w
            val yNext = min(h - 1, y + 1) * w

            for (x in 0 until w) {
                val idx = rowOffset + x
                val center = pixels[idx]

                val a = (center shr 24) and 0xFF
                var r = (center shr 16) and 0xFF
                var g = (center shr 8) and 0xFF
                var b = center and 0xFF

                // 1. Smart Unsharp Mask / Sharpening kernel
                if (sharpenWeight > 0.01f) {
                    val xPrev = max(0, x - 1)
                    val xNext = min(w - 1, x + 1)

                    val top = pixels[yPrev + x]
                    val bottom = pixels[yNext + x]
                    val left = pixels[rowOffset + xPrev]
                    val right = pixels[rowOffset + xNext]

                    val avgR = (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) +
                            ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) shr 2
                    val avgG = (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) +
                            ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) shr 2
                    val avgB = ((top and 0xFF) + (bottom and 0xFF) +
                            (left and 0xFF) + (right and 0xFF)) shr 2

                    val diffR = r - avgR
                    val diffG = g - avgG
                    val diffB = b - avgB

                    // Subtle clamping to prevent high-frequency halo artifacts
                    r = (r + diffR * sharpenWeight).roundToInt().coerceIn(0, 255)
                    g = (g + diffG * sharpenWeight).roundToInt().coerceIn(0, 255)
                    b = (b + diffB * sharpenWeight).roundToInt().coerceIn(0, 255)
                }

                // 2. Adaptive local contrast / Enhance (S-curve boost)
                if (enhanceWeight > 0.01f) {
                    // Mid-tone contrast curve that lifts shadows and enhances dynamic range
                    fun boost(channel: Int): Int {
                        val norm = channel / 255f
                        val sCurve = if (norm < 0.5f) {
                            2f * norm * norm
                        } else {
                            1f - 2f * (1f - norm) * (1f - norm)
                        }
                        val blended = (1f - enhanceWeight) * norm + enhanceWeight * sCurve
                        return (blended * 255f).roundToInt().coerceIn(0, 255)
                    }
                    r = boost(r)
                    g = boost(g)
                    b = boost(b)
                }

                outPixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        output.setPixels(outPixels, 0, w, 0, 0, w, h)
        return output
    }

    /**
     * Rotates [degrees] (e.g. 90, -90, 180, 270) or flips horizontally / vertically.
     */
    fun rotateOrFlip(
        bitmap: Bitmap,
        degrees: Int = 0,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false
    ): Bitmap {
        if (bitmap.isRecycled) return bitmap
        if (degrees % 360 == 0 && !flipHorizontal && !flipVertical) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val matrix = Matrix()
        if (degrees % 360 != 0) {
            matrix.postRotate(degrees.toFloat())
        }
        val sx = if (flipHorizontal) -1f else 1f
        val sy = if (flipVertical) -1f else 1f
        if (flipHorizontal || flipVertical) {
            matrix.postScale(sx, sy)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Crops bitmap to specified normalized rectangle [0f..1f].
     */
    fun cropBitmap(
        bitmap: Bitmap,
        normRect: RectF
    ): Bitmap {
        if (bitmap.isRecycled) return bitmap
        val left = (normRect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 2)
        val top = (normRect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 2)
        val right = (normRect.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (normRect.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)

        val cropW = right - left
        val cropH = bottom - top
        if (cropW <= 0 || cropH <= 0) return bitmap.copy(Bitmap.Config.ARGB_8888, true)

        return Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
    }

    /**
     * Resizes bitmap with high quality bilinear filtering.
     */
    fun resizeBitmap(
        bitmap: Bitmap,
        newWidth: Int,
        newHeight: Int
    ): Bitmap {
        if (bitmap.isRecycled) return bitmap
        val targetW = newWidth.coerceIn(10, 8192)
        val targetH = newHeight.coerceIn(10, 8192)
        if (targetW == bitmap.width && targetH == bitmap.height) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    /**
     * Applies blur ONLY to user-drawn finger strokes or region.
     * [strokes]: list of paths or touch points in normalized [0f..1f] coordinate space.
     * [brushRadiusPx]: radius of finger brush in bitmap pixels.
     * [blurRadius]: blur strength [1..25].
     */
    fun applySelectiveBlur(
        bitmap: Bitmap,
        strokes: List<List<PointF>>,
        brushRadiusPx: Float,
        blurRadius: Int
    ): Bitmap {
        if (bitmap.isRecycled) return bitmap
        if (strokes.isEmpty() || strokes.all { it.isEmpty() }) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val w = bitmap.width
        val h = bitmap.height

        // 1. Create selection mask bitmap (WHITE = blurred, BLACK = untouched)
        val maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = (brushRadiusPx * 2f).coerceAtLeast(4f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            if (stroke.size == 1) {
                val pt = stroke.first()
                maskCanvas.drawCircle(pt.x * w, pt.y * h, brushRadiusPx, maskPaint.apply { style = Paint.Style.FILL })
                maskPaint.style = Paint.Style.STROKE
            } else {
                val path = Path()
                path.moveTo(stroke[0].x * w, stroke[0].y * h)
                for (i in 1 until stroke.size) {
                    path.lineTo(stroke[i].x * w, stroke[i].y * h)
                }
                maskCanvas.drawPath(path, maskPaint)
            }
        }

        // 2. Generate blurred version of image (fast box/stack blur)
        val blurredBitmap = fastBlur(bitmap, blurRadius.coerceIn(1, 25))

        // 3. Composite blurred pixels where mask is active
        val maskPixels = IntArray(w * h)
        maskBitmap.getPixels(maskPixels, 0, w, 0, 0, w, h)
        val origPixels = IntArray(w * h)
        bitmap.getPixels(origPixels, 0, w, 0, 0, w, h)
        val blurPixels = IntArray(w * h)
        blurredBitmap.getPixels(blurPixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)

        for (i in 0 until w * h) {
            val maskAlpha = (maskPixels[i] ushr 24) and 0xFF
            if (maskAlpha == 0) {
                outPixels[i] = origPixels[i]
            } else if (maskAlpha == 255) {
                outPixels[i] = blurPixels[i]
            } else {
                val factor = maskAlpha / 255f
                val inv = 1f - factor
                val o = origPixels[i]
                val b = blurPixels[i]
                val a = (((o ushr 24) and 0xFF) * inv + ((b ushr 24) and 0xFF) * factor).toInt()
                val r = (((o shr 16) and 0xFF) * inv + ((b shr 16) and 0xFF) * factor).toInt()
                val g = (((o shr 8) and 0xFF) * inv + ((b shr 8) and 0xFF) * factor).toInt()
                val bl = ((o and 0xFF) * inv + (b and 0xFF) * factor).toInt()
                outPixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or bl
            }
        }

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.setPixels(outPixels, 0, w, 0, 0, w, h)

        maskBitmap.recycle()
        blurredBitmap.recycle()

        return output
    }

    /**
     * Erases unwanted area marked by user, filling it intelligently with surrounding texture/pixels
     * using multi-scale perimeter sampling & Poisson-style inverse-distance color diffusion.
     * Does NOT affect areas outside the selected region.
     */
    fun applyObjectRemoval(
        bitmap: Bitmap,
        strokes: List<List<PointF>>,
        brushRadiusPx: Float
    ): Bitmap {
        if (bitmap.isRecycled) return bitmap
        if (strokes.isEmpty() || strokes.all { it.isEmpty() }) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val w = bitmap.width
        val h = bitmap.height

        // 1. Create binary mask (255 = erase region, 0 = keep original)
        val maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        val maskCanvas = Canvas(maskBitmap)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = (brushRadiusPx * 2f).coerceAtLeast(4f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            if (stroke.size == 1) {
                val pt = stroke.first()
                maskCanvas.drawCircle(pt.x * w, pt.y * h, brushRadiusPx, maskPaint.apply { style = Paint.Style.FILL })
                maskPaint.style = Paint.Style.STROKE
            } else {
                val path = Path()
                path.moveTo(stroke[0].x * w, stroke[0].y * h)
                for (i in 1 until stroke.size) {
                    path.lineTo(stroke[i].x * w, stroke[i].y * h)
                }
                maskCanvas.drawPath(path, maskPaint)
            }
        }

        val maskBytes = ByteArray(w * h)
        val byteBuffer = java.nio.ByteBuffer.wrap(maskBytes)
        maskBitmap.copyPixelsToBuffer(byteBuffer)
        maskBitmap.recycle()

        val origPixels = IntArray(w * h)
        bitmap.getPixels(origPixels, 0, w, 0, 0, w, h)
        val resultPixels = origPixels.clone()

        // 2. Find bounding box of masked region to optimize computation
        var minX = w
        var maxX = 0
        var minY = h
        var maxY = 0
        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                if (maskBytes[rowOffset + x].toInt() and 0xFF > 30) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (minX > maxX || minY > maxY) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        // Expand bounds slightly for surrounding sample ring
        val margin = (brushRadiusPx * 1.5f).toInt().coerceIn(6, 40)
        val bboxL = max(0, minX - margin)
        val bboxR = min(w - 1, maxX + margin)
        val bboxT = max(0, minY - margin)
        val bboxB = min(h - 1, maxY + margin)

        // 3. Collect perimeter boundary points (unmasked pixels adjacent to mask)
        val perimeterList = ArrayList<Triple<Int, Int, Int>>() // x, y, color
        for (y in bboxT..bboxB) {
            val rowOffset = y * w
            for (x in bboxL..bboxR) {
                val isMasked = (maskBytes[rowOffset + x].toInt() and 0xFF) > 30
                if (!isMasked) {
                    // Check if adjacent to a masked pixel
                    var touchesMask = false
                    for (dy in -2..2 step 2) {
                        val ny = (y + dy).coerceIn(0, h - 1)
                        for (dx in -2..2 step 2) {
                            val nx = (x + dx).coerceIn(0, w - 1)
                            if ((maskBytes[ny * w + nx].toInt() and 0xFF) > 30) {
                                touchesMask = true
                                break
                            }
                        }
                        if (touchesMask) break
                    }
                    if (touchesMask) {
                        perimeterList.add(Triple(x, y, origPixels[rowOffset + x]))
                    }
                }
            }
        }

        if (perimeterList.isEmpty()) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        // Downsample perimeter sample points if too many for fast O(N) evaluation
        val step = max(1, perimeterList.size / 120)
        val samplePoints = if (step > 1) {
            perimeterList.filterIndexed { index, _ -> index % step == 0 }
        } else {
            perimeterList
        }

        // 4. In-paint masked pixels using Inverse Distance Weighted (IDW) interpolation
        for (y in minY..maxY) {
            val rowOffset = y * w
            for (x in minX..maxX) {
                val maskAlpha = maskBytes[rowOffset + x].toInt() and 0xFF
                if (maskAlpha > 30) {
                    var totalWeight = 0f
                    var rAcc = 0f
                    var gAcc = 0f
                    var bAcc = 0f

                    for (p in samplePoints) {
                        val dx = (x - p.first).toFloat()
                        val dy = (y - p.second).toFloat()
                        val distSq = dx * dx + dy * dy + 1f
                        val wgt = 1f / (distSq * sqrt(distSq)) // p = 3 for local smoothness
                        val col = p.third
                        rAcc += ((col shr 16) and 0xFF) * wgt
                        gAcc += ((col shr 8) and 0xFF) * wgt
                        bAcc += (col and 0xFF) * wgt
                        totalWeight += wgt
                    }

                    val inpaintR = if (totalWeight > 0f) (rAcc / totalWeight).toInt().coerceIn(0, 255) else 0
                    val inpaintG = if (totalWeight > 0f) (gAcc / totalWeight).toInt().coerceIn(0, 255) else 0
                    val inpaintB = if (totalWeight > 0f) (bAcc / totalWeight).toInt().coerceIn(0, 255) else 0

                    val orig = origPixels[rowOffset + x]
                    val origA = (orig shr 24) and 0xFF
                    val blendFactor = (maskAlpha / 255f)

                    val origR = (orig shr 16) and 0xFF
                    val origG = (orig shr 8) and 0xFF
                    val origB = orig and 0xFF

                    val finalR = ((1f - blendFactor) * origR + blendFactor * inpaintR).toInt().coerceIn(0, 255)
                    val finalG = ((1f - blendFactor) * origG + blendFactor * inpaintG).toInt().coerceIn(0, 255)
                    val finalB = ((1f - blendFactor) * origB + blendFactor * inpaintB).toInt().coerceIn(0, 255)

                    resultPixels[rowOffset + x] = (origA shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                }
            }
        }

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.setPixels(resultPixels, 0, w, 0, 0, w, h)
        return output
    }

    /**
     * Transforms four-corner quadrilateral [srcCorners] (in normalized coords [0f..1f])
     * into a straight, perfectly aligned rectangular document.
     * Order of [srcCorners]: Top-Left, Top-Right, Bottom-Right, Bottom-Left.
     */
    fun applyPerspectiveCorrection(
        bitmap: Bitmap,
        srcCorners: List<PointF>
    ): Bitmap {
        if (bitmap.isRecycled) return bitmap
        if (srcCorners.size != 4) return bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()

        val tl = PointF(srcCorners[0].x * bmpW, srcCorners[0].y * bmpH)
        val tr = PointF(srcCorners[1].x * bmpW, srcCorners[1].y * bmpH)
        val br = PointF(srcCorners[2].x * bmpW, srcCorners[2].y * bmpH)
        val bl = PointF(srcCorners[3].x * bmpW, srcCorners[3].y * bmpH)

        // Compute width & height from Euclidean distances of quad sides
        val topWidth = hypot(tr.x - tl.x, tr.y - tl.y)
        val bottomWidth = hypot(br.x - bl.x, br.y - bl.y)
        val leftHeight = hypot(bl.x - tl.x, bl.y - tl.y)
        val rightHeight = hypot(br.x - tr.x, br.y - tr.y)

        val outW = max(topWidth, bottomWidth).roundToInt().coerceIn(100, 4096)
        val outH = max(leftHeight, rightHeight).roundToInt().coerceIn(100, 4096)

        val srcPoints = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)
        if (!success) {
            Log.e(TAG, "Matrix setPolyToPoly failed to invert perspective quad")
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.save()
        canvas.concat(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()

        return output
    }

    /**
     * Subject-based background removal.
     * Uses boundary-seeded geodesic flood fill, spatial foreground priors, edge barriers,
     * and hole filling so complex backgrounds (brick walls, patterns, scenery, textures)
     * are removed without hollowing out or destroying the subject's face, hair, or clothes.
     *
     * [sensitivity]: [0.1f..0.9f] controls the aggressiveness of background removal.
     */
    fun removeBackground(
        bitmap: Bitmap,
        sensitivity: Float = 0.40f
    ): Bitmap {
        if (bitmap.isRecycled) return bitmap
        val origW = bitmap.width
        val origH = bitmap.height

        // Downscale for fast, smooth mask generation
        val targetDim = 320
        val scaleDown = minOf(1f, targetDim.toFloat() / maxOf(origW, origH))
        val procW = (origW * scaleDown).roundToInt().coerceAtLeast(16)
        val procH = (origH * scaleDown).roundToInt().coerceAtLeast(16)

        val smallBitmap = if (scaleDown < 0.99f) {
            Bitmap.createScaledBitmap(bitmap, procW, procH, true)
        } else {
            bitmap
        }

        val pixels = IntArray(procW * procH)
        smallBitmap.getPixels(pixels, 0, procW, 0, 0, procW, procH)
        if (smallBitmap !== bitmap) {
            smallBitmap.recycle()
        }

        val totalPixels = procW * procH

        // 1. Compute Grayscale Luminance and Sobel Gradient magnitude
        val lum = FloatArray(totalPixels)
        val rChan = FloatArray(totalPixels)
        val gChan = FloatArray(totalPixels)
        val bChan = FloatArray(totalPixels)
        for (i in 0 until totalPixels) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF).toFloat()
            val g = ((p shr 8) and 0xFF).toFloat()
            val b = (p and 0xFF).toFloat()
            rChan[i] = r
            gChan[i] = g
            bChan[i] = b
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Gradients (Sobel-like)
        val grad = FloatArray(totalPixels)
        for (y in 1 until procH - 1) {
            val row = y * procW
            for (x in 1 until procW - 1) {
                val idx = row + x
                val gx = (lum[idx + 1] - lum[idx - 1]) +
                        0.5f * ((lum[idx + procW + 1] - lum[idx + procW - 1]) + (lum[idx - procW + 1] - lum[idx - procW - 1]))
                val gy = (lum[idx + procW] - lum[idx - procW]) +
                        0.5f * ((lum[idx + procW + 1] - lum[idx - procW + 1]) + (lum[idx + procW - 1] - lum[idx - procW - 1]))
                grad[idx] = hypot(gx, gy)
            }
        }

        // 2. Spatial Foreground Prior & Boundary-seeded Flood Fill (BFS)
        val cx = procW * 0.5f
        val cy = procH * 0.48f

        val queue = java.util.ArrayDeque<Int>(totalPixels / 4)
        val isBg = BooleanArray(totalPixels)
        val bgDist = FloatArray(totalPixels) { Float.MAX_VALUE }

        fun seedBorder(x: Int, y: Int) {
            val idx = y * procW + x
            if (!isBg[idx]) {
                isBg[idx] = true
                bgDist[idx] = 0f
                queue.add(idx)
            }
        }

        for (x in 0 until procW) {
            seedBorder(x, 0)
            seedBorder(x, procH - 1)
        }
        for (y in 0 until procH) {
            seedBorder(0, y)
            seedBorder(procW - 1, y)
        }

        val baseGradientThreshold = 22f + (1f - sensitivity.coerceIn(0.05f, 0.95f)) * 55f

        val dx = intArrayOf(1, -1, 0, 0)
        val dy = intArrayOf(0, 0, 1, -1)

        while (queue.isNotEmpty()) {
            val currIdx = queue.poll() ?: break
            val curX = currIdx % procW
            val curY = currIdx / procW
            val curDist = bgDist[currIdx]

            for (d in 0..3) {
                val nx = curX + dx[d]
                val ny = curY + dy[d]
                if (nx in 0 until procW && ny in 0 until procH) {
                    val nIdx = ny * procW + nx
                    if (!isBg[nIdx]) {
                        val ndx = (nx - cx) / cx
                        val ndy = (ny - cy) / cy
                        val distCenterNorm = hypot(ndx, ndy)

                        val centerProtection = if (distCenterNorm < 0.75f) {
                            (1f - (distCenterNorm / 0.75f)) * 40f
                        } else 0f

                        val effectiveGradThresh = baseGradientThreshold + centerProtection
                        val edgeStrength = grad[nIdx]

                        val dr = abs(rChan[nIdx] - rChan[currIdx])
                        val dg = abs(gChan[nIdx] - gChan[currIdx])
                        val db = abs(bChan[nIdx] - bChan[currIdx])
                        val colorDiff = dr + dg + db

                        if (edgeStrength < effectiveGradThresh && colorDiff < (120f * (0.6f + sensitivity * 0.8f))) {
                            isBg[nIdx] = true
                            bgDist[nIdx] = curDist + 1f
                            queue.add(nIdx)
                        }
                    }
                }
            }
        }

        // 3. Hole filling & Edge Feathering
        val featheredMask = ByteArray(totalPixels)
        for (y in 0 until procH) {
            val row = y * procW
            for (x in 0 until procW) {
                val idx = row + x
                if (isBg[idx]) {
                    var fgCount = 0
                    if (x + 1 < procW && !isBg[idx + 1]) fgCount++
                    if (x - 1 >= 0 && !isBg[idx - 1]) fgCount++
                    if (y + 1 < procH && !isBg[idx + procW]) fgCount++
                    if (y - 1 >= 0 && !isBg[idx - procW]) fgCount++
                    featheredMask[idx] = if (fgCount > 0) (fgCount * 45).toByte() else 0.toByte()
                } else {
                    var bgCount = 0
                    if (x + 1 < procW && isBg[idx + 1]) bgCount++
                    if (x - 1 >= 0 && isBg[idx - 1]) bgCount++
                    if (y + 1 < procH && isBg[idx + procW]) bgCount++
                    if (y - 1 >= 0 && isBg[idx - procW]) bgCount++
                    featheredMask[idx] = if (bgCount > 0) (255 - bgCount * 35).toByte() else 255.toByte()
                }
            }
        }

        // 4. Apply alpha mask to full-resolution bitmap
        val output = Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(origW * origH)
        bitmap.getPixels(outPixels, 0, origW, 0, 0, origW, origH)

        val xRatio = procW.toFloat() / origW
        val yRatio = procH.toFloat() / origH

        for (y in 0 until origH) {
            val my = (y * yRatio).toInt().coerceIn(0, procH - 1)
            val origRow = y * origW
            val maskRow = my * procW
            for (x in 0 until origW) {
                val mx = (x * xRatio).toInt().coerceIn(0, procW - 1)
                val alpha = featheredMask[maskRow + mx].toInt() and 0xFF
                val origColor = outPixels[origRow + x]
                outPixels[origRow + x] = (alpha shl 24) or (origColor and 0x00FFFFFF)
            }
        }

        output.setPixels(outPixels, 0, origW, 0, 0, origW, origH)
        return output
    }

    /**
     * Applies manual refinement
     */
    fun applyManualRefinement(
        subjectBitmap: Bitmap,
        originalBitmap: Bitmap,
        strokes: List<MaskStroke>
    ): Bitmap {
        if (subjectBitmap.isRecycled || originalBitmap.isRecycled) return subjectBitmap
        val w = subjectBitmap.width
        val h = subjectBitmap.height
        val output = subjectBitmap.copy(Bitmap.Config.ARGB_8888, true)

        val subPixels = IntArray(w * h)
        output.getPixels(subPixels, 0, w, 0, 0, w, h)

        val origPixels = IntArray(w * h)
        originalBitmap.getPixels(origPixels, 0, w, 0, 0, w, h)

        for (stroke in strokes) {
            val radius = stroke.radiusPx.coerceAtLeast(2f)
            val radSq = radius * radius
            val isRestore = stroke.isRestore
            val isSoft = stroke.isSoft

            for (pt in stroke.points) {
                val cx = (pt.x * w).roundToInt().coerceIn(0, w - 1)
                val cy = (pt.y * h).roundToInt().coerceIn(0, h - 1)
                val rInt = radius.toInt()

                val minX = max(0, cx - rInt)
                val maxX = min(w - 1, cx + rInt)
                val minY = max(0, cy - rInt)
                val maxY = min(h - 1, cy + rInt)

                for (y in minY..maxY) {
                    val row = y * w
                    val dy = (y - cy).toFloat()
                    for (x in minX..maxX) {
                        val dx = (x - cx).toFloat()
                        val distSq = dx * dx + dy * dy
                        if (distSq <= radSq) {
                            val idx = row + x
                            val origP = origPixels[idx]
                            val r = (origP shr 16) and 0xFF
                            val g = (origP shr 8) and 0xFF
                            val b = origP and 0xFF

                            if (isRestore) {
                                // Restore subject from original
                                if (isSoft) {
                                    val factor = (1f - sqrt(distSq) / radius).coerceIn(0f, 1f)
                                    val currentAlpha = (subPixels[idx] shr 24) and 0xFF
                                    val newAlpha = max(currentAlpha, (factor * 255f).roundToInt())
                                    subPixels[idx] = (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
                                } else {
                                    subPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                                }
                            } else {
                                // Erase background
                                if (isSoft) {
                                    val factor = (sqrt(distSq) / radius).coerceIn(0f, 1f)
                                    val currentAlpha = (subPixels[idx] shr 24) and 0xFF
                                    val newAlpha = min(currentAlpha, ((1f - factor) * 0f + factor * currentAlpha).roundToInt())
                                    subPixels[idx] = (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
                                } else {
                                    subPixels[idx] = 0 // Transparent
                                }
                            }
                        }
                    }
                }
            }
        }

        output.setPixels(subPixels, 0, w, 0, 0, w, h)
        return output
    }

    /**
     * Composites a subject bitmap (with transparent background) over a chosen background style:
     * - Transparent
     * - Solid Color
     * - Two-color Gradient (Vertical, Horizontal, Diagonal)
     * - Image from Gallery (scaled with Center-Crop)
     */
    fun compositeSubjectOverBackground(
        subjectBitmap: Bitmap,
        bgType: BgOptionType,
        solidColor: Int = android.graphics.Color.WHITE,
        gradientColor1: Int = android.graphics.Color.BLUE,
        gradientColor2: Int = android.graphics.Color.MAGENTA,
        gradientDirection: GradientDirection = GradientDirection.VERTICAL,
        bgImage: Bitmap? = null,
        subjectTranslationX: Float = 0f,
        subjectTranslationY: Float = 0f,
        subjectScale: Float = 1f,
        subjectRotation: Float = 0f
    ): Bitmap {
        if (subjectBitmap.isRecycled) return subjectBitmap
        val w = subjectBitmap.width
        val h = subjectBitmap.height

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (bgType) {
            BgOptionType.TRANSPARENT -> {
                // Background remains transparent
            }
            BgOptionType.SOLID_COLOR -> {
                canvas.drawColor(solidColor)
            }
            BgOptionType.GRADIENT -> {
                val (x0, y0, x1, y1) = when (gradientDirection) {
                    GradientDirection.VERTICAL -> listOf(0f, 0f, 0f, h.toFloat())
                    GradientDirection.HORIZONTAL -> listOf(0f, 0f, w.toFloat(), 0f)
                    GradientDirection.DIAGONAL -> listOf(0f, 0f, w.toFloat(), h.toFloat())
                }
                val shader = android.graphics.LinearGradient(
                    x0, y0, x1, y1,
                    gradientColor1, gradientColor2,
                    android.graphics.Shader.TileMode.CLAMP
                )
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
            }
            BgOptionType.IMAGE -> {
                if (bgImage != null && !bgImage.isRecycled) {
                    val srcW = bgImage.width.toFloat()
                    val srcH = bgImage.height.toFloat()
                    val scale = maxOf(w.toFloat() / srcW, h.toFloat() / srcH)
                    val scaledW = srcW * scale
                    val scaledH = srcH * scale
                    val dx = (w - scaledW) / 2f
                    val dy = (h - scaledH) / 2f
                    val matrix = android.graphics.Matrix().apply {
                        postScale(scale, scale)
                        postTranslate(dx, dy)
                    }
                    canvas.drawBitmap(bgImage, matrix, paint)
                } else {
                    canvas.drawColor(android.graphics.Color.WHITE)
                }
            }
        }

        // Draw the extracted subject on top of the background with optional transform
        if (subjectTranslationX == 0f && subjectTranslationY == 0f && subjectScale == 1f && subjectRotation == 0f) {
            canvas.drawBitmap(subjectBitmap, 0f, 0f, paint)
        } else {
            val subjectMatrix = android.graphics.Matrix().apply {
                val cx = w / 2f
                val cy = h / 2f
                postTranslate(-cx, -cy)
                postScale(subjectScale, subjectScale)
                postRotate(subjectRotation)
                postTranslate(cx + subjectTranslationX, cy + subjectTranslationY)
            }
            canvas.drawBitmap(subjectBitmap, subjectMatrix, paint)
        }

        return output
    }

    /**
     * Fast two-pass box blur for smooth UI blur operations.
     */
    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (curY in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (curI in -radius..radius) {
                p = pix[yi + min(wm, max(curI, 0))]
                sir = stack[curI + radius]
                sir[0] = (p shr 16) and 0xFF
                sir[1] = (p shr 8) and 0xFF
                sir[2] = p and 0xFF
                rbs = r1 - abs(curI)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (curI > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (curX in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curY == 0) {
                    vmin[curX] = min(curX + radius + 1, wm)
                }
                p = pix[yw + vmin[curX]]

                sir[0] = (p shr 16) and 0xFF
                sir[1] = (p shr 8) and 0xFF
                sir[2] = p and 0xFF

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (curX in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (curI in -radius..radius) {
                yi = max(0, yp) + curX
                sir = stack[curI + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - abs(curI)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (curI > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (curI < hm) {
                    yp += w
                }
            }
            yi = curX
            stackpointer = radius
            for (curY in 0 until h) {
                pix[yi] = (0xFF shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curX == 0) {
                    vmin[curY] = min(curY + r1, hm) * w
                }
                p = curX + vmin[curY]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * Burns user added text overlays directly onto the bitmap canvas.
     */
    fun burnText(bitmap: Bitmap, textItems: List<TextItem>): Bitmap {
        if (bitmap.isRecycled || textItems.isEmpty()) return bitmap
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val w = output.width.toFloat()
        val h = output.height.toFloat()

        for (item in textItems) {
            val scaleFactor = (max(w, h) / 1000f).coerceIn(0.5f, 5f)
            val px = item.normX * w
            val py = item.normY * h

            val baseAlpha = (item.color ushr 24) and 0xFF
            val finalAlpha = (baseAlpha * item.opacity.coerceIn(0.05f, 1f)).toInt().coerceIn(0, 255)
            val colorWithAlpha = (item.color and 0x00FFFFFF) or (finalAlpha shl 24)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorWithAlpha
                textSize = (item.fontSizeSp * scaleFactor).coerceAtLeast(14f)
                typeface = item.customTypeface ?: PhotoFonts.getTypeface(item.fontId, item.isBold, item.isItalic)
                isUnderlineText = item.isUnderline
                isStrikeThruText = item.isStrikethrough
                letterSpacing = item.letterSpacing
                textAlign = when (item.alignment) {
                    TextAlignment.LEFT -> Paint.Align.LEFT
                    TextAlignment.CENTER -> Paint.Align.CENTER
                    TextAlignment.RIGHT -> Paint.Align.RIGHT
                    TextAlignment.JUSTIFY -> Paint.Align.LEFT
                }
            }

            if (item.textureBitmap != null && !item.textureBitmap!!.isRecycled) {
                paint.shader = android.graphics.BitmapShader(
                    item.textureBitmap!!,
                    android.graphics.Shader.TileMode.REPEAT,
                    android.graphics.Shader.TileMode.REPEAT
                )
            }

            canvas.save()
            if (item.rotationDeg != 0f) {
                canvas.rotate(item.rotationDeg, px, py)
            }
            if (item.perspectiveSlant != 0f) {
                canvas.skew(item.perspectiveSlant * 0.01f, 0f)
            }

            val lines = item.text.split("\n")
            val fontMetrics = paint.fontMetrics
            val lineHeight = (fontMetrics.bottom - fontMetrics.top) * item.lineSpacingMultiplier.coerceIn(0.5f, 3.0f)

            // Measure bounds for background rect
            var maxLineWidth = 0f
            for (line in lines) {
                val lw = paint.measureText(line)
                if (lw > maxLineWidth) maxLineWidth = lw
            }
            val totalTextHeight = lines.size * lineHeight

            val padX = (item.backgroundPadding + item.paddingX) * scaleFactor
            val padY = (item.backgroundPadding + item.paddingY) * scaleFactor
            val bgLeft = when (item.alignment) {
                TextAlignment.LEFT, TextAlignment.JUSTIFY -> px - padX
                TextAlignment.CENTER -> px - (maxLineWidth / 2f) - padX
                TextAlignment.RIGHT -> px - maxLineWidth - padX
            }
            val bgTop = py - (totalTextHeight / 2f) - padY
            val bgRight = bgLeft + maxLineWidth + (padX * 2f)
            val bgBottom = bgTop + totalTextHeight + (padY * 2f)

            // Draw Background if enabled
            if (item.backgroundColor != null) {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.backgroundColor!!
                    style = Paint.Style.FILL
                }
                val cornerR = item.backgroundCornerRadius * scaleFactor
                canvas.drawRoundRect(RectF(bgLeft, bgTop, bgRight, bgBottom), cornerR, cornerR, bgPaint)
            }

            // 3D Extrusion
            if (item.depth3D > 0f) {
                val depthColor = item.depth3DColor ?: Color.argb((finalAlpha * 0.7f).toInt(), 50, 50, 50)
                val depthPaint = Paint(paint).apply {
                    color = depthColor
                    style = Paint.Style.FILL
                    shader = null
                }
                val steps = item.depth3D.roundToInt().coerceIn(1, 20)
                for (s in steps downTo 1) {
                    val off = s * 1.5f * scaleFactor
                    var dY = py - ((lines.size - 1) * lineHeight / 2f) + off
                    for (line in lines) {
                        canvas.drawText(line, px + off, dY, depthPaint)
                        dY += lineHeight
                    }
                }
            }

            // Outer Shadow
            if (item.shadowColor != null && item.shadowRadius > 0f) {
                paint.setShadowLayer(
                    item.shadowRadius * scaleFactor,
                    item.shadowDx * scaleFactor,
                    item.shadowDy * scaleFactor,
                    item.shadowColor!!
                )
            } else {
                paint.clearShadowLayer()
            }

            // Stroke
            if (item.strokeColor != null && item.strokeWidth > 0f) {
                val strokePaint = Paint(paint).apply {
                    color = item.strokeColor!!
                    style = Paint.Style.STROKE
                    strokeWidth = item.strokeWidth * scaleFactor
                    shader = null
                    clearShadowLayer()
                }
                var curY = py - ((lines.size - 1) * lineHeight / 2f)
                for (line in lines) {
                    canvas.drawText(line, px, curY, strokePaint)
                    curY += lineHeight
                }
            }

            // Draw Main Text
            var currentY = py - ((lines.size - 1) * lineHeight / 2f)
            for (line in lines) {
                canvas.drawText(line, px, currentY, paint)
                currentY += lineHeight
            }

            // Reflection Effect
            if (item.hasReflection) {
                val reflectionPaint = Paint(paint).apply {
                    alpha = (finalAlpha * 0.35f).toInt()
                    clearShadowLayer()
                }
                canvas.save()
                canvas.scale(1f, -0.6f, px, bgBottom)
                var refY = bgBottom + lineHeight
                for (line in lines) {
                    canvas.drawText(line, px, refY, reflectionPaint)
                    refY += lineHeight
                }
                canvas.restore()
            }

            canvas.restore()
        }
        return output
    }

    /**
     * Burns user drawings/strokes directly onto the bitmap canvas.
     */
    fun burnDrawing(bitmap: Bitmap, strokes: List<DrawingStroke>): Bitmap {
        if (bitmap.isRecycled || strokes.isEmpty()) return bitmap
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val w = output.width.toFloat()
        val h = output.height.toFloat()

        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            val scaleFactor = (max(w, h) / 1000f).coerceIn(0.5f, 5f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                if (stroke.isEraser) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    color = stroke.color
                    alpha = (stroke.opacity.coerceIn(0.05f, 1f) * 255f).toInt()
                }
                strokeWidth = (stroke.brushSizePx * scaleFactor).coerceAtLeast(2f)
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            if (stroke.points.size == 1) {
                val pt = stroke.points.first()
                paint.style = Paint.Style.FILL
                canvas.drawCircle(pt.x * w, pt.y * h, stroke.brushSizePx * scaleFactor / 2f, paint)
            } else {
                val path = Path()
                path.moveTo(stroke.points[0].x * w, stroke.points[0].y * h)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].x * w, stroke.points[i].y * h)
                }
                canvas.drawPath(path, paint)
            }
        }
        return output
    }
}
