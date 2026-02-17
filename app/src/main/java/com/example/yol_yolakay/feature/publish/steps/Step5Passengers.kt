package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Step5Passengers(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val min = 1
    val max = 4 // ✅ FIX

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Yo'lovchilar uchun nechta joy bor?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sedan uchun odatda 1–4 o‘rin",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

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
                StepCounterButton(
                    enabled = count > min,
                    onClick = { onCountChange((count - 1).coerceAtLeast(min)) }
                ) { Icon(Icons.Rounded.Remove, null) }

                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface
                )

                StepCounterButton(
                    enabled = count < max,
                    onClick = { onCountChange((count + 1).coerceAtMost(max)) }
                ) { Icon(Icons.Rounded.Add, null) }
            }
        }
    }
}

@Composable
private fun StepCounterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.35f,
        animationSpec = tween(140),
        label = "btn_alpha"
    )

    Surface(
        modifier = Modifier
            .size(46.dp)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = cs.surface
    ) {
        Box(contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides cs.onSurface.copy(alpha = alpha)) {
                content()
            }
        }
    }
}
