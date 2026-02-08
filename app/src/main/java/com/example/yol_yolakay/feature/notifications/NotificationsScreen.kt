package com.example.yol_yolakay.feature.notifications

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.network.model.NotificationApiModel
import com.example.yol_yolakay.core.session.SessionStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    val store = remember { SessionStore(ctx) }

    val isLoggedIn by store.isLoggedIn
        .map { it }
        .distinctUntilChanged()
        .collectAsState(initial = false)

    LaunchedEffect(isLoggedIn) {
        vm.onLoginState(isLoggedIn)
    }

    val state = vm.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Yangiliklar", fontWeight = FontWeight.SemiBold)
                        if (state.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Badge { Text(state.unreadCount.toString()) }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash")
                    }
                    IconButton(
                        onClick = { vm.markAllRead() },
                        enabled = state.isLoggedIn && state.items.isNotEmpty()
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Hammasini o‘qilgan qilish")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !state.isLoggedIn -> LoggedOutState()

                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.error != null -> ErrorState(
                    message = state.error ?: "Xatolik",
                    onRetry = { vm.refresh() }
                )

                state.items.isEmpty() -> EmptyState(onRefresh = { vm.refresh() })

                else -> NotificationsList(
                    items = state.items,
                    onClick = { n ->
                        vm.markRead(n.id)
                        when {
                            !n.threadId.isNullOrBlank() -> onOpenThread(n.threadId)
                            !n.tripId.isNullOrBlank() -> onOpenTrip(n.tripId)
                        }
                    }
                )
            }
        }
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
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text("Qayta urinish")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
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
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                    Text("Yangilash")
                }
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
        items.sortedWith(compareBy<NotificationApiModel> { it.isRead }.thenByDescending { it.id })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sorted, key = { it.id }) { n ->
            NotificationCard(n = n, onClick = { onClick(n) })
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun NotificationCard(
    n: NotificationApiModel,
    onClick: () -> Unit
) {
    val container = if (!n.isRead) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

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
            val dotColor = if (!n.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Surface(
                color = dotColor,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(10.dp)
            ) {}
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = n.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!n.isRead) FontWeight.SemiBold else FontWeight.Medium
                )
                n.body?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
