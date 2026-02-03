package com.example.yol_yolakay.feature.notifications

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.session.CurrentUser
import com.example.yol_yolakay.core.network.model.NotificationApiModel
import kotlinx.coroutines.launch

data class NotificationsState(
    val isLoading: Boolean = true,
    val items: List<NotificationApiModel> = emptyList(),
    val error: String? = null
) {
    val unreadCount: Int get() = items.count { !it.isRead }
}

class NotificationsViewModel(
    private val repo: NotificationsRemoteRepository
) : ViewModel() {

    var state by mutableStateOf(NotificationsState())
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching { repo.list() }
                .onSuccess { list -> state = state.copy(isLoading = false, items = list) }
                .onFailure { e -> state = state.copy(isLoading = false, error = e.message ?: "Xatolik") }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            runCatching { repo.markRead(id) }
                .onSuccess { updated ->
                    state = state.copy(
                        items = state.items.map {
                            if (it.id == id) (updated ?: it.copy(isRead = true)) else it
                        }
                    )
                }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { repo.markAllRead() }
                .onSuccess {
                    state = state.copy(items = state.items.map { it.copy(isRead = true) })
                }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val uid = CurrentUser.id(context)
                    return NotificationsViewModel(NotificationsRemoteRepository(uid)) as T
                }
            }
    }
}
