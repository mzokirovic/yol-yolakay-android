package com.example.yol_yolakay

import android.app.Application
import com.example.yol_yolakay.core.di.AppGraph
import com.example.yol_yolakay.core.i18n.LanguageStore
import kotlinx.coroutines.runBlocking

class YolYolakayApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ✅ Avval locale apply (Activity ochilishidan oldin)
        runBlocking {
            val lang = LanguageStore(applicationContext).get() ?: "uz"
            LanguageStore.apply(lang)
        }

        // ✅ Keyin DI / Backend init
        AppGraph.init(this)
    }
}