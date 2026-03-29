package com.type4me.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.type4me.core.domain.model.Settings
import com.type4me.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Settings()
        )

    fun setPreferOffline(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { copy(preferOffline = value) }
        }
    }

    fun setAutoDownloadModel(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { copy(autoDownloadModel = value) }
        }
    }

    fun setAutoOptimize(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { copy(autoOptimize = value) }
        }
    }

    fun setPrimaryProvider(provider: com.type4me.core.domain.model.LLMProvider) {
        viewModelScope.launch {
            settingsRepository.updateSettings { copy(primaryProvider = provider) }
        }
    }

    fun setOpenAiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { copy(openAiKey = key) }
        }
    }

    fun setGeminiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { copy(geminiKey = key) }
        }
    }

    fun setClaudeKey(key: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { copy(claudeKey = key) }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }
}