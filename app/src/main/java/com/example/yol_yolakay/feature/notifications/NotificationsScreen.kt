package com.example.yol_yolakay.feature.notifications

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.di.AppGraph
import com.example.yol_yolakay.core.network.model.NotificationApiModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class NotificationsVmFactory(
    private val context: Context? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotificationsViewModel(NotificationsRemoteRepository()) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onOpenTrip: (String) -> Unit,
    onOpenThread: (String) -> Unit,
    vm: NotificationsViewModel = viewModel(factory = NotificationsVmFactory())
) {
    val ctx = LocalContext.current.applicationContext
    val store = remember { AppGraph.sessionStore(ctx) }

    val isLoggedIn by store.isLoggedIn
        .map { it }
        .distinctUntilChanged()
        .collectAsState(initial = false)

    LaunchedEffect(isLoggedIn) { vm.onLoginState(isLoggedIn) }

    val state = vm.state
    val pullState = rememberPullToRefreshState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bildirishnomalar",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    if (state.isLoggedIn && state.items.any { !it.isRead }) {
                        TextButton(onClick = { vm.markAllRead() }) {
                            Text("Barchasini o'qish", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullToRefresh(
                    isRefreshing = state.isLoading,
                    onRefresh = { vm.refresh() },
                    state = pullState,
                    enabled = state.isLoggedIn
                )
        ) {
            when {
                !state.isLoggedIn -> LoggedOutState()

                state.error != null && state.items.isEmpty() -> ErrorState(
                    message = state.error ?: "Xatolik",
                    onRetry = { vm.refresh() }
                )

                state.items.isEmpty() && state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                state.items.isEmpty() -> EmptyState()

                else -> NotificationsList(
                    items = state.items,
                    onClick = { n ->
                        // 🚀 1. O'qilgan deb belgilaymiz
                        vm.markRead(n.id)

                        // 🚀 2. To'g'ridan-to'g'ri kerakli ekranga o'tamiz (Dialogsiz)
                        if (!n.threadId.isNullOrBlank()) {
                            onOpenThread(n.threadId)
                        } else if (!n.tripId.isNullOrBlank()) {
                            onOpenTrip(n.tripId)
                        }
                    }
                )
            }

            if (state.isLoggedIn && state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREMIUM UI KOMPONENTLARI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsList(
    items: List<NotificationApiModel>,
    onClick: (NotificationApiModel) -> Unit
) {
    val sorted = remember(items) {
        items.sortedWith(
            compareBy<NotificationApiModel> { it.isRead }
                .thenByDescending { it.createdAt ?: it.id }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp) // Bottom padding navigatsiya uchun
    ) {
        items(sorted, key = { it.id }) { n ->
            NotificationRow(n = n, onClick = { onClick(n) })
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
private fun NotificationRow(
    n: NotificationApiModel,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val timeText = remember(n.createdAt) { relativeTimeUz(n.createdAt) }

    // Xabar turiga qarab icon va rang tanlash
    val (icon, iconBgColor, iconTintColor) = when {
        !n.threadId.isNullOrBlank() -> Triple(Icons.Rounded.ChatBubbleOutline, Color(0xFFE3F2FD), Color(0xFF1976D2)) // Blue for chat
        !n.tripId.isNullOrBlank() -> Triple(Icons.Rounded.DirectionsCar, Color(0xFFE8F5E9), Color(0xFF388E3C)) // Green for rides
        else -> Triple(Icons.Rounded.Info, Color(0xFFF5F5F5), Color(0xFF616161)) // Gray for general
    }

    // O'qilmagan xabarlar fonini sal farqlash
    val backgroundColor = if (!n.isRead) cs.primary.copy(alpha = 0.04f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Ikonka qismi
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTintColor, modifier = Modifier.size(24.dp))

            // Unread Blue Dot
            if (!n.isRead) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(cs.surface)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(cs.primary)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // Text qismi
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = n.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!n.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    color = cs.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (!n.isRead) cs.primary else cs.onSurfaceVariant
                )
            }

            n.body?.takeIf { it.isNotBlank() }?.let { body ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// Qolgan yordamchi funksiyalar (LoggedOutState, EmptyState, relativeTimeUz) o'zgarishsiz qoldi.
// Ular pastda xuddi avvalgidek qolaveradi.

@Composable
private fun LoggedOutState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Text("Kirish talab qilinadi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Xatolik: $message", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Qayta urinish") }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(Modifier.height(16.dp))
            Text("Hozircha bildirishnomalar yo‘q", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        }
    }
}

private fun relativeTimeUz(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return "Hozir"
    val instant = runCatching { Instant.parse(createdAt) }
        .getOrElse { runCatching { OffsetDateTime.parse(createdAt).toInstant() }.getOrNull() } ?: return "Hozir"

    val now = Instant.now()
    val diff = Duration.between(instant, now)

    return when {
        diff.toMinutes() < 1 -> "Hozir"
        diff.toMinutes() < 60 -> "${diff.toMinutes()} daq"
        diff.toHours() < 24 -> "${diff.toHours()} soat"
        diff.toDays() == 1L -> "Kecha"
        diff.toDays() < 7 -> "${diff.toDays()} kun"
        else -> instant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }
}