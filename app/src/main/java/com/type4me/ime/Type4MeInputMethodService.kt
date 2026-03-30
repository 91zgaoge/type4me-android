package com.type4me.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import timber.log.Timber

class Type4MeInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "Type4MeIME"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    private var statusTextView: TextView? = null

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("onCreate called")
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Timber.tag(TAG).d("onReadyForSpeech")
                        statusTextView?.text = "🎤 正在听..."
                    }

                    override fun onBeginningOfSpeech() {
                        Timber.tag(TAG).d("onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Timber.tag(TAG).d("onEndOfSpeech")
                        isRecording = false
                        statusTextView?.text = "识别中..."
                    }

                    override fun onError(error: Int) {
                        Timber.tag(TAG).e("onError: $error")
                        isRecording = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "录音失败"
                            SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少权限"
                            SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                            SpeechRecognizer.ERROR_NO_MATCH -> "未能识别"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                            SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音"
                            else -> "未知错误"
                        }
                        statusTextView?.text = "错误: $errorMsg"
                    }

                    override fun onResults(results: Bundle?) {
                        Timber.tag(TAG).d("onResults")
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            Timber.tag(TAG).d("识别结果: $text")
                            currentInputConnection?.commitText(text, 1)
                            statusTextView?.text = "✓ 已上屏: $text"
                        } else {
                            statusTextView?.text = "未能识别"
                        }
                        isRecording = false
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            statusTextView?.text = "识别中: $text"
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            Timber.tag(TAG).d("SpeechRecognizer initialized")
        } else {
            Timber.tag(TAG).e("SpeechRecognizer not available")
        }
    }

    override fun onCreateInputView(): View {
        Timber.tag(TAG).d("onCreateInputView called")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                500
            )
            setPadding(20, 20, 20, 20)
        }

        // 标题
        val titleView = TextView(this).apply {
            text = "Type4Me 语音输入"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(titleView)

        // 状态显示
        statusTextView = TextView(this).apply {
            text = "点击话筒开始录音"
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
            }
        }
        layout.addView(statusTextView)

        // 录音按钮
        val micButton = Button(this).apply {
            text = "🎤 点击录音"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                if (isRecording) {
                    stopRecording()
                    text = "🎤 点击录音"
                } else {
                    startRecording()
                    text = "⏹ 停止录音"
                }
            }
        }
        layout.addView(micButton)

        Timber.tag(TAG).d("Input view created")
        return layout
    }

    private fun startRecording() {
        Timber.tag(TAG).d("startRecording called")

        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        if (speechRecognizer == null) {
            statusTextView?.text = "语音识别不可用"
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
            isRecording = true
            Timber.tag(TAG).d("startListening called")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "startListening failed")
            statusTextView?.text = "启动失败: ${e.message}"
            isRecording = false
        }
    }

    private fun stopRecording() {
        Timber.tag(TAG).d("stopRecording called")
        speechRecognizer?.stopListening()
        isRecording = false
        statusTextView?.text = "点击话筒开始录音"
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("onDestroy called")
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
