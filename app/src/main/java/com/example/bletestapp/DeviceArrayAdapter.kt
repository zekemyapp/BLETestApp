package com.example.bletestapp

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.TextView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE


class DeviceArrayAdapter(
    private val mContext: Context
) : ArrayAdapter<BluetoothDevice>(mContext, 0) {

    private val deviceList = ArrayList<BluetoothDevice>()
    private var mConnected = false;

    override fun add(device: BluetoothDevice) {
        deviceList.forEach{
            if (it.address == device.address) {
                deviceList[deviceList.indexOf(it)] = device
                return
            }
        }
        deviceList.add(device)
    }

    override fun getItem(position: Int): BluetoothDevice {
        return deviceList[position];
    }

    fun setConnected(isConnected: Boolean) {
        mConnected = isConnected
    }

    override fun getCount(): Int {
        return deviceList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var listItem: View? = convertView
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.device_list_item, parent, false)

        val currentDev = deviceList.get(position)

        val name = listItem!!.findViewById(R.id.textView_name) as TextView
        name.text = currentDev.name?:"null"

        val address = listItem!!.findViewById(R.id.textView_address) as TextView
        address.text = currentDev.address

        val connect = listItem!!.findViewById(R.id.textView_connection) as TextView
        connect.visibility = if(mConnected) INVISIBLE else VISIBLE

        return listItem
    }
}