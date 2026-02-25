package com.example.yol_yolakay.feature.publish.components

import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.feature.publish.LocationModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

// Regionlar ro'yxati o'zgarishsiz qoladi
val UZBEKISTAN_REGIONS = listOf(
    "Toshkent shahri", "Toshkent viloyati", "Andijon viloyati", "Buxoro viloyati",
    "Farg'ona viloyati", "Jizzax viloyati", "Xorazm viloyati", "Namangan viloyati",
    "Navoiy viloyati", "Qashqadaryo viloyati", "Qoraqalpog'iston", "Samarqand viloyati",
    "Sirdaryo viloyati", "Surxondaryo viloyati"
)

@Composable
fun LocationSelector(
    label: String,
    placeholder: String,
    currentLocation: LocationModel?,
    onLocationSelected: (LocationModel) -> Unit,
    suggestions: List<LocationModel>
) {
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Orqaga qaytish mantiqi
    BackHandler(enabled = selectedRegion != null) {
        selectedRegion = null
        searchText = ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sarlavha qismi (Dynamic)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedRegion != null) {
                IconButton(onClick = { selectedRegion = null; searchText = "" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }
            Text(
                text = selectedRegion ?: "Hududni tanlang",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = if (selectedRegion == null) 8.dp else 0.dp)
            )
        }

        AnimatedContent(
            targetState = selectedRegion,
            label = "RegionTransition",
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
            }
        ) { region ->
            if (region == null) {
                RegionList(onRegionClick = { selectedRegion = it })
            } else {
                val pointsInRegion = suggestions.filter { it.region.equals(region, ignoreCase = true) }
                DistrictPointsScreen(
                    regionName = region,
                    points = pointsInRegion,
                    searchText = searchText,
                    onSearchChange = { searchText = it },
                    onPointClick = onLocationSelected,
                    onMapClick = { showMapPicker = true }
                )
            }
        }
    }

    // Xarita dialogi
    if (showMapPicker) {
        val initial = currentLocation?.let { LatLng(it.lat, it.lng) }
            ?: LatLng(41.311081, 69.240562)

        // ✅ selectedRegion bo‘yicha base pointlarni mapga chiqaramiz (limit qo‘ydik)
        val basePointsForMap = remember(selectedRegion, suggestions) {
            val region = selectedRegion
            if (region.isNullOrBlank()) emptyList()
            else suggestions
                .filter { it.region.equals(region, ignoreCase = true) }
                .take(80) // ✅ performance uchun
                .map { loc ->
                    MapBasePoint(
                        id = loc.pointId ?: "${loc.name}:${loc.lat}:${loc.lng}",
                        name = loc.name,
                        position = LatLng(loc.lat, loc.lng),
                        pointId = loc.pointId,
                        region = loc.region
                    )
                }
        }

        MapPickerDialog(
            title = "Xaritadan belgilash",
            initialLatLng = initial,
            initialZoom = 13f,
            onDismiss = { showMapPicker = false },

            // ✅ hozir tanlangan point highlight bo‘lsin
            initialSelectedBasePointId = currentLocation?.pointId,

            // ✅ base points marker bo‘lib chiqadi
            basePoints = basePointsForMap,

            onBasePointPicked = { bp ->
                // marker bosilganda darrov tanlaymiz va yopamiz
                showMapPicker = false
                onLocationSelected(
                    LocationModel(
                        name = bp.name,
                        lat = bp.position.latitude,
                        lng = bp.position.longitude,
                        pointId = bp.pointId,
                        region = selectedRegion ?: (bp.region ?: "")
                    )
                )
            },

            onPicked = { latLng ->
                showMapPicker = false
                scope.launch {
                    val name = reverseGeocodeBestEffort(ctx, latLng)
                        ?: "Pin: ${latLng.latitude.round5()}, ${latLng.longitude.round5()}"

                    onLocationSelected(
                        LocationModel(
                            name = name,
                            lat = latLng.latitude,
                            lng = latLng.longitude,
                            pointId = null,
                            region = selectedRegion ?: ""
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun RegionList(onRegionClick: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(UZBEKISTAN_REGIONS) { region ->
            ListItem(
                headlineContent = { Text(region, fontWeight = FontWeight.SemiBold) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable { onRegionClick(region) }
            )
            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun DistrictPointsScreen(
    regionName: String,
    points: List<LocationModel>,
    searchText: String,
    onSearchChange: (String) -> Unit,
    onPointClick: (LocationModel) -> Unit,
    onMapClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            placeholder = { Text("Qidirish...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            "Xaritadan belgilash",
                            color = cs.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Map, null, tint = cs.primary) },
                    modifier = Modifier.clickable { onMapClick() }
                )
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
            }

            val filtered = points.filter { it.name.contains(searchText, ignoreCase = true) }
            items(filtered) { point ->
                ListItem(
                    headlineContent = { Text(point.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Text(point.region, style = MaterialTheme.typography.bodySmall)
                    },
                    leadingContent = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.clickable { onPointClick(point) }
                )
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

// ---------------------- helpers (FAYL OXIRIDA bo‘lishi kerak) ----------------------

private fun Double.round5(): Double = (this * 100000.0).roundToInt() / 100000.0

private suspend fun reverseGeocodeBestEffort(
    context: android.content.Context,
    latLng: LatLng
): String? = withContext(Dispatchers.IO) {
    try {
        if (!Geocoder.isPresent()) return@withContext null
        val g = Geocoder(context, Locale.getDefault())
        val list = g.getFromLocation(latLng.latitude, latLng.longitude, 1)
        val a = list?.firstOrNull() ?: return@withContext null
        a.getAddressLine(0)
    } catch (_: Throwable) {
        null
    }
}