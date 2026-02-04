package com.example.yol_yolakay

import android.app.Application
import com.example.yol_yolakay.feature.notifications.NotificationsWork

class YolYolakayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationsWork.start(this)
    }
}
