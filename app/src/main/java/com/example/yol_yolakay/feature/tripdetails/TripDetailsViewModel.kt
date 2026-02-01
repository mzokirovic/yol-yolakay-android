package com.example.yol_yolakay.feature.tripdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TripDetailsViewModel : ViewModel() {
    private val repo = TripRepository()

    private val _uiState = MutableStateFlow(TripDetailsUiState())
    val uiState = _uiState.asStateFlow()

    fun load(tripId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repo.getTripDetails(tripId)
                .onSuccess { resp ->
                    _uiState.update { it.copy(isLoading = false, trip = resp.trip, seats = resp.seats) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun onSeatClick(seatNo: Int) {
        _uiState.update { it.copy(selectedSeatNo = seatNo) }
    }

    fun closeSeatSheet() {
        _uiState.update { it.copy(selectedSeatNo = null) }
    }

    fun bookSelectedSeat(tripId: String, seatNo: Int, clientId: String, holderName: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) }
            repo.bookSeat(tripId, seatNo, clientId, holderName)
                .onSuccess { resp ->
                    _uiState.update {
                        it.copy(
                            isBooking = false,
                            trip = resp.trip,
                            seats = resp.seats,
                            selectedSeatNo = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    // ✅ Driver actions
    fun blockSeat(tripId: String, seatNo: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) } // reuse flag (MVP)
            repo.blockSeat(tripId, seatNo)
                .onSuccess { resp ->
                    _uiState.update { it.copy(isBooking = false, trip = resp.trip, seats = resp.seats, selectedSeatNo = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    fun unblockSeat(tripId: String, seatNo: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) }
            repo.unblockSeat(tripId, seatNo)
                .onSuccess { resp ->
                    _uiState.update { it.copy(isBooking = false, trip = resp.trip, seats = resp.seats, selectedSeatNo = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }
}
