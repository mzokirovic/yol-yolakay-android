package com.example.yol_yolakay.feature.tripdetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.network.model.SeatApiModel
import com.example.yol_yolakay.core.session.CurrentUser
import com.example.yol_yolakay.feature.inbox.InboxRemoteRepository
import com.example.yol_yolakay.feature.tripdetails.components.SeatMap
import com.example.yol_yolakay.feature.tripdetails.components.TripLifecycleCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    tripId: String,
    onBack: () -> Unit,
    onOpenThread: (String) -> Unit,
    onOpenInbox: () -> Unit,
    viewModel: TripDetailsViewModel = viewModel(
        factory = TripDetailsViewModel.factory(LocalContext.current)
    )
) {
    val ctx = LocalContext.current
    val clientId = CurrentUser.id(ctx)

    val inboxRepo = remember { InboxRemoteRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var startConfirmOpen by remember { mutableStateOf(false) }
    val ui by viewModel.uiState.collectAsState()

    LaunchedEffect(tripId) {
        viewModel.load(tripId)
    }

    val trip = ui.trip

    // --- MANTIQIY O'ZGARUVCHILAR ---
    val isDriver = remember(trip, clientId) { trip?.driverId != null && trip?.driverId == clientId }

    val iBooked = remember(ui.seats, clientId) {
        ui.seats.any { it.status == "booked" && it.holderClientId == clientId }
    }

    val canChatWithDriver = remember(trip) { !trip?.driverId.isNullOrBlank() }
    val canOpenChat = isDriver || (iBooked && canChatWithDriver)

    // --- FORMATLASH ---
    val dep = remember(trip?.departureTime) { trip?.departureTime?.let(::parseDeparture) }
    val datePretty = remember(dep) {
        dep?.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("uz")))
            ?.replaceFirstChar { it.uppercase() } ?: "—"
    }
    val timePretty = remember(dep) { dep?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—:—" }

    // --- ROUTE META (ETA) ---
    val distanceKm = remember(trip?.distanceKm) { trip?.distanceKm?.takeIf { it > 0 } }
    val durationMin = remember(trip?.durationMin) { trip?.durationMin?.takeIf { it > 0 } }

    val arrivalTimePretty = remember(dep, durationMin) {
        if (dep != null && durationMin != null) {
            dep.plusMinutes(durationMin.toLong())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } else "—:—"
    }

    // --- STATUS + TIME (UI LOCK) ---
    val tripStatus = remember(trip?.status) { normalizeStatus(trip?.status) }

    val now by produceState(initialValue = Instant.now(), key1 = dep, key2 = tripStatus) {
        while (true) {
            value = Instant.now()
            delay(30_000)
        }
    }

    val timeLocked = remember(dep, now) {
        dep?.toInstant()?.isBefore(now) ?: false
    }

    val uiLocked = remember(tripStatus, timeLocked) {
        (tripStatus != "active") || timeLocked
    }

    val bookedCount by remember(ui.seats) {
        derivedStateOf { ui.seats.count { it.status == "booked" } }
    }

    val canStartNow = remember(dep, now) {
        dep?.toInstant()?.let { !it.isAfter(now) } ?: false
    }

    val startEnabled = remember(tripStatus, canStartNow) {
        tripStatus == "active" && canStartNow
    }

    val finishEnabled = remember(tripStatus) {
        tripStatus == "in_progress"
    }

    // --- BOTTOM SHEET ---
    SeatActionBottomSheet(
        selectedSeatNo = ui.selectedSeatNo,
        seats = ui.seats,
        isDriver = isDriver,
        clientId = clientId,
        isBusy = ui.isBooking,
        uiLocked = uiLocked,
        onInfo = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
        onDismiss = viewModel::closeSeatSheet,
        onRequest = { seatNo -> viewModel.requestSeat(tripId, seatNo) },
        onCancel = { seatNo -> viewModel.cancelRequest(tripId, seatNo) },
        onApprove = { seatNo -> viewModel.approveSeat(tripId, seatNo) },
        onReject = { seatNo -> viewModel.rejectSeat(tripId, seatNo) },
        onBlock = { seatNo -> viewModel.blockSeat(tripId, seatNo) },
        onUnblock = { seatNo -> viewModel.unblockSeat(tripId, seatNo) }
    )

    // START CONFIRM
    if (startConfirmOpen) {
        AlertDialog(
            onDismissRequest = { startConfirmOpen = false },
            title = { Text("Yo‘lovchi yo‘q") },
            text = { Text("Hozircha hech kim bron qilmagan. Baribir safarni boshlaysizmi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        startConfirmOpen = false
                        viewModel.startTrip(tripId)
                    }
                ) { Text("Boshlash") }
            },
            dismissButton = {
                TextButton(onClick = { startConfirmOpen = false }) { Text("Bekor") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // Clean background
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Safar tafsilotlari", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background // Flat top bar
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load(tripId) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Yangilash")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ChatBottomBar(
                enabled = canOpenChat,
                isDriver = isDriver,
                onClick = {
                    if (isDriver) {
                        onOpenInbox()
                    } else {
                        scope.launch {
                            val did = trip?.driverId
                            if (did != null) {
                                val tid = runCatching { inboxRepo.createThread(did, tripId) }.getOrNull()
                                if (tid != null) onOpenThread(tid)
                                else snackbarHostState.showSnackbar("Chat ochilmadi")
                            }
                        }
                    }
                }
            )
        }
    ) { pad ->
        Box(modifier = Modifier.padding(pad).fillMaxSize()) {
            when {
                // ✅ 1) Faqat birinchi yuklash: trip yo'q paytida full-screen
                ui.isLoading && ui.trip == null -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                // ✅ 2) Trip yo'q + error: full-screen error
                ui.trip == null && ui.error != null -> {
                    ErrorState(
                        message = ui.error!!,
                        onRetry = { viewModel.load(tripId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // ✅ 3) Trip bor: doim contentni ko'rsatamiz (refresh bo'lsa ham)
                ui.trip != null -> {
                    val trip = ui.trip!!

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            TripHeaderCard(
                                datePretty = datePretty,
                                timePretty = timePretty,
                                arrivalTimePretty = arrivalTimePretty,
                                fromCity = trip.fromCity,
                                toCity = trip.toCity,
                                price = trip.price,
                                availableSeats = trip.availableSeats,
                                distanceKm = distanceKm,
                                durationMin = durationMin
                            )
                        }

                        item {
                            DriverInfoCard(
                                driverName = trip.driverName ?: "Haydovchi",
                                carModel = trip.carModel ?: "Noma'lum"
                            )
                        }

                        if (isDriver) {
                            item {
                                TripLifecycleCard(
                                    status = tripStatus,
                                    isBusy = ui.isLifecycleBusy,
                                    startEnabled = startEnabled,
                                    finishEnabled = finishEnabled,
                                    bookedCount = bookedCount,
                                    onStart = {
                                        if (bookedCount == 0) startConfirmOpen = true
                                        else viewModel.startTrip(tripId)
                                    },
                                    onFinish = { viewModel.finishTrip(tripId) }
                                )
                            }
                        }

                        item {
                            SeatSectionCard {
                                Text(
                                    text = "O'rindiqlar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                SeatMap(
                                    seats = ui.seats,
                                    clientId = clientId,
                                    isDriver = isDriver,
                                    uiLocked = uiLocked,
                                    onSeatClick = viewModel::onSeatClick
                                )
                            }
                        }
                    }

                    // ✅ Refresh xatoligi: full-screen emas, snackbar
                    LaunchedEffect(ui.error) {
                        ui.error?.let { snackbarHostState.showSnackbar(it) }
                    }
                }

                else -> {
                    Text(
                        "Ma'lumot topilmadi",
                        Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Oyna bloklanganda chiqadigan loading indicator
            if (ui.isBooking || ui.isLifecycleBusy) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(16.dp))
                            Text("Bajarilmoqda...", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// UI COMPONENTS (Premium Clean)
// ---------------------------------------------------------

@Composable
private fun TripHeaderCard(
    datePretty: String,
    timePretty: String,
    arrivalTimePretty: String,
    fromCity: String,
    toCity: String,
    price: Double,
    availableSeats: Int,
    distanceKm: Int? = null,
    durationMin: Int? = null
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant), // Soya o'rniga yupqa border
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = datePretty,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )

                // Minimalist Price Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = price.toInt().toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "so‘m",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Premium Timeline
            TripTimeline(
                departureTime = timePretty,
                departureCity = fromCity,
                arrivalTime = arrivalTimePretty,
                arrivalCity = toCity
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(thickness = 1.dp, color = cs.outlineVariant)
            Spacer(Modifier.height(16.dp))

            // Footer (Seats and Route Meta)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Person,
                        null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$availableSeats ta joy qoldi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurfaceVariant
                    )
                }

                if (distanceKm != null || durationMin != null) {
                    val meta = listOfNotNull(
                        distanceKm?.let { "$it km" },
                        durationMin?.let { formatDurationUz(it) }
                    ).joinToString(" • ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDurationUz(min: Int): String {
    val m = min.coerceAtLeast(0)
    if (m < 60) return "$m daq"
    val h = m / 60
    val r = m % 60
    return if (r == 0) "${h} soat" else "${h} soat ${r} daq"
}

@Composable
fun TripTimeline(
    departureTime: String,
    departureCity: String,
    arrivalTime: String,
    arrivalCity: String
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min), // Dinamik balandlik
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(52.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(departureTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
            Spacer(Modifier.height(24.dp)) // Ochiq joy
            Text(arrivalTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant)
        }

        // Timeline dots and line
        Canvas(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
                .padding(vertical = 6.dp)
        ) {
            val cx = size.width / 2f
            val dotRadius = 4.dp.toPx()
            val lineTop = dotRadius * 2.5f
            val lineBottom = size.height - dotRadius * 2.5f

            drawCircle(color = cs.onSurface, radius = dotRadius, center = Offset(cx, dotRadius))
            drawLine(
                color = cs.outlineVariant,
                start = Offset(cx, lineTop),
                end = Offset(cx, lineBottom),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(
                color = cs.onSurfaceVariant,
                radius = dotRadius,
                center = Offset(cx, size.height - dotRadius),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(departureCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(24.dp))
            Text(arrivalCity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DriverInfoCard(driverName: String, carModel: String) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cs.onSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    driverName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = cs.surface
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(driverName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.DirectionsCar, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text(carModel, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("4.8", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SeatSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun ChatBottomBar(enabled: Boolean, isDriver: Boolean, onClick: () -> Unit) {
    if (!enabled && !isDriver) return
    val cs = MaterialTheme.colorScheme
    Surface(
        color = cs.surface,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f)) // Tepadagi chegara uchun
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).safeDrawingPadding()) {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.onSurface, contentColor = cs.surface) // Premium black/white button
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    if (isDriver) "Chatlar" else "Haydovchiga yozish",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Outlined.DirectionsCar,
            null,
            modifier = Modifier.size(56.dp),
            tint = cs.error
        )
        Spacer(Modifier.height(16.dp))
        Text("Xatolik yuz berdi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = cs.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp),
            border = BorderStroke(1.dp, cs.outlineVariant)
        ) {
            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp), tint = cs.onSurface)
            Spacer(Modifier.width(8.dp))
            Text("Qayta urinish", fontWeight = FontWeight.SemiBold, color = cs.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeatActionBottomSheet(
    selectedSeatNo: Int?,
    seats: List<SeatApiModel>,
    isDriver: Boolean,
    clientId: String,
    isBusy: Boolean,
    uiLocked: Boolean,
    onInfo: (String) -> Unit,
    onDismiss: () -> Unit,
    onRequest: (Int) -> Unit,
    onCancel: (Int) -> Unit,
    onApprove: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onBlock: (Int) -> Unit,
    onUnblock: (Int) -> Unit
) {
    if (selectedSeatNo == null) return
    val seat = seats.find { it.seatNo == selectedSeatNo } ?: return

    val rawStatus = seat.status
    val status = if (uiLocked && rawStatus != "booked") "blocked" else rawStatus
    val isMine = seat.holderClientId == clientId
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Joy №$selectedSeatNo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )

                val (txt, bg, fg) = when (status) {
                    "available" -> Triple("Bo‘sh", cs.primaryContainer, cs.onPrimaryContainer)
                    "booked" -> Triple(if (isMine) "Sizniki" else "Band", cs.surfaceVariant, cs.onSurfaceVariant)
                    "pending" -> Triple("Kutilmoqda", cs.secondaryContainer, cs.onSecondaryContainer)
                    "blocked" -> Triple("Yopiq", cs.outlineVariant, cs.onSurfaceVariant)
                    else -> Triple(status, cs.surfaceVariant, cs.onSurfaceVariant)
                }

                Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        txt,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = fg,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (status == "booked" || status == "pending") {
                val who =
                    seat.holderProfile?.displayName?.takeIf { !it.isNullOrBlank() }
                        ?: seat.holderName?.takeIf { it.isNotBlank() }
                        ?: "Noma’lum yo'lovchi"

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(who, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = cs.outlineVariant)
            }

            if (isDriver) {
                when (status) {
                    "pending" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) {
                                SecondaryButton("Rad etish", isBusy) { onReject(selectedSeatNo) }
                            }
                            Box(Modifier.weight(1f)) {
                                PrimaryButton("Tasdiqlash", isBusy) { onApprove(selectedSeatNo) }
                            }
                        }
                    }
                    "available" -> SecondaryButton("Joyni yopish", isBusy) { onBlock(selectedSeatNo) }
                    "blocked" -> {
                        if (uiLocked) {
                            SecondaryButton("Joyni ochish", isBusy = false) {
                                onInfo("Safar boshlangan. Hozircha joylarni ochib bo‘lmaydi.")
                                onDismiss()
                            }
                        } else {
                            SecondaryButton("Joyni ochish", isBusy) { onUnblock(selectedSeatNo) }
                        }
                    }
                }
            } else {
                if (status == "available") {
                    PrimaryButton("Band qilish", isBusy) { onRequest(selectedSeatNo) }
                } else if (status == "pending" && isMine) {
                    SecondaryButton("Bekor qilish", isBusy) { onCancel(selectedSeatNo) }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, isBusy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (isBusy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        else Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun SecondaryButton(text: String, isBusy: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (isBusy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurface)
        else Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun parseDeparture(raw: String): ZonedDateTime? =
    runCatching {
        OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault())
    }.getOrNull()

private fun normalizeStatus(raw: String?): String {
    val s = (raw ?: "active").trim().lowercase()
    return when (s) {
        "inprogress", "in-progress", "started" -> "in_progress"
        "done", "completed" -> "finished"
        else -> s
    }
}