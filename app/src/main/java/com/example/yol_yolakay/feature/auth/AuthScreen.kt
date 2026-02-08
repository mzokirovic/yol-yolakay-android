// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/auth/AuthScreen.kt

package com.example.yol_yolakay.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.network.BackendClient
import com.example.yol_yolakay.core.session.SessionStore

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    val ctx = LocalContext.current

    // ViewModelni shu yerda yaratamiz (Dependency Injection soddalashtirilgan)
    val sessionStore = remember { SessionStore(ctx) }
    // Clientni init qilamiz (agar qilinmagan bo'lsa)
    BackendClient.init(ctx, sessionStore)
    val repo = remember { AuthRemoteRepository(BackendClient.client) }

    val vm: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(ctx, repo, sessionStore)
    )

    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigatsiya
    LaunchedEffect(state.event) {
        when (state.event) {
            is AuthEvent.NavigateToHome -> {
                vm.consumeEvent()
                onNavigateToHome()
            }
            is AuthEvent.NavigateToCompleteProfile -> {
                vm.consumeEvent()
                onNavigateToCompleteProfile()
            }
            AuthEvent.None -> {}
        }
    }

    // Xatolik chiqqanda Snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (state.step == AuthStep.PHONE) "Kirish" else "Tasdiqlash",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(32.dp))

            when (state.step) {
                AuthStep.PHONE -> {
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = vm::setPhone,
                        label = { Text("Telefon raqami") },
                        placeholder = { Text("+998 90 123 45 67") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = vm::sendOtp,
                        enabled = !state.isLoading && state.phone.length > 4,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (state.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Kod olish")
                    }
                }

                AuthStep.CODE -> {
                    Text("Sizning raqamingizga kod yuborildi: ${state.phone}")
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.code,
                        onValueChange = vm::setCode,
                        label = { Text("SMS kod") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = vm::verifyOtp,
                        enabled = !state.isLoading && state.code.length >= 6,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (state.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Tasdiqlash")
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = vm::backToPhone) {
                        Text("Raqamni o'zgartirish")
                    }
                }
            }
        }
    }
}