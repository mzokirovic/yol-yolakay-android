package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicProfileApiModel(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val rating: Double? = null
)
