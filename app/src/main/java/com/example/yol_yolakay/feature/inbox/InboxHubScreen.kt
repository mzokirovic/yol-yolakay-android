package com.example.yol_yolakay.feature.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.yol_yolakay.feature.notifications.NotificationsScreen
import com.example.yol_yolakay.feature.notifications.NotificationsViewModel

private enum class InboxTab { CHATS, UPDATES }

@Composable
fun InboxHubScreen(
    onOpenThread: (String) -> Unit,
    onOpenTrip: (String) -> Unit,
    notifVm: NotificationsViewModel,
    openUpdatesSignal: Int = 0
) {
    var tab by rememberSaveable { mutableStateOf(InboxTab.CHATS) }

    // ✅ Notification orqali kelganda Updates tab’ni ochamiz
    LaunchedEffect(openUpdatesSignal) {
        if (openUpdatesSignal > 0) {
            tab = InboxTab.UPDATES
            notifVm.refresh()
        }
    }

    val unread = notifVm.state.unreadCount

    Column {
        TabRow(selectedTabIndex = tab.ordinal) {
            Tab(
                selected = tab == InboxTab.CHATS,
                onClick = { tab = InboxTab.CHATS },
                text = { Text("Chats") }
            )
            Tab(
                selected = tab == InboxTab.UPDATES,
                onClick = {
                    tab = InboxTab.UPDATES
                    notifVm.refresh()
                },
                text = {
                    if (unread > 0) {
                        BadgedBox(badge = { Badge { Text(unread.toString()) } }) { Text("Updates") }
                    } else Text("Updates")
                }
            )
        }

        when (tab) {
            InboxTab.CHATS -> InboxScreen(onOpenThread = onOpenThread)
            InboxTab.UPDATES -> NotificationsScreen(
                onOpenTrip = onOpenTrip,
                onOpenThread = onOpenThread,
                vm = notifVm
            )
        }
    }
}
