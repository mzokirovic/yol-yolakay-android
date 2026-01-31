package com.example.yol_yolakay.feature.publish.model

// ESKI IMPORTLARNI O'CHIRIB, SHULARNI YOZING:
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable // Endi bu qizil bo'lmasligi kerak
data class TripDto(
    @SerialName("fromLocation") val fromLocation: String,
    @SerialName("toLocation") val toLocation: String,

    @SerialName("fromLat") val fromLat: Double = 41.2995,
    @SerialName("fromLng") val fromLng: Double = 69.2401,
    @SerialName("toLat") val toLat: Double = 39.6542,
    @SerialName("toLng") val toLng: Double = 66.9597,

    @SerialName("date") val date: String,
    @SerialName("time") val time: String,
    @SerialName("price") val price: Double,
    @SerialName("seats") val seats: Int,
    @SerialName("driverId") val driverId: String = "test-driver-id"
)