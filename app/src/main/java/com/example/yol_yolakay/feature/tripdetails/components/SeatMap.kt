// /home/mzokirovic/AndroidStudioProjects/YolYolakay/app/src/main/java/com/example/yol_yolakay/feature/tripdetails/components/SeatMap.kt

package com.example.yol_yolakay.feature.tripdetails.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.core.network.model.SeatApiModel

// UI colors
private val ColorAvailable = Color(0xFFFFFFFF)
private val ColorBookedMine = Color(0xFF4CAF50)
private val ColorBookedOthers = Color(0xFFE0E0E0)
private val ColorPending = Color(0xFFFF9800)
private val ColorBlocked = Color(0xFFBDBDBD)

private enum class SeatUiStatus { AVAILABLE, BOOKED, PENDING, BLOCKED, MINE_BOOKED }

private fun SeatApiModel.toUiStatus(clientId: String, uiLocked: Boolean): SeatUiStatus {
    // ✅ Trip “boshlangan” bo‘lsa: booked’dan boshqa hammasi yopiq ko‘rinsin (MVP)
    if (uiLocked) {
        return when (status) {
            "booked" -> if (!holderClientId.isNullOrBlank() && holderClientId == clientId) {
                SeatUiStatus.MINE_BOOKED
            } else {
                SeatUiStatus.BOOKED
            }
            else -> SeatUiStatus.BLOCKED
        }
    }

    return when (status) {
        "available" -> SeatUiStatus.AVAILABLE
        "pending" -> SeatUiStatus.PENDING
        "booked" -> if (!holderClientId.isNullOrBlank() && holderClientId == clientId) {
            SeatUiStatus.MINE_BOOKED
        } else {
            SeatUiStatus.BOOKED
        }
        "blocked" -> SeatUiStatus.BLOCKED
        else -> SeatUiStatus.BLOCKED
    }
}

@Composable
fun SeatMap(
    seats: List<SeatApiModel>,
    clientId: String,
    isDriver: Boolean,
    uiLocked: Boolean = false,
    onSeatClick: (Int) -> Unit
) {
    val byNo = seats.associateBy { it.seatNo }
    val seatSize = 64.dp
    val gap = 10.dp

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ✅ Legend uiLocked bo‘lsa “Yopiq” ham ko‘rsatiladi
        SeatLegendRow(uiLocked = uiLocked)

        // Old qator (Haydovchi + 1-joy)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                DriverSeatCard(size = seatSize)

                SeatCard(
                    seatNo = 1,
                    seat = byNo[1],
                    clientId = clientId,
                    isDriver = isDriver,
                    uiLocked = uiLocked,
                    size = seatSize,
                    onSeatClick = onSeatClick
                )
            }
        }

        // Orqa qator (2,3,4)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (n in listOf(2, 3, 4)) {
                    SeatCard(
                        seatNo = n,
                        seat = byNo[n],
                        clientId = clientId,
                        isDriver = isDriver,
                        uiLocked = uiLocked,
                        size = seatSize,
                        onSeatClick = onSeatClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SeatLegendRow(uiLocked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LegendPill(text = "Bo‘sh", color = ColorAvailable, isBordered = true)
        LegendPill(text = "So‘rov", color = ColorPending)
        LegendPill(text = "Band", color = ColorBookedOthers)
        LegendPill(text = "Siz", color = ColorBookedMine, contentColor = Color.White)

        // ✅ MVP lock holatida: qo‘shimcha izoh
        if (uiLocked) {
            LegendPill(text = "Yopiq", color = ColorBlocked, contentColor = Color.White)
        }
    }
}

@Composable
private fun LegendPill(
    text: String,
    color: Color,
    contentColor: Color = Color.Black,
    isBordered: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color,
        border = if (isBordered) BorderStroke(1.dp, Color.LightGray) else null,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = contentColor
        )
    }
}

@Composable
private fun DriverSeatCard(size: Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = ColorBlocked
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun SeatCard(
    seatNo: Int,
    seat: SeatApiModel?,
    clientId: String,
    isDriver: Boolean,
    uiLocked: Boolean,
    size: Dp,
    onSeatClick: (Int) -> Unit
) {
    val ui = seat?.toUiStatus(clientId, uiLocked) ?: SeatUiStatus.BLOCKED
    val lockedByDriver = seat?.lockedByDriver == true

    // uiLocked bo‘lsa, BOOKED/MINE_BOOKED dan tashqari hammasi “tripLocked” ko‘rinadi
    val tripLocked = uiLocked && ui != SeatUiStatus.BOOKED && ui != SeatUiStatus.MINE_BOOKED

    val targetBg = when (ui) {
        SeatUiStatus.AVAILABLE -> ColorAvailable
        SeatUiStatus.PENDING -> ColorPending
        SeatUiStatus.MINE_BOOKED -> ColorBookedMine
        SeatUiStatus.BOOKED -> if (isDriver) ColorBookedMine else ColorBookedOthers
        SeatUiStatus.BLOCKED -> ColorBlocked
    }

    val bg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(180),
        label = "seatBg"
    )

    val contentColor = when {
        ui == SeatUiStatus.PENDING -> Color.White
        ui == SeatUiStatus.MINE_BOOKED -> Color.White
        ui == SeatUiStatus.BOOKED && isDriver -> Color.White
        ui == SeatUiStatus.BLOCKED -> Color.White
        else -> Color.Black
    }

    // ✅ CLICK RULES:
    // - uiLocked bo‘lsa: driver bosishi mumkin; yo‘lovchi faqat o‘z joyini (MINE_BOOKED) bosadi (read-only sheet)
    // - normal: driver hammasini bosadi; yo‘lovchi available/pending/mine_booked bosadi
    val clickable = seat != null

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
        shadowElevation = if (pressed) 6.dp else 2.dp,
        color = bg,
        contentColor = contentColor
    ) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {

            // ✅ Lock icon:
            // - tripLocked bo‘lsa ham lock ko‘rsatamiz
            // - yoki seat locked_by_driver bo‘lsa ham ko‘rsatamiz
            if (tripLocked || (ui == SeatUiStatus.BLOCKED && lockedByDriver)) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).size(14.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }

            when (ui) {
                SeatUiStatus.MINE_BOOKED, SeatUiStatus.BOOKED -> {
                    val displayName =
                        seat?.holderProfile?.displayName?.trim()?.takeIf { it.isNotBlank() }
                            ?: seat?.holderName?.trim()?.takeIf { it.isNotBlank() }

                    val initial = displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"


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
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                }

                SeatUiStatus.BLOCKED -> {
                    Text("—", modifier = Modifier.align(Alignment.Center))
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

            if (ui == SeatUiStatus.MINE_BOOKED) {
                Text(
                    "Siz",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
