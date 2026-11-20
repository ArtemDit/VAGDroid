package com.vagdroid.proto

import com.vagdroid.Logger
import com.vagdroid.link.Link

/**
 * Реализация протокола VW KW1281 (VAG-COM Release 311 уровня).
 * Порт логики kw1281test (gmenounos) на Kotlin.
 *
 * Формат блока: [len][counter][title][data...][0x03], контрольной суммы нет.
 * Каждый переданный/принятый байт квитируется инверсией на линии.
 */
class Kw1281(private val link: Link) {

    private var blockCounter: Int? = null

    class ProtoException(msg: String) : Exception(msg)

    /** Результат wakeup: версия протокола (1281, 2000, 2027...). */
    fun wakeUp(address: Int, evenParity: Boolean = false): Int {
        link.setBaud(10400)
        Thread.sleep(300)
        if (!bitBang5Baud(address, evenParity)) {
            throw ProtoException("Адаптер не поддерживает управление линией BREAK — slow init невозможен")
        }
        link.setBaud(10400)
        link.purge()

        val sync = readByte(2000)
        if (sync != 0x55) throw ProtoException("Нет ответа блока (sync=0x55 не получен)")

        val kwLsb = readByte(2000)
        val kwMsb = readByte(2000)
        val version = ((kwMsb and 0x7F) shl 7) or (kwLsb and 0x7F)
        Logger.log("KW: протокол KW $version")

        Thread.sleep(25)
        writeByte((kwMsb.inv()) and 0xFF)
        if (version >= 2000) {
            readComplement(adjustParity(address, evenParity))
        }
        return version
    }

    /** Подключение: после wakeup контроллер присылает блоки идентификации. */
    fun connect(address: Int): IdentResult {
        val version = wakeUp(address)
        if (version != 1281) throw ProtoException("Блок ответил протоколом KW $version, ожидался KW1281")
        val blocks = receiveBlocks()
        return parseIdent(blocks)
    }

    fun endCommunication() {
        try {
            sendBlock(listOf(T_END))
        } catch (_: Exception) {
        }
    }

    // ---------- Идентификация ----------

    class IdentResult(
        val lines: List<String>,
        val componentId: String,
        val coding: Int,
        val wsc: Int
    )

    fun readIdent(): IdentResult {
        sendBlock(listOf(T_READ_IDENT))
        return parseIdent(receiveBlocks())
    }

    private fun parseIdent(blocks: List<Block>): IdentResult {
        val lines = mutableListOf<String>()
        var componentId = ""
        var coding = -1
        var wsc = 0
        for (b in blocks) {
            when (b.title.toInt() and 0xFF) {
                T_ASCII -> {
                    if (b.data.isNotEmpty() && b.data[0].toInt() == 0) {
                        // Блок 0xF6 с третьим байтом 0x00: кодировка + WSC
                        val d = b.data
                        if (d.size >= 5) {
                            val rawCoding = (d[1].toInt() and 0xFF) * 256 + (d[2].toInt() and 0xFF)
                            coding = rawCoding / 2
                            wsc = (d[3].toInt() and 0xFF) * 256 + (d[4].toInt() and 0xFF)
                            if ((d[2].toInt() and 1) == 1) wsc += 65536
                        }
                    } else {
                        val txt = b.data.joinToString("") { (it.toInt() and 0x7F).toChar().toString() }
                        if (txt.isNotBlank()) lines.add(txt.trim())
                        if (componentId.isEmpty()) componentId = txt.trim().split(Regex("\\s+")).firstOrNull() ?: ""
                    }
                }
            }
        }
        return IdentResult(lines, componentId, coding, wsc)
    }

    // ---------- Коды неисправностей ----------

    class Fault(val dtc: Int, val status: Int) {
        val sporadic: Boolean get() = (status and 0x80) != 0
        fun code5(): String = "%05d".format(dtc)
        fun statusStr(): String = "%02d-%02d".format(status and 0x7F, (status shr 7) * 10)
    }

    fun readFaultCodes(): List<Fault> {
        sendBlock(listOf(T_FAULTS_READ))
        return collectFaults()
    }

    fun clearFaultCodes(): List<Fault> {
        sendBlock(listOf(T_FAULTS_DELETE))
        return collectFaults()
    }

