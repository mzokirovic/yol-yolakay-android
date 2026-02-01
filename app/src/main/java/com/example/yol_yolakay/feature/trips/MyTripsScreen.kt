package com.example.yol_yolakay.feature.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.search.components.TripItem

private enum class MyTripsTab { DRIVER, PASSENGER }

@Composable
fun MyTripsScreen(
    onTripClick: (String) -> Unit, // ✅ qo‘shildi
    viewModel: MyTripsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf(MyTripsTab.DRIVER) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == MyTripsTab.DRIVER) {
            viewModel.loadMyTrips()
        }
    }

    Column(Modifier.fillMaxSize()) {

        TabRow(selectedTabIndex = if (selectedTab == MyTripsTab.DRIVER) 0 else 1) {
            Tab(
                selected = selectedTab == MyTripsTab.DRIVER,
                onClick = { selectedTab = MyTripsTab.DRIVER },
                text = { Text("Haydovchi") }
            )
            Tab(
                selected = selectedTab == MyTripsTab.PASSENGER,
                onClick = { selectedTab = MyTripsTab.PASSENGER },
                text = { Text("Yo‘lovchi") }
            )
        }

        Box(Modifier.fillMaxSize()) {
            when (selectedTab) {
                MyTripsTab.DRIVER -> {
                    when {
                        uiState.isLoading ->
                            CircularProgressIndicator(Modifier.align(Alignment.Center))

                        uiState.error != null ->
                            Text(uiState.error!!, Modifier.align(Alignment.Center))

                        uiState.trips.isEmpty() ->
                            Text("Hali e’lon qilingan safarlar yo‘q", Modifier.align(Alignment.Center))

                        else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(uiState.trips) { trip ->
                                TripItem(
                                    trip = trip,
                                    onClick = {
                                        val id = trip.id
                                        if (!id.isNullOrBlank()) onTripClick(id)
                                    }
                                )
                            }
                        }
                    }
                }

                MyTripsTab.PASSENGER -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Yo‘lovchi safarlari (bronlar) tez kunda")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Keyingi bosqich: Bookings endpoint + MyBookings list",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
