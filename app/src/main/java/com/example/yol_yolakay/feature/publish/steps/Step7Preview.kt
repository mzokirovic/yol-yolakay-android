package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.feature.publish.PublishStep
import com.example.yol_yolakay.feature.publish.PublishUiState
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Step7Preview(uiState: PublishUiState, onEditStep: (PublishStep) -> Unit) {
    val draft = uiState.draft
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ma'lumotlarni tekshiring", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))

        PreviewRow("Yo'nalish", "${draft.fromLocation?.name} → ${draft.toLocation?.name}") { onEditStep(PublishStep.FROM) }
        PreviewRow("Sana va vaqt", "${draft.date}, ${draft.time}") { onEditStep(PublishStep.DATE) }
        PreviewRow("O'rinlar soni", "${draft.passengers} kishi") { onEditStep(PublishStep.PASSENGERS) }
        PreviewRow("Narx (bir kishi uchun)", "${draft.price} so'm") { onEditStep(PublishStep.PRICE) }
    }
}

@Composable
private fun PreviewRow(label: String, value: String, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() }.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Outlined.Edit, null, tint = cs.outlineVariant, modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
}