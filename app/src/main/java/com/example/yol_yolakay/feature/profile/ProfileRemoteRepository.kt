package com.example.yol_yolakay.feature.profile

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class ProfileRemoteRepository(
    private val userId: String
) {
    private fun io.ktor.client.request.HttpRequestBuilder.attachUser() {
        header("x-user-id", userId)
    }

    suspend fun getMe(): ProfileApiModel =
        BackendClient.client.get("api/profile/me") {
            attachUser()
        }.body()

    suspend fun updateMe(req: UpdateProfileRequest): ProfileApiModel =
        BackendClient.client.put("api/profile/me") {
            attachUser()
            setBody(req)
        }.body()

    suspend fun getVehicle(): VehicleApiModel? =
        BackendClient.client.get("api/profile/me/vehicle") {
            attachUser()
        }.body()

    suspend fun upsertVehicle(req: UpsertVehicleRequest): VehicleApiModel =
        BackendClient.client.put("api/profile/me/vehicle") {
            attachUser()
            setBody(req)
        }.body()
}
