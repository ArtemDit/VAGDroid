package com.vagdroid.proto

import com.vagdroid.Logger
import com.vagdroid.link.Link

/**
 * Высокоуровневая сессия с выбранным блоком управления.
 * Пробует подключиться по KW1281 (slow init, odd parity), при неудаче — KWP2000
 * (fast init / KW2027+ после slow init).
 */
class VagConnection(private val link: Link, val address: Int) {

    enum class Proto { KW1281, KW2000 }

    var proto: Proto? = null
        private set
    var protocolVersion: Int = 0
        private set

    var kw1281: Kw1281? = null
        private set
    var kw2000: Kw2000? = null
        private set

    var identLines: List<String> = emptyList()
    var componentId: String = ""
    var coding: Int = -1
    var wsc: Int = 0

    class ConnectException(msg: String) : Exception(msg)

    fun connect() {
        val k = Kw1281(link)
        val errors = mutableListOf<String>()

        // Попытка 1: slow init (KW1281)
        try {
            val result = k.connect(address)
            proto = Proto.KW1281
            protocolVersion = 1281
            kw1281 = k
            applyIdent(result.lines, result.componentId, result.coding, result.wsc)
            Logger.log("Подключено по KW1281, адрес ${"%02X".format(address)}")
            return
        } catch (e: Exception) {
            errors.add("KW1281: ${e.message}")
            try { k.endCommunication() } catch (_: Exception) {}
            Thread.sleep(1000)
        }

        // Попытка 2: slow init дал другой протокол (KW2027 и т.п.) → KWP2000 поверх
        try {
            val version = k.wakeUp(address)
            protocolVersion = version
            if (version >= 2000) {
                val k2 = Kw2000(link)
                k2.startCommunication(address)
                proto = Proto.KW2000
                kw2000 = k2
                val ident = k2.readIdent()
                applyIdent(ident.lines, ident.componentId, -1, 0)
                Logger.log("Подключено по KWP2000 (после slow init), адрес ${"%02X".format(address)}")
                return
            }
            errors.add("KW2027: неподдерживаемая версия $version")
        } catch (e: Exception) {
            errors.add("KW2000 slow: ${e.message}")
        }

        Thread.sleep(1000)

        // Попытка 3: fast init (KWP2000)
        try {
            val k2 = Kw2000(link)
            val version = k2.fastInit(address)
            protocolVersion = version
            proto = Proto.KW2000
            kw2000 = k2
            val ident = k2.readIdent()
            applyIdent(ident.lines, ident.componentId, -1, 0)
            Logger.log("Подключено по KWP2000 (fast init), адрес ${"%02X".format(address)}")
            return
        } catch (e: Exception) {
            errors.add("KW2000 fast: ${e.message}")
        }

        throw ConnectException(errors.joinToString("\n"))
    }

    private fun applyIdent(lines: List<String>, compId: String, cod: Int, w: Int) {
        identLines = lines
        componentId = compId.uppercase().replace(Regex("[^A-Z0-9]"), "")
        coding = cod
        wsc = w
    }

    fun refreshIdent() {
        kw1281?.let {
            val r = it.readIdent()
            applyIdent(r.lines, r.componentId, r.coding, r.wsc)
        }
    }

    fun close() {
        try { kw1281?.endCommunication() } catch (_: Exception) {}
        try { kw2000?.stopCommunication() } catch (_: Exception) {}
    }
}
