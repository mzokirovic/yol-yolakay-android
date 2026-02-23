package com.example.yol_yolakay.feature.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengersPickerField(
    count: Int,
    onCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 4
) {
    val cs = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }

    // ✅ Premium Slim Tile Card
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp) // Kichraytirilgan nafis balandlik
            .clickable { open = true },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f)),
        color = cs.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Ikonka va Faint Blue orqa fon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cs.primary.copy(alpha = 0.08f)), // Faint Premium Blue
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    tint = cs.primary, // Ikonka Blue rangda
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text("Yo'lovchi", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text(
                    text = "$count kishi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
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
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Necha yo‘lovchi?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = cs.onSurface
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Yopish")
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundControlButton(
                enabled = count > min,
                onClick = onMinus
            ) { Icon(Icons.Rounded.Remove, null) }

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )

            RoundControlButton(
                enabled = count < max,
                onClick = onPlus
            ) { Icon(Icons.Rounded.Add, null) }
        }

        Text(
            text = "Kabinada faqat ${max} kishi ketishi mumkin",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
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
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = if (enabled) cs.onSurface.copy(alpha = 0.05f) else cs.outlineVariant.copy(alpha = 0.3f),
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