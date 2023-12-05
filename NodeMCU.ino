#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <LiquidCrystal_I2C.h>
#include <Servo.h>
#include <Wire.h>

#define LCD_COLUMNS 16
#define LCD_ROWS 2

  /* LED Variables */
  const int ledRojo = 0;
  const int ledVerde = 14;
  const int ledAzul = 15;
  const int piezo = 12;

  const int MIN_DISTANCE = 10;

  /* Descomentar cuando se instale la pantalla LCD */
  LiquidCrystal_I2C lcd(0x27, 16, 2);

  // Servomotor
  Servo servomotor;

  // Estacionamientos disponibles
  int available = 5;

  // Pines del sensor ultrasónico
  const int trig = 2;
  const int echo = 16;

  /* Duración y distancia del objeto frente al sensor */
  long duration;
  int distance;
  // token pruebas: mqtt://mqttandroid1234:uHVq1MmCaaVlwtQx@mqttandroid1234.cloud.shiftr.io
  // token: mqtt://holamundoestesi:NefigV8TBcepXelX@holamundoestesi.cloud.shiftr.io
  // Configuración de MQTT
  const char* ssid = "iPhone Fake";      
  const char* password = "12345677"; 
  const char* mServer = "mqttandroid1234.cloud.shiftr.io";
  const char* mUser = "mqttandroid1234";
  const char* mPassword = "uHVq1MmCaaVlwtQx";  
  const char* topic = "access";

  WiFiClient espClient;
  PubSubClient client(espClient);

  /* Descomentar funciones del
  lcd cuando se conecte
  */
  void setup() {
    lcd.begin(); 
    Wire.begin();
    lcd.backlight();
    pinMode(ledRojo, OUTPUT);
    pinMode(ledVerde, OUTPUT);
    pinMode(ledAzul, OUTPUT);
    servomotor.attach(13);
    pinMode(trig, OUTPUT);
    pinMode(echo, INPUT);

    // Conexión a WiFi
    Serial.begin(115200);
    WiFi.begin(ssid, password);
    while(WiFi.status() != WL_CONNECTED) {
      delay(1000);
      Serial.println("Connecting to WiFi...");
    }

    Serial.println("Connected to WiFi");

    // Configuración de MQTT
    client.setServer(mServer, 1883);
    client.setCallback(callback);
    reconnect();
  }

  void loop() {
      
    if (!client.connected()) {
      reconnect();
    }

    if(distance > 10){
      servomotor.write(0);
    }

    client.loop();

    clearTrigPin();
    digitalWrite(trig, HIGH);
    digitalWrite(trig, LOW);

    // Leer los datos que recibe el pin echo
    duration = pulseIn(echo, HIGH);

    // Calcular la distancia
    distance = duration * 0.034 / 2;

    noTone(piezo);
    
    // Encender el LED rojo
    analogWrite(ledRojo, 255);
    analogWrite(ledVerde, 0);
    analogWrite(ledAzul, 0);

    lcd.setCursor(0, 0);
    lcd.print("Disponibles: ");
    lcd.setCursor(14, 0);
    lcd.print(available);

    publishAvailability();
  }

  /* 
    Limpiar el pin emisor
  */
  void clearTrigPin() {
    digitalWrite(trig, LOW);
    delay(100);
  }

  /*
    Imprimir la distancia
    
    @param _distance
    Distancia entre el objeto y el sensor

    note: Descomentar las líneas de LCD
  */
  void unlockBarrier(int _distance) {
    lcd.clear();
    if (available <= 5 && available > 0) {
      Serial.print("Distance: ");
      Serial.println(_distance);

      if (_distance < MIN_DISTANCE) {
        available--;
        // Girar el motor en 90°
        servomotor.write(110);
      
        // Encender el LED verde
        analogWrite(ledRojo, 0);
        analogWrite(ledVerde, 0);
        analogWrite(ledAzul, 255);

        // Secuencia piezo
        for (int i = 0; i < 3; i++) {
          noTone(piezo);    
          delay(250);
          tone(piezo, 500);
        }
        // Imprimir en el LCD
        lcd.setCursor(0, 0);
        lcd.print("Bienvenido");
      }
    }else{
        lcd.clear();
        lcd.setCursor(0, 0);
        lcd.print("No disponible");
      }

    noTone(piezo);
    delay(3000);
    servomotor.write(0);
    lcd.clear();
  }

  /*
    Informar que no quedan más
    espacios en el estacionamiento

    @var _distance
    Distancia entre el objeto y el 
    sensor ultrasónico
  */
  void noAvailable(int _distance) {
    lcd.clear();
    Serial.print("Distance: ");
    Serial.println(_distance);
    if (_distance < MIN_DISTANCE) {
      
      analogWrite(ledRojo, 255);
      analogWrite(ledVerde, 0);
      analogWrite(ledAzul, 0);

      // Imprimir en el LCD
      lcd.setCursor(0, 0);
      lcd.print("No disponible");
      for (int i = 0; i < 12; i++) {
        delay(200);
        lcd.scrollDisplayLeft();
      }
    }

    delay(1000);
    lcd.clear();
  }

  /*
    Marcar la salida de un vehículo
    @param _distance
    Distancia entre el objeto y el sensor
  */
  void vehicleOut(int _distance) {
    if (available >= 0) {
      lcd.clear();
      Serial.println("Vehicle out"); 

        if (available < 5) {
          available++;
        }

        // Girar el motor en 90°
        servomotor.write(110);

        // Encender el LED verde
        analogWrite(ledRojo, 0);
        analogWrite(ledVerde, 0);
        analogWrite(ledAzul, 255);

        // Secuencia piezo
        for (int i = 0; i < 3; i++) {
          noTone(piezo);    
          delay(250);
          tone(piezo, 500);
        }
        // Imprimir en el LCD
        lcd.setCursor(0, 0);
        lcd.print("Vuelva pronto...");
        noTone(piezo);
        for (int i = 0; i < 16; i++) {
          delay(250);
          lcd.scrollDisplayLeft();
        }  

    }
    delay(3000);
    servomotor.write(0);
    
    noTone(piezo);
    lcd.clear();
  }

  void callback(char* topic, byte* payload, unsigned int length) {
    Serial.print("Message arrived [");
    Serial.print(topic);
    Serial.print("] ");
    
    String message = "";
    for (int i = 0; i < length; i++) {
      message += (char)payload[i];
    }
    Serial.println(message);

    // Comparar el mensaje y realizar acciones correspondientes
    if (message == "vehicle in") {
      unlockBarrier(distance);
    } else if (message == "vehicle out") {
      vehicleOut(distance);
    }
  }

  void reconnect() {
    // Loop hasta que estemos re-conectados
    while (!client.connected()) {
      Serial.print("Attempting MQTT connection...");
      
      // Intentar conectar
      if (client.connect("ESP8266Client", mUser, mPassword)) {
        Serial.println("connected");
        // Suscribirse al tópico al conectar
        client.subscribe(topic);
      } else {
        Serial.print("failed, rc=");
        Serial.print(client.state());
        Serial.println(" Try again in 3 seconds.");
        // Esperar 5 segundos antes de volver a intentar
        delay(3000);
      }
    }
  }

  void publishAvailability() {
  // Convertir el valor de 'available' a una cadena
  String availabilityMessage = String(available);

  // Publicar el mensaje en el tópico "disponibles"
  client.publish("disponibles", availabilityMessage.c_str());
}





