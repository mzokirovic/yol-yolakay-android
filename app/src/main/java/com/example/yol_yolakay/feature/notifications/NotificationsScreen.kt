package com.example.yol_yolakay.feature.notifications

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

// ✅ BACKWARD-COMPATIBLE: MainScreen NotificationsVmFactory(ctx) deb chaqiryapti
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

    // ✅ Material3 pull-to-refresh STATE (sizdagi versiyada isRefreshing/endRefresh yo‘q)
    val pullState = rememberPullToRefreshState()

    // ✅ Details bottom sheet
    var sheetItem by remember { mutableStateOf<NotificationApiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bildirishnomalar", fontWeight = FontWeight.SemiBold)
                        if (state.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Badge { Text(state.unreadCount.toString()) }
                        }
                    }
                },
                actions = {
                    // ✅ Refresh tugmasiz (faqat pull-to-refresh)
                    TextButton(
                        onClick = { vm.markAllRead() },
                        enabled = state.isLoggedIn && state.items.isNotEmpty()
                    ) { Text("O‘qildi") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullToRefresh(
                    // ✅ sizdagi API shuni kutadi
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
                        CircularProgressIndicator()
                    }
                }

                state.items.isEmpty() -> EmptyState()

                else -> NotificationsList(
                    items = state.items,
                    onClick = { n ->
                        vm.markRead(n.id)
                        sheetItem = n
                    }
                )
            }

            // ✅ Pull indikator o‘rniga: Uber-like top loading bar
            if (state.isLoggedIn && state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }
        }
    }

    // ✅ Details sheet
    sheetItem?.let { n ->
        NotificationDetailsSheet(
            n = n,
            onDismiss = { sheetItem = null },
            onOpenTrip = {
                sheetItem = null
                n.tripId?.let(onOpenTrip)
            },
            onOpenThread = {
                sheetItem = null
                n.threadId?.let(onOpenThread)
            }
        )
    }
}

@Composable
private fun LoggedOutState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Kirish talab qilinadi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bildirishnomalarni ko‘rish uchun akkauntga kiring.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Xatolik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Qayta urinish") }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Yoki tepadan pastga tortib yangilang",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Hozircha bildirishnomalar yo‘q",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tepadan pastga tortib tekshirib turing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sorted, key = { it.id }) { n ->
            NotificationRow(n = n, onClick = { onClick(n) })
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun NotificationRow(
    n: NotificationApiModel,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val container = if (!n.isRead) cs.primaryContainer else cs.surfaceVariant
    val titleWeight = if (!n.isRead) FontWeight.SemiBold else FontWeight.Medium
    val timeText = remember(n.createdAt) { relativeTimeUz(n.createdAt) }

    val leadingIcon = when {
        !n.threadId.isNullOrBlank() -> Icons.Default.ChatBubbleOutline
        !n.tripId.isNullOrBlank() -> Icons.Default.DirectionsCar
        else -> Icons.Default.NotificationsActive
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dotColor = if (!n.isRead) cs.primary else cs.outline
            Surface(
                color = dotColor,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(10.dp)
            ) {}

            Spacer(Modifier.width(12.dp))

            Surface(
                color = cs.surface.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.large
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = cs.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = n.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = titleWeight,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }

                n.body?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationDetailsSheet(
    n: NotificationApiModel,
    onDismiss: () -> Unit,
    onOpenTrip: () -> Unit,
    onOpenThread: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val timeText = remember(n.createdAt) { relativeTimeUz(n.createdAt) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = n.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )

            n.body?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(12.dp))
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(16.dp))

            if (!n.threadId.isNullOrBlank()) {
                Button(
                    onClick = onOpenThread,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("Chatni ochish") }
                Spacer(Modifier.height(10.dp))
            }

            if (!n.tripId.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onOpenTrip,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("Tripni ko‘rish") }
                Spacer(Modifier.height(10.dp))
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) { Text("Yopish") }
        }
    }
}

private fun relativeTimeUz(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return "Hozir"

    val instant = runCatching { Instant.parse(createdAt) }
        .getOrElse {
            runCatching { OffsetDateTime.parse(createdAt).toInstant() }.getOrNull()
        } ?: return "Hozir"

    val now = Instant.now()
    val diff = Duration.between(instant, now)

    val minutes = diff.toMinutes()
    val hours = diff.toHours()
    val days = diff.toDays()

    return when {
        minutes < 1 -> "Hozir"
        minutes < 60 -> "${minutes} daqiqa oldin"
        hours < 24 -> "${hours} soat oldin"
        days == 1L -> "Kecha"
        days < 7 -> "${days} kun oldin"
        else -> instant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }
}
