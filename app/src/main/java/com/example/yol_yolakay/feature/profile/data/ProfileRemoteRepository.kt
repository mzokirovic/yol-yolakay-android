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
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

class ProfileRemoteRepository {

    private val client = BackendClient.client

    // Ktor’dagi Json config bilan mos
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private fun JsonPrimitive.asStringOrNull(): String? =
        try {
            if (this.isString) this.content else null
        } catch (_: Exception) {
            null
        }

    private fun JsonPrimitive.asBooleanOrNull(): Boolean? =
        try {
            // eski versiyalarda booleanOrNull yo‘q — shunday qilamiz
            this.boolean
        } catch (_: Exception) {
            null
        }

    private fun extractErrorMessage(text: String): String? = runCatching {
        val el: JsonElement = json.parseToJsonElement(text)
        val obj = el as? JsonObject ?: return@runCatching null

        // success=false bo‘lsa
        val successPrim = obj["success"] as? JsonPrimitive
        val success = successPrim?.asBooleanOrNull()
        if (success == false) {
            val errEl = obj["error"]

            // error: "..."
            val errPrim = errEl as? JsonPrimitive
            val errStr = errPrim?.asStringOrNull()
            if (!errStr.isNullOrBlank()) return@runCatching errStr

            // error: { message: "..." }
            val errObj = errEl as? JsonObject
            val msgPrim = errObj?.get("message") as? JsonPrimitive
            val msg = msgPrim?.asStringOrNull()
            if (!msg.isNullOrBlank()) return@runCatching msg
        }

        // ba'zan { message: "..." }
        val msgPrim = obj["message"] as? JsonPrimitive
        val msg = msgPrim?.asStringOrNull()
        if (!msg.isNullOrBlank()) return@runCatching msg

        null
    }.getOrNull()

    private suspend inline fun <reified T> HttpResponse.decodeOrThrow(endpoint: String): T {
        val text = runCatching { bodyAsText() }.getOrElse { "" }

        // 1) HTTP non-2xx
        if (!status.isSuccess()) {
            val msg = extractErrorMessage(text) ?: text.ifBlank { "Server xatosi" }
            throw Exception("$endpoint -> HTTP ${status.value} ${status.description}\n$msg")
        }

        // 2) 2xx bo‘lsa ham backend success=false yuborishi mumkin
        val embeddedErr = extractErrorMessage(text)
        if (!embeddedErr.isNullOrBlank()) {
            throw Exception("$endpoint -> Server error\n$embeddedErr")
        }

        // 3) decode
        return try {
            json.decodeFromString(text)
        } catch (e: SerializationException) {
            throw Exception("$endpoint -> Bad response format\n$text")
        }
    }

    private suspend fun HttpResponse.ensureOkOrThrow(endpoint: String) {
        val text = runCatching { bodyAsText() }.getOrElse { "" }

        if (!status.isSuccess()) {
            val msg = extractErrorMessage(text) ?: text.ifBlank { "Server xatosi" }
            throw Exception("$endpoint -> HTTP ${status.value} ${status.description}\n$msg")
        }

        val embeddedErr = extractErrorMessage(text)
        if (!embeddedErr.isNullOrBlank()) {
            throw Exception("$endpoint -> Server error\n$embeddedErr")
        }
    }

    suspend fun getMe(): ProfileApiModel {
        val resp = client.get("api/profile/me")
        return resp.decodeOrThrow("GET /api/profile/me")
    }

    suspend fun updateMe(req: UpdateProfileRequest): ProfileApiModel {
        val resp = client.put("api/profile/me") { setBody(req) }
        return resp.decodeOrThrow("PUT /api/profile/me")
    }

    /**
     * ✅ Til uchun: ProfileApiModel parse qilmaymiz — “Illegal input” yo‘qoladi.
     */
    suspend fun updateLanguage(code: String) {
        val resp = client.put("api/profile/me") {
            setBody(mapOf("language" to code))
        }
        resp.ensureOkOrThrow("PUT /api/profile/me (language)")
    }

    suspend fun getVehicle(): VehicleApiModel? {
        val resp = client.get("api/profile/me/vehicle")
        return if (resp.status.isSuccess()) resp.body() else null
    }

    suspend fun upsertVehicle(req: UpsertVehicleRequest): VehicleApiModel {
        val resp = client.put("api/profile/me/vehicle") { setBody(req) }
        return resp.decodeOrThrow("PUT /api/profile/me/vehicle")
    }

    suspend fun deleteVehicle(): Result<Unit> = try {
        val resp = client.delete("api/profile/me/vehicle")
        if (resp.status.isSuccess()) Result.success(Unit)
        else {
            val text = runCatching { resp.bodyAsText() }.getOrElse { "" }
            Result.failure(Exception("DELETE /api/profile/me/vehicle -> HTTP ${resp.status.value}\n$text"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCarReferences(): Result<List<CarBrandDto>> = try {
        val resp = client.get("api/profile/cars")
        val body = resp.decodeOrThrow<VehicleReferenceResponse>("GET /api/profile/cars")
        Result.success(body.data)
    } catch (e: Exception) {
        Result.failure(e)
    }

    @Deprecated("Use upsertVehicle(req) instead")
    suspend fun saveVehicle(req: UpsertVehicleRequest): Result<Unit> = try {
        upsertVehicle(req)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}