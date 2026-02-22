package com.example.yol_yolakay.feature.inbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.MessageApiModel
import com.example.yol_yolakay.core.session.CurrentUser
import kotlinx.coroutines.launch

// --- UI State ---
data class ThreadState(
    val isLoading: Boolean = true,
    val messages: List<MessageApiModel> = emptyList(),
    val error: String? = null,
    val input: String = ""
)

// --- ViewModel ---
class ThreadViewModel(
    private val userId: String,
    private val repo: InboxRemoteRepository,
    private val threadId: String
) : ViewModel() {

    var state by mutableStateOf(ThreadState())
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching { repo.getMessages(threadId) }
                .onSuccess { msgs -> state = state.copy(isLoading = false, messages = msgs) }
                .onFailure { e -> state = state.copy(isLoading = false, error = e.message ?: "Xatolik") }
        }
    }

    fun onInput(v: String) { state = state.copy(input = v) }

    fun send() {
        val text = state.input.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            runCatching { repo.sendMessage(threadId, text) }
                .onSuccess {
                    state = state.copy(input = "")
                    refresh()
                }
                .onFailure { e -> state = state.copy(error = e.message ?: "Xatolik") }
        }
    }

    companion object {
        fun factory(context: android.content.Context, threadId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val uid = CurrentUser.id(context)
                    return ThreadViewModel(uid, InboxRemoteRepository(), threadId) as T
                }
            }
    }
}

// --- Main Screen ---
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    threadId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: ThreadViewModel = viewModel(factory = ThreadViewModel.factory(context, threadId))
    val s = vm.state
    val cs = MaterialTheme.colorScheme
    val myId = remember(context) { CurrentUser.id(context) }

    val pullState = rememberPullRefreshState(
        refreshing = s.isLoading,
        onRefresh = { vm.refresh() }
    )

    Scaffold(
        containerColor = cs.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = cs.surface
                )
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .pullRefresh(pullState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. Xabarlar ro'yxati
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(s.messages, key = { it.id ?: it.hashCode() }) { m ->
                        MessageBubble(text = m.text, isMine = m.sender_id == myId)
                    }
                }

                // 2. Premium Composer (Xabar yozish joyi)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = cs.surface,
                    border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding() // Tizim tugmalari ustida turadi
                            .imePadding(), // Klaviatura ochilganda yuqoriga ko'tariladi
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = s.input,
                            onValueChange = vm::onInput,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Xabar yozing...") },
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = cs.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = cs.surfaceVariant.copy(alpha = 0.3f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            maxLines = 4
                        )

                        Spacer(Modifier.width(12.dp))

                        // Yuborish tugmasi
                        IconButton(
                            onClick = vm::send,
                            enabled = s.input.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (s.input.isNotBlank()) cs.onSurface else cs.outlineVariant.copy(alpha = 0.5f)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Send,
                                contentDescription = "Send",
                                tint = if (s.input.isNotBlank()) cs.surface else cs.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Yangilash indikatori
            PullRefreshIndicator(
                refreshing = s.isLoading,
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// --- Xabar pufakchasi ---
@Composable
private fun MessageBubble(text: String, isMine: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            color = if (isMine) cs.onSurface else cs.surfaceVariant.copy(alpha = 0.6f),
            border = if (isMine) null else BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.4f))
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = if (isMine) cs.surface else cs.onSurface
            )
        }
    }
}