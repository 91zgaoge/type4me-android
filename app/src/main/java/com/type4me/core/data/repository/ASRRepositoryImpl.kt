package com.type4me.core.data.repository

import com.type4me.core.data.remote.GoogleSpeechRecognizer
import com.type4me.core.domain.model.ASREngine
import com.type4me.core.domain.model.ASREngineState
import com.type4me.core.domain.model.RecognitionResult
import com.type4me.core.domain.model.Settings
import com.type4me.core.domain.repository.ASRRepository
import com.type4me.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ASRRepositoryImpl @Inject constructor(
    private val googleRecognizer: GoogleSpeechRecognizer,
    private val settingsRepository: SettingsRepository
) : ASRRepository {

    private var activeEngine: ASREngine? = null
    private val _offlineEngineState = MutableStateFlow<ASREngineState>(ASREngineState.Idle)
    private val offlineEngineState = _offlineEngineState.asStateFlow()

    override suspend fun startRecognition(): Flow<RecognitionResult> = flow {
        val settings = settingsRepository.getSettingsSnapshot()

        if (settings.preferOffline && isOfflineEngineReady()) {
            // 优先使用离线引擎
            activeEngine = ASREngine.SHERPA_ONNX_LOCAL
            emitAll(startOfflineRecognition())
        } else {
            // 使用在线引擎，失败时降级
            try {
                activeEngine = ASREngine.GOOGLE_ONLINE
                googleRecognizer.startRecognition()
                    .catch { e ->
                        if (e is IOException && isOfflineEngineReady()) {
                            Timber.w("在线识别失败，降级到离线引擎")
                            activeEngine = ASREngine.SHERPA_ONNX_LOCAL
                            emitAll(startOfflineRecognition())
                        } else {
                            throw e
                        }
                    }
                    .collect { result ->
                        emit(result)
                    }
            } catch (e: Exception) {
                Timber.e(e, "ASR 识别失败")
                throw e
            }
        }
    }

    private fun startOfflineRecognition(): Flow<RecognitionResult> = flow {
        // TODO: 实现 SherpaOnnx 本地识别
        throw NotImplementedError("离线引擎尚未实现")
    }

    override fun stopRecognition() {
        googleRecognizer.stopListening()
        activeEngine = null
    }

    override fun getActiveEngine(): ASREngine? = activeEngine

    override fun isOfflineEngineReady(): Boolean {
        // TODO: 检查 SherpaOnnx 模型是否已加载
        return false
    }

    override fun getOfflineEngineState(): Flow<ASREngineState> = offlineEngineState

    override suspend fun initializeOfflineEngine() {
        _offlineEngineState.value = ASREngineState.Initializing
        try {
            // TODO: 下载并加载 SherpaOnnx 模型
            _offlineEngineState.value = ASREngineState.Error(
                NotImplementedError("离线引擎尚未实现")
            )
        } catch (e: Exception) {
            Timber.e(e, "离线引擎初始化失败")
            _offlineEngineState.value = ASREngineState.Error(e)
        }
    }
}