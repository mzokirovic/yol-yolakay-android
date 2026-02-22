package com.example.yol_yolakay.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.network.model.TripApiModel
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Design tokens ───────────────────────────────────────────────────────────
private val CardRadius = 20.dp
private val CardPaddingH = 20.dp
private val CardPaddingV = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    from: String,
    to: String,
    date: String,
    passengers: Int,
    onBack: () -> Unit,
    onTripClick: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val cs = MaterialTheme.colorScheme

    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    fun reload() = viewModel.searchTrips(from, to, date, passengers)
    LaunchedEffect(from, to, date, passengers) { reload() }

    val datePretty = remember(date) {
        runCatching { LocalDate.parse(date) }
            .getOrNull()
            ?.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("uz")))
            ?.replaceFirstChar { it.uppercase() }
            ?: date
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TripListTopBar(
                from = from,
                to = to,
                datePretty = datePretty,
                isScrolled = isScrolled,
                onBack = onBack,
                onRefresh = { reload() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading && uiState.trips.isEmpty() -> LoadingView()
                uiState.error != null -> ErrorView(
                    message = uiState.error!!,
                    onRetry = { reload() },
                    modifier = Modifier.align(Alignment.Center)
                )
                uiState.trips.isEmpty() -> EmptyStateView(
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 20.dp, end = 20.dp,
                            top = 16.dp, bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ResultSummaryRow(
                                count = uiState.trips.size,
                                passengers = passengers
                            )
                        }

                        items(uiState.trips, key = { it.id ?: it.hashCode().toString() }) { trip ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { 24 }
                            ) {
                                TripItemCard(
                                    trip = trip,
                                    onClick = {
                                        val id = trip.id
                                        if (!id.isNullOrBlank()) onTripClick(id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripListTopBar(
    from: String,
    to: String,
    datePretty: String,
    isScrolled: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surface) {
        Column {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = from,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = cs.onSurfaceVariant
                            )
                            Text(
                                text = to,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            datePretty,
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Yangilash")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = cs.surface
                )
            )
            if (isScrolled) {
                HorizontalDivider(color = cs.outlineVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result summary row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultSummaryRow(count: Int, passengers: Int) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$count ta safar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = cs.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = cs.onSurfaceVariant
                )
                Text(
                    "$passengers kishi",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Trip Item Card (To'g'rilangan)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TripItemCard(
    trip: TripApiModel,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val depTime = remember(trip.departureTime) {
        runCatching {
            OffsetDateTime.parse(trip.departureTime)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull() ?: "--:--"
    }

    val arrTime = remember(trip.departureTime, trip.durationMin) {
        val min = trip.durationMin
        if (min == null || min <= 0) return@remember null
        runCatching {
            OffsetDateTime.parse(trip.departureTime)
                .plusMinutes(min.toLong())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull()
    }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(CardRadius),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(CardPaddingH, CardPaddingV)) {

            // ── Main row: Dinamik balandlik (IntrinsicSize.Min) ────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min), // ✅ Qat'iy 64.dp olib tashlandi
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vaqt
                Column(
                    modifier = Modifier.width(48.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = depTime,
                        style = MaterialTheme.typography.titleMedium, // ✅ Shrift ixchamlashtirildi
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Spacer(Modifier.height(18.dp)) // ✅ Ochiq joy (Line uchun)
                    Text(
                        text = arrTime ?: "—:—",
                        style = MaterialTheme.typography.bodyMedium, // ✅ Shrift ixchamlashtirildi
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurfaceVariant
                    )
                }

                // Journey timeline
                JourneyTimeline(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                )

                // Shaharlar
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = trip.fromCity,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = trip.toCity,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Price (Minimalist)
                PriceBadge(price = trip.price)
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(thickness = 1.dp, color = cs.outlineVariant)
            Spacer(Modifier.height(16.dp))

            // ── Footer: Driver info + Car ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(cs.onSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (trip.driverName?.firstOrNull()?.uppercaseChar() ?: 'H').toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.surface
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Ism va Reyting
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.driverName ?: "Haydovchi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                        maxLines = 1, // ✅ Ism 1 qatordan oshmaydi
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFF59E0B)
                        )
                        Text(
                            "4.8",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }

                // Car badge
                if (!trip.carModel.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = cs.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.widthIn(max = 120.dp) // ✅ Mashina nomi juda uzun bo'lsa kesiladi
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = cs.onSurfaceVariant
                            )
                            Text(
                                text = trip.carModel,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Journey timeline: qora nuqta -> chiziq -> ochiq doira
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JourneyTimeline(modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val dotRadius = 4.dp.toPx()
        val lineTop = dotRadius * 2.5f
        val lineBottom = size.height - dotRadius * 2.5f

        // Top dot
        drawCircle(color = onSurface, radius = dotRadius, center = Offset(cx, dotRadius))

        // Line
        drawLine(
            color = outlineVariant,
            start = Offset(cx, lineTop),
            end = Offset(cx, lineBottom),
            strokeWidth = 1.dp.toPx()
        )

        // Bottom dot (hollow)
        drawCircle(
            color = onSurfaceVariant,
            radius = dotRadius,
            center = Offset(cx, size.height - dotRadius),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Price badge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PriceBadge(price: Double) {
    val cs = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "${price.toInt()}",
            style = MaterialTheme.typography.titleMedium, // ✅ Shrift ixchamlashtirildi
            fontWeight = FontWeight.Bold,
            color = cs.onSurface
        )
        Text(
            text = "so'm",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading shimmer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingView() {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(4) { index ->
            val alpha by animateFloatAsState(
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, delayMillis = index * 120, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shimmer_$index"
            )
            ShimmerCard(alpha = alpha)
        }
    }
}

@Composable
private fun ShimmerCard(alpha: Float) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardRadius),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(CardPaddingH, CardPaddingV)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier.width(48.dp).height(22.dp).clip(RoundedCornerShape(6.dp))
                        .background(cs.onSurface.copy(alpha = alpha))
                )
                Box(
                    Modifier.width(2.dp).height(48.dp).clip(RoundedCornerShape(1.dp))
                        .background(cs.onSurface.copy(alpha = alpha * 0.5f))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp))
                            .background(cs.onSurface.copy(alpha = alpha))
                    )
                    Box(
                        Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp))
                            .background(cs.onSurface.copy(alpha = alpha * 0.6f))
                    )
                }
                Box(
                    Modifier.size(52.dp, 44.dp).clip(RoundedCornerShape(12.dp))
                        .background(cs.onSurface.copy(alpha = alpha))
                )
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = cs.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(cs.onSurface.copy(alpha = alpha))
                )
                Box(
                    Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(4.dp))
                        .background(cs.onSurface.copy(alpha = alpha * 0.7f))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error & Empty states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Outlined.DirectionsCar,
            contentDescription = null,
            tint = cs.error,
            modifier = Modifier.size(56.dp)
        )
        Text(
            "Xatolik yuz berdi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
        ) {
            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Qayta urinish")
        }
    }
}

@Composable
fun EmptyStateView(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = cs.outlineVariant,
            modifier = Modifier.size(72.dp)
        )
        Text(
            "Safarlar topilmadi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface
        )
        Text(
            "Boshqa sana yoki yo'nalishni tanlab ko'ring",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, cs.outlineVariant)
        ) {
            Text("Qidiruvga qaytish", color = cs.onSurface)
        }
    }
}