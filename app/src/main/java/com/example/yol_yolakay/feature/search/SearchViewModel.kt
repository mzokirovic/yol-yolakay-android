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
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

class SearchViewModel : ViewModel() {

    private val repository = TripRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private fun Throwable.toUserMessage(): String = when (this) {
        is UnknownHostException -> "Internet bilan aloqa yo‘q"
        is SocketTimeoutException -> "Server javob bermadi. Keyinroq urinib ko‘ring"
        is IOException -> "Tarmoq xatosi. Ulanishni tekshiring"
        else -> this.message?.takeIf { it.isNotBlank() } ?: "Noma’lum xatolik"
    }

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
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
                }
        }
    }
}
