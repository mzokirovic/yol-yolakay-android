package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    val sug = uiState.priceSuggestion
    val hasRange = sug.hasRange
    val minL = sug.minLong
    val maxL = sug.maxLong
    val recL = sug.recommendedLong

    val step = 5000

    val decEnabled = if (hasRange) currentPriceLong > minL else currentPriceLong > 5000L
    val incEnabled = if (hasRange) currentPriceLong < maxL else true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Bir kishi uchun narx",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Bu ridesharing: maqsad — xarajatlarni bo‘lishish",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )

        if (sug.isLoading) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cs.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Masofa va tavsiya narx hisoblanmoqda…", color = cs.onSurfaceVariant)
                }
            }
        } else {
            // ✅ Suggestion card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cs.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Masofa: ${sug.distanceKm} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Tavsiya narx",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant
                            )
                            Text(
                                text = "${formatMoney(recL)} so‘m",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.primary
                            )
                        }

                        FilledTonalButton(
                            onClick = { if (recL > 0) onPriceChange(recL.toString()) },
                            enabled = recL > 0,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.TipsAndUpdates, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Olish")
                        }
                    }

                    if (hasRange) {
                        Text(
                            text = "Oraliq: ${formatMoney(minL)} .. ${formatMoney(maxL)} so‘m",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ✅ Big price control
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cs.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalIconButton(
                    onClick = { onAdjustPrice(-step) },
                    enabled = decEnabled,
                    modifier = Modifier.size(52.dp)
                ) { Icon(Icons.Default.Remove, contentDescription = "Kamaytirish") }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatMoney(currentPriceLong),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSurface
                    )
                    Text("so‘m", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }

                FilledTonalIconButton(
                    onClick = { onAdjustPrice(step) },
                    enabled = incEnabled,
                    modifier = Modifier.size(52.dp)
                ) { Icon(Icons.Default.Add, contentDescription = "Oshirish") }
            }
        }

        // ✅ Smart feedback (sodda, keyin o‘zgarishi mumkin)
        if (currentPriceLong > 0 && !sug.isLoading && recL > 0) {
            val low = (recL * 0.7).roundToLong()
            val high = (recL * 1.5).roundToLong()

            val (text, container, content) = when {
                currentPriceLong < low ->
                    Triple("Narx juda past bo‘lishi mumkin. Yo‘l xarajatini hisobga oling.", cs.tertiaryContainer, cs.onTertiaryContainer)
                currentPriceLong > high ->
                    Triple("Narx yuqori bo‘lishi mumkin. Yo‘lovchi topish qiyinlashadi.", cs.errorContainer, cs.onErrorContainer)
                else ->
                    Triple("✅ Narx ridesharing uchun mos ko‘rinadi.", cs.primaryContainer, cs.onPrimaryContainer)
            }

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

        // ✅ Manual input (har doim bor)
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

private fun formatMoney(amount: Long): String {
    if (amount <= 0) return "0"
    return NumberFormat.getNumberInstance(Locale.US).format(amount).replace(",", " ")
}
