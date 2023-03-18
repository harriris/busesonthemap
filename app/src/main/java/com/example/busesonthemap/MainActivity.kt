package com.example.busesonthemap

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.busesonthemap.model.HslBus
import com.example.busesonthemap.service.HslMqttBusService
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
import java.text.DateFormat
import java.text.DateFormat.getDateTimeInstance
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private val busMarkers: HashMap<String, Marker> = HashMap()

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

    private lateinit var localDateTimeFormat: DateFormat
    private lateinit var busDateParser: SimpleDateFormat

    private fun getFormattedTimestamp(timestamp: String?): String {
        if (timestamp == null || timestamp.isEmpty()) {
            return "Unknown timestamp"
        }
        val parsedDate: Date
        try {
            parsedDate = busDateParser.parse(timestamp) as Date
        } catch (exc: ParseException) {
            return "Unknown timestamp"
        }
        return localDateTimeFormat.format(parsedDate)
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
        busMarker.snippet = "${hslBus.speedKph}<br>${getFormattedTimestamp(hslBus.timestamp)}"
        busMarker.position = GeoPoint(hslBus.lat!!, hslBus.lon!!)
    }

    private fun updateMarkers(busLines: ArrayList<HslBus>?) {
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

        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_main)

        initBusDateTimeFormatter()
        initMap()
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

    private fun initBusDateTimeFormatter() {
        localDateTimeFormat = getDateTimeInstance()
        busDateParser = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.getDefault(),
        )
        busDateParser.timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun initMap() {
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        val mapController = map.controller
        mapController.setZoom(15.0)
        val startPoint = GeoPoint(60.1699, 24.9384)
        mapController.setCenter(startPoint)
    }
}
