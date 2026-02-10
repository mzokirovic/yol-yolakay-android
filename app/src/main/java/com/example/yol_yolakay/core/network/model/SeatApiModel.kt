package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeatHolderProfileApiModel(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val rating: Double? = null
)

@Serializable
data class SeatApiModel(
    @SerialName("seat_no") val seatNo: Int,
    val status: String, // available|booked|pending|blocked
    @SerialName("holder_name") val holderName: String? = null,
    @SerialName("holder_client_id") val holderClientId: String? = null,
    @SerialName("locked_by_driver") val lockedByDriver: Boolean = false,

    // ✅ YANGI: backend yuborsa keladi, yubormasa null bo‘lib qoladi (break qilmaydi)
    @SerialName("holder_profile") val holderProfile: SeatHolderProfileApiModel? = null
)
