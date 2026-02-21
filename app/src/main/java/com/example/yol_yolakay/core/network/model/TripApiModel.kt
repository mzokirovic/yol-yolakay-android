package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class TripApiModel(
    val id: String? = null,

    @SerialName("from_city") val fromCity: String,
    @SerialName("to_city") val toCity: String,
    @SerialName("departure_time") val departureTime: String,

    val price: Double,

    @SerialName("available_seats") val availableSeats: Int,

    @SerialName("driver_name") val driverName: String? = null,
    @SerialName("car_model") val carModel: String? = null,

    // ✅ Chat uchun kerak:
    // Backend ba’zan driver_id, ba’zan driverId chiqarishi mumkin.
    @JsonNames("driver_id", "driverId")
    val driverId: String? = null,

    @JsonNames("my_role", "myRole")
    val myRole: String? = null,

    @SerialName("status") val status: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,

    @SerialName("distance_km") val distanceKm: Int? = null,
    @SerialName("duration_min") val durationMin: Int? = null
)
