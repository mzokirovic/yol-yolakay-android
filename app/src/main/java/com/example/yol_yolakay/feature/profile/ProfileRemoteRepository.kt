// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/profile/ProfileRemoteRepository.kt

package com.example.yol_yolakay.feature.profile

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.CarBrandDto
import com.example.yol_yolakay.core.network.model.ProfileApiModel
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import com.example.yol_yolakay.core.network.model.UpsertVehicleRequest
import com.example.yol_yolakay.core.network.model.VehicleApiModel
import com.example.yol_yolakay.core.network.model.VehicleReferenceResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

// 🚨 O'ZGARISH: Konstruktor bo'sh. Argument kerak emas.
class ProfileRemoteRepository {

    private val client = BackendClient.client

    // 🚨 O'ZGARISH: attachUser() funksiyasi olib tashlandi.
    // Endi BackendClient o'zi header qo'shadi.

    suspend fun getMe(): ProfileApiModel {
        return client.get("api/profile/me").body()
    }

    suspend fun updateMe(req: UpdateProfileRequest): ProfileApiModel {
        return client.put("api/profile/me") {
            setBody(req)
        }.body()
    }

    suspend fun getVehicle(): VehicleApiModel? {
        return client.get("api/profile/me/vehicle").body()
    }

    suspend fun upsertVehicle(req: UpsertVehicleRequest): VehicleApiModel {
        return client.put("api/profile/me/vehicle") {
            setBody(req)
        }.body()
    }

    suspend fun getCarReferences(): Result<List<CarBrandDto>> = try {
        val resp = client.get("api/profile/cars")
        if (resp.status.isSuccess()) {
            val body = resp.body<VehicleReferenceResponse>()
            Result.success(body.data)
        } else {
            Result.failure(Exception("HTTP ${resp.status.value}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun saveVehicle(req: UpsertVehicleRequest): Result<Unit> = try {
        val resp = client.post("api/profile/vehicle") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (resp.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}