package com.example.yol_yolakay.core.network.model

import com.example.yol_yolakay.feature.publish.model.TripDto
import kotlinx.serialization.Serializable

@Serializable
data class TripResponse(
    val success: Boolean,
    val count: Int? = 0,
    val data: List<TripDto> // Asl safarlar ro'yxati shu yerda
)