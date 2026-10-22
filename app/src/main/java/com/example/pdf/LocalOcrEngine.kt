package com.example.pdf

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class OcrTextBlock(
    val text: String,
    val boundingBox: Rect,
    val cornerPoints: List<android.graphics.Point>? = null,
    val lines: List<OcrTextLine>
)

data class OcrTextLine(
    val text: String,
    val boundingBox: Rect,
    val elements: List<OcrTextElement>
)

data class OcrTextElement(
    val text: String,
    val boundingBox: Rect
)

object LocalOcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromBitmap(bitmap: Bitmap): List<OcrTextBlock> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.textBlocks.map { block ->
                OcrTextBlock(
                    text = block.text,
                    boundingBox = block.boundingBox ?: Rect(),
                    cornerPoints = block.cornerPoints?.toList(),
                    lines = block.lines.map { line ->
                        OcrTextLine(
                            text = line.text,
                            boundingBox = line.boundingBox ?: Rect(),
                            elements = line.elements.map { element ->
                                OcrTextElement(
                                    text = element.text,
                                    boundingBox = element.boundingBox ?: Rect()
                                )
                            }
                        )
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
