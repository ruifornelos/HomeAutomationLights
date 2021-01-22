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
import pm.ersc.estg.homeautomationlights.ble.ConnectionEventListener
import pm.ersc.estg.homeautomationlights.ble.ConnectionManager
import timber.log.Timber

private const val BEDROOMCHARUUID = "4ac8a682-9736-4e5d-932b-e9b31405049c"
private const val READCHARUUID = "0972ef8c-7613-4075-ad52-756f33d4da91"

class ControlActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var mSensorManager: SensorManager
    private var mLuminosity: Sensor? = null
    private var resume = false;
    private lateinit var device: BluetoothDevice
    private lateinit var bedroomChar: BluetoothGattCharacteristic
    private lateinit var readChar: BluetoothGattCharacteristic
    private var bathroomState: Boolean = false
    private var livingRoomState: Boolean = false
    private var kitchenState: Boolean = false
    private var bedroomState: Boolean = false
    private var roomSelected: Int = 0
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
            if (characteristic.uuid.toString() == BEDROOMCHARUUID)
                bedroomChar = characteristic
            if (characteristic.uuid.toString() == READCHARUUID) {
                readChar = characteristic
            }
        }

        ConnectionManager.readCharacteristic(device, bedroomChar)

        //Variables (Buttons and Textviews)////////////////////////////////////
        val divisionTextView = findViewById<TextView>(R.id.divisionPrint)
        val switchPower = findViewById<Switch>(R.id.switchPower)
        val disconnectDevice = findViewById<Button>(R.id.disconnect)
        val bedroomButton = findViewById<ImageButton>(R.id.BedRoomButton)
        val libraryButton = findViewById<ImageButton>(R.id.LibraryButton)
        val kitchenButton = findViewById<ImageButton>(R.id.KitchenButton)
        val bathroomButton = findViewById<ImageButton>(R.id.BathroomButton)
        val switchMode = findViewById<Switch>(R.id.switchMode)
        //Lighting Sensor //////////////////////////////////////////////////////
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLuminosity = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        //SwitchPower Button ////////////////////////////////////////////////////////
        switchPower?.setOnCheckedChangeListener { _, onSwitch ->
            if (onSwitch) {
                if (roomSelected == 0) {
                    Toast.makeText(this, getString(R.string.noRoomSelected), Toast.LENGTH_SHORT)
                        .show()
                } else {
                    when (roomSelected) {
                        1 -> {
                            if (!bathroomState) {
                                ledStateChange(roomSelected)
                                Toast.makeText(this, getString(R.string.ledsON), Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        2 -> {
                            if (!kitchenState) {
                                ledStateChange(roomSelected)
                                Toast.makeText(this, getString(R.string.ledsON), Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        3 -> {
                            if (!livingRoomState) {
                                ledStateChange(roomSelected)
                                Toast.makeText(this, getString(R.string.ledsON), Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        4 -> {
                            if (!bedroomState) {
                                ledStateChange(roomSelected)
                                Toast.makeText(this, getString(R.string.ledsON), Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            } else {
                if (roomSelected == 0) {
                    Toast.makeText(this, getString(R.string.noRoomSelected), Toast.LENGTH_SHORT)
                        .show()
                } else {
                    ledStateChange(roomSelected)
                    Toast.makeText(this, getString(R.string.ledsOFF), Toast.LENGTH_SHORT).show()
                }

            }
        }
        //SwitchMode Button ////////////////////////////////////////////////////////
        switchMode?.setOnCheckedChangeListener { _, onSwitch ->
            if (onSwitch) {
                if (roomSelected == 0) {
                    Toast.makeText(this, getString(R.string.noRoomSelected), Toast.LENGTH_SHORT)
                        .show()
                } else {
                    resume = true
                    mSensorManager.registerListener(
                        this,
                        mLuminosity,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                    Toast.makeText(this, getString(R.string.automatic_mode), Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                resume = false
                Toast.makeText(this, getString(R.string.manual_mode), Toast.LENGTH_SHORT).show()
            }
        }

        bathroomButton?.setOnClickListener() {
            divisionTextView.text = getString(R.string.bathroom_selected)
            roomSelected = 1
            switchPower.isChecked = bathroomState
        }

        kitchenButton?.setOnClickListener() {
            divisionTextView.text = getString(R.string.kitchen_selected)
            roomSelected = 2
            switchPower.isChecked = kitchenState
        }
        libraryButton?.setOnClickListener() {
            divisionTextView.text = getString(R.string.library_selected)
            roomSelected = 3
            switchPower.isChecked = livingRoomState
        }

        bedroomButton?.setOnClickListener() {
            divisionTextView.text = getString(R.string.bedroom_selected)
            roomSelected = 4
            switchPower.isChecked = bedroomState
        }

        disconnectDevice?.setOnClickListener {
            ConnectionManager.teardownConnection(device)
            roomSelected = 0
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
                Timber.d("Lido: ${event.values[0]} | Conv: ${map(event.values[0].toInt(),0,1300,255,0)}")
                if (event.values[0] >= 100) {
                    // TODO: 17/01/2021 Diminuir brilho
                } else {
                    // TODO: 17/01/2021 Aumentar brilho
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun map(x: Int, in_min: Int, in_max: Int, out_min: Int, out_max: Int): Int {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
    }

    private fun ledStateChange(room: Int) {
        when (room) {
            1 -> {
                bathroomState = if (!bathroomState) {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "bathroom 255".toByteArray(Charsets.UTF_8)
                    )
                    true
                } else {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "bathroom 0".toByteArray(Charsets.UTF_8)
                    )
                    false
                }
            }
            2 -> {
                kitchenState = if (!kitchenState) {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "kitchen 255".toByteArray(Charsets.UTF_8)
                    )
                    true
                } else {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "kitchen 0".toByteArray(Charsets.UTF_8)
                    )
                    false
                }
            }
            3 -> {
                livingRoomState = if (!livingRoomState) {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "living 255".toByteArray(Charsets.UTF_8)
                    )
                    true
                } else {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "living 0".toByteArray(Charsets.UTF_8)
                    )
                    false
                }
            }
            4 -> {
                bedroomState = if (!bedroomState) {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "bedroom 255".toByteArray(Charsets.UTF_8)
                    )
                    true
                } else {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "bedroom 0".toByteArray(Charsets.UTF_8)
                    )
                    false
                }
            }
            0 -> Toast.makeText(this, getString(R.string.noRoomSelected), Toast.LENGTH_SHORT).show()
        }
    }
}





