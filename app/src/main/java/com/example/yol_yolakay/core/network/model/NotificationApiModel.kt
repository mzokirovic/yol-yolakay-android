package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationApiModel(
    val id: String,
    val type: String,
    val title: String,
    val body: String? = null,

    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("seat_no") val seatNo: Int? = null,

    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)
