package com.example.yol_yolakay.feature.search

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onSearchClick: (String, String, String, Int) -> Unit // Navigatsiya uchun qo'shildi
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
            onSearchSubmit = { // Tugma bosilganda ishlaydi
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
    onSearchSubmit: () -> Unit // Parametr sifatida qo'shildi
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 1. Manzillar (From -> To)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LocationTimeline()
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    LocationInput(uiState.fromLocation, onFromChange, "Qayerdan?")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    LocationInput(uiState.toLocation, onToChange, "Qayerga?")
                }
                IconButton(onClick = onSwap) {
                    Icon(Icons.Rounded.SwapVert, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Sana va Odam soni
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DatePickerButton(date = uiState.date, onDateSelected = onDateChange, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                PassengerCounter(count = uiState.passengers, onCountChange = onPassengersChange)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Qidirish Tugmasi
            Button(
                onClick = onSearchSubmit, // Navigatsiya funksiyasi ulandi
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Qidirish", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// --- Kichik UI Bo'laklari ---

@Composable
fun DatePickerButton(
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Android Native DatePicker Dialog
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
fun LocationInput(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.LightGray) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent
        ),
        singleLine = true
    )
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