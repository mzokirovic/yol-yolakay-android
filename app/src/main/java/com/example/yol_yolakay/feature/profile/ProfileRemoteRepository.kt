package com.example.yol_yolakay.feature.profile

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.ProfileApiModel
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import com.example.yol_yolakay.core.network.model.UpsertVehicleRequest
import com.example.yol_yolakay.core.network.model.VehicleApiModel
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class ProfileRemoteRepository(
    private val userId: String? = null // ✅ Null bo'lishi mumkin (optional)
) {

    // So'rovga user ID qo'shish (faqat mavjud bo'lsa)
    private fun HttpRequestBuilder.attachUser() {
        if (!userId.isNullOrBlank()) {
            header("x-user-id", userId)
        }
    }

    suspend fun getMe(): ProfileApiModel {
        return BackendClient.client.get("api/profile/me") {
            attachUser()
        }.body()
    }

    suspend fun updateMe(req: UpdateProfileRequest): ProfileApiModel {
        return BackendClient.client.put("api/profile/me") {
            attachUser()
            setBody(req)
        }.body()
    }

    suspend fun getVehicle(): VehicleApiModel? {
        return BackendClient.client.get("api/profile/me/vehicle") {
            attachUser()
        }.body()
    }

    suspend fun upsertVehicle(req: UpsertVehicleRequest): VehicleApiModel {
        return BackendClient.client.put("api/profile/me/vehicle") {
            attachUser()
            setBody(req)
        }.body()
    }
}