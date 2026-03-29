package com.type4me.util

import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun getClipboardText(): String? {
        return try {
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
