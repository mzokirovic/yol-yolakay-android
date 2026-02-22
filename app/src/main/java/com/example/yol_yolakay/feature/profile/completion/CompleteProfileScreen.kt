package com.example.yol_yolakay.feature.profile.completion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yol_yolakay.core.network.model.UpdateProfileRequest
import com.example.yol_yolakay.feature.profile.data.ProfileRemoteRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    repo: ProfileRemoteRepository,
    onDone: () -> Unit
) {
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = cs.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .statusBarsPadding()
        ) {
            // ✅ Header Section
            Text(
                text = "Xush kelibsiz!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )

            Text(
                text = "Safar boshlash uchun profilingizni yakunlang",
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            // ✅ Form Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it; error = null },
                    label = { Text("Ism") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.onSurface,
                        focusedLabelColor = cs.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = last,
                    onValueChange = { last = it; error = null },
                    label = { Text("Familiya (ixtiyoriy)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.onSurface,
                        focusedLabelColor = cs.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    )
                )
            }

            // Error Display
            if (error != null) {
                Text(
                    text = error!!,
                    color = cs.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ✅ Premium Black Action Button
            Button(
                onClick = {
                    val f = first.trim()
                    if (f.isBlank()) {
                        error = "Iltimos, ismingizni kiriting"
                        return@Button
                    }
                    val displayName = if (last.trim().isBlank()) f else "$f ${last.trim()}"

                    loading = true
                    scope.launch {
                        runCatching { repo.updateMe(UpdateProfileRequest(displayName = displayName)) }
                            .onSuccess { onDone() }
                            .onFailure { e -> error = e.message ?: "Xatolik yuz berdi" }
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.onSurface,
                    disabledContainerColor = cs.onSurface.copy(alpha = 0.6f)
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Davom etish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}