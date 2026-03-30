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
import android.widget.EditText
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
    private var micButton: Button? = null
    private var editText: EditText? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("onCreate called")
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Timber.tag(TAG).e("SpeechRecognizer not available")
            return
        }

        createNewRecognizer()
    }

    private fun createNewRecognizer() {
        // 销毁旧的
        speechRecognizer?.destroy()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Timber.tag(TAG).d("onReadyForSpeech")
                    statusTextView?.text = "🎤 正在听...请说话"
                }

                override fun onBeginningOfSpeech() {
                    Timber.tag(TAG).d("onBeginningOfSpeech")
                    statusTextView?.text = "👂 听到声音了..."
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Timber.tag(TAG).d("onEndOfSpeech")
                    statusTextView?.text = "识别中..."
                }

                override fun onError(error: Int) {
                    Timber.tag(TAG).e("onError: $error")
                    isRecording = false
                    micButton?.text = "🎤 点击说话"

                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "录音失败"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少麦克风权限，请在设置中开启"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_NO_MATCH -> "未能识别，请重试"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            handler.postDelayed({ createNewRecognizer() }, 300)
                            "识别器忙，正在重置..."
                        }
                        SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音，请重试"
                        else -> "未知错误"
                    }
                    statusTextView?.text = "错误: $errorMsg"
                }

                override fun onResults(results: Bundle?) {
                    Timber.tag(TAG).d("onResults")
                    isRecording = false
                    micButton?.text = "🎤 点击说话"

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        Timber.tag(TAG).d("识别结果: $text")
                        editText?.setText(text)
                        statusTextView?.text = "✓ 识别成功，点击上屏按钮"
                    } else {
                        statusTextView?.text = "未能识别，请重试"
                    }

                    // 重置识别器
                    handler.postDelayed({ createNewRecognizer() }, 500)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        editText?.setText(text)
                        statusTextView?.text = "识别中: $text"
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    override fun onCreateInputView(): View {
        Timber.tag(TAG).d("onCreateInputView called")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF2C2C2C.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(20, 20, 20, 20)
        }

        // 标题
        val titleView = TextView(this).apply {
            text = "Type4Me 语音输入"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
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
        micButton = Button(this).apply {
            text = "🎤 点击说话"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 15
            }
            setOnClickListener {
                if (isRecording) {
                    stopRecording()
                } else {
                    startRecording()
                }
            }
        }
        layout.addView(micButton)

        // 文字编辑框
        editText = EditText(this).apply {
            hint = "识别结果将显示在这里..."
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 15
            }
        }
        layout.addView(editText)

        // 操作按钮行
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
            }
        }

        // 上屏按钮
        buttonRow.addView(Button(this).apply {
            text = "上屏"
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener {
                val text = editText?.text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    currentInputConnection?.commitText(text, 1)
                    statusTextView?.text = "✓ 已上屏"
                    editText?.setText("")
                }
            }
        })

        // 清空按钮
        buttonRow.addView(Button(this).apply {
            text = "清空"
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener {
                editText?.setText("")
                statusTextView?.text = "已清空"
            }
        })

        // 关闭按钮
        buttonRow.addView(Button(this).apply {
            text = "关闭"
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener {
                requestHideSelf(0)
            }
        })

        layout.addView(buttonRow)

        return layout
    }

    private fun startRecording() {
        Timber.tag(TAG).d("startRecording called")

        if (speechRecognizer == null) {
            createNewRecognizer()
        }

        if (speechRecognizer == null) {
            statusTextView?.text = "语音识别不可用"
            return
        }

        // 清空之前的文字
        editText?.setText("")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
            isRecording = true
            micButton?.text = "⏹ 停止录音"
            Timber.tag(TAG).d("startListening called")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "startListening failed")
            statusTextView?.text = "启动失败，正在重试..."
            isRecording = false

            handler.postDelayed({
                createNewRecognizer()
                statusTextView?.text = "已重置，请再次点击"
            }, 500)
        }
    }

    private fun stopRecording() {
        Timber.tag(TAG).d("stopRecording called")
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "stopListening failed")
        }
        isRecording = false
        micButton?.text = "🎤 点击说话"
        statusTextView?.text = "已停止"
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("onDestroy called")
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
