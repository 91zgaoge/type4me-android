package com.type4me.core.data.remote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.type4me.core.domain.model.RecognitionResult
import com.type4me.util.PermissionUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun startRecognition(): Flow<RecognitionResult> = callbackFlow {
        if (!PermissionUtil.hasRecordAudioPermission(context)) {
            close(IllegalStateException("录音权限未授予"))
            return@callbackFlow
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Timber.d("准备就绪，可以开始说话")
                }

                override fun onBeginningOfSpeech() {
                    Timber.d("检测到语音开始")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // 音量变化，可用于显示波形
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // 音频缓冲区接收
                }

                override fun onEndOfSpeech() {
                    Timber.d("语音结束")
                }

                override fun onError(error: Int) {
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "音频录制失败"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_NO_MATCH -> "无法识别语音"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                        SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "说话超时"
                        else -> "未知错误"
                    }
                    Timber.e("语音识别错误: $errorMsg ($error)")
                    close(Exception(errorMsg))
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        val confidence = confidences?.get(0) ?: 1.0f
                        Timber.d("识别结果: $text (置信度: $confidence)")
                        trySend(RecognitionResult(text, isFinal = true, confidence = confidence))
                        close()
                    } else {
                        close(Exception("没有识别结果"))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!partial.isNullOrEmpty()) {
                        val text = partial[0]
                        Timber.d("部分识别: $text")
                        trySend(RecognitionResult(text, isFinal = false))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // 其他事件
                }
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        speechRecognizer?.startListening(intent)

        awaitClose {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}