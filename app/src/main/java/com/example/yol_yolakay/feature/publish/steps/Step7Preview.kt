package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.feature.publish.PublishUiState
import java.time.format.DateTimeFormatter

@Composable
fun Step7Preview(
    uiState: PublishUiState,
    onPublish: () -> Unit = {},
    onEditStep: (Int) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Ma'lumotlarni tekshiring",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ TUZATILDI: Barcha ma'lumotlar draft ichidan olinmoqda

        PreviewItem("Qayerdan:", uiState.draft.fromLocation?.name ?: "Tanlanmagan")
        PreviewItem("Qayerga:", uiState.draft.toLocation?.name ?: "Tanlanmagan")

        PreviewItem("Sana:", uiState.draft.date.format(DateTimeFormatter.ISO_DATE))
        PreviewItem("Vaqt:", uiState.draft.time.format(DateTimeFormatter.ofPattern("HH:mm")))
        PreviewItem("Yo'lovchilar:", "${uiState.draft.passengers} kishi")
        PreviewItem("Narx:", "${uiState.draft.price} so'm")
    }
}

@Composable
fun PreviewItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}