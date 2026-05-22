package com.example.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

@JsonClass(generateAdapter = true)
data class AppInsight(
    val title: String,
    val body: String
)

data class GeminiResult(
    val score: Int,
    val feedback: String
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }

    /**
     * Converts & downsamples JPEG image from path into Base64 string.
     */
    private fun compressImageToBase64(filePath: String, maxDimension: Int = 512): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Image file not found: $filePath")
                return null
            }

            // Inspect image dimensions only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            // Determine downsample factor (inSampleSize)
            var sampleSize = 1
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth > maxDimension || srcHeight > maxDimension) {
                val halfWidth = srcWidth / 2
                val halfHeight = srcHeight / 2
                while ((halfWidth / sampleSize) >= maxDimension && (halfHeight / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }

            // Decode image downsampled
            val finalOptions = BitmapFactory.Options()
            finalOptions.inSampleSize = sampleSize
            
            val bitmap = BitmapFactory.decodeFile(filePath, finalOptions) ?: return null

            // Compress to JPEG with 70% quality
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (t: Throwable) {
            Log.e(TAG, "Error compressing image to Base64: ${t.message}", t)
            null
        }
    }

    /**
     * Evaluates a progress score from 1 to 10 and generates a Vietnamese coaching review for the moment.
     */
    suspend fun evaluateProgress(projectTitle: String, noteText: String, imagePath: String?): GeminiResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is missing or default placeholder!")
            return@withContext GeminiResult(4, "Một bước tiến tốt! Hãy tiếp tục chăm chỉ mỗi ngày hằng ngày nhé. (AI offline)")
        }

        val prompt = """
            You are an expert personal growth coach in Vietnam. The user is tracking a project called '$projectTitle'. 
            They just logged this moment with the following note: '$noteText'.
            We may have also attached a real photo of their effort.
            
            Evaluate this achievement:
            1. Provide a score from 1 to 10 on how much this activity/photo demonstrates meaningful progress towards their goal.
            2. Write a short, encouraging, and friendly coaching feedback message in Vietnamese (maximum 2 sentences) supporting the user.
            
            Reply ONLY with a raw JSON object in the following format, with no markdown delimiters:
            {
              "score": <integer from 1 to 10>,
              "feedback": "coaching message in Vietnamese"
            }
        """.trimIndent()

        // Construct parts list. If image exists and is a real file, add it as a part.
        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))

        if (imagePath != null && !imagePath.startsWith("placeholder_")) {
            val base64Data = compressImageToBase64(imagePath)
            if (base64Data != null) {
                parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data)))
                Log.d(TAG, "Sending multimodal request with attached image.")
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts))
        )

        try {
            val response = service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d(TAG, "Gemini Response: $responseText")

            if (responseText != null) {
                try {
                    val jsonStart = responseText.indexOf('{')
                    val jsonEnd = responseText.lastIndexOf('}')
                    if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                        val jsonStr = responseText.substring(jsonStart, jsonEnd + 1)
                        val jsonObj = JSONObject(jsonStr)
                        val score = jsonObj.getInt("score").coerceIn(1, 10)
                        val feedback = jsonObj.getString("feedback").trim()
                        GeminiResult(score, feedback)
                    } else {
                        val scoreString = responseText.replace(Regex("[^0-9]"), "")
                        val score = scoreString.toIntOrNull() ?: 4
                        GeminiResult(score.coerceIn(1, 10), "Nỗ lực tuyệt vời! Hãy duy trì thói quen này hằng ngày nhé!")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse JSON response: ${e.message}", e)
                    GeminiResult(4, "Một bước tiến tốt! Hãy tiếp tục chăm chỉ mỗi ngày hằng ngày nhé.")
                }
            } else {
                GeminiResult(4, "Một bước tiến tốt! Hãy tiếp tục chăm chỉ mỗi ngày hằng ngày nhé.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            GeminiResult(4, "Một bước tiến tốt! Hãy tiếp tục chăm chỉ mỗi ngày hằng ngày nhé.")
        }
    }

    suspend fun generateInsights(serializedData: String): List<AppInsight> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val prompt = """
            Analyze this user's project tracking data and give 3 short, encouraging insights (max 2 sentences each). Focus on patterns, consistency, and what they're doing well. End with one actionable suggestion.
            
            Data: ${'$'}serializedData
            
            Format your entire response strictly as a single clean JSON array of objects: [{"title": "insight title", "body": "insight body"}]. 
            Do not wrap with markdown code fences. Respond only with the JSON content.
            Use Vietnamese for titles and body texts in a warm and friendly coach persona.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d(TAG, "Gemini Insights Response: ${'$'}responseText")

            if (responseText != null) {
                val jsonStart = responseText.indexOf('[')
                val jsonEnd = responseText.lastIndexOf(']')
                if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    val jsonStr = responseText.substring(jsonStart, jsonEnd + 1)
                    val jsonArray = org.json.JSONArray(jsonStr)
                    val list = mutableListOf<AppInsight>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            AppInsight(
                                title = obj.optString("title", "Gợi ý thói quen"),
                                body = obj.optString("body", "")
                            )
                        )
                    }
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating insights: ${'$'}{e.message}", e)
        }

        // Return pleasant default insights as a fallback
        listOf(
            AppInsight(
                "Đầu tư thời gian chất lượng",
                "Bạn đang bắt đầu xây dựng thói quen kiên trì qua từng ngày. Mỗi khoảnh khắc được lưu lại là minh chứng rõ ràng nhất cho nỗ lực của bạn!"
            ),
            AppInsight(
                "Tính bền bỉ vượt trội",
                "Các hành trình của bạn đang có tín hiệu phát triển rất tích cực. Đừng nản lòng khi gặp khó khăn, hãy tiếp tục duy trì!"
            ),
            AppInsight(
                "Gợi ý hành động tiếp theo",
                "Hãy ghi lại thêm một khoảnh khắc mới ngày hôm nay cho các hành trình chưa ghi chép để củng cố chuỗi thói quen của mình."
            )
        )
    }
}
