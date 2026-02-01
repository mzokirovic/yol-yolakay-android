package com.example.yol_yolakay.feature.publish

import java.time.LocalDate
import java.time.LocalTime

// Qadamlar ro'yxati (Tartib muhim!)
enum class PublishStep {
    FROM,       // Qayerdan
    TO,         // Qayerga
    DATE,       // Sana
    TIME,       // Soat
    PASSENGERS, // Odam soni
    PRICE,      // Narx
    PREVIEW     // Tasdiqlash
}

data class PublishUiState(
    val currentStep: PublishStep = PublishStep.FROM,

    // Ma'lumotlar
    val fromLocation: String = "",
    val toLocation: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val passengers: Int = 1,
    val price: String = "",

    // Network/UI holatlar
    val isPublishing: Boolean = false,
    val publishError: String? = null,
    val isPublished: Boolean = false
) {
    // Tugma "Enable" bo'lishi uchun shartlar
    val isNextEnabled: Boolean
        get() = when (currentStep) {
            PublishStep.FROM -> fromLocation.isNotBlank()
            PublishStep.TO -> toLocation.isNotBlank()
            PublishStep.DATE -> true
            PublishStep.TIME -> true
            PublishStep.PASSENGERS -> passengers in 1..4
            PublishStep.PRICE -> price.isNotBlank() && price.all { it.isDigit() }

            // ✅ PREVIEW'da ham hammasi valid bo'lsa enable
            PublishStep.PREVIEW ->
                fromLocation.isNotBlank() &&
                        toLocation.isNotBlank() &&
                        passengers in 1..4 &&
                        price.isNotBlank() && price.all { it.isDigit() }
        }

    // Progress bar uchun (0.0 dan 1.0 gacha)
    val progress: Float
        get() = (currentStep.ordinal + 1) / PublishStep.values().size.toFloat()
}
