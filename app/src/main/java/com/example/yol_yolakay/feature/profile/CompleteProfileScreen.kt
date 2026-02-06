package com.example.yol_yolakay.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import kotlinx.coroutines.launch

@Composable
fun CompleteProfileScreen(
    repo: ProfileRemoteRepository,
    onDone: () -> Unit
) {
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Profilni to‘ldirish",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Ilovadan foydalanish uchun ismingizni kiriting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Ism kiritish
        OutlinedTextField(
            value = first,
            onValueChange = {
                first = it
                error = null
            },
            label = { Text("Ism") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Familiya kiritish
        OutlinedTextField(
            value = last,
            onValueChange = {
                last = it
                error = null
            },
            label = { Text("Familiya (ixtiyoriy)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            )
        )

        // Xatolik xabari
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Saqlash tugmasi
        Button(
            onClick = {
                val f = first.trim()
                val l = last.trim()

                if (f.isBlank()) {
                    error = "Ism kiritish shart"
                    return@Button
                }

                val displayName = if (l.isBlank()) f else "$f $l"

                loading = true
                scope.launch {
                    runCatching {
                        repo.updateMe(UpdateProfileRequest(displayName = displayName))
                    }
                        .onSuccess {
                            onDone() // Muvaffaqiyatli bo'lsa MainActivityga signal beramiz
                        }
                        .onFailure { e ->
                            error = e.message ?: "Internet bilan aloqa yo'q yoki xatolik yuz berdi"
                        }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Saqlash va davom etish")
            }
        }
    }
}