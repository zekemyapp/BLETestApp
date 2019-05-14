package com.example.bletestapp

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.*
import android.content.ComponentName
import android.os.IBinder
import android.content.ServiceConnection
import android.content.IntentFilter





private const val SCAN_PERIOD: Long = 10000
private const val REQUEST_ENABLE_BT = 1

class DeviceScanActivity:AppCompatActivity() {

    private lateinit var mHandler: Handler
    private lateinit var  listView: ListView

    private var bluetoothLeService: BluetoothLeService? = null

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

        //val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        //bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);



        // Initialize the handler instance
        mHandler = Handler()
        listView = findViewById<ListView>(R.id.list)
        listView.adapter = deviceListAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, id ->
            Toast.makeText(this, "Click on " + deviceListAdapter?.getItem(position)?.name, Toast.LENGTH_SHORT).show()
        }

        if (bluetoothAdapter.isEnabled) scanLeDevice(true)
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        Intent(this, BluetoothLeService::class.java).also { intent ->
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume(){
        super.onResume()

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_GATT_CONNECTED)
        intentFilter.addAction(ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(ACTION_DATA_AVAILABLE)
        intentFilter.addAction(ACTION_TEST)
        return intentFilter
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
            deviceListAdapter?.add(device)
            deviceListAdapter?.notifyDataSetChanged()
        }
    }

    /* Handles various events fired by the Service.
     * ACTION_GATT_CONNECTED: connected to a GATT server.
     * ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     * ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     * ACTION_DATA_AVAILABLE: received data from the device. This can be a
     * result of read or notification operations. */
    private val gattUpdateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action){
                ACTION_GATT_CONNECTED -> {
                    //connected = true
                    //updateConnectionState(R.string.connected)
                    //(context as? Activity)?.invalidateOptionsMenu()
                }
                ACTION_GATT_DISCONNECTED -> {
                    //connected = false
                    //updateConnectionState(R.string.disconnected)
                    //(context as? Activity)?.invalidateOptionsMenu()
                    //clearUI()
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the
                    // user interface.
                    //displayGattServices(bluetoothLeService.getSupportedGattServices())
                }
                ACTION_DATA_AVAILABLE -> {
                    //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                }
                ACTION_TEST -> {
                    Log.i("TEST LOG", "VALID TEST")
                }
            }
        }
    }

    // Code to manage Service lifecycle.
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            bluetoothLeService = (service as BluetoothLeService.LocalBinder).getService()
            if (!bluetoothLeService!!.initialize()) {
                Log.e("TEST LOG", "Unable to initialize Bluetooth")
            } else {
                Log.e("TEST LOG", "Initialized Service!!!")
            }

            bluetoothLeService?.test()
            // Automatically connects to the device upon successful start-up initialization.
            //bluetoothLeService.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothLeService = null
        }
    }

}
