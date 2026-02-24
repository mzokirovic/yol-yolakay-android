package com.example.yol_yolakay.feature.profile.settings.language

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.R
import com.example.yol_yolakay.core.i18n.LanguageStore
import com.example.yol_yolakay.feature.profile.data.ProfileRemoteRepository
import kotlinx.coroutines.launch

data class LanguageState(
    val isLoading: Boolean = false,
    val selected: String = "uz",
    val error: String? = null
)

class LanguageViewModel(
    private val repo: ProfileRemoteRepository,
    private val langStore: LanguageStore
) : ViewModel() {

    var state by mutableStateOf(LanguageState(isLoading = true))
        private set

    private var initialSelected: String = "uz"

    init { load() }

    private fun load() {
        viewModelScope.launch {
            // 1) Local (tez)
            val local = runCatching { langStore.get() }.getOrNull()
            val selected = local?.takeIf { it.isNotBlank() } ?: "uz"
            initialSelected = selected

            state = state.copy(isLoading = false, selected = selected, error = null)

            // 2) Remote (ixtiyoriy)
            runCatching { repo.getMe() }
                .onSuccess { me ->
                    val remote = me.language.trim()
                    if (remote.isNotBlank() && remote != state.selected) {
                        state = state.copy(selected = remote)
                        initialSelected = remote
                        runCatching { langStore.set(remote) }
                    }
                }
                .onFailure { /* jim */ }
        }
    }

    fun consumeError() { state = state.copy(error = null) }

    fun select(code: String) {
        state = state.copy(selected = code, error = null)
    }

    fun hasChanges(): Boolean = state.selected != initialSelected

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val code = state.selected
            state = state.copy(isLoading = true, error = null)

            // 1) Local apply (ilova darhol o'zgaradi)
            runCatching {
                langStore.set(code)
                LanguageStore.apply(code)
            }.onFailure { e ->
                state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                return@launch
            }

            // 2) Remote (xato bo'lsa ham til ishlayveradi)
            runCatching { repo.updateLanguage(code) }
                .onSuccess {
                    initialSelected = code
                    state = state.copy(isLoading = false)
                    onDone()
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Serverga saqlanmadi")
                }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appCtx = context.applicationContext
                    return LanguageViewModel(
                        repo = ProfileRemoteRepository(),
                        langStore = LanguageStore(appCtx)
                    ) as T
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

    // ❗️String res qo‘shmaymiz: compile muammosiz bo‘lsin
    val options = remember {
        listOf(
            "uz" to "O‘zbek",
            "ru" to "Русский",
            "en" to "English"
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.language_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = cs.background,
        bottomBar = {
            Surface(color = cs.background, tonalElevation = 0.dp) {
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
                        enabled = !s.isLoading && vm.hasChanges(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        if (s.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = cs.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Saqlanmoqda…")
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { (code, label) ->
                val selected = s.selected == code

                Surface(
                    color = if (selected) cs.primary.copy(alpha = 0.08f) else cs.surface,
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !s.isLoading) { vm.select(code) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )

                        RadioButton(
                            selected = selected,
                            onClick = { vm.select(code) },
                            enabled = !s.isLoading
                        )
                    }
                }
            }
        }
    }
}