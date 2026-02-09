// feature/publish/model/PublishTripRequest.kt
package com.example.yol_yolakay.feature.publish.model

import kotlinx.serialization.Serializable

@Serializable
data class PublishTripRequest(
    val fromLocation: String,
    val fromLat: Double,
    val fromLng: Double,
    val fromPointId: String? = null,

    val toLocation: String,
    val toLat: Double,
    val toLng: Double,
    val toPointId: String? = null,

    val date: String,   // "2026-02-08"
    val time: String,   // "14:30"
    val price: Double,
    val seats: Int
)