    private fun collectFaults(): List<Fault> {
        val faults = mutableListOf<Fault>()
        val blocks = receiveBlocks()
        for (b in blocks) {
            if ((b.title.toInt() and 0xFF) == T_FAULTS_RESP) {
                var i = 0
                while (i + 2 < b.data.size + 1 && i + 2 < b.data.size) {
                    val dtc = (b.data[i].toInt() and 0xFF) * 256 + (b.data[i + 1].toInt() and 0xFF)
                    val st = b.data[i + 2].toInt() and 0xFF
                    if (!(dtc == 0xFFFF && st == 0x88)) faults.add(Fault(dtc, st))
                    i += 3
                }
            }
        }
        return faults
    }

    // ---------- Измеряемые группы ----------

    class GroupValue(val sensorId: Int, val a: Int, val b: Int) {
        fun raw(): Int = a * 256 + b
        fun text(): String = SensorConv.convert(sensorId, a, b)
    }

    class GroupResult(val group: Int, val values: List<GroupValue>, val text: String)

    /** Одно чтение группы. Ответ: 0xE7 (значения) или 0x02 (со значениями-текстом). */
    fun readGroup(group: Int): GroupResult {
        sendBlock(listOf(T_GROUP_READ, group and 0xFF))
        val b = receiveBlock()
        return when (b.title.toInt() and 0xFF) {
            T_GROUP_RESP -> {
                val vals = mutableListOf<GroupValue>()
                var i = 0
                while (i + 2 < b.data.size + 1 && i + 2 < b.data.size) {
                    vals.add(GroupValue(b.data[i].toInt() and 0xFF, b.data[i + 1].toInt() and 0xFF, b.data[i + 2].toInt() and 0xFF))
                    i += 3
                }
                GroupResult(group, vals, "")
            }
            T_GROUP_TEXT_RESP -> {
                // Подблоки: [type][data][len][body...]; type 0x8D — текстовая таблица
                val texts = mutableListOf<String>()
                var i = 0
                val d = b.data
                while (i + 2 < d.size) {
                    val type = d[i].toInt() and 0xFF
                    val len = d[i + 2].toInt() and 0xFF
                    if (i + 3 + len > d.size) break
                    if (type == 0x8D) {
                        val s = String(d.copyOfRange(i + 3, i + 3 + len), Charsets.US_ASCII)
                        texts.addAll(s.split(0x03.toChar()))
                    }
                    i += 3 + len
                }
                GroupResult(group, emptyList(), texts.filter { it.isNotEmpty() }.joinToString(" | "))
            }
            T_NAK -> GroupResult(group, emptyList(), "Группа $group недоступна")
            else -> GroupResult(group, emptyList(), "Неожиданный ответ 0x%02X".format(b.title))
        }
    }

    // ---------- Базовые установки (та же механика, title 0x28) ----------

    fun basicSetting(group: Int): GroupResult {
        sendBlock(listOf(T_BASIC_SETTING, group and 0xFF))
        val b = receiveBlock()
        return when (b.title.toInt() and 0xFF) {
            T_GROUP_RESP -> {
                val vals = mutableListOf<GroupValue>()
                var i = 0
                while (i + 2 < b.data.size) {
                    vals.add(GroupValue(b.data[i].toInt() and 0xFF, b.data[i + 1].toInt() and 0xFF, b.data[i + 2].toInt() and 0xFF))
                    i += 3
                }
                GroupResult(group, vals, "")
            }
            T_NAK -> GroupResult(group, emptyList(), "Группа $group недоступна")
            else -> GroupResult(group, emptyList(), "Неожиданный ответ 0x%02X".format(b.title))
        }
    }

    // ---------- Тест исполнителей ----------

    class ActuatorResult(val testByte: Int, val text: String)

    fun actuatorTest(value: Int): ActuatorResult {
        sendBlock(listOf(T_ACTUATOR, value and 0xFF))
        val b = receiveBlock()
        if ((b.title.toInt() and 0xFF) == T_NAK) return ActuatorResult(value, "Недоступно")
        val txt = b.data.joinToString("") { (it.toInt() and 0x7F).toChar().toString() }.trim()
        return ActuatorResult(value, txt)
    }

    // ---------- Логин / Адаптация / Кодирование ----------

    /** true — логин принят, false — отказ. */
    fun login(code: Int, wsc: Int): Boolean {
        sendBlock(listOf(T_LOGIN, (code shr 8) and 0xFF, code and 0xFF,
            (wsc shr 16) and 0xFF, (wsc shr 8) and 0xFF, wsc and 0xFF))
        val b = receiveBlock()
        return (b.title.toInt() and 0xFF) == T_ACK
    }

