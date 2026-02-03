package com.example.yol_yolakay.feature.tripdetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.inbox.InboxRemoteRepository
import com.example.yol_yolakay.feature.tripdetails.components.SeatMap
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

    // --- Formatlash ---
    val dep = remember(trip?.departureTime) { trip?.departureTime?.let(::parseDeparture) }
    val datePretty = remember(dep) {
        dep?.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("uz")))
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
        onRequest = { seatNo -> viewModel.requestSeat(tripId, seatNo, clientId, null) },
        onCancel = { seatNo -> viewModel.cancelRequest(tripId, seatNo, clientId) },
        onApprove = { seatNo -> viewModel.approveSeat(tripId, seatNo, clientId) },
        onReject = { seatNo -> viewModel.rejectSeat(tripId, seatNo, clientId) },
        onBlock = { seatNo -> viewModel.blockSeat(tripId, seatNo, clientId) },
        onUnblock = { seatNo -> viewModel.unblockSeat(tripId, seatNo, clientId) }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Safar tafsilotlari", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
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
                            snackbarHostState.showSnackbar("Xatolik: Haydovchi ma'lumotlari to'liq emas.")
                            return@launch
                        }
                        val tid = trip?.id ?: tripId
                        val threadId = runCatching { inboxRepo.createThread(peerId = driverId, tripId = tid) }.getOrNull()
                        if (!threadId.isNullOrBlank()) onOpenThread(threadId)
                        else snackbarHostState.showSnackbar("Chat ochilmadi. Qayta urinib ko‘ring.")
                    }
                }
            )
        }
    ) { pad ->
        Box(modifier = Modifier.padding(pad).fillMaxSize()) {
            when {
                ui.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> ErrorState(
                    message = ui.error!!,
                    onRetry = { viewModel.load(tripId) },
                    modifier = Modifier.align(Alignment.Center)
                )
                trip == null -> Text("Ma'lumot topilmadi", Modifier.align(Alignment.Center))
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. YANGI TIMELINE HEADER KARTASI
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

                        // 2. Haydovchi haqida
                        item {
                            DriverInfoCard(
                                driverName = trip.driverName ?: "Haydovchi",
                                carModel = trip.carModel ?: "Mashina modeli yo'q"
                            )
                        }

                        // 3. O'rindiqlar sxemasi
                        item {
                            Text(
                                "Joyni tanlang",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            SeatSectionCard {
                                SeatMap(
                                    seats = ui.seats,
                                    clientId = clientId,
                                    isDriver = isDriver,
                                    onSeatClick = viewModel::onSeatClick
                                )
                            }
                        }

                        // 4. Qo'shimcha info
                        item {
                            AnimatedVisibility(
                                visible = (!isDriver && iBooked && !canChatWithDriver),
                            ) {
                                AssistiveInfo(text = "Diqqat: Haydovchi bilan bog'lanishda muammo bo'lsa, iltimos qo'llab-quvvatlash xizmatiga yozing.")
                            }
                        }
                    }
                }
            }

            // Loading Overlay
            if (ui.isBooking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Row(
                            Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(16.dp))
                            Text("Bajarilmoqda...", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// --- YANGILANGAN HEADER VA TIMELINE ---

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
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header: Sana va Narx
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = datePretty,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = "${price.toInt()} so‘m",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(24.dp))

            // TIMELINE: Vertikal chiziqli marshrut
            TripTimeline(
                departureTime = timePretty,
                departureCity = fromCity,
                // Joy nomlari backendda bo'lmasa, shartli nomlar:
                departurePlace = "Belgilangan joy",
                arrivalTime = "—:—",
                arrivalCity = toCity,
                arrivalPlace = "Shahar markazi"
            )

            Spacer(Modifier.height(24.dp))

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            // Footer: Joylar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "$availableSeats ta joy qoldi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TripTimeline(
    departureTime: String,
    departureCity: String,
    departurePlace: String,
    arrivalTime: String,
    arrivalCity: String,
    arrivalPlace: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Balandlikni ichki elementlarga moslash
    ) {
        // 1-USTUN: VAQT
        Column(
            modifier = Modifier.width(60.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = departureTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = arrivalTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2-USTUN: CHIZIQ (Timeline)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Circle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Uzuq-uzuq chiziq (Dashed Line)
            Canvas(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        // 3-USTUN: SHAHARLAR
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = departureCity,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = departurePlace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Column {
                Text(
                    text = arrivalCity,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = arrivalPlace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ------------------------------------

@Composable
private fun DriverInfoCard(
    driverName: String,
    carModel: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = driverName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = driverName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = carModel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "4.8",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    "Tasdiqlangan",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun SeatSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun ChatBottomBar(
    enabled: Boolean,
    isDriver: Boolean,
    iBooked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .safeDrawingPadding()
        ) {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        isDriver -> "Chatlar (Yo‘lovchilar)"
                        iBooked -> "Haydovchiga yozish"
                        else -> "Chat (Joy band qilgach)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AssistiveInfo(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Qayta urinish") }
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

    val canRequest = !isDriver && status == "available"
    val canCancel = !isDriver && status == "pending" && isMine
    val canApprove = isDriver && status == "pending"
    val canReject = isDriver && status == "pending"
    val canBlock = isDriver && status == "available"
    val canUnblock = isDriver && status == "blocked"

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Joy №$selectedSeatNo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                val (statusText, statusColor) = when (status) {
                    "available" -> "Bo‘sh" to Color(0xFF4CAF50)
                    "booked" -> (if (isMine) "Sizniki" else "Band") to Color.Gray
                    "pending" -> "Kutilmoqda" to Color(0xFFFF9800)
                    "blocked" -> "Yopiq" to Color.Red
                    else -> status to Color.Gray
                }

                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (status == "booked" || status == "pending") {
                val who = seat?.holderName?.takeIf { it.isNotBlank() } ?: "Noma’lum yo'lovchi"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(who, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
            }

            when {
                canRequest -> PrimaryButton(text = "So‘rov yuborish", isBusy = isBusy) { onRequest(selectedSeatNo) }
                canCancel -> SecondaryButton(text = "Bekor qilish", isBusy = isBusy) { onCancel(selectedSeatNo) }
                canApprove || canReject -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { SecondaryButton(text = "Rad etish", isBusy = isBusy) { onReject(selectedSeatNo) } }
                        Box(Modifier.weight(1f)) { PrimaryButton(text = "Qabul qilish", isBusy = isBusy) { onApprove(selectedSeatNo) } }
                    }
                }
                canBlock -> SecondaryButton(text = "Joyni yopish (Block)", isBusy = isBusy) { onBlock(selectedSeatNo) }
                canUnblock -> SecondaryButton(text = "Joyni ochish (Unblock)", isBusy = isBusy) { onUnblock(selectedSeatNo) }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, isBusy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isBusy) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White) else Text(text)
    }
}

@Composable
private fun SecondaryButton(text: String, isBusy: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isBusy) CircularProgressIndicator(Modifier.size(24.dp)) else Text(text)
    }
}

private fun parseDeparture(raw: String): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(raw) }.getOrNull()