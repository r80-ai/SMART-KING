package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAiHelper {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private const val MODEL_NAME = "gemini-2.5-flash"

    // In-memory or user-custom API key override
    var customApiKey: String = ""

    fun getEffectiveApiKey(): String {
        if (customApiKey.isNotBlank()) return customApiKey.trim()
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY" && buildKey != "your_api_key_here") {
            return buildKey.trim()
        }
        return ""
    }

    fun isApiKeyConfigured(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    suspend fun summarizeDocument(documentText: String, apiKeyOverride: String? = null): String = withContext(Dispatchers.IO) {
        val key = apiKeyOverride?.takeIf { it.isNotBlank() } ?: getEffectiveApiKey()
        if (key.isBlank()) {
            return@withContext "API key required for AI features. Please configure your API key in Settings."
        }

        val prompt = """
            You are SMART KING's AI Document Assistant. Analyze the following document text and generate an accurate, structured executive summary.
            Include:
            1. Key Takeaways (3-4 bullet points)
            2. Executive Overview
            3. Actionable Items or Important Clauses
            
            Document Text:
            $documentText
        """.trimIndent()

        callGeminiApi(prompt, key)
    }

    suspend fun askQuestionAboutDocument(documentText: String, question: String, apiKeyOverride: String? = null): String = withContext(Dispatchers.IO) {
        val key = apiKeyOverride?.takeIf { it.isNotBlank() } ?: getEffectiveApiKey()
        if (key.isBlank()) {
            return@withContext "API key required for AI features. Please configure your API key in Settings."
        }

        val prompt = """
            You are SMART KING's AI Assistant. Answer the user's question directly and thoroughly based on the provided document text.
            If the answer cannot be found in the document, explicitly say so.
            
            User Question:
            $question
            
            Document Text:
            $documentText
        """.trimIndent()

        callGeminiApi(prompt, key)
    }

    suspend fun translateDocument(documentText: String, targetLanguage: String, apiKeyOverride: String? = null): String = withContext(Dispatchers.IO) {
        val key = apiKeyOverride?.takeIf { it.isNotBlank() } ?: getEffectiveApiKey()
        if (key.isBlank()) {
            return@withContext "API key required for AI features. Please configure your API key in Settings."
        }

        val prompt = """
            You are a professional document translator for SMART KING.
            Translate the following document text accurately into $targetLanguage.
            Maintain the formal tone, headers, and structure of the original document.
            
            Document Text:
            $documentText
        """.trimIndent()

        callGeminiApi(prompt, key)
    }

    suspend fun generateStudyNotes(documentText: String, apiKeyOverride: String? = null): String = withContext(Dispatchers.IO) {
        val key = apiKeyOverride?.takeIf { it.isNotBlank() } ?: getEffectiveApiKey()
        if (key.isBlank()) {
            return@withContext "API key required for AI features. Please configure your API key in Settings."
        }

        val prompt = """
            You are an educational study guide generator for SMART KING.
            Convert the following document into comprehensive study notes including:
            1. Core Concepts & Definitions
            2. Key Formulas / Principles / Timeline
            3. 5 Practice Review Questions with Answers (Flashcard style)
            4. Quick Exam Revision Summary
            
            Document Text:
            $documentText
        """.trimIndent()

        callGeminiApi(prompt, key)
    }

    suspend fun performOcrOnImage(bitmap: android.graphics.Bitmap, language: String = "English", apiKeyOverride: String? = null): String = withContext(Dispatchers.IO) {
        val key = apiKeyOverride?.takeIf { it.isNotBlank() } ?: getEffectiveApiKey()
        if (key.isBlank()) {
            throw Exception("API key required for AI features. Please configure your API key in Settings.")
        }

        val prompt = """
            You are an Optical Character Recognition (OCR) engine.
            Transcribe and extract the full text from this image.
            Language: $language (support English, Bengali, Hindi accurately).
            Return ONLY the extracted text. Maintain paragraphs and structure. Do NOT include any conversational text like "Here is the text:" or "This image contains:".
        """.trimIndent()

        callGeminiApiWithImage(prompt, bitmap, key)
    }

    suspend fun enhancePortraitWithImage(
        sourceBitmap: android.graphics.Bitmap,
        prompt: String,
        apiKeyOverride: String? = null
    ): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        val key = apiKeyOverride?.takeIf { it.isNotBlank() } ?: getEffectiveApiKey()
        if (key.isBlank()) {
            throw Exception("API key required for AI features. Please configure your API key in Settings.")
        }

        val maxDimension = 1536
        val origW = sourceBitmap.width
        val origH = sourceBitmap.height
        val scale = if (kotlin.math.max(origW, origH) > maxDimension) {
            maxDimension.toFloat() / kotlin.math.max(origW, origH).toFloat()
        } else {
            1f
        }
        val uploadBmp = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(
                sourceBitmap,
                (origW * scale).toInt(),
                (origH * scale).toInt(),
                true
            )
        } else {
            sourceBitmap
        }

        val stream = java.io.ByteArrayOutputStream()
        uploadBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
        val base64Image = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
        if (uploadBmp != sourceBitmap) {
            uploadBmp.recycle()
        }

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                val modalities = JSONArray().apply {
                    put("IMAGE")
                    put("TEXT")
                }
                put("responseModalities", modalities)
            })
        }

        // Try preferred models in order: gemini-3.1-flash-image-preview, then gemini-2.5-flash-image, then gemini-3.1-flash-image
        val candidateModels = listOf("gemini-3.1-flash-image-preview", "gemini-2.5-flash-image", "gemini-3.1-flash-image")
        var lastException: Exception? = null

        for (modelName in candidateModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$key"
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        // Model not found on this endpoint, try next candidate
                        continue
                    }
                    throw Exception(when (response.code) {
                        400 -> "Gemini API Error (400): Bad Request. Please check your API key or prompt."
                        403 -> "Gemini API Error (403): API key invalid or does not have permissions."
                        429 -> "Gemini API Error (429): Quota exceeded. Please try again later."
                        else -> "Gemini API Error (${response.code}): ${response.message}"
                    })
                }

                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    throw Exception("No response candidates returned by Gemini.")
                }

                var resultBmp: android.graphics.Bitmap? = null
                for (c in 0 until candidates.length()) {
                    val candidate = candidates.getJSONObject(c)
                    val content = candidate.optJSONObject("content") ?: continue
                    val parts = content.optJSONArray("parts") ?: continue
                    for (p in 0 until parts.length()) {
                        val part = parts.getJSONObject(p)
                        val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                        if (inlineData != null) {
                            val imgData = inlineData.optString("data", "")
                            if (imgData.isNotBlank()) {
                                val decodedBytes = android.util.Base64.decode(imgData, android.util.Base64.DEFAULT)
                                resultBmp = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                if (resultBmp != null) break
                            }
                        }
                    }
                    if (resultBmp != null) break
                }

                if (resultBmp != null) {
                    return@withContext resultBmp
                } else {
                    throw Exception("AI image editing did not return an image. Please try again.")
                }
            } catch (e: Exception) {
                lastException = e
                // If it wasn't a 404, we rethrow or keep lastException
            }
        }

        throw lastException ?: Exception("AI image editing did not return an image. Please try again.")
    }

    private fun callGeminiApiWithImage(prompt: String, bitmap: android.graphics.Bitmap, apiKey: String): String {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
            val base64Image = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                throw Exception(when (response.code) {
                    400 -> "Gemini API Error (400): Bad Request. Please check your API key or prompt."
                    403 -> "Gemini API Error (403): API key invalid or does not have permissions."
                    429 -> "Gemini API Error (429): Quota exceeded. Please try again later."
                    else -> "Gemini API Error (${response.code}): ${response.message}"
                })
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: throw Exception("No response candidates returned by Gemini.")
            if (candidates.length() == 0) throw Exception("No response generated by Gemini.")

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: throw Exception("Empty content returned by Gemini.")
            val parts = content.optJSONArray("parts") ?: throw Exception("Empty parts returned by Gemini.")
            if (parts.length() == 0) throw Exception("Empty parts returned by Gemini.")

            val resultText = parts.getJSONObject(0).optString("text", "")
            if (resultText.isBlank()) throw Exception("No text content generated.") else resultText
        } catch (e: Exception) {
            throw Exception("Network error communicating with Gemini API: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun callGeminiApi(prompt: String, apiKey: String): String {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val part = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(part)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return when (response.code) {
                    400 -> "Gemini API Error (400): Bad Request. Please check your API key or prompt."
                    403 -> "Gemini API Error (403): API key invalid or does not have permissions."
                    429 -> "Gemini API Error (429): Quota exceeded. Please try again later."
                    else -> "Gemini API Error (${response.code}): ${response.message}"
                }
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return "No response candidates returned by Gemini."
            if (candidates.length() == 0) return "No response generated by Gemini."

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return "Empty content returned by Gemini."
            val parts = content.optJSONArray("parts") ?: return "Empty parts returned by Gemini."
            if (parts.length() == 0) return "Empty parts returned by Gemini."

            val resultText = parts.getJSONObject(0).optString("text", "")
            if (resultText.isBlank()) "No text content generated." else resultText
        } catch (e: Exception) {
            "Network error communicating with Gemini API: ${e.localizedMessage ?: e.message}"
        }
    }
}
