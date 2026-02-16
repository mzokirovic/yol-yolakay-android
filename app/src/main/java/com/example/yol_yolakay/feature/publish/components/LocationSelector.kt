package com.example.yol_yolakay.feature.publish.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = currentLocation?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Place, null) },
            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
            )
        )
        Box(modifier = Modifier.matchParentSize().clickable { showDialog = true })
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
                    title = { Text(if (selectedRegion == null) "Hududni tanlang" else selectedRegion!!) },
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
                                when {
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
                                mapInitial = if (first != null && (abs(first.lat) > 0.0001 || abs(first.lng) > 0.0001)) {
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
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "SHAHARNI TANLANG",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(UZBEKISTAN_REGIONS, key = { it }) { region ->
            ListItem(
                headlineContent = { Text(region) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray) },
                modifier = Modifier.clickable { onRegionClick(region) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = Color.LightGray.copy(alpha = 0.2f)
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
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            placeholder = { Text("Qidirish (Masalan: Avtovokzal)") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = { Text("Xaritadan belgilash", color = MaterialTheme.colorScheme.primary) },
                    leadingContent = { Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onMapClick() }
                )
                HorizontalDivider()
            }

            item {
                Text(
                    "MASHHUR JOYLAR (PITAKLAR)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
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
                        Text("Bu hududda hozircha pitaklar yo'q", color = Color.Gray)
                    }
                }
            }

            items(filtered, key = { (it.pointId ?: "") + it.name + it.lat + it.lng }) { point ->
                ListItem(
                    headlineContent = { Text(point.name) },
                    supportingContent = { Text(point.region.ifBlank { regionName }) },
                    leadingContent = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.clickable { onPointClick(point) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = Color.LightGray.copy(alpha = 0.2f)
                )
            }
        }
    }
}

private fun fmt(v: Double): String = String.format("%.5f", v)
