package com.vagdroid.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vagdroid.Logger
import com.vagdroid.R
import com.vagdroid.Session
import com.vagdroid.diag.Dtc

/** Коды неисправностей (функция 02): чтение и стирание. */
class FaultCodesActivity : BaseActivity() {

    private lateinit var adapter: SimpleAdapter
    private var faults: List<Any> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fault_codes)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = SimpleAdapter()
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnClear).setOnClickListener { clearCodes() }

        readCodes()
    }

    private fun readCodes() {
        val conn = Session.conn ?: run { toast("Блок не выбран"); finish(); return }
        runIo {
            val lines = mutableListOf<String>()
            when (conn.proto) {
                com.vagdroid.proto.VagConnection.Proto.KW1281 -> {
                    val k = conn.kw1281 ?: throw Exception("Нет сессии KW1281")
                    val fs = k.readFaultCodes()
                    faults = fs
                    if (fs.isEmpty()) lines.add("Кодов неисправностей не обнаружено")
                    for (f in fs) {
                        val desc = Dtc.vagDescription(f.dtc) ?: "Описание отсутствует в базе"
                        val sporadic = if (f.sporadic) " (спорадически)" else ""
                        lines.add("%s — %s%s\nСтатус: %s — %s".format(
                            f.code5(), desc, sporadic, f.statusStr(),
                            Dtc.statusDescription(f.status)))
                    }
                }
                com.vagdroid.proto.VagConnection.Proto.KW2000 -> {
                    val k = conn.kw2000 ?: throw Exception("Нет сессии KWP2000")
                    val fs = k.readFaultCodes()
                    faults = fs
                    if (fs.isEmpty()) lines.add("Кодов неисправностей не обнаружено")
                    for (f in fs) {
                        val code = f.dtcStr()
                        val desc = Dtc.obdDescription(code) ?: "Описание отсутствует в базе"
                        lines.add("%s — %s".format(code, desc))
                    }
                }
                null -> lines.add("Нет соединения")
            }
            runOnUiThread { adapter.setItems(lines) }
        }
    }

    private fun clearCodes() {
        val conn = Session.conn ?: return
        AlertDialog.Builder(this)
            .setTitle("Стирание кодов")
            .setMessage("Стереть все коды неисправностей в блоке %02X?".format(conn.address))
            .setPositiveButton("Стереть") { _, _ ->
                runIo {
                    val msg: String
                    when (conn.proto) {
                        com.vagdroid.proto.VagConnection.Proto.KW1281 -> {
                            val k = conn.kw1281 ?: throw Exception("Нет сессии")
                            val remaining = k.clearFaultCodes()
                            msg = if (remaining.isEmpty()) "Коды стёрты"
                                  else "Стёрто. Осталось кодов: ${remaining.size}"
                        }
                        com.vagdroid.proto.VagConnection.Proto.KW2000 -> {
                            conn.kw2000?.clearFaultCodes()
                            msg = "Команда стирания отправлена"
                        }
                        null -> msg = "Нет соединения"
                    }
                    Logger.log("Стирание кодов %02X: $msg".format(conn.address))
                    runOnUiThread { toast(msg); readCodes() }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
