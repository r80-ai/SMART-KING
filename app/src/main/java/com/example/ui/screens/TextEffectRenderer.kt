package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.EmbossMaskFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.pdf.PdfEngine
import com.example.photo.PhotoFonts
import com.example.photo.TextAlignment
import com.example.photo.TextItem
import kotlin.math.abs
import kotlin.math.max

object TextEffectRenderer {

    /**
     * Renders a PhotoEditor TextItem on an Android Canvas with full real-time effects:
     * - Texture filling with BitmapShader
     * - Curved text on path (-100 to +100 arc)
     * - Line spacing with StaticLayout
     * - Outer Stroke with user strokeWidth & strokeColor
     * - Drop shadow & Inner shadow
     * - Emboss filter
     * - 3D Perspective rotation (X, Y, slant)
     * - 3D stacked depth extrusion
     * - 3D floor shadow
     * - Background with padding and corner radius
     * - Mirror reflection
     */
    fun drawPhotoTextItem(
        canvas: Canvas,
        item: TextItem,
        viewWidth: Float,
        viewHeight: Float,
        canvasScale: Float = 1f
    ) {
        if (!item.isVisible || item.text.isBlank()) return

        val px = item.normX * viewWidth
        val py = item.normY * viewHeight
        val fontSizePx = (item.fontSizeSp * canvasScale).coerceAtLeast(8f)

        val tf = item.customTypeface ?: run {
            val fontDef = PhotoFonts.getFont(item.fontId)
            var style = Typeface.NORMAL
            if (item.isBold && item.isItalic) style = Typeface.BOLD_ITALIC
            else if (item.isBold) style = Typeface.BOLD
            else if (item.isItalic) style = Typeface.ITALIC

            val base = if (fontDef.category == "Bengali") {
                Typeface.SANS_SERIF
            } else {
                when (fontDef.fontFamily) {
                    androidx.compose.ui.text.font.FontFamily.Serif -> Typeface.SERIF
                    androidx.compose.ui.text.font.FontFamily.Monospace -> Typeface.MONOSPACE
                    androidx.compose.ui.text.font.FontFamily.Cursive -> Typeface.create("casual", Typeface.NORMAL)
                    else -> Typeface.DEFAULT
                }
            }
            Typeface.create(base, style)
        }

        drawAdvancedTextPipeline(
            canvas = canvas,
            text = item.text,
            posX = px,
            posY = py,
            fontSizePx = fontSizePx,
            colorHex = item.color,
            alpha = item.opacity.coerceIn(0.05f, 1f),
            typeface = tf,
            alignment = when (item.alignment) {
                TextAlignment.CENTER -> "CENTER"
                TextAlignment.RIGHT -> "RIGHT"
                TextAlignment.JUSTIFY -> "JUSTIFY"
                else -> "LEFT"
            },
            isUnderline = item.isUnderline,
            isStrikethrough = item.isStrikethrough,
            letterSpacing = item.letterSpacing,
            lineSpacingMultiplier = if (item.lineSpacingMultiplier > 0f) item.lineSpacingMultiplier else 1f,
            rotationZ = item.rotationDeg,
            rotationX = item.rotationX,
            rotationY = item.rotationY,
            perspectiveSlant = item.perspectiveSlant,
            boxWidth = if (item.boxWidth > 0f) item.boxWidth * canvasScale else 0f,
            boxHeight = if (item.boxHeight > 0f) item.boxHeight * canvasScale else 0f,
            backgroundColor = item.backgroundColor,
            backgroundPadding = item.backgroundPadding * canvasScale,
            backgroundCornerRadius = item.backgroundCornerRadius * canvasScale,
            strokeColor = item.strokeColor,
            strokeWidth = item.strokeWidth * canvasScale,
            shadowColor = item.shadowColor,
            shadowRadius = item.shadowRadius * canvasScale,
            shadowDx = item.shadowDx * canvasScale,
            shadowDy = item.shadowDy * canvasScale,
            innerShadowColor = item.innerShadowColor,
            innerShadowRadius = item.innerShadowRadius * canvasScale,
            isEmboss = item.isEmboss,
            curveArc = item.curveArc,
            is3dText = item.depth3D > 0f,
            depth3d = item.depth3D * canvasScale,
            depth3dColor = item.depth3DColor,
            is3dShadow = item.shadow3D,
            depth3dShadowDist = item.depth3DShadowDistance * canvasScale,
            isReflection = item.hasReflection,
            textureBitmap = item.textureBitmap,
            scaleFactor = canvasScale
        )
    }

