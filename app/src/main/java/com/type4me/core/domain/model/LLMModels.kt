package com.type4me.core.domain.model

/**
 * LLM 提供商
 */
enum class LLMProvider {
    OPENAI,
    GEMINI,
    CLAUDE,
    GEMINI_FREE // 免费备选
}

/**
 * LLM 错误类型
 */
sealed class LLMError(val message: String) {
    class NetworkError(message: String) : LLMError(message)
    class AuthError(val provider: String, message: String) : LLMError(message)
    class RateLimitError(val provider: String, message: String) : LLMError(message)
    class UnknownError(val throwable: Throwable, message: String = throwable.message ?: "Unknown error") : LLMError(message)
}

/**
 * 提供商配置
 */
data class ProviderConfig(
    val provider: LLMProvider,
    val baseUrl: String,
    val modelName: String,
    val headerBuilder: (apiKey: String) -> Map<String, String>,
    val requestBuilder: (model: String, messages: List<Message>) -> Map<String, Any>
) {
    data class Message(
        val role: String,
        val content: String
    )
}
