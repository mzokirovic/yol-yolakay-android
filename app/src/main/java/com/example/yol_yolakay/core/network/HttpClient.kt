package com.example.yol_yolakay.core.network

import android.content.Context
import com.example.yol_yolakay.BuildConfig
import com.example.yol_yolakay.core.session.CurrentUser
import com.example.yol_yolakay.core.session.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BackendClient {

    private const val BASE_URL = "https://yol-yolakay-backend.onrender.com/"

    @Volatile private var _client: HttpClient? = null

    fun init(context: Context, sessionStore: SessionStore) {
        if (_client != null) return
        synchronized(this) {
            if (_client != null) return
            _client = build(context.applicationContext, sessionStore)
        }
    }

    val client: HttpClient
        get() = _client ?: error("BackendClient.init(context, sessionStore) chaqirilmagan!")

    private fun build(appContext: Context, sessionStore: SessionStore) = HttpClient(Android) {

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = BuildConfig.DEBUG
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }

        if (BuildConfig.DEBUG) {
            install(Logging) { level = LogLevel.ALL }
        }

        install(Auth) {
            bearer {
                loadTokens { sessionStore.bearerTokensOrNull() }
                sendWithoutRequest { req ->
                    // auth endpointlarga bearer yubormaymiz
                    !req.url.encodedPath.startsWith("/api/auth")
                }
            }
        }

        defaultRequest {
            url(BASE_URL)
            contentType(ContentType.Application.Json)

            // guest migratsiya uchun
            headers.append("X-Device-Id", CurrentUser.deviceId(appContext))
        }
    }
}
