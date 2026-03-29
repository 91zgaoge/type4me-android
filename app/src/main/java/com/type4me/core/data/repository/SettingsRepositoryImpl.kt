package com.type4me.core.data.repository

import com.type4me.core.data.local.SettingsDataStore
import com.type4me.core.domain.model.Settings
import com.type4me.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override fun getSettings(): Flow<Settings> = dataStore.settingsFlow

    override suspend fun getSettingsSnapshot(): Settings = dataStore.getSettings()

    override suspend fun updateSettings(settings: Settings) {
        dataStore.updateSettings(settings)
    }

    override suspend fun updateSettings(transform: Settings.() -> Settings) {
        val current = getSettingsSnapshot()
        updateSettings(current.transform())
    }

    override suspend fun resetToDefaults() {
        dataStore.resetToDefaults()
    }
}
