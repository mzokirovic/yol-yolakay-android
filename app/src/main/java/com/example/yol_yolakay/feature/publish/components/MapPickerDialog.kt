package com.example.yol_yolakay.feature.publish.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    title: String,
    initialLatLng: LatLng = LatLng(41.311081, 69.240562), // Toshkent default
    initialZoom: Float = 12f,
    onDismiss: () -> Unit,
    onPicked: (LatLng) -> Unit
) {
    var picked by remember { mutableStateOf(initialLatLng) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, initialZoom)
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
                Surface(tonalElevation = 2.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) { Text("Bekor qilish") }

                        Button(
                            onClick = { onPicked(picked) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Tanlash") }
                    }
                }
            }
        ) { padding ->
            GoogleMap(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    picked = latLng
                },
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                Marker(
                    state = MarkerState(position = picked),
                    title = "Tanlangan joy"
                )
            }
        }
    }
}
