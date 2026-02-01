package com.example.yol_yolakay.data.repository

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.BookSeatRequest
import com.example.yol_yolakay.core.network.model.TripApiModel
import com.example.yol_yolakay.core.network.model.TripDetailsResponse
import com.example.yol_yolakay.core.network.model.TripResponse
import com.example.yol_yolakay.feature.publish.model.PublishTripRequest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class TripRepository {

    private val client = BackendClient.client

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

        if (resp.status.isSuccess()) {
            val body = resp.body<TripResponse>()
            if (body.success) Result.success(body.data)
            else Result.failure(Exception("Search success=false"))
        } else {
            Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun publishTrip(req: PublishTripRequest): Result<Unit> = try {
        val resp = client.post("api/trips/publish") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (resp.status.isSuccess()) Result.success(Unit)
        else Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getMyTrips(driverName: String): Result<List<TripApiModel>> = try {
        val resp = client.get("api/trips/my") {
            parameter("driverName", driverName)
        }
        if (resp.status.isSuccess()) {
            val body = resp.body<TripResponse>()
            if (body.success) Result.success(body.data)
            else Result.failure(Exception("MyTrips success=false"))
        } else {
            Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTripDetails(tripId: String): Result<TripDetailsResponse> = try {
        val resp = client.get("api/trips/$tripId")
        if (resp.status.isSuccess()) Result.success(resp.body())
        else Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun bookSeat(
        tripId: String,
        seatNo: Int,
        clientId: String,
        holderName: String? = null
    ): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/book") {
            contentType(ContentType.Application.Json)
            setBody(BookSeatRequest(clientId = clientId, holderName = holderName))
        }
        if (resp.status.isSuccess()) Result.success(resp.body())
        else Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ✅ Driver: block seat
    suspend fun blockSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/block")
        if (resp.status.isSuccess()) Result.success(resp.body())
        else Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ✅ Driver: unblock seat
    suspend fun unblockSeat(tripId: String, seatNo: Int): Result<TripDetailsResponse> = try {
        val resp = client.post("api/trips/$tripId/seats/$seatNo/unblock")
        if (resp.status.isSuccess()) Result.success(resp.body())
        else Result.failure(Exception("HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
