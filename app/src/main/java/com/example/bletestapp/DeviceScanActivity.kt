package com.example.bletestapp

import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.widget.*

private const val SCAN_PERIOD: Long = 10000

/**
 * Activity for scanning and displaying available BLE devices.
 */
class DeviceScanActivity(
    //private val bluetoothAdapter: BluetoothAdapter//,
    //private val handler: Handler
) : AppCompatActivity() {

    private var mScanning: Boolean = false
    var adapter: ArrayAdapter<String>? = null

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_scan)

        val listView = findViewById<ListView>(R.id.list)
        adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter

        adapter?.add("Hello World")
        adapter?.notifyDataSetChanged()

        scanLeDevice(true)
    }

    private fun scanLeDevice(enable: Boolean) {
        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
               // handler.postDelayed({
                    //mScanning = false
                    //bluetoothAdapter.stopLeScan(leScanCallback)
                //}, SCAN_PERIOD)
                mScanning = true
                bluetoothAdapter.startLeScan(leScanCallback)
            }
            else -> {
                mScanning = false
                bluetoothAdapter.stopLeScan(leScanCallback)
            }
        }
    }



    private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {
            adapter?.add(device.address)
            adapter?.notifyDataSetChanged()
        }
    }
}
