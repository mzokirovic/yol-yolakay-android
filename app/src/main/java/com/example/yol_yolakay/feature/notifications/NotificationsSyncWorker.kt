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
        return runCatching {
            val uid = CurrentUser.id(applicationContext)
            val repo = NotificationsRemoteRepository(uid)

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