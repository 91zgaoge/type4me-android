package com.type4me.core.data

import com.type4me.core.domain.model.ContextVariables
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptProcessor @Inject constructor() {

    /**
     * 处理模板，替换变量。
     * 注意：替换顺序很重要！先替换 clipboard 再替换 text，
     * 防止 clipboard 内容中包含 {text} 导致的注入问题。
     */
    fun process(template: String, context: ContextVariables): String {
        return template
            .replace("{clipboard}", escape(context.clipboardContent ?: ""))
            .replace("{text}", escape(context.recognizedText))
    }

    /**
     * 转义特殊字符，防止模板注入
     */
    private fun escape(input: String): String {
        // 简单的转义，防止变量中的大括号被二次解析
        return input.replace("{", "\\{").replace("}", "\\}")
    }
}
