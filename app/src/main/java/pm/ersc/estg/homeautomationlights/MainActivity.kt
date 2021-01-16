package pm.ersc.estg.homeautomationlights

import android.Manifest
import android.app.Activity
import android.app.admin.ConnectEvent
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import org.jetbrains.anko.Android
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import pm.ersc.estg.homeautomationlights.ble.ConnectionManager
import timber.log.Timber
import java.util.*

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val TAG = "****"
private const val ServUUID = "ab0828b1-198e-4351-b779-901fa0e0371e"
private const val CHARUUID = "4ac8a682-9736-4e5d-932b-e9b31405049c"
private const val RED = 0x11

class MainActivity : AppCompatActivity() {
    private var blueDevice: BluetoothDevice? = null
    private lateinit var ledCharacteristic: BluetoothGattCharacteristic

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var isScanning = false
    /*set(value) {
        field = value
        runOnUiThread { connectDevice.text = if (value) "Stop Scan" else "Start Scan" }
    }*/

    private val scanResults = mutableListOf<ScanResult>()

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    /*******************************************
     * Activity function overrides
     *******************************************/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        //Variables (Buttons and Textviews)////////////////////////////////////
        val connectedtextview = findViewById<TextView>(R.id.connectedDevice)
        val connectDevice = findViewById<Button>(R.id.selectdevice)
        val DisconnectDevice = findViewById<Button>(R.id.disconnect_from_device)
        val RedSelection = findViewById<Button>(R.id.REDButton)
        val GreenSelection = findViewById<Button>(R.id.GREENButton)
        val BlueSelection = findViewById<Button>(R.id.BLUEButton)
        val SwitchONOFF = findViewById<Switch>(R.id.switch1)
        /////////////////////////////////////////////////////////////////////////

        //Switch Button ////////////////////////////////////////////////////////
        SwitchONOFF?.setOnCheckedChangeListener { _, onSwitch ->
            if (onSwitch) {
                toast("liga")
            } else {
                toast("desliga")
            }
        }

        //Scanner Button
        connectDevice?.setOnClickListener() { if (isScanning) stopBleScan() else startBleScan() }

        //Color Buttons and actions
        RedSelection?.setOnClickListener() {
            //ConnectionManager.writeCharacteristic(blueDevice, ,0x11)

        }

        GreenSelection?.setOnClickListener() { toast("Green Color") }
        BlueSelection?.setOnClickListener() { toast("Blue Color") }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        } else {
            scanResults.clear()
            //scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            alert {
                title = "Location permission required"
                message = "Starting from Android M (6.0), the system requires apps to be granted " +
                        "location access in order to scan for BLE devices."
                isCancelable = false
                positiveButton(android.R.string.ok) {
                    requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }.show()
        }
    }

    /*******************************************
     * Callback bodies
     *******************************************/

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                //scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Timber.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                    if(name == "ESP32-BLE"){
                        blueDevice = this
                        stopBleScan()
                        ConnectionManager.connect(this, this@MainActivity)
                    }
                }
                scanResults.add(result)
                //scanResultAdapter.notifyItemInserted(scanResults.size - 1)

            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }

    /*******************************************
     * Extension functions
     *******************************************/

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }
}





