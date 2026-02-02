package com.example.yol_yolakay.feature.profile

import com.example.yol_yolakay.core.network.model.ProfileApiModel
import com.example.yol_yolakay.core.network.model.VehicleApiModel

data class ProfileState(
    val isLoading: Boolean = true,
    val profile: ProfileApiModel? = null,
    val vehicle: VehicleApiModel? = null,
    val message: String? = null,
    val error: String? = null
)
