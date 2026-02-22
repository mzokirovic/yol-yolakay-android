package com.example.yol_yolakay.feature.profile.vehicle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

val CAR_COLORS = listOf("Oq", "Qora", "Kulrang", "Kumush", "Qizil", "Ko'k", "Yashil", "Boshqa")
private const val TOTAL_STEPS = 4
private enum class Step { Brand, Model, Color, Plate }
private enum class Mode { Summary, Wizard }
private const val OTHER_COLOR = "Boshqa"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(
    onBack: () -> Unit,
    vm: VehicleViewModel = viewModel(factory = VehicleViewModel.factory(LocalContext.current))
) {
    val s = vm.state
    val cs = MaterialTheme.colorScheme

    val hasSavedVehicle = remember(s.selectedBrand, s.selectedModelName, s.selectedColor, s.plateNumber) {
        s.selectedBrand != null && s.selectedModelName.isNotBlank() && s.selectedColor.isNotBlank() && s.plateNumber.isNotBlank()
    }

    var mode by rememberSaveable { mutableStateOf(Mode.Wizard) }
    var didInitMode by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableStateOf(Step.Brand) }
    var didInitStep by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.isLoading, didInitMode, hasSavedVehicle) {
        if (!s.isLoading && !didInitMode) {
            mode = if (hasSavedVehicle) Mode.Summary else Mode.Wizard
            didInitMode = true
        }
    }

    LaunchedEffect(mode, s.isLoading, didInitStep) {
        if (mode == Mode.Wizard && !s.isLoading && !didInitStep) {
            step = when {
                s.selectedBrand == null -> Step.Brand
                s.selectedModelName.isBlank() -> Step.Model
                s.selectedColor.isBlank() -> Step.Color
                else -> Step.Plate
            }
            didInitStep = true
        }
    }

    val stepIndex = when (step) { Step.Brand -> 0; Step.Model -> 1; Step.Color -> 2; Step.Plate -> 3 }
    val progress = (stepIndex + 1).toFloat() / TOTAL_STEPS.toFloat()

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (mode == Mode.Summary) "Mening avtomobilim" else "Avtomobil qo'shish",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (mode == Mode.Summary) onBack()
                            else {
                                when (step) {
                                    Step.Brand -> if (hasSavedVehicle) mode = Mode.Summary else onBack()
                                    Step.Model -> step = Step.Brand
                                    Step.Color -> step = Step.Model
                                    Step.Plate -> step = Step.Color
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                if (mode == Mode.Wizard) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = cs.onSurface,
                        trackColor = cs.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        },
        bottomBar = {
            if (mode == Mode.Wizard && step == Step.Plate) {
                Surface(
                    color = cs.surface,
                    border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                        Button(
                            onClick = { vm.save(onDone = { mode = Mode.Summary }) },
                            enabled = s.isSaveEnabled && !s.isLoading,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.onSurface)
                        ) {
                            if (s.isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = cs.surface, strokeWidth = 2.dp)
                            else Text("Saqlash", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (s.isLoading && s.brands.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.onSurface, strokeWidth = 2.dp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (mode == Mode.Summary && hasSavedVehicle) {
                    VehicleSummaryCard(
                        brand = s.selectedBrand?.name.orEmpty(),
                        model = s.selectedModelName,
                        color = s.selectedColor,
                        plate = s.plateNumber,
                        onEdit = { mode = Mode.Wizard; step = Step.Brand; didInitStep = true },
                        onDelete = { showDeleteConfirm = true }
                    )
                }
            }

            if (mode == Mode.Wizard) {
                item {
                    Text(
                        text = when(step) {
                            Step.Brand -> "Avtomobil markasini tanlang"
                            Step.Model -> "Modelni tanlang"
                            Step.Color -> "Rangni tanlang"
                            Step.Plate -> "Davlat raqamini kiriting"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StepIndicatorChip(s.selectedBrand?.name ?: "Marka", step == Step.Brand) { step = Step.Brand }
                        StepIndicatorChip(s.selectedModelName.ifBlank { "Model" }, step == Step.Model, s.selectedBrand != null) { step = Step.Model }
                        StepIndicatorChip(s.selectedColor.ifBlank { "Rang" }, step == Step.Color, s.selectedModelName.isNotBlank()) { step = Step.Color }
                    }
                }

                when (step) {
                    Step.Brand -> items(s.brands) { b ->
                        SelectableRow(b.name, s.selectedBrand?.id == b.id) {
                            vm.onBrandSelected(b)
                            step = Step.Model
                        }
                    }
                    Step.Model -> items(s.selectedBrand?.car_models?.map { it.name }.orEmpty()) { m ->
                        SelectableRow(m, s.selectedModelName == m) {
                            vm.onModelSelected(m)
                            step = Step.Color
                        }
                    }
                    Step.Color -> {
                        items(CAR_COLORS) { c ->
                            SelectableRow(c, s.selectedColor == c) {
                                if (c == OTHER_COLOR) vm.onColorSelected(OTHER_COLOR)
                                else { vm.onColorSelected(c); step = Step.Plate }
                            }
                        }
                        if (s.selectedColor == OTHER_COLOR || (s.selectedColor.isNotBlank() && s.selectedColor !in CAR_COLORS)) {
                            item {
                                CustomColorInput(
                                    value = if (s.selectedColor in CAR_COLORS) "" else s.selectedColor,
                                    onConfirm = { vm.onColorSelected(it); step = Step.Plate }
                                )
                            }
                        }
                    }
                    Step.Plate -> item {
                        OutlinedTextField(
                            value = s.plateNumber,
                            onValueChange = vm::onPlateChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("01 A 777 AA") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("O'chirish") },
            text = { Text("Avtomobil ma'lumotlarini o'chirmoqchimisiz?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.deleteVehicle { mode = Mode.Wizard; step = Step.Brand; didInitStep = true }
                }) { Text("O'chirish", color = cs.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Bekor") } }
        )
    }
}

@Composable
private fun VehicleSummaryCard(brand: String, model: String, color: String, plate: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(cs.onSurface.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.DirectionsCar, null, tint = cs.onSurface)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("$brand $model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$color • $plate", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Tahrirlash", color = cs.onSurface)
                }
                Button(onClick = onDelete, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = cs.error.copy(alpha = 0.1f))) {
                    Text("O'chirish", color = cs.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SelectableRow(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) cs.onSurface.copy(alpha = 0.05f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.1f)) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            if (isSelected) Icon(Icons.Outlined.Check, null, tint = cs.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StepIndicatorChip(text: String, isActive: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = if (enabled) onClick else ({}),
        shape = CircleShape,
        color = if (isActive) cs.onSurface else cs.onSurface.copy(alpha = 0.05f),
        contentColor = if (isActive) cs.surface else cs.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.4f),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CustomColorInput(value: String, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Rangni yozing (masalan: jigarrang)") },
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onConfirm(text) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
        ) { Text("Davom etish") }
    }
}