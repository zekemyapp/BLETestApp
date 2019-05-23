package com.example.bletestapp

import android.bluetooth.BluetoothGattService
import android.content.Context
import android.widget.TextView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.BaseExpandableListAdapter
import java.util.*
import kotlin.collections.ArrayList

const val DEVICE_INFO =  "0000180A-0000-1000-8000-00805F9B34FB"
const val GENERIC_ACCESS =  "00001800-0000-1000-8000-00805F9B34FB"
const val GENERIC_ATT =  "00001801-0000-1000-8000-00805F9B34FB"

class ServiceArrayAdapter(private val mContext: Context) : BaseExpandableListAdapter() {

    private val charList = ArrayList<BluetoothGattService>()

    override fun getGroupCount(): Int {
        return charList.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return charList[groupPosition].characteristics.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return charList[groupPosition];
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return charList[groupPosition].characteristics[childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong();
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true;
    }

    override fun hasStableIds(): Boolean {
        return false;
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        var listItem: View? = convertView
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.serv_list_item, parent, false)

        val currentChar = charList[groupPosition]

        val name = listItem!!.findViewById(R.id.textView_name) as TextView
        name.text = getUUIDName(currentChar.uuid.toString().toUpperCase())

        val address = listItem.findViewById(R.id.textView_uuid) as TextView
        address.text = currentChar.uuid.toString()

        return listItem
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean,
                                convertView: View?, parent: ViewGroup? ): View {
        var listItem: View? = convertView
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.char_list_item, parent, false)

        val currentChar = charList[groupPosition].characteristics[childPosition]

        val name = listItem!!.findViewById(R.id.textView_name) as TextView
        name.text = currentChar.uuid.toString()?:"null"

        val address = listItem!!.findViewById(R.id.textView_uuid) as TextView
        address.text = currentChar.uuid.toString()

        return listItem
    }

    fun add(c: BluetoothGattService) {
        charList.add(c)
    }

    fun clear() {
        charList.clear()
    }

    fun getUUIDName(uuid: String):String{
        return when (uuid){
            DEVICE_INFO -> "Device Info"
            GENERIC_ACCESS -> "Generic Access"
            GENERIC_ATT -> "Generic Attribute"
            else -> "Custom Service"
        }
    }
}