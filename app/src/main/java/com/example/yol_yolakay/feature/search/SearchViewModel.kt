package com.example.yol_yolakay.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.TripApiModel
import com.example.yol_yolakay.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class SearchViewModel : ViewModel() {

    private val repository = TripRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    // UI
    fun onFromLocationChange(v: String) = _uiState.update { it.copy(fromLocation = v) }
    fun onToLocationChange(v: String) = _uiState.update { it.copy(toLocation = v) }
    fun onDateChange(v: LocalDate) = _uiState.update { it.copy(date = v) }

    // ✅ DB constraint 1..4
    fun onPassengersChange(v: Int) = _uiState.update { it.copy(passengers = v.coerceIn(1, 4)) }

    fun onSwapLocations() = _uiState.update { it.copy(fromLocation = it.toLocation, toLocation = it.fromLocation) }

    // Network
    fun searchTrips(from: String, to: String, date: String, passengers: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result: Result<List<TripApiModel>> =
                repository.searchTrips(from, to, date, passengers)

            result
                .onSuccess { list: List<TripApiModel> ->
                    _uiState.update { it.copy(isLoading = false, trips = list) }
                }
                .onFailure { e: Throwable ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
