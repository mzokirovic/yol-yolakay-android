package com.example.yol_yolakay.feature.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.NotificationApiModel
import kotlinx.coroutines.launch

data class NotificationsState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
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

    /**
     * Screen ochilganda / login state o'zgarganda chaqiriladi.
     * Login bo'lmasa - network yo'q.
     */
    fun onLoginState(isLoggedIn: Boolean) {
        val prev = state.isLoggedIn
        state = state.copy(isLoggedIn = isLoggedIn, error = null)

        // login bo'lganda bir marta auto-refresh
        if (isLoggedIn && !prev && state.items.isEmpty()) {
            refresh()
        }

        // logout bo'lsa UI tozalanadi
        if (!isLoggedIn && prev) {
            state = NotificationsState(isLoggedIn = false, isLoading = false)
        }
    }

    fun refresh() {
        if (!state.isLoggedIn) {
            // login yo'q -> UI xatosiz, faqat info holat
            state = state.copy(isLoading = false, error = null, items = emptyList())
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching { repo.list() }
                .onSuccess { list ->
                    state = state.copy(isLoading = false, items = list, error = null)
                }
                .onFailure { e ->
                    // 401 bo'lsa login state bilan bog'liq bo'lishi mumkin
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    fun markRead(id: String) {
        if (!state.isLoggedIn) return
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
        if (!state.isLoggedIn) return
        viewModelScope.launch {
            runCatching { repo.markAllRead() }
                .onSuccess {
                    state = state.copy(items = state.items.map { it.copy(isRead = true) })
                }
        }
    }
}
