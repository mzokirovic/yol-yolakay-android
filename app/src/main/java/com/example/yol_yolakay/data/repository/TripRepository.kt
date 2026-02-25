package com.example.yol_yolakay.data.repository

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.ApiErrorResponse
import com.example.yol_yolakay.core.network.model.TripApiModel
import com.example.yol_yolakay.core.network.model.TripDetailsResponse
import com.example.yol_yolakay.core.network.model.TripResponse
import com.example.yol_yolakay.feature.publish.LocationModel
import com.example.yol_yolakay.feature.publish.model.PublishTripRequest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class TripRepository {

    private val client = BackendClient.client

    // --- REQUEST/RESPONSE DTOs ---
    @Serializable
    private data class SeatRequestBody(val holderName: String? = null)

    @Serializable
    private data class PointsResponse(val success: Boolean, val data: List<PointDto>)

    @Serializable
    private data class PointDto(
        val id: String,
        val city_name: String,
        val point_name: String,
        val latitude: Double,
        val longitude: Double,
        val region_name: String? = null
    )

    @Serializable
    data class PricePreviewRequest(
        val fromLat: Double,
        val fromLng: Double,
        val toLat: Double,
        val toLng: Double
    )

    @Serializable
    data class PricePreviewResponse(
        val success: Boolean,
        val distanceKm: Int,
        val recommended: Double,
        val min: Double,
        val max: Double
    )

    @Serializable
    data class RoutePreviewRequest(
        val fromLat: Double,
        val fromLng: Double,
        val toLat: Double,
        val toLng: Double
    )

    @Serializable
    data class RoutePreviewResponse(
        val success: Boolean,
        val provider: String = "unknown",
        val polyline: String? = null,  // encoded polyline (precision=5)
        val distanceKm: Int = 0,
        val durationMin: Int = 0,
        val error: String? = null
    )

    // --- ERROR PARSER (FINAL) ---
    private val errJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private class BackendException(
        val status: Int,
        override val message: String
    ) : Exception(message)

    private suspend fun HttpResponse.prettyError(prefix: String): BackendException {
        val fallback = "$prefix: HTTP ${status.value}"

        val text = runCatching { bodyAsText() }
            .getOrNull()
            .orEmpty()
            .trim()

        if (text.isBlank()) return BackendException(status.value, fallback)

        val msg = runCatching {
            // HTML yoki oddiy text qaytsa ham yiqilmasin
            errJson.decodeFromString<ApiErrorResponse>(text).bestMessage()
        }.getOrNull()

        val finalMsg = msg?.takeIf { it.isNotBlank() }
            ?: text.take(200).takeIf { it.isNotBlank() }
            ?: fallback

        return BackendException(status.value, finalMsg)
    }

    private suspend inline fun <reified T> HttpResponse.bodyOrFail(prefix: String): Result<T> = try {
        if (status.isSuccess()) Result.success(body())
        else Result.failure(prettyError(prefix))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun HttpResponse.unitOrFail(prefix: String): Result<Unit> = try {
        if (status.isSuccess()) Result.success(Unit)
        else Result.failure(prettyError(prefix))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // --- METHODS ---

    suspend fun getPricePreview(
        fromLat: Double, fromLng: Double, toLat: Double, toLng: Double
    ): Result<PricePreviewResponse> = try {
        val resp = client.post("api/trips/calculate-price") {
            contentType(ContentType.Application.Json)
            setBody(PricePreviewRequest(fromLat, fromLng, toLat, toLng))
        }
        resp.bodyOrFail("PricePreview")
    } catch (e: Exception) {
        Result.failure(e)
    }


    suspend fun getRoutePreview(
        fromLat: Double, fromLng: Double, toLat: Double, toLng: Double
    ): Result<RoutePreviewResponse> = try {
        val resp = client.post("api/trips/calculate-route") {
            contentType(ContentType.Application.Json)
            setBody(RoutePreviewRequest(fromLat, fromLng, toLat, toLng))
        }
        resp.bodyOrFail("RoutePreview")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPopularPoints(city: String? = null): Result<List<LocationModel>> = try {
        val resp = client.get("api/trips/points") {
            if (!city.isNullOrBlank()) parameter("city", city)
        }

        resp.bodyOrFail<PointsResponse>("Points")
            .map { body ->
                body.data.map { dto ->
                    LocationModel(
                        name = "${dto.city_name} (${dto.point_name})",
                        lat = dto.latitude,
                        lng = dto.longitude,
                        pointId = dto.id,
                        region = dto.region_name ?: "Boshqa"
                    )
                }
            }
    } catch (e: Exception) {
        // MVP fallback (offline/demo)
        val mockPoints = listOf(
            LocationModel("Toshkent (Olmazor Metro)", 41.2858, 69.2040, null, "Toshkent shahri"),
            LocationModel("Toshkent (Qo'yliq)", 41.2345, 69.3456, null, "Toshkent shahri")
        )
        Result.success(mockPoints)
    }

    suspend fun searchTrips(
        from: String? = null,
        to: String? = null,
        date: String? = null,
        passengers: Int? = null
    ): Result<List<TripApiModel>> = try {
        val resp = client.get("api/trips/search") {
            if (!from.isNullOrBlank()) parameter("from", from)
            if (!to.isNullOrBlank()) parameter("to", to)
            if (!date.isNullOrBlank()) parameter("date", date)
            if (passengers != null) parameter("passengers", passengers)
        }

        resp.bodyOrFail<TripResponse>("Search").map { body ->
            if (body.success) body.data else throw Exception("Search success=false")
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun publishTrip(req: PublishTripRequest): Result<Unit> = try {
        val resp = client.post("api/trips/publish") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        resp.unitOrFail("PublishTrip")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getMyTrips(): Result<List<TripApiModel>> = try {
        val resp = client.get("api/trips/my")
        resp.bodyOrFail<TripResponse>("MyTrips").map { body ->
            if (body.success) body.data else throw Exception("MyTrips success=false")
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTripDetails(tripId: String): Result<TripDetailsResponse> = try {
        val resp = client.get("api/trips/$tripId")
        resp.bodyOrFail("TripDetails")
    } catch (e: Exception) {
        Result.failure(e)
    }

    // --- SEATS ---
    suspend fun requestSeat(tripId: String, seatNo: Int, holderName: String? = null): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/request") {
            contentType(ContentType.Application.Json)
            setBody(SeatRequestBody(holderName))
        }
        resp.bodyOrFail("RequestSeat")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cancelSeatRequest(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/cancel")
        resp.bodyOrFail("CancelSeat")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun approveSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/approve")
        resp.bodyOrFail("ApproveSeat")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rejectSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/reject")
        resp.bodyOrFail("RejectSeat")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun blockSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/block")
        resp.bodyOrFail("BlockSeat")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun unblockSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/unblock")
        resp.bodyOrFail("UnblockSeat")
    } catch (e: Exception) {
        Result.failure(e)
    }

    // --- LIFECYCLE ---
    suspend fun startTrip(tripId: String): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/start")
        resp.bodyOrFail("StartTrip")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun finishTrip(tripId: String): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/finish")
        resp.bodyOrFail("FinishTrip")
    } catch (e: Exception) {
        Result.failure(e)
    }
}
