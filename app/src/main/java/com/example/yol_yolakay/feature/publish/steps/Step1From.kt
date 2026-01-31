package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Step1From(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Qayerdan yo'lga chiqasiz?", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Shahar yoki tuman") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}