// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/publish/model/PublishTripRequest.kt

package com.example.yol_yolakay.feature.publish.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublishTripRequest(
    // Backend "fromLocation" va "toLocation" nomlarini kutadi
    @SerialName("fromLocation") val fromLocation: String,
    @SerialName("toLocation") val toLocation: String,

    // Koordinatalar (Controller buni start_lat ga o'girib oladi)
    @SerialName("fromLat") val fromLat: Double,
    @SerialName("fromLng") val fromLng: Double,
    @SerialName("toLat") val toLat: Double,
    @SerialName("toLng") val toLng: Double,

    @SerialName("fromPointId") val fromPointId: String? = null,
    @SerialName("toPointId") val toPointId: String? = null,

    // Format: "YYYY-MM-DD" va "HH:MM"
    @SerialName("date") val date: String,
    @SerialName("time") val time: String,

    @SerialName("price") val price: Double,
    @SerialName("seats") val seats: Int,

    // Backend Headerdan ID ni oladi, lekin ba'zi hollarda bodyda ham ketgani yaxshi (optional)
    @SerialName("driverId") val driverId: String? = null,

    // Ixtiyoriy (Backend o'zi to'ldiradi, lekin yuborish zarar qilmaydi)
    @SerialName("driverName") val driverName: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("carModel") val carModel: String? = null
)