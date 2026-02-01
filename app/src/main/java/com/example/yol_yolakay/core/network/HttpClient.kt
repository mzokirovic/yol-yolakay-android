package com.example.yol_yolakay.core.network

import com.example.yol_yolakay.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BackendClient {

    private const val BASE_URL = "https://yol-yolakay-backend.onrender.com/"

    val client = HttpClient(Android) {

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = BuildConfig.DEBUG
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        // ✅ Timeoutlar (osilib qolmasin)
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }

        // ✅ Logging faqat DEBUG’da
        if (BuildConfig.DEBUG) {
            install(Logging) {
                level = LogLevel.ALL
            }
        }

        defaultRequest {
            url(BASE_URL)
        }
    }
}
