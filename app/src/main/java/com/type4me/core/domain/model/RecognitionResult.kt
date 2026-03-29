package com.type4me.core.domain.model

/**
 * 语音识别结果
 */
data class RecognitionResult(
    val text: String,
    val isFinal: Boolean,
    val confidence: Float = 1.0f
)

/**
 * ASR 引擎类型
 */
enum class ASREngine {
    GOOGLE_ONLINE,
    SHERPA_ONNX_LOCAL
}

/**
 * ASR 引擎状态
 */
sealed class ASREngineState {
    object Idle : ASREngineState()
    object Initializing : ASREngineState()
    data class Ready(val engine: ASREngine) : ASREngineState()
    data class Error(val throwable: Throwable) : ASREngineState()
}
