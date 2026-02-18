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
import com.example.yol_yolakay.core.di.AppGraph
import com.example.yol_yolakay.core.network.BackendClient

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    val ctx = LocalContext.current

    // ✅ Ensure graph/client initialized (idempotent)
    LaunchedEffect(Unit) { AppGraph.init(ctx) }

    // ✅ DI
    val sessionStore = remember { AppGraph.sessionStore(ctx) }
    val repo = remember { AuthRemoteRepository(BackendClient.client) }

    // ✅ VM
    val vm: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(repo, sessionStore)
    )
    val state by vm.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.event) {
        when (state.event) {
            AuthEvent.NavigateHome -> {
                vm.consumeEvent()
                onNavigateToHome()
            }
            AuthEvent.NavigateCompleteProfile -> {
                vm.consumeEvent()
                onNavigateToCompleteProfile()
            }
            AuthEvent.None -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (state.step == AuthStep.PHONE) "Kirish" else "Tasdiqlash",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.step == AuthStep.PHONE)
                    "Telefon raqamingizga SMS kod yuboramiz"
                else
                    "6 xonali kodni kiriting",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(22.dp))

            when (state.step) {
                AuthStep.PHONE -> PhoneStep(
                    phone = state.phoneRaw,
                    onPhoneChange = vm::setPhone,
                    isLoading = state.isLoading,
                    onSend = vm::sendOtp
                )

                AuthStep.CODE -> CodeStep(
                    phone = state.phoneE164.ifBlank { state.phoneRaw },
                    code = state.code,
                    onCodeChange = vm::setCode,
                    isLoading = state.isLoading,
                    canResend = state.canResend,
                    resendSecondsLeft = state.resendSecondsLeft,
                    onResend = vm::resendOtp,
                    onEditPhone = vm::backToPhone,
                    onVerify = vm::verifyOtp
                )
            }
        }
    }
}

@Composable
private fun PhoneStep(
    phone: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Telefon raqami") },
        placeholder = { Text("+998 90 123 45 67") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(14.dp))

    Button(
        onClick = onSend,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(12.dp))
            Text("Yuborilyapti…")
        } else {
            Text("Kod olish", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CodeStep(
    phone: String,
    code: String,
    onCodeChange: (String) -> Unit,
    isLoading: Boolean,
    canResend: Boolean,
    resendSecondsLeft: Int,
    onResend: () -> Unit,
    onEditPhone: () -> Unit,
    onVerify: () -> Unit
) {
    Text(
        text = "Kod yuborildi: $phone",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = code,
        onValueChange = { raw ->
            // ✅ faqat raqam + max 6
            onCodeChange(raw.filter(Char::isDigit).take(6))
        },
        label = { Text("SMS kod") },
        placeholder = { Text("______") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(14.dp))

    Button(
        onClick = onVerify,
        enabled = !isLoading && code.length == 6,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(12.dp))
            Text("Tekshirilmoqda…")
        } else {
            Text("Tasdiqlash", fontWeight = FontWeight.SemiBold)
        }
    }

    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onEditPhone) { Text("Raqamni o‘zgartirish") }

        TextButton(
            onClick = onResend,
            enabled = canResend && !isLoading
        ) {
            Text(if (canResend) "Qayta yuborish" else "Qayta yuborish: ${resendSecondsLeft}s")
        }
    }
}
