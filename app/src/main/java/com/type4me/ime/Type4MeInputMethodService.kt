package com.type4me.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Type4MeInputMethodService : InputMethodService(), LifecycleOwner {

    private lateinit var lifecycleRegistry: LifecycleRegistry
    private var composeView: ComposeView? = null

    @Inject
    lateinit var viewModelFactory: IMEViewModel.Factory

    private lateinit var viewModel: IMEViewModel

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        viewModel = ViewModelProvider(this, viewModelFactory)[IMEViewModel::class.java]

        composeView = ComposeView(this).apply {
            setContent {
                KeyboardScreen(viewModel = viewModel)
            }
        }

        return composeView!!
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        composeView?.disposeComposition()
        composeView = null
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    fun sendEnter() {
        currentInputConnection?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND)
    }
}
