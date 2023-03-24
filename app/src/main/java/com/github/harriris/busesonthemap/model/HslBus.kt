package com.github.harriris.busesonthemap.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HslBus(
    @SerialName("route") val lineName: String,
    @SerialName("veh") val vehicleNumber: Int,
    @SerialName("oper") val operatorNumber: Int,
    val lat: Double?,
    @SerialName("long") val lon: Double?,
    @SerialName("spd") val speed: Double?,
    @SerialName("tst") val timestamp: String?,
) {
    val id: String
        get() = "${this.lineName}${this.operatorNumber}${this.vehicleNumber}"
    val speedKph: String
        get() = when {
            this.speed != null -> "${String.format("%.2f", this.speed * 3.6)} km/h"
            else -> "? km/h"
        }

    companion object {
        const val DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    }
}
