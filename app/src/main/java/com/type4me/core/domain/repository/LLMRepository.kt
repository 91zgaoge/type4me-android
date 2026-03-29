package com.type4me.core.domain.repository

import com.type4me.core.domain.model.ContextVariables
import com.type4me.core.domain.model.LLMError
import com.type4me.core.domain.model.LLMProvider
import com.type4me.core.domain.model.PromptTemplate

/**
 * LLM 仓库接口
 */
interface LLMRepository {
    /**
     * 使用 LLM 优化文本
     * 自动处理降级逻辑
     */
    suspend fun optimizeText(
        text: String,
        clipboard: String?,
        promptTemplate: PromptTemplate
    ): Result<String>

    /**
     * 获取所有可用的提供商
     */
    fun getAvailableProviders(): List<LLMProvider>

    /**
     * 获取上次错误
     */
    fun getLastError(): LLMError?

    /**
     * 测试提供商连接
     */
    suspend fun testProvider(provider: LLMProvider, apiKey: String): Result<Unit>
}
