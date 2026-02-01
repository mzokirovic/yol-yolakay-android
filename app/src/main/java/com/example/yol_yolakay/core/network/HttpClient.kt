package com.example.yol_yolakay.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BackendClient {

    // ---------------------------------------------------------------------
    // MUHIM: Renderdagi URLingizni shu yerga qo'ying.
    // Oxirida "/api/" bo'lishi shart (chunki backend route shunday tuzilgan)
    // ---------------------------------------------------------------------
    private const val BASE_URL = "https://yol-yolakay-backend.onrender.com/"
    // ^^^ Yuqoridagi manzilni o'zingizning Render URLingizga almashtiring!

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        // Loglarni yoqib qo'yamiz, serverga nima ketayotganini ko'rish uchun
        install(Logging) {
            level = LogLevel.ALL
        }

        defaultRequest {
            url(BASE_URL)
        }
    }
}