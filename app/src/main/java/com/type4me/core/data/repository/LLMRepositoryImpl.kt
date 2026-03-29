package com.type4me.core.data.repository

import com.type4me.core.data.PromptProcessor
import com.type4me.core.data.remote.LLMClient
import com.type4me.core.domain.model.ContextVariables
import com.type4me.core.domain.model.LLMError
import com.type4me.core.domain.model.LLMProvider
import com.type4me.core.domain.model.PromptTemplate
import com.type4me.core.domain.model.Settings
import com.type4me.core.domain.repository.LLMRepository
import com.type4me.core.domain.repository.SettingsRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMRepositoryImpl @Inject constructor(
    private val llmClient: LLMClient,
    private val promptProcessor: PromptProcessor,
    private val settingsRepository: SettingsRepository
) : LLMRepository {

    private var lastError: LLMError? = null

    override suspend fun optimizeText(
        text: String,
        clipboard: String?,
        promptTemplate: PromptTemplate
    ): Result<String> {
        val settings = settingsRepository.getSettingsSnapshot()
        val context = ContextVariables(recognizedText = text, clipboardContent = clipboard)
        val prompt = promptProcessor.process(promptTemplate.prompt, context)

        // 1. 尝试主提供商
        val primaryResult = tryProvider(settings.primaryProvider, prompt, settings)

        if (primaryResult.isSuccess) {
            return primaryResult
        }

        // 2. 如果是网络错误，尝试 Gemini 免费版（降级）
        val error = primaryResult.exceptionOrNull()
        if (shouldTryFallback(error)) {
            Timber.w("主提供商失败，降级到 Gemini 免费版")
            val fallbackResult = tryProvider(LLMProvider.GEMINI_FREE, prompt, settings)

            if (fallbackResult.isSuccess) {
                return fallbackResult
            }
        }

        // 3. 记录错误并返回失败
        if (error is LLMError) {
            lastError = error
        }

        return primaryResult
    }

    private suspend fun tryProvider(
        provider: LLMProvider,
        prompt: String,
        settings: Settings
    ): Result<String> {
        val (apiKey, model) = when (provider) {
            LLMProvider.OPENAI -> settings.openAiKey to settings.openAiModel
            LLMProvider.GEMINI -> settings.geminiKey to settings.geminiModel
            LLMProvider.CLAUDE -> settings.claudeKey to settings.claudeModel
            LLMProvider.GEMINI_FREE -> "" to "gemini-pro" // 免费版使用内置 key 或空
        }

        // 检查 API Key
        if (provider != LLMProvider.GEMINI_FREE && apiKey.isBlank()) {
            return Result.failure(
                LLMError.AuthError(provider.name, "API Key 未配置")
            )
        }

        return llmClient.chatCompletion(
            provider = provider,
            apiKey = apiKey,
            model = model,
            messages = listOf(
                LLMClient.Message(role = "user", content = prompt)
            )
        )
    }

    private fun shouldTryFallback(error: Throwable?): Boolean {
        return error is LLMError.NetworkError ||
                error is LLMError.RateLimitError
    }

    override fun getAvailableProviders(): List<LLMProvider> {
        return listOf(
            LLMProvider.GEMINI_FREE,
            LLMProvider.OPENAI,
            LLMProvider.GEMINI,
            LLMProvider.CLAUDE
        )
    }

    override fun getLastError(): LLMError? = lastError

    override suspend fun testProvider(provider: LLMProvider, apiKey: String): Result<Unit> {
        val model = when (provider) {
            LLMProvider.OPENAI -> "gpt-3.5-turbo"
            LLMProvider.GEMINI, LLMProvider.GEMINI_FREE -> "gemini-pro"
            LLMProvider.CLAUDE -> "claude-3-sonnet-20240229"
        }

        return llmClient.chatCompletion(
            provider = provider,
            apiKey = apiKey,
            model = model,
            messages = listOf(
                LLMClient.Message(role = "user", content = "Hello")
            )
        ).map { }
    }
}
