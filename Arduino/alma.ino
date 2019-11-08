#include <DHT.h>
#include <DHT_U.h>
#include <WiFiEsp.h>
#include <SoftwareSerial.h>
#include <PubSubClient.h>
#include <SPI.h>
#include <MFRC522.h>

//PINOS SET:
const int pinoSensor = A0;
const int ledBrancoPin =  5;
const int ledColoridoPin =  6;
const int exaustorPin =  7; 
const int motorRegaPin = 8; 
const int higPin = A1;

//PINOS ESTADOS:
int ledColoridoState = HIGH;
int ledBrancoState = HIGH;
int exaustorState = HIGH;
int motorRegaState = LOW;
int higrometroState = LOW;

// HIGROMETRO SET:
int analogSoloSeco = 400;
int analogSoloMolhado = 150;
int percSoloSeco = 0;
int percSoloMolhado = 100;

// WIFI SET:
#define AP_SSID "Casa da Macumbeira"
#define AP_PASSWORD "iamamiwhoami"
SoftwareSerial esp8266Serial(4, 3);
WiFiEspClient espClient;
int status = WL_IDLE_STATUS;

//MQTT SET:
#define ID_MQTT  "EstufaUma"
PubSubClient client(espClient);
long lstRcnctAtmpt = 0;

boolean reconnect() {
  if (client.connect(ID_MQTT, "hdydjcmi", "onkA4tk5__E6")) {
      client.subscribe("aplicativo/+", 1);
      
  digitalWrite(ledBrancoPin, LOW);
  delay(100);
  digitalWrite(ledBrancoPin, HIGH);
  }
  return client.connected();
}


//NFC SET:
#define SS_PIN 10
#define RST_PIN 9
MFRC522 rfid(SS_PIN, RST_PIN);
MFRC522::MIFARE_Key key;
String conteudo= ""; 
byte nuidPICC[4];
bool lendoVaso = false;
String nomeVaso = "Manjericão";
String vStr = "";
char vaso[50];
bool hasVaso = false;

//DHT SET:
#define DHTPIN 2
#define DHTTYPE DHT22
DHT_Unified dht(DHTPIN, DHTTYPE);

//TIMERS SET:
unsigned long currentMillis;
unsigned long previousMSGMillis = 0;
unsigned long PCM = 0;
unsigned long PBM = 0;
unsigned long previousMotorMillis = 0;
unsigned long PHM = 0;
unsigned long PDM = 0;

unsigned long MSGInterval = 30000;
unsigned long interval = 43200000;
unsigned long intervalBranco = 43200000;
unsigned long intervalMotorRega = 3500;
unsigned long intervalDHT = 600000;
unsigned long intervalHigrometro = 7200000;

//DADOS PARA PUBLICAR:
String umidSol_string = "";
String temp_string = "";
String umid_string = "";
char umidSol[50];
char temp[50];
char umid[50];

int umidadeSolo = 0;

bool rega = false;
bool modoAutomatico = true;
bool conectada = false;

void sendMSG(){
  if (hasVaso){
    afereHig();
  }
  getTempHumid();
}

 void getTempHumid(){
  sensors_event_t event;  
  dht.temperature().getEvent(&event);
  if (isnan(event.temperature)) {
    //tratar erro
  }
  else {
    float temperatura = event.temperature;
    temp_string = String(temperatura);
    temp_string.toCharArray(temp, temp_string.length() + 1);
    publishData("estufa/temp", temp);
  }
  dht.humidity().getEvent(&event);
  if (isnan(event.relative_humidity)) {
    //tratar erro
  }
  else {
    float umidade = event.relative_humidity;
    umid_string = String(umidade);
    umid_string.toCharArray(umid, umid_string.length() + 1);
    publishData("estufa/umid", umid);
  }
}

void trocaLed(){
  PCM = currentMillis;
  if (ledColoridoState == LOW) {
    ledColoridoState = HIGH;
  } else {
    ledColoridoState = LOW;
  }
  digitalWrite(ledColoridoPin, ledColoridoState);
}

void afereHig(){
  PHM = currentMillis;
  digitalWrite(higPin, HIGH);
  delay(500);
  umidadeSolo = constrain(analogRead(pinoSensor),analogSoloMolhado,analogSoloSeco);
  umidadeSolo = map(umidadeSolo,analogSoloMolhado,analogSoloSeco,percSoloMolhado,percSoloSeco);
  digitalWrite(higPin, LOW);
  umidSol_string = String(umidadeSolo);
  umidSol_string.toCharArray(umidSol, umidSol_string.length() + 1);
  publishData("estufa/umidSo", umidSol);
  if (umidadeSolo < 40){
    rega = true;
  }else{
    rega = false;
  }
  
}
void mfrc522_fast_Reset()
  {
    digitalWrite(RST_PIN, HIGH);
    rfid.PCD_Reset();
    rfid.PCD_WriteRegister(rfid.TModeReg, 0x80);      // TAuto=1; timer starts automatically at the end of the transmission in all communication modes at all speeds
    rfid.PCD_WriteRegister(rfid.TPrescalerReg, 0x43);   // 10μs.
  //  rfid.PCD_WriteRegister(rfid.TPrescalerReg, 0x20);   // test

    rfid.PCD_WriteRegister(rfid.TReloadRegH, 0x00);   // Reload timer with 0x064 = 30, ie 0.3ms before timeout.
    rfid.PCD_WriteRegister(rfid.TReloadRegL, 0x1E);
    //mfrc522.PCD_WriteRegister(rfid.TReloadRegL, 0x1E);

    rfid.PCD_WriteRegister(rfid.TxASKReg, 0x40);    // Default 0x00. Force a 100 % ASK modulation independent of the ModGsPReg register setting
    rfid.PCD_WriteRegister(rfid.ModeReg, 0x3D);   // Default 0x3F. Set the preset value for the CRC coprocessor for the CalcCRC command to 0x6363 (ISO 14443-3 part 6.2.4)

    rfid.PCD_AntennaOn();            // Enable the antenna driver pins TX1 and TX2 (they were disabled by the reset)
  }

