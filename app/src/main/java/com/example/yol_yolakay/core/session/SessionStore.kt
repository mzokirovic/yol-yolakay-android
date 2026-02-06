package com.example.yol_yolakay.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("session_store")

class SessionStore(private val appContext: Context) {

    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
    }

    val isLoggedIn: Flow<Boolean> =
        appContext.dataStore.data.map { prefs -> !prefs[Keys.ACCESS].isNullOrBlank() }

    suspend fun userIdOrNull(): String? =
        appContext.dataStore.data.first()[Keys.USER_ID]

    suspend fun save(accessToken: String, refreshToken: String?, userId: String?) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.ACCESS] = accessToken
            if (!refreshToken.isNullOrBlank()) prefs[Keys.REFRESH] = refreshToken
            if (!userId.isNullOrBlank()) prefs[Keys.USER_ID] = userId
        }
    }

    suspend fun clear() {
        appContext.dataStore.edit { it.clear() }
    }

    suspend fun bearerTokensOrNull(): BearerTokens? {
        val prefs = appContext.dataStore.data.first()
        val access = prefs[Keys.ACCESS]
        if (access.isNullOrBlank()) return null
        val refresh = prefs[Keys.REFRESH].orEmpty()
        return BearerTokens(access, refresh)
    }
}
