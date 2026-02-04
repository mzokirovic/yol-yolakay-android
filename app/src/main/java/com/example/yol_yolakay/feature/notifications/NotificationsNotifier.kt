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

    // ✅ eski (polling/list)
    fun show(ctx: Context, items: List<NotificationApiModel>) {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        if (items.isEmpty()) return
        ensureChannel(ctx)

        val count = items.size
        val title = if (count == 1) items.first().title else "Yangiliklar: $count ta yangi"
        val text = items.first().body?.takeIf { it.isNotBlank() } ?: "Ilovaga kirib ko‘ring"

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val pi = PendingIntent.getActivity(ctx, 1001, intent, flags)

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(ctx).notify(1001, notif)
    }

    // ✅ Push (FCM) — click bo‘lganda kerakli joyga olib o‘tadi + info ko‘rsatadi
    fun showPush(
        ctx: Context,
        notificationId: String?,
        title: String,
        body: String,
        threadId: String?,
        tripId: String?
    ) {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        ensureChannel(ctx)

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_id", notificationId)
            putExtra("thread_id", threadId)
            putExtra("trip_id", tripId)
            putExtra("open_updates", true)

            // ✅ snackbar uchun
            putExtra("push_title", title)
            putExtra("push_body", body)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val reqCode = (notificationId?.hashCode() ?: 2001)
        val pi = PendingIntent.getActivity(ctx, reqCode, intent, flags)

        val safeTitle = title.ifBlank { "Yo‘l-yo‘lakay" }
        val safeBody = body.ifBlank { "Ilovaga kirib ko‘ring" }

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(safeTitle)
            .setContentText(safeBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(safeBody))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(ctx).notify(reqCode, notif)
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}
