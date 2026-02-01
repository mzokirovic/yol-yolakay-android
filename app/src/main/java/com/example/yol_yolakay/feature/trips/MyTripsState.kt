package com.example.yol_yolakay.feature.trips

import com.example.yol_yolakay.core.network.model.TripApiModel

data class MyTripsUiState(
    val isLoading: Boolean = false,
    val trips: List<TripApiModel> = emptyList(),
    val error: String? = null
)
