package com.example.yol_yolakay.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.search.components.TripItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    from: String,
    to: String,
    date: String,
    onBack: () -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.searchTrips(from, to, date)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$from → $to") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(uiState.error!!, Modifier.align(Alignment.Center))
            } else if (uiState.trips.isEmpty()) {
                Text("Safarlar topilmadi", Modifier.align(Alignment.Center))
            } else {
                LazyColumn {
                    items(uiState.trips) { trip ->
                        TripItem(trip = trip, onClick = { /* Detal keyin */ })
                    }
                }
            }
        }
    }
}