    /**
     * Renders a PDF EditItem on an Android Canvas with full real-time effects
     */
    fun drawPdfEditItem(
        canvas: Canvas,
        item: PdfEngine.EditItem,
        strokeScale: Float = 1f,
        densityDpiScale: Float = 1f
    ) {
        if (!item.isVisible || item.text.isBlank()) return

        val baseFontSize = if (item.fontSize < 60f) item.fontSize * densityDpiScale else item.fontSize
        val fontSizePx = (baseFontSize * strokeScale).coerceAtLeast(14f)

        val tf = run {
            val base = when {
                item.customFontPath != null -> try {
                    Typeface.createFromFile(item.customFontPath)
                } catch (_: Exception) { Typeface.DEFAULT }
                item.fontFamily.equals("BENGALI", ignoreCase = true) -> Typeface.SANS_SERIF
                item.fontFamily.equals("SERIF", ignoreCase = true) -> Typeface.SERIF
                item.fontFamily.equals("MONOSPACE", ignoreCase = true) -> Typeface.MONOSPACE
                item.fontFamily.equals("HANDWRITING", ignoreCase = true) || item.fontFamily.equals("CURSIVE", ignoreCase = true) ->
                    Typeface.create("casual", Typeface.NORMAL)
                else -> Typeface.DEFAULT
            }
            val style = when {
                item.isBold && item.isItalic -> Typeface.BOLD_ITALIC
                item.isBold -> Typeface.BOLD
                item.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            Typeface.create(base, style)
        }

        drawAdvancedTextPipeline(
            canvas = canvas,
            text = item.text,
            posX = item.posX * strokeScale,
            posY = item.posY * strokeScale,
            fontSizePx = fontSizePx,
            colorHex = item.colorHex,
            alpha = item.opacity.coerceIn(0.05f, 1f),
            typeface = tf,
            alignment = item.alignment,
            isUnderline = item.isUnderline,
            isStrikethrough = item.isStrikethrough,
            letterSpacing = item.letterSpacing,
            lineSpacingMultiplier = if (item.lineSpacingMultiplier > 0f) item.lineSpacingMultiplier else 1.15f,
            rotationZ = item.getRotationAngle(),
            rotationX = item.rotate3dX,
            rotationY = item.rotate3dY,
            perspectiveSlant = item.perspectiveDistort,
            boxWidth = if (item.boxWidth > 0f) item.boxWidth * strokeScale else 0f,
            boxHeight = if (item.boxHeight > 0f) item.boxHeight * strokeScale else 0f,
            backgroundColor = if (item.backgroundColorHex != 0) item.backgroundColorHex else null,
            backgroundPadding = item.backgroundPadding * strokeScale,
            backgroundCornerRadius = item.backgroundCornerRadius * strokeScale,
            strokeColor = if (item.strokeColorHex != 0) item.strokeColorHex else null,
            strokeWidth = item.textStrokeWidth * strokeScale,
            shadowColor = if (item.shadowColorHex != 0) item.shadowColorHex else null,
            shadowRadius = item.shadowRadius * strokeScale,
            shadowDx = item.shadowDx * strokeScale,
            shadowDy = item.shadowDy * strokeScale,
            innerShadowColor = if (item.innerShadowColorHex != 0) item.innerShadowColorHex else null,
            innerShadowRadius = item.innerShadowRadius * strokeScale,
            isEmboss = item.isEmboss,
            curveArc = item.curveRadius,
            is3dText = item.is3dText && item.depth3d > 0f,
            depth3d = item.depth3d * strokeScale,
            depth3dColor = null,
            is3dShadow = item.is3dShadow,
            depth3dShadowDist = item.depth3dShadowDistance * strokeScale,
            isReflection = item.isReflection,
            textureBitmap = item.textureBitmap,
            scaleFactor = strokeScale
        )
    }

    /**
     * Unified, high-fidelity rendering pipeline executing all text effects in exact visual order
     */
    private fun drawAdvancedTextPipeline(
        canvas: Canvas,
        text: String,
        posX: Float,
        posY: Float,
        fontSizePx: Float,
        colorHex: Int,
        alpha: Float,
        typeface: Typeface,
        alignment: String,
        isUnderline: Boolean,
        isStrikethrough: Boolean,
        letterSpacing: Float,
        lineSpacingMultiplier: Float,
        rotationZ: Float,
        rotationX: Float,
        rotationY: Float,
        perspectiveSlant: Float,
        boxWidth: Float,
        boxHeight: Float,
        backgroundColor: Int?,
        backgroundPadding: Float,
        backgroundCornerRadius: Float,
        strokeColor: Int?,
        strokeWidth: Float,
        shadowColor: Int?,
        shadowRadius: Float,
        shadowDx: Float,
        shadowDy: Float,
        innerShadowColor: Int?,
        innerShadowRadius: Float,
        isEmboss: Boolean,
        curveArc: Float,
        is3dText: Boolean,
        depth3d: Float,
        depth3dColor: Int?,
        is3dShadow: Boolean,
        depth3dShadowDist: Float,
        isReflection: Boolean,
        textureBitmap: Bitmap?,
        scaleFactor: Float
    ) {
        val basePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = fontSizePx
            this.color = colorHex
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            this.isUnderlineText = isUnderline
            this.isStrikeThruText = isStrikethrough
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && letterSpacing != 0f) {
                this.letterSpacing = letterSpacing * 0.05f
            }
        }

