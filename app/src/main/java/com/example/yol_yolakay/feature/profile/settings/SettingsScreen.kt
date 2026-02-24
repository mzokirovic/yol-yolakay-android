package com.example.yol_yolakay.feature.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.R
import com.example.yol_yolakay.core.di.AppGraph
import com.example.yol_yolakay.core.i18n.LanguageStore
import com.example.yol_yolakay.feature.profile.ui.SectionTitle
import com.example.yol_yolakay.feature.profile.ui.SettingsGroup
import com.example.yol_yolakay.feature.profile.ui.SettingsRow
import com.example.yol_yolakay.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onLogoutSuccess: () -> Unit // 🚀 MUHIM: NavHost'ga xabar berish uchun callback qo'shdik
) {
    val ctx = LocalContext.current.applicationContext
    val sessionStore = remember { AppGraph.sessionStore(ctx) }
    val langStore = remember { LanguageStore(ctx) }

    val currentCode by langStore.languageFlow.collectAsState(initial = null)
    val languageLabel = remember(currentCode) {
        when ((currentCode ?: "uz").lowercase()) {
            "ru" -> "Русский"
            "en" -> "English"
            else -> "O‘zbek"
        }
    }

    val scope = rememberCoroutineScope()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            SectionTitle(stringResource(R.string.settings_general))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.settings_language),
                    trailingValue = languageLabel,
                    onClick = { onNavigate(Screen.Language) }
                )
            }

            SectionTitle(stringResource(R.string.settings_account))
            SettingsGroup {
                DangerRow(
                    title = stringResource(R.string.logout),
                    onClick = { showLogoutConfirm = true }
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { if (!loggingOut) showLogoutConfirm = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_body)) },
            confirmButton = {
                TextButton(
                    enabled = !loggingOut,
                    onClick = {
                        loggingOut = true
                        scope.launch {
                            sessionStore.clear() // Xotiradan tozalaymiz
                            onLogoutSuccess() // 🚀 NavHost'ga "Auth ga qayt" deb buyruq beramiz
                        }
                    }
                ) { Text(if (loggingOut) "..." else stringResource(R.string.logout_confirm_yes)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !loggingOut,
                    onClick = { showLogoutConfirm = false }
                ) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun DangerRow(
    title: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(cs.errorContainer.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                contentDescription = null,
                tint = cs.error,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = cs.error
        )
    }
}