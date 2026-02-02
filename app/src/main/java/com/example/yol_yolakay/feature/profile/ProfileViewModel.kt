package com.example.yol_yolakay.feature.profile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.session.CurrentUser
import kotlinx.coroutines.launch

class ProfileViewModel(private val repo: ProfileRemoteRepository) : ViewModel() {

    var state by mutableStateOf(ProfileState())
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching {
                val me = repo.getMe()
                val vehicle = repo.getVehicle()
                me to vehicle
            }.onSuccess { (me, vehicle) ->
                state = state.copy(isLoading = false, profile = me, vehicle = vehicle)
            }.onFailure { e ->
                state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
            }
        }
    }


    fun showSoon(featureName: String) {
        state = state.copy(message = "$featureName: tez orada 🙂")
    }



    fun consumeMessage() { state = state.copy(message = null) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val userId = CurrentUser.id(context)
                    return ProfileViewModel(ProfileRemoteRepository(userId)) as T
                }
            }
    }
}
