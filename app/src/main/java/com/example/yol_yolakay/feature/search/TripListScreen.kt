package com.example.yol_yolakay.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.search.components.TripItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    from: String,
    to: String,
    date: String,
    passengers: Int,
    onBack: () -> Unit,
    onTripClick: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    fun reload() {
        viewModel.searchTrips(from, to, date, passengers)
    }

    LaunchedEffect(from, to, date, passengers) { reload() }

    val datePretty = remember(date) {
        runCatching { LocalDate.parse(date) }.getOrNull()
            ?.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("uz")))
            ?.replaceFirstChar { it.uppercase() }
            ?: date
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$from → $to") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash")
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
            // 🔹 Header card (sana + passenger + natija soni)
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(datePretty, style = MaterialTheme.typography.titleSmall)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { /* no-op */ },
                            label = { Text("Yo‘lovchi: $passengers") }
                        )
                        AssistChip(
                            onClick = { /* no-op */ },
                            label = {
                                val c = uiState.trips.size
                                Text("Topildi: $c")
                            }
                        )
                    }

                    if (uiState.isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // 🔹 Body
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.trips.isEmpty() -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }

                    uiState.error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(uiState.error!!, style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { reload() }) { Text("Qayta urinib ko‘rish") }
                        }
                    }

                    uiState.trips.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Safarlar topilmadi", style = MaterialTheme.typography.titleMedium)
                            Text("Sana yoki yo‘nalishni o‘zgartirib ko‘ring", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = onBack) { Text("Qidiruvga qaytish") }
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item { Spacer(Modifier.height(2.dp)) }

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
            }
        }
    }
}
