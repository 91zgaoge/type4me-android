package com.type4me.core.domain.model

/**
 * Prompt 模板
 */
data class PromptTemplate(
    val id: String,
    val name: String,
    val prompt: String,
    val isBuiltIn: Boolean = false
)

/**
 * 上下文变量（用于 Prompt 替换）
 */
data class ContextVariables(
    val recognizedText: String,
    val clipboardContent: String?
)

/**
 * 内置 Prompt 模板
 */
object DefaultPrompts {
    val TRANSLATE = PromptTemplate(
        id = "translate",
        name = "翻译成中文",
        prompt = "将以下内容翻译成中文：\\n\\n{text}",
        isBuiltIn = true
    )

    val POLISH = PromptTemplate(
        id = "polish",
        name = "润色文字",
        prompt = "请润色以下文字，使其更通顺、专业：\\n\\n{text}",
        isBuiltIn = true
    )

    val SUMMARIZE = PromptTemplate(
        id = "summarize",
        name = "总结概括",
        prompt = "请总结以下内容的要点：\\n\\n{text}",
        isBuiltIn = true
    )

    val REPLY = PromptTemplate(
        id = "reply",
        name = "智能回复",
        prompt = "剪贴板内容：\\n{clipboard}\\n\\n请根据以上背景，针对以下话题给出一个得体的回复：\\n\\n{text}",
        isBuiltIn = true
    )

    val ALL = listOf(TRANSLATE, POLISH, SUMMARIZE, REPLY)
}
