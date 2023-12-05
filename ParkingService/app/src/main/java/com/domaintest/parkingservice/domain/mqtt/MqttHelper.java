package com.domaintest.parkingservice.domain.mqtt;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.domaintest.parkingservice.viewmodel.MessageViewModel;
import com.domaintest.parkingservice.views.MainActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;


/*
*  @author Andresito404
*  @date: 01-12-2023
*
*  Define los métodos necesarios para
*  una conexión a un broker Mqtt,
*  en este caso se usa el servicio
*  de Shiftr.io (Broker MQTT), los métodos
*  de esta clase son los suficientes para
*  enviar mensajes a un tópico y recibir
*  mensajes de otro. Necesarios para comunicar
*  el ESP8266, los mensajes que envía este Helper
*  son los que actúan como entrada y salida de vehículos
*  en el sistema de control de acceso
* */
public class MqttHelper implements ViewModelStoreOwner {

    // token: mqtt://parkingservice:uvGKmWxxmzeLBP4@parkingservice.cloud.shiftr.io
    private String TAG = "MqttHelper";

    private Context context;
    private String clientId = "";

    private static String mHost = "tcp://mqttandroid1234.cloud.shiftr.io:1883";
    private static String mUser = "mqttandroid1234";
    private static String mPass = "uHVq1MmCaaVlwtQx";


    private MqttAndroidClient client;
    private MqttConnectOptions options;

    private static String[] topics = {"access", "disponibles"};
    private static String topicMsgOn = "Turn on";
    private static String topicMsgOff = "Turn off";

    private ViewModelStore viewModelStore = new ViewModelStore();

    private Consumer<String> messageListener;
    private MessageViewModel messageViewModel;

    /* El contexto que recibe este constructor es
     *  sumamente necesario para iniciar la conexión
     *  al servidor Mqtt
     */
    public MqttHelper(Context context) {
        this.context = context;
        this.messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
    }


    /*
     * Evaluar si la conexión con el servidor mqtt está establecida,
     * si no está establecida, se fuerza una conexión
     *
     * @return Si la conexión está establecida con el Broker
     */
    public Boolean checkConnection() {
        if (!client.isConnected()) {
            connectBroker();

        }
        return client.isConnected();

    }


    /* Realizar la conexión al Broker MQTT
     *  empleando el uso de una configuración
     *  que contiene el usuario y contraseña
     *  contenidos en el token de acceso al broker
     *
     * @throw Exception Cuando ocurre algún error al intentar la
     *       conexión con el servidor
     */
    public void connectBroker() {
        this.client = new MqttAndroidClient(context, mHost, clientId);
        this.options = new MqttConnectOptions();
        this.options.setUserName(mUser);
        this.options.setPassword(mPass.toCharArray());

        // Iniciar la conexión
        try {
            Log.d(TAG, "connectBroker: Iniciando conexión...    ");
            IMqttToken token = client.connect(options);
            Log.d(TAG, "connectBroker: " + token);
            token.setActionCallback(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onSuccess: Connection success");
                    subTopic();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onFailure: Connection failed!");
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "connectBroker: " + e.getMessage());
        }
    }


    /* Obtener el Nombre del cliente para ser enviado
     *  al servidor
     */
    public void getClientName() {
        String manufacturer = Build.MANUFACTURER;
        String modelName = Build.MODEL;
        clientId = manufacturer + " " + modelName;

    }


    /* Enviar mensaje a un tópico, son necesarios el tópico y el mensaje,
     *  previamente el cliente debe estar suscrito al tópico
     *  para poder publicar un nuevo mensaje que será
     *  enviado a todos los clientes suscritos a ese tópico,
     *  aquí es cuando entra nuestro dispositivo iot
     *  que recibe los mensajes que este cliente envía a ese tópico.
     *
     *  @param topic Tópico a publicar
     *  @param msg Mensaje a publicar
     *
     *  @throw Exception Cuando ocurre un error con el mensaje
     */
    public void sendMessage(String topic, String msg) {
        try {
            int qos = 0;
            client.publish(topic, msg.getBytes(), qos, false);
            Toast.makeText(context, topic + ": " + msg.getBytes(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.d(TAG, "sendMessage: " + e.getMessage());
            e.getMessage();
        }
    }

    /* Es necesario suscribir el cliente a un tópico
     * para que se puedan publicar o leer mensajes
     *
     * @throw MqttSecurityException Cuando un cliente no está
     *       autorizado para realizar una operación en el tópico
     *
     * @throw MqttException Cuando ocurre un error de comunicación
     *       con el servidor*/
    private void subTopic() {
        try {
            int[] qos = {0, 0};
            client.subscribe(topics, qos);
        } catch (MqttSecurityException e) {
            throw new RuntimeException(e);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(context, "Server disconnected!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handleMessage(topic, message);
                Log.d(TAG, "messageArrived: Message arrived!");

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Toast.makeText(context, "Message sent!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /* Finalizar la comunicación con el servidor
     *
     * @throw MqttException Cuando ocurre un error en la comunicación
     *       con el servidor
     */
    public void disconnect() {
        try {
            this.client.disconnect();
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    /*  */
    public void setOnMessageReceivedListener(Consumer<String> listener) {
        messageListener = listener;
    }


    /* Manejar el mensaje recibido, el mensaje que debemos recibir
     *  consiste en la disponibilidad del estacionamiento, una vez
     *  recibido, ejecutaremos messageArrived que
     *
     *  @param topic Tópico del cual recibiremos el mensaje
     *  @param msg Mensaje que recibiremos */
    public void handleMessage(String topic, MqttMessage msg) {
        if (messageListener != null) {
            try {
                if (topic == "disponibles") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        messageListener.accept(new String(msg.getPayload()));
                        Log.d(TAG, "handleMessage: " + new String(msg.getPayload()));
                        // Update msgVidemodel}
                        messageViewModel.setMessage(new String(msg.getPayload()));


                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setMessage(MessageViewModel viewModel, String msg) {
        viewModel.setMessage(msg);
    }


    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }

}
