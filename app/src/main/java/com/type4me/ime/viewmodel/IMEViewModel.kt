package com.type4me.ime.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.type4me.core.domain.model.ASREngine
import com.type4me.core.domain.model.DefaultPrompts
import com.type4me.core.domain.model.PromptTemplate
import com.type4me.core.domain.model.RecognitionResult
import com.type4me.core.domain.repository.ASRRepository
import com.type4me.core.domain.repository.LLMRepository
import com.type4me.core.domain.repository.SettingsRepository
import com.type4me.ime.Type4MeInputMethodService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class IMEViewModel(
    private val service: Type4MeInputMethodService,
    private val asrRepository: ASRRepository,
    private val llmRepository: LLMRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    sealed class State {
        data object Idle : State()
        data class Recording(val engine: ASREngine) : State()
        data class Recognizing(val partialText: String) : State()
        data class Result(val text: String, val isOptimized: Boolean = false) : State()
        data class Optimizing(val originalText: String) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    private val _selectedPrompt = MutableStateFlow<PromptTemplate>(DefaultPrompts.POLISH)
    val selectedPrompt: StateFlow<PromptTemplate> = _selectedPrompt.asStateFlow()

    fun startRecording() {
        viewModelScope.launch {
            _state.value = State.Recording(ASREngine.GOOGLE_ONLINE)
            try {
                asrRepository.startRecognition().collect { result ->
                    if (result.isFinal) {
                        _currentText.value = result.text
                        _state.value = State.Result(result.text)
                    } else {
                        _state.value = State.Recognizing(result.text)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Recording failed")
                _state.value = State.Error(e.message ?: "识别失败")
            }
        }
    }

    fun stopRecording() {
        asrRepository.stopRecognition()
    }

    fun commitText(text: String, commitCallback: (String) -> Unit) {
        commitCallback(text)
        reset()
    }

    fun optimizeText(text: String) {
        viewModelScope.launch {
            _state.value = State.Optimizing(text)
            val clipboard = "" // TODO: Get clipboard content
            val result = llmRepository.optimizeText(text, clipboard, _selectedPrompt.value)
            result.onSuccess { optimized ->
                _state.value = State.Result(optimized, isOptimized = true)
            }.onFailure { error ->
                _state.value = State.Error(error.message ?: "优化失败")
            }
        }
    }

    fun selectPrompt(prompt: PromptTemplate) {
        _selectedPrompt.value = prompt
    }

    fun reset() {
        _state.value = State.Idle
        _currentText.value = ""
    }

    class Factory(
        private val service: Type4MeInputMethodService,
        private val asrRepository: ASRRepository,
        private val llmRepository: LLMRepository,
        private val settingsRepository: SettingsRepository
    ) {
        fun create(): IMEViewModel {
            return IMEViewModel(service, asrRepository, llmRepository, settingsRepository)
        }

        fun asViewModelFactory(): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return create() as T
                }
            }
        }
    }
}
