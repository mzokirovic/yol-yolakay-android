package com.example.yol_yolakay.feature.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.network.model.TripApiModel // Sizdagi model importi
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    fun reload() {
        viewModel.searchTrips(from, to, date, passengers)
    }

    LaunchedEffect(from, to, date, passengers) { reload() }

    // Sana formatini chiroyli qilish
    val datePretty = remember(date) {
        runCatching { LocalDate.parse(date) }.getOrNull()
            ?.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("uz")))
            ?.replaceFirstChar { it.uppercase() }
            ?: date
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), // Orqa fon biroz kulrang
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$from → $to", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(datePretty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading && uiState.trips.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    ErrorView(message = uiState.error!!, onRetry = { reload() }, modifier = Modifier.align(Alignment.Center))
                }

                uiState.trips.isEmpty() -> {
                    EmptyStateView(onBack = onBack, modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header info
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${uiState.trips.size} ta safar topildi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "$passengers yo'lovchi",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        items(uiState.trips) { trip ->
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

// -------------------------------------------------------------------------
// YANGI OPTIMALLASHTIRILGAN LIST ITEM (Option B Design)
// -------------------------------------------------------------------------

@Composable
fun TripItemCard(
    trip: com.example.yol_yolakay.core.network.model.TripApiModel, // Model nomini o'zingiznikiga to'g'rilang
    onClick: () -> Unit
) {
    val depTime = remember(trip.departureTime) {
        runCatching { OffsetDateTime.parse(trip.departureTime).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() ?: "--:--"
    }
    // Agar arrivalTime backenddan kelsa o'shani qo'ying, hozircha fake
    val arrTime = "--:--"

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            ) {
                // 1-USTUN: Vaqtlar
                Column(
                    modifier = Modifier.width(50.dp), // Kichikroq joy
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(depTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(arrTime, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 2-USTUN: Grafik (Timeline - TO'G'RI CHIZIQ)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tepa: Dumaloq
                    Icon(
                        imageVector = Icons.Outlined.Circle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    // O'rta: UZUN TO'G'RI CHIZIQ (Solid Line)
                    Canvas(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f) // Bor bo'yni egallaydi
                            .padding(vertical = 2.dp)
                    ) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3f
                            // PathEffect yo'q -> demak uzuq emas, to'g'ri chiziq bo'ladi
                        )
                    }

                    // Past: Lokatsiya belgisi
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3-USTUN: Shaharlar va Narx
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // From
                    Text(trip.fromCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    // To
                    Text(trip.toCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                // 4-USTUN: Narx (O'ng tarafda ajralib turishi kerak)
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "${trip.price.toInt()} so'm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))

            // Footer: Haydovchi va Mashina
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar (Default)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(6.dp))
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(trip.driverName ?: "Haydovchi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFC107))
                        Text(" 4.8", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Mashina info
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(trip.carModel ?: "Mashina", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// YORDAMCHI EKRANLAR
// -------------------------------------------------------------------------

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text("Qayta urinish") }
    }
}

@Composable
fun EmptyStateView(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
        Text("Safarlar topilmadi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Boshqa sana yoki yo‘nalishni tanlab ko‘ring", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack) { Text("Qidiruvga qaytish") }
    }
}