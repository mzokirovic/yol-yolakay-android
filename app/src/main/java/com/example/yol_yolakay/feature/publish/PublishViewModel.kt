package com.example.yol_yolakay.feature.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.data.repository.TripRepository
import com.example.yol_yolakay.feature.publish.components.PolylineDecoder
import com.example.yol_yolakay.feature.publish.model.PublishTripRequest
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

class PublishViewModel : ViewModel() {

    private val repository = TripRepository()

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState = _uiState.asStateFlow()

    private var lastRouteKey: String? = null

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
                .onFailure { /* optional */ }
        }
    }

    private fun routeKey(f: LocationModel, t: LocationModel): String =
        String.format(Locale.US, "%.5f,%.5f|%.5f,%.5f", f.lat, f.lng, t.lat, t.lng)

    private fun loadRoutePreviewIfPossible() {
        val draft = _uiState.value.draft
        val f = draft.fromLocation ?: return
        val t = draft.toLocation ?: return

        val key = routeKey(f, t)
        if (key == lastRouteKey && _uiState.value.routePreview.hasRoute) return
        lastRouteKey = key

        _uiState.update {
            it.copy(routePreview = it.routePreview.copy(isLoading = true, error = null))
        }

        viewModelScope.launch {
            repository.getRoutePreview(f.lat, f.lng, t.lat, t.lng)
                .onSuccess { res ->
                    val decoded = res.polyline?.let { PolylineDecoder.decode(it) }.orEmpty()

                    val points = if (decoded.size >= 2) decoded else listOf(
                        LatLng(f.lat, f.lng),
                        LatLng(t.lat, t.lng)
                    )

                    _uiState.update { state ->
                        state.copy(
                            routePreview = RoutePreviewState(
                                points = points,
                                provider = res.provider,
                                distanceKm = res.distanceKm,
                                durationMin = res.durationMin,
                                isLoading = false,
                                error = res.error
                            )
                        )
                    }
                }
                .onFailure { e ->
                    // fallback: straight line
                    val points = listOf(LatLng(f.lat, f.lng), LatLng(t.lat, t.lng))
                    _uiState.update { state ->
                        state.copy(
                            routePreview = state.routePreview.copy(
                                points = points,
                                provider = "line",
                                isLoading = false,
                                error = e.message
                            )
                        )
                    }
                }
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
            if (next == PublishStep.PREVIEW) loadRoutePreviewIfPossible()
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
        if (step == PublishStep.PREVIEW) loadRoutePreviewIfPossible()
    }

    fun onFromSelected(loc: LocationModel) {
        updateDraft { it.copy(fromLocation = loc) }
        loadRoutePreviewIfPossible()
        onNext()
    }

    fun onToSelected(loc: LocationModel) {
        updateDraft { it.copy(toLocation = loc) }
        loadRoutePreviewIfPossible()
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
                fromRegion = from.region.takeIf { it.isNotBlank() },

                toLocation = to.name,
                toLat = to.lat,
                toLng = to.lng,
                toPointId = to.pointId,
                toRegion = to.region.takeIf { it.isNotBlank() },

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
        lastRouteKey = null
        _uiState.value = PublishUiState()
        loadPopularPoints()
    }
}