    class AdaptationResult(val ok: Boolean, val channelValue: Int, val raw: List<Int>)

    fun adaptationRead(channel: Int): AdaptationResult {
        sendBlock(listOf(T_ADAPT_READ, channel and 0xFF))
        val b = receiveBlock()
        return parseAdaptation(b)
    }

    fun adaptationTest(channel: Int, value: Int): AdaptationResult {
        sendBlock(listOf(T_ADAPT_TEST, channel and 0xFF, (value shr 8) and 0xFF, value and 0xFF))
        val b = receiveBlock()
        return parseAdaptation(b)
    }

    fun adaptationSave(channel: Int, value: Int, wsc: Int): AdaptationResult {
        sendBlock(listOf(T_ADAPT_SAVE, channel and 0xFF, (value shr 8) and 0xFF, value and 0xFF,
            (wsc shr 16) and 0xFF, (wsc shr 8) and 0xFF, wsc and 0xFF))
        val b = receiveBlock()
        return parseAdaptation(b)
    }

    private fun parseAdaptation(b: Block): AdaptationResult {
        val title = b.title.toInt() and 0xFF
        return when {
            title == T_ADAPT_RESP -> {
                val d = b.data
                val value = if (d.size >= 2) (d[0].toInt() and 0xFF) * 256 + (d[1].toInt() and 0xFF) else -1
                AdaptationResult(true, value, d.map { it.toInt() and 0xFF })
            }
            title == T_NAK -> AdaptationResult(false, -1, emptyList())
            else -> AdaptationResult(false, -1, listOf(title))
        }
    }

    fun setSoftwareCoding(coding: Int, wsc: Int): Boolean {
        val raw = coding * 2
        val bytes = mutableListOf(
            T_SOFTWARE_CODING,
            (raw / 256) and 0xFF, raw % 256,
            (wsc and 0xFFFF) / 256, wsc % 256
        )
        if (wsc > 65535) bytes[2] = (bytes[2] + 1) and 0xFF
        sendBlock(bytes)
        val blocks = receiveBlocks()
        val ident = parseIdent(blocks)
        return ident.coding == coding
    }

    // ---------- Механика блоков ----------

    class Block(val title: Byte, val data: ByteArray) {
        val isAckNak: Boolean
            get() = (title.toInt() and 0xFF) == T_ACK || (title.toInt() and 0xFF) == T_NAK
    }

    fun sendBlock(payload: List<Int>) {
        Thread.sleep(25)
        val counter = blockCounter ?: 1
        blockCounter = (counter + 1) and 0xFF
        val frame = mutableListOf<Int>()
        frame.add(payload.size + 2)      // len
        frame.add(counter)
        frame.addAll(payload)
        for (b in frame) {
            writeByteAndReadComplement(b)
            Thread.sleep(2)
        }
        writeByte(0x03)                  // конец блока без квитирования
    }

    fun receiveBlocks(): List<Block> {
        val blocks = mutableListOf<Block>()
        while (true) {
            val b = receiveBlock()
            blocks.add(b)
            if ((b.title.toInt() and 0xFF) == T_ACK || (b.title.toInt() and 0xFF) == T_NAK) break
            sendAckBlock()
        }
        return blocks
    }

    fun receiveBlock(): Block {
        val len = readAndAckFirst()
        val ctr = readAndAckByte()
        if (blockCounter == null) {
            blockCounter = (ctr + 1) and 0xFF
        } else {
            blockCounter = (ctr + 1) and 0xFF
        }
        val title = readAndAckByte().toByte()
        val dataLen = len - 3
        val data = ByteArray(maxOf(dataLen, 0))
        var i = 0
        while (i < dataLen) {
            data[i] = readAndAckByte().toByte()
            i++
        }
        val end = readByte(1000)
        if (end != 0x03) throw ProtoException("Конец блока 0x%02X вместо 0x03".format(end))
        return Block(title, data)
    }

    private fun sendAckBlock() {
        sendBlock(listOf(T_ACK))
    }

    private fun readAndAckByte(): Int {
        val b = readByte(1000)
        if (b < 0) throw ProtoException("Таймаут чтения блока")
        Thread.sleep(2)
        writeByte(b.inv() and 0xFF)
        return b
    }

