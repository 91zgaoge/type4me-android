package com.type4me.core.domain.repository

import com.type4me.core.domain.model.ASREngine
import com.type4me.core.domain.model.ASREngineState
import com.type4me.core.domain.model.RecognitionResult
import kotlinx.coroutines.flow.Flow

/**
 * ASR 仓库接口
 * 自动处理引擎选择和降级逻辑
 */
interface ASRRepository {
    /**
     * 开始语音识别
     * 自动选择最佳可用引擎，在线失败时降级到离线
     */
    suspend fun startRecognition(): Flow<RecognitionResult>

    /**
     * 停止语音识别
     */
    fun stopRecognition()

    /**
     * 获取当前活跃的引擎
     */
    fun getActiveEngine(): ASREngine?

    /**
     * 检查离线引擎是否已就绪
     */
    fun isOfflineEngineReady(): Boolean

    /**
     * 获取离线引擎初始化状态
     */
    fun getOfflineEngineState(): Flow<ASREngineState>

    /**
     * 初始化离线引擎（异步）
     */
    suspend fun initializeOfflineEngine()
}
