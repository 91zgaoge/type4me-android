package com.type4me.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.type4me.ime.ui.KeyboardScreen
import com.type4me.ime.viewmodel.IMEViewModel
import com.type4me.ime.viewmodel.asViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

@AndroidEntryPoint
class Type4MeInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner {

    private lateinit var lifecycleRegistry: LifecycleRegistry
    private val _viewModelStore = ViewModelStore()
    private var composeView: ComposeView? = null

    private lateinit var viewModelFactory: IMEViewModel.Factory

    private lateinit var viewModel: IMEViewModel

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        // Get factory using EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            IMEViewModelFactoryEntryPoint::class.java
        )
        viewModelFactory = entryPoint.imeViewModelFactory()
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        viewModel = ViewModelProvider(
            this,
            viewModelFactory.asViewModelFactory(this)
        )[IMEViewModel::class.java]

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
        _viewModelStore.clear()
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    fun sendEnter() {
        currentInputConnection?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND)
    }
}
