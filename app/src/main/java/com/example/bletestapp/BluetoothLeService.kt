package com.example.bletestapp

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor



private val TAG = BluetoothLeService::class.java.simpleName
private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING   = 1
private const val STATE_CONNECTED    = 2
const val ACTION_GATT_CONNECTED           = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED        = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
const val ACTION_DATA_AVAILABLE           = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
const val EXTRA_DATA                      = "com.example.bluetooth.le.EXTRA_DATA"
val UUID_HEART_RATE_MEASUREMENT       = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

/* A service that interacts with the BLE device via the Android BLE API. */
//class BluetoothLeService(private var bluetoothGatt: BluetoothGatt?) : Service() {
class BluetoothLeService : Service() {

    private var connectionState = STATE_DISCONNECTED
    private val mBinder = LocalBinder()

    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothDevice: BluetoothDevice? = null
    private var mConnectionState = STATE_DISCONNECTED

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        fun getService() : BluetoothLeService? {
            return this@BluetoothLeService
        }
    }


    /**
     * Initializes shit.
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        /*if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter()
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }*/

        return true
    }

    fun connect(device: BluetoothDevice): Boolean {

        // TODO Previously connected device.  Try to reconnect.
        if (mBluetoothDevice != null && device == mBluetoothDevice && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, gattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        mBluetoothDevice = device
        mConnectionState = STATE_CONNECTING
        return true
    }

    fun disconnect() {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    fun getSupportedGattServices(): List<BluetoothGattService>? {
        return if (mBluetoothGatt == null) null
        else mBluetoothGatt!!.services
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.readCharacteristic(characteristic)
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }

        var bytes = ByteArray(1){
            0x09
        }
        characteristic.value = bytes
        mBluetoothGatt!!.writeCharacteristic(characteristic)
    }

    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }

        mBluetoothGatt?.setCharacteristicNotification(characteristic, enabled)
    }

    // Various callback methods defined by the BLE API.
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(intentAction)
                    Log.i(TAG, "Connected to GATT server.")
                    Log.i(TAG, "Attempting to start service discovery: " + mBluetoothGatt?.discoverServices())
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    connectionState = STATE_DISCONNECTED
                    Log.i(TAG, "Disconnected from GATT server.")
                    broadcastUpdate(intentAction)
                }
            }
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                else -> Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        // Result of a characteristic read operation
        override fun onCharacteristicRead(gatt:BluetoothGatt, characteristic:BluetoothGattCharacteristic, status:Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        // This is special handling for the Heart Rate Measurement profile. Data parsing is carried out as per profile specifications.
        when (characteristic.uuid) {
            UUID_HEART_RATE_MEASUREMENT -> {
                /*val flag = characteristic.properties
                val format = when (flag and 0x01) {
                    0x01 -> {
                        Log.d(TAG, "Heart rate format UINT16.")
                        BluetoothGattCharacteristic.FORMAT_UINT16
                    }
                    else -> {
                        Log.d(TAG, "Heart rate format UINT8.")
                        BluetoothGattCharacteristic.FORMAT_UINT8
                    }
                }
                val heartRate = characteristic.getIntValue(format, 1)
                Log.d(TAG, String.format("Received heart rate: %d", heartRate))
                intent.putExtra(EXTRA_DATA, (heartRate).toString())*/
            }
            else -> {
                // For all other profiles, writes the data formatted in HEX.
                val name: String = characteristic.uuid.toString()
                val data: ByteArray? = characteristic.value
                if (data?.isNotEmpty() == true) {
                    val hexString: String = data.joinToString(separator = " ") {
                        String.format("%02X", it)
                    }
                    intent.putExtra(EXTRA_DATA, "$name: $hexString")
                }
            }

        }
        sendBroadcast(intent)
    }
}