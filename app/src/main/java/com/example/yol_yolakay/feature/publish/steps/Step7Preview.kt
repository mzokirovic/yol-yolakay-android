package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
fun Step7Preview(
    uiState: PublishUiState,
    onEditStep: (PublishStep) -> Unit
) {
    val scroll = rememberScrollState()

    val dateText = uiState.draft.date.format(DateTimeFormatter.ISO_DATE)
    val timeText = uiState.draft.time.format(DateTimeFormatter.ofPattern("HH:mm"))

    val priceLong = uiState.draft.price.filter(Char::isDigit).toLongOrNull() ?: 0L
    val priceText = if (priceLong > 0) "${formatMoney(priceLong)} so‘m" else "Kiritilmagan"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ma'lumotlarni tekshiring",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        PreviewRow("Qayerdan", uiState.draft.fromLocation?.name ?: "Tanlanmagan") {
            onEditStep(PublishStep.FROM)
        }
        PreviewRow("Qayerga", uiState.draft.toLocation?.name ?: "Tanlanmagan") {
            onEditStep(PublishStep.TO)
        }
        PreviewRow("Sana", dateText) { onEditStep(PublishStep.DATE) }
        PreviewRow("Vaqt", timeText) { onEditStep(PublishStep.TIME) }
        PreviewRow("Yo'lovchilar", "${uiState.draft.passengers} o‘rin") { onEditStep(PublishStep.PASSENGERS) }
        PreviewRow("Narx", priceText) { onEditStep(PublishStep.PRICE) }

        Spacer(Modifier.height(20.dp)) // ✅ pastda nafas
    }
}

@Composable
private fun PreviewRow(
    label: String,
    value: String,
    onEdit: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.Edit, contentDescription = "Tahrirlash", tint = cs.primary)
        }
    }
}

private fun formatMoney(amount: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount).replace(",", " ")
}
