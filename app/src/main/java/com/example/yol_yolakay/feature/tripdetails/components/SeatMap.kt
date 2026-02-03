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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.core.network.model.SeatApiModel

// Ranglar statusga qarab o'zgaradi
private val ColorAvailable = Color(0xFFFFFFFF) // Oq
private val ColorBookedMine = Color(0xFF4CAF50) // Yashil (Mening joyim)
private val ColorBookedOthers = Color(0xFFE0E0E0) // Och Kulrang (Birovnikini kulrang qilamiz)
private val ColorPending = Color(0xFFFF9800) // Orange (Kutilmoqda)
private val ColorBlocked = Color(0xFFBDBDBD) // To'qroq kulrang (Yopiq)

// Statuslar o'zgarmadi
private enum class SeatUiStatus { AVAILABLE, BOOKED, PENDING, BLOCKED, MINE_BOOKED }

private fun SeatApiModel.toUiStatus(clientId: String): SeatUiStatus = when (status) {
    "available" -> SeatUiStatus.AVAILABLE
    "pending" -> SeatUiStatus.PENDING
    // Agar booked bo'lsa va egasi men bo'lsam -> MINE_BOOKED, aks holda shunchaki BOOKED
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

        // Legend (Izohlar)
        SeatLegendRow()

        // Old qator (Haydovchi va 1-joy)
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

        // Orqa qator (2, 3, 4 - joylar)
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
        LegendPill(text = "Bo‘sh", color = ColorAvailable, isBordered = true)
        LegendPill(text = "So‘rov", color = ColorPending)
        LegendPill(text = "Band", color = ColorBookedOthers)
        LegendPill(text = "Siz", color = ColorBookedMine, contentColor = Color.White)
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
        shape = RoundedCornerShape(50), // To'liq aylana
        color = color,
        border = if (isBordered) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray) else null,
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
private fun DriverSeatCard(size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = ColorBlocked // Haydovchi o'rindig'i ham statik kulrang
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
    size: androidx.compose.ui.unit.Dp,
    onSeatClick: (Int) -> Unit
) {
    val ui = seat?.toUiStatus(clientId) ?: SeatUiStatus.BLOCKED
    val lockedByDriver = seat?.lockedByDriver == true

    // --- ASOSIY O'ZGARISH SHU YERDA ---
    val targetBg = when (ui) {
        SeatUiStatus.AVAILABLE -> ColorAvailable

        // PENDING: Har doim Orange bo'ladi
        SeatUiStatus.PENDING -> ColorPending

        // MINE_BOOKED: Men band qilganman -> Yashil
        SeatUiStatus.MINE_BOOKED -> ColorBookedMine

        // BOOKED (Birov band qilgan):
        // Agar men Haydovchi bo'lsam -> Yashil ko'raman (puli to'langan joy)
        // Agar men yo'lovchi bo'lsam -> Kulrang ko'raman (band joy)
        SeatUiStatus.BOOKED -> if (isDriver) ColorBookedMine else ColorBookedOthers

        // BLOCKED: Yopiq joy
        SeatUiStatus.BLOCKED -> ColorBlocked
    }
    // ----------------------------------

    val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(180), label = "seatBg")

    // Matn va Ikonka rangini fonga moslash (Kontrast uchun)
    val contentColor = when {
        ui == SeatUiStatus.PENDING -> Color.White
        ui == SeatUiStatus.MINE_BOOKED -> Color.White
        ui == SeatUiStatus.BOOKED && isDriver -> Color.White // Haydovchi yashil ko'rganda oq yozuv
        ui == SeatUiStatus.BLOCKED -> Color.White
        else -> Color.Black // Available va oddiy Booked uchun qora
    }

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
        shadowElevation = if (pressed) 6.dp else 2.dp, // tonalElevation o'rniga shadow aniqroq ko'rinadi rangli fonda
        color = bg,
        contentColor = contentColor // Matn rangini qo'shdik
    ) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {

            // Qulf belgisi
            if (ui == SeatUiStatus.BLOCKED && lockedByDriver) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).size(14.dp),
                    tint = contentColor.copy(alpha = 0.7f)
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
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                }

                SeatUiStatus.BLOCKED -> {
                    // Bloklanganda chiziqcha
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

            // "Siz" degan yozuv (faqat sizniki bo'lsa)
            if (ui == SeatUiStatus.MINE_BOOKED) {
                Text(
                    "Siz",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified, // juda kichik bo'lmasligi uchun
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}