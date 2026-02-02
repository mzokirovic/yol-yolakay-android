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
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import com.example.yol_yolakay.core.session.CurrentUser
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.OptIn

data class EditProfileState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val phone: String = "",
    val message: String? = null,
    val error: String? = null
)

class EditProfileViewModel(private val repo: ProfileRemoteRepository) : ViewModel() {
    var state by mutableStateOf(EditProfileState())
        private set

    init { load() }

    private fun load() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching { repo.getMe() }
                .onSuccess { me ->
                    state = state.copy(
                        isLoading = false,
                        displayName = me.displayName,
                        phone = me.phone.orEmpty()
                    )
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    fun updateName(v: String) { state = state.copy(displayName = v) }
    fun updatePhone(v: String) { state = state.copy(phone = v) }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching {
                repo.updateMe(
                    UpdateProfileRequest(
                        displayName = state.displayName.trim().ifBlank { "Guest" },
                        phone = state.phone.trim().ifBlank { null }
                    )
                )
            }.onSuccess {
                state = state.copy(isLoading = false, message = "Saqlandi")
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
                    return EditProfileViewModel(ProfileRemoteRepository(userId)) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    vm: EditProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = EditProfileViewModel.factory(LocalContext.current)
    )
) {
    val s = vm.state

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profilni tahrirlash") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Orqaga") }
                }
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
            if (s.error != null) {
                Text(s.error, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = s.displayName,
                onValueChange = vm::updateName,
                label = { Text("Ism Familiya") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = s.phone,
                onValueChange = vm::updatePhone,
                label = { Text("Telefon (ixtiyoriy)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Avatar: hozircha keyin qo‘shamiz (photo picker).",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
