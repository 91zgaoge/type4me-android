package com.type4me.core.domain.model

/**
 * 应用设置
 */
data class Settings(
    // ASR 设置
    val asrEngine: ASREngine = ASREngine.GOOGLE_ONLINE,
    val preferOffline: Boolean = false,
    val autoDownloadModel: Boolean = true,

    // LLM 设置
    val primaryProvider: LLMProvider = LLMProvider.GEMINI_FREE,
    val openAiKey: String = "",
    val openAiModel: String = "gpt-3.5-turbo",
    val geminiKey: String = "",
    val geminiModel: String = "gemini-pro",
    val claudeKey: String = "",
    val claudeModel: String = "claude-3-sonnet",

    // 通用设置
    val autoOptimize: Boolean = false,
    val defaultPromptId: String = DefaultPrompts.POLISH.id,
    val customPrompts: List<PromptTemplate> = emptyList()
)
