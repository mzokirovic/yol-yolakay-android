package com.example.yol_yolakay.feature.publish.model

import kotlinx.serialization.Serializable


@Serializable
data class PublishTripRequest(
    val fromLocation: String,
    val toLocation: String,
    val fromLat: Double,
    val fromLng: Double,
    val toLat: Double,
    val toLng: Double,
    val date: String,   // yyyy-MM-dd
    val time: String,   // HH:mm
    val price: Double,
    val seats: Int,
    val driverId: String? = null // MVP: auth bo'lmaguncha null
)
