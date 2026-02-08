package com.example.yol_yolakay.feature.publish.steps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.yol_yolakay.feature.publish.LocationModel
import com.example.yol_yolakay.feature.publish.components.LocationSelector

@Composable
fun Step2To(
    currentLocation: LocationModel?,
    onLocationSelected: (LocationModel) -> Unit,
    suggestions: List<LocationModel>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Qayerga borasiz?",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Biz yaratgan universal komponent
        LocationSelector(
            label = "Manzil",
            placeholder = "Masalan: Samarqand, Registon...",
            currentLocation = currentLocation,
            onLocationSelected = onLocationSelected,
            suggestions = suggestions
        )
    }
}