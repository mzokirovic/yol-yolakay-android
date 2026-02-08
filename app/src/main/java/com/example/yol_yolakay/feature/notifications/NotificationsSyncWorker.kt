package com.example.yol_yolakay.feature.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.yol_yolakay.core.session.CurrentUser

class NotificationsSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val session = com.example.yol_yolakay.core.session.SessionStore(applicationContext)
        if (session.bearerTokensOrNull() == null) {
            return Result.success() // ✅ login yo'q -> network yo'q
        }

        return runCatching {
            val repo = NotificationsRemoteRepository()
            val list = repo.list()
            val unread = list.filter { !it.isRead }

            val newOnes = NotificationsStore.filterNew(applicationContext, unread)
            if (newOnes.isNotEmpty()) {
                NotificationsNotifier.show(applicationContext, newOnes)
                NotificationsStore.remember(applicationContext, newOnes)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}