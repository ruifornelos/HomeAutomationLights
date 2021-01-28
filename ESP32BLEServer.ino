#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID "ab0828b1-198e-4351-b779-901fa0e0371e" // UART service UUID
#define CHARACTERISTIC_UUID_BEDROOM "4ac8a682-9736-4e5d-932b-e9b31405049c"
#define CHARACTERISTIC_UUID_BATHROOM "39605286-762c-4c00-af79-56a806c3980c"
#define CHARACTERISTIC_UUID_KITCHEN "ad413ba7-b596-41be-838a-f858e67d1561"
#define CHARACTERISTIC_UUID_LIBRARY "5ef4748f-a961-4a98-8d37-bbba49d22fd2"

///////////////////////////////////////////////////
///          Function Declaration               ///
void ledLuminosity(int room, int dutycycle);     // LEDs Device Driver
float ldrDeviceDriver();                           // LDR Device Driver
///////////////////////////////////////////////////
///            Object Declaration               ///
BLECharacteristic *bedroomChar;
BLECharacteristic *bathroomChar;
BLECharacteristic *kitchenChar;
BLECharacteristic *libraryChar;
///////////////////////////////////////////////////
///              Global Variables               ///
bool deviceConnected = false;
// LDR pin
const int ldr = 36;
int luminosity;
int ldrReading = 1;
String division = "";
// LED's pins
const int bathroom = 23;
const int kitchen = 22;
const int livingRoom = 21;
const int bedroom = 19;
// PWM Frequency, channels and bit resolution
const int freq = 5000;
const int kitchenChannel = 0;
const int bedroomChannel = 1;
const int livingRoomChannel = 2;
const int bathroomChannel = 3;
const int resolution = 8;     // 2^8 = 256
///////////////////////////////////////////////////
///            BLE Server Callbacks             ///
class ServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("Ligado");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("Desligado");
    }
};
///////////////////////////////////////////////////
///        Characteristics Callbacks            ///
class CharacteristicCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
      String rxString = String(rxValue.c_str());
      division = rxString.substring(0, rxString.indexOf(" "));
      String intensity = rxString.substring(rxString.indexOf(" "));
      int value = intensity.toInt();
      intensity.trim();
      Serial.println(value);
      Serial.println(division);

      if (division == "bedroom") {
        if (value != -1) {      // -1 means that the choosen sensor is the LDR
          ldrReading = 1;
          ledLuminosity(1, value);
          pCharacteristic->setValue(value);
        } else {
          ldrReading = 0;
          ledLuminosity(1, int(ldrDeviceDriver()));
          pCharacteristic->setValue(value);
        }
      }
      if (division == "kitchen") {
        if (value != -1) {
          ldrReading = 1;
          ledLuminosity(2, value);
          pCharacteristic->setValue(value);
        } else {
          ldrReading = 0;
          ledLuminosity(2, int(ldrDeviceDriver()));
          pCharacteristic->setValue(value);
        }
      }
      if (division == "bathroom") {
        if (value != -1) {
          ldrReading = 1;
          ledLuminosity(3, value);
          pCharacteristic->setValue(value);
        } else {
          ldrReading = 0;
          ledLuminosity(3, int(ldrDeviceDriver()));
          pCharacteristic->setValue(value);
        }
      }
      if (division == "living") {
        if (value != -1) {
          ldrReading = 1;
          ledLuminosity(4, value);
          pCharacteristic->setValue(value);
        } else {
          ldrReading = 0;
          ledLuminosity(4, int(ldrDeviceDriver()));
          pCharacteristic->setValue(value);
        }
      }
    }
};

