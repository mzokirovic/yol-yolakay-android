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

    fun loadMyTrips(driverName: String = "Test Haydovchi") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result: Result<List<TripApiModel>> = repo.getMyTrips(driverName)

            result.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, trips = list) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
