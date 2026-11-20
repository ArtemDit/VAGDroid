package com.vagdroid.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vagdroid.Logger
import com.vagdroid.R
import com.vagdroid.Session
import com.vagdroid.diag.Modules
import com.vagdroid.proto.VagConnection

/** Выбор блока управления (функция 01). */
class SelectModuleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_module)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = SimpleAdapter { pos -> connectTo(Modules.all[pos].address) }
        recycler.adapter = adapter
        adapter.setItems(Modules.all.map { "%02X — %s".format(it.address, it.name) })
    }

    private fun connectTo(address: Int) {
        val link = Session.link ?: run { toast("Адаптер не подключён"); return }
        val progress = findViewById<ProgressBar>(R.id.progress)
        runOnUiThread { progress.visibility = View.VISIBLE }

        runIo(onError = { e ->
            runOnUiThread { progress.visibility = View.GONE }
            Session.closeConnection()
            toast("Блок %02X не ответил".format(address))
            Logger.log("Ошибка подключения к %02X: ${e.message}".format(address))
        }) {
            Session.closeConnection()
            Thread.sleep(300)
            val conn = VagConnection(link, address)
            conn.connect()
            Session.conn = conn
            Session.selectedAddress = address
            Session.identText = conn.identLines.joinToString("\n")
            Session.componentId = conn.componentId
            Session.coding = conn.coding
            Session.wsc = conn.wsc
            Logger.log("Идентификация ${"%02X".format(address)}: ${conn.identLines.joinToString(" | ")}")
            runOnUiThread {
                progress.visibility = View.GONE
                startActivity(Intent(this, ControllerActivity::class.java))
                finish()
            }
        }
    }
}
