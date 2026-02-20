package com.example.yol_yolakay.feature.profile.data

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.CarBrandDto
import com.example.yol_yolakay.core.network.model.ProfileApiModel
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import com.example.yol_yolakay.core.network.model.UpsertVehicleRequest
import com.example.yol_yolakay.core.network.model.VehicleApiModel
import com.example.yol_yolakay.core.network.model.VehicleReferenceResponse
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess

class ProfileRemoteRepository {

    private val client = BackendClient.client

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

    /**
     * ✅ Yagona to‘g‘ri yo‘l (compat endpoint): PUT /me/vehicle
     * Backend: vehicles jadvalini upsert qiladi va (agar multi table bo‘lsa) primary’ni ham sync qiladi.
     */
    suspend fun upsertVehicle(req: UpsertVehicleRequest): VehicleApiModel {
        return client.put("api/profile/me/vehicle") {
            setBody(req)
        }.body()
    }

    /**
     * ✅ MUHIM: “raqam o‘chmayapti” fix shu.
     * Backend: DELETE /me/vehicle -> DB’dan vehicles row o‘chadi.
     */
    suspend fun deleteVehicle(): Result<Unit> = try {
        val resp = client.delete("api/profile/me/vehicle")
        if (resp.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) {
        Result.failure(e)
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

    /**
     * ⚠️ Legacy: avval siz POST /profile/vehicle ishlatgansiz.
     * Endi app tomonda ishlatmaymiz. Qoldirsak ham zarar qilmaydi (orqaga moslik).
     */
    @Deprecated("Use upsertVehicle(req) instead")
    suspend fun saveVehicle(req: UpsertVehicleRequest): Result<Unit> = try {
        // Minimal risk: eski chaqiruvlar sinmasin deb, ichidan yangi endpointni chaqiramiz.
        upsertVehicle(req)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}