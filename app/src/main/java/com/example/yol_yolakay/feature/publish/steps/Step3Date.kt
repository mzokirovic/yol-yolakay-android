package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step3Date(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val uz = Locale("uz")

    val dateText = date
        .format(DateTimeFormatter.ofPattern("d MMMM, yyyy", uz))
        .replaceFirstChar { it.uppercase(uz) }

    var open by remember { mutableStateOf(false) }

    // LocalDate -> millis
    val initialMillis = remember(date) {
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
        // minDate cheklovi Material3 DatePicker’da to‘g‘ridan-to‘g‘ri yo‘q.
        // Agar kerak bo‘lsa, confirm paytida “bugundan oldin bo‘lsa qabul qilmaymiz”.
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Qaysi kuni yo'lga chiqasiz?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sana keyinroq ham tahrirlanadi",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        PickerTile(
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            label = "Sana",
            value = dateText,
            onClick = { open = true }
        )
    }

    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis
                        if (millis != null) {
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            // ✅ minimum date: bugundan oldin bo‘lsa qabul qilmaymiz
                            if (!picked.isBefore(LocalDate.now())) {
                                onDateChange(picked)
                            }
                        }
                        open = false
                    }
                ) { Text("Tanlash") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Bekor") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun PickerTile(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = cs.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cs.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides cs.onPrimaryContainer) {
                        icon()
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = cs.onSurfaceVariant
            )
        }
    }
}
