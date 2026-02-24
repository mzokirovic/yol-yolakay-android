package com.example.yol_yolakay.feature.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.yol_yolakay.MainActivity
import com.example.yol_yolakay.R
import com.example.yol_yolakay.core.network.model.NotificationApiModel

object NotificationsNotifier {

    private const val CHANNEL_ID = "updates"
    private const val CHANNEL_NAME = "Updates"
    private val SMALL_ICON = R.drawable.ic_stat_notify // Sizning iconkangiz

    fun show(ctx: Context, items: List<NotificationApiModel>) {
        if (!checkPermission(ctx)) return
        if (items.isEmpty()) return
        ensureChannel(ctx)

        val count = items.size
        val first = items.first()
        val text = first.body?.takeIf { it.isNotBlank() } ?: "Kirib ko'ring"

        // ✅ 1 ta bo‘lsa: aynan o‘sha notification’ni ochamiz (trip/thread bo‘lsa)
        if (count == 1) {
            showPush(
                ctx = ctx,
                notificationId = first.id,
                title = first.title,
                body = text,
                threadId = first.threadId,
                tripId = first.tripId
            )
            return
        }

        // ✅ ko‘p bo‘lsa: summary (Inbox -> Bildirishnomalar)
        val title = "$count ta yangi xabar"

        val intent = Intent(ctx, MainActivity::class.java).apply {
            action = "com.example.yol_yolakay.OPEN_UPDATES"
            data = android.net.Uri.parse("yolyolakay://updates/${System.currentTimeMillis()}")

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

            putExtra("open_updates", true)
            putExtra("push_title", title)
            putExtra("push_body", text)
        }

        // ✅ reqCode ham unique bo‘lsin (PendingIntent collide bo‘lmasin)
        val reqCode = intent.data.toString().hashCode()
        val pi = createPendingIntent(ctx, intent, reqCode)

        val notif = buildNotification(ctx, title, text, pi)
        try {
            NotificationManagerCompat.from(ctx).notify(1001, notif)
        } catch (_: SecurityException) {}
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

        // ✅ Unique key: har bir notif uchun UNIQUE bo‘lsin (collision bo‘lmasin)
        val key = (notificationId?.takeIf { it.isNotBlank() }
            ?: "${System.currentTimeMillis()}_${(title + body).hashCode()}")

        val intent = Intent(ctx, MainActivity::class.java).apply {
            action = "com.example.yol_yolakay.OPEN_PUSH"
            // ✅ data URI PendingIntent uniqueness’ni kuchaytiradi
            data = android.net.Uri.parse("yolyolakay://push/$key")

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

            putExtra("notification_id", notificationId)
            putExtra("thread_id", threadId)
            putExtra("trip_id", tripId)

            // ✅ agar trip/thread bo'lsa baribir open_updates=true yuborish shart emas
            putExtra("open_updates", threadId.isNullOrBlank() && tripId.isNullOrBlank())

            putExtra("push_title", title)
            putExtra("push_body", body)
        }

        // ✅ reqCode: Int overflow qilmasin, key hash ishlatamiz
        val reqCode = key.hashCode()

        val pi = createPendingIntent(ctx, intent, reqCode)
        val notif = buildNotification(ctx, title, body, pi)

        try {
            NotificationManagerCompat.from(ctx).notify(reqCode, notif)
        } catch (_: SecurityException) {}
    }

    private fun checkPermission(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        }
    }

    // 🚀 O'ZGARISH: Bu yerdagi dublikatlar olib tashlandi va Android 12+ xavfsizligi to'g'irlandi
    private fun createPendingIntent(ctx: Context, intent: Intent? = null, reqCode: Int = 1001): PendingIntent {
        val i = intent ?: Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Android 12+ xavfsizlik talablari uchun FLAG_IMMUTABLE
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(ctx, reqCode, i, flags)
    }

    private fun buildNotification(ctx: Context, title: String, body: String, pi: PendingIntent): Notification {
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