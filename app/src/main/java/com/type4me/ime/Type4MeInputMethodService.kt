package com.type4me.ime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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

    private val hasRecordPermission: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

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
        speechRecognizer?.destroy()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    statusTextView?.text = "🎤 正在听...请说话"
                }

                override fun onBeginningOfSpeech() {
                    statusTextView?.text = "👂 听到声音了..."
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    statusTextView?.text = "识别中..."
                }

                override fun onError(error: Int) {
                    Timber.tag(TAG).e("onError: $error")
                    isRecording = false
                    micButton?.text = "🎤 点击说话"

                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            statusTextView?.text = "需要麦克风权限"
                            // 延迟后尝试打开设置
                            handler.postDelayed({ openAppSettings() }, 500)
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            handler.postDelayed({ createNewRecognizer() }, 300)
                            statusTextView?.text = "识别器忙，正在重置..."
                        }
                        else -> {
                            val errorMsg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "录音失败"
                                SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                                SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                                SpeechRecognizer.ERROR_NO_MATCH -> "未能识别，请重试"
                                SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音，请重试"
                                else -> "未知错误"
                            }
                            statusTextView?.text = "错误: $errorMsg"
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    isRecording = false
                    micButton?.text = "🎤 点击说话"

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        editText?.setText(text)
                        statusTextView?.text = "✓ 识别成功，点击上屏"
                    } else {
                        statusTextView?.text = "未能识别，请重试"
                    }
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

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            statusTextView?.text = "请在设置中开启麦克风权限"
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to open settings")
            statusTextView?.text = "请手动去设置开启权限"
        }
    }

    override fun onCreateInputView(): View {
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
        layout.addView(TextView(this).apply {
            text = "Type4Me 语音输入"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
        })

        // 权限警告（如果没有权限）
        if (!hasRecordPermission) {
            layout.addView(TextView(this).apply {
                text = "⚠️ 缺少麦克风权限，点击去设置"
                setTextColor(0xFFFF6B6B.toInt())
                textSize = 14f
                setOnClickListener { openAppSettings() }
            })
        }

        // 状态显示
        statusTextView = TextView(this).apply {
            text = if (hasRecordPermission) "点击话筒开始录音" else "需要麦克风权限"
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10 }
        }
        layout.addView(statusTextView)

        // 录音按钮
        micButton = Button(this).apply {
            text = "🎤 点击说话"
            isEnabled = hasRecordPermission
            setOnClickListener {
                if (!hasRecordPermission) {
                    openAppSettings()
                    return@setOnClickListener
                }
                if (isRecording) stopRecording() else startRecording()
            }
        }
        layout.addView(micButton)

        // 编辑框
        editText = EditText(this).apply {
            hint = "识别结果..."
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
        }
        layout.addView(editText)

        // 按钮行
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        buttonRow.addView(Button(this).apply {
            text = "上屏"
            setOnClickListener {
                editText?.text?.toString()?.let { text ->
                    if (text.isNotEmpty()) {
                        currentInputConnection?.commitText(text, 1)
                        editText?.setText("")
                    }
                }
            }
        })

        buttonRow.addView(Button(this).apply {
            text = "设置"
            setOnClickListener { openAppSettings() }
        })

        buttonRow.addView(Button(this).apply {
            text = "关闭"
            setOnClickListener { requestHideSelf(0) }
        })

        layout.addView(buttonRow)
        return layout
    }

    private fun startRecording() {
        if (!hasRecordPermission) {
            statusTextView?.text = "需要麦克风权限"
            openAppSettings()
            return
        }

        if (speechRecognizer == null) {
            createNewRecognizer()
        }

        editText?.setText("")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            isRecording = true
            micButton?.text = "⏹ 停止"
        } catch (e: Exception) {
            statusTextView?.text = "启动失败，请重试"
            isRecording = false
        }
    }

    private fun stopRecording() {
        speechRecognizer?.stopListening()
        isRecording = false
        micButton?.text = "🎤 点击说话"
        statusTextView?.text = "已停止"
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
