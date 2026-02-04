package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterPushTokenRequest(
    val token: String,
    val platform: String = "android"
)