    private fun readAndAckFirst(): Int {
        var b = readByte(2000)
        var attempts = 0
        while (b == 0x55 && attempts < 5) {
            val kwLsb = readByte(2000)
            val kwMsb = readByte(2000)
            Thread.sleep(25)
            writeByte(kwMsb.inv() and 0xFF)
            b = readByte(2000)
            attempts++
        }
        if (b < 0) throw ProtoException("Таймаут чтения блока")
        Thread.sleep(2)
        writeByte(b.inv() and 0xFF)
        return b
    }

    private fun writeByteAndReadComplement(b: Int) {
        writeByte(b)
        readComplement(b)
    }

    private fun readComplement(expected: Int) {
        val c = readByte(2000)
        if (c < 0) throw ProtoException("Таймаут квитирования")
        // Строгая проверка инверсии отключена: часть адаптеров искажает эхо
    }

    private fun readByte(timeoutMs: Int): Int = link.readByte(timeoutMs)

    private fun writeByte(b: Int) = link.write(b)

    // ---------- 5-baud init ----------

    private fun bitBang5Baud(address: Int, evenParity: Boolean): Boolean {
        if (!link.supportsBreak) return false
        var b = adjustParity(address, evenParity)
        val msPerBit = 200L
        return try {
            link.setBreak(false)
            Thread.sleep(10)
            fun bitBang(level: Boolean) {
                link.setBreak(!level)
            }
            val t0 = System.currentTimeMillis()
            fun waitNext(bitIndex: Int) {
                val target = t0 + bitIndex * msPerBit
                while (System.currentTimeMillis() < target) { /* busy wait */ }
            }
            waitNext(0); bitBang(false)                 // start
            for (i in 0 until 8) {
                waitNext(i + 1)
                bitBang((b and 1) == 1)
                b = b shr 1
            }
            waitNext(9); bitBang(true)                  // stop
            waitNext(10)
            link.setBreak(false)
            true
        } catch (e: Exception) {
            Logger.log("KW: ошибка bit-bang: ${e.message}")
            false
        }
    }

    companion object {
        // Заголовки блоков
        const val T_READ_IDENT = 0x00
        const val T_GROUP_TEXT_RESP = 0x02
        const val T_ACTUATOR = 0x04
        const val T_FAULTS_DELETE = 0x05
        const val T_END = 0x06
        const val T_FAULTS_READ = 0x07
        const val T_ACK = 0x09
        const val T_NAK = 0x0A
        const val T_SOFTWARE_CODING = 0x10
        const val T_BASIC_SETTING = 0x28
        const val T_GROUP_READ = 0x29
        const val T_ADAPT_READ = 0x21
        const val T_ADAPT_TEST = 0x22
        const val T_ADAPT_SAVE = 0x2A
        const val T_LOGIN = 0x2B
        const val T_ADAPT_RESP = 0xE6
        const val T_GROUP_RESP = 0xE7
        const val T_ACTUATOR_RESP = 0xF5
        const val T_ASCII = 0xF6
        const val T_FAULTS_RESP = 0xFC

        /** 7-й бит = бит чётности (odd для KW1281, even для KW2000). */
        fun adjustParity(b: Int, even: Boolean): Int {
            var x = b and 0x7F
            var ones = 0
            var t = x
            while (t != 0) { ones += t and 1; t = t shr 1 }
            val parityBit = if (even) (ones + 1) % 2 else ones % 2
            return x or (parityBit shl 7)
        }
    }
}

