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
        // Tokenni faqat user login qilgan bo'lsa yangilaymiz
        val userId = CurrentUser.id(applicationContext)
        if (userId.isNotEmpty()) {
            serviceScope.launch {
                try {
                    NotificationsRemoteRepository(userId).registerPushToken(token)
                } catch (e: Exception) {
                    // Jimgina logga yozamiz, foydalanuvchiga bildirish shart emas
                    Log.w("FCM", "Token yangilashda xatolik", e)
                }
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