package com.example.yol_yolakay.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val FieldRadius = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengersPickerField(
    count: Int,
    onCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 4,
    label: String = "Yo‘lovchilar"
) {
    val cs = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable { open = true },
        shape = RoundedCornerShape(FieldRadius),
        color = cs.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cs.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = cs.onSecondaryContainer
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text(
                    text = "$count kishi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
            }
        }
    }

    if (open) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
            containerColor = cs.surface
        ) {
            PassengersSheetContent(
                count = count,
                min = min,
                max = max,
                onMinus = { onCountChange((count - 1).coerceAtLeast(min)) },
                onPlus = { onCountChange((count + 1).coerceAtMost(max)) },
                onClose = { open = false }
            )
        }
    }
}

@Composable
private fun PassengersSheetContent(
    count: Int,
    min: Int,
    max: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onClose: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Necha yo‘lovchi?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = cs.onSurface
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Yopish")
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundControlButton(
                enabled = count > min,
                onClick = onMinus
            ) { Icon(Icons.Rounded.Remove, null) }

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )

            RoundControlButton(
                enabled = count < max,
                onClick = onPlus
            ) { Icon(Icons.Rounded.Add, null) }
        }

        Text(
            text = "1–4 kishi",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun RoundControlButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = cs.surfaceVariant.copy(alpha = if (enabled) 0.55f else 0.30f),
        tonalElevation = 0.dp,
        onClick = onClick,
        enabled = enabled
    ) {
        Box(contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides cs.onSurface.copy(alpha = if (enabled) 1f else 0.4f)) {
                content()
            }
        }
    }
}
