package com.github.harriris.busesonthemap.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HslBusResponse(@SerialName("VP") val bus: HslBus)
