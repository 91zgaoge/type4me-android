package com.type4me.ime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import com.type4me.core.data.local.SherpaOnnxEngine
import com.type4me.core.data.local.VoskEngine
import com.type4me.ime.ui.KeyboardScreen
import com.type4me.ui.theme.Type4MeTheme
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

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
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Compose 状态
    private var composeRecognizedText by mutableStateOf("")
    private var composeStatusText by mutableStateOf("")
    private var composeIsRecording by mutableStateOf(false)
    private var composePermissionGranted by mutableStateOf(false)
    private var composeEngineStatus by mutableStateOf("初始化中...")

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

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("onCreate called")

        // 设置 IME 主题
        setTheme(com.type4me.R.style.Theme_Type4Me)

        // 延迟初始化引擎，避免在 onCreate 中崩溃
        handler.postDelayed({
            try {
                sherpaEngine = SherpaOnnxEngine(this)
                voskEngine = VoskEngine(this)
                initGoogleSpeechRecognizer()
                Timber.tag(TAG).d("Engines initialized successfully")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to initialize engines")
                composeStatusText = "引擎初始化失败: ${e.message}"
            }
        }, 100)
    }

    override fun onCreateInputView(): View {
        Timber.tag(TAG).d("onCreateInputView called")

        // 更新初始状态
        composePermissionGranted = hasRecordPermission
        updateComposeEngineStatus()

        val composeView = ComposeView(this).apply {
            // 设置生命周期策略，避免 IME 生命周期问题
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow
            )
            setContent {
                Type4MeTheme(darkTheme = true) {
                    KeyboardScreen(
                        isRecording = composeIsRecording,
                        recognizedText = composeRecognizedText,
                        statusText = composeStatusText,
                        permissionGranted = composePermissionGranted,
                        currentEngine = currentEngine,
                        engineStatus = composeEngineStatus,
                        onMicClick = {
                            if (!hasRecordPermission) {
                                composeStatusText = "需要麦克风权限"
                                openAppSettings()
                            } else {
                                if (isRecording) stopRecording() else startRecording()
                            }
                        },
                        onCommitClick = {
                            if (composeRecognizedText.isNotEmpty()) {
                                currentInputConnection?.commitText(composeRecognizedText, 1)
                                composeRecognizedText = ""
                            }
                        },
                        onSettingsClick = { openAppSettings() },
                        onCloseClick = { requestHideSelf(0) },
                        onEngineSwitch = { toggleEngine() },
                        onRefreshPermission = {
                            composePermissionGranted = hasRecordPermission
                            updateComposeEngineStatus()
                        }
                    )
                }
            }
        }

        // 设置键盘高度 - 包装在固定高度的 FrameLayout 中
        return android.widget.FrameLayout(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(composeView)
        }
    }

    private fun updateComposeEngineStatus() {
        val voskInStorage = voskEngine?.isModelInExternalStorage() == true
        val voskNeedsExtraction = voskEngine?.needsExtraction() == true
        val sherpaReady = sherpaEngine?.isModelReady() == true

        // 调试信息
        val modelPath = voskEngine?.let { File(it.getActualModelPath()) }
        val pathExists = modelPath?.exists() == true
        val isDir = modelPath?.isDirectory == true

        Timber.tag(TAG).d("Model check: path=$modelPath, exists=$pathExists, isDir=$isDir, " +
            "voskInStorage=$voskInStorage, voskNeedsExtraction=$voskNeedsExtraction")

        composeEngineStatus = when {
            voskInStorage -> "就绪"
            voskNeedsExtraction -> "需解压"
            sherpaReady -> "就绪 (Sherpa)"
            pathExists -> "模型路径存在但结构不对"
            else -> "需下载"
        }
    }

    override fun onComputeInsets(outInsets: InputMethodService.Insets?) {
        super.onComputeInsets(outInsets)
        outInsets?.let {
            it.contentTopInsets = it.visibleTopInsets
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        Timber.tag(TAG).d("onWindowShown - refreshing permission state")
        composePermissionGranted = hasRecordPermission
        updateComposeEngineStatus()
    }

    @Deprecated("Deprecated in Java")
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        composePermissionGranted = hasRecordPermission
    }

    private fun toggleEngine() {
        when (currentEngine) {
            ASREngine.GOOGLE_ONLINE -> {
                if (voskEngine?.isModelInExternalStorage() == true) {
                    initVoskEngine()
                } else if (voskEngine?.needsExtraction() == true) {
                    extractVoskModelFromAssets()
                } else {
                    downloadVoskModel()
                }
            }
            ASREngine.VOSK_OFFLINE -> {
                if (sherpaEngine?.isModelReady() == true) {
                    initSherpaEngine()
                } else {
                    downloadModel()
                }
            }
            ASREngine.SHERPA_ONNX_OFFLINE -> {
                currentEngine = ASREngine.GOOGLE_ONLINE
                composeStatusText = "已切换到Google在线引擎（中国不可用）"
                updateComposeEngineStatus()
            }
        }
    }

    private fun extractVoskModelFromAssets() {
        scope.launch {
            composeStatusText = "正在解压模型，请稍候..."

            val success = voskEngine?.extractModelFromAssets { progress ->
                composeStatusText = "解压模型中... $progress%"
            } == true

            if (success) {
                composeStatusText = "模型解压完成，正在初始化..."
                val initialized = voskEngine?.initialize() == true
                if (initialized) {
                    currentEngine = ASREngine.VOSK_OFFLINE
                    composeStatusText = "Vosk 引擎已就绪"
                } else {
                    composeStatusText = "Vosk 初始化失败"
                }
            } else {
                composeStatusText = "模型解压失败，尝试下载..."
                downloadVoskModel()
            }
            updateComposeEngineStatus()
        }
    }

    private fun initVoskEngine() {
        scope.launch {
            composeStatusText = "正在初始化 Vosk 引擎..."
            val initialized = voskEngine?.initialize() == true
            if (initialized) {
                currentEngine = ASREngine.VOSK_OFFLINE
                composeStatusText = "Vosk 引擎已就绪"
            } else {
                composeStatusText = "Vosk 初始化失败，请检查模型"
            }
            updateComposeEngineStatus()
        }
    }

    private fun initSherpaEngine() {
        scope.launch {
            composeStatusText = "正在初始化 Sherpa 引擎..."
            val initialized = sherpaEngine?.initialize() == true
            if (initialized) {
                currentEngine = ASREngine.SHERPA_ONNX_OFFLINE
                composeStatusText = "Sherpa 引擎已就绪"
            } else {
                composeStatusText = "Sherpa 初始化失败"
            }
            updateComposeEngineStatus()
        }
    }

    private fun downloadVoskModel() {
        composeStatusText = "请下载 Vosk 中文模型 (~40MB)"
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://alphacephei.com/vosk/models")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            composeStatusText = "请手动下载模型: alphacephei.com/vosk/models"
        }
    }

    private fun downloadModel() {
        scope.launch {
            composeStatusText = "正在下载模型，请稍候..."

            val success = sherpaEngine?.downloadModel { progress ->
                composeStatusText = "下载模型中... $progress%"
            } == true

            if (success) {
                composeStatusText = "模型下载完成，正在初始化..."
                val initialized = sherpaEngine?.initialize() == true
                if (initialized) {
                    currentEngine = ASREngine.SHERPA_ONNX_OFFLINE
                    composeStatusText = "离线引擎已就绪"
                } else {
                    composeStatusText = "引擎初始化失败"
                }
            } else {
                composeStatusText = "模型下载失败"
            }
            updateComposeEngineStatus()
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
                    composeIsRecording = false

                    composeStatusText = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            handler.postDelayed({ openAppSettings() }, 500)
                            "需要麦克风权限"
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            handler.postDelayed({ createNewGoogleRecognizer() }, 300)
                            "识别器忙，正在重置..."
                        }
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误，无法连接语音识别服务器"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时，请检查网络连接"
                        SpeechRecognizer.ERROR_NO_MATCH -> "未能识别到语音，请重试"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音输入"
                        SpeechRecognizer.ERROR_AUDIO -> "录音设备错误"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                        SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                        else -> "错误代码: $error"
                    }
                }

                override fun onResults(results: Bundle?) {
                    Timber.tag(TAG).d("onResults called")
                    isRecording = false
                    composeIsRecording = false

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Timber.tag(TAG).d("Recognition results: $matches")

                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        Timber.tag(TAG).d("Best match: $text")
                        composeRecognizedText = text
                        composeStatusText = "✓ 识别成功，点击上屏"
                    } else {
                        composeStatusText = "未能识别，请重试"
                    }
                    handler.postDelayed({ createNewGoogleRecognizer() }, 500)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Timber.tag(TAG).d("onPartialResults: $matches")
                    if (!matches.isNullOrEmpty()) {
                        composeRecognizedText = matches[0]
                        composeStatusText = "识别中: ${matches[0]}"
                    }
                }

                override fun onReadyForSpeech(params: Bundle?) {
                    Timber.tag(TAG).d("onReadyForSpeech called")
                    composeStatusText = "🎤 正在听...请说话"
                }

                override fun onBeginningOfSpeech() {
                    Timber.tag(TAG).d("onBeginningOfSpeech called")
                    composeStatusText = "👂 听到声音了..."
                }

                override fun onEndOfSpeech() {
                    Timber.tag(TAG).d("onEndOfSpeech called")
                    composeStatusText = "识别中..."
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
            composeStatusText = "请在设置中开启麦克风权限"
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to open settings")
            composeStatusText = "请手动去设置开启权限"
        }
    }

    private fun startRecording() {
        if (!hasRecordPermission) {
            composeStatusText = "需要麦克风权限"
            openAppSettings()
            return
        }

        Timber.tag(TAG).d("startRecording called, currentEngine=$currentEngine")
        composeRecognizedText = ""

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
            composeIsRecording = true
            composeStatusText = "🎤 正在听...请说话 (在线)"
            Timber.tag(TAG).d("startListening called successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start Google listening")
            composeStatusText = "在线引擎启动失败，尝试离线引擎..."
            currentEngine = ASREngine.SHERPA_ONNX_OFFLINE
            updateComposeEngineStatus()
            startSherpaRecording()
        }
    }

    private fun startVoskRecording() {
        Timber.tag(TAG).d("Starting Vosk recording")

        if (voskEngine?.let { it.isModelReady() || it.getModelStatus().contains("就绪") } != true) {
            composeStatusText = "Vosk 模型未就绪，点击切换引擎下载"
            return
        }

        scope.launch {
            val initialized = voskEngine?.initialize() == true
            if (!initialized) {
                composeStatusText = "Vosk 引擎初始化失败"
                return@launch
            }

            scope.launch {
                voskEngine?.partialResult?.collect { text ->
                    if (text.isNotEmpty()) {
                        composeRecognizedText = text
                        composeStatusText = "识别中: $text"
                    }
                }
            }

            scope.launch {
                voskEngine?.recognitionResult?.collect { text ->
                    if (text.isNotEmpty()) {
                        composeRecognizedText = text
                        composeStatusText = "✓ 识别成功，点击上屏"
                    } else {
                        composeStatusText = "未能识别，请重试"
                    }
                    isRecording = false
                    composeIsRecording = false
                }
            }

            val started = voskEngine?.startRecording() == true
            if (started) {
                isRecording = true
                composeIsRecording = true
                composeStatusText = "🎤 正在听...请说话 (Vosk离线)"
            } else {
                composeStatusText = "Vosk 引擎启动失败"
            }
        }
    }

    private fun startSherpaRecording() {
        Timber.tag(TAG).d("Starting SherpaOnnx recording")

        if (sherpaEngine?.isModelReady() != true) {
            composeStatusText = "离线模型未就绪"
            return
        }

        scope.launch {
            val initialized = sherpaEngine?.initialize() == true
            if (!initialized) {
                composeStatusText = "离线引擎初始化失败"
                return@launch
            }

            scope.launch {
                sherpaEngine?.recognitionResult?.collect { text ->
                    if (text.isNotEmpty()) {
                        composeRecognizedText = text
                        composeStatusText = "✓ 识别成功，点击上屏"
                    } else {
                        composeStatusText = "未能识别，请重试"
                    }
                    isRecording = false
                    composeIsRecording = false
                }
            }

            val started = sherpaEngine?.startRecording() == true
            if (started) {
                isRecording = true
                composeIsRecording = true
                composeStatusText = "🎤 正在听...请说话 (离线)"
            } else {
                composeStatusText = "离线引擎启动失败"
            }
        }
    }

    private fun stopRecording() {
        Timber.tag(TAG).d("stopRecording called, currentEngine=$currentEngine")

        when (currentEngine) {
            ASREngine.GOOGLE_ONLINE -> speechRecognizer?.stopListening()
            ASREngine.SHERPA_ONNX_OFFLINE -> sherpaEngine?.stopRecording()
            ASREngine.VOSK_OFFLINE -> voskEngine?.stopRecording()
        }

        isRecording = false
        composeIsRecording = false
        composeStatusText = "已停止"
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        sherpaEngine?.release()
        voskEngine?.release()
        scope.cancel()
    }
}
