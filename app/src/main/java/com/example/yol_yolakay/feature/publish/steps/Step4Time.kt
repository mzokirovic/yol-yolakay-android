package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step4Time(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val timeText = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Soat nechada?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Taxminiy jo'nash vaqtini belgilang",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        TimePickerTile(
            label = "Ketish vaqti",
            value = timeText,
            onClick = { showPicker = true }
        )
    }

    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = cs.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            CustomTimePickerSheet(
                initialTime = time,
                onDismiss = { showPicker = false },
                onConfirm = {
                    onTimeChange(it)
                    showPicker = false
                }
            )
        }
    }
}

@Composable
private fun TimePickerTile(label: String, value: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.6f)),
        color = cs.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(cs.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AccessTime, null, tint = cs.onSurface)
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = cs.outlineVariant)
        }
    }
}

// ✅ Custom Wheel Picker For Time
@Composable
private fun CustomTimePickerSheet(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val hours = (0..23).toList()
    val minutes = (0..59 step 5).toList()

    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf((Math.round(initialTime.minute / 5.0) * 5).toInt() % 60) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Vaqtni belgilang",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                WheelPicker(
                    items = hours.map { it.toString().padStart(2, '0') },
                    selectedIndex = hours.indexOf(selectedHour).takeIf { it >= 0 } ?: 0,
                    onItemSelected = { selectedHour = hours[it] }
                )
            }
            Text(":", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                WheelPicker(
                    items = minutes.map { it.toString().padStart(2, '0') },
                    selectedIndex = minutes.indexOf(selectedMinute).takeIf { it >= 0 } ?: 0,
                    onItemSelected = { selectedMinute = minutes[it] }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Box(modifier = Modifier.height(24.dp).width(1.dp).background(cs.outlineVariant))
            Spacer(modifier = Modifier.width(24.dp))
            TextButton(onClick = { onConfirm(LocalTime.of(selectedHour, selectedMinute)) }) {
                Text("OK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
            }
        }
    }
}

@Composable
private fun WheelPicker(items: List<String>, selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    val safeIndex = if (selectedIndex in items.indices) selectedIndex else 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeight = 48.dp

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val index = listState.firstVisibleItemIndex
            if (index in items.indices && index != safeIndex) {
                onItemSelected(index)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(itemHeight * 3), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.6f).height(itemHeight).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp)))
        LazyColumn(state = listState, flingBehavior = flingBehavior, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            item { Spacer(modifier = Modifier.height(itemHeight)) }
            items(items.size) { index ->
                val isSelected = index == listState.firstVisibleItemIndex
                Box(modifier = Modifier.height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(text = items[index], style = MaterialTheme.typography.headlineSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.alpha(if (isSelected) 1f else 0.4f))
                }
            }
            item { Spacer(modifier = Modifier.height(itemHeight)) }
        }
    }
}