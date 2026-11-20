package com.vagdroid.ui

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vagdroid.R
import com.vagdroid.diag.Autoscan

/** Выбор кузова для автосканирования. */
class ChassisActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chassis)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val chassis = Autoscan.load(this)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = SimpleAdapter { pos ->
            AutoscanActivity.selectedChassis = chassis[pos]
            finish()
        }
        recycler.adapter = adapter
        adapter.setItems(chassis.map { "${it.code} — ${it.name} (${it.addresses.size} блоков)" })
    }
}
