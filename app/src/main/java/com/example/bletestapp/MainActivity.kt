package com.example.bletestapp

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.*

private const val DEVICE_NAME = "SimpleBLEPeripheral"
private const val CHAR3_UUID = ""
private const val CHAR4_UUID = ""

private const val REQUEST_ENABLE_BT = 1
private const val REQUEST_ENABLE_LOCATION = 2
private const val SCAN_PERIOD: Long = 2000

class MainActivity : AppCompatActivity() {

    private var state_text :TextView? = null

    private var bluetoothLeService: BluetoothLeService? = null

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private lateinit var mHandler: Handler
    private lateinit var  listView: ListView
    private val deviceListAdapter: DeviceArrayAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        DeviceArrayAdapter(this)
    }

    private lateinit var  serviceListView: ExpandableListView
    private val serviceListAdapter: ServiceArrayAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        ServiceArrayAdapter(this)
    }

    private lateinit var  charListView: ListView
    private val charListAdapter: CharArrayAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        CharArrayAdapter(this)
    }

    private lateinit var editorView: View
    private lateinit var buttonRead: Button
    private lateinit var buttonWrite: Button
    private lateinit var buttonSub: Button
    private lateinit var mEddit: EditText

    private var mScanning: Boolean = false
    private var mConnected: Boolean = false
    private var mDevice: BluetoothDevice? = null
    private var mChar: BluetoothGattCharacteristic? = null
    private var mChar3: BluetoothGattCharacteristic? = null // Writable characteristic
    private var mChar4: BluetoothGattCharacteristic? = null // Sends notifications with the value of Char3


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        state_text = findViewById<TextView>(R.id.main_state_content);

        // If Bluetooth is not available/enabled, displays a dialog requesting user to enable Bluetooth.
        bluetoothAdapter.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            state_text?.text = "Please enable Bluetooth"
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_ENABLE_LOCATION)

            state_text?.text = "Please grant Location Permission"

        } else {
            //val ScanActivityintent = Intent(this, DeviceScanActivity::class.java)
            //startActivity(ScanActivityintent)

            state_text?.text = "Searching for devices..."
        }

        // Handler for asynchronous operation
        mHandler = Handler()

        // Initialize the list adapter
        listView = findViewById<ListView>(R.id.device_list)
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
                        //bluetoothLeService?.disconnect()
                        //mDevice = deviceListAdapter?.getItem(position)
                        //bluetoothLeService?.connect(mDevice!!)
                    }
                }
            }
        }

        serviceListView = findViewById<ExpandableListView>(R.id.char_list)
        serviceListView.setAdapter(serviceListAdapter)

        charListView = findViewById<ListView>(R.id.char_list2)
        charListView.setAdapter(charListAdapter)

        charListView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, id ->
            run {
                mChar = charListAdapter!!.getItem(position)

                findViewById<TextView>(R.id.text_characteristic).text = mChar!!.uuid.toString()

                if ((mChar!!.properties and BluetoothGattCharacteristic.PROPERTY_READ) !=0) {
                    buttonRead.visibility = View.VISIBLE
                } else {
                    buttonRead.visibility = View.GONE
                }

                if ((mChar!!.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) !=0) {
                    buttonWrite.visibility = View.VISIBLE
                } else {
                    buttonWrite.visibility = View.GONE
                }

                if ((mChar!!.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) !=0) {
                   buttonSub.visibility = View.VISIBLE
                } else {
                   buttonSub.visibility = View.GONE
                }
            }
        }

        editorView = findViewById<View>(R.id.editor)
        buttonRead = findViewById<Button>(R.id.button_read)
        buttonRead.setOnClickListener(View.OnClickListener {
            bluetoothLeService!!.readCharacteristic(mChar!!)
        })

        buttonWrite = findViewById<Button>(R.id.button_write)
        buttonWrite.setOnClickListener(View.OnClickListener {
            val c = if (mEddit.text.isNotEmpty()) mEddit.text[0] else '0'
            var bytes = byteArrayOf(c.toByte())

            bluetoothLeService!!.writeCharacteristic(mChar!!,bytes)
        })

        buttonSub = findViewById<Button>(R.id.button_sub)
        buttonSub.setOnClickListener(View.OnClickListener {
            bluetoothLeService!!.setCharacteristicNotification(mChar!!,true);
        })

        mEddit = findViewById<EditText>(R.id.edit_characteristic)

        val toggleall = findViewById<Switch>(R.id.all_chars)
        if (!toggleall.isChecked) serviceListView.visibility = View.GONE

        toggleall.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                serviceListView.visibility = View.VISIBLE
                charListView.visibility = View.GONE
                editorView.visibility = View.GONE
            } else {
                serviceListView.visibility = View.GONE
                charListView.visibility = View.VISIBLE
                editorView.visibility = View.VISIBLE
            }
        }

        // If Bluetooth was enabled start scanning
        if (bluetoothAdapter.isEnabled) scanLeDevice(true)
    }

    override fun onStart() {
        super.onStart()
        // Bind to BluetoothLEService
        Intent(this, BluetoothLeService::class.java).also { intent ->
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume(){
        super.onResume()
        // Register Receiver for BluetoothLEService
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }



    private fun scanLeDevice(enable: Boolean) {
        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed({
                    mScanning = false
                    bluetoothAdapter.stopLeScan(leScanCallback)
                    state_text?.text = "Ready"
                    state_text?.setTextColor(Color.GREEN)
                }, SCAN_PERIOD)
                mScanning = true
                bluetoothAdapter.startLeScan(leScanCallback)
            }
            else -> {
                mScanning = false
                bluetoothAdapter.stopLeScan(leScanCallback)
                state_text?.text = "Stopped"
            }
        }
    }

    private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {
            if(device.name == DEVICE_NAME) {
                deviceListAdapter?.add(device)
                deviceListAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // If request was successful, start scanning
                scanLeDevice(true)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ENABLE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted
                    //val ScanActivityintent = Intent(this, DeviceScanActivity::class.java)
                    //startActivity(ScanActivityintent)
                }
                return
            }

            else -> {
                // Ignore all other requests.
            }
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
                    mConnected = true
                    state_text?.text = "Connected"
                    state_text?.setTextColor(Color.GREEN)
                    deviceListAdapter?.setConnected(true);
                    deviceListAdapter?.notifyDataSetChanged();
                }
                ACTION_GATT_DISCONNECTED -> {
                    mConnected = false
                    state_text?.text = "Disconnected"
                    state_text?.setTextColor(Color.RED)
                    deviceListAdapter?.setConnected(false);
                    deviceListAdapter?.notifyDataSetChanged();
                    serviceListAdapter?.clear();
                    serviceListAdapter?.notifyDataSetChanged()
                    charListAdapter?.clear();
                    charListAdapter?.notifyDataSetChanged()
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the user interface.
                    val gattServices: List<BluetoothGattService>? = bluetoothLeService?.getSupportedGattServices()
                    gattServices?.forEach { gattService ->
                        Log.i("GATT_TEST", gattService.uuid.toString())
                        serviceListAdapter?.add(gattService)
                        serviceListAdapter?.notifyDataSetChanged()
                        val gattCharacteristics = gattService.characteristics
                        gattCharacteristics.forEach { gattCharacteristic ->
                            Log.i("GATT_TEST", "\t Characteristic: " + gattCharacteristic.uuid.toString())
                            if (gattService.uuid.toString() == "0000fff0-0000-1000-8000-00805f9b34fb") {
                                charListAdapter?.add(gattCharacteristic)
                                charListAdapter?.notifyDataSetChanged()
                            }
                            //if (gattCharacteristic.uuid.toString() == "0000fff3-0000-1000-8000-00805f9b34fb") mChar3 = gattCharacteristic
                            //if (gattCharacteristic.uuid.toString() == "0000fff4-0000-1000-8000-00805f9b34fb") mChar4 = gattCharacteristic
                        }
                    }

                    //if(mChar4 != null) bluetoothLeService!!.setCharacteristicNotification(mChar4!!,true)
                    //if(mChar3 != null) bluetoothLeService!!.writeCharacteristic(mChar3!!)
                }
                ACTION_DATA_AVAILABLE -> {
                    Log.i("GATT_TEST", "Data Received from: " + intent.getStringExtra(EXTRA_DATA))
                    val data = intent.getStringExtra(EXTRA_DATA).split(':')

                    val isNotif = intent.getBooleanExtra(EXTRA_NOTIF,false)

                    if(isNotif) findViewById<TextView>(R.id.text_notif).text = data[1]
                    else findViewById<TextView>(R.id.text_read).text = data[1]
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

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_GATT_CONNECTED)
        intentFilter.addAction(ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(ACTION_DATA_AVAILABLE)
        return intentFilter
    }

}
