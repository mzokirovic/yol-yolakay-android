package com.example.yol_yolakay.core.network.model

import kotlinx.serialization.Serializable

// Eski model (Saqlab qolamiz)
@Serializable
data class VehicleApiModel(
    val userId: String? = null,
    val make: String? = null,
    val model: String? = null,
    val color: String? = null,
    val plate: String? = null,
    val seats: Int? = null,
    val updatedAt: String? = null
)

// ✅ YANGI: Reference (Brend va Modellar) uchun modellar
@Serializable
data class VehicleReferenceResponse(
    val success: Boolean,
    val data: List<CarBrandDto>
)

@Serializable
data class CarBrandDto(
    val id: String,
    val name: String,
    val car_models: List<CarModelDto> // Supabase nested relation
)

@Serializable
data class CarModelDto(
    val id: String,
    val name: String
)

// ✅ YANGI: Saqlash uchun so'rov modeli
@Serializable
data class UpsertVehicleRequest(
    val make: String,  // Chevrolet
    val model: String, // Cobalt
    val color: String, // Oq
    val plate: String, // 01 A 777 AA
    val seats: Int = 4
)