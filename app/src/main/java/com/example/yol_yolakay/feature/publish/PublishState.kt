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
    val price: String = ""
) {
    // Tugma "Enable" bo'lishi uchun shartlar
    val isNextEnabled: Boolean
        get() = when (currentStep) {
            PublishStep.FROM -> fromLocation.isNotBlank()
            PublishStep.TO -> toLocation.isNotBlank()
            PublishStep.DATE -> true
            PublishStep.TIME -> true
            PublishStep.PASSENGERS -> passengers > 0
            PublishStep.PRICE -> price.isNotBlank() && price.all { it.isDigit() }
            PublishStep.PREVIEW -> true
        }

    // Progress bar uchun (0.0 dan 1.0 gacha)
    val progress: Float
        get() = (currentStep.ordinal + 1) / PublishStep.values().size.toFloat()
}