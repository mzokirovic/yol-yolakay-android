package com.example.yol_yolakay.feature.search.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val UZ_REGIONS = listOf(
    "Toshkent",
    "Andijon",
    "Buxoro",
    "Farg'ona",
    "Jizzax",
    "Xorazm",
    "Namangan",
    "Navoiy",
    "Qashqadaryo",
    "Qoraqalpog'iston",
    "Samarqand",
    "Sirdaryo",
    "Surxondaryo",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelectorField(
    placeholder: String,
    value: String,
    enableCurrentLocation: Boolean,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Place, null) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            singleLine = true
        )
        Box(Modifier.matchParentSize().clickable { showDialog = true })
    }

    if (showDialog) {
        RegionSelectionDialog(
            enableCurrentLocation = enableCurrentLocation,
            onDismiss = { showDialog = false },
            onSelected = {
                onSelected(it)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionSelectionDialog(
    enableCurrentLocation: Boolean,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val requestPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        val granted = (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true) ||
                (res[Manifest.permission.ACCESS_FINE_LOCATION] == true)

        if (!granted) {
            scope.launch { snackbar.showSnackbar("Lokatsiya ruxsati berilmadi") }
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            isResolving = true
            val region = resolveRegionFromCurrentLocation(context)
            isResolving = false

            val normalized = region?.let { normalizeRegion(it) }
            if (normalized != null) onSelected(normalized)
            else snackbar.showSnackbar("Joriy joylashuvdan viloyat aniqlanmadi")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Hududni tanlang") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Qidirish (masalan: Samarqand)") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    singleLine = true
                )

                if (isResolving) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                val filtered = remember(searchText) {
                    val q = searchText.trim()
                    if (q.isBlank()) UZ_REGIONS
                    else UZ_REGIONS.filter { it.contains(q, ignoreCase = true) }
                }

                LazyColumn(Modifier.fillMaxSize()) {

                    if (enableCurrentLocation) {
                        item {
                            ListItem(
                                headlineContent = { Text("Joriy joylashuv (Use current location)") },
                                leadingContent = { Icon(Icons.Default.MyLocation, null) },
                                modifier = Modifier.clickable(enabled = !isResolving) {
                                    val hasPerm =
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED ||
                                                ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.ACCESS_FINE_LOCATION
                                                ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPerm) {
                                        scope.launch {
                                            isResolving = true
                                            val region = resolveRegionFromCurrentLocation(context)
                                            isResolving = false

                                            val normalized = region?.let { normalizeRegion(it) }
                                            if (normalized != null) onSelected(normalized)
                                            else snackbar.showSnackbar("Joriy joylashuvdan viloyat aniqlanmadi")
                                        }
                                    } else {
                                        requestPerm.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        )
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }

                    items(filtered) { region ->
                        ListItem(
                            headlineContent = { Text(region) },
                            modifier = Modifier.clickable { onSelected(region) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private suspend fun resolveRegionFromCurrentLocation(context: Context): String? = withContext(Dispatchers.IO) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun last(provider: String): Location? = runCatching {
        if (!lm.isProviderEnabled(provider)) return@runCatching null
        lm.getLastKnownLocation(provider)
    }.getOrNull()

    val loc = listOfNotNull(
        last(LocationManager.NETWORK_PROVIDER),
        last(LocationManager.GPS_PROVIDER),
        last(LocationManager.PASSIVE_PROVIDER),
    ).maxByOrNull { it.time } ?: return@withContext null

    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = runCatching { geocoder.getFromLocation(loc.latitude, loc.longitude, 1) }
        .getOrNull()

    val a = addresses?.firstOrNull() ?: return@withContext null

    // AdminArea ko‘pincha viloyat/regionni beradi, ba’zida subAdminArea ham kerak bo‘ladi
    return@withContext a.adminArea ?: a.subAdminArea ?: a.locality
}

private fun normalizeRegion(raw: String): String? {
    val s = raw.lowercase(Locale.getDefault())

    // Uzbek/Rus/Eng bo'lishi mumkin — eng ko‘p uchraydigan mapping
    return when {
        s.contains("toshkent") || s.contains("ташкент") || s.contains("tashkent") -> "Toshkent"
        s.contains("andijon") || s.contains("андижан") || s.contains("andijan") -> "Andijon"
        s.contains("buxoro") || s.contains("бухара") || s.contains("bukhara") -> "Buxoro"
        s.contains("farg") || s.contains("ферган") || s.contains("ferg") -> "Farg'ona"
        s.contains("jizz") || s.contains("джиз") -> "Jizzax"
        s.contains("xorazm") || s.contains("хорезм") -> "Xorazm"
        s.contains("namangan") || s.contains("наманган") -> "Namangan"
        s.contains("navoiy") || s.contains("навои") -> "Navoiy"
        s.contains("qashq") || s.contains("кашк") -> "Qashqadaryo"
        s.contains("qoraqalp") || s.contains("карака") || s.contains("karakal") -> "Qoraqalpog'iston"
        s.contains("samarq") || s.contains("самарк") -> "Samarqand"
        s.contains("sird") || s.contains("сырд") -> "Sirdaryo"
        s.contains("surxon") || s.contains("сурх") -> "Surxondaryo"
        else -> null
    }
}
