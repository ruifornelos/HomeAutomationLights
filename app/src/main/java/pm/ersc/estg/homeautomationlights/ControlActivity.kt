package pm.ersc.estg.homeautomationlights

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import pm.ersc.estg.homeautomationlights.ble.ConnectionEventListener
import pm.ersc.estg.homeautomationlights.ble.ConnectionManager
import pm.ersc.estg.homeautomationlights.ble.isNotifiable
import pm.ersc.estg.homeautomationlights.ble.isReadable
import timber.log.Timber

private const val WRITECHARUUID = "4ac8a682-9736-4e5d-932b-e9b31405049c"
private const val READCHARUUID = "0972ef8c-7613-4075-ad52-756f33d4da91"

class ControlActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var mSensorManager : SensorManager
    private var mLuminosity : Sensor?= null
    private var resume = true;
    private lateinit var device: BluetoothDevice
    private lateinit var writeChar: BluetoothGattCharacteristic
    private lateinit var readChar: BluetoothGattCharacteristic
    private var bathroomState: Boolean = false
    private var livingRoomState: Boolean = false
    private var kitchenState: Boolean = false
    private var bedroomState: Boolean = false
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }

    /*******************************************
     * Activity function overrides
     *******************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get bluetooth device from intent
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")

        setContentView(R.layout.activity_control)

        characteristics.forEach { characteristic ->
            if(characteristic.uuid.toString() == WRITECHARUUID)
                writeChar = characteristic
            if(characteristic.uuid.toString() == READCHARUUID) {
                readChar = characteristic
                Timber.d("passei aqui caralho ${readChar.uuid}")
            }
        }

        //Variables (Buttons and Textviews)////////////////////////////////////
        val divisionTextView = findViewById<TextView>(R.id.divisionPrint)
        val luminosityTextView = findViewById<TextView>(R.id.LuminosityLevels)
        val disconnectDevice = findViewById<Button>(R.id.disconnect)
        val bedroomButton = findViewById<ImageButton>(R.id.BedRoomButton)
        val libraryButton = findViewById<ImageButton>(R.id.LibraryButton)
        val kitchenButton = findViewById<ImageButton>(R.id.KitchenButton)
        val bathroomButton = findViewById<ImageButton>(R.id.BathroomButton)
        val switchONOFF = findViewById<Switch>(R.id.switch1)

        //Lighting Sensor //////////////////////////////////////////////////////
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLuminosity = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        //Switch Button ////////////////////////////////////////////////////////
        switchONOFF?.setOnCheckedChangeListener { _, onSwitch ->
            /*if(readChar != null){
                var teste = ConnectionManager.readCharacteristic(device, readChar)
                Timber.d("Bacalhau com natas ${teste.toString()}")
            }*/
            if (onSwitch) {
                /*if (readChar?.isNotifiable()) {
                    var teste = ConnectionManager.readCharacteristic(device,readChar)
                    Toast.makeText(this, "calminha ${teste}", Toast.LENGTH_SHORT).show()
                }else{
                    Timber.d("Not readable... ${readChar.properties}")
                }*/
            } else {
                toast("desliga")
            }
        }

        bathroomButton?.setOnClickListener(){
            divisionTextView.text = getString(R.string.bathroom_selected)
            bathroomState = if(!bathroomState){
                ConnectionManager.writeCharacteristic(device,writeChar,"wcON".toByteArray(Charsets.UTF_8))
                true
            } else {
                ConnectionManager.writeCharacteristic(device,writeChar,"wcOFF".toByteArray(Charsets.UTF_8))
                false
            }
        }

        kitchenButton?.setOnClickListener(){
            divisionTextView.text = getString(R.string.kitchen_selected)
            kitchenState = if(!kitchenState){
                ConnectionManager.writeCharacteristic(device,writeChar,"kitchenON".toByteArray(Charsets.UTF_8))
                true
            } else {
                ConnectionManager.writeCharacteristic(device,writeChar,"kitchenOFF".toByteArray(Charsets.UTF_8))
                false
            }
        }
        libraryButton?.setOnClickListener(){
            divisionTextView.text = getString(R.string.library_selected)
            livingRoomState = if(!livingRoomState){
                ConnectionManager.writeCharacteristic(device,writeChar,"livingON".toByteArray(Charsets.UTF_8))
                true
            } else {
                ConnectionManager.writeCharacteristic(device,writeChar,"livingOFF".toByteArray(Charsets.UTF_8))
                false
            }
        }

        bedroomButton?.setOnClickListener(){
            divisionTextView.text = getString(R.string.bedroom_selected)
            bedroomState = if(!bedroomState){
                ConnectionManager.writeCharacteristic(device,writeChar,"255".toByteArray(Charsets.UTF_8))
                true
            } else {
                ConnectionManager.writeCharacteristic(device,writeChar,"128".toByteArray(Charsets.UTF_8))
                false
            }
        }

        disconnectDevice?.setOnClickListener {
            ConnectionManager.teardownConnection(device)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mLuminosity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        mSensorManager.unregisterListener(this)
        ConnectionManager.unregisterListener(connectionEventListener)
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected or unable to connect to device."
                        positiveButton("OK") {}
                    }.show()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && resume) {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                if(event.values[0] >= 100){
                    // TODO: 17/01/2021 Diminuir brilho
                } else {
                    // TODO: 17/01/2021 Aumentar brilho
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}





