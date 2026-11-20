package com.vagdroid

import android.content.Context
import com.vagdroid.link.Link
import com.vagdroid.proto.VagConnection
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Глобальная сессия приложения (текущий адаптер и соединение с блоком). */
object Session {
    var link: Link? = null
    var conn: VagConnection? = null
    var selectedAddress: Int = -1
    var selectedName: String = ""
    var identText: String = ""
    var componentId: String = ""
    var coding: Int = -1
    var wsc: Int = 0
    var protocolName: String = ""

    fun closeConnection() {
        try { conn?.close() } catch (_: Exception) {}
        conn = null
        selectedAddress = -1
        identText = ""
        componentId = ""
        coding = -1
    }

    fun disconnectAll() {
        closeConnection()
        try { link?.close() } catch (_: Exception) {}
        link = null
    }

    fun isAdapterConnected(): Boolean = link?.isOpen() == true
}

/** Журнал диагностики в памяти + запись в файл. */
object Logger {
    private val sb = StringBuilder()
    private val listeners = mutableListOf<() -> Unit>()
    @Volatile var context: Context? = null

    fun log(msg: String) {
        synchronized(sb) {
            sb.append(msg).append('\n')
            if (sb.length > 200_000) sb.delete(0, sb.length - 150_000)
        }
        listeners.forEach { try { it() } catch (_: Exception) {} }
    }

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }

    fun text(): String = synchronized(sb) { sb.toString() }

    fun clear() { synchronized(sb) { sb.setLength(0) } }

    fun saveToFile(): File? {
        val ctx = context ?: return null
        val dir = File(ctx.getExternalFilesDir(null), "logs")
        if (!dir.exists()) dir.mkdirs()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val f = File(dir, "vagdroid_$ts.txt")
        f.writeText(text())
        return f
    }
}
