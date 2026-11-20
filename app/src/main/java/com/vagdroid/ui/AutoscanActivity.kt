package com.vagdroid.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.vagdroid.Logger
import com.vagdroid.R
import com.vagdroid.Session
import com.vagdroid.diag.Autoscan
import com.vagdroid.diag.Dtc
import com.vagdroid.diag.Modules
import com.vagdroid.proto.VagConnection

/** Автосканирование всех блоков выбранного кузова. */
class AutoscanActivity : BaseActivity() {

    companion object {
        var selectedChassis: Autoscan.Chassis? = null
    }

    private lateinit var tvLog: TextView
    private lateinit var tvProgress: TextView
    @Volatile private var stopRequested = false
    @Volatile private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autoscan)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tvLog = findViewById(R.id.tvLog)
        tvProgress = findViewById(R.id.tvProgress)
        val btnChassis = findViewById<Button>(R.id.btnChassis)

        if (selectedChassis == null) selectedChassis = Autoscan.defaultAllLong(this)
        btnChassis.text = "Кузов: ${selectedChassis!!.code} — ${selectedChassis!!.name}"

        btnChassis.setOnClickListener {
            startActivityForResult(Intent(this, ChassisActivity::class.java), 1)
        }
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (running) { toast("Сканирование уже идёт"); return@setOnClickListener }
            startScan()
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopRequested = true }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        selectedChassis?.let {
            findViewById<Button>(R.id.btnChassis).text = "Кузов: ${it.code} — ${it.name}"
        }
    }

    private fun startScan() {
        val link = Session.link ?: run { toast("Адаптер не подключён"); return }
        val chassis = selectedChassis!!
        stopRequested = false
        running = true
        Logger.log("=== Автосканирование ${chassis.code} — ${chassis.name} ===")

        runIo(onError = { e ->
            running = false
            appendLog("\nОШИБКА: ${e.message}")
            Logger.log("Автоскан: ошибка ${e.message}")
        }) {
            appendLog("Автосканирование: ${chassis.code} — ${chassis.name}\n")
            var found = 0
            for (addr in chassis.addresses) {
                if (stopRequested) break
                runOnUiThread { tvProgress.text = "Опрос адреса %02X — %s".format(addr, Modules.nameOf(addr)) }
                try {
                    val conn = VagConnection(link, addr)
                    conn.connect()
                    found++
                    val sb = StringBuilder()
                    sb.append("\n=== %02X — %s ===\n".format(addr, Modules.nameOf(addr)))
                    for (l in conn.identLines) sb.append(l).append('\n')
                    if (conn.coding >= 0) sb.append("Кодировка ПО: %05d\n".format(conn.coding))
                    try {
                        when (conn.proto) {
                            VagConnection.Proto.KW1281 -> {
                                val fs = conn.kw1281?.readFaultCodes() ?: emptyList()
                                if (fs.isEmpty()) sb.append("Кодов неисправностей нет\n")
                                for (f in fs) {
                                    val desc = Dtc.vagDescription(f.dtc) ?: "?"
                                    sb.append("%s — %s (%s)\n".format(f.code5(), desc, f.statusStr()))
                                }
                            }
                            VagConnection.Proto.KW2000 -> {
                                val fs = conn.kw2000?.readFaultCodes() ?: emptyList()
                                if (fs.isEmpty()) sb.append("Кодов неисправностей нет\n")
                                for (f in fs) sb.append("%s\n".format(f.dtcStr()))
                            }
                            null -> {}
                        }
                    } catch (e: Exception) {
                        sb.append("Чтение кодов: ${e.message}\n")
                    }
                    appendLog(sb.toString())
                    conn.close()
                    Thread.sleep(1000)
                } catch (_: Exception) {
                    // блок не ответил — тихо идём дальше
                    Thread.sleep(300)
                }
            }
            appendLog("\nГотово. Найдено блоков: $found\n")
            Logger.log("=== Автоскан завершён, найдено блоков: $found ===")
            running = false
        }
    }

    private fun appendLog(s: String) {
        runOnUiThread { tvLog.append(s) }
    }

    override fun onPause() {
        super.onPause()
        stopRequested = true
    }
}