        // Texture BitmapShader mapping
        if (textureBitmap != null && !textureBitmap.isRecycled) {
            val shader = BitmapShader(textureBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            val matrix = Matrix()
            val texScale = (fontSizePx / textureBitmap.height.toFloat().coerceAtLeast(1f)).coerceIn(0.1f, 10f)
            matrix.setScale(texScale, texScale)
            matrix.postTranslate(posX, posY)
            shader.setLocalMatrix(matrix)
            basePaint.shader = shader
        }

        // Emboss MaskFilter
        if (isEmboss) {
            try {
                basePaint.maskFilter = EmbossMaskFilter(floatArrayOf(1f, 1f, 1f), 0.45f, 7f, 3.5f * scaleFactor)
            } catch (_: Exception) {}
        }

        // Measure text content
        val lines = text.split("\n")
        var maxLineW = 0f
        for (l in lines) {
            val w = basePaint.measureText(l)
            if (w > maxLineW) maxLineW = w
        }

        val naturalW = maxLineW + backgroundPadding * 2f
        val targetW = if (boxWidth > 0f) max(boxWidth, 20f) else naturalW
        val align = when (alignment.uppercase()) {
            "CENTER" -> Layout.Alignment.ALIGN_CENTER
            "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, basePaint, targetW.toInt().coerceAtLeast(10))
                .setAlignment(align)
                .setLineSpacing(0f, lineSpacingMultiplier)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, basePaint, targetW.toInt().coerceAtLeast(10), align, lineSpacingMultiplier, 0f, false)
        }

        val totalH = staticLayout.height.toFloat() + backgroundPadding * 2f
        val finalBoxW = targetW
        val finalBoxH = if (boxHeight > 0f) max(boxHeight, totalH) else totalH

