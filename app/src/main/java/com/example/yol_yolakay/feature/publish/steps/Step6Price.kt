package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun Step6Price(
    price: String,
    onPriceChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Bir kishi uchun narx qancha?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "O'rtacha narxni belgilash tavsiya etiladi",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = price,
            onValueChange = onPriceChange,
            label = { Text("Narx (so'm)") },
            placeholder = { Text("Masalan: 50000") },
            suffix = { Text("so'm") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Faqat raqam klaviaturasi
            shape = MaterialTheme.shapes.medium,
            singleLine = true
        )
    }
}