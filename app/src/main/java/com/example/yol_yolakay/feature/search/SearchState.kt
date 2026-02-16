package com.example.yol_yolakay.feature.search

import com.example.yol_yolakay.core.network.model.TripApiModel
import java.time.LocalDate

data class SearchUiState(
    val fromLocation: String = "",
    val toLocation: String = "",
    val date: LocalDate = LocalDate.now(),
    val passengers: Int = 1,
    val isLoading: Boolean = false,
    val trips: List<TripApiModel> = emptyList(),
    val error: String? = null
) {
    val validationMessage: String?
        get() = when {
            fromLocation.isBlank() -> "Qayerdan tanlang."
            toLocation.isBlank() -> "Qayerga tanlang."
            fromLocation.equals(toLocation, ignoreCase = true) -> "Qayerdan va Qayerga bir xil bo‘lishi mumkin emas."
            else -> null
        }

    val isSearchEnabled: Boolean get() = validationMessage == null && !isLoading
}
