package com.example.yol_yolakay.feature.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.notifications.NotificationsScreen
import com.example.yol_yolakay.feature.notifications.NotificationsViewModel
import com.example.yol_yolakay.feature.notifications.NotificationsVmFactory

private enum class InboxTab { CHATS, UPDATES }

@Composable
fun InboxHubScreen(
    onOpenThread: (String) -> Unit,
    onOpenTrip: (String) -> Unit,
    notifVm: NotificationsViewModel
) {
    var tab by rememberSaveable { mutableStateOf(InboxTab.CHATS) }

    val ctx = LocalContext.current

    // ✅ Bitta VM — badge ham, list ham shu VM’dan
    val notifVm: NotificationsViewModel = viewModel(factory = NotificationsVmFactory(ctx))

    val unread = notifVm.state.items.count { !it.isRead }

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
                    notifVm.refresh() // ✅ har kirganda yangilab olamiz
                },
                text = {
                    if (unread > 0) {
                        BadgedBox(badge = { Badge { Text(unread.toString()) } }) {
                            Text("Updates")
                        }
                    } else {
                        Text("Updates")
                    }
                }
            )
        }

        when (tab) {
            InboxTab.CHATS -> InboxScreen(onOpenThread = onOpenThread)

            InboxTab.UPDATES -> NotificationsScreen(
                onOpenTrip = onOpenTrip,
                onOpenThread = onOpenThread,
                vm = notifVm // ✅ shu yerda pass qilamiz
            )
        }
    }
}
