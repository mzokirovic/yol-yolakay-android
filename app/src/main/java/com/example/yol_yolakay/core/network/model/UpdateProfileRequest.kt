package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val language: String? = null
)
