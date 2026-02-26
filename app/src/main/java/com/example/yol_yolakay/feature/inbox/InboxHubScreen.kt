package com.example.yol_yolakay.feature.inbox

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val cs = MaterialTheme.colorScheme

    // ✅ MUHIM: Inbox VM’ni HUB darajasida yaratamiz — tab switchda qayta yaratilmaydi
    val inboxVm: InboxViewModel = viewModel(factory = InboxViewModel.factory())

    LaunchedEffect(openUpdatesSignal) {
        if (openUpdatesSignal > 0) {
            tab = InboxTab.NOTIFS
            notifVm.refresh()
        }
    }

    val unread = notifVm.state.unreadCount

    Column(Modifier.fillMaxSize().background(cs.background)) {
        Surface(
            color = cs.background,
            modifier = Modifier.statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Inbox",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(16.dp))

                SegmentedSelector(
                    selectedIndex = if (tab == InboxTab.CHATS) 0 else 1,
                    items = listOf("Chatlar", "Bildirishnomalar"),
                    badgeCount = if (tab == InboxTab.CHATS) 0 else unread,
                    onSelectionChange = {
                        if (it == 0) {
                            tab = InboxTab.CHATS
                            // ✅ ixtiyoriy: chatsga qaytganda refresh qilmoqchi bo‘lsang:
                            // inboxVm.refresh()
                        } else {
                            tab = InboxTab.NOTIFS
                            notifVm.refresh()
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (tab) {
                InboxTab.CHATS -> InboxScreen(
                    onOpenThread = onOpenThread,
                    vm = inboxVm // ✅ endi VM qayta yaratilmaydi
                )

                InboxTab.NOTIFS -> NotificationsScreen(
                    onOpenTrip = onOpenTrip,
                    onOpenThread = onOpenThread,
                    vm = notifVm
                )
            }
        }
    }
}

@Composable
private fun SegmentedSelector(
    selectedIndex: Int,
    items: List<String>,
    badgeCount: Int,
    onSelectionChange: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cs.onSurface.copy(alpha = 0.05f))
            .padding(4.dp)
    ) {
        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedIndex == 0) 0.dp else 136.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "selector"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(cs.surface)
                .zIndex(1f)
        )

        Row(modifier = Modifier.fillMaxSize().zIndex(2f)) {
            items.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val textColor by animateColorAsState(
                    if (isSelected) cs.onSurface else cs.onSurfaceVariant,
                    label = "color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectionChange(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1
                        )

                        if (index == 1 && badgeCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = cs.error,
                                shape = CircleShape,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = cs.onError,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}