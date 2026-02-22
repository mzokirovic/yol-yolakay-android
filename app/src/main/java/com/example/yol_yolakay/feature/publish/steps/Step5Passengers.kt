package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Step5Passengers(count: Int, onCountChange: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("Bo'sh o'rinlar soni", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (count > 1) onCountChange(count - 1) },
                modifier = Modifier.size(64.dp).border(1.5.dp, cs.outlineVariant, CircleShape)
            ) { Icon(Icons.Outlined.Remove, null, modifier = Modifier.size(32.dp)) }

            Text(text = count.toString(), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)

            IconButton(
                onClick = { if (count < 4) onCountChange(count + 1) },
                modifier = Modifier.size(64.dp).background(cs.onSurface, CircleShape)
            ) { Icon(Icons.Outlined.Add, null, tint = cs.surface, modifier = Modifier.size(32.dp)) }
        }

        Surface(color = cs.surfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp)) {
            Text(
                "Haydovchi o'rni hisobga olinmagan. Yo'lovchilar uchun ochiq joylarni belgilang.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}