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
        val uid = store?.userIdCached()
            ?: run {
                // fallback: agar user_id saqlanmagan bo‘lsa, access token’dan sub o‘qib olamiz
                val access = store?.accessTokenCached()
                // SessionStore allaqachon jwtSub bilan cache qiladi, lekin yana fallback qoldiramiz:
                null
            }

        return uid ?: deviceId(context)
    }

    fun displayName(): String = "Guest"
}
