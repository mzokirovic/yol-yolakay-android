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

    // Passenger: request
    fun requestSeat(tripId: String, seatNo: Int, userId: String, holderName: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) }
            repo.requestSeat(tripId, seatNo, userId, holderName)
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

    // Passenger: cancel request
    fun cancelRequest(tripId: String, seatNo: Int, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) }
            repo.cancelSeatRequest(tripId, seatNo, userId)
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

    // Driver: approve
    fun approveSeat(tripId: String, seatNo: Int, driverId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) }
            repo.approveSeat(tripId, seatNo, driverId)
                .onSuccess { resp ->
                    _uiState.update { it.copy(isBooking = false, trip = resp.trip, seats = resp.seats, selectedSeatNo = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    // Driver: reject
    fun rejectSeat(tripId: String, seatNo: Int, driverId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) }
            repo.rejectSeat(tripId, seatNo, driverId)
                .onSuccess { resp ->
                    _uiState.update { it.copy(isBooking = false, trip = resp.trip, seats = resp.seats, selectedSeatNo = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    // Driver: block/unblock
    fun blockSeat(tripId: String, seatNo: Int, driverId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) }
            repo.blockSeat(tripId, seatNo, driverId)
                .onSuccess { resp ->
                    _uiState.update { it.copy(isBooking = false, trip = resp.trip, seats = resp.seats, selectedSeatNo = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    fun unblockSeat(tripId: String, seatNo: Int, driverId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) }
            repo.unblockSeat(tripId, seatNo, driverId)
                .onSuccess { resp ->
                    _uiState.update { it.copy(isBooking = false, trip = resp.trip, seats = resp.seats, selectedSeatNo = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }
}
