package com.example.yol_yolakay.feature.search

import android.Manifest
import android.app.DatePickerDialog
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.window.Dialog


private val UZ_REGIONS_SIMPLE = listOf(
    "Toshkent",
    "Andijon",
    "Buxoro",
    "Farg‘ona",
    "Jizzax",
    "Xorazm",
    "Namangan",
    "Navoiy",
    "Qashqadaryo",
    "Qoraqalpog‘iston",
    "Samarqand",
    "Sirdaryo",
    "Surxondaryo"
)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onSearchClick: (String, String, String, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        MapPlaceholder()

        SearchCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .statusBarsPadding(),
            uiState = uiState,
            onFromChange = viewModel::onFromLocationChange,
            onToChange = viewModel::onToLocationChange,
            onSwap = viewModel::onSwapLocations,
            onDateChange = viewModel::onDateChange,
            onPassengersChange = viewModel::onPassengersChange,
            onSearchSubmit = {
                onSearchClick(
                    uiState.fromLocation,
                    uiState.toLocation,
                    uiState.date.toString(),
                    uiState.passengers
                )
            }
        )
    }
}

@Composable
fun SearchCard(
    modifier: Modifier = Modifier,
    uiState: SearchUiState,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onSwap: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onPassengersChange: (Int) -> Unit,
    onSearchSubmit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showFromDialog by remember { mutableStateOf(false) }
    var showToDialog by remember { mutableStateOf(false) }

    // Qaysi field uchun "current location" ishlatyapmiz
    var pendingSet by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Joylashuv ruxsati berilmadi", Toast.LENGTH_SHORT).show()
            pendingSet = null
            return@rememberLauncherForActivityResult
        }

        val setter = pendingSet
        pendingSet = null

        if (setter != null) {
            scope.launch {
                val region = getCurrentUzRegionOrNull(context)
                if (region == null) {
                    Toast.makeText(context, "Hudud aniqlanmadi", Toast.LENGTH_SHORT).show()
                } else {
                    setter(region)
                }
            }
        }
    }

    fun useCurrentLocation(setter: (String) -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (granted) {
            scope.launch {
                val region = getCurrentUzRegionOrNull(context)
                if (region == null) {
                    Toast.makeText(context, "Hudud aniqlanmadi", Toast.LENGTH_SHORT).show()
                } else {
                    setter(region)
                }
            }
        } else {
            pendingSet = setter
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // 1) From -> To (bosilganda dialog ochiladi)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LocationTimeline()
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    LocationPickerField(
                        value = uiState.fromLocation,
                        placeholder = "Qayerdan?",
                        onClick = { showFromDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    LocationPickerField(
                        value = uiState.toLocation,
                        placeholder = "Qayerga?",
                        onClick = { showToDialog = true }
                    )
                }

                IconButton(onClick = onSwap) {
                    Icon(Icons.Rounded.SwapVert, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 2) Sana va Odam soni
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DatePickerButton(date = uiState.date, onDateSelected = onDateChange, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                PassengerCounter(count = uiState.passengers, onCountChange = onPassengersChange)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3) Qidirish
            Button(
                onClick = onSearchSubmit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.fromLocation.isNotBlank() && uiState.toLocation.isNotBlank()
            ) {
                Text("Qidirish", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showFromDialog) {
        RegionPickerDialog(
            title = "Qayerdan?",
            onDismiss = { showFromDialog = false },
            onSelected = {
                onFromChange(it)
                showFromDialog = false
            },
            onUseCurrentLocation = {
                useCurrentLocation { region ->
                    onFromChange(region)
                    showFromDialog = false
                }
            }
        )
    }

    if (showToDialog) {
        RegionPickerDialog(
            title = "Qayerga?",
            onDismiss = { showToDialog = false },
            onSelected = {
                onToChange(it)
                showToDialog = false
            },
            onUseCurrentLocation = {
                useCurrentLocation { region ->
                    onToChange(region)
                    showToDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerField(
    value: String,
    placeholder: String,
    onClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder, color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Default.Place, null) },
            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent
            ),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onClick() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        val q = query.trim()
        if (q.isBlank()) UZ_REGIONS_SIMPLE
        else UZ_REGIONS_SIMPLE.filter { it.contains(q, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null)
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
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Hududni qidiring") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        ListItem(
                            headlineContent = { Text("Joriy joylashuvdan foydalanish", color = MaterialTheme.colorScheme.primary) },
                            leadingContent = { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { onUseCurrentLocation() }
                        )
                        HorizontalDivider()
                    }

                    item {
                        Text(
                            "HUDUDLAR",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    items(filtered) { region ->
                        ListItem(
                            headlineContent = { Text(region) },
                            leadingContent = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.clickable { onSelected(region) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

// --- Kichik UI Bo'laklari (sizdagi eski kodlar saqlanadi) ---

@Composable
fun DatePickerButton(
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        date.year, date.monthValue - 1, date.dayOfMonth
    )
    datePickerDialog.datePicker.minDate = System.currentTimeMillis()

    Column(
        modifier = modifier
            .clickable { datePickerDialog.show() }
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sana", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PassengerCounter(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onCountChange(count - 1) },
            enabled = count > 1,
            modifier = Modifier.size(32.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Rounded.Remove, null, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(
            onClick = { onCountChange(count + 1) },
            modifier = Modifier.size(32.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun LocationTimeline() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Place, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Box(modifier = Modifier.height(24.dp).width(2.dp).background(Color.LightGray))
        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun MapPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Text("Xarita (Google/Yandex Map)", color = Color.Gray)
    }
}

// -------------------- Current location -> Region --------------------

private suspend fun getCurrentUzRegionOrNull(context: android.content.Context): String? = withContext(Dispatchers.IO) {
    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager

    // permission tekshiruvi (bu yerga kelguncha permission berilgan bo'ladi)
    val loc = bestLastKnownLocation(lm) ?: return@withContext null

    val raw = runCatching {
        @Suppress("DEPRECATION")
        Geocoder(context, Locale.getDefault())
            .getFromLocation(loc.latitude, loc.longitude, 1)
            ?.firstOrNull()
            ?.adminArea
            ?: ""
    }.getOrNull().orEmpty()

    normalizeUzRegion(raw)
}

private fun bestLastKnownLocation(lm: LocationManager): Location? {
    val providers = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )

    var best: Location? = null
    for (p in providers) {
        val l = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
        if (best == null || l.accuracy < best!!.accuracy) best = l
    }
    return best
}

private fun normalizeUzRegion(adminAreaRaw: String): String? {
    val s = adminAreaRaw.trim()
    if (s.isBlank()) return null

    val low = s.lowercase(Locale.ROOT)

    // ENG/RUS variantlarni normallashtiramiz
    val mapped = when {
        low.contains("tashkent") || low.contains("ташкент") -> "Toshkent"
        low.contains("andijan") || low.contains("андижан") -> "Andijon"
        low.contains("bukhara") || low.contains("бухара") -> "Buxoro"
        low.contains("fergana") || low.contains("fargona") || low.contains("ферган") -> "Farg‘ona"
        low.contains("jizzakh") || low.contains("джиз") -> "Jizzax"
        low.contains("khorezm") || low.contains("xorazm") || low.contains("хорез") -> "Xorazm"
        low.contains("namangan") || low.contains("наман") -> "Namangan"
        low.contains("navoi") || low.contains("наво") -> "Navoiy"
        low.contains("kashkadarya") || low.contains("qashqa") || low.contains("кашк") -> "Qashqadaryo"
        low.contains("karakalpak") || low.contains("қарақалп") -> "Qoraqalpog‘iston"
        low.contains("samarkand") || low.contains("самар") -> "Samarqand"
        low.contains("sirdarya") || low.contains("сирдар") -> "Sirdaryo"
        low.contains("surkhandarya") || low.contains("surx") || low.contains("сурх") -> "Surxondaryo"
        else -> null
    }

    return mapped?.takeIf { it in UZ_REGIONS_SIMPLE }
}
