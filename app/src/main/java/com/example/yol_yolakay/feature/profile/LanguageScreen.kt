package com.example.yol_yolakay.feature.profile

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import com.example.yol_yolakay.core.session.CurrentUser
import kotlinx.coroutines.launch
import kotlin.OptIn

data class LanguageState(
    val isLoading: Boolean = true,
    val selected: String = "uz",
    val error: String? = null
)

class LanguageViewModel(private val repo: ProfileRemoteRepository) : ViewModel() {

    var state by mutableStateOf(LanguageState())
        private set

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching { repo.getMe() }
                .onSuccess { me ->
                    state = state.copy(isLoading = false, selected = me.language)
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    fun select(code: String) {
        state = state.copy(selected = code)
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching {
                repo.updateMe(UpdateProfileRequest(language = state.selected))
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
                    // 🚨 Argument olib tashlandi
                    return LanguageViewModel(ProfileRemoteRepository()) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    vm: LanguageViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = LanguageViewModel.factory(LocalContext.current)
    )
) {
    val s = vm.state
    val options = remember { listOf("uz" to "O‘zbek", "ru" to "Русский") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Til") },
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
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            options.forEach { (code, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.select(code) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label)
                    RadioButton(
                        selected = s.selected == code,
                        onClick = { vm.select(code) }
                    )
                }
            }

            Button(
                onClick = { vm.save(onDone = onBack) },
                enabled = !s.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (s.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Saqlash")
                }
            }
        }
    }
}
