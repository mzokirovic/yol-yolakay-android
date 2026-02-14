// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/core/network/HttpClient.kt

package com.example.yol_yolakay.core.network

import android.content.Context
import com.example.yol_yolakay.BuildConfig // Agar qizil bo'lsa, paket nomini tekshiring
import com.example.yol_yolakay.core.session.CurrentUser
import com.example.yol_yolakay.core.session.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import io.ktor.client.call.body


object BackendClient {

    private const val BASE_URL = "https://yol-yolakay-backend.onrender.com/"

    @Volatile
    private var _client: HttpClient? = null

    // SessionStore ni saqlab qolamiz, chunki u header uchun kerak
    private lateinit var sessionStore: SessionStore

    fun init(context: Context, store: SessionStore) {
        if (_client != null) return
        synchronized(this) {
            if (_client != null) return
            sessionStore = store
            CurrentUser.bind(store)
            _client = build(context.applicationContext, store)
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
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }

        if (BuildConfig.DEBUG) {
            install(Logging) {
                level = LogLevel.ALL
                logger = object : Logger {
                    override fun log(message: String) {
                        android.util.Log.d("KtorClient", message)
                    }
                }
            }
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val tokens = sessionStore.bearerTokensOrNull()
                    if (tokens != null) BearerTokens(tokens.accessToken, tokens.refreshToken ?: "")
                    else null
                }

                refreshTokens {
                    val refresh = sessionStore.refreshTokenCached()
                    if (refresh.isNullOrBlank()) return@refreshTokens null

                    // refresh endpointga bearer yuborilmaydi (sendWithoutRequest /auth/ ni skip qiladi)
                    val resp = client.post("api/auth/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("refresh_token" to refresh))
                    }

                    if (!resp.status.isSuccess()) {
                        // refresh ishlamasa sessionni tozalaymiz — bu aniq va nazoratli
                        sessionStore.clear()
                        android.util.Log.e("HttpClient", "Unauthorized -> NOT clearing session here", Throwable())

                    }

                    val body = resp.body<com.example.yol_yolakay.feature.auth.AuthRemoteRepository.AuthResponse>()
                    val newAccess = body.token ?: return@refreshTokens null
                    val newRefresh = body.refreshToken ?: refresh

                    sessionStore.save(newAccess, newRefresh, body.userId)

                    BearerTokens(newAccess, newRefresh)
                }

                sendWithoutRequest { req ->
                    !req.url.encodedPath.contains("/auth/")
                }
            }
        }


        defaultRequest {
            url(BASE_URL)
            contentType(ContentType.Application.Json)

            // ✅ REAL USER ID ni headerga qo'shish (Eng muhim joy)
            // Ktor da runBlocking safe emas, lekin client init bo'lganda bu muammo tug'dirmaydi.
            // Yoki yaxshirog'i: Headerga ID ni har bir requestda dinamik qo'shamiz.

            // Guest/Device ID
            header("X-Client-Id", CurrentUser.id(appContext))   // ✅ login bo‘lsa uid, bo‘lmasa deviceId
            header("X-Device-Id", CurrentUser.deviceId(appContext))


        }
    }

    fun reset() {
        _client?.close()
        _client = null
    }
}