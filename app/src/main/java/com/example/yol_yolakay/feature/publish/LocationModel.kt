package com.example.yol_yolakay.feature.publish

import kotlinx.serialization.Serializable

@Serializable
data class LocationModel(
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val pointId: String? = null,
    val region: String = ""
)