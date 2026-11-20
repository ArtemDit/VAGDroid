package com.vagdroid.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.vagdroid.Logger
import com.vagdroid.R
import com.vagdroid.Session

/** Логин - 11 (SecurityAccess). */
class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etLogin = findViewById<EditText>(R.id.etLogin)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        findViewById<Button>(R.id.btnDoLogin).setOnClickListener {
            val code = etLogin.text.toString().toIntOrNull()
            if (code == null) { toast("Введите числовой код"); return@setOnClickListener }
            val prefs = getSharedPreferences("vagdroid", MODE_PRIVATE)
            val wsc = prefs.getInt("wsc", 12345)
            runIo(onError = { tvResult.text = "Ошибка: ${it.message}" }) {
                val conn = Session.conn ?: throw Exception("Блок не выбран")
                val ok = when (conn.proto) {
                    com.vagdroid.proto.VagConnection.Proto.KW1281 ->
                        conn.kw1281?.login(code, wsc) == true
                    com.vagdroid.proto.VagConnection.Proto.KW2000 ->
                        conn.kw2000?.securityAccess(1, code) == true
                    null -> false
                }
                Logger.log("Логин $code в блок %02X: %s".format(conn.address, if (ok) "принят" else "ОТКАЗ"))
                runOnUiThread {
                    tvResult.text = if (ok) "Логин принят" else "Логин отклонён блоком"
                }
            }
        }
    }
}
