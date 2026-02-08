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
fun Step1From(
    currentLocation: LocationModel?,
    onLocationSelected: (LocationModel) -> Unit,
    suggestions: List<LocationModel>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Qayerdan yo'lga chiqasiz?",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Yangi komponentni chaqiramiz
        LocationSelector(
            label = "Jo'nash manzili",
            placeholder = "Masalan: Toshkent, Olmazor...",
            currentLocation = currentLocation,
            onLocationSelected = onLocationSelected,
            suggestions = suggestions
        )
    }
}