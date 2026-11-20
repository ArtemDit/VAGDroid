package com.vagdroid.link

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/** Bluetooth SPP-подключение (KKL-адаптеры с последовательным профилем). */
@SuppressLint("MissingPermission")
class BtLink(val device: BluetoothDevice) : Link {

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    override val name: String
        get() = device.name ?: device.address

    override val supportsBreak: Boolean = false

    override fun isOpen(): Boolean = socket?.isConnected == true

    override fun open() {
        val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
        socket = sock
        sock.connect()
        input = sock.inputStream
        output = sock.outputStream
    }

    override fun readByte(timeoutMs: Int): Int {
        val inp = input ?: return -1
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (inp.available() > 0) return inp.read()
            try { Thread.sleep(2) } catch (_: InterruptedException) { return -1 }
        }
        return -1
    }

    override fun write(b: Int) {
        output?.write(b)
        output?.flush()
    }

    override fun write(bytes: ByteArray) {
        output?.write(bytes)
        output?.flush()
    }

    override fun setBaud(baud: Int) {
        // SPP-профиль не позволяет менять боды; игнорируем
    }

    override fun setBreak(state: Boolean): Boolean = false

    override fun purge() {
        val inp = input ?: return
        val deadline = System.currentTimeMillis() + 100
        while (System.currentTimeMillis() < deadline && inp.available() > 0) {
            inp.read()
        }
    }

    override fun close() {
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
