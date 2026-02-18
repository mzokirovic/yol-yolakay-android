package com.example.yol_yolakay.feature.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.search.components.PassengersPickerField
import com.example.yol_yolakay.feature.search.components.RegionSelectorField
import com.example.yol_yolakay.feature.search.components.SearchDatePickerField
import java.time.LocalDate

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
    val isReady = uiState.fromLocation.isNotBlank() && uiState.toLocation.isNotBlank()

    // Search bosilganda qaysi field bo‘sh bo‘lsa — o‘sha sheet ochiladi
    var openFromSheet by remember { mutableStateOf(false) }
    var openToSheet by remember { mutableStateOf(false) }

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

                CardHeader()

                Spacer(Modifier.height(SectionSpacing))

                LocationBlock(
                    uiState = uiState,
                    onFromChange = onFromChange,
                    onToChange = onToChange,
                    onSwap = onSwap,
                    openFromSheet = openFromSheet,
                    onOpenFromSheetChange = { openFromSheet = it },
                    openToSheet = openToSheet,
                    onOpenToSheetChange = { openToSheet = it },
                )

                Spacer(Modifier.height(SectionSpacing))

                QuickDateRow(
                    onDateChange = onDateChange,
                    selectedDate = uiState.date
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ Calendar field (dialog + date picker)
                    // ❗ fieldRadius paramni bermaymiz — type mismatch bo‘lmasin
                    SearchDatePickerField(
                        date = uiState.date,
                        onDateSelected = onDateChange,
                        modifier = Modifier.weight(1f)
                    )

                    PassengersPickerField(
                        count = uiState.passengers,
                        onCountChange = onPassengersChange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(SectionSpacing))

                SearchButton(
                    isReady = isReady,
                    onClick = {
                        when {
                            uiState.fromLocation.isBlank() -> {
                                openFromSheet = true
                                openToSheet = false
                            }

                            uiState.toLocation.isBlank() -> {
                                openToSheet = true
                                openFromSheet = false
                            }

                            else -> onSearchSubmit()
                        }
                    }
                )
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
    onSwap: () -> Unit,
    openFromSheet: Boolean,
    onOpenFromSheetChange: (Boolean) -> Unit,
    openToSheet: Boolean,
    onOpenToSheetChange: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    Row(verticalAlignment = Alignment.CenterVertically) {

        LocationTimeline()
        Spacer(Modifier.width(12.dp))

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
                        onSelected = onFromChange,
                        openSheet = openFromSheet,
                        onOpenSheetChange = onOpenFromSheetChange
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
                        enableCurrentLocation = true, // ✅ qayerga ichida ham GPS bo‘lsin
                        onSelected = onToChange,
                        openSheet = openToSheet,
                        onOpenSheetChange = onOpenToSheetChange
                    )
                }
            }

            // Modern swap button
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 12.dp)
                    .size(42.dp),
                shape = CircleShape,
                color = cs.primaryContainer,
                tonalElevation = 2.dp,
                shadowElevation = 6.dp,
                onClick = onSwap
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = "Joylarni almashtirish",
                        tint = cs.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
private fun SearchButton(
    isReady: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(ButtonRadius),
        enabled = true,
        colors = ButtonDefaults.buttonColors(
            containerColor = cs.primary,
            contentColor = cs.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        )
    ) {
        AnimatedContent(
            targetState = isReady,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "btn_text"
        ) { _ ->
            Text(
                text = "Qidirish",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
private fun LocationTimeline() {
    val cs = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 18.dp, bottom = 18.dp)
    ) {
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
                        cs.surface,
                        cs.surfaceVariant.copy(alpha = 0.25f),
                        cs.surface.copy(alpha = 0.90f)
                    )
                )
            )
    )
}
