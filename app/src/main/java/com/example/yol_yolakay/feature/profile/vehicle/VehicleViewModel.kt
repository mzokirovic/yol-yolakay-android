package com.example.yol_yolakay.feature.profile.vehicle

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.CarBrandDto
import com.example.yol_yolakay.core.network.model.UpsertVehicleRequest
import com.example.yol_yolakay.feature.profile.data.ProfileRemoteRepository
import kotlinx.coroutines.launch

data class VehicleState(
    val isLoading: Boolean = false,
    val brands: List<CarBrandDto> = emptyList(),
    val selectedBrand: CarBrandDto? = null,
    val selectedModelName: String = "",
    val selectedColor: String = "",
    val plateNumber: String = "",
    val seats: String = "4",
    val error: String? = null
) {
    val isSaveEnabled: Boolean
        get() = selectedBrand != null &&
                selectedModelName.isNotBlank() &&
                selectedColor.isNotBlank() &&
                plateNumber.trim().length >= 6 // 8 emas: "01A777AA" kabi formatlar turlicha bo‘lishi mumkin
}

class VehicleViewModel(private val repo: ProfileRemoteRepository) : ViewModel() {
    var state by mutableStateOf(VehicleState(isLoading = true))
        private set

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repo.getCarReferences()
                .onSuccess { brands ->
                    state = state.copy(brands = brands)
                    loadExistingVehicle(brands)
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    private suspend fun loadExistingVehicle(brands: List<CarBrandDto>) {
        runCatching { repo.getVehicle() }
            .onSuccess { v ->
                if (v != null) {
                    val brand = brands.find { it.name == v.make }
                    state = state.copy(
                        isLoading = false,
                        selectedBrand = brand,
                        selectedModelName = v.model.orEmpty(),
                        selectedColor = v.color.orEmpty(),
                        plateNumber = v.plate.orEmpty(),
                        seats = v.seats?.toString() ?: "4",
                        error = null
                    )
                } else {
                    state = state.copy(isLoading = false, error = null)
                }
            }
            .onFailure {
                state = state.copy(isLoading = false, error = null)
            }
    }

    fun onBrandSelected(brand: CarBrandDto) {
        state = state.copy(
            selectedBrand = brand,
            selectedModelName = "",
            // brend o‘zgarsa model reset bo‘lishi shart, qolganlari qolsa ham bo‘ladi
            error = null
        )
    }

    fun onModelSelected(modelName: String) {
        state = state.copy(selectedModelName = modelName, error = null)
    }

    fun onColorSelected(color: String) {
        state = state.copy(selectedColor = color, error = null)
    }

    fun onPlateChange(v: String) {
        // foydalanuvchi yozayotgan paytda formatlashni juda agressiv qilmang
        state = state.copy(plateNumber = v.uppercase(), error = null)
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            val req = UpsertVehicleRequest(
                make = state.selectedBrand?.name.orEmpty(),
                model = state.selectedModelName.trim(),
                color = state.selectedColor.trim(),
                plate = state.plateNumber.trim().uppercase(),
                seats = state.seats.toIntOrNull() ?: 4
            )

            runCatching { repo.upsertVehicle(req) }
                .onSuccess { saved ->
                    // server qaytargan data bilan state’ni “canon” qilamiz
                    val brand = state.brands.find { it.name == saved.make } ?: state.selectedBrand
                    state = state.copy(
                        isLoading = false,
                        selectedBrand = brand,
                        selectedModelName = saved.model.orEmpty(),
                        selectedColor = saved.color.orEmpty(),
                        plateNumber = saved.plate.orEmpty(),
                        seats = saved.seats?.toString() ?: state.seats,
                        error = null
                    )
                    onDone()
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik yuz berdi")
                }
        }
    }

    private fun clearVehicleLocal() {
        state = state.copy(
            selectedBrand = null,
            selectedModelName = "",
            selectedColor = "",
            plateNumber = "",
            seats = "4",
            error = null
        )
    }

    fun deleteVehicle(onDone: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            repo.deleteVehicle()
                .onSuccess {
                    clearVehicleLocal()
                    state = state.copy(isLoading = false)
                    onDone()
                }
                .onFailure { e ->
                    // ❗️Backend delete ishlamasa — local tozalab yubormaymiz.
                    state = state.copy(isLoading = false, error = e.message ?: "O‘chirishda xatolik")
                }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VehicleViewModel(ProfileRemoteRepository()) as T
                }
            }
    }
}