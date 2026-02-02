package com.example.yol_yolakay.feature.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.data.repository.TripRepository
import com.example.yol_yolakay.feature.publish.model.PublishTripRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PublishViewModel : ViewModel() {

    private val repository = TripRepository()

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState = _uiState.asStateFlow()

    // ✅ Seam: hozircha device id, keyin auth qo‘shilsa shu joy o‘zgaradi xolos
    private var currentUserId: String = "device"
    fun setCurrentUser(id: String) {
        currentUserId = id.ifBlank { "device" }
    }

    fun onFromChange(v: String) = _uiState.update { it.copy(fromLocation = v) }
    fun onToChange(v: String) = _uiState.update { it.copy(toLocation = v) }
    fun onDateChange(v: LocalDate) = _uiState.update { it.copy(date = v) }
    fun onTimeChange(v: LocalTime) = _uiState.update { it.copy(time = v) }

    // ✅ DB constraint: 1..4
    fun onPassengersChange(v: Int) = _uiState.update { it.copy(passengers = v.coerceIn(1, 4)) }

    fun onPriceChange(v: String) = _uiState.update { it.copy(price = v.filter { char -> char.isDigit() }) }

    fun onNext() {
        val steps = PublishStep.values()
        val currentOrd = _uiState.value.currentStep.ordinal

        if (currentOrd < steps.lastIndex) {
            _uiState.update { it.copy(currentStep = steps[currentOrd + 1]) }
        } else {
            onPublish()
        }
    }

    fun onBack() {
        val steps = PublishStep.values()
        val currentOrd = _uiState.value.currentStep.ordinal
        if (currentOrd > 0) {
            _uiState.update { it.copy(currentStep = steps[currentOrd - 1]) }
        }
    }

    private fun onPublish() {
        val s = _uiState.value

        // 1) UI: publishing boshlandi
        _uiState.update { it.copy(isPublishing = true, publishError = null, isPublished = false) }

        viewModelScope.launch {
            val req = PublishTripRequest(
                fromLocation = s.fromLocation.trim(),
                toLocation = s.toLocation.trim(),

                // TODO: keyin Map'dan olinadi
                fromLat = 41.3111,
                fromLng = 69.2401,
                toLat = 39.6542,
                toLng = 66.9597,

                date = s.date.format(DateTimeFormatter.ISO_DATE),
                time = s.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                price = s.price.toDoubleOrNull() ?: 0.0,
                seats = s.passengers,

                // ✅ MUHIM: endi driverId null emas
                driverId = currentUserId
            )

            val result = repository.publishTrip(req)

            result.onSuccess {
                _uiState.update { it.copy(isPublishing = false, isPublished = true) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isPublishing = false,
                        publishError = e.message ?: "Xatolik yuz berdi"
                    )
                }
            }
        }
    }

    fun resetAfterPublish() {
        _uiState.value = PublishUiState()
    }
}
