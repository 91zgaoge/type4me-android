package com.type4me.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("onCreate called")
    }

    private fun createNewRecognizer(): SpeechRecognizer? {
        // 销毁旧的识别器
        speechRecognizer?.let { oldRecognizer ->
            try {
                oldRecognizer.cancel()
                oldRecognizer.destroy()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error destroying old recognizer")
            }
            speechRecognizer = null
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Timber.tag(TAG).e("SpeechRecognizer not available")
            return null
        }

        return SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(createRecognitionListener())
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Timber.tag(TAG).d("onReadyForSpeech")
                statusTextView?.text = "🎤 正在听...请说话"
            }

            override fun onBeginningOfSpeech() {
                Timber.tag(TAG).d("onBeginningOfSpeech")
                statusTextView?.text = "👂 听到声音了..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                // 可以显示音量变化
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Timber.tag(TAG).d("onEndOfSpeech")
                statusTextView?.text = "识别中..."
            }

            override fun onError(error: Int) {
                Timber.tag(TAG).e("onError: $error")
                isRecording = false
                updateMicButtonText(false)

                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "录音失败"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少麦克风权限"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "未能识别，请重试"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        // 识别器忙，延迟重试
                        handler.postDelayed({
                            statusTextView?.text = "识别器重置中..."
                            createNewRecognizer()
                        }, 500)
                        "识别器忙，正在重置..."
                    }
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音，请重试"
                    else -> "未知错误"
                }

                if (error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    statusTextView?.text = "错误: $errorMsg"
                }
            }

            override fun onResults(results: Bundle?) {
                Timber.tag(TAG).d("onResults")
                isRecording = false
                updateMicButtonText(false)

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Timber.tag(TAG).d("识别结果: $text")
                    currentInputConnection?.commitText(text, 1)
                    statusTextView?.text = "✓ 已上屏"

                    // 识别成功后创建新的识别器，避免下次繁忙
                    handler.postDelayed({
                        createNewRecognizer()
                    }, 500)
                } else {
                    statusTextView?.text = "未能识别，请重试"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    statusTextView?.text = "识别中: $text"
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun updateMicButtonText(recording: Boolean) {
        val rootView = window?.window?.decorView?.findViewById<View>(android.R.id.content) as? ViewGroup
        val layout = rootView?.getChildAt(0) as? LinearLayout
        val micButton = layout?.getChildAt(2) as? Button
        micButton?.text = if (recording) "⏹ 停止录音" else "🎤 点击录音"
    }

    override fun onCreateInputView(): View {
        Timber.tag(TAG).d("onCreateInputView called")

        // 首次创建识别器
        if (speechRecognizer == null) {
            speechRecognizer = createNewRecognizer()
        }

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
                    stopRecording(this)
                } else {
                    startRecording(this)
                }
            }
        }
        layout.addView(micButton)

        Timber.tag(TAG).d("Input view created")
        return layout
    }

    private fun startRecording(button: Button) {
        Timber.tag(TAG).d("startRecording called")

        // 如果识别器为空或忙，创建新的
        if (speechRecognizer == null) {
            speechRecognizer = createNewRecognizer()
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
            // 设置较短的超时时间
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
        }

        try {
            speechRecognizer?.startListening(intent)
            isRecording = true
            button.text = "⏹ 停止录音"
            Timber.tag(TAG).d("startListening called")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "startListening failed")
            statusTextView?.text = "启动失败: ${e.message}，正在重试..."
            isRecording = false

            // 如果失败，创建新的识别器重试
            handler.postDelayed({
                speechRecognizer = createNewRecognizer()
                statusTextView?.text = "已重置，请再次点击录音"
            }, 500)
        }
    }

    private fun stopRecording(button: Button) {
        Timber.tag(TAG).d("stopRecording called")
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "stopListening failed")
        }
        isRecording = false
        button.text = "🎤 点击录音"
        statusTextView?.text = "已停止"
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("onDestroy called")
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in onDestroy")
        }
        speechRecognizer = null
    }
}
