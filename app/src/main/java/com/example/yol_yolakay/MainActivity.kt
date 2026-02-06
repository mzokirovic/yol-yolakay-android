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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.di.AppGraph
import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.session.SessionStore
import com.example.yol_yolakay.feature.auth.AuthRemoteRepository
import com.example.yol_yolakay.feature.auth.AuthScreen
import com.example.yol_yolakay.feature.auth.AuthViewModel
import com.example.yol_yolakay.feature.auth.AuthViewModelFactory
import com.example.yol_yolakay.feature.notifications.NotificationsRemoteRepository
import com.example.yol_yolakay.feature.profile.CompleteProfileScreen
import com.example.yol_yolakay.feature.profile.ProfileRemoteRepository
import com.example.yol_yolakay.main.MainScreen
import com.example.yol_yolakay.ui.theme.YolYolakayTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// DeepLink ma'lumotlari
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
    private lateinit var sessionStore: SessionStore

    // Ruxsat so'rash (Android 13+)
    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) syncFcmTokenIfLoggedIn()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Tizimni ishga tushirish
        AppGraph.init(this)
        sessionStore = AppGraph.sessionStore(this)

        // 2. Notification Kanalini yaratish (Muhim: Push kechikmasligi uchun)
        createNotificationChannel()

        // 3. Deep linkni ushlab olish
        deepLinkState.value = parseIntent(intent)
        markReadBestEffort(deepLinkState.value?.notificationId)

        // 4. Ekranni chizish
        setContent {
            YolYolakayTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(
                        sessionStore = sessionStore,
                        deepLink = deepLinkState.value,
                        onDeepLinkHandled = { deepLinkState.value = null }
                    )
                }
            }
        }

        // 5. Ruxsat va Token ishlari
        requestPostNotificationsIfNeeded()
        syncFcmTokenIfLoggedIn()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkState.value = parseIntent(intent)
        markReadBestEffort(deepLinkState.value?.notificationId)
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
        val openUpdates = i.getBooleanExtra("open_updates", false)
        val title = i.getStringExtra("push_title")
        val body = i.getStringExtra("push_body")

        if (notifId.isNullOrBlank() && threadId.isNullOrBlank() && tripId.isNullOrBlank() && !openUpdates) {
            return null
        }
        return AppDeepLink(notifId, threadId, tripId, openUpdates, title, body)
    }

    private fun markReadBestEffort(notificationId: String?) {
        if (notificationId.isNullOrBlank()) return
        lifecycleScope.launch(Dispatchers.IO) {
            val uid = sessionStore.userIdOrNull() ?: return@launch
            runCatching { NotificationsRemoteRepository(uid).markRead(notificationId) }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun syncFcmTokenIfLoggedIn() {
        lifecycleScope.launch(Dispatchers.IO) {
            val uid = sessionStore.userIdOrNull() ?: return@launch
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    lifecycleScope.launch(Dispatchers.IO) {
                        runCatching { NotificationsRemoteRepository(uid).registerPushToken(token) }
                    }
                }
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
    val isLoggedIn by sessionStore.isLoggedIn.collectAsState(initial = false)
    val context = LocalContext.current

    // ✅ SENIOR FIX: Login bo'lgan zahoti tokenni yangilash
    // Bu oldingi "Login qildim, lekin push kelmadi" muammosini 100% yechadi.
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val uid = sessionStore.userIdOrNull()
            if (!uid.isNullOrBlank()) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        // UI o'lib qolsa ham ishlashi uchun GlobalScope ishlatamiz (xavfsiz)
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                            runCatching {
                                NotificationsRemoteRepository(uid).registerPushToken(token)
                                Log.d("FCM", "Token synced on login: $token")
                            }
                        }
                    }
                }
            }
        }
    }

    if (!isLoggedIn) {
        val repo = remember { AuthRemoteRepository(BackendClient.client) }
        val vm: AuthViewModel = viewModel(factory = AuthViewModelFactory(repo, sessionStore))
        AuthScreen(
            vm = vm,
            onNavigateToHome = { },
            onNavigateToCompleteProfile = { }
        )
    } else {
        val profileRepo = remember { ProfileRemoteRepository() }

        // Uchta holat: Yuklanmoqda | Xatolik | Profil Kerak | Tayyor
        var isLoading by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) } // ✅ Yangi state (Internet yo'qligi uchun)
        var needsProfile by remember { mutableStateOf(false) }

        // Profilni tekshirish funksiyasi
        fun checkProfile() {
            isLoading = true
            isError = false
            // Main Dispatcherda ishga tushamiz, lekin so'rovni IO da bajaramiz
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                runCatching {
                    withContext(Dispatchers.IO) { profileRepo.getMe() }
                }
                    .onSuccess { p ->
                        // ✅ Space (Probel) muammosini shu yerda trim() orqali hal qilamiz
                        val name = p.displayName?.trim() ?: ""

                        // Agar ism bo'sh bo'lsa -> Demak yangi yoki to'ldirilmagan -> Profil oynasiga
                        // Agar ism bor bo'lsa -> Main Screen ga
                        needsProfile = name.isBlank() || name.equals("Guest", ignoreCase = true)
                        isLoading = false
                    }
                    .onFailure {
                        // ✅ ENG MUHIM JOY: Xato bo'lsa, adashib Registratsiyaga o'tib ketmaymiz!
                        // Foydalanuvchiga "Qayta urinish" tugmasini beramiz.
                        Log.e("AppRoot", "Profile check failed", it)
                        isError = true
                        isLoading = false
                    }
            }
        }

        // Ilova ochilganda bir marta tekshirsin
        LaunchedEffect(Unit) {
            checkProfile()
        }

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            isError -> {
                // ✅ Internet uzilganda chiqadigan oyna
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Internet bilan aloqa yo'q", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { checkProfile() }) {
                            Text("Qayta urinish")
                        }
                    }
                }
            }
            needsProfile -> {
                CompleteProfileScreen(
                    repo = profileRepo,
                    onDone = { needsProfile = false }
                )
            }
            else -> {
                MainScreen(
                    deepLink = deepLink,
                    onDeepLinkHandled = onDeepLinkHandled
                )
            }
        }
    }
}