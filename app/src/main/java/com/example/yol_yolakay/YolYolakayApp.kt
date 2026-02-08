package com.example.yol_yolakay

import android.app.Application
import com.example.yol_yolakay.core.di.AppGraph

class YolYolakayApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ✅ avval session+client tayyor
        AppGraph.init(this)

    }
}
