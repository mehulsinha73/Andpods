package com.mehulsinha.andpods.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.mehulsinha.andpods.R

class DeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private val listener: OnDeviceClickListener
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    interface OnDeviceClickListener {
        fun onDeviceClick(device: BluetoothDevice)
    }

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.deviceNameTextView)
        val addressTextView: TextView = view.findViewById(R.id.deviceAddressTextView)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeviceClick(devices[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]

        if (ActivityCompat.checkSelfPermission(
                holder.itemView.context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_CONNECT
                } else {
                    Manifest.permission.BLUETOOTH
                }
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            holder.nameTextView.text = "Unavailable (No permission)"
            holder.addressTextView.text = "Unavailable (No permission)"
            return
        }

        val deviceName = device.name ?: "Unknown Device"
        holder.nameTextView.text = deviceName
        holder.addressTextView.text = device.address
    }

    override fun getItemCount() = devices.size
}