void buscaVaso(){
  mfrc522_fast_Reset();
  delay(500);
  Serial.println(F("busca"));
  if ( ! rfid.PICC_IsNewCardPresent()) 
  {
    Serial.println(F("vaso antigo"));
    return;
  }
  if ( ! rfid.PICC_ReadCardSerial()) 
  {
    return;
  }
  byte letra;
  for (byte i = 0; i < rfid.uid.size; i++) 
  {
     conteudo.concat(String(rfid.uid.uidByte[i] < 0x10 ? " 0" : " "));
     conteudo.concat(String(rfid.uid.uidByte[i], HEX));
     conteudo.toUpperCase();
     Serial.println(conteudo.substring(1));
    if (conteudo.substring(1) == "42 11 EE 1A") //UID 1 - Cartão
    {
      vStr = String(nomeVaso);
      vStr.toCharArray(vaso, vStr.length() + 1);
      hasVaso = true;
      publishData("estufa/nomeVaso", vaso);
    }
  }

  conteudo = "";
  // Halt PICC
  rfid.PICC_HaltA();
  // Stop encryption on PCD
  rfid.PCD_StopCrypto1();
  lendoVaso = false;
}
  
void publishData(char* a,byte* b){
  client.publish(a, b);
}

void mqtt_callback(char* topic, byte* payload, unsigned int length) 
  {
      String msg;
      for(int i = 0; i < length; i++){
         char c = (char)payload[i];
         msg += c;
      }
      if (strcmp(topic,"aplicativo/conecta")==0) {
        if(msg == ID_MQTT){
          if(client.publish("estufa/conecta", "true")){
            conectada = true;
            if(modoAutomatico){
              publishData("estufa/modo", "true");
            }else{
              publishData("estufa/modo", "false");
            }
            sendMSG();
          }
        }
      }
      if (strcmp(topic,"aplicativo/get")==0){
        sendMSG();
      }
      if (strcmp(topic,"aplicativo/conectaVaso")==0){
        lendoVaso = true;
      }
      if(strcmp(topic,"aplicativo/bco")==0){
        if(msg == "true"){
          digitalWrite(ledBrancoPin, LOW);
          publishData("estufa/lb", "true");
        }else{
          digitalWrite(ledBrancoPin, HIGH);
          publishData("estufa/lb", "false");
        }
      }
  }

void setup() {
  Serial.begin(9600);
  esp8266Serial.begin(9600);
  dht.begin();
  client.setServer("farmer.cloudmqtt.com", 16775);
  client.setCallback(mqtt_callback);
  
  SPI.begin(); // Init SPI bus
  rfid.PCD_Init(); // Init MFRC522 

  for (byte i = 0; i < 6; i++) {
    key.keyByte[i] = 0xFF;
  }
 
  conectaWifi();
  lstRcnctAtmpt = 0;
  
  pinMode(ledColoridoPin, OUTPUT);
  pinMode(ledBrancoPin, OUTPUT);
  pinMode(exaustorPin, OUTPUT);
  pinMode(motorRegaPin, OUTPUT);
  pinMode(higPin, OUTPUT);
  pinMode(pinoSensor, INPUT);
  
  digitalWrite(ledColoridoPin, LOW);
  digitalWrite(exaustorPin, HIGH);
  digitalWrite(ledBrancoPin, HIGH);
  digitalWrite(motorRegaPin, LOW);
  digitalWrite(higPin, LOW);
}

void conectaWifi(){
  WiFi.init(&esp8266Serial);
  if (status == WL_NO_SHIELD){
    while (true);
  }
  while (status != WL_CONNECTED) {
    status = WiFi.begin(AP_SSID, AP_PASSWORD);
  }
}

void loop() {
  currentMillis = millis();

  if (!client.connected()) {
    rega = false;
    if (currentMillis - lstRcnctAtmpt > 3000) {
      lstRcnctAtmpt = currentMillis;
      // Attempt to reconnect
      if (reconnect()) {
        lstRcnctAtmpt = 0;
      }
    }
  } else {
   if (currentMillis - previousMSGMillis >= MSGInterval && conectada == true) {
      previousMSGMillis = currentMillis;
      sendMSG();
    };
    if (currentMillis - PCM >= interval) {
      trocaLed();
    };
    if (hasVaso && currentMillis -  PHM >= intervalHigrometro) {
      afereHig();
    };
//    if(rega == true && currentMillis - previousMotorMillis >= intervalMotorRega){
//        if(motorRegaState == LOW){
//          motorRegaState = HIGH;
//          digitalWrite(motorRegaPin, HIGH);
//          previousMotorMillis = currentMillis;
//        } else{
//          motorRegaState = LOW;
//          digitalWrite(motorRegaPin, LOW);
//          previousMotorMillis = currentMillis;
//        }
//        
//    }
    if (currentMillis -  PDM >= intervalDHT) {
      PDM = currentMillis;
      getTempHumid();
    };
    if (lendoVaso) {
      buscaVaso();
    };
  }
  client.loop();
}