void setup() {
  Serial.begin(115200);
  pinMode(bedroom, OUTPUT);
  pinMode(livingRoom, OUTPUT);
  pinMode(kitchen, OUTPUT);
  pinMode(bathroom, OUTPUT);
  pinMode(ldr, INPUT);
  // configure LED PWM functionalities
  ledcSetup(kitchenChannel, freq, resolution);
  ledcSetup(bedroomChannel, freq, resolution);
  ledcSetup(livingRoomChannel, freq, resolution);
  ledcSetup(bathroomChannel, freq, resolution);
  // attach the channel to the GPIO to be controlled
  ledcAttachPin(kitchen, kitchenChannel);
  ledcAttachPin(bedroom, bedroomChannel);
  ledcAttachPin(livingRoom, livingRoomChannel);
  ledcAttachPin(bathroom, bathroomChannel);
  /////////////////////////////////////////////////////
  // Create the BLE Device
  /////////////////////////////////////////////////////
  BLEDevice::init("ESP32-BLE"); // BLE device name
  BLEServer *server = BLEDevice::createServer(); // Create the BLE Server
  server->setCallbacks(new ServerCallbacks()); //sets server's callback
  BLEService *service = server->createService(SERVICE_UUID); // Create the BLE Service with a certain UUID

  // Create BLE Characteristics to receive commands
  bedroomChar = service->createCharacteristic(
                  CHARACTERISTIC_UUID_BEDROOM,
                  BLECharacteristic::PROPERTY_WRITE |
                  BLECharacteristic::PROPERTY_READ
                );

  kitchenChar = service->createCharacteristic(
                  CHARACTERISTIC_UUID_KITCHEN,
                  BLECharacteristic::PROPERTY_WRITE |
                  BLECharacteristic::PROPERTY_READ
                );

  bathroomChar = service->createCharacteristic(
                   CHARACTERISTIC_UUID_BATHROOM,
                   BLECharacteristic::PROPERTY_WRITE |
                   BLECharacteristic::PROPERTY_READ
                 );

  libraryChar = service->createCharacteristic(
                  CHARACTERISTIC_UUID_LIBRARY,
                  BLECharacteristic::PROPERTY_WRITE |
                  BLECharacteristic::PROPERTY_READ
                );
  // Callback's settings
  bedroomChar->setCallbacks(new CharacteristicCallbacks());
  kitchenChar->setCallbacks(new CharacteristicCallbacks());
  bathroomChar->setCallbacks(new CharacteristicCallbacks());
  libraryChar->setCallbacks(new CharacteristicCallbacks());
  //Characteristics Initialization
  bedroomChar->setValue("0");
  bathroomChar->setValue("0");
  kitchenChar->setValue("0");
  libraryChar->setValue("0");
  // Start the service
  service->start();
  // Start advertising (descoberta do ESP32)
  server->getAdvertising()->start();
}

void loop() {
  if (ldrReading == 0) {
    luminosity = ldrDeviceDriver();
    if (luminosity > 0) {
      if (division == "bedroom") {
        ledLuminosity(1, int(luminosity));
      }
      if (division == "bathroom") {
        ledLuminosity(3, int(luminosity));
      }
      if (division == "kitchen") {
        ledLuminosity(2, int(luminosity));
      }
      if (division == "living") {
        ledLuminosity(4, int(luminosity));
      }
    } else {
      if (division == "bedroom") {
        ledLuminosity(1, 0);
      }
      if (division == "bathroom") {
        ledLuminosity(3, 0);
      }
      if (division == "kitchen") {
        ledLuminosity(2, 0);
      }
      if (division == "living") {
        ledLuminosity(4, 0);
      }
    }
  }
}

/* Leds Device Driver
    Device driver that will change led state/brightness
   Inputs:
      room - division selected
      dutycycle - brightness pretended
   Output: none
*/
void ledLuminosity(int room, int dutycycle) {
  switch (room) {
    case 1:
      ledcWrite(bedroomChannel, dutycycle);
      break;
    case 2:
      ledcWrite(kitchenChannel, dutycycle);
      break;
    case 3:
      ledcWrite(bathroomChannel, dutycycle);
      break;
    case 4:
      ledcWrite(livingRoomChannel, dutycycle);
      break;
    default:
      break;
  }
}
/* LDR device driver
    Reads analog value and maps it to value between
   Input: nothing
   Output: Luminosity Value maped for PWM
*/
float ldrDeviceDriver() {
  int valorLDR = analogRead(ldr);  // reads analog value of LDR sensor
  float luminosidade = map(valorLDR, 0, 4095, 0, 255); // maps analog value into PWM
  Serial.print("LDR: ");
  Serial.println(luminosidade);
  return luminosidade;
}
