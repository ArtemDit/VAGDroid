package com.vagdroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.vagdroid.diag.Modules
import com.vagdroid.ui.*

class MainActivity : BaseActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvModule: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Logger.context = applicationContext
        supportActionBar?.hide()

        tvStatus = findViewById(R.id.tvStatus)
        tvModule = findViewById(R.id.tvModule)

        findViewById<Button>(R.id.btnSelect).setOnClickListener {
            if (requireAdapter()) startActivity(Intent(this, SelectModuleActivity::class.java))
        }
        findViewById<Button>(R.id.btnFaults).setOnClickListener { open(FaultCodesActivity::class.java) }
        findViewById<Button>(R.id.btnBlocks).setOnClickListener { open(BlocksActivity::class.java) }
        findViewById<Button>(R.id.btnOutputTests).setOnClickListener { open(OutputTestsActivity::class.java) }
        findViewById<Button>(R.id.btnBasic).setOnClickListener { open(BasicSettingsActivity::class.java) }
        findViewById<Button>(R.id.btnLogin).setOnClickListener { open(LoginActivity::class.java) }
        findViewById<Button>(R.id.btnAdaptation).setOnClickListener { open(AdaptationActivity::class.java) }
        findViewById<Button>(R.id.btnCoding).setOnClickListener { open(CodingActivity::class.java) }
        findViewById<Button>(R.id.btnAutoscan).setOnClickListener {
            if (requireAdapter()) startActivity(Intent(this, AutoscanActivity::class.java))
        }
        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            startActivity(Intent(this, ConnectActivity::class.java))
        }
        findViewById<Button>(R.id.btnLog).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnAbout).setOnClickListener { showAbout() }
    }

    private fun <T> open(cls: Class<T>) {
        if (!requireAdapter()) return
        if (Session.conn == null) {
            toast("Сначала выберите блок управления (01)")
            return
        }
        startActivity(Intent(this, cls))
    }

    private fun requireAdapter(): Boolean {
        if (!Session.isAdapterConnected()) {
            toast("Сначала подключите адаптер")
            startActivity(Intent(this, ConnectActivity::class.java))
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        val link = Session.link
        tvStatus.text = if (link != null && link.isOpen())
            "Адаптер: ${link.name}" else "Адаптер не подключен"
        val conn = Session.conn
        tvModule.text = if (conn != null)
            "Блок: ${"%02X".format(conn.address)} — ${Modules.nameOf(conn.address)} (${conn.proto})"
        else "Блок не выбран"
    }

    override fun onDestroy() {
        super.onDestroy()
        // Соединение закрывается только явно из настроек/подключения
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("VAGDroid 1.0")
            .setMessage(
                "Диагностика автомобилей VAG (VW / Audi / Seat / Skoda) по K-line.\n\n" +
                "Адаптеры: KKL VAG-COM 409.1 (USB: CH340/FTDI/CP210x, Bluetooth SPP).\n\n" +
                "Функции: выбор блока, коды неисправностей, измеряемые группы, " +
                "тест исполнителей, базовые установки, логин, адаптация, кодирование, автосканирование.\n\n" +
                "Протоколы: KW1281, KWP2000 (ISO 14230).\n" +
                "Порты логики kw1281test (github.com/gmenounos/kw1281test).\n\n" +
                "ВНИМАНИЕ: используйте на свой риск. Неверные кодирование/адаптация " +
                "могут вывести оборудование из строя."
            )
            .setPositiveButton("OK", null)
            .show()
    }
}
