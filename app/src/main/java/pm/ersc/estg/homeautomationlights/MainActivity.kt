package pm.ersc.estg.homeautomationlights

import android.Manifest
import android.app.admin.ConnectEvent
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
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
import org.jetbrains.anko.toast
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = "****"
        const val REQUEST_ENABLE_BT = 1
        const val UUIDservice = "ab0828b1-198e-4351-b779-901fa0e0371e"
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Variables (Buttons and Textviews)////////////////////////////////////
        val connectedtextview = findViewById<TextView>(R.id.connectedDevice)
        val ConnectDevice = findViewById<Button>(R.id.selectdevice)
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
        ConnectDevice?.setOnClickListener() { }

        //Color Buttons and actions
        RedSelection?.setOnClickListener() { toast("Red Color") }
        GreenSelection?.setOnClickListener() { toast("Green Color") }
        BlueSelection?.setOnClickListener() { toast("Blue Color") }
    }


    override fun onResume() {
        super.onResume()
        if (bluetoothAdapter.isEnabled) {
            toast("Bluetooth Already Enabled!")
            startBleScan(this, UUID.fromString(UUIDservice))
        } else {
            Log.v(TAG, "BT is disabled!")
            val btIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(btIntent, REQUEST_ENABLE_BT)
        }
    }


    private fun startBleScan(context: Context, serviceUUID: UUID) {
        Log.v(TAG, "Start Scan pt1")

        val uuid = ParcelUuid(serviceUUID)
        val filter = ScanFilter.Builder().setServiceUuid(uuid).build()
        val filters = listOf(filter)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        Log.v(TAG, "Start Scan pt2")

        context.runWithPermissions(Manifest.permission.ACCESS_FINE_LOCATION){
            bluetoothAdapter.bluetoothLeScanner.startScan(filters, settings, bleScanCallback)
        }

    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        results?.forEach{ result -> deviceFound(result.device)}
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            //result?.let { deviceFound(result.device) }
            Log.v(TAG,"Founded Device : " + "${result?.device}")
        }

        override fun onScanFailed(errorCode: Int) {
           Log.v(TAG, "Scan Failed to execute")
        }

    }

    private fun deviceFound(device: BluetoothDevice){
        device.connectGatt(this, true, gattCallback)
    }


    private val gattCallback = object : BluetoothGattCallback(){
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED){
                gatt?.requestMtu(256)
                gatt?.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
        }

    }
}





