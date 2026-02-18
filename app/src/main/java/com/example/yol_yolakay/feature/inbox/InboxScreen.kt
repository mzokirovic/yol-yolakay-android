package com.example.yol_yolakay.feature.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.ThreadApiModel
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
            // Pull refresh paytida ham loading true bo‘ladi
            state = state.copy(isLoading = true, error = null)
            runCatching { repo.listThreads() }
                .onSuccess { list -> state = state.copy(isLoading = false, threads = list) }
                .onFailure { e -> state = state.copy(isLoading = false, error = e.message ?: "Xatolik") }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InboxViewModel(InboxRemoteRepository()) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InboxScreen(
    onOpenThread: (String) -> Unit,
    vm: InboxViewModel = viewModel(factory = InboxViewModel.factory())
) {
    val s = vm.state

    // ✅ Pull-to-refresh state
    val pullState = rememberPullRefreshState(
        refreshing = s.isLoading,
        onRefresh = { vm.refresh() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullState)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // ✅ Header — refresh tugmasiz
            Text(
                text = "Chatlar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )

            Spacer(Modifier.height(12.dp))

            when {
                s.error != null -> {
                    ErrorState(
                        message = s.error!!,
                        onRetry = { vm.refresh() }
                    )
                }

                !s.isLoading && s.threads.isEmpty() -> {
                    EmptyState(
                        title = "Hali chatlar yo‘q",
                        subtitle = "Safar e’lonlaridan chat boshlang — hammasi shu yerda ko‘rinadi."
                    )
                }

                else -> {
                    ThreadsList(
                        threads = s.threads,
                        onOpenThread = onOpenThread
                    )
                }
            }
        }

        // ✅ Indicator (tepa markazda)
        PullRefreshIndicator(
            refreshing = s.isLoading,
            state = pullState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun ThreadsList(
    threads: List<ThreadApiModel>,
    onOpenThread: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(threads, key = { it.id }) { t ->
                ThreadRowModern(
                    title = t.otherUserName ?: (t.otherUserId ?: "Foydalanuvchi"),
                    subtitle = t.lastMessage ?: "Xabar yo‘q",
                    onClick = { onOpenThread(t.id) }
                )
                Divider(
                    thickness = 1.dp,
                    color = cs.outline.copy(alpha = 0.10f),
                    modifier = Modifier.padding(start = 72.dp)
                )
            }
        }
    }
}

@Composable
private fun ThreadRowModern(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle (text-based)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = cs.primaryContainer
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = cs.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Qayta urinish") }
            Spacer(Modifier.height(10.dp))
            Text(
                "Yuqoridan pastga tortib ham yangilashingiz mumkin.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}
