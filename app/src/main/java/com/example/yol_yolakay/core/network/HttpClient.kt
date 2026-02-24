package com.example.yol_yolakay.core.network

import android.content.Context
import com.example.yol_yolakay.BuildConfig
import com.example.yol_yolakay.core.session.CurrentUser
import com.example.yol_yolakay.core.session.SessionStore
import io.ktor.client.HttpClient
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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object BackendClient {

    private const val BASE_URL = "https://yol-yolakay-backend.onrender.com/"

    @Volatile private var _client: HttpClient? = null
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
            // 🚀 MUHIM: Render.com uxlab qolsa uyg'onishi uchun 60 soniya beramiz
            connectTimeoutMillis = 60_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
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
            connectTimeoutMillis = 60_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
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

                    val raw = _rawClient ?: return@refreshTokens null

                    // 🚀 Xavfsiz tarmoq so'rovi (Exception otsa app qotmasligi uchun)
                    val resp = runCatching {
                        raw.post("api/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("refresh_token" to refresh))
                        }
                    }.getOrNull()

                    // 1. Agar internet bo'lmasa yoki timeout bo'lsa, LOGOUT QILMAYMIZ! (null qaytaramiz)
                    if (resp == null) return@refreshTokens null

                    // 2. Agar token rostdan ham o'lgan bo'lsa (401 yoki 400), ana shundagina LOGOUT qilamiz
                    if (resp.status == HttpStatusCode.Unauthorized || resp.status == HttpStatusCode.BadRequest) {
                        sessionStore.clear()
                        return@refreshTokens null
                    }

                    if (!resp.status.isSuccess()) return@refreshTokens null

                    // 3. Xavfsiz JSON parsing (Model classga qarab o'tirmaymiz)
                    val bodyText = runCatching { resp.bodyAsText() }.getOrNull() ?: return@refreshTokens null
                    val json = Json { ignoreUnknownKeys = true }

                    val jsonObj = runCatching { json.parseToJsonElement(bodyText).jsonObject }.getOrNull() ?: return@refreshTokens null

                    // Backenddagi snake_case nomlar bilan olamiz
                    val newAccess = jsonObj["access_token"]?.jsonPrimitive?.contentOrNull ?: return@refreshTokens null
                    val newRefresh = jsonObj["refresh_token"]?.jsonPrimitive?.contentOrNull ?: refresh
                    val userId = jsonObj["user_id"]?.jsonPrimitive?.contentOrNull

                    sessionStore.save(newAccess, newRefresh, userId)

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