package com.example.bletestapp

import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
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

    private var mConnected: Boolean = false
    private var mDevice: BluetoothDevice? = null

    private var mChar3: BluetoothGattCharacteristic? = null
    private var mChar4: BluetoothGattCharacteristic? = null

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
            run {
                if (!mConnected) {
                    mDevice = deviceListAdapter?.getItem(position)
                    bluetoothLeService?.connect(mDevice!!)
                } else {
                    if(deviceListAdapter?.getItem(position) == mDevice) bluetoothLeService?.disconnect()
                    else {
                        // TODO Maybe this crashes due to race conditions
                        bluetoothLeService?.disconnect()
                        mDevice = deviceListAdapter?.getItem(position)
                        bluetoothLeService?.connect(mDevice!!)
                    }
                }
            }
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
                    Toast.makeText(applicationContext, "Connected to " + mDevice?.name, Toast.LENGTH_SHORT).show()
                    mConnected = true
                }
                ACTION_GATT_DISCONNECTED -> {
                    //connected = false
                    //updateConnectionState(R.string.disconnected)
                    //(context as? Activity)?.invalidateOptionsMenu()
                    //clearUI()
                    Toast.makeText(applicationContext, "Disconnected from " + mDevice?.name, Toast.LENGTH_SHORT).show()
                    mConnected = false
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the
                    // user interface.
                    //displayGattServices(bluetoothLeService.getSupportedGattServices())
                    Toast.makeText(applicationContext, "Services Discovered from " + mDevice?.name, Toast.LENGTH_SHORT).show()
                    val gattServices: List<BluetoothGattService>? = bluetoothLeService?.getSupportedGattServices()
                    gattServices?.forEach { gattService ->
                        Log.i("GATT_TEST", gattService.uuid.toString())
                        val gattCharacteristics = gattService.characteristics
                        gattCharacteristics.forEach { gattCharacteristic ->
                            Log.i("GATT_TEST", "\t Characteristic: " + gattCharacteristic.uuid.toString())
                            if (gattCharacteristic.uuid.toString() == "0000fff3-0000-1000-8000-00805f9b34fb") mChar3 = gattCharacteristic
                            if (gattCharacteristic.uuid.toString() == "0000fff4-0000-1000-8000-00805f9b34fb") mChar4 = gattCharacteristic
                        }
                    }

                    if(mChar4 != null) bluetoothLeService!!.setCharacteristicNotification(mChar4!!,true)
                    if(mChar3 != null) bluetoothLeService!!.writeCharacteristic(mChar3!!)
                }
                ACTION_DATA_AVAILABLE -> {
                    //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                    Log.i("GATT_TEST", "Data Received from: " + intent.getStringExtra(EXTRA_DATA))
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
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothLeService = null
        }
    }

}
