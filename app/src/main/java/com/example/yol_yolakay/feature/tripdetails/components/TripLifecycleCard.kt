package com.example.yol_yolakay.feature.tripdetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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

    val (title, chipText, chipBg, chipFg) = when (st) {
        "active" -> Quad(
            "Safar faol",
            "ACTIVE",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        "in_progress" -> Quad(
            "Safar boshlangan",
            "IN PROGRESS",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        "finished" -> Quad(
            "Safar tugagan",
            "FINISHED",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> Quad(
            "Safar holati",
            "UNKNOWN",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = chipBg
                ) {
                    Text(
                        chipText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = chipFg
                    )
                }
            }

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
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Bajarilmoqda...")
                        } else {
                            Text("Safarni boshlash", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                "in_progress" -> {
                    Button(
                        onClick = onFinish,
                        enabled = !isBusy && finishEnabled,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Bajarilmoqda...")
                        } else {
                            Text("Safarni tugatish", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                "finished" -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Safar tugagan") }
                }

                else -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Holat noma’lum") }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
