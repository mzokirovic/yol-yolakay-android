package com.example.yol_yolakay.feature.profile.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.profile.ui.*
import com.example.yol_yolakay.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigate: (Screen) -> Unit,
    vm: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(LocalContext.current))
) {
    val state = vm.state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAvatarSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        // ✅ "Profilni yakunlang" faqat kerak bo'lsa chiqadi
        val name = state.profile?.displayName.orEmpty().trim()
        val hasName = name.isNotBlank() && name != "Guest"

        val hasVehicle = state.vehicle?.let { v ->
            !v.make.isNullOrBlank() && !v.model.isNullOrBlank() && !v.plate.isNullOrBlank()
        } ?: false

        val showCompleteProfileCard = !hasName || !hasVehicle

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            item {
                state.error?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            item {
                ProfileHeader(
                    displayName = state.profile?.displayName ?: "Guest",
                    phone = state.profile?.phone ?: "Telefon yo‘q",
                    ratingText = "5.0",
                    onAvatarClick = { showAvatarSheet = true }
                )
            }

            if (showCompleteProfileCard) {
                item {
                    HighlightCard(
                        title = "Profilni yakunlang",
                        subtitle = "Ishonch va xavfsizlik uchun ma’lumotlarni to‘liq kiriting.",
                        cta = "Tahrirlash",
                        onClick = { onNavigate(Screen.ProfileEdit) }
                    )
                }
            }

            item { SectionTitle("Account") }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Outlined.Person,
                        title = "Shaxsiy ma’lumotlar",
                        subtitle = "Ism, telefon, profil",
                        onClick = { onNavigate(Screen.ProfileEdit) }
                    )
                    DividerInGroup()

                    SettingsRow(
                        icon = Icons.Outlined.Payment,
                        title = "To‘lov usullari",
                        subtitle = "Tez orada",
                        onClick = { onNavigate(Screen.PaymentMethods) }
                    )
                    DividerInGroup()

                    val vehicleSubtitle = state.vehicle?.let { v ->
                        listOfNotNull(v.make, v.model, v.color)
                            .joinToString(" • ")
                            .ifBlank { "Kiritilmagan" }
                    } ?: "Kiritilmagan"

                    SettingsRow(
                        icon = Icons.Outlined.DirectionsCar,
                        title = "Avtomobil ma’lumotlari",
                        subtitle = vehicleSubtitle,
                        onClick = { onNavigate(Screen.Vehicle) }
                    )
                }
            }

            item { SectionTitle("Preferences") }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Outlined.Translate,
                        title = "Til",
                        subtitle = "Ilova tili",
                        trailingValue = state.profile?.language ?: "uz",
                        onClick = { onNavigate(Screen.Language) }
                    )
                    DividerInGroup()

                    SettingsRow(
                        icon = Icons.Outlined.DarkMode,
                        title = "Theme",
                        subtitle = "Light / Dark / System",
                        onClick = { vm.showSoon("Theme") }
                    )
                }
            }

            item { SectionTitle("Support") }

            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Yordam markazi",
                        subtitle = "Savollar va qo‘llab-quvvatlash",
                        onClick = { vm.showSoon("Yordam markazi") }
                    )
                }
            }
        }
    }

    if (showAvatarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Profil rasmi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Boshqalar ko‘radi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(6.dp))

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAvatarSheet = false
                            vm.showSoon("Rasmni yangilash")
                        }
                ) {
                    Text(
                        text = "Rasmni yangilash",
                        modifier = Modifier.padding(vertical = 14.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = { showAvatarSheet = false },
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Bekor") }
            }
        }
    }
}

/* ---- local-only (shu ekranga xos) ---- */

@Composable
private fun ProfileHeader(
    displayName: String,
    phone: String,
    ratingText: String,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            AssistChip(
                onClick = {},
                label = { Text("★ $ratingText") },
                leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconContentColor = MaterialTheme.colorScheme.tertiary
                )
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val initial = displayName.firstOrNull()?.uppercase() ?: "G"

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}