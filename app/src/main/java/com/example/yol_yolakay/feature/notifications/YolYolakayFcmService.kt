package com.example.yol_yolakay.feature.notifications

import android.util.Log
import com.example.yol_yolakay.core.network.model.NotificationApiModel
import com.example.yol_yolakay.core.session.CurrentUser
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YolYolakayFcmService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // ✅ har doim saqlab qo'yamiz
        NotificationsStore.saveFcmToken(applicationContext, token)

        serviceScope.launch {
            try {
                val session = com.example.yol_yolakay.core.session.SessionStore(applicationContext)
                if (session.bearerTokensOrNull() != null) {
                    NotificationsRemoteRepository().registerPushToken(token)
                }
            } catch (e: Exception) {
                Log.w("FCM", "Token register xatolik", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // 1. Bizning Backend asosan "data" payload ishlatadi
        if (message.data.isNotEmpty()) {
            val data = message.data
            NotificationsNotifier.showPush(
                ctx = applicationContext,
                notificationId = data["notification_id"],
                title = data["title"] ?: "YolYolakay",
                body = data["body"] ?: "Yangi xabar",
                threadId = data["thread_id"],
                tripId = data["trip_id"]
            )

            // ✅ DEDUPE: push kelganini eslab qolamiz
            val nid = data["notification_id"]
            if (!nid.isNullOrBlank()) {
                NotificationsStore.rememberId(applicationContext, nid)
            }
        }

        // 2. Fallback: Agar Firebase konsol orqali yuborilsa
        else {
            message.notification?.let {
                NotificationsNotifier.showPush(
                    ctx = applicationContext,
                    notificationId = null,
                    title = it.title ?: "YolYolakay",
                    body = it.body ?: "",
                    threadId = null,
                    tripId = null
                )
            }
        }
    }
}