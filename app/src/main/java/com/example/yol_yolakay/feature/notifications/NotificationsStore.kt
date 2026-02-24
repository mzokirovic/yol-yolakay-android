package com.example.yol_yolakay.feature.notifications

import android.content.Context
import com.example.yol_yolakay.core.network.model.NotificationApiModel

object NotificationsStore {

    private const val PREF = "notif_sync"
    private const val KEY_IDS_CSV = "shown_ids_csv"
    private const val KEY_FCM = "fcm_token"
    private const val MAX = 50

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ✅ ordered list read
    private fun readIds(ctx: Context): MutableList<String> {
        val raw = prefs(ctx).getString(KEY_IDS_CSV, "").orEmpty().trim()
        if (raw.isBlank()) return mutableListOf()
        return raw.split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()
    }

    // ✅ ordered list write (limit guaranteed)
    private fun writeIds(ctx: Context, ids: List<String>) {
        val trimmed = if (ids.size <= MAX) ids else ids.takeLast(MAX)
        prefs(ctx).edit().putString(KEY_IDS_CSV, trimmed.joinToString("|")).apply()
    }

    fun filterNew(ctx: Context, unread: List<NotificationApiModel>): List<NotificationApiModel> {
        val shown = readIds(ctx).toHashSet()
        return unread.filterNot { shown.contains(it.id) }
    }

    fun remember(ctx: Context, shownNow: List<NotificationApiModel>) {
        val ids = readIds(ctx)

        for (n in shownNow) {
            // ✅ uniq + keep recent last
            ids.remove(n.id)
            ids.add(n.id)
        }

        writeIds(ctx, ids)
    }

    fun rememberId(ctx: Context, id: String) {
        if (id.isBlank()) return
        val ids = readIds(ctx)
        ids.remove(id)
        ids.add(id)
        writeIds(ctx, ids)
    }

    fun saveFcmToken(ctx: Context, token: String) {
        prefs(ctx).edit().putString(KEY_FCM, token).apply()
    }

    fun getFcmToken(ctx: Context): String? =
        prefs(ctx).getString(KEY_FCM, null)
}