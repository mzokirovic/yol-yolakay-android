package com.example.yol_yolakay.feature.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.search.components.PassengersPickerField
import com.example.yol_yolakay.feature.search.components.RegionSelectorField
import com.example.yol_yolakay.feature.search.components.SearchDatePickerField
import java.time.LocalDate

// ─── UI tokens ───────────────────────────────────────────────────────────────
private val FieldRadius = 16.dp
private val ButtonRadius = 16.dp
private val SectionSpacing = 24.dp

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onSearchClick: (String, String, String, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Toza Oq fon
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SearchCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 20.dp)
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
    val isReady = uiState.fromLocation.isNotBlank() && uiState.toLocation.isNotBlank()

    var openFromSheet by remember { mutableStateOf(false) }
    var openToSheet by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(380)) + slideInVertically(tween(380, easing = EaseOutCubic)) { -36 },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Asosiy Sarlavha
            Text(
                text = "Safar qidirish",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = SectionSpacing)
            )

            // ✅ Yagona blok: Manzillar (Qayerdan - Qayerga)
            LocationBlock(
                uiState = uiState,
                onFromChange = onFromChange,
                onToChange = onToChange,
                onSwap = onSwap,
                openFromSheet = openFromSheet,
                onOpenFromSheetChange = { openFromSheet = it },
                openToSheet = openToSheet,
                onOpenToSheetChange = { openToSheet = it }
            )

            Spacer(Modifier.height(16.dp))

            // Sana va Yo'lovchilar qatori
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SearchDatePickerField(
                        date = uiState.date,
                        onDateSelected = onDateChange
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    PassengersPickerField(
                        count = uiState.passengers,
                        onCountChange = onPassengersChange
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Qidirish tugmasi
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

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FieldRadius))
                .border(
                    width = 1.dp,
                    color = cs.outlineVariant,
                    shape = RoundedCornerShape(FieldRadius)
                )
                .background(cs.surface)
        ) {
            // Timeline: Qora nuqta -> Chiziq -> To'rtburchak (KATTALASHTIRILDI)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 22.dp, top = 32.dp, bottom = 32.dp, end = 12.dp)
            ) {
                // Start Nuqtasi (Oldingi 8.dp dan 12.dp ga kattalashtirildi)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(cs.onSurface)
                )

                // Bog'lovchi vertikal chiziq
                Spacer(
                    modifier = Modifier
                        .width(1.5.dp) // Chiziq sal qalinlashdi
                        .height(44.dp) // Uzaytirildi
                        .background(cs.outlineVariant)
                )

                // End Nuqtasi (Kvadrat shakli berildi va kattalashtirildi 12.dp)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .border(2.5.dp, cs.onSurfaceVariant) // Qalinroq ramka
                )
            }

            // Kiritish maydonlari
            Column(modifier = Modifier.weight(1f)) {
                // Qayerdan
                Box(
                    modifier = Modifier.height(72.dp), // Maydon kattalashdi (64.dp dan 72.dp ga)
                    contentAlignment = Alignment.CenterStart
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

                HorizontalDivider(color = cs.outlineVariant)

                // Qayerga
                Box(
                    modifier = Modifier.height(72.dp), // Maydon kattalashdi
                    contentAlignment = Alignment.CenterStart
                ) {
                    RegionSelectorField(
                        placeholder = "Qayerga",
                        value = uiState.toLocation,
                        enableCurrentLocation = true,
                        onSelected = onToChange,
                        openSheet = openToSheet,
                        onOpenSheetChange = onOpenToSheetChange
                    )
                }
            }

            Spacer(modifier = Modifier.width(56.dp))
        }

        // Swap (Almashtirish) tugmasi
        Surface(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(44.dp)
                .zIndex(1f),
            shape = CircleShape,
            color = cs.surface,
            border = BorderStroke(1.dp, cs.outlineVariant),
            shadowElevation = 4.dp,
            onClick = onSwap
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = "Almashtirish",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
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
            .height(56.dp),
        shape = RoundedCornerShape(ButtonRadius),
        enabled = true,
        colors = ButtonDefaults.buttonColors(
            containerColor = cs.primary,
            contentColor = cs.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        AnimatedContent(
            targetState = isReady,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "btn_text"
        ) { _ ->
            Text(
                text = "Qidirish",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}