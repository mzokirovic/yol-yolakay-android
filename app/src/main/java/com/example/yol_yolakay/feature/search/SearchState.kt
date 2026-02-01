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
)
