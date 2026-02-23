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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
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
import androidx.compose.ui.text.input.KeyboardCapitalization
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

    // ✅ Boshlang'ich holat "Null" bo'ladi (Qotib qolish va miltillashning oldini oladi)
    var mode by rememberSaveable { mutableStateOf<Mode?>(null) }
    var step by rememberSaveable { mutableStateOf(Step.Brand) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    // ✅ Serverdan javob kelgachgina ekran holatini tanlaymiz
    LaunchedEffect(s.isLoading) {
        if (!s.isLoading && mode == null) {
            mode = if (hasSavedVehicle) Mode.Summary else Mode.Wizard
            step = when {
                s.selectedBrand == null -> Step.Brand
                s.selectedModelName.isBlank() -> Step.Model
                s.selectedColor.isBlank() -> Step.Color
                else -> Step.Plate
            }
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
                            text = when (mode) {
                                Mode.Summary -> "Mening avtomobilim"
                                Mode.Wizard -> "Avtomobil qo'shish"
                                null -> "Yuklanmoqda..." // ✅ Bo'sh vaqtda miltillamasligi uchun
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        // ✅ Ma'lumot aniq bo'lgandagina orqaga qaytish tugmasini ko'rsatamiz
                        if (mode != null) {
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
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                            }
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
                            if (s.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = cs.surface, strokeWidth = 2.dp)
                            } else {
                                Text("Saqlash", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        // ✅ Agar mode aniqlanmagan yoki yuklanayotgan bo'lsa, butun ekranda faqat aylanuvchi Loader turadi
        if (mode == null || (s.isLoading && s.brands.isEmpty())) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.onSurface, strokeWidth = 3.dp)
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
                        isLoading = s.isLoading,
                        onEdit = { mode = Mode.Wizard; step = Step.Brand },
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
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
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
                            onValueChange = { vm.onPlateChange(it.uppercase()) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("01 A 777 AA", color = cs.onSurfaceVariant.copy(alpha = 0.5f)) },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = cs.primary,
                                unfocusedBorderColor = cs.outlineVariant
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Raqamni bo'shliqlarsiz kiritishingiz ham mumkin (Masalan: 01A777AA)",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = cs.surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("O'chirish", fontWeight = FontWeight.Bold) },
            text = { Text("Haqiqatan ham avtomobil ma'lumotlarini o'chirib tashlamoqchimisiz?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.deleteVehicle { mode = Mode.Wizard; step = Step.Brand }
                }) { Text("O'chirish", color = cs.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Bekor", color = cs.onSurfaceVariant) }
            }
        )
    }
}

@Composable
private fun VehicleSummaryCard(
    brand: String,
    model: String,
    color: String,
    plate: String,
    isLoading: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(cs.onSurface.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = cs.onSurface, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("$brand $model", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = cs.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                            Text(color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(plate, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Tahrirlash", color = cs.onSurface, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDelete,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.errorContainer)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = cs.error, strokeWidth = 2.dp)
                    } else {
                        Text("O'chirish", color = cs.error, fontWeight = FontWeight.Bold)
                    }
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
        color = if (isSelected) cs.primary.copy(alpha = 0.08f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, cs.primary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
            if (isSelected) Icon(Icons.Rounded.Check, null, tint = cs.primary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun StepIndicatorChip(text: String, isActive: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = if (enabled) onClick else ({}),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) cs.onSurface else cs.onSurface.copy(alpha = 0.05f),
        contentColor = if (isActive) cs.surface else cs.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.4f),
        modifier = Modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
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
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onConfirm(text) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text("Davom etish", fontWeight = FontWeight.Bold) }
    }
}