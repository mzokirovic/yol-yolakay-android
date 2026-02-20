package com.example.yol_yolakay.feature.profile.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

// Ranglar ro'yxati
val CAR_COLORS = listOf("Oq (White)", "Qora (Black)", "Kulrang (Grey)", "Kumush (Silver)", "Qizil", "Ko'k", "Yashil", "Boshqa")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(
    onBack: () -> Unit,
    vm: VehicleViewModel = viewModel(
        factory = VehicleViewModel.factory(LocalContext.current)
    )
) {
    val s = vm.state // State ni ViewModeldan olamiz
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog holatlari
    var showBrandDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Avtomobil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (s.error != null) {
                Text(s.error, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 1. BREND
            SelectionField(
                label = "Mashina markasi",
                value = s.selectedBrand?.name ?: "",
                placeholder = "Tanlang (Masalan: Chevrolet)",
                onClick = { showBrandDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. MODEL
            SelectionField(
                label = "Model",
                value = s.selectedModelName,
                placeholder = "Tanlang (Masalan: Cobalt)",
                onClick = {
                    if (s.selectedBrand != null) showModelDialog = true
                    else scope.launch { snackbarHostState.showSnackbar("Avval markani tanlang") }
                },
                isEnabled = s.selectedBrand != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. RANG
            SelectionField(
                label = "Rangi",
                value = s.selectedColor,
                placeholder = "Tanlang",
                onClick = { showColorDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. DAVLAT RAQAMI
            OutlinedTextField(
                value = s.plateNumber,
                onValueChange = vm::onPlateChange,
                label = { Text("Davlat raqami") },
                placeholder = { Text("01 A 777 AA") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { vm.save(onDone = onBack) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = s.isSaveEnabled && !s.isLoading
            ) {
                if (s.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Saqlash")
            }
        }
    }

    // --- DIALOGLAR ---

    if (showBrandDialog) {
        SimpleListDialog(
            title = "Markani tanlang",
            items = s.brands.map { it.name },
            onDismiss = { showBrandDialog = false },
            onSelected = { name ->
                val brand = s.brands.find { it.name == name }
                if (brand != null) vm.onBrandSelected(brand)
                showBrandDialog = false
            }
        )
    }

    if (showModelDialog) {
        SimpleListDialog(
            title = "Modelni tanlang",
            items = s.selectedBrand?.car_models?.map { it.name } ?: emptyList(),
            onDismiss = { showModelDialog = false },
            onSelected = { name ->
                vm.onModelSelected(name)
                showModelDialog = false
            }
        )
    }

    if (showColorDialog) {
        SimpleListDialog(
            title = "Rangni tanlang",
            items = CAR_COLORS,
            onDismiss = { showColorDialog = false },
            onSelected = { name ->
                vm.onColorSelected(name)
                showColorDialog = false
            }
        )
    }
}

// --- YORDAMCHI KOMPONENTLAR ---

@Composable
fun SelectionField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = if(isEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.surfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = isEnabled, onClick = onClick)
        )
    }
}

@Composable
fun SimpleListDialog(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(items) { item ->
                    ListItem(
                        headlineContent = { Text(item) },
                        modifier = Modifier.clickable { onSelected(item) }
                    )
                    Divider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Yopish") }
        }
    )
}