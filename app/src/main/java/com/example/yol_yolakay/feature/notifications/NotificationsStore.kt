package com.example.yol_yolakay.feature.notifications

import android.content.Context
import com.example.yol_yolakay.core.network.model.NotificationApiModel

object NotificationsStore {

    private const val PREF = "notif_sync"
    private const val KEY_IDS = "shown_ids"
    private const val KEY_FCM = "fcm_token"
    private const val MAX = 50

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun filterNew(ctx: Context, unread: List<NotificationApiModel>): List<NotificationApiModel> {
        val shown = prefs(ctx).getStringSet(KEY_IDS, emptySet()) ?: emptySet()
        return unread.filterNot { shown.contains(it.id) }
    }

    fun remember(ctx: Context, shownNow: List<NotificationApiModel>) {
        val p = prefs(ctx)
        val current = (p.getStringSet(KEY_IDS, emptySet()) ?: emptySet()).toMutableSet()
        shownNow.forEach { current.add(it.id) }

        // ✅ LIMIT (takeLast ishlatmaymiz)
        val list = current.toList()
        val trimmedList =
            if (list.size <= MAX) list
            else list.drop(list.size - MAX)

        p.edit().putStringSet(KEY_IDS, trimmedList.toSet()).apply()
    }

    fun saveFcmToken(ctx: Context, token: String) {
        prefs(ctx).edit().putString(KEY_FCM, token).apply()
    }

    fun getFcmToken(ctx: Context): String? =
        prefs(ctx).getString(KEY_FCM, null)
}
