package com.type4me.core.data.local

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.vosk.Model
import org.vosk.Recognizer
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Vosk 离线语音识别引擎
 * 纯离线，无需网络，支持中文
 */
class VoskEngine(private val context: Context) {

    companion object {
        private const val TAG = "VoskEngine"
        private const val SAMPLE_RATE = 16000

        // 模型下载地址
        private const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip"
        private const val MODEL_DIR = "vosk-model-small-cn-0.22"
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isInitialized = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    // Assets 模型管理器
    private val assetsModelManager by lazy { AssetsModelManager(context) }

    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    private val _recognitionResult = MutableSharedFlow<String>()
    val recognitionResult: SharedFlow<String> = _recognitionResult.asSharedFlow()

    private val _partialResult = MutableSharedFlow<String>()
    val partialResult: SharedFlow<String> = _partialResult.asSharedFlow()

    /**
     * 获取模型路径
     */
    private fun getModelPath(): String {
        return File(context.getExternalFilesDir(null), MODEL_DIR).absolutePath
    }

    /**
     * 检查模型是否已就绪（外部存储或 assets）
     */
    fun isModelReady(): Boolean {
        // 检查外部存储
        if (isModelInExternalStorage()) {
            return true
        }

        // 检查 assets 中是否有模型（需要解压）
        if (assetsModelManager.hasModelInAssets()) {
            return true
        }

        return false
    }

    /**
     * 检查外部存储中是否有完整模型
     */
    fun isModelInExternalStorage(): Boolean {
        val basePath = File(getModelPath())

        // 情况1: 标准路径下直接有模型文件
        if (checkModelStructure(basePath)) {
            return true
        }

        // 情况2: 多嵌套了一层目录
        val nestedDir = File(basePath, MODEL_DIR)
        if (checkModelStructure(nestedDir)) {
            return true
        }

        // 情况3: 检查任何子目录
        basePath.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
            if (checkModelStructure(subDir)) {
                return true
            }
        }

        return false
    }

    /**
     * 检查是否需要从 assets 解压
     */
    fun needsExtraction(): Boolean {
        return assetsModelManager.hasModelInAssets() && !isModelInExternalStorage()
    }

    /**
     * 从 assets 解压模型
     */
    suspend fun extractModelFromAssets(progressCallback: (Int) -> Unit): Boolean {
        return assetsModelManager.extractModel(progressCallback)
    }

    /**
     * 获取实际模型路径（处理嵌套目录情况）
     * 优先使用已解压的外部存储路径
     */
    fun getActualModelPath(): String {
        val basePath = File(getModelPath())

        // 标准路径
        if (checkModelStructure(basePath)) {
            return basePath.absolutePath
        }

        // 嵌套目录
        val nestedDir = File(basePath, MODEL_DIR)
        if (checkModelStructure(nestedDir)) {
            return nestedDir.absolutePath
        }

        // 任何子目录
        basePath.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
            if (checkModelStructure(subDir)) {
                return subDir.absolutePath
            }
        }

        // 默认返回 assets 管理器的路径（用于首次解压）
        return assetsModelManager.getModelPath()
    }

    /**
     * 检查指定路径是否包含完整的模型结构
     */
    private fun checkModelStructure(modelDir: File): Boolean {
        if (!modelDir.exists() || !modelDir.isDirectory) return false

        // 检查关键文件是否存在
        val hasAmDir = File(modelDir, "am").exists()
        val hasConfDir = File(modelDir, "conf").exists()
        val hasModelFile = File(modelDir, "am/final.mdl").exists()

        Timber.tag(TAG).d("Checking ${modelDir.absolutePath}: am=$hasAmDir, conf=$hasConfDir, final.mdl=$hasModelFile")

        return hasAmDir && hasConfDir && hasModelFile
    }

    /**
     * 获取模型状态描述
     */
    fun getModelStatus(): String {
        return when {
            isInitialized -> "引擎已就绪"
            isModelInExternalStorage() -> "模型已就绪"
            assetsModelManager.hasModelInAssets() -> "首次使用需解压（约5秒）"
            else -> "需要下载模型"
        }
    }

    /**
     * 初始化引擎
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true

        if (!isModelReady()) {
            Timber.tag(TAG).d("Model not ready, cannot initialize")
            return@withContext false
        }

        try {
            val modelPath = getActualModelPath()
            Timber.tag(TAG).d("Loading model from: $modelPath")

            model = Model(modelPath)
            recognizer = Recognizer(model, SAMPLE_RATE.toFloat())

            isInitialized = true
            Timber.tag(TAG).d("Vosk initialized successfully")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize Vosk")
            false
        }
    }

    /**
     * 开始录音识别
     */
    fun startRecording(): Boolean {
        if (!isInitialized) {
            Timber.tag(TAG).e("Engine not initialized")
            return false
        }

        if (isRecording) {
            Timber.tag(TAG).d("Already recording")
            return false
        }

        try {
            // 重置识别器
            recognizer?.let {
                it.setWords(false)
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Timber.tag(TAG).e("AudioRecord initialization failed")
                return false
            }

            isRecording = true
            audioRecord?.startRecording()

            scope.launch {
                val buffer = ShortArray(bufferSize / 2)

                while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        processAudio(buffer, read)
                    }
                }

                // 录音结束，获取最终结果
                if (isInitialized) {
                    val finalResult = recognizer?.finalResult
                    finalResult?.let { result ->
                        val text = extractText(result)
                        if (text.isNotEmpty()) {
                            _recognitionResult.emit(text)
                        }
                    }
                }
            }

            Timber.tag(TAG).d("Recording started")
            return true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start recording")
            isRecording = false
            return false
        }
    }

    /**
     * 处理音频数据
     */
    private suspend fun processAudio(buffer: ShortArray, length: Int) {
        if (!isInitialized) return

        try {
            recognizer?.let { recog ->
                // 转换为 byte array for Vosk (16-bit PCM)
                val byteBuffer = java.nio.ByteBuffer.allocate(length * 2)
                byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until length) {
                    byteBuffer.putShort(buffer[i])
                }

                if (recog.acceptWaveForm(byteBuffer.array(), byteBuffer.array().size)) {
                    // 有完整结果
                    val result = recog.result
                    val text = extractText(result)
                    if (text.isNotEmpty()) {
                        _recognitionResult.emit(text)
                    }
                } else {
                    // 部分结果
                    val partial = recog.partialResult
                    val text = extractText(partial)
                    _partialResult.emit(text)
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error processing audio")
        }
    }

    /**
     * 从 JSON 结果中提取文本
     */
    private fun extractText(jsonResult: String): String {
        return try {
            // 简单解析 JSON 结果
            // 格式: {"text": "识别结果"} 或 {"partial": "部分结果"}
            val textMatch = Regex(""""text"\s*:\s*"([^"]*)""").find(jsonResult)
            val partialMatch = Regex(""""partial"\s*:\s*"([^"]*)""").find(jsonResult)

            textMatch?.groupValues?.get(1)
                ?: partialMatch?.groupValues?.get(1)
                ?: ""
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to parse result: $jsonResult")
            ""
        }
    }

    /**
     * 停止录音
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
     * 释放资源
     */
    fun release() {
        stopRecording()
        scope.cancel()

        recognizer?.close()
        recognizer = null
        model?.close()
        model = null

        isInitialized = false
        Timber.tag(TAG).d("Vosk released")
    }

    /**
     * 检查是否正在录音
     */
    fun isRecording(): Boolean = isRecording
}
