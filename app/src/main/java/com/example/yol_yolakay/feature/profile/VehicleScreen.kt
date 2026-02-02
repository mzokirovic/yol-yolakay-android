package com.example.yol_yolakay.feature.profile

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.UpsertVehicleRequest
import com.example.yol_yolakay.core.session.CurrentUser
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class VehicleState(
    val isLoading: Boolean = true,
    val make: String = "",
    val model: String = "",
    val color: String = "",
    val plate: String = "",
    val seats: String = "",
    val error: String? = null
)

class VehicleViewModel(private val repo: ProfileRemoteRepository) : ViewModel() {
    var state by mutableStateOf(VehicleState())
        private set

    init { load() }

    private fun load() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching { repo.getVehicle() }
                .onSuccess { v ->
                    state = state.copy(
                        isLoading = false,
                        make = v?.make.orEmpty(),
                        model = v?.model.orEmpty(),
                        color = v?.color.orEmpty(),
                        plate = v?.plate.orEmpty(),
                        seats = v?.seats?.toString().orEmpty()
                    )
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    fun setMake(v: String) { state = state.copy(make = v) }
    fun setModel(v: String) { state = state.copy(model = v) }
    fun setColor(v: String) { state = state.copy(color = v) }
    fun setPlate(v: String) { state = state.copy(plate = v) }
    fun setSeats(v: String) { state = state.copy(seats = v.filter { it.isDigit() }) }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val seatsInt = state.seats.toIntOrNull()
            runCatching {
                repo.upsertVehicle(
                    UpsertVehicleRequest(
                        make = state.make.trim().ifBlank { null },
                        model = state.model.trim().ifBlank { null },
                        color = state.color.trim().ifBlank { null },
                        plate = state.plate.trim().ifBlank { null },
                        seats = seatsInt
                    )
                )
            }.onSuccess {
                state = state.copy(isLoading = false)
                onDone()
            }.onFailure { e ->
                state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val userId = CurrentUser.id(context)
                    return VehicleViewModel(ProfileRemoteRepository(userId)) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(
    onBack: () -> Unit,
    vm: VehicleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = VehicleViewModel.factory(LocalContext.current)
    )
) {
    val s = vm.state

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Avtomobil") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Orqaga") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (s.error != null) Text(s.error, color = MaterialTheme.colorScheme.error)

            OutlinedTextField(s.make, vm::setMake, label = { Text("Marka") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.model, vm::setModel, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.color, vm::setColor, label = { Text("Rang") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.plate, vm::setPlate, label = { Text("Davlat raqami (ixtiyoriy)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.seats, vm::setSeats, label = { Text("O‘rindiqlar (ixtiyoriy)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = { vm.save(onDone = onBack) },
                enabled = !s.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (s.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                else Text("Saqlash")
            }
        }
    }
}
