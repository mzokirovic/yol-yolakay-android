package com.example.yol_yolakay

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // ✅ IMPORT QILINDI
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.yol_yolakay.core.di.AppGraph
import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.session.SessionStore
import com.example.yol_yolakay.feature.auth.AuthScreen
import com.example.yol_yolakay.feature.notifications.NotificationsRemoteRepository
import com.example.yol_yolakay.feature.notifications.NotificationsWork
import com.example.yol_yolakay.feature.profile.completion.CompleteProfileScreen
import com.example.yol_yolakay.feature.profile.data.ProfileRemoteRepository
import com.example.yol_yolakay.main.MainScreen
import com.example.yol_yolakay.ui.theme.YolYolakayTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.yol_yolakay.core.i18n.LanguageStore
import kotlinx.coroutines.runBlocking

data class AppDeepLink(
    val notificationId: String? = null,
    val threadId: String? = null,
    val tripId: String? = null,
    val openUpdates: Boolean = false,
    val title: String? = null,
    val body: String? = null
)

class MainActivity : AppCompatActivity() {

    private val deepLinkState = mutableStateOf<AppDeepLink?>(null)
    private lateinit var sessionStore: SessionStore

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) syncFcmTokenIfLoggedIn()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(this)

        // ✅ ILova to'liq ekranga yoyildi (Edge to Edge dizayn)
        enableEdgeToEdge()

        runBlocking {
            val lang = LanguageStore(applicationContext).get()
            if (!lang.isNullOrBlank()) {
                LanguageStore.apply(lang)
            }
        }

        sessionStore = AppGraph.sessionStore(this)
        BackendClient.init(this, sessionStore)

        createNotificationChannel()

        deepLinkState.value = parseIntent(intent)
        markReadBestEffort(deepLinkState.value?.notificationId)

        setContent {
            YolYolakayTheme {
                // Surface orqali orqa fon belgilangan (Endi tizim panellari ostiga kiradi)
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(
                        sessionStore = sessionStore,
                        deepLink = deepLinkState.value,
                        onDeepLinkHandled = { deepLinkState.value = null }
                    )
                }
            }
        }

        requestPostNotificationsIfNeeded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Yangi intentni o'rnatamiz, shunda parseIntent to'g'ri o'qiydi
        setIntent(intent)
        val newDeepLink = parseIntent(intent)
        if (newDeepLink != null) {
            deepLinkState.value = newDeepLink
            markReadBestEffort(newDeepLink.notificationId)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "updates"
            val channelName = "Updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Ilova yangiliklari va xabarlari"
                enableVibration(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun parseIntent(i: Intent?): AppDeepLink? {
        if (i == null) return null

        val notifId = i.getStringExtra("notification_id")
        val threadId = i.getStringExtra("thread_id")
        val tripId = i.getStringExtra("trip_id")

        // Agar intent.extras null bo'lmasa tekshiramiz
        val rawOpenUpdates = i.getBooleanExtra("open_updates", false)
        val openUpdates = if (!threadId.isNullOrBlank() || !tripId.isNullOrBlank()) false else rawOpenUpdates

        val title = i.getStringExtra("push_title")
        val body = i.getStringExtra("push_body")

        if (notifId.isNullOrBlank() && threadId.isNullOrBlank() && tripId.isNullOrBlank() && !openUpdates) {
            return null
        }

        // Qayta ishlab ketmasligi uchun intent extraslarini tozalab qo'yamiz
        i.removeExtra("notification_id")
        i.removeExtra("thread_id")
        i.removeExtra("trip_id")
        i.removeExtra("open_updates")

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
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { NotificationsRemoteRepository().markRead(notificationId) }
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

    private fun syncFcmTokenIfLoggedIn() {
        lifecycleScope.launch(Dispatchers.IO) {
            val uid = sessionStore.userIdOrNull() ?: return@launch
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                NotificationsRemoteRepository().registerPushToken(token)
                Log.d("FCM", "Token synced (permission flow) uid=$uid")
            }
        }
    }
}

@Composable
private fun AppRoot(
    sessionStore: SessionStore,
    deepLink: AppDeepLink?,
    onDeepLinkHandled: () -> Unit
) {
    val isLoggedIn by sessionStore.isLoggedIn.collectAsState(
        initial = !sessionStore.accessTokenCached().isNullOrBlank()
    )

    var authCompleted by remember { mutableStateOf(false) }
    var profileState by remember { mutableStateOf<ProfileState>(ProfileState.Loading) }

    val ctx = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    val hasFastDeepLink = remember(deepLink) {
        deepLink?.let { !it.threadId.isNullOrBlank() || !it.tripId.isNullOrBlank() } == true
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            NotificationsWork.start(ctx)
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                NotificationsRemoteRepository().registerPushToken(token)
                Log.d("FCM", "Token synced on login")
            }
        } else {
            NotificationsWork.stop(ctx)
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            profileState = ProfileState.LoggedOut
            return@LaunchedEffect
        }

        profileState = ProfileState.Loading
        runCatching {
            val p = ProfileRemoteRepository().getMe()
            val name = p.displayName?.trim() ?: ""
            val needs = name.isBlank() || name.equals("Guest", ignoreCase = true)
            profileState = if (needs) ProfileState.NeedsCompletion else ProfileState.Complete
        }.onFailure {
            Log.e("AppRoot", "Profile check failed", it)
            profileState = ProfileState.Error
        }
    }

    if (isLoggedIn && hasFastDeepLink) {
        MainScreen(
            deepLink = deepLink,
            onDeepLinkHandled = onDeepLinkHandled
        )
        return
    }

    when (profileState) {
        ProfileState.LoggedOut -> {
            AuthScreen(
                onNavigateToHome = { authCompleted = true },
                onNavigateToCompleteProfile = { authCompleted = true }
            )
        }

        ProfileState.Loading -> {
            // ✅ Edge to edge bo'lganda elementlar qisilib qolmasligi uchun systemBarsPadding() qilingan
            Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        ProfileState.Error -> {
            // ✅ Edge to edge bo'lganda elementlar qisilib qolmasligi uchun systemBarsPadding() qilingan
            Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Internet bilan aloqa yo'q", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { authCompleted = !authCompleted }) {
                        Text("Qayta urinish")
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { scope.launch { sessionStore.clear() } }) {
                        Text("Chiqish")
                    }
                }
            }
        }

        ProfileState.NeedsCompletion -> {
            CompleteProfileScreen(
                repo = remember { ProfileRemoteRepository() },
                onDone = { profileState = ProfileState.Complete }
            )
        }

        ProfileState.Complete -> {
            MainScreen(
                deepLink = deepLink,
                onDeepLinkHandled = onDeepLinkHandled
            )
        }
    }
}

enum class ProfileState {
    LoggedOut,
    Loading,
    Error,
    NeedsCompletion,
    Complete
}