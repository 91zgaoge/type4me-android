package com.type4me.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import timber.log.Timber

/**
 * 简化的输入法服务 - 先确保基本功能正常
 */
class Type4MeInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "Type4MeIME"
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("onCreate called")
    }

    override fun onCreateInputView(): View {
        Timber.tag(TAG).d("onCreateInputView called")

        // 创建最简单的 UI - 一个垂直布局，包含标题和按钮
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                400 // 固定高度 400px
            )
            setPadding(20, 20, 20, 20)
        }

        // 标题
        val titleView = TextView(this).apply {
            text = "Type4Me 语音输入"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(titleView)

        // 录音按钮
        val micButton = Button(this).apply {
            text = "🎤 点击录音"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                Timber.tag(TAG).d("Mic button clicked")
                currentInputConnection?.commitText("测试语音输入", 1)
            }
        }
        layout.addView(micButton)

        // 上屏按钮
        val commitButton = Button(this).apply {
            text = "上屏测试文字"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
            }
            setOnClickListener {
                Timber.tag(TAG).d("Commit button clicked")
                currentInputConnection?.commitText("Hello Type4Me!", 1)
            }
        }
        layout.addView(commitButton)

        Timber.tag(TAG).d("Input view created successfully")
        return layout
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Timber.tag(TAG).d("onStartInput called, restarting=$restarting")
    }

    override fun onWindowShown() {
        super.onWindowShown()
        Timber.tag(TAG).d("onWindowShown called")
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        Timber.tag(TAG).d("onWindowHidden called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("onDestroy called")
    }
}
