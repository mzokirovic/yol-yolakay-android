package com.example.yol_yolakay.feature.profile.language

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import com.example.yol_yolakay.feature.profile.data.ProfileRemoteRepository
import kotlinx.coroutines.launch

data class LanguageState(
    val isLoading: Boolean = true,
    val selected: String = "uz",
    val error: String? = null
)

class LanguageViewModel(private val repo: ProfileRemoteRepository) : ViewModel() {

    var state by mutableStateOf(LanguageState())
        private set

    init { load() }

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

    fun consumeError() { state = state.copy(error = null) }

    fun select(code: String) {
        state = state.copy(selected = code, error = null)
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching { repo.updateMe(UpdateProfileRequest(language = state.selected)) }
                .onSuccess {
                    state = state.copy(isLoading = false)
                    onDone()
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LanguageViewModel(ProfileRemoteRepository()) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    vm: LanguageViewModel = viewModel(factory = LanguageViewModel.factory(LocalContext.current))
) {
    val s = vm.state
    val cs = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(s.error) {
        s.error?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            vm.consumeError()
        }
    }

    val options = remember {
        listOf(
            "uz" to "O‘zbek",
            "ru" to "Русский"
        )
    }

    val isInitialLoading = s.isLoading && s.error == null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Til") },
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
                            Text("Saqlash")
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (code, label) ->
                Surface(
                    color = cs.surface,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !s.isLoading) { vm.select(code) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = s.selected == code,
                            onClick = { vm.select(code) },
                            enabled = !s.isLoading
                        )
                    }
                }
            }
        }
    }
}