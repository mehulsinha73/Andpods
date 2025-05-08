package com.mehulsinha.andpods

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mehulsinha.andpods.bluetooth.DeviceAdapter
import com.mehulsinha.andpods.bluetooth.DeviceLogger
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), DeviceAdapter.OnDeviceClickListener {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var scanButton: Button
    private lateinit var statusTextView: TextView
    private val deviceList = mutableListOf<BluetoothDevice>()
    private val connectedGatts = mutableMapOf<String, BluetoothGatt>()
    private val loggingHandlers = mutableMapOf<String, Handler>()
    private val deviceLoggers = mutableMapOf<String, DeviceLogger>()
    private val SCAN_PERIOD: Long = 10000

    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startScan()
        } else {
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startScan()
        } else {
            Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            if (!deviceList.contains(device)) {
                deviceList.add(device)
                deviceAdapter.notifyItemInserted(deviceList.size - 1)
                Log.d(TAG, "Found device: ${device.address}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        statusTextView = findViewById(R.id.statusTextView)
        scanButton = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            if (hasPermissions()) {
                if (bluetoothAdapter.isEnabled) {
                    startScan()
                } else {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                }
            } else {
                requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
            }
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.deviceRecyclerView)
        deviceAdapter = DeviceAdapter(deviceList, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = deviceAdapter
    }

    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_SCAN
                } else {
                    Manifest.permission.BLUETOOTH
                }
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        deviceList.clear()
        deviceAdapter.notifyDataSetChanged()
        statusTextView.text = "Scanning..."

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothAdapter.bluetoothLeScanner.startScan(null, scanSettings, scanCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            stopScan()
        }, SCAN_PERIOD)
    }

    private fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_SCAN
                } else {
                    Manifest.permission.BLUETOOTH
                }
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        statusTextView.text = "Scan completed. Found ${deviceList.size} devices."
    }

    override fun onDeviceClick(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_CONNECT
                } else {
                    Manifest.permission.BLUETOOTH
                }
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val deviceName = device.name ?: "Unknown Device"
        val deviceAddress = device.address

        Toast.makeText(this, "Connecting to $deviceName", Toast.LENGTH_SHORT).show()

        // Check if already connected
        if (connectedGatts.containsKey(deviceAddress)) {
            Toast.makeText(this, "Already connected to $deviceName", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the logger
        val logger = DeviceLogger(this, deviceAddress, deviceName)
        deviceLoggers[deviceAddress] = logger

        // Set up periodic logging
        val handler = Handler(Looper.getMainLooper())
        loggingHandlers[deviceAddress] = handler

        // Connect to the device
        val gatt = device.connectGatt(this, false, gattCallback)
        connectedGatts[deviceAddress] = gatt
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Manifest.permission.BLUETOOTH_CONNECT
                    } else {
                        Manifest.permission.BLUETOOTH
                    }
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val deviceName = gatt.device.name ?: "Unknown Device"

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server for $deviceName")
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Connected to $deviceName",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Discover services after connected
                gatt.discoverServices()

                // Start logging
                startLogging(deviceAddress)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server for $deviceName")
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Disconnected from $deviceName",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Stop logging
                stopLogging(deviceAddress)

                // Remove from connected devices
                connectedGatts.remove(deviceAddress)
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Manifest.permission.BLUETOOTH_CONNECT
                        } else {
                            Manifest.permission.BLUETOOTH
                        }
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                val deviceName = gatt.device.name ?: "Unknown Device"
                val deviceAddress = gatt.device.address

                Log.i(TAG, "Discovered services for $deviceName")

                // Log discovered services and characteristics
                val logger = deviceLoggers[deviceAddress]
                logger?.logInfo("Services discovered")

                for (service in gatt.services) {
                    logger?.logInfo("Service: ${service.uuid}")

                    for (characteristic in service.characteristics) {
                        logger?.logInfo("  Characteristic: ${characteristic.uuid}")

                        // Enable notifications for characteristics that support it
                        val properties = characteristic.properties
                        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            gatt.setCharacteristicNotification(characteristic, true)
                            logger?.logInfo("  Enabled notifications for ${characteristic.uuid}")
                        }
                    }
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val deviceAddress = gatt.device.address
                val value = characteristic.value
                val valueString = value?.let { String(it) } ?: "null"

                val logger = deviceLoggers[deviceAddress]
                logger?.logData("Read from ${characteristic.uuid}: $valueString")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val deviceAddress = gatt.device.address
            val value = characteristic.value
            val valueString = value?.let { String(it) } ?: "null"

            val logger = deviceLoggers[deviceAddress]
            logger?.logData("Notification from ${characteristic.uuid}: $valueString")
        }
    }

    private fun startLogging(deviceAddress: String) {
        val handler = loggingHandlers[deviceAddress] ?: return
        val logger = deviceLoggers[deviceAddress] ?: return
        val gatt = connectedGatts[deviceAddress] ?: return

        logger.logInfo("Started continuous logging")

        // Set up a repeating task to read characteristics and log data
        handler.post(object : Runnable {
            override fun run() {
                if (connectedGatts.containsKey(deviceAddress)) {
                    // Log basic device info periodically
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Manifest.permission.BLUETOOTH_CONNECT
                            } else {
                                Manifest.permission.BLUETOOTH
                            }
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }

                    val deviceName = gatt.device.name ?: "Unknown Device"
                    val rssi = gatt.readRemoteRssi()

                    logger.logInfo("Connection check for $deviceName, RSSI: $rssi")

                    // Schedule the next check
                    handler.postDelayed(this, 5000) // Every 5 seconds
                }
            }
        })
    }

    private fun stopLogging(deviceAddress: String) {
        val handler = loggingHandlers[deviceAddress]
        val logger = deviceLoggers[deviceAddress]

        // Stop the handler callbacks
        handler?.removeCallbacksAndMessages(null)

        // Log disconnect event
        logger?.logInfo("Stopped logging due to disconnection")

        // Clean up
        loggingHandlers.remove(deviceAddress)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Close all connections and clean up
        for (gatt in connectedGatts.values) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Manifest.permission.BLUETOOTH_CONNECT
                    } else {
                        Manifest.permission.BLUETOOTH
                    }
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continue
            }
            gatt.close()
        }

        connectedGatts.clear()

        // Stop all logging handlers
        for (handler in loggingHandlers.values) {
            handler.removeCallbacksAndMessages(null)
        }

        loggingHandlers.clear()

        // Close all loggers
        for (logger in deviceLoggers.values) {
            logger.close()
        }

        deviceLoggers.clear()
    }

    companion object {
        private const val TAG = "BluetoothLogger"
    }
}