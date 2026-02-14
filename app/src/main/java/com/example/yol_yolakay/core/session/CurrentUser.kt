package com.example.yol_yolakay.core.session

import android.content.Context
import android.provider.Settings

object CurrentUser {

    @Volatile private var store: SessionStore? = null

    fun bind(store: SessionStore) {
        this.store = store
    }

    fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device"

    /**
     * ✅ Beton mantiq saqlanadi:
     * - login bo‘lsa: clientId = Supabase uid
     * - login bo‘lmasa: clientId = ANDROID_ID
     */
    fun id(context: Context): String {
        val s = store
        val uid =
            s?.userIdCached()
                ?: s?.accessTokenCached()?.let { jwtSub(it) }

        return uid ?: deviceId(context)
    }

    private fun jwtSub(accessToken: String): String? {
        return try {
            val parts = accessToken.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val jsonBytes = android.util.Base64.decode(
                payload,
                android.util.Base64.URL_SAFE or
                        android.util.Base64.NO_WRAP or
                        android.util.Base64.NO_PADDING
            )
            val obj = org.json.JSONObject(String(jsonBytes))
            obj.optString("sub").takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }


    fun displayName(): String = "Guest"
}
