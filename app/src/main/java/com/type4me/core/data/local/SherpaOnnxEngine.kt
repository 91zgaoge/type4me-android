package com.type4me.core.data.local

import android.content.Context
import android.os.Handler
import android.os.Looper
// import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * SherpaOnnx 离线语音识别引擎
 *
 * 注意：使用此引擎需要手动下载 SherpaOnnx Android AAR 文件
 * 并将其添加到 app/libs 目录，然后在 build.gradle.kts 中添加：
 * implementation(files("libs/sherpa-onnx.aar"))
 */
class SherpaOnnxEngine(private val context: Context) {

    companion object {
        private const val TAG = "SherpaOnnxEngine"

        // 模型配置 - 使用 Paraformer 中文模型
        private const val MODEL_URL_BASE = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models"
        private const val MODEL_DIR = "sherpa-onnx-paraformer-zh-2023-09-14"

        // 模型文件名
        private const val ENCODER_NAME = "encoder.onnx"
        private const val DECODER_NAME = "decoder.onnx"
        private const val JOINER_NAME = "joiner.onnx"
        private const val TOKENS_NAME = "tokens.txt"
    }

    // private var recognizer: OfflineRecognizer? = null
    private var isInitialized = false
    private var isRecording = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // 音频采集参数
    private val sampleRate = 16000
    private var audioRecord: android.media.AudioRecord? = null
    private val bufferSize = android.media.AudioRecord.getMinBufferSize(
        sampleRate,
        android.media.AudioFormat.CHANNEL_IN_MONO,
        android.media.AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    private val _recognitionResult = MutableSharedFlow<String>()
    val recognitionResult: SharedFlow<String> = _recognitionResult.asSharedFlow()

    /**
     * 检查模型是否已下载
     */
    fun isModelReady(): Boolean {
        val modelDir = File(context.getExternalFilesDir(null), MODEL_DIR)
        return modelDir.exists() &&
               File(modelDir, ENCODER_NAME).exists() &&
               File(modelDir, DECODER_NAME).exists() &&
               File(modelDir, JOINER_NAME).exists() &&
               File(modelDir, TOKENS_NAME).exists()
    }

    /**
     * 获取模型下载状态
     */
    fun getModelStatus(): String {
        return if (isModelReady()) {
            "模型已就绪"
        } else {
            "需要下载模型（约 200MB）"
        }
    }

    /**
     * 初始化引擎
     *
     * 注意：需要添加 SherpaOnnx AAR 依赖后才能使用
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        // 需要 SherpaOnnx AAR 库
        Timber.tag(TAG).w("SherpaOnnx not integrated. Please download AAR from https://github.com/k2-fsa/sherpa-onnx/releases")
        false

        /* 取消注释以下代码并添加 SherpaOnnx AAR 依赖后使用：
        if (isInitialized) return@withContext true

        if (!isModelReady()) {
            Timber.tag(TAG).d("Model not ready, cannot initialize")
            return@withContext false
        }

        try {
            val modelDir = File(context.getExternalFilesDir(null), MODEL_DIR)

            val config = OfflineRecognizerConfig(
                featConfig = FeatureExtractorConfig(
                    sampleRate = sampleRate.toFloat(),
                    featureDim = 80
                ),
                modelConfig = OfflineModelConfig(
                    paraformer = OfflineParaformerModelConfig(
                        model = File(modelDir, ENCODER_NAME).absolutePath
                    ),
                    tokens = File(modelDir, TOKENS_NAME).absolutePath,
                    numThreads = 4,
                    debug = false
                ),
                lmConfig = OfflineLMConfig(),
                decodingMethod = "greedy_search",
                maxActivePaths = 4
            )

            recognizer = OfflineRecognizer(config)
            isInitialized = true
            Timber.tag(TAG).d("SherpaOnnx initialized successfully")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize SherpaOnnx")
            false
        }
        */
    }

    /**
     * 开始录音识别
     */
    fun startRecording(): Boolean {
        Timber.tag(TAG).w("SherpaOnnx not integrated")
        return false

        /* 取消注释以下代码并添加 SherpaOnnx AAR 依赖后使用：
        if (!isInitialized) {
            Timber.tag(TAG).e("Engine not initialized")
            return false
        }

        if (isRecording) {
            Timber.tag(TAG).d("Already recording")
            return false
        }

        try {
            audioRecord = android.media.AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != android.media.AudioRecord.STATE_INITIALIZED) {
                Timber.tag(TAG).e("AudioRecord initialization failed")
                return false
            }

            isRecording = true
            audioRecord?.startRecording()

            scope.launch {
                val samples = mutableListOf<Float>()
                val buffer = ShortArray(bufferSize)

                while (isRecording && audioRecord?.recordingState == android.media.AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // 转换为 Float 样本
                        for (i in 0 until read) {
                            samples.add(buffer[i] / 32768.0f)
                        }
                    }
                    delay(10) // 10ms 采样间隔
                }

                // 识别
                if (samples.isNotEmpty()) {
                    recognize(samples.toFloatArray())
                }
            }

            Timber.tag(TAG).d("Recording started")
            return true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start recording")
            isRecording = false
            return false
        }
        */
    }

    /**
     * 停止录音并识别
     */
    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        Timber.tag(TAG).d("Recording stopped")
    }

    /**
     * 识别音频数据
     */
    private suspend fun recognize(samples: FloatArray) {
        // 需要 SherpaOnnx AAR 库
        _recognitionResult.emit("")

        /* 取消注释以下代码并添加 SherpaOnnx AAR 依赖后使用：
        withContext(Dispatchers.IO) {
            try {
                recognizer?.let { recog ->
                    val stream = recog.createStream()
                    stream.acceptWaveform(samples, sampleRate)

                    recog.decode(stream)
                    val result = recog.getResult(stream)

                    val text = result.text.trim()
                    if (text.isNotEmpty()) {
                        Timber.tag(TAG).d("Recognition result: $text")
                        _recognitionResult.emit(text)
                    } else {
                        _recognitionResult.emit("")
                    }

                    stream.release()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Recognition failed")
                _recognitionResult.emit("")
            }
        }
        */
    }

    /**
     * 下载模型文件
     */
    suspend fun downloadModel(progressCallback: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelDir = File(context.getExternalFilesDir(null), MODEL_DIR)
            modelDir.mkdirs()

            val files = listOf(
                ENCODER_NAME to "$MODEL_URL_BASE/$MODEL_DIR/$ENCODER_NAME",
                DECODER_NAME to "$MODEL_URL_BASE/$MODEL_DIR/$DECODER_NAME",
                JOINER_NAME to "$MODEL_URL_BASE/$MODEL_DIR/$JOINER_NAME",
                TOKENS_NAME to "$MODEL_URL_BASE/$MODEL_DIR/$TOKENS_NAME"
            )

            files.forEachIndexed { index, (filename, url) ->
                val file = File(modelDir, filename)
                if (file.exists() && file.length() > 0) {
                    Timber.tag(TAG).d("$filename already exists, skipping")
                } else {
                    Timber.tag(TAG).d("Downloading $filename from $url")
                    downloadFile(url, file)
                }
                progressCallback(((index + 1) * 100) / files.size)
            }

            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to download model")
            false
        }
    }

    private fun downloadFile(urlString: String, outputFile: File) {
        val url = URL(urlString)
        url.openStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun release() {
        stopRecording()
        scope.cancel()
        // recognizer?.release()
        // recognizer = null
        isInitialized = false
    }
}
