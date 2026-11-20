package com.vagdroid.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.vagdroid.R
import com.vagdroid.Session

/** Экран идентификации выбранного блока управления. */
class ControllerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvIdent = findViewById<TextView>(R.id.tvIdent)
        val btnOk = findViewById<Button>(R.id.btnOk)
        val progress = findViewById<ProgressBar>(R.id.progress)

        val conn = Session.conn
        if (conn == null) { finish(); return }

        tvIdent.text = buildString {
            append("Адрес: %02X\n".format(conn.address))
            append("Протокол: ${conn.proto} (версия ${conn.protocolVersion})\n\n")
            append(Session.identText.ifBlank { "Идентификация недоступна" })
            if (conn.coding >= 0) append("\n\nКодировка ПО: %05d\n".format(conn.coding))
            if (conn.wsc > 0) append("Код мастерской (WSC): %05d".format(conn.wsc))
            append("\n\nКомпонент: ${Session.componentId}")
        }
        progress.visibility = View.GONE

        btnOk.setOnClickListener {
            runIo {
                try { conn.refreshIdent() } catch (_: Exception) {}
                runOnUiThread { finish() }
            }
        }
    }
}
