package com.example.yol_yolakay.core.network

import android.content.Context
import com.example.yol_yolakay.BuildConfig
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

object BackendClient {

    private const val BASE_URL = "https://yol-yolakay-backend.onrender.com/"

    @Volatile private var _client: HttpClient? = null

    // ✅ Auth plugin’siz raw client (refresh recursion bo‘lmasligi uchun)
    @Volatile private var _rawClient: HttpClient? = null

    private lateinit var sessionStore: SessionStore

    fun init(context: Context, store: SessionStore) {
        if (_client != null) return
        synchronized(this) {
            if (_client != null) return
            sessionStore = store
            CurrentUser.bind(store)

            val appCtx = context.applicationContext
            _rawClient = buildRaw(appCtx)
            _client = buildAuthed(appCtx, store)
        }
    }

    val client: HttpClient
        get() = _client ?: error("BackendClient.init(context, sessionStore) chaqirilmagan!")

    private fun buildRaw(appContext: Context) = HttpClient(Android) {
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
                        android.util.Log.d("KtorRaw", message)
                    }
                }
            }
        }

        defaultRequest {
            url(BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }

    private fun buildAuthed(appContext: Context, sessionStore: SessionStore) = HttpClient(Android) {

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
                    if (tokens != null) BearerTokens(tokens.accessToken, tokens.refreshToken)
                    else null
                }

                refreshTokens {
                    val refresh = sessionStore.refreshTokenCached()
                    if (refresh.isNullOrBlank()) return@refreshTokens null

                    // ✅ MUHIM: refresh uchun Auth’siz raw client ishlatamiz
                    val raw = _rawClient ?: return@refreshTokens null

                    val resp = raw.post("api/auth/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("refresh_token" to refresh))
                    }

                    if (!resp.status.isSuccess()) {
                        // refresh ishlamasa sessionni tozalaymiz (nazoratli)
                        sessionStore.clear()
                        return@refreshTokens null
                    }

                    val body =
                        resp.body<com.example.yol_yolakay.feature.auth.AuthRemoteRepository.AuthResponse>()

                    val newAccess = body.token ?: return@refreshTokens null
                    val newRefresh = body.refreshToken ?: refresh

                    sessionStore.save(newAccess, newRefresh, body.userId)

                    BearerTokens(newAccess, newRefresh)
                }

                // Auth endpointlarda bearer yubormaymiz
                sendWithoutRequest { req ->
                    !req.url.encodedPath.contains("/auth/")
                }
            }
        }

        defaultRequest {
            url(BASE_URL)
            contentType(ContentType.Application.Json)

            // ✅ Device/User headerlar
            header("X-Client-Id", CurrentUser.id(appContext))
            header("X-Device-Id", CurrentUser.deviceId(appContext))
        }
    }

    fun reset() {
        _client?.close()
        _client = null

        _rawClient?.close()
        _rawClient = null
    }
}
