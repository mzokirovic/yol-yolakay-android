package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class ThreadApiModel(
    val id: String,

    @JsonNames("trip_id", "tripId")
    val tripId: String? = null,

    @JsonNames("other_user_id", "otherUserId")
    val otherUserId: String,

    @JsonNames("other_user_name", "otherUserName")
    val otherUserName: String? = null,

    @JsonNames("last_message", "lastMessage")
    val lastMessage: String? = null,

    @JsonNames("updated_at", "updatedAt")
    val updatedAt: String? = null
)
