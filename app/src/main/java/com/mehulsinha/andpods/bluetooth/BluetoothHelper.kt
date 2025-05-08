package com.mehulsinha.andpods.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.annotation.RequiresPermission
import com.mehulsinha.andpods.utils.Logger

class BluetoothHelper(private val context: Context) {
    var logger = Logger.getLogger<Any>()
    @SuppressLint("MissingPermission")
    fun getConnectedDeviceInfo(): String {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            logger.info("Bluetooth is not enabled.")
            return "Bluetooth is not enabled."
        }

        val connectedDevices = bluetoothAdapter.bondedDevices
        if (connectedDevices.isEmpty()) {
            logger.info("No connected Bluetooth devices found.")
            return "No connected Bluetooth devices found."
        }

        val deviceInfo = connectedDevices.joinToString("\n\n") { device ->
            val info = "Name: ${device.name}\n" +
                    "Address: ${device.address}\n" +
                    "Type: ${getDeviceType(device)}\n" +
                    "Bond State: ${getBondState(device)}"
            logger.info(info)
            info
        }
        return deviceInfo
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun getDeviceType(device: BluetoothDevice): String {
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode"
            else -> "Unknown"
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun getBondState(device: BluetoothDevice): String {
        return when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> "Bonded"
            BluetoothDevice.BOND_BONDING -> "Bonding"
            BluetoothDevice.BOND_NONE -> "Not Bonded"
            else -> "Unknown"
        }
    }
}