package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageApiModel(
    val id: String,
    val thread_id: String? = null,   // backend snake_case qaytsa ham ignoreUnknownKeys bor
    val sender_id: String,
    val text: String,
    val created_at: String? = null
)
