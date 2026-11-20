package com.vagdroid.link

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException

/** USB-подключение через usb-serial-for-android (FTDI/CH340/CP210x/PL2303/CDC). */
class UsbLink(
    private val usbManager: UsbManager,
    val device: UsbDevice
) : Link {

    private var port: UsbSerialPort? = null
    private var connection: UsbDeviceConnection? = null

    override val name: String
        get() = device.productName ?: device.deviceName

    override val supportsBreak: Boolean = true

    override fun isOpen(): Boolean = port?.isOpen == true

    override fun open() {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            ?: throw IOException("Неизвестный USB-адаптер (vid=${device.vendorId}, pid=${device.productId})")
        val conn = usbManager.openDevice(device) ?: throw IOException("Не удалось открыть USB-устройство")
        connection = conn
        val p = driver.ports[0]
        port = p
        p.open(conn)
        p.setParameters(10400, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    override fun readByte(timeoutMs: Int): Int {
        val p = port ?: return -1
        val buf = ByteArray(1)
        val n = p.read(buf, timeoutMs)
        return if (n > 0) buf[0].toInt() and 0xFF else -1
    }

    override fun write(b: Int) {
        port?.write(byteArrayOf(b.toByte()), 1000)
    }

    override fun write(bytes: ByteArray) {
        port?.write(bytes, 2000)
    }

    override fun setBaud(baud: Int) {
        port?.setParameters(baud, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    override fun setBreak(state: Boolean): Boolean {
        val p = port ?: return false
        return try {
            p.setBreak(state)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun purge() {
        try { port?.purgeHwBuffers(true, true) } catch (_: Exception) {}
    }

    override fun close() {
        try { port?.close() } catch (_: Exception) {}
        try { connection?.close() } catch (_: Exception) {}
        port = null
        connection = null
    }
}
