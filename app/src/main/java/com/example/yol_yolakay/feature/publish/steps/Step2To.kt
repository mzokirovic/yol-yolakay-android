package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yol_yolakay.feature.publish.LocationModel
import com.example.yol_yolakay.feature.publish.components.LocationSelector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step2To(
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
            text = "Qayerga borasiz?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clickable { isSheetOpen = true },
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
                    // ✅ Borish manzili uchun zamonaviy ikonka
                    Icon(Icons.Outlined.PinDrop, null, tint = cs.onSurface, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Borish manzili", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    Text(
                        text = currentLocation?.name ?: "Manzilni tanlang",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = cs.outlineVariant)
            }
        }

        if (isSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isSheetOpen = false },
                sheetState = sheetState,
                containerColor = cs.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Box(modifier = Modifier.fillMaxHeight(0.85f).padding(horizontal = 16.dp)) {
                    LocationSelector(
                        label = "Qayerga borasiz?",
                        placeholder = "Manzilni kiriting...",
                        currentLocation = currentLocation,
                        onLocationSelected = { loc ->
                            scope.launch {
                                sheetState.hide()
                                isSheetOpen = false
                                onLocationSelected(loc)
                            }
                        },
                        suggestions = suggestions
                    )
                }
            }
        }
    }
}