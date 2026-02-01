package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class TripDetailsResponse(
    val success: Boolean,
    val trip: TripApiModel,
    val seats: List<SeatApiModel> = emptyList()
)
