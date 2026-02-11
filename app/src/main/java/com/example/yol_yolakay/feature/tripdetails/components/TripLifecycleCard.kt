// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/tripdetails/components/TripLifecycleCard.kt

package com.example.yol_yolakay.feature.tripdetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TripLifecycleCard(
    status: String,
    isBusy: Boolean,
    startEnabled: Boolean,
    finishEnabled: Boolean,
    bookedCount: Int,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    val stRaw = status.trim().lowercase()
    val st = when (stRaw) {
        "inprogress", "in-progress", "started" -> "in_progress"
        "done", "completed" -> "finished"
        else -> stRaw
    }

    val title = when (st) {
        "active" -> "Safar faol"
        "in_progress" -> "Safar boshlangan"
        "finished" -> "Safar tugagan"
        else -> "Safar holati"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Kichik info (MVP)
            if (st == "active") {
                val info = when {
                    bookedCount == 0 -> "Hozircha bron yo‘q. Xohlasangiz baribir safarni boshlashingiz mumkin."
                    else -> "Bronlar: $bookedCount ta"
                }
                Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (!startEnabled) {
                    Text(
                        "Safarni faqat belgilangan vaqt kelganda boshlash mumkin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (st) {
                "active" -> {
                    Button(
                        onClick = onStart,
                        enabled = !isBusy && startEnabled,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isBusy) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                        else Text("Safarni boshlash")
                    }
                }

                "in_progress" -> {
                    Button(
                        onClick = onFinish,
                        enabled = !isBusy && finishEnabled,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isBusy) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                        else Text("Safarni tugatish")
                    }
                }

                "finished" -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Safar tugagan") }
                }

                else -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Holat noma’lum") }
                }
            }
        }
    }
}
