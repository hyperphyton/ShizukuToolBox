package com.narezany.shizukutoolbox.core

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Простые всплывающие сообщения: экраны кидают текст, корневой Scaffold показывает snackbar.
 */
object Ui {

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun toast(text: String) {
        messages.tryEmit(text)
    }
}
