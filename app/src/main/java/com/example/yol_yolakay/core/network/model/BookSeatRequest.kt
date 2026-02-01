package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class BookSeatRequest(
    val clientId: String,
    val holderName: String? = null
)
