package com.example.yol_yolakay.feature.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.data.repository.TripRepository
import com.example.yol_yolakay.feature.publish.model.PublishTripRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class PublishViewModel : ViewModel() {

    private val repository = TripRepository()

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPopularPoints()
    }

    private fun updateDraft(transform: (TripDraft) -> TripDraft) {
        _uiState.update { it.copy(draft = transform(it.draft)) }
    }

    private fun loadPopularPoints() {
        viewModelScope.launch {
            repository.getPopularPoints()
                .onSuccess { points -> _uiState.update { it.copy(popularPoints = points) } }
                .onFailure { /* xohlasangiz error state qo‘shamiz */ }
        }
    }

    private fun loadPriceSuggestion() {
        val draft = _uiState.value.draft
        val f = draft.fromLocation ?: return
        val t = draft.toLocation ?: return

        _uiState.update { it.copy(priceSuggestion = it.priceSuggestion.copy(isLoading = true)) }

        viewModelScope.launch {
            repository.getPricePreview(f.lat, f.lng, t.lat, t.lng)
                .onSuccess { res ->
                    _uiState.update { state ->
                        val newPrice = if (state.draft.price.isBlank())
                            res.recommended.toInt().toString()
                        else state.draft.price

                        state.copy(
                            draft = state.draft.copy(price = newPrice),
                            priceSuggestion = PriceSuggestionState(
                                recommended = res.recommended,
                                min = res.min,
                                max = res.max,
                                distanceKm = res.distanceKm,
                                isLoading = false
                            )
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(priceSuggestion = it.priceSuggestion.copy(isLoading = false)) }
                }
        }
    }

    fun onNext() {
        val steps = PublishStep.values()
        val cur = _uiState.value.currentStep.ordinal
        if (cur < steps.lastIndex) {
            val next = steps[cur + 1]
            _uiState.update { it.copy(currentStep = next, publishError = null) }
            if (next == PublishStep.PRICE) loadPriceSuggestion()
        } else {
            onPublish()
        }
    }

    fun onBack() {
        val ord = _uiState.value.currentStep.ordinal
        if (ord > 0) _uiState.update { it.copy(currentStep = PublishStep.values()[ord - 1], publishError = null) }
    }

    fun goToStep(step: PublishStep) {
        _uiState.update { it.copy(currentStep = step, publishError = null) }
        if (step == PublishStep.PRICE) loadPriceSuggestion()
    }

    fun onFromSelected(loc: LocationModel) {
        updateDraft { it.copy(fromLocation = loc) }
        onNext()
    }

    fun onToSelected(loc: LocationModel) {
        updateDraft { it.copy(toLocation = loc) }
        onNext()
    }

    fun onDateChange(v: java.time.LocalDate) = updateDraft { it.copy(date = v) }
    fun onTimeChange(v: java.time.LocalTime) = updateDraft { it.copy(time = v) }
    fun onPassengersChange(v: Int) = updateDraft { it.copy(passengers = v.coerceIn(1, 4)) }

    fun onPriceChange(v: String) = updateDraft { it.copy(price = v.filter(Char::isDigit)) }

    fun adjustPrice(amount: Int) {
        updateDraft { d ->
            val cur = d.price.toIntOrNull() ?: 0
            val next = (cur + amount).coerceAtLeast(5000)
            d.copy(price = next.toString())
        }
    }

    private fun onPublish() {
        val state = _uiState.value

        // ✅ Global validation (PREVIEW’da ham vaqt o‘tib ketishi mumkin)
        state.publishValidationMessage?.let { msg ->
            _uiState.update { it.copy(publishError = msg, isPublishing = false) }
            return
        }

        val draft = state.draft
        val from = draft.fromLocation ?: return
        val to = draft.toLocation ?: return

        val price = draft.price.toDoubleOrNull()
        if (price == null || price <= 0) {
            _uiState.update { it.copy(publishError = "Narxni kiriting", isPublishing = false) }
            return
        }

        _uiState.update { it.copy(isPublishing = true, publishError = null) }

        viewModelScope.launch {
            val req = PublishTripRequest(
                fromLocation = from.name,
                fromLat = from.lat,
                fromLng = from.lng,
                fromPointId = from.pointId,
                toLocation = to.name,
                toLat = to.lat,
                toLng = to.lng,
                toPointId = to.pointId,
                date = draft.date.format(DateTimeFormatter.ISO_DATE),
                time = draft.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                price = price,
                seats = draft.passengers
            )

            repository.publishTrip(req)
                .onSuccess { _uiState.update { it.copy(isPublishing = false, isPublished = true) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isPublishing = false, publishError = e.message ?: "Xatolik") }
                }
        }
    }

    fun resetAfterPublish() {
        _uiState.value = PublishUiState()
        loadPopularPoints()
    }
}
