package com.type4me.core.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.type4me.core.domain.model.LLMError
import com.type4me.core.domain.model.LLMProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMClient @Inject constructor() {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun chatCompletion(
        provider: LLMProvider,
        apiKey: String,
        model: String,
        messages: List<Message>
    ): Result<String> {
        return try {
            val (url, headers, body) = buildRequest(provider, apiKey, model, messages)

            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
            val request = requestBuilder.post(body.toRequestBody(jsonMediaType)).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Timber.e("LLM API 错误: ${response.code}, $errorBody")

                    val error = when (response.code) {
                        401 -> LLMError.AuthError(provider.name, "API Key 无效")
                        429 -> LLMError.RateLimitError(provider.name, "请求过于频繁")
                        in 500..599 -> LLMError.NetworkError("服务器错误")
                        else -> LLMError.NetworkError("请求失败: ${response.code}")
                    }
                    return Result.failure(error)
                }

                val responseBody = response.body?.string()
                    ?: return Result.failure(LLMError.NetworkError("空响应"))

                val result = parseResponse(provider, responseBody)
                Result.success(result)
            }
        } catch (e: IOException) {
            Timber.e(e, "网络请求失败")
            Result.failure(LLMError.NetworkError("网络连接失败: ${e.message}"))
        } catch (e: Exception) {
            Timber.e(e, "未知错误")
            Result.failure(LLMError.UnknownError(e))
        }
    }

    private fun buildRequest(
        provider: LLMProvider,
        apiKey: String,
        model: String,
        messages: List<Message>
    ): Triple<String, Map<String, String>, String> {
        return when (provider) {
            LLMProvider.OPENAI -> {
                val url = "https://api.openai.com/v1/chat/completions"
                val headers = mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json"
                )
                val bodyMap = mapOf(
                    "model" to model,
                    "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
                    "temperature" to 0.7,
                    "max_tokens" to 2048
                )
                Triple(url, headers, gson.toJson(bodyMap))
            }

            LLMProvider.GEMINI, LLMProvider.GEMINI_FREE -> {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val headers = mapOf("Content-Type" to "application/json")
                val contents = messages.map { msg ->
                    mapOf(
                        "role" to if (msg.role == "user") "user" else "model",
                        "parts" to listOf(mapOf("text" to msg.content))
                    )
                }
                val bodyMap = mapOf("contents" to contents)
                Triple(url, headers, gson.toJson(bodyMap))
            }

            LLMProvider.CLAUDE -> {
                val url = "https://api.anthropic.com/v1/messages"
                val headers = mapOf(
                    "x-api-key" to apiKey,
                    "anthropic-version" to "2023-06-01",
                    "Content-Type" to "application/json"
                )
                val bodyMap = mapOf(
                    "model" to model,
                    "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
                    "max_tokens" to 2048
                )
                Triple(url, headers, gson.toJson(bodyMap))
            }
        }
    }

    private fun parseResponse(provider: LLMProvider, responseBody: String): String {
        val jsonObj = gson.fromJson(responseBody, JsonObject::class.java)

        return when (provider) {
            LLMProvider.OPENAI -> {
                val choices = jsonObj.getAsJsonArray("choices")
                choices?.get(0)?.asJsonObject?.getAsJsonObject("message")?.get("content")?.asString
                    ?: throw IllegalStateException("无法解析 OpenAI 响应")
            }

            LLMProvider.GEMINI, LLMProvider.GEMINI_FREE -> {
                val candidates = jsonObj.getAsJsonArray("candidates")
                candidates?.get(0)?.asJsonObject?.getAsJsonObject("content")
                    ?.getAsJsonArray("parts")?.get(0)?.asJsonObject?.get("text")?.asString
                    ?: throw IllegalStateException("无法解析 Gemini 响应")
            }

            LLMProvider.CLAUDE -> {
                val content = jsonObj.getAsJsonArray("content")
                content?.get(0)?.asJsonObject?.get("text")?.asString
                    ?: throw IllegalStateException("无法解析 Claude 响应")
            }
        }
    }

    data class Message(
        val role: String,
        val content: String
    )
}