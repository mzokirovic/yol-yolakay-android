// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/trips/MyTripsViewModel.kt

package com.example.yol_yolakay.feature.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.TripApiModel
import com.example.yol_yolakay.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyTripsViewModel : ViewModel() {
    private val repo = TripRepository()

    private val _uiState = MutableStateFlow(MyTripsUiState())
    val uiState = _uiState.asStateFlow()

    // 🚨 O'ZGARISH: Argument yo'q. Faqat real ma'lumot.
    fun loadMyTrips() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Repodan argument olib tashlandi. Repository o'zi headerga ID qo'shadi.
            val result: Result<List<TripApiModel>> = repo.getMyTrips()

            result.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, trips = list) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}