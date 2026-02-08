package com.example.yol_yolakay.feature.profile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.CarBrandDto
import com.example.yol_yolakay.core.network.model.UpsertVehicleRequest
import com.example.yol_yolakay.core.session.CurrentUser
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
                plateNumber.length >= 8
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
                    state = state.copy(isLoading = false, error = e.message)
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
                        seats = v.seats?.toString() ?: "4"
                    )
                } else {
                    state = state.copy(isLoading = false)
                }
            }
            .onFailure {
                state = state.copy(isLoading = false)
            }
    }

    fun onBrandSelected(brand: CarBrandDto) {
        state = state.copy(
            selectedBrand = brand,
            selectedModelName = "",
        )
    }

    fun onModelSelected(modelName: String) {
        state = state.copy(selectedModelName = modelName)
    }

    fun onColorSelected(color: String) {
        state = state.copy(selectedColor = color)
    }

    fun onPlateChange(v: String) {
        state = state.copy(plateNumber = v.uppercase())
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val req = UpsertVehicleRequest(
                make = state.selectedBrand?.name ?: "",
                model = state.selectedModelName,
                color = state.selectedColor,
                plate = state.plateNumber,
                seats = state.seats.toIntOrNull() ?: 4
            )

            repo.saveVehicle(req)
                .onSuccess {
                    state = state.copy(isLoading = false)
                    onDone()
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik yuz berdi")
                }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    // 🚨 Argument olib tashlandi
                    return VehicleViewModel(ProfileRemoteRepository()) as T
                }
            }
    }
}