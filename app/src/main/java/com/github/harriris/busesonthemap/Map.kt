package com.github.harriris.busesonthemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import com.github.harriris.busesonthemap.model.HslBus
import com.github.harriris.busesonthemap.util.DatetimeFormatter
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val busDatetimeFormatter = DatetimeFormatter(HslBus.DATETIME_FORMAT)

@Composable
fun Map(busLines: SnapshotStateList<HslBus>) {
    val map = createMap()
    MapLifecycleObserver(map)
    AndroidView({ map })
    BusMarkers(map, busLines)
}

@Composable
private fun createMap(): MapView {
    val context = LocalContext.current

    Configuration.getInstance()
        .load(context, PreferenceManager.getDefaultSharedPreferences(context))
    Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

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

@Composable
private fun MapLifecycleObserver(map: MapView) {
    val lifecycleObserver = remember(map) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> {}
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }
}

@Composable
private fun BusMarkers(map: MapView, busLines: SnapshotStateList<HslBus>) {
    val busLineState = remember { busLines }
    val busMarkerState = remember { HashMap<String, Marker>() }
    busLineState.forEach { hslBus ->
        var busMarker: Marker? = busMarkerState[hslBus.id]
        if (busMarker == null) {
            busMarker = Marker(map)
            busMarker.id = hslBus.id
            map.overlays.add(busMarker)
            busMarkerState[hslBus.id] = busMarker
        }
        setBusMarkerInfo(busMarker, hslBus)
    }
    map.invalidate()
}

private fun setBusMarkerInfo(busMarker: Marker, hslBus: HslBus) {
    busMarker.title = hslBus.lineName
    busMarker.snippet = "${hslBus.speedKph}<br>${busDatetimeFormatter.format(hslBus.timestamp)}"
    busMarker.position = GeoPoint(hslBus.lat!!, hslBus.lon!!)
}