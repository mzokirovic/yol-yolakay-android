package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yol_yolakay.feature.publish.PublishUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ Snackbar spamdan himoya qilish uchun Job
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    val currentPriceLong = uiState.draft.price.filter(Char::isDigit).toLongOrNull() ?: 0L
    val sug = uiState.priceSuggestion
    val recL = sug.recommendedLong

    // ✅ Chegaralarni biroz kengaytirdik (Ride-sharing uchun moslashuvchan)
    // Minimal: Tavsiyaning 30% idan kam bo'lmasin (Masalan: 40k -> 12k)
    // Maksimal: Tavsiyaning 1.6 baravaridan oshmasin (Masalan: 40k -> 64k)
    val minLimit = (recL * 0.3).roundToLong().coerceAtLeast(5000L)
    val maxLimit = (recL * 1.6).roundToLong().coerceAtLeast(15000L)

    val step = 5000

    // Bildirishnoma chiqarish funksiyasi
    val showLimitAlert: (String) -> Unit = { message ->
        snackbarJob?.cancel() // Oldingi bildirishnomani darhol yopish
        snackbarJob = scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Narx belgilang",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            // 1. Smart Pricing Card
            if (sug.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape), color = cs.primary)
            } else if (recL > 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cs.primaryContainer.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.15f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Tavsiya etilgan", style = MaterialTheme.typography.labelMedium, color = cs.primary)
                            Text("${formatMoney(recL)} so'm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { onPriceChange(recL.toString()) }) {
                            Text("Tanlash", fontWeight = FontWeight.Bold, color = cs.primary)
                        }
                    }
                }
            }

            // 2. Price Controls (+/-)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cs.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (currentPriceLong - step >= minLimit) onAdjustPrice(-step)
                            else showLimitAlert("Minimal narx: ${formatMoney(minLimit)} so'm")
                        },
                        modifier = Modifier.size(56.dp).border(1.dp, cs.outlineVariant, CircleShape)
                    ) { Icon(Icons.Outlined.Remove, null) }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatMoney(currentPriceLong), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                        Text("so'm / o'rin", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }

                    IconButton(
                        onClick = {
                            if (currentPriceLong + step <= maxLimit) onAdjustPrice(step)
                            else showLimitAlert("Maksimal narx: ${formatMoney(maxLimit)} so'm")
                        },
                        modifier = Modifier.size(56.dp).background(cs.onSurface, CircleShape)
                    ) { Icon(Icons.Outlined.Add, null, tint = cs.surface) }
                }
            }

            // 3. Smart Feedback
            if (currentPriceLong > 0 && !sug.isLoading && recL > 0) {
                val lowThreshold = (recL * 0.7).roundToLong()
                val highThreshold = (recL * 1.3).roundToLong()

                val (msg, color) = when {
                    currentPriceLong < lowThreshold -> "Narx biroz past." to Color(0xFFF59E0B)
                    currentPriceLong > highThreshold -> "Narx biroz baland." to cs.error
                    else -> "Narx juda mos." to Color(0xFF10B981)
                }

                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 4. Manual Input
            OutlinedTextField(
                value = uiState.draft.price,
                onValueChange = { onPriceChange(it.filter { c -> c.isDigit() }) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Aniq summa kiritish") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        // ✅ Premium Black Snackbar with Auto-Cancel logic
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        ) { data ->
            Surface(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF111827),
                contentColor = Color.White,
                tonalElevation = 6.dp
            ) {
                Text(
                    text = data.visuals.message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun formatMoney(amount: Long): String =
    NumberFormat.getNumberInstance(Locale.US).format(amount).replace(",", " ")