package com.example.yol_yolakay.feature.publish

import java.time.LocalDate
import java.time.LocalTime

enum class PublishStep {
    FROM, TO, DATE, TIME, PASSENGERS, PRICE, PREVIEW
}

// ✅ 1. YANGI: Faqat user kiritgan ma'lumotlar (Toza Data)
data class TripDraft(
    val fromLocation: LocationModel? = null,
    val toLocation: LocationModel? = null,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val passengers: Int = 1,
    val price: String = ""
)

// ✅ 2. YANGI: Faqat Narx bo'yicha serverdan kelgan maslahatlar
data class PriceSuggestionState(
    val recommended: Double = 0.0,
    val min: Double = 0.0,
    val max: Double = 0.0,
    val distanceKm: Int = 0,
    val isLoading: Boolean = false
)

// ✅ 3. ASOSIY STATE: UI ni boshqarish uchun
data class PublishUiState(
    val currentStep: PublishStep = PublishStep.FROM,

    // Barcha ma'lumotlar shu "draft" qutichasi ichida
    val draft: TripDraft = TripDraft(),

    // Narx maslahatlari alohida qutichada
    val priceSuggestion: PriceSuggestionState = PriceSuggestionState(),

    // Tizim holatlari
    val popularPoints: List<LocationModel> = emptyList(),
    val isPublishing: Boolean = false,
    val publishError: String? = null,
    val isPublished: Boolean = false
) {
    // UI Logic: Tugmani yoqish/o'chirish
    val isNextEnabled: Boolean
        get() = when (currentStep) {
            PublishStep.FROM -> draft.fromLocation != null
            PublishStep.TO -> draft.toLocation != null
            PublishStep.DATE -> true
            PublishStep.TIME -> true
            PublishStep.PASSENGERS -> draft.passengers in 1..4
            PublishStep.PRICE -> draft.price.isNotBlank()
            PublishStep.PREVIEW -> true
        }

    val progress: Float
        get() = (currentStep.ordinal + 1) / PublishStep.values().size.toFloat()
}