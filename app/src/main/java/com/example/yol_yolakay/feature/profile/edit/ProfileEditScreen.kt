package com.example.yol_yolakay.feature.profile.edit

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import com.example.yol_yolakay.feature.profile.data.ProfileRemoteRepository
import kotlinx.coroutines.launch

// 1. Ekran holatlarini boshqarish uchun Enum
enum class EditMode {
    ViewProfile, EditName
}

data class EditProfileState(
    val isLoading: Boolean = true,
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val message: String? = null,
    val error: String? = null,
    val currentMode: EditMode = EditMode.ViewProfile
) {
    val displayName: String get() = "$firstName $lastName".trim()
}

class EditProfileViewModel(private val repo: ProfileRemoteRepository) : ViewModel() {
    var state by mutableStateOf(EditProfileState())
        private set

    init { load() }

    private fun load() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, message = null)
            runCatching { repo.getMe() }
                .onSuccess { me ->
                    val parts = me.displayName.split(" ", limit = 2)
                    state = state.copy(
                        isLoading = false,
                        firstName = parts.getOrNull(0) ?: "",
                        lastName = parts.getOrNull(1) ?: "",
                        phone = me.phone.orEmpty()
                    )
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
        }
    }

    fun updateFirstName(v: String) { state = state.copy(firstName = v, error = null) }
    fun updateLastName(v: String) { state = state.copy(lastName = v, error = null) }

    // Ekranni o'zgartirish funksiyasi
    fun setMode(mode: EditMode) { state = state.copy(currentMode = mode) }

    fun consumeError() { state = state.copy(error = null) }
    fun consumeMessage() { state = state.copy(message = null) }

    fun saveName() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, message = null)
            runCatching {
                repo.updateMe(
                    UpdateProfileRequest(
                        displayName = state.displayName.ifBlank { "Foydalanuvchi" },
                        phone = state.phone.trim().ifBlank { null }
                    )
                )
            }.onSuccess {
                // Saqlangach, orqaga (ViewProfile rejimiga) qaytamiz
                state = state.copy(
                    isLoading = false,
                    message = "Ism yangilandi",
                    currentMode = EditMode.ViewProfile
                )
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

@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    vm: EditProfileViewModel = viewModel(factory = EditProfileViewModel.factory(LocalContext.current))
) {
    val s = vm.state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(s.error) {
        s.error?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            vm.consumeError()
        }
    }

    LaunchedEffect(s.message) {
        s.message?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            vm.consumeMessage()
        }
    }

    // 2. AnimatedContent orqali 2 ta alohida ekranni silliq almashtiramiz
    AnimatedContent(
        targetState = s.currentMode,
        transitionSpec = {
            if (targetState == EditMode.EditName) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "Profile Screen Animation"
    ) { mode ->
        when (mode) {
            EditMode.ViewProfile -> {
                ProfileViewMode(
                    state = s,
                    snackbarHostState = snackbarHostState,
                    onBack = onBack,
                    onEditNameClick = { vm.setMode(EditMode.EditName) }
                )
            }
            EditMode.EditName -> {
                EditNameMode(
                    state = s,
                    onBack = { vm.setMode(EditMode.ViewProfile) },
                    onSave = { vm.saveName() },
                    onFirstNameChange = vm::updateFirstName,
                    onLastNameChange = vm::updateLastName
                )
            }
        }
    }
}

// ============================================================================
// EKRAN 1: MA'LUMOTLARNI KO'RISH (ASOSIY PROFIL EKRANI)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileViewMode(
    state: EditProfileState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEditNameClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Shaxsiy ma'lumotlar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = cs.surfaceVariant.copy(alpha = 0.3f) // Zamonaviy kulrang fon
    ) { padding ->

        if (state.isLoading && state.firstName.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Profil Rasmi Qismi
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(cs.primary.copy(alpha = 0.1f))
                            .clickable { /* MVP dan keyin rasm o'zgartirish qo'shiladi */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.firstName.take(1).uppercase(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.primary
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(cs.surface)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(cs.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Ma'lumotlar Kartochkasi (Card Style)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cs.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ProfileRowItem(
                            label = "Ism va familiya",
                            value = state.displayName,
                            onClick = onEditNameClick
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outlineVariant.copy(alpha = 0.5f))

                        // 🚀 Telefon raqamni aqlli formatlash
                        val displayPhone = if (state.phone.isNotBlank()) {
                            if (state.phone.startsWith("+")) state.phone else "+${state.phone}"
                        } else {
                            "Kiritilmagan"
                        }

                        ProfileRowItem(
                            label = "Telefon raqam",
                            value = displayPhone,
                            onClick = { /* MVP dan keyin telefon raqam o'zgartirish ekrani ochiladi */ }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Ba'zi ma'lumotlar xavfsizlik maqsadida boshqa foydalanuvchilarga ko'rsatilmaydi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileRowItem(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

// ============================================================================
// EKRAN 2: ISMNI TAHRIRLASH EKRANI (FULL SCREEN)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditNameMode(
    state: EditProfileState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val isFormValid = state.firstName.isNotBlank() && state.lastName.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ismni tahrirlash", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = cs.background,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSave()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isLoading && isFormValid
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = cs.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Saqlash", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = cs.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Haqiqiy ismingizni kiriting",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bu ism haydovchilar va yo'lovchilarga ko'rinadi. Shaffoflik va ishonch uchun o'z ismingizni yozing.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = state.firstName,
                onValueChange = onFirstNameChange,
                label = { Text("Ism") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.lastName,
                onValueChange = onLastNameChange,
                label = { Text("Familiya") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}