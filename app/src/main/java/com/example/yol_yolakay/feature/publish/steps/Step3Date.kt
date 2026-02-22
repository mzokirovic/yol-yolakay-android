package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step3Date(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    val uz = Locale("uz")
    val dateText = date.format(DateTimeFormatter.ofPattern("d MMMM, yyyy", uz)).replaceFirstChar { it.uppercase(uz) }
    var open by remember { mutableStateOf(false) }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Qaysi kuni yo'lga chiqasiz?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("Sana keyinroq ham tahrirlanishi mumkin", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(12.dp))

        PickerTile(
            label = "Tanlangan sana",
            value = dateText,
            onClick = { open = true }
        )
    }

    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = {
                pickerState.selectedDateMillis?.let {
                    val picked = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    if (!picked.isBefore(LocalDate.now())) onDateChange(picked)
                }
                open = false
            }) { Text("Tanlash") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Bekor") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun PickerTile(label: String, value: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.6f)),
        color = cs.surface
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(cs.surfaceVariant.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = cs.onSurface)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = cs.outlineVariant)
        }
    }
}