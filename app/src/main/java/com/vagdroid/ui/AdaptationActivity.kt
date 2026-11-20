package com.vagdroid.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.vagdroid.Logger
import com.vagdroid.R
import com.vagdroid.Session

/** Адаптация - 10 (каналы 0-99: чтение / тест / сохранение). */
class AdaptationActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adaptation)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etChannel = findViewById<EditText>(R.id.etChannel)
        val etValue = findViewById<EditText>(R.id.etValue)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        fun channel(): Int = etChannel.text.toString().toIntOrNull() ?: 0
        fun value(): Int = etValue.text.toString().toIntOrNull() ?: 0
        fun wsc(): Int = getSharedPreferences("vagdroid", MODE_PRIVATE).getInt("wsc", 12345)

        findViewById<Button>(R.id.btnRead).setOnClickListener {
            runIo(onError = { tvResult.text = "Ошибка: ${it.message}" }) {
                val k = Session.conn?.kw1281 ?: throw Exception("Доступно только для KW1281")
                val r = k.adaptationRead(channel())
                Logger.log("Адаптация чтение канал ${channel()}: ${r.raw.joinToString(" ") { b -> "%02X".format(b) }}")
                runOnUiThread {
                    tvResult.text = if (r.ok) "Канал ${channel()}: значение ${r.channelValue}"
                                    else "Канал ${channel()}: недоступен или требуется логин"
                }
            }
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            runIo(onError = { tvResult.text = "Ошибка: ${it.message}" }) {
                val k = Session.conn?.kw1281 ?: throw Exception("Доступно только для KW1281")
                val r = k.adaptationTest(channel(), value())
                runOnUiThread {
                    tvResult.text = if (r.ok) "Тестовое значение ${value()} применено" else "Отказ"
                }
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            runIo(onError = { tvResult.text = "Ошибка: ${it.message}" }) {
                val k = Session.conn?.kw1281 ?: throw Exception("Доступно только для KW1281")
                val r = k.adaptationSave(channel(), value(), wsc())
                Logger.log("Адаптация сохранение канал ${channel()} = ${value()}: ok=${r.ok}")
                runOnUiThread {
                    tvResult.text = if (r.ok) "Значение ${value()} сохранено в канале ${channel()}" else "Отказ"
                }
            }
        }
    }
}
