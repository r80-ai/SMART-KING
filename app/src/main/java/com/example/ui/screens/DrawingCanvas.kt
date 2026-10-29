package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.photo.DrawingViewModel

/**
 * Advanced Drawing Canvas for Photo Editor ported from PDF Edit -> Pen engine.
 * Provides:
 * - Smooth quadratic Bézier curves (quadTo) for Pen & Highlighter
 * - Geometric Shapes (Rectangle, Circle, Line)
 * - Complete Arrow engine (Single, Double-ended, Dashed, Curved, Block, Chevron, Filled-head)
 * - Vector Icons & Symbols (Checkmarks, Crossmarks, Stars, Badges, etc.)
 * - Scalable & Movable Emoji Stamps
 * - Dual Eraser: Smooth CLEAR Stroke Eraser & Object Eraser
 * - Live real-time Eyedropper Color Picker
 * - Full Undo / Redo history
 */
@Composable
fun DrawingCanvas(
    viewModel: DrawingViewModel,
    bitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        // Core Annotation & Drawing View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                AnnotationCanvasOverlayView(ctx).apply {
                    isInteractionEnabled = true
                    onAnnotationAdded = { item -> viewModel.addAnnotation(item) }
                    onAnnotationsRemoved = { items -> viewModel.removeAnnotations(items) }
                    onAnnotationUpdated = { item -> viewModel.updateAnnotation(item) }
                    onAnnotationSelected = { id -> viewModel.selectedAnnotationId = id }
                    onHistoryPush = { viewModel.pushHistory() }
                }
            },
            update = { view ->
                view.isInteractionEnabled = true
                view.selectedAnnotationId = viewModel.selectedAnnotationId
                view.eraserType = viewModel.eraserType
                view.eraserSize = viewModel.eraserSize
                view.eraserStrokeWidthDp = viewModel.eraserStrokeWidth

                // Sync annotations
                view.setAnnotations(viewModel.annotations.toList(), 0)

                // Sync current active tool & styling
                when (viewModel.activeSubTool) {
                    AnnotationCanvasOverlayView.ToolMode.SELECT -> {
                        view.setSelectMode()
                    }
                    AnnotationCanvasOverlayView.ToolMode.PEN -> {
                        view.setPenMode(
                            viewModel.penColor,
                            viewModel.penStrokeThickness,
                            viewModel.penOpacity
                        )
                    }
                    AnnotationCanvasOverlayView.ToolMode.HIGHLIGHT -> {
                        view.setHighlightMode(
                            viewModel.highlightColor,
                            viewModel.highlightWidth,
                            viewModel.highlightOpacity
                        )
                    }
                    AnnotationCanvasOverlayView.ToolMode.SHAPE -> {
                        view.setShapeMode(
                            viewModel.shapeType,
                            viewModel.shapeColor,
                            viewModel.shapeStrokeWidth,
                            viewModel.penOpacity,
                            viewModel.shapeFillColor,
                            viewModel.arrowStyle
                        )
                    }
                    AnnotationCanvasOverlayView.ToolMode.ICON -> {
                        view.setIconMode(
                            viewModel.iconType,
                            viewModel.iconColor,
                            viewModel.iconStrokeWidth,
                            1.0f,
                            viewModel.iconFillColor
                        )
                    }
                    AnnotationCanvasOverlayView.ToolMode.ERASER -> {
                        view.setEraserMode(
                            viewModel.eraserSize,
                            viewModel.eraserStrokeWidth
                        )
                    }
                }
            }
        )

        // Real-time Eyedropper Color Picker Overlay
        if (viewModel.isColorPickerActive && bitmap != null && !bitmap.isRecycled) {
            val activeColor = when (viewModel.colorPickerTarget) {
                "PEN" -> viewModel.penColor
                "HIGHLIGHT" -> viewModel.highlightColor
                "SHAPE" -> viewModel.shapeColor
                "ICON" -> viewModel.iconColor
                else -> viewModel.penColor
            }

            EyedropperOverlay(
                bitmap = bitmap,
                currentColor = activeColor,
                onColorSampled = { sampled ->
                    when (viewModel.colorPickerTarget) {
                        "PEN" -> viewModel.penColor = sampled
                        "HIGHLIGHT" -> viewModel.highlightColor = sampled
                        "SHAPE" -> viewModel.shapeColor = sampled
                        "ICON" -> viewModel.iconColor = sampled
                    }
                },
                onColorConfirmed = { finalColor ->
                    when (viewModel.colorPickerTarget) {
                        "PEN" -> viewModel.penColor = finalColor
                        "HIGHLIGHT" -> viewModel.highlightColor = finalColor
                        "SHAPE" -> viewModel.shapeColor = finalColor
                        "ICON" -> viewModel.iconColor = finalColor
                    }
                    viewModel.isColorPickerActive = false
                    Toast.makeText(
                        context,
                        "Color picked: #${Integer.toHexString(finalColor).uppercase()}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
            )
        }

        // Emoji Picker Sheet
        if (viewModel.showEmojiPicker) {
            PdfEditorEmojiPickerSheet(
                onEmojiSelected = { emoji ->
                    viewModel.showEmojiPicker = false
                    val bmpW = bitmap?.width?.toFloat() ?: 800f
                    val bmpH = bitmap?.height?.toFloat() ?: 800f
                    viewModel.addEmoji(emoji, bmpW / 2f, bmpH / 2f, bmpW, bmpH)
                    Toast.makeText(context, "Emoji stamp added", Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    viewModel.showEmojiPicker = false
                }
            )
        }
    }
}
