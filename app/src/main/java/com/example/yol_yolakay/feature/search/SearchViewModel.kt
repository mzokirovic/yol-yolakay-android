package com.example.yol_yolakay.feature.search

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class SearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    // Qayerdan o'zgarganda
    fun onFromLocationChange(newValue: String) {
        _uiState.update { it.copy(fromLocation = newValue) }
    }

    // Qayerga o'zgarganda
    fun onToLocationChange(newValue: String) {
        _uiState.update { it.copy(toLocation = newValue) }
    }

    // Swap (Almashtirish)
    fun onSwapLocations() {
        _uiState.update {
            it.copy(
                fromLocation = it.toLocation,
                toLocation = it.fromLocation
            )
        }
    }

    // Sana o'zgarganda
    fun onDateChange(newDate: LocalDate) {
        _uiState.update { it.copy(date = newDate) }
    }

    // Yo'lovchi soni o'zgarganda (1 dan 8 gacha chegara)
    fun onPassengersChange(count: Int) {
        val safeCount = count.coerceIn(1, 8)
        _uiState.update { it.copy(passengers = safeCount) }
    }
}