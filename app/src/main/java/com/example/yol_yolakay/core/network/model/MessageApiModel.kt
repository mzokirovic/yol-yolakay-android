package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class MessageApiModel(
    val id: String,

    @JsonNames("thread_id", "threadId")
    val thread_id: String? = null,

    @JsonNames("sender_id", "senderId")
    val sender_id: String,

    val text: String,

    @JsonNames("created_at", "createdAt")
    val created_at: String? = null
)
