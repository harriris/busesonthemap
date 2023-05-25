package com.github.harriris.busesonthemap

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import com.github.harriris.busesonthemap.model.HslBus
import com.github.harriris.busesonthemap.service.HslMqttBusService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val busLines = mutableStateListOf<HslBus>()

    private var hslMqttService: HslMqttBusService? = null
    private val hslMqttServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as HslMqttBusService.LocalBinder
            hslMqttService = binder.getService()
            pollForBusLineUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hslMqttService = null
        }
    }

    private fun pollForBusLineUpdates(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                runOnUiThread {
                    busLines.clear()
                    hslMqttService?.getBusLines?.let { busLines.addAll(it) }
                }
                delay(1000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Map(busLines)
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, HslMqttBusService::class.java).also { intent ->
            bindService(intent, hslMqttServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(hslMqttServiceConnection)
    }
}
