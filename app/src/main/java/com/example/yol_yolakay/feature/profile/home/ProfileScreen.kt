package com.example.yol_yolakay.feature.profile.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.profile.ui.HighlightCard
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
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                state.error?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
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
                    onAvatarClick = { showAvatarSheet = true },
                    onHeaderClick = { onNavigate(Screen.ProfileEdit) },
                    onRatingClick = { vm.showSoon("Ratinglar tizimi") }
                )
            }

            item {
                ProfileStats(
                    trips = 24,
                    passengers = 89,
                    onTripsClick = { vm.showSoon("Safarlar tarixi") },
                    onPassengersClick = { vm.showSoon("Yo'lovchilar ro'yxati") }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
            }

            if (showCompleteProfileCard) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        HighlightCard(
                            title = "Profilni yakunlang",
                            subtitle = "Ishonch va xavfsizlik uchun ma’lumotlarni to‘liq kiriting.",
                            cta = "Tahrirlash",
                            onClick = { onNavigate(Screen.ProfileEdit) }
                        )
                    }
                }
            }

            item {
                val vehicleSubtitle = state.vehicle?.let { v ->
                    listOfNotNull(v.make, v.model)
                        .joinToString(" ")
                        .plus(v.color?.let { " • $it" } ?: "")
                        .ifBlank { "Kiritilmagan" }
                } ?: "Kiritilmagan"

                ProfileMenuRow(
                    icon = Icons.Rounded.DirectionsCar,
                    title = "Avtomobilim",
                    subtitle = vehicleSubtitle,
                    onClick = { onNavigate(Screen.Vehicle) }
                )
                MenuDivider()
            }

            item {
                ProfileMenuRow(
                    icon = Icons.Rounded.Person,
                    title = "Shaxsiy ma'lumotlar",
                    onClick = { onNavigate(Screen.ProfileEdit) }
                )
                MenuDivider()
            }

            item {
                ProfileMenuRow(
                    icon = Icons.Rounded.Settings,
                    title = "Sozlamalar",
                    onClick = { onNavigate(Screen.Settings) }
                )
                MenuDivider()
            }

            item {
                ProfileMenuRow(
                    icon = Icons.Rounded.Shield,
                    title = "Maxfiylik va xavfsizlik",
                    onClick = { vm.showSoon("Maxfiylik va xavfsizlik") }
                )
                MenuDivider()
            }

            item {
                ProfileMenuRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Bildirishnomalar",
                    onClick = { vm.showSoon("Bildirishnomalar") }
                )
                MenuDivider()
            }

            item {
                ProfileMenuRow(
                    icon = Icons.Rounded.HelpOutline,
                    title = "Yordam",
                    onClick = { vm.showSoon("Yordam") }
                )
                MenuDivider()
            }

            item {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "Versiya 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
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
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
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

/* ---- local-only components ---- */

@Composable
private fun ProfileHeader(
    displayName: String,
    phone: String,
    ratingText: String,
    onAvatarClick: () -> Unit,
    onHeaderClick: () -> Unit,
    onRatingClick: () -> Unit // Yangi parametr
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHeaderClick() }
            .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // 🚀 O'ZGARISH: Uber kabi Rating tugmasi
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // Kulrang fon
                modifier = Modifier.clickable { onRatingClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107), // Sariq yulduz
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val initial = displayName.firstOrNull()?.uppercase() ?: "G"

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun ProfileStats(
    trips: Int,
    passengers: Int,
    onTripsClick: () -> Unit,
    onPassengersClick: () -> Unit
) {
    // 🚀 O'ZGARISH: Ilovaning Asosiy Ko'k rangi
    val primaryBlue = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { onTripsClick() }
                .padding(vertical = 4.dp, horizontal = 4.dp) // Bosish maydonini kattalashtirish
        ) {
            Text(
                text = trips.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = primaryBlue // Ko'k rang
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Safarlar",
                style = MaterialTheme.typography.bodyMedium,
                color = primaryBlue // Ko'k rang
            )
        }
        Column(
            modifier = Modifier
                .clickable { onPassengersClick() }
                .padding(vertical = 4.dp, horizontal = 4.dp) // Bosish maydonini kattalashtirish
        ) {
            Text(
                text = passengers.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = primaryBlue // Ko'k rang
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Yo'lovchilar",
                style = MaterialTheme.typography.bodyMedium,
                color = primaryBlue // Ko'k rang
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 80.dp, end = 24.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}