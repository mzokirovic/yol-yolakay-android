package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yol_yolakay.feature.publish.PublishUiState
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun Step6Price(
    uiState: PublishUiState,
    onPriceChange: (String) -> Unit,
    onAdjustPrice: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val currentPriceLong = uiState.draft.price.filter(Char::isDigit).toLongOrNull() ?: 0L
    val hasRange = uiState.priceSuggestion.hasRange
    val minL = uiState.priceSuggestion.minLong
    val maxL = uiState.priceSuggestion.maxLong
    val recL = uiState.priceSuggestion.recommendedLong

    val decEnabled = if (hasRange) currentPriceLong > minL else currentPriceLong > 5000L
    val incEnabled = if (hasRange) currentPriceLong < maxL else true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bir kishi uchun narx",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        // Distance + recommended
        if (uiState.priceSuggestion.isLoading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Masofa o'lchanmoqda...", color = cs.onSurfaceVariant)
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cs.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Masofa: ${uiState.priceSuggestion.distanceKm} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tavsiya: ${formatMoney(recL)} so‘m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                    if (hasRange) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Oraliq: ${formatMoney(minL)} .. ${formatMoney(maxL)} so‘m",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        // Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalIconButton(
                onClick = { onAdjustPrice(-5000) },
                enabled = decEnabled,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Kamaytirish")
            }

            Spacer(Modifier.width(20.dp))

            Text(
                text = formatMoney(currentPriceLong),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )

            Spacer(Modifier.width(20.dp))

            FilledTonalIconButton(
                onClick = { onAdjustPrice(5000) },
                enabled = incEnabled,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Oshirish")
            }
        }

        Text("so‘m", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)

        Spacer(Modifier.height(16.dp))

        // Warnings (theme colors)
        if (currentPriceLong > 0 && !uiState.priceSuggestion.isLoading && recL > 0) {
            val low = (recL * 0.7).roundToLong()
            val high = (recL * 1.5).roundToLong()

            when {
                currentPriceLong < low -> {
                    InfoCard(
                        text = "Narx juda past! Benzin xarajatini qoplamasligi mumkin.",
                        container = cs.tertiaryContainer,
                        content = cs.onTertiaryContainer
                    )
                }
                currentPriceLong > high -> {
                    InfoCard(
                        text = "Narx juda baland! Yo'lovchi topish qiyin bo‘lishi mumkin.",
                        container = cs.errorContainer,
                        content = cs.onErrorContainer
                    )
                }
                else -> {
                    InfoCard(
                        text = "✅ Narx bozor talablariga mos",
                        container = cs.primaryContainer,
                        content = cs.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedTextField(
            value = uiState.draft.price,
            onValueChange = { onPriceChange(it.filter(Char::isDigit)) },
            label = { Text("Aniq summa kiritish") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun InfoCard(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = container,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp)
        )
    }
}

private fun formatMoney(amount: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount).replace(",", " ")
}
