package com.example.yol_yolakay.feature.search

import com.example.yol_yolakay.feature.publish.model.TripDto
import java.time.LocalDate

data class SearchUiState(
    val fromLocation: String = "",
    val toLocation: String = "",
    val date: LocalDate = LocalDate.now(),
    val passengers: Int = 1,
    // Network holati
    val isLoading: Boolean = false,
    val trips: List<TripDto> = emptyList(),
    val error: String? = null
)