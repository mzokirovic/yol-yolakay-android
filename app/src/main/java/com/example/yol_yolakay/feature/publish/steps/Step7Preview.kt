package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.feature.publish.LocationModel
import com.example.yol_yolakay.feature.publish.PublishStep
import com.example.yol_yolakay.feature.publish.PublishUiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun Step7Preview(
    uiState: PublishUiState,
    onEditStep: (PublishStep) -> Unit
) {
    val draft = uiState.draft
    val cs = MaterialTheme.colorScheme

    val from = draft.fromLocation
    val to = draft.toLocation

    val money = remember {
        NumberFormat.getInstance(Locale("uz", "UZ")).apply { maximumFractionDigits = 0 }
    }
    val priceText = formatSom(draft.price, money)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Ma'lumotlarni tekshiring",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(6.dp))

        // ✅ MAP PREVIEW (return ishlatmaymiz, shuning uchun compile yiqilmaydi)
        if (from != null && to != null) {
            RouteMapPreviewCard(
                from = from,
                to = to,
                route = uiState.routePreview,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Tanlanmagan bo'lsa ham chiroyli placeholder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = MaterialTheme.shapes.large,
                color = cs.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Xarita ko‘rinishi uchun manzillarni tanlang",
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        PreviewRow(
            label = "Yo'nalish",
            value = "${from?.name ?: "—"} → ${to?.name ?: "—"}"
        ) { onEditStep(PublishStep.FROM) }

        PreviewRow(
            label = "Sana va vaqt",
            value = "${draft.date ?: "—"}, ${draft.time ?: "—"}"
        ) { onEditStep(PublishStep.DATE) }

        PreviewRow(
            label = "O'rinlar soni",
            value = "${draft.passengers ?: 0} kishi"
        ) { onEditStep(PublishStep.PASSENGERS) }

        PreviewRow(
            label = "Narx (bir kishi uchun)",
            value = priceText
        ) { onEditStep(PublishStep.PRICE) }
    }
}

@Composable
private fun RouteMapPreviewCard(
    from: LocationModel,
    to: LocationModel,
    route: com.example.yol_yolakay.feature.publish.RoutePreviewState,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val fromLL = remember(from.lat, from.lng) { LatLng(from.lat, from.lng) }
    val toLL = remember(to.lat, to.lng) { LatLng(to.lat, to.lng) }

    val points = remember(route.points, fromLL, toLL) {
        if (route.hasRoute) route.points else listOf(fromLL, toLL)
    }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(points) {
        val b = LatLngBounds.builder()
        points.forEach { b.include(it) }
        runCatching {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(b.build(), 120),
                durationMs = 650
            )
        }
    }

    Surface(
        modifier = modifier.height(240.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    rotationGesturesEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false
                )
            ) {
                Marker(state = MarkerState(position = fromLL), title = "Qayerdan")
                Marker(state = MarkerState(position = toLL), title = "Qayerga")

                Polyline(
                    points = points,
                    color = cs.primary,
                    width = 10f
                )
            }

            // mini info
            val info = remember(route.distanceKm, route.durationMin) {
                val d = if (route.distanceKm > 0) "${route.distanceKm} km" else null
                val t = if (route.durationMin > 0) "${route.durationMin} min" else null
                listOfNotNull(d, t).joinToString(" • ").ifBlank { "Yo‘l" }
            }

            Surface(
                color = cs.surface.copy(alpha = 0.92f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(info, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    if (route.isLoading) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // custom zoom
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) } }
                ) { Icon(Icons.Default.Add, contentDescription = "Zoom in") }

                FilledTonalIconButton(
                    onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomOut()) } }
                ) { Icon(Icons.Default.Remove, contentDescription = "Zoom out") }
            }
        }
    }
}

@Composable
private fun PreviewRow(
    label: String,
    value: String,
    onEdit: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Icon(
            Icons.Outlined.Edit,
            contentDescription = null,
            tint = cs.outlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
    HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.45f), thickness = 0.5.dp)
}


private fun formatSom(raw: Any?, nf: NumberFormat): String {
    if (raw == null) return "—"

    // 1) Agar Number bo‘lsa — formatlaymiz
    val number: Long? = when (raw) {
        is Number -> raw.toLong()
        is String -> raw
            .trim()
            .replace(Regex("[^0-9]"), "")
            .toLongOrNull()
        else -> null
    }

    // 2) Agar son topilsa — chiroyli format
    if (number != null) return "${nf.format(number)} so'm"

    // 3) Aks holda — boricha ko‘rsatamiz (ikki marta so'm qo‘shmaymiz)
    val s = raw.toString().trim()
    if (s.isBlank()) return "—"
    return if (s.contains("so'm", ignoreCase = true) || s.contains("som", ignoreCase = true)) s else "$s so'm"
}