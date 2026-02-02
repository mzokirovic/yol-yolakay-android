package com.example.yol_yolakay.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.navigation.Screen
import kotlinx.coroutines.launch
import kotlin.OptIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigate: (Screen) -> Unit,
    vm: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(LocalContext.current))
) {
    val state = vm.state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message) {
        state.message?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profil") },
                actions = { TextButton(onClick = { vm.refresh() }) { Text("Refresh") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            ProfileHeader(
                displayName = state.profile?.displayName ?: "Guest",
                phone = state.profile?.phone ?: "Telefon yo‘q",
                onAvatarClick = { onNavigate(Screen.ProfileEdit) }
            )

            ProfileMenuItem(
                title = "To‘lov usullari",
                subtitle = "Tez orada",
                onClick = { onNavigate(Screen.PaymentMethods) } // ✅ showSoon o‘rniga
            )

            val vehicleSubtitle = state.vehicle?.let { v ->
                listOfNotNull(v.make, v.model, v.color).joinToString(" • ").ifBlank { "Kiritilmagan" }
            } ?: "Kiritilmagan"

            ProfileMenuItem(
                title = "Avtomobil ma’lumotlari",
                subtitle = vehicleSubtitle,
                onClick = { onNavigate(Screen.Vehicle) }
            )

            ProfileMenuItem(
                title = "Til sozlamalari",
                subtitle = state.profile?.language ?: "uz",
                onClick = { onNavigate(Screen.Language) }
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    displayName: String,
    phone: String,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val initial = displayName.firstOrNull()?.uppercase() ?: "G"

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        TextButton(onClick = onAvatarClick) { Text("Edit") }
    }
}

@Composable
private fun ProfileMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
