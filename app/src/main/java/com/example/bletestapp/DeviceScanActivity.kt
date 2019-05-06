package com.example.bletestapp

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.widget.*

private const val SCAN_PERIOD: Long = 10000
private const val REQUEST_ENABLE_BT = 1

class DeviceScanActivity:AppCompatActivity() {

    private lateinit var mHandler: Handler
    private lateinit var  listView: ListView

    private var mScanning: Boolean = false
    private val deviceListAdapter: DeviceArrayAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        DeviceArrayAdapter(this)
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_scan)

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        bluetoothAdapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Initialize the handler instance
        mHandler = Handler()
        listView = findViewById<ListView>(R.id.list)
        listView.adapter = deviceListAdapter

        if (bluetoothAdapter.isEnabled) scanLeDevice(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                scanLeDevice(true)
            }
        }
    }

    private fun scanLeDevice(enable: Boolean) {
        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed({
                    mScanning = false
                    bluetoothAdapter.stopLeScan(leScanCallback)
                }, SCAN_PERIOD)
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
            deviceListAdapter?.add(DeviceItem(device.name?:"null",device.address))
            deviceListAdapter?.notifyDataSetChanged()
        }
    }
}
