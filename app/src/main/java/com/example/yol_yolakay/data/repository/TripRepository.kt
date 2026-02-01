package com.example.yol_yolakay.data.repository

import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.network.model.TripResponse
import com.example.yol_yolakay.feature.publish.model.TripDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class TripRepository {

    // Ktor Clientni chaqiramiz
    private val client = BackendClient.client

    // Safarlarni qidirish funksiyasi
    // Parametrlar ixtiyoriy (null bo'lishi mumkin)
    suspend fun searchTrips(from: String? = null, to: String? = null, date: String? = null): Result<List<TripDto>> {
        return try {
            // Serverga GET so'rov
            val response = client.get("api/trips/search") {
                // Agar parametrlar bo'lsa, URLga qo'shamiz (?from=...&to=...)
                if (!from.isNullOrBlank()) parameter("from", from)
                if (!to.isNullOrBlank()) parameter("to", to)
                if (!date.isNullOrBlank()) parameter("date", date)
            }.body<TripResponse>() // Javobni modelga o'giramiz

            if (response.success) {
                // Muvaffaqiyatli bo'lsa, ro'yxatni qaytaramiz
                Result.success(response.data)
            } else {
                Result.failure(Exception("Server muvaffaqiyatsiz javob berdi"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Internet yo'q yoki server xatosi
            Result.failure(e)
        }
    }
}