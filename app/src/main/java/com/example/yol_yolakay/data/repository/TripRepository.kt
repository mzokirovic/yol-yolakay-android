// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/data/repository/TripRepository.kt

package com.example.yol_yolakay.data.repository

import com.example.yol_yolakay.core.network.BackendClient
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
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

class TripRepository {

    private val client = BackendClient.client

    // 🚨 attachUser helperi olib tashlandi (Global Interceptor ishlaydi)

    // --- DATA CLASSES ---
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

    // --- METHODS ---

    suspend fun getPricePreview(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Result<PricePreviewResponse> = try {
        val resp = client.post("api/trips/calculate-price") {
            contentType(ContentType.Application.Json)
            setBody(PricePreviewRequest(fromLat, fromLng, toLat, toLng))
        }
        if (resp.status.isSuccess()) {
            Result.success(resp.body())
        } else {
            Result.failure(Exception("HTTP ${resp.status.value}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPopularPoints(city: String? = null): Result<List<LocationModel>> = try {
        val resp = client.get("api/trips/points") {
            if (!city.isNullOrBlank()) parameter("city", city)
        }
        if (resp.status.isSuccess()) {
            val body = resp.body<PointsResponse>()
            val uiList = body.data.map { dto ->
                LocationModel(
                    name = "${dto.city_name} (${dto.point_name})",
                    lat = dto.latitude,
                    lng = dto.longitude,
                    pointId = dto.id,
                    region = dto.region_name ?: "Boshqa"
                )
            }
            Result.success(uiList)
        } else {
            Result.failure(Exception("HTTP ${resp.status.value}"))
        }
    } catch (e: Exception) {
        val mockPoints = listOf(
            LocationModel("Toshkent (Olmazor Metro)", 41.2858, 69.2040, "mock_1", "Toshkent shahri"),
            LocationModel("Toshkent (Qo'yliq)", 41.2345, 69.3456, "mock_2", "Toshkent shahri")
        )
        Result.success(mockPoints)
    }

    suspend fun searchTrips(from: String? = null, to: String? = null, date: String? = null, passengers: Int? = null): Result<List<TripApiModel>> = try {
        val resp = client.get("api/trips/search") {
            if (!from.isNullOrBlank()) parameter("from", from)
            if (!to.isNullOrBlank()) parameter("to", to)
            if (!date.isNullOrBlank()) parameter("date", date)
            if (passengers != null) parameter("passengers", passengers)
        }
        if (resp.status.isSuccess()) {
            val body = resp.body<TripResponse>()
            if (body.success) Result.success(body.data) else Result.failure(Exception("Search success=false"))
        } else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun publishTrip(req: PublishTripRequest): Result<Unit> = try {
        val resp = client.post("api/trips/publish") {
            // attachUser kerak emas, ID headerdan olinadi
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (resp.status.isSuccess()) Result.success(Unit) else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getMyTrips(): Result<List<TripApiModel>> = try {
        val resp = client.get("api/trips/my")
        if (resp.status.isSuccess()) {
            val body = resp.body<TripResponse>()
            if (body.success) Result.success(body.data) else Result.failure(Exception("MyTrips success=false"))
        } else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getTripDetails(tripId: String): Result<TripDetailsResponse> = try {
        val resp = client.get("api/trips/$tripId")
        if (resp.status.isSuccess()) Result.success(resp.body()) else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    // ✅ YANGI: argumentlar tozalandi (faqat logika uchun keraklilari qoldi)
    suspend fun requestSeat(tripId: String, seatNo: Int, holderName: String? = null): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/request") {
            contentType(ContentType.Application.Json)
            setBody(SeatRequestBody(holderName))
        }
        if (resp.status.isSuccess()) Result.success(resp.body()) else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun cancelSeatRequest(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/cancel")
        if (resp.status.isSuccess()) Result.success(resp.body()) else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun approveSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/approve")
        if (resp.status.isSuccess()) Result.success(resp.body()) else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun rejectSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/reject")
        if (resp.status.isSuccess()) Result.success(resp.body()) else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun blockSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/block")
        if (resp.status.isSuccess()) Result.success(resp.body()) else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun unblockSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/unblock")
        if (resp.status.isSuccess()) Result.success(resp.body()) else Result.failure(Exception("HTTP ${resp.status.value}"))
    } catch (e: Exception) { Result.failure(e) }
}