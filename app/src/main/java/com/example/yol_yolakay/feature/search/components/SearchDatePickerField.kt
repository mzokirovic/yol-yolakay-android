package com.example.yol_yolakay.feature.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDatePickerField(
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }

    val dateText = remember(date) {
        date.format(DateTimeFormatter.ofPattern("d MMM, EEE", Locale("uz", "UZ")))
    }

    // ✅ Premium Slim Tile Card
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { open = true },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f)),
        color = cs.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cs.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Sana",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // ✅ iOS/Uber style Custom Bottom Sheet Date Picker
    if (open) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
            containerColor = cs.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            CustomDatePickerSheet(
                initialDate = date,
                onDismiss = { open = false },
                onConfirm = { selectedDate ->
                    onDateSelected(selectedDate)
                    open = false
                }
            )
        }
    }
}

@Composable
private fun CustomDatePickerSheet(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val currentYear = LocalDate.now().year
    val years = (currentYear..(currentYear + 2)).toList()
    val months = (1..12).toList()
    val monthNames = listOf("YAN", "FEV", "MART", "APR", "MAY", "IYUN", "IYUL", "AVG", "SENT", "OKT", "NOY", "DEK")

    var selectedYear by remember { mutableStateOf(initialDate.year) }
    var selectedMonth by remember { mutableStateOf(initialDate.monthValue) }
    var selectedDay by remember { mutableStateOf(initialDate.dayOfMonth) }

    val maxDays = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
    if (selectedDay > maxDays) selectedDay = maxDays
    val days = (1..maxDays).toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${monthNames[selectedMonth - 1].lowercase().replaceFirstChar { it.uppercase() }}, $selectedYear",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WheelPicker(
                    items = days.map { it.toString().padStart(2, '0') },
                    selectedIndex = days.indexOf(selectedDay),
                    onItemSelected = { selectedDay = days[it] }
                )
            }
            Box(modifier = Modifier.weight(1.5f)) {
                WheelPicker(
                    items = monthNames,
                    selectedIndex = months.indexOf(selectedMonth),
                    onItemSelected = { selectedMonth = months[it] }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                WheelPicker(
                    items = years.map { it.toString() },
                    selectedIndex = years.indexOf(selectedYear),
                    onItemSelected = { selectedYear = years[it] }
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
            TextButton(onClick = { onConfirm(LocalDate.of(selectedYear, selectedMonth, selectedDay)) }) {
                Text("OK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
            }
        }
    }
}

@Composable
private fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
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

    Box(
        modifier = Modifier.fillMaxWidth().height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(itemHeight)) }
            items(items.size) { index ->
                val isSelected = index == listState.firstVisibleItemIndex
                Box(
                    modifier = Modifier.height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alpha(if (isSelected) 1f else 0.4f)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(itemHeight)) }
        }
    }
}