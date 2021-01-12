package pm.ersc.estg.homeautomationlights

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.jetbrains.anko.toast
import java.util.*
import java.util.jar.Manifest


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = "****"
        const val REQUEST_ENABLE_BT = 1
        val FINE_LOC_RQ = 101
        const val UUIDservice = "da8753f9-b3f3-4203-a589-00c2ce38f044"
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Variables (Buttons and Textviews)////////////////////////////////////
        val divisionTextView = findViewById<TextView>(R.id.divisionPrint)
        val luminosityTextView = findViewById<TextView>(R.id.LuminosityLevels)
        val connectDevice = findViewById<Button>(R.id.connect)
        val disconnectDevice = findViewById<Button>(R.id.disconnect)
        val bedroomButton = findViewById<ImageButton>(R.id.BedRoomButton)
        val libraryButton = findViewById<ImageButton>(R.id.LibraryButton)
        val kitchenButton = findViewById<ImageButton>(R.id.KitchenButton)
        val bathroomButton = findViewById<ImageButton>(R.id.BathroomButton)
        val switchONOFF = findViewById<Switch>(R.id.switch1)
        /////////////////////////////////////////////////////////////////////////

        //Switch Button ////////////////////////////////////////////////////////
        switchONOFF?.setOnCheckedChangeListener { _, onSwitch ->
            if (onSwitch) {
                toast("liga")
            } else {
                toast("desliga")
            }
        }

        //Scanner Button
        connectDevice?.setOnClickListener() { }

        bathroomButton?.setOnClickListener(){
            divisionTextView.setText("The light from Bathroom was selected")
            //lalalalalalalalal
        }

        kitchenButton?.setOnClickListener(){
            divisionTextView.setText("The light from Kitchen was selected")
            //lalalalalalalalal
        }
        libraryButton?.setOnClickListener(){
            divisionTextView.setText("The light from Library was selected")
            //lalalalalalalalal
        }

        bedroomButton?.setOnClickListener(){
            divisionTextView.setText("The light from Bedroom was selected")
            //lalalalalalalalal
        }
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

        if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            toast("Permission for location is enabled!")
            bluetoothAdapter.bluetoothLeScanner.startScan(filters, settings, bleScanCallback)
        } else {

            if(shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)){
                toast("Permission of location needed")
            }
            //requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION} , FINE_LOC_RQ)
        }
    }


    private val bleScanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result -> deviceFound(result.device) }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { deviceFound(result.device) }
            Log.v(TAG, "Founded Device : " + "${result?.device}")
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





