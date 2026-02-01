package com.example.yol_yolakay.feature.tripdetails

import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.tripdetails.components.SeatMap
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    tripId: String,
    onBack: () -> Unit,
    viewModel: TripDetailsViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val clientId = remember {
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "device"
    }

    val ui by viewModel.uiState.collectAsState()

    LaunchedEffect(tripId) { viewModel.load(tripId) }

    val trip = ui.trip
    val isDriver = remember(trip) {
        // ✅ MVP: auth yo‘q. Hozircha test driverName bilan aniqlaymiz.
        trip?.driverName == "Test Haydovchi"
    }

    // ✅ Chat: passenger faqat o‘zi book qilgandan keyin, driver doim.
    val iBooked = remember(ui.seats, clientId) {
        ui.seats.any { it.status == "booked" && it.holderClientId == clientId }
    }
    val canOpenChat = isDriver || iBooked

    // ---- Seat BottomSheet ----
    val selectedSeat = ui.selectedSeatNo
    if (selectedSeat != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.closeSeatSheet() }) {
            val seat = ui.seats.firstOrNull { it.seatNo == selectedSeat }
            val status = seat?.status ?: "blocked"
            val lockedByDriver = seat?.lockedByDriver == true

            val canBook = !isDriver && status == "available"
            val canBlock = isDriver && status == "available"
            val canUnblock = isDriver && status == "blocked" && lockedByDriver

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Joy #$selectedSeat", style = MaterialTheme.typography.titleLarge)

                val statusText = when (status) {
                    "available" -> "Bo‘sh"
                    "booked" -> "Band qilingan"
                    "pending" -> "So‘rov yuborilgan"
                    "blocked" -> if (lockedByDriver) "Haydovchi bloklagan" else "Yopiq"
                    else -> status
                }
                Text("Holat: $statusText", style = MaterialTheme.typography.bodyMedium)

                if (status == "booked" || status == "pending") {
                    val who = seat?.holderName?.takeIf { it.isNotBlank() } ?: "Noma’lum"
                    Text("Egas(i): $who", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(8.dp))

                // Passenger action
                if (canBook) {
                    Button(
                        onClick = { viewModel.bookSelectedSeat(tripId, selectedSeat, clientId) },
                        enabled = !ui.isBooking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (ui.isBooking) "..." else "Joyni band qilish")
                    }
                } else if (!isDriver) {
                    OutlinedButton(
                        onClick = { /* no-op */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bu joy hozir band/ochiq emas")
                    }
                }

                // Driver actions
                if (canBlock) {
                    Button(
                        onClick = { viewModel.blockSeat(tripId, selectedSeat) },
                        enabled = !ui.isBooking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (ui.isBooking) "..." else "Seatni blok qilish")
                    }
                }

                if (canUnblock) {
                    OutlinedButton(
                        onClick = { viewModel.unblockSeat(tripId, selectedSeat) },
                        enabled = !ui.isBooking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (ui.isBooking) "..." else "Seatni ochish")
                    }
                }

                Spacer(Modifier.height(6.dp))
            }
        }
    }

    // ---- UI helpers: date/time ----
    val dep = remember(trip?.departureTime) { trip?.departureTime?.let(::parseDeparture) }
    val datePretty = remember(dep) {
        dep?.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uz")))
            ?.replaceFirstChar { it.uppercase() }
            ?: "—"
    }
    val timePretty = remember(dep) {
        dep?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—:—"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safar tafsilotlari") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                ui.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                ui.error != null -> Text(
                    ui.error!!,
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )

                trip == null -> Text("Topilmadi", Modifier.align(Alignment.Center))

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // --- Route/Time card ---
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(datePretty, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${trip.fromCity}  →  ${trip.toCity}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Chiqish: $timePretty", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${trip.price.toInt()} so‘m / 1 kishi",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    "Bo‘sh joylar: ${trip.availableSeats}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // --- Driver card ---
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        trip.driverName ?: "Haydovchi",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        trip.carModel ?: "Mashina",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("4.8  •  Tasdiqlangan", style = MaterialTheme.typography.bodySmall)
                                        // rating/verified MVP: placeholder
                                    }
                                }

                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }

                        // --- Seats ---
                        Text("O‘rindiqlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                        SeatMap(
                            seats = ui.seats,
                            clientId = clientId,
                            isDriver = isDriver,
                            onSeatClick = viewModel::onSeatClick
                        )

                        // --- Chat button ---
                        OutlinedButton(
                            onClick = { /* TODO: Inbox/Chat feature */ },
                            enabled = canOpenChat,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when {
                                    isDriver -> "Chatlar (yo‘lovchilar bilan)"
                                    iBooked -> "Haydovchiga yozish"
                                    else -> "Chat (bron qilingandan keyin)"
                                }
                            )
                        }

                        // kichik izoh (MVP)
                        if (!canOpenChat && !isDriver) {
                            Text(
                                "Chat faqat siz joyni band qilganingizdan keyin ochiladi.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseDeparture(raw: String): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(raw) }.getOrNull()
