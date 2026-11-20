package com.vagdroid.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import com.vagdroid.Logger
import com.vagdroid.R
import com.vagdroid.Session
import com.vagdroid.diag.LabelStore
import com.vagdroid.proto.VagConnection

/** Измеряемые группы (функция 08) — с подписями из label-файлов. */
open class BlocksActivity : BaseActivity() {

    protected lateinit var etGroup: EditText
    protected lateinit var tvValues: TextView
    protected lateinit var poll: ToggleButton
    private var labels: Map<Int, LabelStore.GroupLabels> = emptyMap()
    private val pollHandler = Handler(Looper.getMainLooper())
    @Volatile private var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(if (useBasicLayout()) R.layout.activity_basic else R.layout.activity_blocks)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etGroup = findViewById(R.id.etGroup)
        tvValues = findViewById(R.id.tvValues)
        poll = findViewById(R.id.tbtnPoll)

        findViewById<Button>(R.id.btnRead).setOnClickListener { readOnce() }
        findViewById<Button>(R.id.btnPrev).setOnClickListener { stepGroup(-1) }
        findViewById<Button>(R.id.btnNext).setOnClickListener { stepGroup(1) }

        poll.setOnCheckedChangeListener { _, isOn ->
            polling = isOn
            if (isOn) schedulePoll()
        }

        loadLabels()
    }

    protected open fun useBasicLayout(): Boolean = false

    protected open fun readGroup(group: Int): String {
        val conn = Session.conn ?: throw Exception("Блок не выбран")
        return when (conn.proto) {
            VagConnection.Proto.KW1281 -> {
                val k = conn.kw1281 ?: throw Exception("Нет сессии")
                if (useBasicLayout()) formatResult(k.basicSetting(group), group)
                else formatResult(k.readGroup(group), group)
            }
            VagConnection.Proto.KW2000 -> {
                val k = conn.kw2000 ?: throw Exception("Нет сессии")
                val raw = k.readLocalData(group)
                "Группа %d (сырые данные):\n%s".format(group,
                    raw.joinToString(" ") { "%02X".format(it) })
            }
            null -> throw Exception("Нет соединения")
        }
    }

    private fun formatResult(r: com.vagdroid.proto.Kw1281.GroupResult, group: Int): String {
        val gl = labels[group]
        val sb = StringBuilder()
        sb.append("Группа %03d".format(group))
        if (gl != null && gl.comment.isNotBlank()) sb.append(" — ${gl.comment}")
        sb.append('\n')
        if (r.values.isEmpty() && r.text.isNotBlank()) {
            sb.append(r.text)
        }
        for ((i, v) in r.values.withIndex()) {
            val name = gl?.names?.getOrNull(i) ?: "Значение ${i + 1}"
            val spec = gl?.specs?.getOrNull(i)
            sb.append("%-22s %s".format("$name:", v.text()))
            if (spec != null && spec.isNotBlank()) sb.append("   [$spec]")
            sb.append('\n')
        }
        return sb.toString().trimEnd()
    }

    private fun loadLabels() {
        val ctx = this
        runIo {
            try {
                val file = LabelStore.findForComponent(ctx, Session.componentId)
                if (file != null) {
                    labels = LabelStore.load(ctx, file)
                    Logger.log("Label-файл: $file (${labels.size} групп)")
                } else {
                    Logger.log("Label-файл для ${Session.componentId} не найден")
                }
            } catch (e: Exception) {
                Logger.log("Ошибка загрузки labels: ${e.message}")
            }
        }
    }

    protected fun currentGroup(): Int = etGroup.text.toString().toIntOrNull() ?: 1

    private fun stepGroup(delta: Int) {
        val g = (currentGroup() + delta).coerceIn(1, 255)
        etGroup.setText(g.toString())
        readOnce()
    }

    protected open fun readOnce() {
        val group = currentGroup()
        runIo(onError = { e ->
            stopPolling()
            toast(e.message ?: "Ошибка чтения")
        }) {
            val text = readGroup(group)
            Logger.log(text)
            runOnUiThread { tvValues.text = text }
        }
    }

    private fun schedulePoll() {
        pollHandler.postDelayed({
            if (!polling) return@postDelayed
            readOnce()
            schedulePoll()
        }, 1000)
    }

    protected fun stopPolling() {
        polling = false
        runOnUiThread { poll.isChecked = false }
    }

    override fun onPause() {
        super.onPause()
        polling = false
        poll.isChecked = false
    }
}
