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
import com.type4me.core.data.local.SherpaOnnxEngine
import com.type4me.core.data.local.VoskEngine
import kotlinx.coroutines.*
import timber.log.Timber

class Type4MeInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "Type4MeIME"
    }

    // 引擎类型
    enum class ASREngine {
        GOOGLE_ONLINE,
        SHERPA_ONNX_OFFLINE,
        VOSK_OFFLINE
    }

    private var currentEngine: ASREngine = ASREngine.VOSK_OFFLINE
    private var speechRecognizer: SpeechRecognizer? = null
    private var sherpaEngine: SherpaOnnxEngine? = null
    private var voskEngine: VoskEngine? = null
    private var isRecording = false
    private var statusTextView: TextView? = null
    private var micButton: Button? = null
    private var editText: EditText? = null
    private var engineSwitchButton: Button? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var permissionStatusText: TextView? = null

    private val hasRecordPermission: Boolean
        get() = try {
            val result = applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            val granted = result == PackageManager.PERMISSION_GRANTED
            Timber.tag(TAG).d("Permission check: result=$result, granted=$granted")
            granted
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking permission")
            false
        }

    private fun updatePermissionUI() {
        val hasPermission = hasRecordPermission
        Timber.tag(TAG).d("updatePermissionUI: hasPermission=$hasPermission")

        permissionStatusText?.text = if (hasPermission) {
            "✓ 麦克风权限已开启"
        } else {
            "⚠️ 缺少麦克风权限，点击下方按钮去设置"
        }
        permissionStatusText?.setTextColor(
            if (hasPermission) 0xFF4CAF50.toInt() else 0xFFFF6B6B.toInt()
        )

        micButton?.isEnabled = hasPermission
        micButton?.text = if (hasPermission) "🎤 点击说话" else "🎤 请先开启权限"
    }

    override fun onWindowShown() {
        super.onWindowShown()
        Timber.tag(TAG).d("onWindowShown - refreshing permission state")
        updatePermissionUI()
        updateEngineStatusUI()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("onCreate called")

        // 初始化离线引擎
        sherpaEngine = SherpaOnnxEngine(this)
        voskEngine = VoskEngine(this)
        updateEngineStatusUI()

        // Google 引擎在中国不可用，默认使用 Vosk
        initGoogleSpeechRecognizer()
    }

    private fun updateEngineStatusUI() {
        val voskReady = voskEngine?.let { it.isModelReady() || it.getModelStatus().contains("就绪") } == true
        val sherpaReady = sherpaEngine?.isModelReady() == true

        val status = when {
            voskReady -> "Vosk离线引擎就绪"
            sherpaReady -> "Sherpa离线引擎就绪"
            else -> "离线引擎需要下载模型"
        }

        val currentName = when (currentEngine) {
            ASREngine.GOOGLE_ONLINE -> "Google在线"
            ASREngine.SHERPA_ONNX_OFFLINE -> "Sherpa离线"
            ASREngine.VOSK_OFFLINE -> "Vosk离线"
        }

        engineSwitchButton?.text = "$currentName ($status)"
    }

    private fun toggleEngine() {
        when (currentEngine) {
            ASREngine.GOOGLE_ONLINE -> {
                // 切换到 Vosk（推荐）
                if (voskEngine?.isModelReady() == true) {
                    initVoskEngine()
                } else {
                    downloadVoskModel()
                }
            }
            ASREngine.VOSK_OFFLINE -> {
                // 切换到 Sherpa
                if (sherpaEngine?.isModelReady() == true) {
                    initSherpaEngine()
                } else {
                    downloadModel()
                }
            }
            ASREngine.SHERPA_ONNX_OFFLINE -> {
                // 切换回 Google
                currentEngine = ASREngine.GOOGLE_ONLINE
                statusTextView?.text = "已切换到Google在线引擎（中国不可用）"
                updateEngineStatusUI()
            }
        }
    }

    private fun initVoskEngine() {
        scope.launch {
            statusTextView?.text = "正在初始化 Vosk 引擎..."
            val initialized = voskEngine?.initialize() == true
            if (initialized) {
                currentEngine = ASREngine.VOSK_OFFLINE
                statusTextView?.text = "Vosk 引擎已就绪"
            } else {
                statusTextView?.text = "Vosk 初始化失败，请检查模型"
            }
            updateEngineStatusUI()
        }
    }

    private fun initSherpaEngine() {
        scope.launch {
            statusTextView?.text = "正在初始化 Sherpa 引擎..."
            val initialized = sherpaEngine?.initialize() == true
            if (initialized) {
                currentEngine = ASREngine.SHERPA_ONNX_OFFLINE
                statusTextView?.text = "Sherpa 引擎已就绪"
            } else {
                statusTextView?.text = "Sherpa 初始化失败"
            }
            updateEngineStatusUI()
        }
    }

    private fun downloadVoskModel() {
        statusTextView?.text = "请下载 Vosk 中文模型 (~40MB)"
        // 打开浏览器下载模型
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://alphacephei.com/vosk/models")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            statusTextView?.text = "请手动下载模型: alphacephei.com/vosk/models"
        }
    }

    private fun downloadModel() {
        scope.launch {
            statusTextView?.text = "正在下载模型，请稍候..."
            engineSwitchButton?.isEnabled = false

            val success = sherpaEngine?.downloadModel { progress ->
                handler.post {
                    statusTextView?.text = "下载模型中... $progress%"
                }
            } == true

            engineSwitchButton?.isEnabled = true

            if (success) {
                statusTextView?.text = "模型下载完成，正在初始化..."
                val initialized = sherpaEngine?.initialize() == true
                if (initialized) {
                    currentEngine = ASREngine.SHERPA_ONNX_OFFLINE
                    statusTextView?.text = "离线引擎已就绪"
                } else {
                    statusTextView?.text = "引擎初始化失败"
                }
            } else {
                statusTextView?.text = "模型下载失败"
            }
            updateEngineStatusUI()
        }
    }

    private fun initGoogleSpeechRecognizer() {
        createNewGoogleRecognizer()
    }

    private fun createNewGoogleRecognizer() {
        speechRecognizer?.destroy()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onError(error: Int) {
                    Timber.tag(TAG).e("onError called with error code: $error")
                    isRecording = false
                    micButton?.text = "🎤 点击说话"

                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            statusTextView?.text = "需要麦克风权限"
                            handler.postDelayed({ openAppSettings() }, 500)
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            Timber.tag(TAG).d("Recognizer busy, recreating...")
                            handler.postDelayed({ createNewGoogleRecognizer() }, 300)
                            statusTextView?.text = "识别器忙，正在重置..."
                        }
                        SpeechRecognizer.ERROR_NETWORK -> {
                            statusTextView?.text = "网络错误，无法连接语音识别服务器"
                        }
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            statusTextView?.text = "网络超时，请检查网络连接"
                        }
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            statusTextView?.text = "未能识别到语音，请重试"
                        }
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            statusTextView?.text = "未检测到语音输入"
                        }
                        SpeechRecognizer.ERROR_AUDIO -> {
                            statusTextView?.text = "录音设备错误"
                        }
                        SpeechRecognizer.ERROR_CLIENT -> {
                            statusTextView?.text = "客户端错误"
                        }
                        SpeechRecognizer.ERROR_SERVER -> {
                            statusTextView?.text = "服务器错误"
                        }
                        else -> {
                            statusTextView?.text = "错误代码: $error"
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    Timber.tag(TAG).d("onResults called")
                    isRecording = false
                    micButton?.text = "🎤 点击说话"

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Timber.tag(TAG).d("Recognition results: $matches")

                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        Timber.tag(TAG).d("Best match: $text")
                        editText?.setText(text)
                        statusTextView?.text = "✓ 识别成功，点击上屏"
                    } else {
                        statusTextView?.text = "未能识别，请重试"
                    }
                    handler.postDelayed({ createNewGoogleRecognizer() }, 500)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Timber.tag(TAG).d("onPartialResults: $matches")
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        editText?.setText(text)
                        statusTextView?.text = "识别中: $text"
                    }
                }

                override fun onReadyForSpeech(params: Bundle?) {
                    Timber.tag(TAG).d("onReadyForSpeech called")
                    statusTextView?.text = "🎤 正在听...请说话"
                }

                override fun onBeginningOfSpeech() {
                    Timber.tag(TAG).d("onBeginningOfSpeech called")
                    statusTextView?.text = "👂 听到声音了..."
                }

                override fun onEndOfSpeech() {
                    Timber.tag(TAG).d("onEndOfSpeech called")
                    statusTextView?.text = "识别中..."
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    Timber.tag(TAG).d("onEvent: type=$eventType")
                }
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
        layout.addView(TextView(this).apply {
            text = "Type4Me 语音输入"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
        })

        // 权限状态显示（可点击刷新）
        permissionStatusText = TextView(this).apply {
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10 }
            setOnClickListener {
                // 点击刷新权限状态
                updatePermissionUI()
            }
        }
        layout.addView(permissionStatusText)

        // 初始化权限显示
        updatePermissionUI()

        // 引擎切换按钮
        engineSwitchButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
            }
            setOnClickListener { toggleEngine() }
        }
        layout.addView(engineSwitchButton)
        updateEngineStatusUI()

        // 录音按钮
        micButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 15
            }
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

        Timber.tag(TAG).d("startRecording called, currentEngine=$currentEngine")

        editText?.setText("")

        when (currentEngine) {
            ASREngine.GOOGLE_ONLINE -> startGoogleRecording()
            ASREngine.SHERPA_ONNX_OFFLINE -> startSherpaRecording()
            ASREngine.VOSK_OFFLINE -> startVoskRecording()
        }
    }

    private fun startGoogleRecording() {
        Timber.tag(TAG).d("Starting Google SpeechRecognizer")

        if (speechRecognizer == null) {
            createNewGoogleRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            Timber.tag(TAG).d("Calling speechRecognizer.startListening...")
            speechRecognizer?.startListening(intent)
            isRecording = true
            micButton?.text = "⏹ 停止"
            statusTextView?.text = "🎤 正在听...请说话 (在线)"
            Timber.tag(TAG).d("startListening called successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start Google listening")
            statusTextView?.text = "在线引擎启动失败，尝试离线引擎..."
            // 自动切换到离线引擎
            currentEngine = ASREngine.SHERPA_ONNX_OFFLINE
            updateEngineStatusUI()
            startSherpaRecording()
        }
    }

    private fun startVoskRecording() {
        Timber.tag(TAG).d("Starting Vosk recording")

        // 确保引擎已初始化
        if (voskEngine?.let { it.isModelReady() || it.getModelStatus().contains("就绪") } != true) {
            statusTextView?.text = "Vosk 模型未就绪，点击切换引擎下载"
            return
        }

        scope.launch {
            // 初始化引擎
            val initialized = voskEngine?.initialize() == true
            if (!initialized) {
                statusTextView?.text = "Vosk 引擎初始化失败"
                return@launch
            }

            // 收集部分识别结果
            scope.launch {
                voskEngine?.partialResult?.collect { text ->
                    handler.post {
                        if (text.isNotEmpty()) {
                            editText?.setText(text)
                            statusTextView?.text = "识别中: $text"
                        }
                    }
                }
            }

            // 收集最终结果
            scope.launch {
                voskEngine?.recognitionResult?.collect { text ->
                    handler.post {
                        if (text.isNotEmpty()) {
                            editText?.setText(text)
                            statusTextView?.text = "✓ 识别成功，点击上屏"
                        } else {
                            statusTextView?.text = "未能识别，请重试"
                        }
                        isRecording = false
                        micButton?.text = "🎤 点击说话"
                    }
                }
            }

            val started = voskEngine?.startRecording() == true
            if (started) {
                isRecording = true
                micButton?.text = "⏹ 停止"
                statusTextView?.text = "🎤 正在听...请说话 (Vosk离线)"
            } else {
                statusTextView?.text = "Vosk 引擎启动失败"
            }
        }
    }

    private fun startSherpaRecording() {
        Timber.tag(TAG).d("Starting SherpaOnnx recording")

        if (sherpaEngine?.isModelReady() != true) {
            statusTextView?.text = "离线模型未就绪"
            return
        }

        scope.launch {
            val initialized = sherpaEngine?.initialize() == true
            if (!initialized) {
                statusTextView?.text = "离线引擎初始化失败"
                return@launch
            }

            // 收集识别结果
            scope.launch {
                sherpaEngine?.recognitionResult?.collect { text ->
                    handler.post {
                        if (text.isNotEmpty()) {
                            editText?.setText(text)
                            statusTextView?.text = "✓ 识别成功，点击上屏"
                        } else {
                            statusTextView?.text = "未能识别，请重试"
                        }
                        isRecording = false
                        micButton?.text = "🎤 点击说话"
                    }
                }
            }

            val started = sherpaEngine?.startRecording() == true
            if (started) {
                isRecording = true
                handler.post {
                    micButton?.text = "⏹ 停止"
                    statusTextView?.text = "🎤 正在听...请说话 (离线)"
                }
            } else {
                handler.post {
                    statusTextView?.text = "离线引擎启动失败"
                }
            }
        }
    }

    private fun stopRecording() {
        Timber.tag(TAG).d("stopRecording called, currentEngine=$currentEngine")

        when (currentEngine) {
            ASREngine.GOOGLE_ONLINE -> {
                speechRecognizer?.stopListening()
            }
            ASREngine.SHERPA_ONNX_OFFLINE -> {
                sherpaEngine?.stopRecording()
            }
            ASREngine.VOSK_OFFLINE -> {
                voskEngine?.stopRecording()
            }
        }

        isRecording = false
        micButton?.text = "🎤 点击说话"
        statusTextView?.text = "已停止"
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        sherpaEngine?.release()
        voskEngine?.release()
        scope.cancel()
    }
}
