package com.example.yol_yolakay.feature.profile.vehicle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

val CAR_COLORS = listOf(
    "Oq (White)",
    "Qora (Black)",
    "Kulrang (Grey)",
    "Kumush (Silver)",
    "Qizil",
    "Ko'k",
    "Yashil",
    "Boshqa"
)

private const val TOTAL_STEPS = 4 // Brand, Model, Color, Plate

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

    // "Saqlangan avto bor" deb hisoblash (single-vehicle backend uchun)
    val hasSavedVehicle = remember(s.selectedBrand, s.selectedModelName, s.selectedColor, s.plateNumber) {
        s.selectedBrand != null &&
                s.selectedModelName.isNotBlank() &&
                s.selectedColor.isNotBlank() &&
                s.plateNumber.isNotBlank()
    }

    var mode by rememberSaveable { mutableStateOf(Mode.Wizard) }
    var didInitMode by rememberSaveable { mutableStateOf(false) }

    var step by rememberSaveable { mutableStateOf(Step.Brand) }
    var didInitStep by rememberSaveable { mutableStateOf(false) }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    // 1) Screen ochilganda: agar saved bo'lsa Summary, bo'lmasa Wizard
    LaunchedEffect(s.isLoading, didInitMode, hasSavedVehicle) {
        if (!s.isLoading && !didInitMode) {
            mode = if (hasSavedVehicle) Mode.Summary else Mode.Wizard
            didInitMode = true
        }
    }

    // 2) Wizard ochilganda (faqat bir marta): mavjud state'ga qarab step tanlash
    LaunchedEffect(mode, s.isLoading, didInitStep, s.selectedBrand, s.selectedModelName, s.selectedColor) {
        if (mode == Mode.Wizard && !s.isLoading && !didInitStep) {
            step = firstIncompleteStep(
                hasBrand = s.selectedBrand != null,
                hasModel = s.selectedModelName.isNotBlank(),
                hasColor = s.selectedColor.isNotBlank()
            )
            didInitStep = true
        }
    }

    val isInitialLoading = s.isLoading && s.brands.isEmpty()

    // Wizard progress
    val stepIndex = when (step) {
        Step.Brand -> 0
        Step.Model -> 1
        Step.Color -> 2
        Step.Plate -> 3
    }
    val progress = (stepIndex + 1).toFloat() / TOTAL_STEPS.toFloat()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Avtomobil") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (mode) {
                                Mode.Summary -> onBack()
                                Mode.Wizard -> {
                                    when (step) {
                                        Step.Brand -> {
                                            // agar edit rejimida bo'lsak, Brand stepda back -> Summary
                                            if (hasSavedVehicle) {
                                                mode = Mode.Summary
                                            } else {
                                                onBack()
                                            }
                                        }
                                        Step.Model -> step = Step.Brand
                                        Step.Color -> step = Step.Model
                                        Step.Plate -> step = Step.Color
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                }
            )
        },
        containerColor = cs.background,
        bottomBar = {
            // Faqat Wizard + Oxirgi stepda “Saqlash”
            if (mode == Mode.Wizard && step == Step.Plate) {
                Surface(color = cs.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 14.dp)
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = { vm.save(onDone = { mode = Mode.Summary }) },
                            enabled = s.isSaveEnabled && !s.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            if (s.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("Saqlash")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->

        if (isInitialLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            // Minimal error card
            s.error?.let {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = cs.errorContainer,
                    contentColor = cs.onErrorContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(it, modifier = Modifier.padding(14.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            when (mode) {
                Mode.Summary -> {
                    Spacer(Modifier.height(12.dp))

                    if (hasSavedVehicle) {
                        SavedVehicleCard(
                            brand = s.selectedBrand?.name.orEmpty(),
                            model = s.selectedModelName,
                            color = s.selectedColor,
                            plate = s.plateNumber,
                            onOpen = {
                                // Edit / view -> wizard
                                mode = Mode.Wizard
                                step = Step.Brand
                                didInitStep = true // biz stepni qo'lda belgiladik
                            },
                            onEdit = {
                                mode = Mode.Wizard
                                step = Step.Brand
                                didInitStep = true
                            },
                            onDelete = { showDeleteConfirm = true }
                        )
                    } else {
                        // Saved yo'q bo'lsa darhol wizardga o'tamiz
                        mode = Mode.Wizard
                        didInitStep = false
                    }

                    Spacer(Modifier.height(12.dp))
                }

                Mode.Wizard -> {
                    Spacer(Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    // Selected summary (tap qilib stepga qaytish)
                    SelectedSummary(
                        brand = s.selectedBrand?.name,
                        model = s.selectedModelName,
                        color = s.selectedColor,
                        onBrandClick = { step = Step.Brand },
                        onModelClick = { if (s.selectedBrand != null) step = Step.Model },
                        onColorClick = { step = Step.Color }
                    )

                    Spacer(Modifier.height(12.dp))

                    StepCard {
                        when (step) {
                            Step.Brand -> {
                                StepTitle("Markani tanlang")
                                Spacer(Modifier.height(8.dp))
                                LazyColumn {
                                    items(s.brands, key = { it.id }) { b ->
                                        SelectRow(
                                            title = b.name,
                                            selected = (s.selectedBrand?.id == b.id),
                                            onClick = {
                                                vm.onBrandSelected(b)
                                                step = Step.Model
                                            }
                                        )
                                    }
                                }
                            }

                            Step.Model -> {
                                StepTitle("Modelni tanlang")
                                Spacer(Modifier.height(8.dp))
                                val models = s.selectedBrand?.car_models?.map { it.name }.orEmpty()
                                LazyColumn {
                                    items(models, key = { it }) { m ->
                                        SelectRow(
                                            title = m,
                                            selected = (s.selectedModelName == m),
                                            onClick = {
                                                vm.onModelSelected(m)
                                                step = Step.Color
                                            }
                                        )
                                    }
                                }
                            }

                            Step.Color -> {
                                StepTitle("Rangni tanlang")
                                Spacer(Modifier.height(8.dp))

                                // Agar user custom rang kiritgan bo'lsa (listda yo'q), "Boshqa" selected bo'lib turadi
                                val selectedColorKey = remember(s.selectedColor) {
                                    when {
                                        s.selectedColor.isBlank() -> ""
                                        s.selectedColor in CAR_COLORS -> s.selectedColor
                                        else -> OTHER_COLOR
                                    }
                                }

                                var customColor by rememberSaveable { mutableStateOf("") }

                                // Color stepga kirganda: agar custom rang bo'lsa input'ni prefill qilamiz
                                LaunchedEffect(step, s.selectedColor) {
                                    if (step == Step.Color) {
                                        if (s.selectedColor.isNotBlank() && s.selectedColor !in CAR_COLORS && s.selectedColor != OTHER_COLOR) {
                                            customColor = s.selectedColor
                                        }
                                    }
                                }

                                LazyColumn {
                                    items(CAR_COLORS, key = { it }) { c ->
                                        SelectRow(
                                            title = c,
                                            selected = (selectedColorKey == c),
                                            onClick = {
                                                if (c == OTHER_COLOR) {
                                                    vm.onColorSelected(OTHER_COLOR)
                                                    // step o'zgarmaydi, user custom rang yozadi
                                                } else {
                                                    vm.onColorSelected(c)
                                                    customColor = ""
                                                    step = Step.Plate
                                                }
                                            }
                                        )
                                    }
                                }

                                val isOtherSelected = selectedColorKey == OTHER_COLOR

                                if (isOtherSelected) {
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = customColor,
                                        onValueChange = { customColor = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Rang") },
                                        placeholder = { Text("Masalan: bordo / to‘q ko‘k") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Done
                                        )
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            val v = customColor.trim()
                                            if (v.isNotBlank()) {
                                                vm.onColorSelected(v) // "Boshqa" o'rniga real rang
                                                step = Step.Plate
                                            }
                                        },
                                        enabled = customColor.trim().isNotBlank(),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        Text("Davom etish")
                                    }
                                }
                            }

                            Step.Plate -> {
                                StepTitle("Davlat raqami")
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = s.plateNumber,
                                    onValueChange = vm::onPlateChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Raqam") },
                                    placeholder = { Text("01 A 777 AA") },
                                    singleLine = true,
                                    enabled = !s.isLoading,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Done
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }

    // Delete confirm
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Avtomobilni o‘chirish") },
            text = { Text("Haqiqatan ham o‘chirmoqchimisiz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        vm.deleteVehicle(
                            onDone = {
                                // o‘chdi -> wizard qaytadan ochiladi
                                mode = Mode.Wizard
                                step = Step.Brand
                                didInitStep = true
                            }
                        )
                    }
                ) { Text("O‘chirish", color = cs.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Bekor") }
            }
        )
    }
}

private fun firstIncompleteStep(hasBrand: Boolean, hasModel: Boolean, hasColor: Boolean): Step =
    when {
        !hasBrand -> Step.Brand
        !hasModel -> Step.Model
        !hasColor -> Step.Color
        else -> Step.Plate
    }

@Composable
private fun SavedVehicleCard(
    brand: String,
    model: String,
    color: String,
    plate: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "$brand • $model",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$color • $plate",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) { Text("Tahrirlash") }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error)
                ) { Text("O‘chirish", color = Color.White) }
            }
        }
    }
}

@Composable
private fun StepCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content
        )
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SelectRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = cs.primary)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = cs.onSurface
        )
    )
    HorizontalDivider(color = cs.outlineVariant)
}

@Composable
private fun SelectedSummary(
    brand: String?,
    model: String,
    color: String,
    onBrandClick: () -> Unit,
    onModelClick: () -> Unit,
    onColorClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryChip(text = brand ?: "Marka", enabled = true, onClick = onBrandClick)
        SummaryChip(text = if (model.isBlank()) "Model" else model, enabled = brand != null, onClick = onModelClick)
        SummaryChip(text = if (color.isBlank()) "Rang" else color, enabled = true, onClick = onColorClick)
    }
}

@Composable
private fun SummaryChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = cs.surfaceVariant,
            labelColor = cs.onSurface
        )
    )
}