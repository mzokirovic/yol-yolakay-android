package com.example.yol_yolakay.feature.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.ThreadApiModel
import com.example.yol_yolakay.core.session.CurrentUser
import kotlinx.coroutines.launch

data class InboxState(
    val isLoading: Boolean = true,
    val threads: List<ThreadApiModel> = emptyList(),
    val error: String? = null
)

class InboxViewModel(private val repo: InboxRemoteRepository) : ViewModel() {
    var state by mutableStateOf(InboxState())
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching { repo.listThreads() }
                .onSuccess { list -> state = state.copy(isLoading = false, threads = list) }
                .onFailure { e -> state = state.copy(isLoading = false, error = e.message ?: "Xatolik") }
        }
    }

    companion object {
        fun factory(context: android.content.Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val uid = CurrentUser.id(context)
                    return InboxViewModel(InboxRemoteRepository(uid)) as T
                }
            }
    }
}

@Composable
fun InboxScreen(
    onOpenThread: (String) -> Unit,
    vm: InboxViewModel = viewModel(factory = InboxViewModel.factory(LocalContext.current))
) {
    val s = vm.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TopHeader(title = "Xabarlar", onRefresh = { vm.refresh() })

        Spacer(Modifier.height(12.dp))

        if (s.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // ✅ Error bo‘lsa: faqat error + retry, pastdagi empty-state chiqmaydi
        if (s.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.material3.Button(onClick = { vm.refresh() }) {
                        Text("Qayta urinish")
                    }
                }
            }
            return
        }

        if (s.threads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hali chatlar yo‘q")
            }
            return
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                s.threads.forEachIndexed { index, t ->
                    ThreadRow(
                        title = t.otherUserName ?: t.otherUserId,
                        subtitle = t.lastMessage ?: "Xabar yo‘q",
                        onClick = { onOpenThread(t.id) }
                    )
                    if (index != s.threads.lastIndex) {
                        DividerIndented()
                    }
                }
            }
        }
    }
}


@Composable
private fun TopHeader(title: String, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
        }
    }
}

@Composable
private fun ThreadRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DividerIndented() {
    androidx.compose.material3.Divider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        modifier = Modifier.padding(start = 14.dp)
    )
}
