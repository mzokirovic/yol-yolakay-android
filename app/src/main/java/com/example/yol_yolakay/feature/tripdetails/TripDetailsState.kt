package com.example.yol_yolakay.feature.tripdetails

import com.example.yol_yolakay.core.network.model.SeatApiModel
import com.example.yol_yolakay.core.network.model.TripApiModel

data class TripDetailsUiState(
    val isLoading: Boolean = false,
    val trip: TripApiModel? = null,
    val seats: List<SeatApiModel> = emptyList(),
    val selectedSeatNo: Int? = null,
    val isBooking: Boolean = false,
    val error: String? = null
)
