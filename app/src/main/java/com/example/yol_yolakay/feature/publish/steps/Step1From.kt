package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yol_yolakay.feature.publish.LocationModel
import com.example.yol_yolakay.feature.publish.components.LocationSelector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step1From(
    currentLocation: LocationModel?,
    onLocationSelected: (LocationModel) -> Unit,
    suggestions: List<LocationModel>
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Qayerdan yo'lga chiqasiz?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        // ✅ ASOSIY EKRANDAGI KARTA
        LocationPickerTile(
            label = "Jo'nash manzili",
            value = currentLocation?.name ?: "Manzilni kiriting...",
            icon = Icons.Outlined.Explore,
            onClick = { isSheetOpen = true }
        )

        if (isSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isSheetOpen = false },
                sheetState = sheetState,
                containerColor = cs.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                // ✅ MUHIM: BottomSheet ichida faqat qidiruv mantiqi bo'lishi kerak
                Box(modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    LocationSelector(
                        label = "Manzilni qidirish",
                        placeholder = "Shahar yoki tuman nomi...",
                        currentLocation = currentLocation,
                        onLocationSelected = { loc ->
                            scope.launch {
                                sheetState.hide() // Animatsiya bilan yopish
                                isSheetOpen = false // Renderdan olib tashlash
                                onLocationSelected(loc) // ViewModel-ga uzatish
                            }
                        },
                        suggestions = suggestions
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationPickerTile(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f)),
        color = cs.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.onSurface.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = cs.onSurface, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = cs.outlineVariant)
        }
    }
}