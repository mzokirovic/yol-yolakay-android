package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class VehicleApiModel(
    val userId: String,
    val make: String? = null,
    val model: String? = null,
    val color: String? = null,
    val plate: String? = null,
    val seats: Int? = null,
    val updatedAt: String? = null
)
