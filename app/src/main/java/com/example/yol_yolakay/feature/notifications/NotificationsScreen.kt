package com.example.yol_yolakay.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
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
import com.example.yol_yolakay.core.session.CurrentUser
import com.example.yol_yolakay.core.network.model.NotificationApiModel

class NotificationsVmFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val uid = CurrentUser.id(context)
        val repo = NotificationsRemoteRepository(uid)
        return NotificationsViewModel(repo) as T
    }
}

@Composable
fun NotificationsScreen(
    onOpenTrip: (String) -> Unit,
    onOpenThread: (String) -> Unit,
    vm: NotificationsViewModel = viewModel(factory = NotificationsVmFactory(LocalContext.current))
) {
    val state = vm.state


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Yangiliklar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.markAllRead() }) {
                Icon(Icons.Default.DoneAll, contentDescription = "Hammasini o‘qilgan qilish")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hozircha bildirishnomalar yo‘q")
            }
            return
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                state.items.forEachIndexed { index, n ->
                    NotificationRow(
                        n = n,
                        onClick = {
                            vm.markRead(n.id)
                            when {
                                !n.threadId.isNullOrBlank() -> onOpenThread(n.threadId)
                                !n.tripId.isNullOrBlank() -> onOpenTrip(n.tripId)
                            }
                        }
                    )
                    if (index != state.items.lastIndex) {
                        Divider(modifier = Modifier.padding(start = 14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    n: NotificationApiModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unread dot
        val dotColor = if (!n.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = dotColor,
            modifier = Modifier.size(10.dp)
        ) {}

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                n.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (!n.isRead) FontWeight.SemiBold else FontWeight.Medium
            )
            n.body?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
