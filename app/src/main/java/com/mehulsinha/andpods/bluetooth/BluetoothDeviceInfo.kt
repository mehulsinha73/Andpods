package com.mehulsinha.andpods.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile

data class BluetoothDeviceInfo(
    val device: BluetoothDevice,
    val connectedProfiles: List<Int> = emptyList()
) {
    companion object {
        fun getProfileName(profile: Int): String {
            return when (profile) {
                BluetoothProfile.HEADSET -> "Headset"
                BluetoothProfile.A2DP -> "A2DP (Audio)"
                BluetoothProfile.HEALTH -> "Health"
                BluetoothProfile.HID_DEVICE -> "HID (Input Device)"
                BluetoothProfile.GATT -> "GATT"
                BluetoothProfile.GATT_SERVER -> "GATT Server"
                else -> "Unknown Profile ($profile)"
            }
        }
    }
}