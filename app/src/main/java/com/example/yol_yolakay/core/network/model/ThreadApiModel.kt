package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class ThreadApiModel(
    val id: String,
    val tripId: String? = null,
    val otherUserId: String,
    val otherUserName: String? = null,
    val lastMessage: String? = null,
    val updatedAt: String? = null
)
