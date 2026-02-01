package com.example.yol_yolakay.feature.publish.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TripDto(
    @SerialName("from_city") val fromLocation: String, // Backend bilan moslash uchun
    @SerialName("to_city") val toLocation: String,

    @SerialName("start_lat") val fromLat: Double = 41.2995,
    @SerialName("start_lng") val fromLng: Double = 69.2401,
    @SerialName("end_lat") val toLat: Double = 39.6542,
    @SerialName("end_lng") val toLng: Double = 66.9597,

    @SerialName("date") val date: String,
    @SerialName("time") val time: String,
    @SerialName("price") val price: Double,

    @SerialName("available_seats") val seats: Int,
    @SerialName("driver_id") val driverId: String = "0ed9def9-ae36-422d-a6c1-fb45dbd9b90a",

    // --- MANA SHU IKKALASI XATONI TUZATADI ---
    @SerialName("driver_name") val driverName: String? = "Haydovchi",
    @SerialName("car_model") val carModel: String? = "Mashina"
)