        val centerX = posX + finalBoxW / 2f
        val centerY = posY + finalBoxH / 2f

        canvas.save()

        // 3D Perspective Transformations via Camera and Matrix
        if (rotationX != 0f || rotationY != 0f || perspectiveSlant != 0f) {
            val camera = Camera()
            val matrix = Matrix()
            camera.save()
            camera.rotateX(rotationX)
            camera.rotateY(rotationY)
            if (perspectiveSlant != 0f) {
                camera.rotateZ(perspectiveSlant)
            }
            camera.getMatrix(matrix)
            camera.restore()

            matrix.preTranslate(-centerX, -centerY)
            matrix.postTranslate(centerX, centerY)
            canvas.concat(matrix)
        }

        // 2D Rotation Z around center
        if (rotationZ != 0f) {
            canvas.rotate(rotationZ, centerX, centerY)
        }

        // 1. 3D Floor Shadow (Elliptical blurred shadow cast on floor beneath base)
        if (is3dShadow) {
            val shadowDist = if (depth3dShadowDist > 0f) depth3dShadowDist else 14f * scaleFactor
            val shadowP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((alpha * 130).toInt().coerceIn(0, 255), 0, 0, 0)
                try {
                    maskFilter = BlurMaskFilter(12f * scaleFactor, BlurMaskFilter.Blur.NORMAL)
                } catch (_: Exception) {}
            }
            val floorOval = RectF(
                posX - finalBoxW * 0.08f,
                posY + finalBoxH + depth3d + shadowDist,
                posX + finalBoxW * 1.08f,
                posY + finalBoxH + depth3d + shadowDist + finalBoxH * 0.28f
            )
            canvas.drawOval(floorOval, shadowP)
        }

        // 2. Background Fill (solid or rounded rectangle)
        if (backgroundColor != null && backgroundColor != 0) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = backgroundColor
                this.alpha = (alpha * Color.alpha(backgroundColor)).toInt().coerceIn(0, 255)
                style = Paint.Style.FILL
            }
            if (backgroundCornerRadius > 0f) {
                canvas.drawRoundRect(
                    posX, posY, posX + finalBoxW, posY + finalBoxH,
                    backgroundCornerRadius, backgroundCornerRadius, bgPaint
                )
            } else {
                canvas.drawRect(posX, posY, posX + finalBoxW, posY + finalBoxH, bgPaint)
            }
        }

        // 3. Mirror Reflection
        if (isReflection) {
            canvas.save()
            canvas.translate(posX, posY + finalBoxH * 1.9f)
            canvas.scale(1f, -0.6f)
            val refPaint = TextPaint(basePaint).apply {
                this.alpha = (alpha * 60).toInt().coerceIn(0, 255)
                clearShadowLayer()
            }
            val refLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, refPaint, targetW.toInt().coerceAtLeast(10))
                    .setAlignment(align)
                    .setLineSpacing(0f, lineSpacingMultiplier)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, refPaint, targetW.toInt().coerceAtLeast(10), align, lineSpacingMultiplier, 0f, false)
            }
            refLayout.draw(canvas)
            canvas.restore()
        }

        // 4. 3D Text Extrusion Layers (Stacked depth slices along Z with realistic bevel shading)
        if (is3dText && depth3d > 0f) {
            val steps = (depth3d * 0.75f).toInt().coerceIn(2, 22)
            val extrusionPaint = TextPaint(basePaint).apply {
                clearShadowLayer()
                shader = null
            }
            for (step in steps downTo 1) {
                val ratio = step.toFloat() / steps.toFloat()
                val shadeFactor = 1f - ratio * 0.55f
                val stepColor = depth3dColor ?: Color.rgb(
                    (Color.red(colorHex) * shadeFactor).toInt().coerceIn(0, 255),
                    (Color.green(colorHex) * shadeFactor).toInt().coerceIn(0, 255),
                    (Color.blue(colorHex) * shadeFactor).toInt().coerceIn(0, 255)
                )
                extrusionPaint.color = stepColor
                extrusionPaint.alpha = (alpha * 240).toInt().coerceIn(0, 255)

                canvas.save()
                val offX = step * 1.2f * scaleFactor
                val offY = step * 1.2f * scaleFactor
                canvas.translate(posX + backgroundPadding + offX, posY + backgroundPadding + offY)

                if (abs(curveArc) > 0.01f) {
                    drawCurvedText(canvas, text, curveArc, maxLineW, fontSizePx, extrusionPaint, align)
                } else {
                    val stepLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        StaticLayout.Builder.obtain(text, 0, text.length, extrusionPaint, targetW.toInt().coerceAtLeast(10))
                            .setAlignment(align)
                            .setLineSpacing(0f, lineSpacingMultiplier)
                            .setIncludePad(false)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        StaticLayout(text, extrusionPaint, targetW.toInt().coerceAtLeast(10), align, lineSpacingMultiplier, 0f, false)
                    }
                    stepLayout.draw(canvas)
                }
                canvas.restore()
            }
        }

        // 5. Outer Drop Shadow
        if (shadowRadius > 0f && shadowColor != null && shadowColor != 0) {
            basePaint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        } else {
            basePaint.clearShadowLayer()
        }

        // 6. Stroke / Outline Rendering
        if (strokeWidth > 0f && strokeColor != null && strokeColor != 0) {
            val strokePaint = TextPaint(basePaint).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                this.color = strokeColor
                this.alpha = (alpha * Color.alpha(strokeColor)).toInt().coerceIn(0, 255)
                clearShadowLayer()
                shader = null
            }

            canvas.save()
            canvas.translate(posX + backgroundPadding, posY + backgroundPadding)
            if (abs(curveArc) > 0.01f) {
                drawCurvedText(canvas, text, curveArc, maxLineW, fontSizePx, strokePaint, align)
            } else {
                val fm = strokePaint.fontMetrics
                val lineHeight = (fm.descent - fm.ascent) * lineSpacingMultiplier
                var baselineY = -fm.ascent
                for (line in lines) {
                    val lineW = strokePaint.measureText(line)
                    val lineX = when (align) {
                        Layout.Alignment.ALIGN_CENTER -> (targetW - lineW) / 2f
                        Layout.Alignment.ALIGN_OPPOSITE -> targetW - lineW
                        else -> 0f
                    }
                    canvas.drawText(line, lineX, baselineY, strokePaint)
                    baselineY += lineHeight
                }
            }
            canvas.restore()
        }

        // 7. Primary Face Text
        canvas.save()
        canvas.translate(posX + backgroundPadding, posY + backgroundPadding)
        if (abs(curveArc) > 0.01f) {
            drawCurvedText(canvas, text, curveArc, maxLineW, fontSizePx, basePaint, align)
        } else {
            val fm = basePaint.fontMetrics
            val lineHeight = (fm.descent - fm.ascent) * lineSpacingMultiplier
            var baselineY = -fm.ascent
            for (line in lines) {
                val lineW = basePaint.measureText(line)
                val lineX = when (align) {
                    Layout.Alignment.ALIGN_CENTER -> (targetW - lineW) / 2f
                    Layout.Alignment.ALIGN_OPPOSITE -> targetW - lineW
                    else -> 0f
                }
                canvas.drawText(line, lineX, baselineY, basePaint)
                baselineY += lineHeight
            }
        }
        canvas.restore()

        // 8. Inner Shadow Effect
        if (innerShadowRadius > 0f && innerShadowColor != null && innerShadowColor != 0) {
            drawInnerShadow(
                canvas = canvas,
                text = text,
                posX = posX + backgroundPadding,
                posY = posY + backgroundPadding,
                width = targetW,
                height = staticLayout.height.toFloat(),
                shadowRadius = innerShadowRadius,
                shadowColor = innerShadowColor,
                alpha = alpha,
                align = align,
                lineSpacingMultiplier = lineSpacingMultiplier,
                basePaint = basePaint
            )
        }

        canvas.restore()
    }

    /**
     * Curved text along a Path (canvas.drawTextOnPath) supporting arc slider (-100 to +100)
     */
    private fun drawCurvedText(
        canvas: Canvas,
        text: String,
        arcVal: Float,
        measuredW: Float,
        fontSizePx: Float,
        paint: Paint,
        align: Layout.Alignment
    ) {
        val path = Path()
        val textLen = max(measuredW, 40f)

        // Arc calculations: arcVal ranges from -100 to +100
        val clampedArc = arcVal.coerceIn(-100f, 100f)
        val normalizedBend = clampedArc / 100f
        val radius = (textLen / (abs(normalizedBend) * Math.PI.toFloat())).coerceIn(textLen * 0.4f, textLen * 5f)

        if (normalizedBend > 0) {
            // Curves downward like an arch
            val oval = RectF(0f, -radius * 1.5f + fontSizePx * 1.2f, textLen, radius * 0.5f + fontSizePx * 1.2f)
            path.addArc(oval, 180f, 180f)
        } else {
            // Curves upward like a smile/bowl
            val oval = RectF(0f, -radius * 0.5f, textLen, radius * 1.5f)
            path.addArc(oval, 0f, 180f)
        }

        val hOffset = when (align) {
            Layout.Alignment.ALIGN_CENTER -> textLen * 0.15f
            Layout.Alignment.ALIGN_OPPOSITE -> textLen * 0.3f
            else -> 0f
        }

        try {
            canvas.drawTextOnPath(text, path, hOffset, 0f, paint)
        } catch (_: Exception) {
            canvas.drawText(text, 0f, fontSizePx, paint)
        }
    }

    /**
     * Inner Shadow: Clipped inverse alpha mask rendered with PorterDuffXfermode(SRC_ATOP)
     */
    private fun drawInnerShadow(
        canvas: Canvas,
        text: String,
        posX: Float,
        posY: Float,
        width: Float,
        height: Float,
        shadowRadius: Float,
        shadowColor: Int,
        alpha: Float,
        align: Layout.Alignment,
        lineSpacingMultiplier: Float,
        basePaint: TextPaint
    ) {
        try {
            val bounds = RectF(posX, posY, posX + width, posY + height)
            val saveCount = canvas.saveLayer(bounds, null)

            val textLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, basePaint, width.toInt().coerceAtLeast(10))
                    .setAlignment(align)
                    .setLineSpacing(0f, lineSpacingMultiplier)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, basePaint, width.toInt().coerceAtLeast(10), align, lineSpacingMultiplier, 0f, false)
            }

            canvas.save()
            canvas.translate(posX, posY)
            textLayout.draw(canvas)
            canvas.restore()

            // Draw inner shadow stroke/blur on top using SRC_ATOP
            val innerPaint = TextPaint(basePaint).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                color = shadowColor
                this.alpha = (alpha * Color.alpha(shadowColor)).toInt().coerceIn(0, 255)
                maskFilter = BlurMaskFilter(shadowRadius.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                style = Paint.Style.STROKE
                strokeWidth = shadowRadius * 1.5f
            }

            val innerLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, innerPaint, width.toInt().coerceAtLeast(10))
                    .setAlignment(align)
                    .setLineSpacing(0f, lineSpacingMultiplier)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, innerPaint, width.toInt().coerceAtLeast(10), align, lineSpacingMultiplier, 0f, false)
            }

            canvas.save()
            canvas.translate(posX + shadowRadius * 0.4f, posY + shadowRadius * 0.4f)
            innerLayout.draw(canvas)
            canvas.restore()

            canvas.restoreToCount(saveCount)
        } catch (_: Exception) {}
    }
}
