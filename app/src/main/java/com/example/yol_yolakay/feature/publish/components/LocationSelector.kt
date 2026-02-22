package com.example.yol_yolakay.feature.publish.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.feature.publish.LocationModel
import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs

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
    // ✅ Ortiqcha Surface va Dialog olib tashlandi.
    // BottomSheet ichida bevosita qidiruv mantiqi boshlanadi.

    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }

    // Orqaga qaytish mantiqi
    BackHandler(enabled = selectedRegion != null) {
        selectedRegion = null
        searchText = ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sarlavha qismi (Dynamic)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
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

    // Xarita dialogi (agar kerak bo'lsa)
    if (showMapPicker) {
        // MapPickerDialog chaqiruvi bu yerda qoladi...
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = { Text("Xaritadan belgilash", color = cs.primary, fontWeight = FontWeight.Bold) },
                    leadingContent = { Icon(Icons.Default.Map, null, tint = cs.primary) },
                    modifier = Modifier.clickable { onMapClick() }
                )
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
            }

            val filtered = points.filter { it.name.contains(searchText, ignoreCase = true) }
            items(filtered) { point ->
                ListItem(
                    headlineContent = { Text(point.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(point.region, style = MaterialTheme.typography.bodySmall) },
                    leadingContent = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.clickable { onPointClick(point) }
                )
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}