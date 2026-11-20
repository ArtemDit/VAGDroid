package com.vagdroid.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.vagdroid.Logger
import com.vagdroid.R
import com.vagdroid.Session

/** Тест исполнителей (функция 03). */
class OutputTestsActivity : BaseActivity() {

    private var testByte = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_output_tests)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvCurrent = findViewById<TextView>(R.id.tvCurrent)

        findViewById<Button>(R.id.btnNextTest).setOnClickListener {
            runIo(onError = { toast(it.message ?: "Ошибка") }) {
                val conn = Session.conn ?: throw Exception("Блок не выбран")
                val k = conn.kw1281 ?: throw Exception("Доступно только для KW1281")
                val r = k.actuatorTest(testByte)
                Logger.log("Тест 0x%02X: %s".format(testByte, r.text))
                testByte = (testByte + 1) and 0xFF
                runOnUiThread { tvCurrent.text = r.text.ifBlank { "Тест запущен (0x%02X)".format(testByte - 1) } }
            }
        }

        findViewById<Button>(R.id.btnStopTest).setOnClickListener {
            runIo(onError = { }) {
                val conn = Session.conn ?: return@runIo
                val k = conn.kw1281 ?: return@runIo
                try { k.actuatorTest(0x00) } catch (_: Exception) {}
                runOnUiThread { tvCurrent.text = "Тест остановлен" }
            }
        }
    }
}
