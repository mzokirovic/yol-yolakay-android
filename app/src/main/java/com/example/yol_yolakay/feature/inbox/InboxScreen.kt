package com.example.yol_yolakay.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.ThreadApiModel
import com.example.yol_yolakay.ui.components.AppCircularLoading
import com.example.yol_yolakay.ui.components.AppPullRefreshIndicator
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
                .onSuccess { list ->
                    state = state.copy(isLoading = false, threads = list, error = null)
                }
                .onFailure { e ->
                    // data bo'lsa ham list ushlab qolamiz
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
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
    val cs = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }

    val hasData = s.threads.isNotEmpty()
    val isInitialLoading = s.isLoading && !hasData

    // data bor bo'lsa xatoni snackbar qilib chiqaramiz
    var lastSnackError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(s.error, hasData) {
        val err = s.error
        if (hasData && !err.isNullOrBlank() && err != lastSnackError) {
            lastSnackError = err
            snackbarHostState.showSnackbar(err)
        }
    }

    val pullState = rememberPullRefreshState(
        refreshing = s.isLoading && hasData, // ✅ faqat list bo'lganda pull refresh
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
            Text(
                text = "Chatlar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp),
                color = cs.onSurface
            )

            Spacer(Modifier.height(12.dp))

            when {
                // ✅ Initial loading: faqat 1 ta loader
                isInitialLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AppCircularLoading()
                    }
                }

                // ✅ Data yo'q + error: full error
                !hasData && s.error != null -> {
                    ErrorState(message = s.error!!, onRetry = { vm.refresh() })
                }

                // ✅ Empty
                !s.isLoading && !hasData -> {
                    EmptyState(
                        title = "Hali chatlar yo‘q",
                        subtitle = "Safar e’lonlaridan chat boshlang — hammasi shu yerda ko‘rinadi."
                    )
                }

                // ✅ Data bor: list doim ko‘rinadi
                else -> {
                    ThreadsList(
                        threads = s.threads,
                        onOpenThread = onOpenThread
                    )
                }
            }
        }

        // ✅ Pull indicator: faqat data bor paytda ko'rsatamiz (double loader yo'q)
        if (hasData) {
            AppPullRefreshIndicator(
                refreshing = s.isLoading,
                state = pullState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                contentColor = cs.onSurface,
                backgroundColor = cs.surface
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
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
        LazyColumn(contentPadding = PaddingValues(vertical = 6.dp)) {
            items(threads, key = { it.id }) { t ->
                ThreadRowModern(
                    title = t.otherUserName ?: (t.otherUserId ?: "Foydalanuvchi"),
                    subtitle = t.lastMessage ?: "Xabar yo‘q",
                    onClick = { onOpenThread(t.id) }
                )
                HorizontalDivider(
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

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(cs.onSurface.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = cs.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = cs.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
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