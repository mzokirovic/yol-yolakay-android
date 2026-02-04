package com.example.yol_yolakay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.yol_yolakay.core.session.CurrentUser
import com.example.yol_yolakay.feature.notifications.NotificationsRemoteRepository
import com.example.yol_yolakay.main.MainScreen
import com.example.yol_yolakay.ui.theme.YolYolakayTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class AppDeepLink(
    val notificationId: String? = null,
    val threadId: String? = null,
    val tripId: String? = null,
    val openUpdates: Boolean = false,
    val title: String? = null,
    val body: String? = null
)

class MainActivity : ComponentActivity() {

    private val deepLinkState = mutableStateOf<AppDeepLink?>(null)

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLinkState.value = parseIntent(intent)
        markReadBestEffort(deepLinkState.value?.notificationId)

        setContent {
            YolYolakayTheme {
                MainScreen(
                    deepLink = deepLinkState.value,
                    onDeepLinkHandled = { deepLinkState.value = null }
                )
            }
        }

        requestPostNotificationsIfNeeded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        deepLinkState.value = parseIntent(intent)
        markReadBestEffort(deepLinkState.value?.notificationId)
    }

    private fun parseIntent(i: Intent?): AppDeepLink? {
        if (i == null) return null

        val notifId = i.getStringExtra("notification_id")
        val threadId = i.getStringExtra("thread_id")
        val tripId = i.getStringExtra("trip_id")
        val openUpdates = i.getBooleanExtra("open_updates", false)

        val title = i.getStringExtra("push_title")
        val body = i.getStringExtra("push_body")

        if (notifId.isNullOrBlank() && threadId.isNullOrBlank() && tripId.isNullOrBlank() && !openUpdates) {
            return null
        }

        return AppDeepLink(
            notificationId = notifId,
            threadId = threadId,
            tripId = tripId,
            openUpdates = openUpdates,
            title = title,
            body = body
        )
    }

    private fun markReadBestEffort(notificationId: String?) {
        if (notificationId.isNullOrBlank()) return
        val uid = CurrentUser.id(this)

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                NotificationsRemoteRepository(uid).markRead(notificationId)
            }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
