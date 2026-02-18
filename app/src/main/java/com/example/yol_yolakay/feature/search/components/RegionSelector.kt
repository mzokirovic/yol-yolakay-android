package com.example.yol_yolakay.feature.search.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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

@Composable
fun RegionSelectorField(
    placeholder: String,
    value: String,
    enableCurrentLocation: Boolean,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,

    // External control (Search button bosganda sheetni ochish uchun)
    openSheet: Boolean = false,
    onOpenSheetChange: ((Boolean) -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme

    var internalShow by remember { mutableStateOf(false) }
    val showSheet = if (onOpenSheetChange != null) openSheet else internalShow

    fun setSheet(v: Boolean) {
        if (onOpenSheetChange != null) onOpenSheetChange(v) else internalShow = v
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { setSheet(true) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))

            // Bo‘sh bo‘lsa ko‘rinmaydi, lekin layout balandligi saqlanadi
            Text(
                text = value.ifBlank { "\u200B" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (value.isBlank()) cs.onSurfaceVariant.copy(alpha = 0.01f) else cs.onSurface
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant
        )
    }

    if (showSheet) {
        RegionSelectionSheet(
            enableCurrentLocation = enableCurrentLocation,
            onDismiss = { setSheet(false) },
            onSelected = {
                onSelected(it)
                setSheet(false)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionSelectionSheet(
    enableCurrentLocation: Boolean,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var searchText by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Permission request launcher
    val requestPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        val granted = (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true) ||
                (res[Manifest.permission.ACCESS_FINE_LOCATION] == true)

        if (!granted) {
            scope.launch { snackbar.showSnackbar("Lokatsiya ruxsati berilmadi") }
            return@rememberLauncherForActivityResult
        }

        // Permission berildi — endi Location yoqilganmi tekshiramiz
        scope.launch {
            if (!isLocationEnabled(context)) {
                val r = snackbar.showSnackbar(
                    message = "Lokatsiya (GPS) o‘chiq",
                    actionLabel = "Yoqish"
                )
                if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    openLocationSettings(context)
                }
                return@launch
            }

            isResolving = true
            val region = resolveRegionFromCurrentLocation(context)
            isResolving = false

            val normalized = region?.let { normalizeRegion(it) }
            if (normalized != null) onSelected(normalized)
            else snackbar.showSnackbar("Joriy joylashuvdan viloyat aniqlanmadi")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hududni tanlang",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = cs.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            // Search
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Qidirish (masalan: Samarqand)") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.outlineVariant,
                    unfocusedBorderColor = cs.outlineVariant,
                    focusedContainerColor = cs.surfaceVariant.copy(alpha = 0.40f),
                    unfocusedContainerColor = cs.surfaceVariant.copy(alpha = 0.32f)
                )
            )

            Spacer(Modifier.height(10.dp))

            if (isResolving) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            SnackbarHost(hostState = snackbar)

            val filtered = remember(searchText) {
                val q = searchText.trim()
                if (q.isBlank()) UZ_REGIONS else UZ_REGIONS.filter { it.contains(q, ignoreCase = true) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                if (enableCurrentLocation) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = cs.surfaceVariant.copy(alpha = 0.40f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isResolving) {
                                        val hasPerm =
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED ||
                                                    ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.ACCESS_FINE_LOCATION
                                                    ) == PackageManager.PERMISSION_GRANTED

                                        if (!hasPerm) {
                                            requestPerm.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.ACCESS_FINE_LOCATION
                                                )
                                            )
                                            return@clickable
                                        }

                                        // ✅ Permission bor — endi Location yoqilganmi?
                                        if (!isLocationEnabled(context)) {
                                            scope.launch {
                                                val r = snackbar.showSnackbar(
                                                    message = "Lokatsiya (GPS) o‘chiq",
                                                    actionLabel = "Yoqish"
                                                )
                                                if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                    openLocationSettings(context)
                                                }
                                            }
                                            return@clickable
                                        }

                                        // ✅ Hammasi OK — region aniqlaymiz
                                        scope.launch {
                                            isResolving = true
                                            val region = resolveRegionFromCurrentLocation(context)
                                            isResolving = false

                                            val normalized = region?.let { normalizeRegion(it) }
                                            if (normalized != null) onSelected(normalized)
                                            else snackbar.showSnackbar("Joriy joylashuvdan viloyat aniqlanmadi")
                                        }
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, null, tint = cs.primary)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Joriy joylashuv",
                                        fontWeight = FontWeight.SemiBold,
                                        color = cs.onSurface
                                    )
                                    Text(
                                        "GPS orqali aniqlash",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.KeyboardArrowRight, null, tint = cs.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = cs.outlineVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                items(filtered) { region ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(region) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = region,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = cs.onSurfaceVariant)
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = cs.outlineVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun isLocationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gps = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
    val network = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
    return gps || network
}

private fun openLocationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private suspend fun resolveRegionFromCurrentLocation(context: Context): String? =
    withContext(Dispatchers.IO) {
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
        val addresses = runCatching { geocoder.getFromLocation(loc.latitude, loc.longitude, 1) }.getOrNull()
        val a = addresses?.firstOrNull() ?: return@withContext null

        return@withContext a.adminArea ?: a.subAdminArea ?: a.locality
    }

private fun normalizeRegion(raw: String): String? {
    val s = raw.lowercase(Locale.getDefault())
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
