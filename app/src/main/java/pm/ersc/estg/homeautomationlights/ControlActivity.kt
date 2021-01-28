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
private const val KITCHENCHARUUID = "ad413ba7-b596-41be-838a-f858e67d1561"
private const val BATHROOMCHARUUID = "39605286-762c-4c00-af79-56a806c3980c"
private const val LIBRARYCHARUUID = "5ef4748f-a961-4a98-8d37-bbba49d22fd2"

class ControlActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var mSensorManager: SensorManager
    private var mLuminosity: Sensor? = null
    private var resume = false;
    private lateinit var device: BluetoothDevice
    private lateinit var bedroomChar: BluetoothGattCharacteristic
    private lateinit var kitchenChar: BluetoothGattCharacteristic
    private lateinit var bathroomChar: BluetoothGattCharacteristic
    private lateinit var libraryChar: BluetoothGattCharacteristic
    private var bathroomState: Boolean = false
    private var livingRoomState: Boolean = false
    private var kitchenState: Boolean = false
    private var bedroomState: Boolean = false
    private var roomSelected: Int = 0
    private var isSensorPhone: Boolean = false
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

        /*
        Stores characteristics pointers in class attribute
         */
        characteristics.forEach { characteristic ->
            if (characteristic.uuid.toString() == BEDROOMCHARUUID)
                bedroomChar = characteristic
            if (characteristic.uuid.toString() == KITCHENCHARUUID)
                kitchenChar = characteristic
            if (characteristic.uuid.toString() == BATHROOMCHARUUID)
                bathroomChar = characteristic
            if (characteristic.uuid.toString() == LIBRARYCHARUUID)
                libraryChar = characteristic
        }

        //Variables (Buttons and Textviews)////////////////////////////////////
        val divisionTextView = findViewById<TextView>(R.id.divisionPrint)
        val switchPower = findViewById<Switch>(R.id.switchPower)
        val disconnectDevice = findViewById<Button>(R.id.disconnect)
        val bedroomButton = findViewById<ImageButton>(R.id.BedRoomButton)
        val libraryButton = findViewById<ImageButton>(R.id.LibraryButton)
        val kitchenButton = findViewById<ImageButton>(R.id.KitchenButton)
        val bathroomButton = findViewById<ImageButton>(R.id.BathroomButton)
        val sensorPhone = findViewById<Button>(R.id.PhoneSensor)
        val sensorLDR = findViewById<Button>(R.id.OnFieldSensor)
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
                            //if (bathroomState == false) {
                                ledStateChange(roomSelected, 255)
                                Toast.makeText(this, getString(R.string.ledsON), Toast.LENGTH_SHORT)
                                    .show()
                                bathroomState = true
                            //}
                        }
                        2 -> {
                            //if (kitchenState == false) {
                                ledStateChange(roomSelected, 255)
                                Toast.makeText(this, getString(R.string.ledsON), Toast.LENGTH_SHORT)
                                    .show()
                                kitchenState = true
                            //}
                        }
                        3 -> {
                            //if (livingRoomState == false) {
                                ledStateChange(roomSelected, 255)
                                Toast.makeText(this, getString(R.string.ledsON), Toast.LENGTH_SHORT)
                                    .show()
                                livingRoomState = true
                            //}
                        }
                        4 -> {
                            //if (bedroomState == false) {
                                ledStateChange(roomSelected, 255)
                                Toast.makeText(this, getString(R.string.ledsON), Toast.LENGTH_SHORT)
                                    .show()
                                bedroomState = true
                            //}
                        }
                    }
                }
            } else {
                when (roomSelected) {
                    1 -> {

                        ledStateChange(roomSelected, 0)
                        Toast.makeText(this, getString(R.string.ledsOFF), Toast.LENGTH_SHORT)
                            .show()
                        bathroomState = false
                    }
                    2 -> {
                        ledStateChange(roomSelected, 0)
                        Toast.makeText(this, getString(R.string.ledsOFF), Toast.LENGTH_SHORT)
                            .show()
                        kitchenState = false

                    }
                    3 -> {

                        ledStateChange(roomSelected, 0)
                        Toast.makeText(this, getString(R.string.ledsOFF), Toast.LENGTH_SHORT)
                            .show()
                        livingRoomState = false

                    }
                    4 -> {
                        ledStateChange(roomSelected, 0)
                        Toast.makeText(this, getString(R.string.ledsOFF), Toast.LENGTH_SHORT)
                            .show()
                        bedroomState = false

                    }
                }
                isSensorPhone = false
                resume = false
                mSensorManager.unregisterListener(this)
                ledStateChange(roomSelected, 0)
                Toast.makeText(this, getString(R.string.ledsOFF), Toast.LENGTH_SHORT).show()
            }
        }
        //Sensor Phone Button ////////////////////////////////////////////////////////
        sensorPhone?.setOnClickListener() {
            if (roomSelected == 0) {
                Toast.makeText(this, getString(R.string.noRoomSelected), Toast.LENGTH_SHORT)
                    .show()
            } else if (!isSensorPhone) {
                resume = true
                mSensorManager.registerListener(
                    this,
                    mLuminosity,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                Toast.makeText(this, getString(R.string.automatic_mode), Toast.LENGTH_SHORT)
                    .show()
            } else if (isSensorPhone) {
                resume = false
                isSensorPhone = false
                Toast.makeText(this, "Phone Sensor deactivated", Toast.LENGTH_SHORT).show()
            }
        }
        //On field Sensor Button ////////////////////////////////////////////////////////
        sensorLDR?.setOnClickListener() {
            isSensorPhone = false
            resume = false
            mSensorManager.unregisterListener(this)
            if (roomSelected != 0) ledStateChange(roomSelected, -1)
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
                Timber.d(
                    "Lido: ${event.values[0]} | Conv: ${
                        map(
                            event.values[0].toInt(),
                            0,
                            1300,
                            255,
                            0
                        )
                    }"
                )
                if (map(event.values[0].toInt(), 0, 3000, 255, 0) > 0) {
                    ledStateChange(roomSelected, map(event.values[0].toInt(), 0, 1300, 255, 0))
                } //else ledStateChange(roomSelected, 0)
            }
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun map(x: Int, in_min: Int, in_max: Int, out_min: Int, out_max: Int): Int {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
    }

    private fun ledStateChange(room: Int, lum: Int) {
        when (room) {
            1 -> {
                bathroomState = if (!bathroomState) {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bathroomChar,
                        "bathroom $lum".toByteArray(Charsets.UTF_8)
                    )
                    true
                } else {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bathroomChar,
                        "bathroom $lum".toByteArray(Charsets.UTF_8)
                    )
                    false
                }
            }
            2 -> {
                kitchenState = if (!kitchenState) {
                    ConnectionManager.writeCharacteristic(
                        device,
                        kitchenChar,
                        "kitchen $lum".toByteArray(Charsets.UTF_8)
                    )
                    true
                } else {
                    ConnectionManager.writeCharacteristic(
                        device,
                        kitchenChar,
                        "kitchen $lum".toByteArray(Charsets.UTF_8)
                    )
                    false
                }
            }
            3 -> {
                livingRoomState = if (!livingRoomState) {
                    ConnectionManager.writeCharacteristic(
                        device,
                        libraryChar,
                        "living $lum".toByteArray(Charsets.UTF_8)
                    )
                    true
                } else {
                    ConnectionManager.writeCharacteristic(
                        device,
                        libraryChar,
                        "living $lum".toByteArray(Charsets.UTF_8)
                    )
                    false
                }
            }
            4 -> {
                bedroomState = if (!bedroomState) {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "bedroom $lum".toByteArray(Charsets.UTF_8)
                    )
                    true
                } else {
                    ConnectionManager.writeCharacteristic(
                        device,
                        bedroomChar,
                        "bedroom $lum".toByteArray(Charsets.UTF_8)
                    )
                    false
                }
            }
            0 -> Toast.makeText(this, getString(R.string.noRoomSelected), Toast.LENGTH_SHORT).show()
        }
    }
}





