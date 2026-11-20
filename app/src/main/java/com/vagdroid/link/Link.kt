package com.vagdroid.link

import java.io.Closeable

/** Байт-ориентированный транспорт: USB-адаптер KKL или Bluetooth SPP. */
interface Link : Closeable {
    val name: String

    /** true, если адаптер умеет управлять линией BREAK (нужно для 5-baud init). */
    val supportsBreak: Boolean

    fun open()
    fun isOpen(): Boolean

    fun readByte(timeoutMs: Int): Int   // -1 при таймауте
    fun write(b: Int)
    fun write(bytes: ByteArray)

    fun setBaud(baud: Int)
    fun setBreak(state: Boolean): Boolean
    fun purge()
}
