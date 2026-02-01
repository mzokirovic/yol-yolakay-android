package com.example.yol_yolakay.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    // UI Funksiyalari
    fun onFromLocationChange(v: String) = _uiState.update { it.copy(fromLocation = v) }
    fun onToLocationChange(v: String) = _uiState.update { it.copy(toLocation = v) }
    fun onDateChange(v: LocalDate) = _uiState.update { it.copy(date = v) }
    fun onPassengersChange(v: Int) = _uiState.update { it.copy(passengers = v.coerceIn(1, 8)) }
    fun onSwapLocations() = _uiState.update { it.copy(fromLocation = it.toLocation, toLocation = it.fromLocation) }

    // Network Funksiyasi
    fun searchTrips(from: String, to: String, date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.searchTrips(from, to, date)
            result.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, trips = list) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}