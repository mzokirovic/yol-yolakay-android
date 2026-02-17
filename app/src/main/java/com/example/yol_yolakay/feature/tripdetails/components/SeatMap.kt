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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.core.network.model.SeatApiModel
import com.example.yol_yolakay.ui.theme.Success
import com.example.yol_yolakay.ui.theme.Warning

private enum class SeatUiStatus { AVAILABLE, BOOKED, PENDING, BLOCKED, MINE_BOOKED }

private fun SeatApiModel.toUiStatus(clientId: String, uiLocked: Boolean): SeatUiStatus {
    if (uiLocked) {
        return when (status) {
            "booked" -> if (!holderClientId.isNullOrBlank() && holderClientId == clientId) {
                SeatUiStatus.MINE_BOOKED
            } else SeatUiStatus.BOOKED
            else -> SeatUiStatus.BLOCKED
        }
    }

    return when (status) {
        "available" -> SeatUiStatus.AVAILABLE
        "pending" -> SeatUiStatus.PENDING
        "booked" -> if (!holderClientId.isNullOrBlank() && holderClientId == clientId) {
            SeatUiStatus.MINE_BOOKED
        } else SeatUiStatus.BOOKED
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
        SeatLegendRow(uiLocked = uiLocked)

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
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LegendPill(text = "Bo‘sh", bg = cs.surface, fg = cs.onSurface, bordered = true)
        LegendPill(text = "So‘rov", bg = Warning, fg = Color.White)
        LegendPill(text = "Band", bg = cs.surfaceVariant, fg = cs.onSurfaceVariant)
        LegendPill(text = "Siz", bg = Success, fg = Color.White)
        if (uiLocked) LegendPill(text = "Yopiq", bg = cs.outlineVariant, fg = cs.onSurfaceVariant)
    }
}

@Composable
private fun LegendPill(
    text: String,
    bg: Color,
    fg: Color,
    bordered: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = if (bordered) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = fg
        )
    }
}

@Composable
private fun DriverSeatCard(size: Dp) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = cs.surfaceVariant
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = cs.onSurfaceVariant)
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
    val cs = MaterialTheme.colorScheme
    val ui = seat?.toUiStatus(clientId, uiLocked) ?: SeatUiStatus.BLOCKED
    val lockedByDriver = seat?.lockedByDriver == true

    val tripLocked = uiLocked && ui != SeatUiStatus.BOOKED && ui != SeatUiStatus.MINE_BOOKED

    // ✅ “Eski kabi” semantika
    val targetBg = when (ui) {
        SeatUiStatus.AVAILABLE -> cs.surface
        SeatUiStatus.PENDING -> Warning
        SeatUiStatus.MINE_BOOKED -> Success
        SeatUiStatus.BOOKED -> if (isDriver) Success else cs.surfaceVariant
        SeatUiStatus.BLOCKED -> cs.outlineVariant
    }

    val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(160), label = "seatBg")

    val contentColor = when (ui) {
        SeatUiStatus.AVAILABLE -> cs.onSurface
        SeatUiStatus.PENDING -> Color.White
        SeatUiStatus.MINE_BOOKED -> Color.White
        SeatUiStatus.BOOKED -> if (isDriver) Color.White else cs.onSurfaceVariant
        SeatUiStatus.BLOCKED -> cs.onSurfaceVariant
    }

    val clickable = seat != null
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && clickable) 0.96f else 1f,
        animationSpec = tween(120),
        label = "seatScale"
    )

    val border = when (ui) {
        SeatUiStatus.AVAILABLE -> BorderStroke(1.dp, cs.outlineVariant)
        else -> null
    }

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
        contentColor = contentColor,
        border = border
    ) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {

            if (tripLocked || (ui == SeatUiStatus.BLOCKED && lockedByDriver)) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).size(14.dp),
                    tint = contentColor.copy(alpha = 0.75f)
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
                        modifier = Modifier.align(Alignment.Center),
                        color = contentColor
                    )
                }

                SeatUiStatus.PENDING -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(24.dp),
                        tint = contentColor
                    )
                }

                SeatUiStatus.BLOCKED -> {
                    Text("—", modifier = Modifier.align(Alignment.Center), color = contentColor)
                }

                SeatUiStatus.AVAILABLE -> {
                    Text(
                        seatNo.toString(),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center),
                        color = contentColor
                    )
                }
            }

            if (ui == SeatUiStatus.MINE_BOOKED) {
                Text(
                    "Siz",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}
