package com.domaintest.parkingservice.views

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domaintest.parkingservice.domain.mqtt.MqttHelper
import com.domaintest.parkingservice.ui.theme.ParkingServiceTheme
import com.domaintest.parkingservice.viewmodel.MessageViewModel
import org.eclipse.paho.client.mqttv3.IMqttMessageListener

class MainActivity : ComponentActivity() {


    // token: mqtt://holamundoestesi:Jf4poF137quTJJzj@holamundoestesi.cloud.shiftr.io
    private val topicServo = "access"

    private val TAG = "MainActivity"

    private lateinit var mMqttClient: MqttHelper

    private val msgSignIn = "vehicle in"
    private val msgSignOut = "vehicle out"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMqttClient = MqttHelper(this)
        mMqttClient.getClientName()
        mMqttClient.connectBroker()
        setContent {
            ParkingServiceTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ParkingView()
                }
            }
        }

        mMqttClient.setOnMessageReceivedListener {
            message -> { setMessage(message) }
            }
    }


    private fun setMessage(message: String) {

    }

    /* Cuando se cierre la app, desconectamos el cliente Mqtt*/
    override fun onDestroy() {
        super.onDestroy()
    }


    @Composable
    private fun ParkingView() {

        val messageViewModel: MessageViewModel = viewModel<MessageViewModel>()
        var receviedMsg by remember {
            mutableStateOf("")
        }
        messageViewModel.message.observe(this, Observer {
            receviedMsg = it
        })

        DisposableEffect(mMqttClient){
            val listener: (String) -> Unit = {
                message ->
                receviedMsg = message
            }

            Log.d(TAG, "ParkingView: " + receviedMsg)
            onDispose {
                mMqttClient.disconnect()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(modifier = Modifier.padding(8.dp),
                text = "Parking Access Control", fontWeight = FontWeight.Bold,
                fontSize = 24.sp)

            Text(text = "Disponibles: $receviedMsg", fontWeight = FontWeight.Bold)

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(top = 50.dp, start = 20.dp, end = 20.dp)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
                ) {
                // Button open barrier
                Button(modifier = Modifier.width(120.dp),
                    onClick = {
                        if(mMqttClient.checkConnection()){
                            mMqttClient.sendMessage(topicServo, msgSignIn)
                        }
                }) {
                    Text(text = "Entrar")
            }

               Spacer(modifier = Modifier.padding(PaddingValues(start = 8.dp, end = 8.dp)))
                Button(modifier = Modifier.width(120.dp),
                    onClick = {
                        if(mMqttClient.checkConnection()){
                            mMqttClient.sendMessage(topicServo, msgSignOut)
                        }
                }){
                    Text(text = "Salir")
                }
            }

        }
    }





    @Composable
    @Preview (showBackground = true)
    fun Screen(){
        ParkingView()
    }
}




