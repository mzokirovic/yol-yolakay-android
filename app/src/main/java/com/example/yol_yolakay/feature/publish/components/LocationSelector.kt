package com.example.yol_yolakay.feature.publish.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.yol_yolakay.feature.publish.LocationModel
import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs

val UZBEKISTAN_REGIONS = listOf(
    "Toshkent shahri", "Toshkent viloyati", "Andijon viloyati", "Buxoro viloyati",
    "Farg'ona viloyati", "Jizzax viloyati", "Xorazm viloyati", "Namangan viloyati",
    "Navoiy viloyati", "Qashqadaryo viloyati", "Qoraqalpog'iston", "Samarqand viloyati",
    "Sirdaryo viloyati", "Surxondaryo viloyati"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelector(
    label: String,
    placeholder: String,
    currentLocation: LocationModel?,
    onLocationSelected: (LocationModel) -> Unit,
    suggestions: List<LocationModel>
) {
    var showDialog by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme

    // ✅ UI: TextField ichidagi label/value tartibli bo‘lishi uchun “tile” ko‘rinishida chizamiz
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cs.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cs.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = cs.onPrimaryContainer)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                val valueText = currentLocation?.name.orEmpty()
                Text(
                    text = if (valueText.isBlank()) placeholder else valueText,
                    style = MaterialTheme.typography.titleSmall, // ✅ barqaror, chiroyli
                    fontWeight = FontWeight.SemiBold,
                    color = if (valueText.isBlank()) cs.onSurfaceVariant else cs.onSurface,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = cs.onSurfaceVariant
            )
        }
    }

    if (showDialog) {
        LocationSelectionDialog(
            suggestions = suggestions,
            onDismiss = { showDialog = false },
            onSelected = {
                onLocationSelected(it)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSelectionDialog(
    suggestions: List<LocationModel>,
    onDismiss: () -> Unit,
    onSelected: (LocationModel) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }

    var showMapPicker by remember { mutableStateOf(false) }
    var mapInitial by remember { mutableStateOf(LatLng(41.311081, 69.240562)) } // Toshkent default

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 1) Avval map ochilgan bo‘lsa, back mapni yopadi
        BackHandler(enabled = showMapPicker || selectedRegion != null) {
            when {
                showMapPicker -> showMapPicker = false
                selectedRegion != null -> {
                    selectedRegion = null
                    searchText = ""
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedRegion == null) "Hududni tanlang" else selectedRegion!!,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            when {
                                showMapPicker -> showMapPicker = false
                                selectedRegion != null -> {
                                    selectedRegion = null
                                    searchText = ""
                                }
                                else -> onDismiss()
                            }
                        }) {
                            Icon(
                                imageVector = when {
                                    showMapPicker -> Icons.AutoMirrored.Filled.ArrowBack
                                    selectedRegion != null -> Icons.AutoMirrored.Filled.ArrowBack
                                    else -> Icons.Default.Close
                                },
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
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
                        RegionListScreen(onRegionClick = { selectedRegion = it })
                    } else {
                        val pointsInRegion = suggestions.filter {
                            it.region.equals(region, ignoreCase = true)
                        }

                        DistrictPointsScreen(
                            regionName = region,
                            points = pointsInRegion,
                            searchText = searchText,
                            onSearchChange = { searchText = it },
                            onPointClick = onSelected,
                            onMapClick = {
                                // Map initial: shu regiondagi birinchi pitak bo‘lsa o‘sha, bo‘lmasa Toshkent
                                val first = pointsInRegion.firstOrNull()
                                mapInitial =
                                    if (first != null && (abs(first.lat) > 0.0001 || abs(first.lng) > 0.0001)) {
                                        LatLng(first.lat, first.lng)
                                    } else {
                                        LatLng(41.311081, 69.240562)
                                    }
                                showMapPicker = true
                            }
                        )
                    }
                }

                if (showMapPicker) {
                    val region = selectedRegion ?: ""
                    MapPickerDialog(
                        title = "Xaritadan belgilash",
                        initialLatLng = mapInitial,
                        onDismiss = { showMapPicker = false },
                        onPicked = { latLng ->
                            val loc = LocationModel(
                                name = "Xaritadan tanlangan joy (${fmt(latLng.latitude)}, ${fmt(latLng.longitude)})",
                                lat = latLng.latitude,
                                lng = latLng.longitude,
                                pointId = null,
                                region = region
                            )
                            showMapPicker = false
                            onSelected(loc)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionListScreen(onRegionClick: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "HUDUDNI TANLANG",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(UZBEKISTAN_REGIONS, key = { it }) { region ->
            ListItem(
                headlineContent = {
                    Text(
                        text = region,
                        style = MaterialTheme.typography.titleSmall, // ✅ tartibli
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null,
                        tint = cs.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { onRegionClick(region) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = cs.outlineVariant.copy(alpha = 0.35f)
            )
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
            placeholder = { Text("Qidirish (Masalan: Avtovokzal)") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            "Xaritadan belgilash",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.primary,
                            maxLines = 1
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Map, null, tint = cs.primary) },
                    modifier = Modifier.clickable { onMapClick() }
                )
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.35f))
            }

            item {
                Text(
                    text = "MASHHUR JOYLAR (PITAKLAR)",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            val filtered = points.filter { it.name.contains(searchText, ignoreCase = true) }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Bu hududda hozircha pitaklar yo'q", color = cs.onSurfaceVariant)
                    }
                }
            }

            items(filtered, key = { (it.pointId ?: "") + it.name + it.lat + it.lng }) { point ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = point.name,
                            style = MaterialTheme.typography.titleSmall, // ✅ tartibli
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    },
                    supportingContent = {
                        Text(
                            text = point.region.ifBlank { regionName },
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 2
                        )
                    },
                    leadingContent = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.clickable { onPointClick(point) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = cs.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}

private fun fmt(v: Double): String = String.format("%.5f", v)
