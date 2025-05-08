package com.mehulsinha.andpods.bluetooth


import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceLogger(
    private val context: Context,
    private val deviceAddress: String,
    private val deviceName: String
) {
    private val infoLogFile: File
    private val dataLogFile: File
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    init {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeDeviceName = deviceName.replace(" ", "_").replace(":", "")
        val dirName = "${safeDeviceName}_${deviceAddress.replace(":", "")}"

        // Create directory for this device
        val dir = File(context.getExternalFilesDir(null), dirName)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        infoLogFile = File(dir, "${timestamp}_info.log")
        dataLogFile = File(dir, "${timestamp}_data.log")

        // Write header
        FileOutputStream(infoLogFile, true).use { fos ->
            fos.write("=== BLUETOOTH DEVICE INFO LOG ===\n".toByteArray())
            fos.write("Device: $deviceName\n".toByteArray())
            fos.write("Address: $deviceAddress\n".toByteArray())
            fos.write("Started: ${sdf.format(Date())}\n".toByteArray())
            fos.write("==============================\n\n".toByteArray())
        }

        FileOutputStream(dataLogFile, true).use { fos ->
            fos.write("=== BLUETOOTH DEVICE DATA LOG ===\n".toByteArray())
            fos.write("Device: $deviceName\n".toByteArray())
            fos.write("Address: $deviceAddress\n".toByteArray())
            fos.write("Started: ${sdf.format(Date())}\n".toByteArray())
            fos.write("==============================\n\n".toByteArray())
        }

        Log.d(TAG, "Logger initialized for $deviceName ($deviceAddress)")
    }

    fun logInfo(message: String) {
        val timestamp = sdf.format(Date())
        val logMessage = "[$timestamp] $message\n"

        try {
            FileOutputStream(infoLogFile, true).use { fos ->
                fos.write(logMessage.toByteArray())
            }
            Log.d(TAG, "[$deviceName] INFO: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to info log: ${e.message}")
        }
    }

    fun logData(message: String) {
        val timestamp = sdf.format(Date())
        val logMessage = "[$timestamp] $message\n"

        try {
            FileOutputStream(dataLogFile, true).use { fos ->
                fos.write(logMessage.toByteArray())
            }
            Log.d(TAG, "[$deviceName] DATA: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to data log: ${e.message}")
        }
    }

    fun close() {
        val timestamp = sdf.format(Date())

        try {
            FileOutputStream(infoLogFile, true).use { fos ->
                fos.write("\n=== LOG CLOSED ===\n".toByteArray())
                fos.write("Closed: $timestamp\n".toByteArray())
            }

            FileOutputStream(dataLogFile, true).use { fos ->
                fos.write("\n=== LOG CLOSED ===\n".toByteArray())
                fos.write("Closed: $timestamp\n".toByteArray())
            }

            Log.d(TAG, "Logger closed for $deviceName ($deviceAddress)")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing logs: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "DeviceLogger"
    }
}