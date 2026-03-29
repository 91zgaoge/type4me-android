package com.type4me.core.domain.repository

import com.type4me.core.domain.model.Settings
import kotlinx.coroutines.flow.Flow

/**
 * 设置仓库接口
 */
interface SettingsRepository {
    /**
     * 获取设置流
     */
    fun getSettings(): Flow<Settings>

    /**
     * 获取当前设置（同步）
     */
    suspend fun getSettingsSnapshot(): Settings

    /**
     * 更新设置
     */
    suspend fun updateSettings(settings: Settings)

    /**
     * 更新部分设置
     */
    suspend fun updateSettings(transform: Settings.() -> Settings)

    /**
     * 重置为默认设置
     */
    suspend fun resetToDefaults()
}
