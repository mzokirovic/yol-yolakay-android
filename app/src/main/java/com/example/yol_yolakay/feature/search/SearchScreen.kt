package com.example.yol_yolakay.feature.search

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.search.components.RegionSelectorField
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── UI tokens ───────────────────────────────────────────────────────────────
private val CardRadius = 28.dp
private val FieldRadius = 20.dp
private val ButtonRadius = 18.dp
private val SectionSpacing = 16.dp
private val FieldPaddingH = 16.dp
private val FieldPaddingV = 14.dp

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onSearchClick: (String, String, String, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        MapPlaceholder()

        SearchCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(),
            uiState = uiState,
            onFromChange = viewModel::onFromLocationChange,
            onToChange = viewModel::onToLocationChange,
            onSwap = viewModel::onSwapLocations,
            onDateChange = viewModel::onDateChange,
            onPassengersChange = viewModel::onPassengersChange,
            onSearchSubmit = {
                onSearchClick(
                    uiState.fromLocation,
                    uiState.toLocation,
                    uiState.date.toString(),
                    uiState.passengers
                )
            }
        )
    }
}

@Composable
private fun SearchCard(
    modifier: Modifier = Modifier,
    uiState: SearchUiState,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onSwap: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onPassengersChange: (Int) -> Unit,
    onSearchSubmit: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val enabled = uiState.fromLocation.isNotBlank() && uiState.toLocation.isNotBlank()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(380)) + slideInVertically(tween(380, easing = EaseOutCubic)) { -36 },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(CardRadius),
                    ambientColor = cs.scrim.copy(alpha = 0.10f),
                    spotColor = cs.scrim.copy(alpha = 0.16f)
                ),
            shape = RoundedCornerShape(CardRadius),
            color = cs.surface,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Header
                CardHeader()

                Spacer(Modifier.height(SectionSpacing))

                // From / To
                LocationBlock(
                    uiState = uiState,
                    onFromChange = onFromChange,
                    onToChange = onToChange,
                    onSwap = onSwap
                )

                Spacer(Modifier.height(SectionSpacing))

                // Quick date chips
                QuickDateRow(onDateChange = onDateChange, selectedDate = uiState.date)

                Spacer(Modifier.height(12.dp))

                // Date + Passengers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DatePickerButton(
                        date = uiState.date,
                        onDateSelected = onDateChange,
                        modifier = Modifier.weight(1f)
                    )
                    PassengerCounter(
                        count = uiState.passengers,
                        onCountChange = onPassengersChange
                    )
                }

                Spacer(Modifier.height(SectionSpacing))

                // Search
                SearchButton(enabled = enabled, onClick = onSearchSubmit)
            }
        }
    }
}

@Composable
private fun CardHeader() {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(cs.primary)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "Safar qidirish",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "Yo'nalish, sana va o'rinlarni belgilang",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationBlock(
    uiState: SearchUiState,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onSwap: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Row(verticalAlignment = Alignment.CenterVertically) {

        // timeline
        LocationTimeline()
        Spacer(Modifier.width(12.dp))

        // container
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FieldRadius))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                cs.surfaceVariant.copy(alpha = 0.55f),
                                cs.surfaceVariant.copy(alpha = 0.35f)
                            )
                        )
                    )
            ) {
                // FROM
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FieldPaddingH, vertical = FieldPaddingV)
                ) {
                    RegionSelectorField(
                        placeholder = "Qayerdan",
                        value = uiState.fromLocation,
                        enableCurrentLocation = true,
                        onSelected = onFromChange
                    )
                }

                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.65f))

                // TO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FieldPaddingH, vertical = FieldPaddingV)
                ) {
                    RegionSelectorField(
                        placeholder = "Qayerga",
                        value = uiState.toLocation,
                        enableCurrentLocation = false,
                        onSelected = onToChange
                    )
                }
            }

            // swap button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 14.dp)
                    .size(40.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(cs.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSwap() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = "Joylarni almashtirish",
                    tint = cs.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickDateRow(
    onDateChange: (LocalDate) -> Unit,
    selectedDate: LocalDate
) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickDateChip(
            label = "Bugun",
            selected = selectedDate == today,
            onClick = { onDateChange(today) }
        )
        QuickDateChip(
            label = "Ertaga",
            selected = selectedDate == tomorrow,
            onClick = { onDateChange(tomorrow) }
        )
    }
}

@Composable
private fun QuickDateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val containerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) cs.primaryContainer else cs.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(180),
        label = "chip_bg"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        animationSpec = tween(180),
        label = "chip_fg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier.height(34.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun DatePickerButton(
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    val dialog = remember(date) {
        DatePickerDialog(
            context,
            { _: DatePicker, y: Int, m: Int, d: Int -> onDateSelected(LocalDate.of(y, m + 1, d)) },
            date.year, date.monthValue - 1, date.dayOfMonth
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }
    }

    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable { dialog.show() },
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
                    .background(cs.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = cs.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Sana", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("d MMM, yyyy", Locale("uz"))),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
            }
        }
    }
}

@Composable
private fun PassengerCounter(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val min = 1
    val max = 4

    Surface(
        modifier = Modifier.height(58.dp),
        shape = RoundedCornerShape(FieldRadius),
        color = cs.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
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
                    tint = cs.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))

            CounterButton(
                enabled = count > min,
                onClick = { onCountChange((count - 1).coerceAtLeast(min)) }
            ) { Icon(Icons.Rounded.Remove, null, modifier = Modifier.size(14.dp)) }

            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
            }

            CounterButton(
                enabled = count < max,
                onClick = { onCountChange((count + 1).coerceAtMost(max)) }
            ) { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(14.dp)) }
        }
    }
}

@Composable
private fun CounterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.35f,
        animationSpec = tween(140),
        label = "counter_alpha"
    )

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(cs.surface.copy(alpha = if (enabled) 1f else 0.55f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides cs.onSurface.copy(alpha = alpha)) {
            content()
        }
    }
}

@Composable
private fun SearchButton(enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.985f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(ButtonRadius),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = cs.primary,
            contentColor = cs.onPrimary,
            disabledContainerColor = cs.surfaceVariant.copy(alpha = 0.75f),
            disabledContentColor = cs.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        )
    ) {
        AnimatedContent(
            targetState = enabled,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "btn_text"
        ) { isEnabled ->
            Text(
                text = if (isEnabled) "Safarlarni qidirish" else "Yo'nalishni to'ldiring",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Decorative components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LocationTimeline() {
    val cs = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 18.dp, bottom = 18.dp)
    ) {
        // top hollow dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(cs.outline.copy(alpha = 0.35f))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(cs.surface)
            )
        }

        Spacer(Modifier.height(6.dp))

        // dashed line
        Canvas(modifier = Modifier.height(36.dp).width(2.dp)) {
            drawLine(
                color = cs.outline.copy(alpha = 0.28f),
                start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        Spacer(Modifier.height(6.dp))

        // bottom filled dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(cs.primary)
        )
    }
}

@Composable
private fun MapPlaceholder() {
    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        cs.background,
                        cs.surfaceVariant.copy(alpha = 0.35f)
                    )
                )
            )
    ) {
        // “map grid” (soft)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 56.dp.toPx()
            val col = cs.outlineVariant.copy(alpha = 0.35f)
            var x = 0f
            while (x < size.width) {
                drawLine(col, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1.dp.toPx())
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(col, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
                y += step
            }
        }

        Text(
            text = "Xarita",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.Center)
                .background(cs.surface.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
