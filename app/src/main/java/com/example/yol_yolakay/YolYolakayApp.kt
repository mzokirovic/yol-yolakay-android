package com.example.yol_yolakay

import android.app.Application
import com.example.yol_yolakay.core.di.AppGraph
import com.example.yol_yolakay.feature.notifications.NotificationsWork

class YolYolakayApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ✅ avval session+client tayyor
        AppGraph.init(this)

        // ✅ keyin worker
        NotificationsWork.start(this)
    }
}
