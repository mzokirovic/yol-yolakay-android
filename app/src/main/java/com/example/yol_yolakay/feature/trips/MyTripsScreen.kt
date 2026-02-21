package com.example.yol_yolakay.feature.trips

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // ❗️MUHIM: remember qilmaymiz — login/token o‘zgarsa ham yangilansin
    val ctx = LocalContext.current
    val clientId = CurrentUser.id(ctx)

    // ✅ my_role kelmasa ham ishlasin + role normalize (trim/lowercase)
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

    // ✅ UX: Driver bo‘sh, Passenger bor bo‘lsa avtomatik o‘sha tabga o‘tib ketadi
    LaunchedEffect(driverTrips.size, passengerTrips.size) {
        if (selectedTab == MyTripsTab.DRIVER && driverTrips.isEmpty() && passengerTrips.isNotEmpty()) {
            selectedTab = MyTripsTab.PASSENGER
        }
    }

    val listForTab = if (selectedTab == MyTripsTab.DRIVER) driverTrips else passengerTrips

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {

        Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
            TabRow(
                selectedTabIndex = if (selectedTab == MyTripsTab.DRIVER) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == MyTripsTab.DRIVER,
                    onClick = { selectedTab = MyTripsTab.DRIVER },
                    text = { Text("Haydovchi", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == MyTripsTab.PASSENGER,
                    onClick = { selectedTab = MyTripsTab.PASSENGER },
                    text = { Text("Yo‘lovchi", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                uiState.isLoading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                uiState.error != null ->
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    }

                listForTab.isEmpty() -> {
                    if (selectedTab == MyTripsTab.DRIVER) {
                        EmptyStateMessage(
                            message = "Hali e’lon qilingan safarlar yo‘q",
                            icon = Icons.Default.AddRoad,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        EmptyStateMessage(
                            message = "Hali bron qilingan safarlar yo‘q",
                            subMessage = "Biror safarga joy so‘rasangiz, shu yerda ko‘rinadi",
                            icon = Icons.Default.HourglassEmpty,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = listForTab,
                        // ✅ key dublikat bo‘lib qolmasin: role ham qo‘shamiz
                        key = { trip -> stableTripKey(trip) }
                    ) { trip ->
                        TripCard(
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

private fun stableTripKey(trip: TripApiModel): String {
    val base = trip.id ?: "${trip.fromCity}|${trip.toCity}|${trip.departureTime}|${trip.price}"
    val role = trip.myRole ?: "?"
    return "$base:$role"
}

// ---------------------------------------------------------
// UI KOMPONENTLAR
// ---------------------------------------------------------

@Composable
private fun TripCard(
    trip: TripApiModel,
    onClick: () -> Unit
) {
    val depTime = remember(trip.departureTime) {
        runCatching {
            OffsetDateTime.parse(trip.departureTime)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull() ?: "--:--"
    }

    // ✅ BlaBlaCar kabi: yetib borish vaqti (durationMin bo‘lsa)
    val arrTime = remember(trip.departureTime, trip.durationMin) {
        val min = trip.durationMin
        if (min == null || min <= 0) return@remember null
        runCatching {
            OffsetDateTime.parse(trip.departureTime)
                .plusMinutes(min.toLong())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull()
    }

    // ✅ kompakt badge: "120 km • ≈ 1 soat 40 daq"
    val routeText = remember(trip.distanceKm, trip.durationMin) {
        val parts = mutableListOf<String>()
        val km = trip.distanceKm
        val min = trip.durationMin
        if (km != null && km > 0) parts += "$km km"
        if (min != null && min > 0) parts += "≈ ${formatDuration(min)}"
        parts.joinToString(" • ")
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top: role chip + price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (trip.myRole == "passenger") "Yo‘lovchi" else "Haydovchi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    "${trip.price.toInt()} so‘m",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // ✅ 2 qator: (dep time + from) / (arr time + to)
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

                // Left: times
                Column(
                    modifier = Modifier.width(56.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(depTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = arrTime ?: "—:—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Middle: line
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.Circle, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Canvas(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .padding(vertical = 2.dp)
                    ) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3f
                        )
                    }
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right: cities
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(trip.fromCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(trip.toCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(10.dp))

            // Footer: seats + route badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Bo'sh joylar: ${trip.availableSeats}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (routeText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AddRoad,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                routeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        if (subMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                subMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(min: Int): String {
    val m = min.coerceAtLeast(0)
    val h = m / 60
    val mm = m % 60
    return if (h <= 0) "${mm} daq" else "${h} soat ${mm} daq"
}