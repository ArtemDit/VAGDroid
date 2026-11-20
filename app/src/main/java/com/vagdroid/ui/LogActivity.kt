package com.vagdroid.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.vagdroid.Logger
import com.vagdroid.R

/** Журнал диагностики. */
class LogActivity : BaseActivity() {

    private lateinit var refresh: () -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvLog = findViewById<TextView>(R.id.tvLog)
        tvLog.text = Logger.text()

        refresh = { runOnUiThread { tvLog.text = Logger.text() } }
        Logger.addListener(refresh)

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val f = Logger.saveToFile()
            toast(if (f != null) "Сохранено: ${f.absolutePath}" else "Ошибка сохранения")
        }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            Logger.clear()
            tvLog.text = ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.removeListener(refresh)
    }
}
