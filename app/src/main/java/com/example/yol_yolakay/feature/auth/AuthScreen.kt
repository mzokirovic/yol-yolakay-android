package com.example.yol_yolakay.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(
    vm: AuthViewModel,
    // Callbacklar endi ikkita: biri Home uchun, biri Profil to'ldirish uchun
    onNavigateToHome: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    val state by vm.state.collectAsState()

    // AuthEvent o'zgarishini kuzatamiz
    LaunchedEffect(state.event) {
        when (state.event) {
            is AuthEvent.NavigateToHome -> {
                vm.consumeEvent() // Eventni o'chiramiz
                onNavigateToHome()
            }
            is AuthEvent.NavigateToCompleteProfile -> {
                vm.consumeEvent()
                onNavigateToCompleteProfile()
            }
            AuthEvent.None -> {
                // Hech narsa qilmaymiz
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Kirish", style = MaterialTheme.typography.headlineSmall)

        when (state.step) {
            AuthStep.PHONE -> {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = vm::setPhone,
                    label = { Text("Telefon (+998...)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.error != null
                )
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = vm::sendOtp,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Kod yuborish")
                    }
                }
            }

            AuthStep.CODE -> {
                OutlinedTextField(
                    value = state.code,
                    onValueChange = vm::setCode,
                    label = { Text("SMS kod") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.error != null
                )
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = vm::verifyOtp,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Tasdiqlash")
                    }
                }
                TextButton(
                    onClick = vm::backToPhone,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    Text("Telefonni o‘zgartirish")
                }
            }
        }
    }
}