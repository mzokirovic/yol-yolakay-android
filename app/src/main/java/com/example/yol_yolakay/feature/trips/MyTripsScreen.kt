package com.example.yol_yolakay.feature.trips

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.network.model.TripApiModel
import com.example.yol_yolakay.core.session.CurrentUser
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private enum class MyTripsTab { DRIVER, PASSENGER }

@Composable
fun MyTripsScreen(
    onTripClick: (String) -> Unit,
    viewModel: MyTripsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(MyTripsTab.DRIVER) }

    LaunchedEffect(Unit) {
        viewModel.loadMyTrips()
    }

    val ctx = LocalContext.current
    val clientId = CurrentUser.id(ctx)

    // ✅ MANTIQ TO'LIQ SAQLANDI: Rollarni aniqlash va normallashtirish
    val normalizedTrips = remember(uiState.trips, clientId) {
        uiState.trips.map { t ->
            val raw = t.myRole?.trim()?.lowercase()
            val fixedRole = when (raw) {
                "driver", "passenger" -> raw
                else -> {
                    val did = t.driverId
                    if (!did.isNullOrBlank() && did == clientId) "driver" else "passenger"
                }
            }
            t.copy(myRole = fixedRole)
        }
    }

    val driverTrips = remember(normalizedTrips) { normalizedTrips.filter { it.myRole == "driver" } }
    val passengerTrips = remember(normalizedTrips) { normalizedTrips.filter { it.myRole == "passenger" } }

    LaunchedEffect(driverTrips.size, passengerTrips.size) {
        if (selectedTab == MyTripsTab.DRIVER && driverTrips.isEmpty() && passengerTrips.isNotEmpty()) {
            selectedTab = MyTripsTab.PASSENGER
        }
    }

    val listForTab = if (selectedTab == MyTripsTab.DRIVER) driverTrips else passengerTrips
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Surface(
                color = cs.surface,
                border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 12.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Safarlarim",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(16.dp))

                    SegmentedControl(
                        selectedIndex = if (selectedTab == MyTripsTab.DRIVER) 0 else 1,
                        onSelectionChange = {
                            selectedTab = if (it == 0) MyTripsTab.DRIVER else MyTripsTab.PASSENGER
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = cs.onSurface
                )

                uiState.error != null -> Text(
                    text = uiState.error!!,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = cs.error,
                    textAlign = TextAlign.Center
                )

                listForTab.isEmpty() -> {
                    EmptyStateMessage(
                        message = if (selectedTab == MyTripsTab.DRIVER) "Hali safar qo'shmagansiz" else "Bron qilingan safarlar yo'q",
                        subMessage = if (selectedTab == MyTripsTab.DRIVER) "Safar e'lon qilish bo'limidan foydalaning" else "Biror safarga joy so'rasangiz, shu yerda chiqadi",
                        icon = if (selectedTab == MyTripsTab.DRIVER) Icons.Outlined.DirectionsCar else Icons.Outlined.ChairAlt,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = listForTab, key = { it.id + (it.myRole ?: "") }) { trip ->
                        TripCard(trip = trip, onClick = { trip.id?.let { onTripClick(it) } })
                    }
                }
            }
        }
    }
}

@Composable
private fun TripCard(
    trip: TripApiModel,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    // ✅ MANTIQ TO'LIQ SAQLANDI: Vaqtni hisoblash
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

    val routeText = remember(trip.distanceKm, trip.durationMin) {
        val parts = mutableListOf<String>()
        val km = trip.distanceKm
        val min = trip.durationMin
        if (km != null && km > 0) parts += "$km km"
        if (min != null && min > 0) parts += "≈ ${formatDuration(min)}"
        parts.joinToString(" • ")
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = cs.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (trip.myRole == "passenger") "Yo‘lovchi" else "Haydovchi",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${trip.price.toInt().toString().reversed().chunked(3).joinToString(" ").reversed()} so‘m",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface
                )
            }

            Spacer(Modifier.height(20.dp))

            // ✅ Timeline (Vaqtlar va Manzillar) - arrTime to'liq tiklandi
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier.width(48.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = depTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    if (arrTime != null) {
                        Text(text = arrTime, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 12.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(cs.onSurface))
                    Spacer(modifier = Modifier.width(1.dp).weight(1f).padding(vertical = 4.dp).background(cs.outlineVariant))
                    Box(modifier = Modifier.size(8.dp).border(1.5.dp, cs.onSurfaceVariant, CircleShape))
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = trip.fromCity, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(text = trip.toCity, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(thickness = 1.dp, color = cs.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Bo'sh joy: ${trip.availableSeats}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (routeText.isNotBlank()) {
                    Text(
                        text = routeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Qolgan yordamchi funksiyalar (SegmentedControl, EmptyStateMessage, formatDuration, stableTripKey)
// o'zgarishsiz qoladi...

@Composable
private fun SegmentedControl(
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val items = listOf("Haydovchi", "Yo'lovchi")

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cs.onSurface.copy(alpha = 0.05f))
            .padding(4.dp)
    ) {
        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedIndex == 0) 0.dp else 136.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "selector"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(cs.surface)
                .zIndex(1f)
        )

        Row(modifier = Modifier.fillMaxSize().zIndex(2f)) {
            items.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val textColor by animateColorAsState(
                    if (isSelected) cs.onSurface else cs.onSurfaceVariant,
                    label = "color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectionChange(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(
    message: String,
    subMessage: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = cs.outlineVariant, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text(text = message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
        if (subMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(text = subMessage, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

private fun formatDuration(min: Int): String {
    val m = min.coerceAtLeast(0)
    val h = m / 60
    val mm = m % 60
    return if (h <= 0) "${mm} daq" else "${h} soat ${mm} daq"
}