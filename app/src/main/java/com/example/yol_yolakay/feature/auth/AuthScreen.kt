package com.example.yol_yolakay.feature.auth

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.di.AppGraph
import com.example.yol_yolakay.core.network.BackendClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { AppGraph.init(ctx) }

    val sessionStore = remember { AppGraph.sessionStore(ctx) }
    val repo = remember { AuthRemoteRepository(BackendClient.client) }

    val vm: AuthViewModel = viewModel(factory = AuthViewModel.factory(repo, sessionStore))
    val state by vm.state.collectAsState()
    val cs = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.event) {
        when (state.event) {
            AuthEvent.NavigateHome -> { vm.consumeEvent(); onNavigateToHome() }
            AuthEvent.NavigateCompleteProfile -> { vm.consumeEvent(); onNavigateToCompleteProfile() }
            AuthEvent.None -> Unit
        }
    }

    Scaffold(
        containerColor = cs.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(60.dp))

            // ✅ Dinamik Sarlavha
            AnimatedContent(
                targetState = state.step,
                label = "auth_title"
            ) { step ->
                Column {
                    Text(
                        text = if (step == AuthStep.PHONE) "Xush kelibsiz!" else "Kodni kiriting",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (step == AuthStep.PHONE)
                            "Ro'yxatdan o'tish uchun telefon raqamingizni kiriting"
                        else
                            "Sizning raqamingizga 6 xonali kod yubordik",
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // ✅ Qadamlar mantiqi
            AnimatedContent(
                targetState = state.step,
                label = "auth_steps"
            ) { step ->
                when (step) {
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
}

@Composable
private fun PhoneStep(
    phone: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column {
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Telefon raqami") },
            placeholder = { Text("+998 90 123 45 67") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary,
                unfocusedBorderColor = cs.outlineVariant
            )
        )

        Spacer(Modifier.height(24.dp))

        // ✅ Premium Blue/Black Button
        Button(
            onClick = onSend,
            enabled = !isLoading && phone.length >= 9,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary, // Ilova uslubidagi Blue rang
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Davom etish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
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
    val cs = MaterialTheme.colorScheme
    Column {
        OutlinedTextField(
            value = code,
            onValueChange = { raw -> onCodeChange(raw.filter(Char::isDigit).take(6)) },
            label = { Text("SMS kod") },
            placeholder = { Text("0 0 0 0 0 0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary,
                unfocusedBorderColor = cs.outlineVariant
            )
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onVerify,
            enabled = !isLoading && code.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cs.onSurface), // Tasdiqlash uchun Qora rang ham chiroyli chiqadi
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Tasdiqlash", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ✅ Footer Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onEditPhone) {
                Text("Raqamni o'zgartirish", color = cs.onSurfaceVariant)
            }

            TextButton(
                onClick = onResend,
                enabled = canResend && !isLoading
            ) {
                Text(
                    text = if (canResend) "Qayta yuborish" else "Qayta yuborish: ${resendSecondsLeft}s",
                    color = if (canResend) cs.primary else cs.onSurfaceVariant,
                    fontWeight = if (canResend) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}