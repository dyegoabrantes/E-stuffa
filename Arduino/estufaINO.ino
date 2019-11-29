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
int ledColoridoState = 0;
int ledBrancoState = 0;
int exaustorState = 0;
int motorRegaState = 0;
int higrometroState = 0;

// HIGROMETRO SET:
int analogSoloSeco = 400;
int analogSoloMolhado = 150;
int percSoloSeco = 0;
int percSoloMolhado = 100;

//NFC SET:
#define SS_PIN 10
#define RST_PIN 9
MFRC522 rfid(SS_PIN, RST_PIN);
MFRC522::MIFARE_Key key;
String conteudo= ""; 
byte nuidPICC[4];
String nomeVaso = "Nenhum";
String vStr = "";
char vaso[50];
bool hasVaso = false;

//DHT SET:
#define DHTPIN 2
#define DHTTYPE DHT22
DHT_Unified dht(DHTPIN, DHTTYPE);

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

void afereHig(){
  digitalWrite(higPin, HIGH);
  delay(500);
  umidadeSolo = constrain(analogRead(pinoSensor),analogSoloMolhado,analogSoloSeco);
  umidadeSolo = map(umidadeSolo,analogSoloMolhado,analogSoloSeco,percSoloMolhado,percSoloSeco);
  digitalWrite(higPin, LOW);
  umidSol_string = String(umidadeSolo);
  umidSol_string.toCharArray(umidSol, umidSol_string.length() + 1);
  publishData("estufa/umidSo", umidSol);
  
}

void buscaVaso(){
  if ( ! rfid.PICC_IsNewCardPresent()) 
  {
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
    if (conteudo.substring(1) == "42 11 EE 1A") //UID 1 - Cartão
    {
      nomeVaso = "Manjericão";
      vStr = String(nomeVaso);
      vStr.toCharArray(vaso, vStr.length() + 1);
      hasVaso = true;
    }
  }

  conteudo = "";
  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
}

#define AP_SSID "Casa da Macumbeira"
#define AP_PASSWORD "azeliabanks"
SoftwareSerial esp8266Serial(4, 3);
WiFiEspClient espClient;
int status = WL_IDLE_STATUS;

#define ID_MQTT  "EstufaUma"
PubSubClient client(espClient);

unsigned long currentMillis;
unsigned long loopInterval = 1000;
unsigned long previousLoopInterval = 0;
unsigned long verificaInterval = 500;
unsigned long previousVerifica = 0;
unsigned long previousSmsg = 0;
unsigned long smsgInterval = 30000;

void conectaWifi(){
  if (status == WL_NO_SHIELD){
    while (true);
  }
  while (status != WL_CONNECTED) {
    status = WiFi.begin(AP_SSID, AP_PASSWORD);
    
  }
  if (status == 1){
    conectaBroker();
  }
}
void conectaBroker(){
    while(client.connected() == 0){
      client.connect(ID_MQTT, "hdydjcmi", "onkA4tk5__E6");
      delay(1000);
    }
    if(client.connected() == 1){
      if(client.subscribe("aplicativo/+", 1)){
      }
    }  
}
  
void publishData(char* a,byte* b){
  client.publish(a, b);
  delay(200);
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
          client.publish("estufa/conecta", "true");
          conectada = true;
          if(modoAutomatico){
            publishData("estufa/modo", "true");
          }else{
            publishData("estufa/modo", "false");
          }
          getTempHumid();
          if (hasVaso){
            afereHig();
          }
        }
      }
      
      if (strcmp(topic,"aplicativo/get")==0){
        getTempHumid();
        if (hasVaso){
          afereHig();
        }
      }
      
      if (strcmp(topic,"aplicativo/conectaVaso")==0){
        publishData("estufa/nomeVaso", vaso);
      }
      
      if(strcmp(topic,"aplicativo/bco")==0){
        if(msg == "true"){
          digitalWrite(ledBrancoPin, LOW);
          ledBrancoState = 1;
          if(ledColoridoState = 1){
            digitalWrite(ledColoridoPin, HIGH);
          }
          publishData("estufa/lb", "true");
        }else if((msg == "false")){
          digitalWrite(ledBrancoPin, HIGH);
          ledBrancoState = 0;
          if(ledColoridoState = 1){
            digitalWrite(ledColoridoPin, LOW);
          }
          publishData("estufa/lb", "false");
        }
      }

      if(strcmp(topic,"aplicativo/vtl")==0){
        if(exaustorState == 0){
          publishData("estufa/vtl", "true");
          digitalWrite(exaustorPin, LOW);
          exaustorState = 1;
        } else if(exaustorState == 1){
          publishData("estufa/vtl", "false");
          digitalWrite(exaustorPin, HIGH);
          exaustorState = 0;
        }
      }

      if (strcmp(topic,"aplicativo/col")==0){
        if(ledColoridoState == 0){
            publishData("estufa/col", "true");
            digitalWrite(ledColoridoPin, LOW);
            ledColoridoState = 1;
          } else if (ledColoridoState == 1){
            publishData("estufa/col", "false");
            digitalWrite(ledColoridoPin, HIGH);
            ledColoridoState = 0;
          }
      }
      if (strcmp(topic,"aplicativo/rg")==0){
        if(motorRegaState == 0){
            publishData("estufa/rg", "true");
            digitalWrite(motorRegaPin, HIGH);
            motorRegaState = 1;
          } else if (motorRegaState == 1){
            publishData("estufa/rg", "false");
            digitalWrite(motorRegaPin, LOW);
            motorRegaState = 0;
          }
      }
  }


void setup() {
  Serial.begin(9600);
  esp8266Serial.begin(9600);
  delay(200);
  WiFi.init(&esp8266Serial);
  client.setServer("farmer.cloudmqtt.com", 16775);
  client.setCallback(mqtt_callback);
  conectaWifi();

  SPI.begin(); // Init SPI bus
  rfid.PCD_Init(); // Init MFRC522 

  for (byte i = 0; i < 6; i++) {
    key.keyByte[i] = 0xFF;
  }

  vStr = String("Nenhum Vaso");
  vStr.toCharArray(vaso, vStr.length() + 1);
      
  pinMode(ledColoridoPin, OUTPUT);
  pinMode(ledBrancoPin, OUTPUT);
  pinMode(exaustorPin, OUTPUT);
  pinMode(motorRegaPin, OUTPUT);
  pinMode(higPin, OUTPUT);
  pinMode(pinoSensor, INPUT);
  
  digitalWrite(ledColoridoPin, LOW);
  ledColoridoState = 1;
  digitalWrite(exaustorPin, HIGH);
  digitalWrite(ledBrancoPin, HIGH);
  digitalWrite(motorRegaPin, LOW);
  digitalWrite(higPin, LOW);
}

void loop() {
  client.loop();
  currentMillis = millis();
  buscaVaso();
  if (currentMillis -  previousVerifica >= verificaInterval) {
    previousVerifica = currentMillis;
    if (client.connected() == 0){
      conectaBroker();
    }
  }
  delay(50);

}
