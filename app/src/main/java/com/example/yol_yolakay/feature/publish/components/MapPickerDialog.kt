package com.example.yol_yolakay.feature.publish.components

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    title: String,
    initialLatLng: LatLng = LatLng(41.311081, 69.240562),
    initialZoom: Float = 12f,
    onDismiss: () -> Unit,
    onPicked: (LatLng) -> Unit,

    // ✅ ixtiyoriy: Google Cloud'dagi Map ID (advanced markers / styling uchun)
    mapId: String? = null,

    // ✅ ixtiyoriy: qaysi basePoint tanlangan bo‘lib ochilsin
    initialSelectedBasePointId: String? = null,

    // ✅ ixtiyoriy: base pointlar marker bo'lib chiqadi
    basePoints: List<MapBasePoint> = emptyList(),
    onBasePointPicked: ((MapBasePoint) -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, initialZoom)
    }

    var address by remember { mutableStateOf<String?>(null) }
    var resolving by remember { mutableStateOf(false) }

    // ✅ Marker tanlash highlight uchun (initial qiymatni ham oladi)
    var selectedBasePointId by remember(initialSelectedBasePointId) {
        mutableStateOf(initialSelectedBasePointId)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { cameraPositionState.isMoving }
            .distinctUntilChanged()
            .filter { moving -> !moving }
            .collect {
                val target = cameraPositionState.position.target
                resolving = true
                address = reverseGeocodeBestEffort(ctx, target)
                resolving = false
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Yopish")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // ✅ Address card (uber-ish bottom card)
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (resolving) "Joy aniqlanmoqda..." else (address ?: "Adres topilmadi"),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Pin markazda — xaritani surib joyni tanlang",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) { Text("Bekor qilish") }

                            Button(
                                onClick = { onPicked(cameraPositionState.position.target) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Tanlash") }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // ✅ Map options (Map ID bo'lsa qo'llaydi)
                val optionsFactory: () -> GoogleMapOptions = {
                    GoogleMapOptions().apply {
                        val id = mapId?.trim().orEmpty()
                        if (id.isNotEmpty()) {
                            mapId(id)
                        }
                    }
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    googleMapOptionsFactory = optionsFactory,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,      // ❌ default +/- ni o'chiramiz
                        myLocationButtonEnabled = false,  // keyin o'zimiz chizamiz
                        compassEnabled = false
                    ),
                    properties = MapProperties()
                ) {
                    // ✅ Base point markerlar
                    basePoints.forEach { p ->
                        val isSelected = (p.id == selectedBasePointId)
                        Marker(
                            state = MarkerState(position = p.position),
                            title = p.name,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (isSelected) BitmapDescriptorFactory.HUE_AZURE
                                else BitmapDescriptorFactory.HUE_RED
                            ),
                            onClick = {
                                selectedBasePointId = p.id
                                onBasePointPicked?.invoke(p)
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(p.position, 15f)
                                    )
                                }
                                true
                            }
                        )
                    }
                }

                // ✅ Center pin (tanlash markazi)
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // ✅ Custom zoom buttons (defaultni almashtirdik)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) } }
                    ) { Icon(Icons.Default.Add, contentDescription = "Zoom in") }

                    FilledTonalIconButton(
                        onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomOut()) } }
                    ) { Icon(Icons.Default.Remove, contentDescription = "Zoom out") }
                }

                // ✅ Recenter
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(initialLatLng, initialZoom)
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 200.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Markazga qaytish")
                }
            }
        }
    }
}

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