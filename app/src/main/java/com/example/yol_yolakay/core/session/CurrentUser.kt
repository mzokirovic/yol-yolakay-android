package com.example.yol_yolakay.core.session

import android.content.Context
import android.provider.Settings

object CurrentUser {
    fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device"

    // backward compat
    fun id(context: Context): String = deviceId(context)

    fun displayName(): String = "Guest"
}
