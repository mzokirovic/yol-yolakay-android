package com.example.yol_yolakay.feature.notifications

import com.example.yol_yolakay.core.session.CurrentUser
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YolYolakayFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // 1) lokal saqlaymiz
        NotificationsStore.saveFcmToken(this, token)

        // 2) backendga yuboramiz (best-effort)
        val uid = CurrentUser.id(this)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { NotificationsRemoteRepository(uid).registerPushToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data

        val title = data["title"] ?: "Yo‘l-yo‘lakay"
        val body = data["body"] ?: ""

        val notificationId = data["notification_id"] ?: data["notificationId"]
        val threadId = data["thread_id"] ?: data["threadId"]
        val tripId = data["trip_id"] ?: data["tripId"]

        NotificationsNotifier.showPush(
            ctx = this,
            notificationId = notificationId,
            title = title,
            body = body,
            threadId = threadId,
            tripId = tripId
        )
    }
}
