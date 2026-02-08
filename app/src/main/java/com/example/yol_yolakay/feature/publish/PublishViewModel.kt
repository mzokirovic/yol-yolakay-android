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

    // Kelajakda buni @Inject (Hilt) orqali olish kerak, lekin hozir "buzmaslik" uchun shunday qoldiramiz.
    private val repository = TripRepository()

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState = _uiState.asStateFlow()

    private var currentUserId: String = "device"

    fun setCurrentUser(id: String) {
        currentUserId = id.ifBlank { "device" }
    }

    init {
        loadPopularPoints()
    }

    // --- HELPER: Draftni xavfsiz yangilash ---
    // Bu funksiya UI holatini buzmasdan faqat datani o'zgartiradi
    private fun updateDraft(transform: (TripDraft) -> TripDraft) {
        _uiState.update { currentState ->
            currentState.copy(draft = transform(currentState.draft))
        }
    }

    private fun loadPopularPoints() {
        viewModelScope.launch {
            repository.getPopularPoints().onSuccess { points ->
                _uiState.update { it.copy(popularPoints = points) }
            }
        }
    }

    // ✅ REFACTORED: Narxni serverdan olish (Isolation)
    private fun loadPriceSuggestion() {
        val draft = _uiState.value.draft
        if (draft.fromLocation == null || draft.toLocation == null) return

        // 1. Loading yoqamiz (faqat narx qismida)
        _uiState.update { it.copy(priceSuggestion = it.priceSuggestion.copy(isLoading = true)) }

        viewModelScope.launch {
            repository.getPricePreview(
                draft.fromLocation.lat, draft.fromLocation.lng,
                draft.toLocation.lat, draft.toLocation.lng
            ).onSuccess { res ->
                _uiState.update { state ->
                    // Agar user hali narx yozmagan bo'lsa, avtomatik to'ldiramiz
                    val newPrice = if (state.draft.price.isBlank()) res.recommended.toInt().toString() else state.draft.price

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
            }.onFailure {
                // Xato bo'lsa ham loadingni o'chiramiz
                _uiState.update { it.copy(priceSuggestion = it.priceSuggestion.copy(isLoading = false)) }
            }
        }
    }

    // --- NAVIGATION LOGIC ---
    fun onNext() {
        val steps = PublishStep.values()
        val currentOrd = _uiState.value.currentStep.ordinal
        if (currentOrd < steps.lastIndex) {
            val nextStep = steps[currentOrd + 1]

            // Stepni o'zgartiramiz
            _uiState.update { it.copy(currentStep = nextStep) }

            // Side Effect: Agar Price stepiga kelsak, narxni yuklaymiz
            if (nextStep == PublishStep.PRICE) {
                loadPriceSuggestion()
            }
        } else {
            onPublish()
        }
    }

    fun onBack() {
        val ord = _uiState.value.currentStep.ordinal
        if (ord > 0) {
            _uiState.update { it.copy(currentStep = PublishStep.values()[ord - 1]) }
        }
    }

    // --- USER INPUTS (Clean & Safe) ---
    // Har bir input endi boshqa inputlarga ta'sir qilmaydi

    fun onFromSelected(loc: LocationModel) {
        updateDraft { it.copy(fromLocation = loc) }
        onNext() // Avtomatik keyingisiga o'tish
    }

    fun onToSelected(loc: LocationModel) {
        updateDraft { it.copy(toLocation = loc) }
        onNext()
    }

    fun onDateChange(v: LocalDate) = updateDraft { it.copy(date = v) }
    fun onTimeChange(v: LocalTime) = updateDraft { it.copy(time = v) }
    fun onPassengersChange(v: Int) = updateDraft { it.copy(passengers = v.coerceIn(1, 4)) }

    fun onPriceChange(v: String) = updateDraft {
        it.copy(price = v.filter { char -> char.isDigit() })
    }

    // Narxni +/- qilish
    fun adjustPrice(amount: Int) {
        updateDraft { draft ->
            val current = draft.price.toIntOrNull() ?: 0
            val newPrice = (current + amount).coerceAtLeast(5000)
            draft.copy(price = newPrice.toString())
        }
    }

    // --- PUBLISH ACTION ---
    private fun onPublish() {
        val draft = _uiState.value.draft
        if (draft.fromLocation == null || draft.toLocation == null) return

        _uiState.update { it.copy(isPublishing = true, publishError = null) }

        viewModelScope.launch {
            val req = PublishTripRequest(
                fromLocation = draft.fromLocation.name,
                fromLat = draft.fromLocation.lat,
                fromLng = draft.fromLocation.lng,
                fromPointId = draft.fromLocation.pointId,
                toLocation = draft.toLocation.name,
                toLat = draft.toLocation.lat,
                toLng = draft.toLocation.lng,
                toPointId = draft.toLocation.pointId,
                date = draft.date.format(DateTimeFormatter.ISO_DATE),
                time = draft.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                price = draft.price.toDoubleOrNull() ?: 0.0,
                seats = draft.passengers,
                driverId = currentUserId
            )
            repository.publishTrip(req)
                .onSuccess {
                    _uiState.update { it.copy(isPublishing = false, isPublished = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isPublishing = false, publishError = e.message) }
                }
        }
    }

    fun resetAfterPublish() {
        _uiState.value = PublishUiState()
        loadPopularPoints()
    }
}