// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/tripdetails/TripDetailsViewModel.kt

package com.example.yol_yolakay.feature.tripdetails

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TripDetailsViewModel(
    private val repository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailsUiState())
    val uiState = _uiState.asStateFlow()

    // Ma'lumotlarni yuklash
    fun load(tripId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getTripDetails(tripId)
                .onSuccess { res ->
                    _uiState.update { it.copy(isLoading = false, trip = res.trip, seats = res.seats) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    // UI eventlari
    fun onSeatClick(seatNo: Int) {
        _uiState.update { it.copy(selectedSeatNo = seatNo) }
    }

    fun closeSeatSheet() {
        _uiState.update { it.copy(selectedSeatNo = null) }
    }

    // --- SEAT ACTIONS (User ID argumenti kerak emas, Repo o'zi hal qiladi) ---

    fun requestSeat(tripId: String, seatNo: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true) }
            // holderName null ketsa, backend o'zi profildan oladi
            repository.requestSeat(tripId, seatNo, holderName = null)
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(isBooking = false, trip = res.trip, seats = res.seats, selectedSeatNo = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    fun cancelRequest(tripId: String, seatNo: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true) }
            repository.cancelSeatRequest(tripId, seatNo)
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(isBooking = false, trip = res.trip, seats = res.seats, selectedSeatNo = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    fun approveSeat(tripId: String, seatNo: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true) }
            repository.approveSeat(tripId, seatNo)
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(isBooking = false, trip = res.trip, seats = res.seats, selectedSeatNo = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    fun rejectSeat(tripId: String, seatNo: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true) }
            repository.rejectSeat(tripId, seatNo)
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(isBooking = false, trip = res.trip, seats = res.seats, selectedSeatNo = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    fun blockSeat(tripId: String, seatNo: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true) }
            repository.blockSeat(tripId, seatNo)
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(isBooking = false, trip = res.trip, seats = res.seats, selectedSeatNo = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    fun unblockSeat(tripId: String, seatNo: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true) }
            repository.unblockSeat(tripId, seatNo)
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(isBooking = false, trip = res.trip, seats = res.seats, selectedSeatNo = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Repository argumentsiz yaratiladi (chunki u ichida Singleton Clientni ishlatadi)
                return TripDetailsViewModel(TripRepository()) as T
            }
        }
    }
}