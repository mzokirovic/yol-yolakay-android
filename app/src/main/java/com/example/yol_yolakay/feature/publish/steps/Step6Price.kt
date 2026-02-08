package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yol_yolakay.feature.publish.PublishUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun Step6Price(
    uiState: PublishUiState,
    onPriceChange: (String) -> Unit,
    onAdjustPrice: (Int) -> Unit
) {
    // ✅ TUZATILDI: Draft ichidan olinmoqda
    val currentPrice = uiState.draft.price.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bir kishi uchun narx",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ TUZATILDI: PriceSuggestion ichidan olinmoqda
        if (uiState.priceSuggestion.isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Masofa o'lchanmoqda...", color = Color.Gray)
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        // ✅ TUZATILDI: PriceSuggestion
                        text = "Masofa: ${uiState.priceSuggestion.distanceKm} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        // ✅ TUZATILDI: PriceSuggestion
                        text = "Tavsiya: ${formatMoney(uiState.priceSuggestion.recommended)} so'm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // CONTROL
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalIconButton(
                onClick = { onAdjustPrice(-5000) },
                modifier = Modifier.size(56.dp),
                // ✅ TUZATILDI: PriceSuggestion
                enabled = currentPrice > uiState.priceSuggestion.min
            ) {
                Text("-", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = formatMoney(currentPrice),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(24.dp))

            FilledTonalIconButton(
                onClick = { onAdjustPrice(5000) },
                modifier = Modifier.size(56.dp),
                // ✅ TUZATILDI: PriceSuggestion
                enabled = currentPrice < uiState.priceSuggestion.max
            ) {
                Icon(Icons.Default.Add, contentDescription = "Oshirish", modifier = Modifier.size(28.dp))
            }
        }

        Text("so'm", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        // WARNINGS
        // ✅ TUZATILDI: PriceSuggestion va isLoading
        if (currentPrice > 0 && !uiState.priceSuggestion.isLoading) {
            val rec = uiState.priceSuggestion.recommended
            when {
                // Nolga bo'linish xavfini oldini olish
                rec > 0 && currentPrice < rec * 0.7 -> {
                    WarningCard(
                        "Narx juda past! Benzin xarajatini qoplamasligi mumkin.",
                        Color(0xFFFFF3E0), Color(0xFFE65100)
                    )
                }
                rec > 0 && currentPrice > rec * 1.5 -> {
                    WarningCard(
                        "Narx juda baland! Yo'lovchi topish qiyin bo'lishi mumkin.",
                        Color(0xFFFFEBEE), Color(0xFFC62828)
                    )
                }
                else -> {
                    Text(
                        "✅ Narx bozor talablariga mos",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedTextField(
            // ✅ TUZATILDI: Draft
            value = uiState.draft.price,
            onValueChange = onPriceChange,
            label = { Text("Aniq summa kiritish") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun WarningCard(text: String, bgColor: Color, textColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", modifier = Modifier.padding(end = 8.dp))
            Text(text = text, color = textColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun formatMoney(amount: Double): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount).replace(",", " ")
}