package com.example.yol_yolakay.core.di

import android.content.Context
import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.session.SessionStore

object AppGraph {
    @Volatile private var _session: SessionStore? = null

    fun sessionStore(context: Context): SessionStore =
        _session ?: synchronized(this) {
            _session ?: SessionStore(context.applicationContext).also { _session = it }
        }

    fun init(context: Context) {
        val store = sessionStore(context)
        BackendClient.init(context.applicationContext, store)
    }
}
