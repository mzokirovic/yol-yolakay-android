package com.example.yol_yolakay.feature.inbox

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.feature.notifications.NotificationsScreen
import com.example.yol_yolakay.feature.notifications.NotificationsViewModel

private enum class InboxTab { CHATS, NOTIFS }

@Composable
fun InboxHubScreen(
    onOpenThread: (String) -> Unit,
    onOpenTrip: (String) -> Unit,
    notifVm: NotificationsViewModel,
    openUpdatesSignal: Int = 0
) {
    var tab by rememberSaveable { mutableStateOf(InboxTab.CHATS) }

    // ✅ Notification orqali kelganda “Bildirishnomalar”ni ochamiz
    LaunchedEffect(openUpdatesSignal) {
        if (openUpdatesSignal > 0) {
            tab = InboxTab.NOTIFS
            notifVm.refresh()
        }
    }

    // ✅ Tab NOTIFS bo‘lganda unread hisobini olamiz
    val unread = notifVm.state.unreadCount

    Column(Modifier.fillMaxSize()) {

        // Header (refresh tugmasiz)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 10.dp)
        ) {
            Text(
                text = "Inbox",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (tab == InboxTab.CHATS) "Chatlaringiz" else "Bildirishnomalar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ✅ Uber-style segmented tabs
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            SegmentedButton(
                selected = tab == InboxTab.CHATS,
                onClick = { tab = InboxTab.CHATS },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Chatlar") }

            SegmentedButton(
                selected = tab == InboxTab.NOTIFS,
                onClick = {
                    tab = InboxTab.NOTIFS
                    // ✅ tabga kirganda refresh (tugmasiz)
                    notifVm.refresh()
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                if (unread > 0) {
                    BadgedBox(badge = { Badge { Text(unread.toString()) } }) {
                        Text("Bildirishnomalar")
                    }
                } else {
                    Text("Bildirishnomalar")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Content
        when (tab) {
            InboxTab.CHATS -> InboxScreen(onOpenThread = onOpenThread)

            InboxTab.NOTIFS -> NotificationsScreen(
                onOpenTrip = onOpenTrip,
                onOpenThread = onOpenThread,
                vm = notifVm
            )
        }
    }
}
