// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/profile/ProfileViewModel.kt

package com.example.yol_yolakay.feature.profile.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.feature.profile.data.ProfileRemoteRepository
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
                // Agar 401 (Unauthorized) bo'lsa, foydalanuvchini loginga otish kerak bo'lishi mumkin.
                // Lekin hozircha xato ko'rsatamiz.
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
                    // 🚨 O'ZGARISH: Argument yo'q. Toza kod.
                    return ProfileViewModel(ProfileRemoteRepository()) as T
                }
            }
    }
}