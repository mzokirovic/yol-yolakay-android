package com.example.yol_yolakay.feature.tripdetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import com.example.yol_yolakay.feature.inbox.InboxRemoteRepository
import com.example.yol_yolakay.feature.tripdetails.components.SeatMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    tripId: String,
    onBack: () -> Unit,
    onOpenThread: (String) -> Unit,
    onOpenInbox: () -> Unit,
    viewModel: TripDetailsViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val clientId = remember { com.example.yol_yolakay.core.session.CurrentUser.id(ctx) }

    val inboxRepo = remember(clientId) { InboxRemoteRepository(clientId) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val ui by viewModel.uiState.collectAsState()
    LaunchedEffect(tripId) { viewModel.load(tripId) }

    val trip = ui.trip
    val driverId = trip?.driverId
    val isDriver = remember(trip, clientId) { trip?.driverId != null && trip.driverId == clientId }

    // Passenger o‘zi booked bo‘lsa chat ochilsin
    val iBooked = remember(ui.seats, clientId) {
        ui.seats.any { it.status == "booked" && it.holderClientId == clientId }
    }

    val canChatWithDriver = !driverId.isNullOrBlank()
    val canOpenChat = isDriver || (iBooked && canChatWithDriver)

    // --- Pretty date/time ---
    val dep = remember(trip?.departureTime) { trip?.departureTime?.let(::parseDeparture) }
    val datePretty = remember(dep) {
        dep?.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uz")))
            ?.replaceFirstChar { it.uppercase() } ?: "—"
    }
    val timePretty = remember(dep) { dep?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—:—" }

    // --- Seat BottomSheet ---
    SeatActionBottomSheet(
        selectedSeatNo = ui.selectedSeatNo,
        seats = ui.seats,
        isDriver = isDriver,
        clientId = clientId,
        tripId = tripId,
        isBusy = ui.isBooking,
        onDismiss = viewModel::closeSeatSheet,
        onRequest = { seatNo ->
            viewModel.requestSeat(tripId = tripId, seatNo = seatNo, userId = clientId, holderName = null)
        },
        onCancel = { seatNo ->
            viewModel.cancelRequest(tripId = tripId, seatNo = seatNo, userId = clientId)
        },
        onApprove = { seatNo ->
            viewModel.approveSeat(tripId = tripId, seatNo = seatNo, driverId = clientId)
        },
        onReject = { seatNo ->
            viewModel.rejectSeat(tripId = tripId, seatNo = seatNo, driverId = clientId)
        },
        onBlock = { seatNo ->
            viewModel.blockSeat(tripId = tripId, seatNo = seatNo, driverId = clientId)
        },
        onUnblock = { seatNo ->
            viewModel.unblockSeat(tripId = tripId, seatNo = seatNo, driverId = clientId)
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Safar tafsilotlari") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ChatBottomBar(
                enabled = canOpenChat,
                isDriver = isDriver,
                iBooked = iBooked,
                onClick = {
                    if (isDriver) {
                        onOpenInbox()
                        return@ChatBottomBar
                    }

                    scope.launch {
                        if (!iBooked) {
                            snackbarHostState.showSnackbar("Chat faqat joy band qilingandan keyin ochiladi.")
                            return@launch
                        }
                        if (driverId.isNullOrBlank()) {
                            snackbarHostState.showSnackbar("driver_id topilmadi. Trip response’da driver_id bo‘lishi kerak.")
                            return@launch
                        }

                        val tid = trip?.id ?: tripId
                        val threadId = runCatching {
                            inboxRepo.createThread(peerId = driverId, tripId = tid)
                        }.getOrNull()

                        if (!threadId.isNullOrBlank()) onOpenThread(threadId)
                        else snackbarHostState.showSnackbar("Chat ochilmadi. Qayta urinib ko‘ring.")
                    }
                }
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            when {
                ui.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                ui.error != null -> ErrorState(
                    message = ui.error!!,
                    onRetry = { viewModel.load(tripId) },
                    modifier = Modifier.align(Alignment.Center)
                )

                trip == null -> Text("Topilmadi", Modifier.align(Alignment.Center))

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            TripHeaderCard(
                                datePretty = datePretty,
                                timePretty = timePretty,
                                fromCity = trip.fromCity,
                                toCity = trip.toCity,
                                price = trip.price,
                                availableSeats = trip.availableSeats
                            )
                        }

                        item {
                            DriverInfoCard(
                                driverName = trip.driverName ?: "Haydovchi",
                                carModel = trip.carModel ?: "Mashina"
                            )
                        }

                        item {
                            Text(
                                "O‘rindiqlar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        item {
                            SeatSectionCard {
                                SeatMap(
                                    seats = ui.seats,
                                    clientId = clientId,
                                    isDriver = isDriver,
                                    onSeatClick = viewModel::onSeatClick
                                )
                            }
                        }

                        item {
                            AnimatedVisibility(
                                visible = (!isDriver && iBooked && !canChatWithDriver),
                                enter = fadeIn(tween(180)),
                                exit = fadeOut(tween(180))
                            ) {
                                AssistiveInfo(
                                    text = "Chat uchun haydovchi ID kerak (driver_id). Backend trip response’da driver_id chiqishini tekshiring."
                                )
                            }
                        }

                        // Pastki bo‘sh joy: bottomBar ustiga chiqib ketmasin
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }

            // Micro: booking/loading overlay (yengil)
            AnimatedVisibility(
                visible = ui.isBooking,
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(120)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Bajarilmoqda...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripHeaderCard(
    datePretty: String,
    timePretty: String,
    fromCity: String,
    toCity: String,
    price: Double,
    availableSeats: Int
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(datePretty, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(
                "$fromCity  →  $toCity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text("Chiqish: $timePretty") }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${price.toInt()} so‘m") }
                )
            }

            Text(
                "Bo‘sh joylar: $availableSeats",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DriverInfoCard(
    driverName: String,
    carModel: String
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                Text(driverName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(carModel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("4.8  •  Tasdiqlangan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SeatSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun AssistiveInfo(text: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(10.dp))
        Button(onClick = onRetry) { Text("Qayta urinish") }
    }
}

@Composable
private fun ChatBottomBar(
    enabled: Boolean,
    isDriver: Boolean,
    iBooked: Boolean,
    onClick: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    when {
                        isDriver -> "Chatlar (yo‘lovchilar bilan)"
                        iBooked -> "Haydovchiga yozish"
                        else -> "Chat (bron qilingandan keyin)"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeatActionBottomSheet(
    selectedSeatNo: Int?,
    seats: List<com.example.yol_yolakay.core.network.model.SeatApiModel>,
    isDriver: Boolean,
    clientId: String,
    tripId: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onRequest: (Int) -> Unit,
    onCancel: (Int) -> Unit,
    onApprove: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onBlock: (Int) -> Unit,
    onUnblock: (Int) -> Unit
) {
    if (selectedSeatNo == null) return

    val seat = seats.firstOrNull { it.seatNo == selectedSeatNo }
    val status = seat?.status ?: "blocked"
    val lockedByDriver = seat?.lockedByDriver == true
    val isMine = seat?.holderClientId == clientId

    // MVP+ actions
    val canRequest = !isDriver && status == "available"
    val canCancel = !isDriver && status == "pending" && isMine

    val canApprove = isDriver && status == "pending"
    val canReject = isDriver && status == "pending"

    val canBlock = isDriver && status == "available"
    // ✅ driver system blocked’ni ham ochsin (backend ham shuni ko‘tarishi kerak)
    val canUnblock = isDriver && status == "blocked"

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Joy #$selectedSeatNo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            val statusText = when (status) {
                "available" -> "Bo‘sh"
                "booked" -> if (isMine) "Sizniki (Band)" else "Band qilingan"
                "pending" -> if (isMine) "So‘rov yuborilgan (siz)" else "So‘rov yuborilgan"
                "blocked" -> if (lockedByDriver) "Haydovchi yopgan" else "Yopiq"
                else -> status
            }

            AssistChip(onClick = {}, label = { Text("Holat: $statusText") })

            if (status == "booked" || status == "pending") {
                val who = seat?.holderName?.takeIf { it.isNotBlank() } ?: "Noma’lum"
                Text("Egas(i): $who", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(6.dp))

            when {
                canRequest -> {
                    Button(
                        onClick = { onRequest(selectedSeatNo) },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isBusy) "..." else "So‘rov yuborish") }
                }

                canCancel -> {
                    OutlinedButton(
                        onClick = { onCancel(selectedSeatNo) },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isBusy) "..." else "So‘rovni bekor qilish") }
                }

                !isDriver -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when (status) {
                                "pending" -> if (isMine) "So‘rov yuborilgan" else "Boshqa yo‘lovchi so‘rov yuborgan"
                                "booked" -> "Band qilingan"
                                "blocked" -> "Yopiq"
                                else -> "Mavjud emas"
                            }
                        )
                    }
                }
            }

            if (canApprove) {
                Button(
                    onClick = { onApprove(selectedSeatNo) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isBusy) "..." else "Qabul qilish (Approve)") }
            }

            if (canReject) {
                OutlinedButton(
                    onClick = { onReject(selectedSeatNo) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isBusy) "..." else "Rad etish (Reject)") }
            }

            if (canBlock) {
                Button(
                    onClick = { onBlock(selectedSeatNo) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isBusy) "..." else "Seatni yopish (Block)") }
            }

            if (canUnblock) {
                OutlinedButton(
                    onClick = { onUnblock(selectedSeatNo) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isBusy) "..." else "Seatni ochish (Unblock)") }
            }
        }
    }
}

private fun parseDeparture(raw: String): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(raw) }.getOrNull()
