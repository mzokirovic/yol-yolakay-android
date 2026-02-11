package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val success: Boolean? = null,
    val error: ApiError? = null,
    val message: String? = null
) {
    @Serializable
    data class ApiError(
        val message: String? = null
    )

    fun bestMessage(): String? = error?.message ?: message
}
