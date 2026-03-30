package com.type4me.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.type4me.core.domain.repository.ASRRepository
import com.type4me.core.domain.repository.LLMRepository
import com.type4me.core.domain.repository.SettingsRepository
import com.type4me.ime.ui.KeyboardScreen
import com.type4me.ime.viewmodel.IMEViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

class Type4MeInputMethodService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun asrRepository(): ASRRepository
        fun llmRepository(): LLMRepository
        fun settingsRepository(): SettingsRepository
    }

    private lateinit var lifecycleRegistry: LifecycleRegistry
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val _viewModelStore = ViewModelStore()
    private var composeView: ComposeView? = null

    private lateinit var viewModel: IMEViewModel

    override fun onCreate() {
        super.onCreate()
        Timber.d("Type4MeInputMethodService onCreate")
        lifecycleRegistry = LifecycleRegistry(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onCreateInputView(): View {
        Timber.d("Type4MeInputMethodService onCreateInputView")
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ServiceEntryPoint::class.java
            )

            val factory = IMEViewModel.Factory(
                service = this,
                asrRepository = entryPoint.asrRepository(),
                llmRepository = entryPoint.llmRepository(),
                settingsRepository = entryPoint.settingsRepository()
            )

            viewModel = ViewModelProvider(
                this,
                factory.asViewModelFactory()
            )[IMEViewModel::class.java]

            composeView = ComposeView(this).apply {
                setContent {
                    MaterialTheme {
                        KeyboardScreen(viewModel = viewModel)
                    }
                }
            }

            Timber.d("Type4MeInputMethodService view created successfully")
            composeView!!
        } catch (e: Exception) {
            Timber.e(e, "Failed to create input view")
            // Return a simple error view as fallback
            android.widget.TextView(this).apply {
                text = "Type4Me 加载失败: ${e.message}"
                setPadding(50, 50, 50, 50)
            }
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        Timber.d("Type4MeInputMethodService onWindowShown")
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        Timber.d("Type4MeInputMethodService onWindowHidden")
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("Type4MeInputMethodService onDestroy")
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        composeView?.disposeComposition()
        composeView = null
        _viewModelStore.clear()
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    fun sendEnter() {
        currentInputConnection?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND)
    }
}
