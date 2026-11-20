package com.vagdroid.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.vagdroid.Logger
import com.vagdroid.R
import com.vagdroid.Session

/** Кодирование - 07 (только KW1281). */
class CodingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coding)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvCurrent = findViewById<TextView>(R.id.tvCurrent)
        val etCoding = findViewById<EditText>(R.id.etCoding)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        val current = Session.coding
        tvCurrent.text = if (current >= 0) "%05d".format(current) else "недоступна"

        findViewById<Button>(R.id.btnApply).setOnClickListener {
            val newCoding = etCoding.text.toString().toIntOrNull()
            if (newCoding == null) { toast("Введите число"); return@setOnClickListener }
            val wsc = getSharedPreferences("vagdroid", MODE_PRIVATE).getInt("wsc", 12345)

            AlertDialog.Builder(this)
                .setTitle("Кодирование")
                .setMessage("Записать кодировку $newCoding в блок %02X?".format(Session.conn?.address ?: 0))
                .setPositiveButton("Записать") { _, _ ->
                    runIo(onError = { tvResult.text = "Ошибка: ${it.message}" }) {
                        val conn = Session.conn ?: throw Exception("Блок не выбран")
                        val k = conn.kw1281 ?: throw Exception("Кодирование поддерживается только для KW1281")
                        val ok = k.setSoftwareCoding(newCoding, wsc)
                        if (ok) conn.refreshIdent()
                        Logger.log("Кодирование блока %02X: $newCoding -> %s".format(conn.address, if (ok) "OK" else "ОТКАЗ"))
                        runOnUiThread {
                            tvResult.text = if (ok) "Кодировка записана" else "Отказ блока (проверьте логин)"
                            if (ok) { tvCurrent.text = "%05d".format(newCoding); Session.coding = newCoding }
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}
