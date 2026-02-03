package com.example.yol_yolakay.feature.trips

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private enum class MyTripsTab { DRIVER, PASSENGER }

@Composable
fun MyTripsScreen(
    onTripClick: (String) -> Unit,
    viewModel: MyTripsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(MyTripsTab.DRIVER) }

    // Mantiq o'zgarmadi: Tab almashganda yuklash
    LaunchedEffect(selectedTab) {
        if (selectedTab == MyTripsTab.DRIVER) {
            viewModel.loadMyTrips()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) // Orqa fonni biroz "yumshatdik"
    ) {

        // TabRow vizual yangilandi (shadow va ranglar), lekin ishlash prinsipi o'sha
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
            when (selectedTab) {
                MyTripsTab.DRIVER -> {
                    when {
                        uiState.isLoading ->
                            CircularProgressIndicator(Modifier.align(Alignment.Center))

                        uiState.error != null ->
                            // Xatolik xabarini chiroyliroq qildik
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                            }

                        uiState.trips.isEmpty() ->
                            // Bo'sh holat (Empty State) vizualizatsiyasi
                            EmptyStateMessage(
                                message = "Hali e’lon qilingan safarlar yo‘q",
                                icon = Icons.Default.AddRoad,
                                modifier = Modifier.align(Alignment.Center)
                            )

                        else -> LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.trips) { trip ->
                                // ESKI TripItem O'RNIGA YANGI VISUAL KARTA
                                // Bu faqat ko'rinishni o'zgartiradi, mantiqni emas.
                                DriverTripCard(
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

                MyTripsTab.PASSENGER -> {
                    // Yo'lovchi qismi mantig'i o'zgarmadi, faqat markazlashtirildi
                    EmptyStateMessage(
                        message = "Yo‘lovchi safarlari (bronlar) tez kunda",
                        subMessage = "Keyingi bosqich: Bookings endpoint + MyBookings list",
                        icon = Icons.Default.HourglassEmpty,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------
// YANGI VIZUAL KOMPONENTLAR (UI Logic Only)
// ---------------------------------------------------------

@Composable
private fun DriverTripCard(
    trip: com.example.yol_yolakay.core.network.model.TripApiModel,
    onClick: () -> Unit
) {
    // Vaqtni formatlash (UI uchun)
    val depTime = remember(trip.departureTime) {
        runCatching {
            OffsetDateTime.parse(trip.departureTime).format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull() ?: "--:--"
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
            // Header: Status va Narx
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status chip (Haydovchi uchun o'z e'loni faol ekanini ko'rsatish)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Faol", // Yoki trip.status (agar backendda bo'lsa)
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

            // Body: Timeline (Vaqt va Shaharlar)
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // 1. Vaqt
                Column(
                    modifier = Modifier.width(50.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(depTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // 2. Chiziq (Timeline) - Option B dizayni
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
                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 3. Shaharlar
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(trip.fromCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp)) // Shaharlar orasini ochish
                    Text(trip.toCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(10.dp))

            // Footer: Mavjud joylar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Bo'sh joylar: ${trip.availableSeats}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            Text(subMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}