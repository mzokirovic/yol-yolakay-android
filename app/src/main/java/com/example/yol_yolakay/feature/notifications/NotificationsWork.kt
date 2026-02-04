package com.example.yol_yolakay.feature.notifications

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object NotificationsWork {

    private const val UNIQUE_NAME = "notifications_sync"

    fun start(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = PeriodicWorkRequestBuilder<NotificationsSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}
