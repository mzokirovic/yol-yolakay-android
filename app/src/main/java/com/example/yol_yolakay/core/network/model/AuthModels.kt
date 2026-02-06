package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable data class SendOtpRequest(val phone: String)
@Serializable data class VerifyOtpRequest(val phone: String, val code: String)

@Serializable
data class AuthResponse(
    val userId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val message: String? = null,
    val isNewUser: Boolean = false
)
