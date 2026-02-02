package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileApiModel(
    val userId: String,
    val displayName: String,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val language: String = "uz",
    val updatedAt: String? = null
)
