package com.example.yol_yolakay.feature.publish

import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class PublishStep { FROM, TO, DATE, TIME, PASSENGERS, PRICE, PREVIEW }

/**
 * ✅ 1) Faqat user kiritgan ma'lumotlar (toza data)
 */
data class TripDraft(
    val fromLocation: LocationModel? = null,
    val toLocation: LocationModel? = null,
    val date: LocalDate = LocalDate.now(),
    // ✅ default time: hozirgi vaqt + 15 daqiqa (5 minutlik grid)
    val time: LocalTime = defaultTime(),
    val passengers: Int = 1,
    // UI string sifatida saqlaymiz (TextField uchun)
    val price: String = ""
)

/**
 * ✅ 2) Faqat serverdan kelgan maslahatlar (price suggestion)
 */
data class PriceSuggestionState(
    val recommended: Double = 0.0,
    val min: Double = 0.0,
    val max: Double = 0.0,
    val distanceKm: Int = 0,
    val isLoading: Boolean = false
) {
    val hasRange: Boolean get() = !isLoading && max > 0.0 && min >= 0.0
    val minLong: Long get() = min.toLong()
    val maxLong: Long get() = max.toLong()
    val recommendedLong: Long get() = recommended.toLong()
}

/**
 * ✅ 3) Asosiy UI state
 */
data class PublishUiState(
    val currentStep: PublishStep = PublishStep.FROM,
    val draft: TripDraft = TripDraft(),
    val priceSuggestion: PriceSuggestionState = PriceSuggestionState(),
    val popularPoints: List<LocationModel> = emptyList(),
    val isPublishing: Boolean = false,
    val publishError: String? = null,
    val isPublished: Boolean = false
) {
    // ---------- UI helpers ----------
    val progress: Float
        get() = (currentStep.ordinal + 1) / PublishStep.values().size.toFloat()

    val primaryButtonText: String
        get() = if (currentStep == PublishStep.PREVIEW) "E'lon qilish" else "Davom etish"

    val canGoBack: Boolean
        get() = currentStep != PublishStep.FROM

    // ---------- Validation helpers ----------
    private val from = draft.fromLocation
    private val to = draft.toLocation

    private fun sameLocation(a: LocationModel?, b: LocationModel?): Boolean {
        if (a == null || b == null) return false
        val idA = a.pointId?.trim()
        val idB = b.pointId?.trim()
        return when {
            !idA.isNullOrBlank() && !idB.isNullOrBlank() -> idA == idB
            else -> a.name.trim().equals(b.name.trim(), ignoreCase = true)
        }
    }

    private val priceValue: Long?
        get() = draft.price.filter { it.isDigit() }.toLongOrNull()

    private val isDateValid: Boolean
        get() = !draft.date.isBefore(LocalDate.now())

    private val isTimeValid: Boolean
        get() = if (draft.date == LocalDate.now()) {
            !draft.time.isBefore(LocalTime.now().minusMinutes(1))
        } else true

    private val isRouteValid: Boolean
        get() = from != null && to != null && !sameLocation(from, to)

    private val isPassengersValid: Boolean
        get() = draft.passengers in 1..4

    private val isPriceValid: Boolean
        get() {
            val p = priceValue ?: return false
            if (p < 1000) return false
            if (priceSuggestion.hasRange) {
                return p in priceSuggestion.minLong..priceSuggestion.maxLong
            }
            return true
        }

    // ✅ Qadamga mos message (UI guidance)
    val validationMessage: String?
        get() = when (currentStep) {
            PublishStep.FROM -> if (from == null) "Jo‘nash manzilini tanlang." else null
            PublishStep.TO -> when {
                to == null -> "Manzilni tanlang."
                !isRouteValid -> "Jo‘nash va manzil bir xil bo‘lishi mumkin emas."
                else -> null
            }
            PublishStep.DATE -> if (!isDateValid) "O‘tib ketgan sanani tanlab bo‘lmaydi." else null
            PublishStep.TIME -> if (!isTimeValid) "O‘tib ketgan vaqtga e’lon berib bo‘lmaydi." else null
            PublishStep.PASSENGERS -> if (!isPassengersValid) "Yo‘lovchi joyi 1..4 bo‘lishi kerak." else null
            PublishStep.PRICE -> when {
                priceValue == null -> "Narxni kiriting."
                priceSuggestion.hasRange && !isPriceValid ->
                    "Narx ${priceSuggestion.minLong}..${priceSuggestion.maxLong} oralig‘ida bo‘lishi kerak."
                else -> null
            }
            PublishStep.PREVIEW -> null
        }

    // ✅ PREVIEW uchun ham real publish-validatsiya (finish fix)
    val publishValidationMessage: String?
        get() = when {
            from == null -> "Jo‘nash manzilini tanlang."
            to == null -> "Manzilni tanlang."
            !isRouteValid -> "Jo‘nash va manzil bir xil bo‘lishi mumkin emas."
            !isDateValid -> "O‘tib ketgan sanani tanlab bo‘lmaydi."
            !isTimeValid -> "O‘tib ketgan vaqtga e’lon berib bo‘lmaydi."
            !isPassengersValid -> "Yo‘lovchi joyi 1..4 bo‘lishi kerak."
            priceValue == null -> "Narxni kiriting."
            priceSuggestion.hasRange && !isPriceValid ->
                "Narx ${priceSuggestion.minLong}..${priceSuggestion.maxLong} oralig‘ida bo‘lishi kerak."
            else -> null
        }

    // ✅ Button enable: endi PREVIEW ham to‘liq tekshiradi
    val isNextEnabled: Boolean
        get() = when (currentStep) {
            PublishStep.FROM -> from != null
            PublishStep.TO -> to != null && isRouteValid
            PublishStep.DATE -> isDateValid
            PublishStep.TIME -> isTimeValid
            PublishStep.PASSENGERS -> isPassengersValid
            PublishStep.PRICE -> isPriceValid
            PublishStep.PREVIEW -> publishValidationMessage == null
        }
}

/**
 * default time: now + 15 min, 5 minutga yaxlitlab.
 */
private fun defaultTime(): LocalTime {
    val t = LocalTime.now().plusMinutes(15)
    val roundedMinute = ((t.minute + 4) / 5) * 5
    val safeMinute = if (roundedMinute == 60) 0 else roundedMinute
    val hour = if (roundedMinute == 60) (t.hour + 1) % 24 else t.hour
    return LocalTime.of(hour, safeMinute).truncatedTo(ChronoUnit.MINUTES)
}
