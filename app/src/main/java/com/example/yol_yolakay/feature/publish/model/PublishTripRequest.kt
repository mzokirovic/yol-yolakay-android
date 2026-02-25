package com.example.yol_yolakay.feature.publish.model

import kotlinx.serialization.Serializable

@Serializable
data class PublishTripRequest(
    val fromLocation: String,
    val fromLat: Double,
    val fromLng: Double,
    val fromPointId: String? = null,
    val fromRegion: String? = null,

    val toLocation: String,
    val toLat: Double,
    val toLng: Double,
    val toPointId: String? = null,
    val toRegion: String? = null,

    val date: String,
    val time: String,
    val price: Double,
    val seats: Int
)