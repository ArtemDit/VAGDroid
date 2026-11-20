package com.vagdroid.ui

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Базовая активность: выполнение кода в потоке связи + прогресс. */
open class BaseActivity : AppCompatActivity() {

    protected val uiHandler = Handler(Looper.getMainLooper())

    protected fun toast(msg: String) {
        uiHandler.post { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
    }

    protected fun runIo(
        onError: ((Exception) -> Unit)? = null,
        block: () -> Unit
    ) {
        Thread {
            try {
                block()
            } catch (e: Exception) {
                runOnUiThread { onError?.invoke(e) ?: toast("Ошибка: ${e.message}") }
            }
        }.start()
    }
}
