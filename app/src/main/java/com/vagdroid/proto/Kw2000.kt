package com.vagdroid.proto

import com.vagdroid.Logger
import com.vagdroid.link.Link

/**
 * KWP2000 (ISO 14230) по K-line.
 * Кадр: [len][tgt][src][data...][checksum], checksum = сумма всех байт кроме len.
 */
class Kw2000(private val link: Link) {

    class ProtoException(msg: String) : Exception(msg)

    var target: Int = 0
        private set
    private val source = 0xF1

    private var baud = 10400

    /** Fast init: 25 мс BREAK, пауза до Twup=50 мс, StartCommunication. */
    fun fastInit(address: Int): Int {
        if (!link.supportsBreak) throw ProtoException("Адаптер не поддерживает BREAK — fast init невозможен")
        target = address
        link.setBaud(baud)
        link.setBreak(false)
        Thread.sleep(300)

        val t0 = System.currentTimeMillis()
        link.setBreak(true)
        while (System.currentTimeMillis() - t0 < 25) { /* busy wait */ }
        link.setBreak(false)
        link.purge()
        while (System.currentTimeMillis() - t0 < 50) { /* busy wait до Twup */ }

        val resp = sendReceive(byteArrayOf(0x81.toByte()), needChecksumHeader = true, rawStart = true)
        // Ожидаем 0xC1 + 2 байта ключевых слов
        if (resp.size >= 3 && (resp[0].toInt() and 0xFF) == 0xC1) {
            val kwLsb = resp[1].toInt() and 0xFF
            val kwMsb = resp[2].toInt() and 0xFF
            val version = ((kwMsb and 0x7F) shl 7) or (kwLsb and 0x7F)
            Logger.log("KW2000: fast init OK, версия $version")
            return version
        }
        throw ProtoException("Блок не ответил на StartCommunication")
    }

    /** Подключение после slow-init (блок уже проснулся). */
    fun startCommunication(address: Int): Int {
        target = address
        link.setBaud(baud)
        val resp = sendReceive(byteArrayOf(0x81.toByte()), rawStart = true)
        if (resp.size >= 3 && (resp[0].toInt() and 0xFF) == 0xC1) {
            val kwLsb = resp[1].toInt() and 0xFF
            val kwMsb = resp[2].toInt() and 0xFF
            return ((kwMsb and 0x7F) shl 7) or (kwLsb and 0x7F)
        }
        throw ProtoException("Нет ответа на StartCommunication")
    }

    fun stopCommunication() {
        try { sendReceive(byteArrayOf(0x82.toByte())) } catch (_: Exception) {}
    }

    // ---------------- Сервисы ----------------

    class IdentResult(val lines: List<String>, val componentId: String)

    fun readIdent(): IdentResult {
        val lines = mutableListOf<String>()
        var componentId = ""
        try {
            val r = sendReceive(byteArrayOf(0x1A, 0x90.toByte()))
            if ((r[0].toInt() and 0xFF) == 0x5A) {
                val s = String(r.copyOfRange(1, r.size), Charsets.US_ASCII).trim()
                if (s.isNotEmpty()) { lines.add(s); componentId = s.split(Regex("\\s+")).firstOrNull() ?: "" }
            }
        } catch (e: Exception) { Logger.log("KW2000: ident 0x90: ${e.message}") }
        try {
            val r2 = sendReceive(byteArrayOf(0x1A, 0x94.toByte()))
            if ((r2[0].toInt() and 0xFF) == 0x5A) {
                val s = String(r2.copyOfRange(1, r2.size), Charsets.US_ASCII).trim()
                if (s.isNotEmpty()) lines.add(s)
            }
        } catch (e: Exception) { Logger.log("KW2000: ident 0x94: ${e.message}") }
        return IdentResult(lines, componentId)
    }

    class Fault(val raw: ByteArray) {
        fun dtcStr(): String {
            if (raw.size < 3) return raw.joinToString(" ") { "%02X".format(it) }
            val a = raw[0].toInt() and 0xFF
            val b = raw[1].toInt() and 0xFF
            // ISO 14230-4 DTC: P/C/B/U + 4 hex
            val kind = when (a shr 6) { 0 -> "P"; 1 -> "C"; 2 -> "B"; else -> "U" }
            val d1 = (a shr 4) and 0x03
            val d2 = a and 0x0F
            val d3 = (b shr 4) and 0x0F
            val d4 = b and 0x0F
            return "%s%X%X%X%X".format(kind, d1, d2, d3, d4)
        }
        fun status(): Int = if (raw.size >= 3) raw[2].toInt() and 0xFF else 0
    }

