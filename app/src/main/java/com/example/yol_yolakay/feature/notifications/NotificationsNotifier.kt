package com.example.yol_yolakay.feature.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.yol_yolakay.MainActivity
import com.example.yol_yolakay.R
import com.example.yol_yolakay.core.network.model.NotificationApiModel

object NotificationsNotifier {

    private const val CHANNEL_ID = "updates"
    private const val CHANNEL_NAME = "Updates"
    private val SMALL_ICON = R.drawable.ic_stat_notify

    // Polling (eski usul) uchun
    fun show(ctx: Context, items: List<NotificationApiModel>) {
        if (!checkPermission(ctx)) return
        if (items.isEmpty()) return
        ensureChannel(ctx)

        val count = items.size
        val title = if (count == 1) items.first().title else "$count ta yangi xabar"
        val text = items.first().body?.takeIf { !it.isNullOrBlank() } ?: "Kirib ko'ring"

        val pi = createPendingIntent(ctx)

        val notif = buildNotification(ctx, title, text, pi)
        try {
            NotificationManagerCompat.from(ctx).notify(1001, notif)
        } catch (e: SecurityException) {
            // Ruxsat yo'q bo'lsa
        }
    }

    // Push (FCM) uchun
    fun showPush(
        ctx: Context,
        notificationId: String?,
        title: String,
        body: String,
        threadId: String?,
        tripId: String?
    ) {
        if (!checkPermission(ctx)) return
        ensureChannel(ctx)

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_id", notificationId)
            putExtra("thread_id", threadId)
            putExtra("trip_id", tripId)
            putExtra("open_updates", true)
            putExtra("push_title", title)
            putExtra("push_body", body)
        }

        // Har bir xabar uchun unikal ID (hashcode)
        val reqCode = notificationId?.hashCode() ?: System.currentTimeMillis().toInt()
        val pi = createPendingIntent(ctx, intent, reqCode)

        val notif = buildNotification(ctx, title, body, pi)

        try {
            NotificationManagerCompat.from(ctx).notify(reqCode, notif)
        } catch (e: SecurityException) {
            // Ruxsat yo'q
        }
    }

    private fun checkPermission(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        }
    }

    private fun createPendingIntent(ctx: Context, intent: Intent? = null, reqCode: Int = 1001): PendingIntent {
        val i = intent ?: Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(ctx, reqCode, i, flags)
    }

    private fun buildNotification(ctx: Context, title: String, body: String, pi: PendingIntent): android.app.Notification {
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(SMALL_ICON)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Ilova yangiliklari va xabarlari"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }
}