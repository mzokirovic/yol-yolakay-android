package com.example.yol_yolakay.feature.publish.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TripDto(
    @SerialName("from_city") val fromLocation: String,
    @SerialName("to_city") val toLocation: String,

    @SerialName("start_lat") val fromLat: Double? = null,
    @SerialName("start_lng") val fromLng: Double? = null,
    @SerialName("end_lat") val toLat: Double? = null,
    @SerialName("end_lng") val toLng: Double? = null,

    @SerialName("departure_time") val departureTime: String? = null,
    @SerialName("price") val price: Double,
    @SerialName("available_seats") val seats: Int,

    @SerialName("driver_id") val driverId: String,
    @SerialName("driver_name") val driverName: String? = null,
    @SerialName("car_model") val carModel: String? = null
)
