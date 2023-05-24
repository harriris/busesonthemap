package com.github.harriris.busesonthemap

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import com.github.harriris.busesonthemap.model.HslBus
import com.github.harriris.busesonthemap.service.HslMqttBusService
import com.github.harriris.busesonthemap.util.DatetimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var busMarkers: HashMap<String, Marker>
    private lateinit var busDatetimeFormatter: DatetimeFormatter

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
                    updateMarkers(hslMqttService?.getBusLines)
                }
                delay(1000L)
            }
        }
    }

    private fun setBusMarkerInfo(busMarker: Marker, hslBus: HslBus) {
        busMarker.title = hslBus.lineName
        busMarker.snippet = "${hslBus.speedKph}<br>${busDatetimeFormatter.format(hslBus.timestamp)}"
        busMarker.position = GeoPoint(hslBus.lat!!, hslBus.lon!!)
    }

    private fun updateMarkers(busLines: ArrayList<HslBus>?) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }
        busLines?.forEach { hslBus ->
            var busMarker: Marker? = busMarkers[hslBus.id]
            if (busMarker == null) {
                busMarker = Marker(map)
                busMarker.id = hslBus.id
                map.overlays.add(busMarker)
                busMarkers[hslBus.id] = busMarker
            }
            setBusMarkerInfo(busMarker, hslBus)
            map.invalidate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        busMarkers = HashMap()
        busDatetimeFormatter = DatetimeFormatter(HslBus.DATETIME_FORMAT)

        setContent {
            Map()
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

    override fun onResume() {
        super.onResume()
        if (this::map.isInitialized) {
            map.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::map.isInitialized) {
            map.onPause()
        }
    }

    @Composable
    fun Map() {
        map = initMap()
        AndroidView({ map })
    }

    @Composable
    private fun initMap(): MapView {
        val context = LocalContext.current

        getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        return remember {
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                controller.setZoom(15.0)
                val startPoint = GeoPoint(60.1699, 24.9384)  // Helsinki city center
                controller.setCenter(startPoint)
            }
        }
    }
}
