package com.example.yol_yolakay.feature.inbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import com.example.yol_yolakay.core.network.model.MessageApiModel
import com.example.yol_yolakay.core.session.CurrentUser
import kotlinx.coroutines.launch

data class ThreadState(
    val isLoading: Boolean = true,
    val messages: List<MessageApiModel> = emptyList(),
    val error: String? = null,
    val input: String = ""
)

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
                    return ThreadViewModel(uid, InboxRemoteRepository(uid), threadId) as T
                }
            }
    }
}

@Composable
fun ThreadScreen(
    threadId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: ThreadViewModel = viewModel(factory = ThreadViewModel.factory(context, threadId))
    val s = vm.state

    // ✅ LocalContext.current ni remember {} ichida ishlatmaymiz
    val myId = remember(context) { CurrentUser.id(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header (TopAppBar’siz)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = "Chat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = { vm.refresh() }) { Text("Refresh") }
        }

        Spacer(Modifier.height(8.dp))

        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (s.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(s.messages) { m ->
                    val isMine = m.sender_id == myId
                    MessageBubble(text = m.text, isMine = isMine)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = s.input,
                onValueChange = vm::onInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Xabar yozing...") }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { vm.send() }) {
                Icon(Icons.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(text: String, isMine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            color = if (isMine) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
