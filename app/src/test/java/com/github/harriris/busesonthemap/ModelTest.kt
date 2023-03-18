package com.github.harriris.busesonthemap

import com.github.harriris.busesonthemap.model.HslBus
import com.github.harriris.busesonthemap.model.HslBusResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Units tests for the data classes in com.github.harriris.busesonthemap.model.
 */
class ModelTest {
    @Test
    fun hslBusResponseFullSerialization() {
        val hslBus = HslBus(
            lineName = "notrandom",
            vehicleNumber = 999,
            operatorNumber = 1,
            lat = "60.1699".toDouble(),
            lon = "24.9384".toDouble(),
            speed = "10.00".toDouble(),
            timestamp = "2023-03-05'T'17:02:00.000Z",
        )
        val hslBusResponse = HslBusResponse(bus = hslBus)
        val jsonString = Json.encodeToString(hslBusResponse)
        val hslBusResponseFromJson: HslBusResponse = Json.decodeFromString(jsonString)
        assertEquals("notrandom1999", hslBusResponseFromJson.bus.id)
        assertEquals("notrandom", hslBusResponseFromJson.bus.lineName)
        assertEquals(60.1699, hslBusResponseFromJson.bus.lat)
        assertEquals(24.9384, hslBusResponseFromJson.bus.lon)
        assertEquals(10.00, hslBusResponseFromJson.bus.speed)
        assertEquals("2023-03-05'T'17:02:00.000Z", hslBusResponseFromJson.bus.timestamp)
        assertEquals(
            "${String.format("%.2f", 10.00 * 3.6)} km/h",
            hslBusResponseFromJson.bus.speedKph,
        )
    }

    @Test
    fun hslBusResponsePartialSerialization() {
        val hslBus = HslBus(
            lineName = "notrandom",
            vehicleNumber = 999,
            operatorNumber = 1,
            lat = null,
            lon = null,
            speed = null,
            timestamp = null,
        )
        val hslBusResponse = HslBusResponse(bus = hslBus)
        val jsonString = Json.encodeToString(hslBusResponse)
        val hslBusResponseFromJson: HslBusResponse = Json.decodeFromString(jsonString)
        assertEquals("notrandom1999", hslBusResponseFromJson.bus.id)
        assertEquals("notrandom", hslBusResponseFromJson.bus.lineName)
        assertNull(hslBusResponseFromJson.bus.lat)
        assertNull(hslBusResponseFromJson.bus.lon)
        assertNull(hslBusResponseFromJson.bus.speed)
        assertNull(hslBusResponseFromJson.bus.timestamp)
        assertEquals("? km/h", hslBusResponseFromJson.bus.speedKph)
    }
}