    fun readFaultCodes(): List<Fault> {
        // 0x13 0xFF 0xFF 0xFF — все DTC всех статусов
        val r = sendReceive(byteArrayOf(0x13, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
        if ((r[0].toInt() and 0xFF) == 0x7F) throw ProtoException("Сервис не поддерживается блоком")
        if ((r[0].toInt() and 0xFF) != 0x53) throw ProtoException("Неожиданный ответ 0x%02X".format(r[0]))
        val faults = mutableListOf<Fault>()
        val dtcBytes = r.size - 1
        var i = 1
        while (i + 2 < r.size + 1 && i + 2 < r.size) {
            faults.add(Fault(byteArrayOf(r[i], r[i + 1], r[i + 2])))
            i += 3
        }
        return faults
    }

    fun clearFaultCodes() {
        sendReceive(byteArrayOf(0x14, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
    }

    /** Измеряемые значения: ReadDataByLocalIdentifier (0x21), локальный идентификатор = группа. */
    fun readLocalData(localId: Int): ByteArray {
        val r = sendReceive(byteArrayOf(0x21, localId.toByte()))
        if ((r[0].toInt() and 0xFF) == 0x7F) throw ProtoException("Группа/идентификатор недоступен")
        if ((r[0].toInt() and 0xFF) != 0x61) throw ProtoException("Неожиданный ответ 0x%02X".format(r[0]))
        return r.copyOfRange(1, r.size)
    }

    /** SecurityAccess (0x27) — «логин». */
    fun securityAccess(level: Int, key: Int): Boolean {
        val seedResp = try { sendReceive(byteArrayOf(0x27, (level shl 2 or 1).toByte())) }
        catch (e: Exception) { return false }
        if ((seedResp[0].toInt() and 0xFF) != 0x67) return false
        // Для VAG K-line ключ обычно передаётся как есть (упрощённо)
        val keyResp = try { sendReceive(byteArrayOf(0x27, (level shl 2 or 2).toByte(),
            (key shr 8).toByte(), key.toByte())) } catch (e: Exception) { return false }
        return (keyResp[0].toInt() and 0xFF) == 0x67
    }

    // ---------------- Транспорт ----------------

    private val readBuf = java.util.concurrent.LinkedBlockingQueue<Int>()

    /**
     * Отправка кадра и приём ответа.
     * rawStart: кадр StartCommunication посылается без обычного заголовка tgt/src (0x81 addr F1 0x81 cs).
     */
    private fun sendReceive(payload: ByteArray, needChecksumHeader: Boolean = false, rawStart: Boolean = false): ByteArray {
        val frame = mutableListOf<Byte>()
        if (rawStart) {
            frame.add(0x81.toByte())
            frame.add(target.toByte())
            frame.add(source.toByte())
        } else {
            frame.add((payload.size + 2).toByte())
            frame.add(target.toByte())
            frame.add(source.toByte())
        }
        payload.forEach { frame.add(it) }
        var cs = 0
        for (i in 1 until frame.size) cs = (cs + frame[i].toInt()) and 0xFF
        frame.add((if (rawStart) {
            var c = 0
            for (b in frame) c = (c + b.toInt()) and 0xFF
            c
        } else cs).toByte())

        link.write(frame.toByteArray())
        // Эхо полудуплексной линии: прочитать столько же байт
        repeat(frame.size) { readByte(1500) }

        // Чтение ответного кадра
        val header = readByte(2000)
        if (header < 0) throw ProtoException("Таймаут ответа")
        // Реинъекция первого байта если это не длина (шум/эхо) — простой вариант: ожидаем len
        val len = header
        val tgt = readByte(1000)
        val src = readByte(1000)
        if (tgt < 0 || src < 0) throw ProtoException("Таймаут заголовка ответа")
        val dataLen = len - 2
        val data = ByteArray(dataLen)
        for (i in 0 until dataLen) {
            val b = readByte(1500)
            if (b < 0) throw ProtoException("Таймаут данных ответа")
            data[i] = b.toByte()
        }
        val rcs = readByte(1500)
        return data
    }

    private fun readByte(timeoutMs: Int): Int = link.readByte(timeoutMs)
}
