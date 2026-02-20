package com.example.yol_yolakay.feature.profile.edit

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import com.example.yol_yolakay.feature.profile.data.ProfileRemoteRepository
import kotlinx.coroutines.launch

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
            state = state.copy(isLoading = true, error = null, message = null)
            runCatching { repo.getMe() }
                .onSuccess { me ->
                    state = state.copy(
                        isLoading = false,
                        displayName = me.displayName ?: "",
                        phone = me.phone.orEmpty()
                    )
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    fun updateName(v: String) { state = state.copy(displayName = v, message = null, error = null) }
    fun updatePhone(v: String) { state = state.copy(phone = v, message = null, error = null) }

    fun consumeError() { state = state.copy(error = null) }
    fun consumeMessage() { state = state.copy(message = null) }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, message = null)
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
                    return EditProfileViewModel(ProfileRemoteRepository()) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    vm: EditProfileViewModel = viewModel(factory = EditProfileViewModel.factory(LocalContext.current))
) {
    val s = vm.state
    val cs = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Snackbar: error
    LaunchedEffect(s.error) {
        s.error?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            vm.consumeError()
        }
    }

    // Snackbar: message (optional)
    LaunchedEffect(s.message) {
        s.message?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            vm.consumeMessage()
        }
    }

    val isInitialLoading =
        s.isLoading && s.displayName.isBlank() && s.phone.isBlank() && s.error == null && s.message == null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Orqaga")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = cs.background,
        bottomBar = {
            Surface(color = cs.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            vm.save(onDone = onBack)
                        },
                        enabled = !s.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        if (s.isLoading && !isInitialLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = cs.onPrimary
                            )
                        } else {
                            Text("Saqlash", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->

        if (isInitialLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = s.displayName,
                    onValueChange = vm::updateName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ism") },
                    singleLine = true,
                    enabled = !s.isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = s.phone,
                    onValueChange = vm::updatePhone,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Telefon") },
                    singleLine = true,
                    enabled = !s.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    )
                )
            }
        }
    }
}