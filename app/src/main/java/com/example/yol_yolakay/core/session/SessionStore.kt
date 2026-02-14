package com.example.yol_yolakay.core.session

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("session_store")

class SessionStore(private val appContext: Context) {

    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
    }

    // ✅ RAM cache (CurrentUser.id() suspend bo‘lmasligi uchun)
    @Volatile
    private var cachedAccess: String? = null
    @Volatile
    private var cachedRefresh: String? = null
    @Volatile
    private var cachedUserId: String? = null

    init {
        // App start bo‘lganda 1 marta cache’ni to‘ldiramiz
        runBlocking {
            val prefs = appContext.dataStore.data.first()
            cachedAccess = prefs[Keys.ACCESS]
            cachedRefresh = prefs[Keys.REFRESH]
            cachedUserId = prefs[Keys.USER_ID] ?: jwtSub(prefs[Keys.ACCESS])
        }
    }

    val isLoggedIn: Flow<Boolean> =
        appContext.dataStore.data.map { prefs -> !prefs[Keys.ACCESS].isNullOrBlank() }

    // ✅ CurrentUser uchun tezkor getterlar (NON-suspend)
    fun userIdCached(): String? = cachedUserId
    fun accessTokenCached(): String? = cachedAccess
    fun refreshTokenCached(): String? = cachedRefresh


    suspend fun userIdOrNull(): String? =
        appContext.dataStore.data.first()[Keys.USER_ID]

    suspend fun save(accessToken: String, refreshToken: String?, userId: String?) {
        val uid = userId?.takeIf { it.isNotBlank() } ?: jwtSub(accessToken)

        appContext.dataStore.edit { prefs ->
            prefs[Keys.ACCESS] = accessToken
            if (!refreshToken.isNullOrBlank()) prefs[Keys.REFRESH] = refreshToken
            if (!uid.isNullOrBlank()) prefs[Keys.USER_ID] = uid
        }

        // ✅ cache update
        cachedAccess = accessToken
        cachedRefresh = refreshToken
        cachedUserId = uid
    }

    suspend fun clear() {
        appContext.dataStore.edit { it.clear() }

        // ✅ cache clear
        cachedAccess = null
        cachedRefresh = null
        cachedUserId = null
    }

    suspend fun bearerTokensOrNull(): BearerTokens? {
        val prefs = appContext.dataStore.data.first()
        val access = prefs[Keys.ACCESS]
        if (access.isNullOrBlank()) return null
        val refresh = prefs[Keys.REFRESH].orEmpty()
        return BearerTokens(access, refresh)
    }

    private fun jwtSub(accessToken: String?): String? {
        return try {
            if (accessToken.isNullOrBlank()) {
                null
            } else {
                val parts = accessToken.split(".")
                if (parts.size < 2) {
                    null
                } else {
                    val payload = parts[1]
                    val jsonBytes = android.util.Base64.decode(
                        payload,
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                    )
                    val obj = org.json.JSONObject(String(jsonBytes))
                    obj.optString("sub").takeIf { it.isNotBlank() }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }
}
