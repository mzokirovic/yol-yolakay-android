package com.example.yol_yolakay.feature.tripdetails.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.core.network.model.SeatApiModel

private enum class SeatUiStatus { AVAILABLE, BOOKED, PENDING, BLOCKED, MINE_BOOKED }

private fun SeatApiModel.toUiStatus(clientId: String): SeatUiStatus = when (status) {
    "available" -> SeatUiStatus.AVAILABLE
    "pending" -> SeatUiStatus.PENDING
    "booked" -> if (!holderClientId.isNullOrBlank() && holderClientId == clientId) SeatUiStatus.MINE_BOOKED else SeatUiStatus.BOOKED
    "blocked" -> SeatUiStatus.BLOCKED
    else -> SeatUiStatus.BLOCKED
}

@Composable
fun SeatMap(
    seats: List<SeatApiModel>,
    clientId: String,
    isDriver: Boolean,
    onSeatClick: (Int) -> Unit
) {
    val byNo = seats.associateBy { it.seatNo }
    val seatSize = 64.dp
    val gap = 10.dp

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Legend (minimal)
        SeatLegendRow()

        // Front row (centered)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                DriverSeatCard(size = seatSize)
                SeatCard(
                    seatNo = 1,
                    seat = byNo[1],
                    clientId = clientId,
                    isDriver = isDriver,
                    size = seatSize,
                    onSeatClick = onSeatClick
                )
            }
        }

        // Back row (centered)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (n in listOf(2, 3, 4)) {
                    SeatCard(
                        seatNo = n,
                        seat = byNo[n],
                        clientId = clientId,
                        isDriver = isDriver,
                        size = seatSize,
                        onSeatClick = onSeatClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SeatLegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LegendPill(text = "Bo‘sh", kind = "available")
        LegendPill(text = "So‘rov", kind = "pending")
        LegendPill(text = "Band", kind = "booked")
        LegendPill(text = "Mening", kind = "mine")
        LegendPill(text = "Yopiq", kind = "blocked")
    }
}

@Composable
private fun LegendPill(text: String, kind: String) {
    val c = when (kind) {
        "available" -> MaterialTheme.colorScheme.surface
        "pending" -> MaterialTheme.colorScheme.secondaryContainer
        "mine" -> MaterialTheme.colorScheme.primaryContainer
        "booked" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 1.dp,
        color = c,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DriverSeatCard(size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null)
        }
    }
}

@Composable
private fun SeatCard(
    seatNo: Int,
    seat: SeatApiModel?,
    clientId: String,
    isDriver: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onSeatClick: (Int) -> Unit
) {
    val ui = seat?.toUiStatus(clientId) ?: SeatUiStatus.BLOCKED
    val lockedByDriver = seat?.lockedByDriver == true
    val isMine = seat?.holderClientId == clientId

    // “Calm” palette
    val targetBg = when (ui) {
        SeatUiStatus.AVAILABLE -> MaterialTheme.colorScheme.surface
        SeatUiStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
        SeatUiStatus.MINE_BOOKED -> MaterialTheme.colorScheme.primaryContainer
        SeatUiStatus.BOOKED -> MaterialTheme.colorScheme.surfaceVariant
        SeatUiStatus.BLOCKED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    }
    val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(180), label = "seatBg")

    // Clickable rules:
    // Driver: available / blocked / pending / booked ham bosib ko‘rsin (approve/reject/unblock UX uchun)
    // Passenger: available / pending / booked (read-only) bosishi mumkin
    val clickable = if (isDriver) {
        true
    } else {
        ui == SeatUiStatus.AVAILABLE || ui == SeatUiStatus.PENDING || ui == SeatUiStatus.BOOKED || ui == SeatUiStatus.MINE_BOOKED || ui == SeatUiStatus.BLOCKED
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && clickable) 0.96f else 1f,
        animationSpec = tween(120),
        label = "seatScale"
    )

    Surface(
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                enabled = clickable,
                interactionSource = interaction,
                indication = null
            ) { onSeatClick(seatNo) },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (pressed) 6.dp else 2.dp,
        color = bg
    ) {
        Box(Modifier.fillMaxSize().padding(10.dp)) {

            // 🔒 faqat driver bloklagan seatda
            if (ui == SeatUiStatus.BLOCKED && lockedByDriver) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).size(16.dp)
                )
            }

            when (ui) {
                SeatUiStatus.MINE_BOOKED, SeatUiStatus.BOOKED -> {
                    val initial = seat?.holderName
                        ?.trim()
                        ?.firstOrNull()
                        ?.uppercaseChar()
                        ?.toString() ?: "?"

                    Text(
                        initial,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                SeatUiStatus.PENDING -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(20.dp)
                    )
                }

                SeatUiStatus.BLOCKED -> {
                    Text("—", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                SeatUiStatus.AVAILABLE -> {
                    Text(
                        seatNo.toString(),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Small “mine” hint (subtle)
            if (ui == SeatUiStatus.MINE_BOOKED) {
                Text(
                    "siz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}
