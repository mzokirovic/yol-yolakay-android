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

// O'zbekiston viloyatlari (Statik ro'yxat - tartib uchun)
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
    suggestions: List<LocationModel> // Backenddan kelgan BARCHA pitaklar
) {
    var showDialog by remember { mutableStateOf(false) }

    // Input Field (O'zgarishsiz)
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
    // Holatlar: Null bo'lsa -> Viloyatlar ro'yxati. String bo'lsa -> Shu viloyat tanlangan
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Dialog ichidagi Back buttonni ushlash
        BackHandler(enabled = selectedRegion != null) {
            selectedRegion = null // Orqaga qaytish (Regionlarga)
            searchText = ""
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (selectedRegion == null) "Hududni tanlang" else selectedRegion!!) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (selectedRegion != null) {
                                selectedRegion = null
                                searchText = ""
                            } else {
                                onDismiss()
                            }
                        }) {
                            Icon(
                                if (selectedRegion != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Animated Content: Regionlar <-> Pitaklar almashinuvi
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
                        // 1-BOSQICH: VILOYATLAR RO'YXATI
                        RegionListScreen(
                            onRegionClick = { selectedRegion = it }
                        )
                    } else {
                        // 2-BOSQICH: PITAKLAR RO'YXATI (Filtrlangan)
                        // Shu viloyatga tegishli pitaklarni ajratib olamiz
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
                                // Xaritadan tanlash (Mock)
                                onSelected(LocationModel("Xaritadan: $region", 41.0, 69.0, null, region))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RegionListScreen(onRegionClick: (String) -> Unit) {
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
        items(UZBEKISTAN_REGIONS) { region ->
            ListItem(
                headlineContent = { Text(region) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray) },
                modifier = Modifier.clickable { onRegionClick(region) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun DistrictPointsScreen(
    regionName: String,
    points: List<LocationModel>,
    searchText: String,
    onSearchChange: (String) -> Unit,
    onPointClick: (LocationModel) -> Unit,
    onMapClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Qidiruv
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
            // Xaritadan belgilash opsiyasi
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

            // Filtrlangan ro'yxat
            val filtered = points.filter { it.name.contains(searchText, ignoreCase = true) }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Bu hududda hozircha pitaklar yo'q", color = Color.Gray)
                    }
                }
            }

            items(filtered) { point ->
                ListItem(
                    headlineContent = { Text(point.name) },
                    supportingContent = { Text(point.region) },
                    leadingContent = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.clickable { onPointClick(point) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
            }
        }
    }
}