package com.vagdroid.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vagdroid.R
import com.vagdroid.Session
import com.vagdroid.link.BtLink
import com.vagdroid.link.UsbLink
import com.vagdroid.Logger

/** Экран выбора и подключения адаптера (USB или Bluetooth). */
class ConnectActivity : BaseActivity() {

    private lateinit var adapter: SimpleAdapter
    private val foundBt = mutableListOf<BluetoothDevice>()
    private var scanning = false

    private val btReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (dev != null && foundBt.none { it.address == dev.address }) {
                        foundBt.add(dev)
                        adapter.addItem(formatBt(dev))
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    scanning = false
                    findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun formatBt(dev: BluetoothDevice): String {
        val name = try { dev.name ?: "?" } catch (e: SecurityException) { "?" }
        return "BT: $name [${dev.address}]"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = SimpleAdapter { pos -> onDeviceClicked(pos) }
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnScanBt).setOnClickListener { scanBluetooth() }

        registerReceiver(btReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        })

        listUsbDevices()
    }

    private fun listUsbDevices() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        for (dev in usbManager.deviceList.values) {
            adapter.addItem("USB: ${dev.productName ?: "устройство"} [${dev.vendorId}:${dev.productId}]")
        }
    }

    private fun hasBtPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }

    @SuppressLint("MissingPermission")
    private fun scanBluetooth() {
        if (!hasBtPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ), 1)
            }
            return
        }
        val bt = BluetoothAdapter.getDefaultAdapter() ?: run {
            toast("Bluetooth не поддерживается")
            return
        }
        if (!bt.isEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 2)
            return
        }
        if (scanning) return
        scanning = true
        findViewById<ProgressBar>(R.id.progress).visibility = View.VISIBLE
        bt.cancelDiscovery()
        foundBt.clear()
        bt.startDiscovery()
        // Уже спаренные устройства
        for (dev in bt.bondedDevices) {
            if (foundBt.none { it.address == dev.address }) {
                foundBt.add(dev)
                adapter.addItem(formatBt(dev) + " (спарен)")
            }
        }
    }

    private fun onDeviceClicked(pos: Int) {
        val item = adapter.getItem(pos) ?: return
        when {
            item.startsWith("USB:") -> connectUsb(item)
            item.startsWith("BT:") -> connectBt(pos, item)
        }
    }

    private fun connectUsb(item: String) {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val match = Regex("\\[(\\d+):(\\d+)\\]").find(item)
        val dev = usbManager.deviceList.values.firstOrNull {
            if (match != null) it.vendorId == match.groupValues[1].toInt() &&
                    it.productId == match.groupValues[2].toInt()
            else false
        } ?: usbManager.deviceList.values.firstOrNull()
        if (dev == null) { toast("USB-устройство не найдено"); return }

        runIo(onError = { toast("Ошибка USB: ${it.message}") }) {
            Session.disconnectAll()
            val link = UsbLink(usbManager, dev)
            link.open()
            Session.link = link
            Logger.log("USB-адаптер подключён: ${link.name}")
            runOnUiThread {
                toast("Подключено: ${link.name}")
                finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBt(pos: Int, item: String) {
        val addr = Regex("\\[([0-9A-Fa-f:]+)\\]").find(item)?.groupValues?.get(1)
        val dev = foundBt.firstOrNull { it.address == addr }
        if (dev == null) { toast("Устройство не найдено"); return }

        runIo(onError = { toast("Ошибка Bluetooth: ${it.message}") }) {
            Session.disconnectAll()
            val link = BtLink(dev)
            link.open()
            Session.link = link
            Logger.log("Bluetooth-адаптер подключён: ${link.name}")
            runOnUiThread {
                toast("Подключено: ${link.name}")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(btReceiver) } catch (_: Exception) {}
    }
}
