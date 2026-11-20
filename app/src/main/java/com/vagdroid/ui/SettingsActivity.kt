package com.vagdroid.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.vagdroid.BuildConfig
import com.vagdroid.R
import com.vagdroid.Session

/** Настройки: WSC, MAC BT-адаптера. Сохраняются автоматически. */
class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences("vagdroid", MODE_PRIVATE)
        val etWsc = findViewById<EditText>(R.id.etWsc)
        val etBt = findViewById<EditText>(R.id.etBtDevice)
        val spBaud = findViewById<Spinner>(R.id.spBaud)

        etWsc.setText(prefs.getInt("wsc", 12345).toString())
        etBt.setText(prefs.getString("bt_mac", ""))
        spBaud.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("10400", "9600"))

        findViewById<TextView>(R.id.tvInfo).text =
            "VAGDroid v${BuildConfig.VERSION_NAME}\nАдаптер: ${Session.link?.name ?: "не подключён"}"
    }

    override fun onPause() {
        super.onPause()
        val prefs = getSharedPreferences("vagdroid", MODE_PRIVATE)
        prefs.edit()
            .putInt("wsc", findViewById<EditText>(R.id.etWsc).text.toString().toIntOrNull() ?: 12345)
            .putString("bt_mac", findViewById<EditText>(R.id.etBtDevice).text.toString())
            .apply()
    }
}
