// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/tripdetails/TripDetailsState.kt

package com.example.yol_yolakay.feature.tripdetails

import com.example.yol_yolakay.core.network.model.SeatApiModel
import com.example.yol_yolakay.core.network.model.TripApiModel

data class TripDetailsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val trip: TripApiModel? = null,
    val seats: List<SeatApiModel> = emptyList(),

    // UI interaksiyasi uchun (Seat tanlash va Bron qilish jarayoni)
    val selectedSeatNo: Int? = null,
    val isBooking: Boolean = false,

    val isLifecycleBusy: Boolean = false
)