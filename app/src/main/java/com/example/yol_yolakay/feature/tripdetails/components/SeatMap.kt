package com.example.yol_yolakay.feature.tripdetails.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DriverSeatCard()

            SeatCard(
                seatNo = 1,
                seat = byNo[1],
                clientId = clientId,
                isDriver = isDriver,
                onSeatClick = onSeatClick
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (n in listOf(2, 3, 4)) {
                SeatCard(
                    seatNo = n,
                    seat = byNo[n],
                    clientId = clientId,
                    isDriver = isDriver,
                    onSeatClick = onSeatClick
                )
            }
        }
    }
}

@Composable
private fun DriverSeatCard() {
    Card(
        modifier = Modifier.size(86.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
    onSeatClick: (Int) -> Unit
) {
    val ui = seat?.toUiStatus(clientId) ?: SeatUiStatus.BLOCKED
    val lockedByDriver = seat?.lockedByDriver == true

    val bg = when (ui) {
        SeatUiStatus.AVAILABLE -> MaterialTheme.colorScheme.surface
        SeatUiStatus.BOOKED -> MaterialTheme.colorScheme.errorContainer
        SeatUiStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
        SeatUiStatus.MINE_BOOKED -> MaterialTheme.colorScheme.tertiaryContainer
        SeatUiStatus.BLOCKED -> MaterialTheme.colorScheme.surfaceVariant
    }

    // Passenger: faqat available/booked/pending/mine bosishi mumkin
    // Driver: available + (blocked AND lockedByDriver) bosishi mumkin (unblock uchun)
    val clickable = if (isDriver) {
        ui == SeatUiStatus.AVAILABLE || (ui == SeatUiStatus.BLOCKED && lockedByDriver)
    } else {
        ui == SeatUiStatus.AVAILABLE || ui == SeatUiStatus.BOOKED || ui == SeatUiStatus.MINE_BOOKED || ui == SeatUiStatus.PENDING
    }

    Card(
        onClick = { onSeatClick(seatNo) },
        enabled = clickable,
        modifier = Modifier.size(86.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(10.dp)) {

            // 🔒 faqat driver blok qilgan seatda
            if (ui == SeatUiStatus.BLOCKED && lockedByDriver) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).size(16.dp)
                )
            }

            when (ui) {
                SeatUiStatus.BOOKED, SeatUiStatus.MINE_BOOKED -> {
                    val initial = seat?.holderName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(
                        initial,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
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
                    Text("—", modifier = Modifier.align(Alignment.Center))
                }

                SeatUiStatus.AVAILABLE -> {
                    Text(
                        seatNo.toString(),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
