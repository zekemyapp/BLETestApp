package com.example.bletestapp

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context

import android.widget.ArrayAdapter
import android.widget.TextView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View

class CharArrayAdapter(private val mContext: Context) : ArrayAdapter<BluetoothGattCharacteristic>(mContext, 0) {

    private val charList = ArrayList<BluetoothGattCharacteristic>()

    override fun add(c: BluetoothGattCharacteristic) {
        charList.add(c)
    }

    override fun getItem(position: Int): BluetoothGattCharacteristic {
        return charList[position];
    }

    override fun getCount(): Int {
        return charList.size
    }

    override fun clear() {
        charList.clear()
        super.clear()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var listItem: View? = convertView
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.char_list_item, parent, false)

        val currentChar = charList.get(position)

        val name = listItem!!.findViewById(R.id.textView_name) as TextView
        name.text = currentChar.uuid.toString()?:"null"

        val address = listItem.findViewById(R.id.textView_uuid) as TextView
        address.text = currentChar.uuid.toString()

        return listItem
    }
}