/** Таблица преобразования значений датчиков KW1281 (по blafusel.de). */
object SensorConv {
    fun convert(id: Int, a: Int, b: Int): String = when (id) {
        1 -> "%.0f rpm".format(0.2 * a * b)
        2 -> "%.1f %%".format(a * 0.002 * b)
        3 -> "%.1f Deg".format(0.002 * a * b)
        4 -> "%.1f °%s".format(abs(b - 127) * 0.01 * a, if (b > 127) "ATDC" else "BTDC")
        5 -> "%.1f °C".format(a * (b - 100) * 0.1)
        6 -> "%.1f V".format(0.001 * a * b)
        7 -> "%.0f km/h".format(0.01 * a * b)
        8 -> "%.1f".format(0.1 * a * b)
        9 -> "%.1f Deg".format((b - 127) * 0.02 * a)
        10 -> if (b == 0) "Cold" else "Warm"
        11 -> "%.1f".format(0.0001 * a * (b - 128) + 1)
        12 -> "%.1f Ω".format(0.001 * a * b)
        13 -> "%.1f mm".format((b - 127) * 0.001 * a)
        14 -> "%.1f bar".format(0.005 * a * b)
        15 -> "%.1f ms".format(0.01 * a * b)
        16 -> "%s %s".format(a.toString(2).padStart(8, '0'), b.toString(2).padStart(8, '0'))
        17 -> "\"%s%s\"".format(a.toChar(), b.toChar())
        18 -> "%.1f mbar".format(0.04 * a * b)
        19 -> "%.1f l".format(a * b * 0.01)
        20 -> "%.1f %%".format(a * (b - 128) / 128.0)
        21 -> "%.1f V".format(0.001 * a * b)
        22 -> "%.1f ms".format(0.001 * a * b)
        23 -> "%.1f %%".format(b / 256.0 * a)
        24 -> "%.1f A".format(0.001 * a * b)
        25 -> "%.1f g/s".format(b * 1.421 + a / 182.0)
        26 -> "%d C".format(b - a)
        27 -> "%.1f °%s".format(abs(b - 128) * 0.01 * a, if (b < 128) "ATDC" else "BTDC")
        28 -> "%d".format(b - a)
        29 -> if (b < a) "Map 1" else "Map 2"
        30 -> "%.1f Deg k/w".format(b / 12.0 * a)
        31 -> "%.1f".format(b / 2560.0 * a)
        32 -> "%d".format(if (b > 128) b - 256 else b)
        33 -> "%.1f %%".format(if (a == 0) 100.0 * b else 100.0 * b / a)
        34 -> "%.1f kW".format((b - 128) * 0.01 * a)
        35 -> "%.1f l/h".format(0.01 * a * b)
        36 -> "%d km".format(a * 2560 + b * 10)
        38 -> "%.1f Deg k/w".format((b - 128) * 0.001 * a)
        39 -> "%.1f mg/h".format(b / 256.0 * a)
        40 -> "%.1f A".format(b * 0.1 + 25.5 * a - 400)
        41 -> "%d Ah".format(b + a * 255)
        42 -> "%.1f Kw".format(b * 0.1 + 25.5 * a - 400)
        43 -> "%.1f V".format(b * 0.1 + 25.5 * a)
        44 -> "%02d:%02d".format(a, b)
        45 -> "%.1f".format(0.1 * a * b / 100.0)
        46 -> "%.1f Deg k/w".format((a * b - 3200) * 0.0027)
        47 -> "%d ms".format((b - 128) * a)
        48 -> "%d".format(b + a * 255)
        49 -> "%.1f mg/h".format(b / 4.0 * a * 0.1)
        50 -> "%.1f mbar".format(if (a == 0) (b - 128) / 0.01 else (b - 128) / (0.01 * a))
        51 -> "%.1f mg/h".format(((b - 128) / 255.0) * a)
        52 -> "%.1f Nm".format(b * 0.02 * a - a)
        53 -> "%.1f g/s".format((b - 128) * 1.4222 + 0.006 * a)
        54 -> "%d".format(a * 256 + b)
        55 -> "%.1f s".format(a * b / 200.0)
        56 -> "%d WSC".format(a * 256 + b)
        57 -> "%d WSC".format(a * 256 + b + 65536)
        58 -> "%.1f /s".format(if (b > 128) 1.0225 * (256 - b) else 1.0225 * b)
        59 -> "%.1f".format((a * 256 + b) / 32768.0)
        60 -> "%.1f sec".format((a * 256 + b) * 0.01)
        61 -> "%.1f".format(if (a == 0) (b - 128).toDouble() else (b - 128) / a.toDouble())
        62 -> "%.1f S".format(0.256 * a * b)
        64 -> "%d Ω".format(a + b)
        65 -> "%.1f mm".format(0.01 * a * (b - 127))
        66 -> "%.1f V".format((a * b) / 511.12)
        67 -> "%.1f Deg".format(640 * a + b * 2.5)
        68 -> "%.1f deg/s".format((256 * a + b) / 7.365)
        69 -> "%.1f Bar".format((256 * a + b) * 0.3254)
        70 -> "%.1f m/s²".format((256 * a + b) * 0.192)
        else -> "(id=%d %d %d)".format(id, a, b)
    }

    private fun abs(x: Int): Int = if (x < 0) -x else x
}
