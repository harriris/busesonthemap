package com.example.busesonthemap.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.busesonthemap.model.HslBus
import com.example.busesonthemap.model.HslBusResponse
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.QoS
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

private const val DEBUG_TAG = "HslMqttBusService"
private const val API_ADDRESS = "mqtt.hsl.fi:1883"
private const val BUS_TOPIC = "/hfp/v2/journey/ongoing/vp/bus/#"

class HslMqttBusService : Service() {
    inner class LocalBinder : Binder() {
        fun getService(): HslMqttBusService = this@HslMqttBusService
    }

    private val binder = LocalBinder()

    private lateinit var hslMqttClient: MqttAndroidClient
    private val json = Json { ignoreUnknownKeys = true }

    private var busLines: HashMap<String, HslBus> = HashMap()
    val getBusLines: ArrayList<HslBus>
        get() {
            val newList = ArrayList<HslBus>()
            busLines.values.forEach { hslBus ->
                newList.add(hslBus.copy())
            }
            return newList
        }

    override fun onCreate() {
        super.onCreate()
        hslMqttClient = MqttAndroidClient(
            this,
            "tcp://$API_ADDRESS",
            MqttClient.generateClientId(),
        )
        setClientCallbacks()
        connectToHslBusApi()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        hslMqttClient.unregisterResources()
        return super.onUnbind(intent)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun connectToHslBusApi() {
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false
        val connectToken: IMqttToken = hslMqttClient.connect(mqttConnectOptions)
        connectToken.actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(DEBUG_TAG, "CONNECTED TO $API_ADDRESS")
                val disconnectedBufferOptions = DisconnectedBufferOptions()
                disconnectedBufferOptions.isBufferEnabled = true
                disconnectedBufferOptions.bufferSize = 100
                disconnectedBufferOptions.isPersistBuffer = false
                disconnectedBufferOptions.isDeleteOldestMessages = false
                hslMqttClient.setBufferOpts(disconnectedBufferOptions)
                subscribeToHslBusTopic()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e(DEBUG_TAG, "CONNECTION FAILED TO $API_ADDRESS")
            }
        }
    }

    private fun setClientCallbacks() {
        hslMqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                Log.d(DEBUG_TAG, "CONNECTION LOST TO $API_ADDRESS")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload)
                if (payload.isEmpty()) {
                    return
                }
                val data: HslBusResponse
                try {
                    data = json.decodeFromString(payload)
                } catch (e: SerializationException) {
                    Log.e(DEBUG_TAG, e.toString())
                    return
                } catch (e: IllegalArgumentException) {
                    Log.e(DEBUG_TAG, e.toString())
                    return
                }

                if (data.bus.lat == null || data.bus.lon == null) {
                    return
                }

                busLines[data.bus.id] = data.bus
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(DEBUG_TAG, "DELIVERY COMPLETED")
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                if (reconnect) {
                    subscribeToHslBusTopic()
                }
            }
        })
    }

    private fun subscribeToHslBusTopic() {
        val subToken = hslMqttClient.subscribe(BUS_TOPIC, QoS.AtMostOnce.value)
        subToken.actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(DEBUG_TAG, "SUBSCRIBE SUCCEEDED: $BUS_TOPIC")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e(DEBUG_TAG, "SUBSCRIBE FAILED: $BUS_TOPIC")
            }
        }
    }
}
