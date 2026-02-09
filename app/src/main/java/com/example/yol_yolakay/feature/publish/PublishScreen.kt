package com.example.yol_yolakay.feature.publish

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.publish.steps.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    viewModel: PublishViewModel = viewModel(),
    onPublished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isPublished) {
        if (uiState.isPublished) {
            delay(900)
            onPublished()
            viewModel.resetAfterPublish()
        }
    }

    val progress = (uiState.currentStep.ordinal + 1) / 7f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(350),
        label = "publish_progress"
    )

    BackHandler(enabled = uiState.currentStep != PublishStep.FROM) {
        viewModel.onBack()
    }

    val stepTitle = remember(uiState.currentStep) {
        when (uiState.currentStep) {
            PublishStep.FROM -> "Jo‘nash joyi"
            PublishStep.TO -> "Manzil"
            PublishStep.DATE -> "Sana"
            PublishStep.TIME -> "Vaqt"
            PublishStep.PASSENGERS -> "O‘rinlar"
            PublishStep.PRICE -> "Narx"
            PublishStep.PREVIEW -> "Tekshirish"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(Modifier.fillMaxWidth()) {
                        Text("E’lon berish", style = MaterialTheme.typography.titleLarge)
                        Text(stepTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (uiState.currentStep != PublishStep.FROM) {
                        IconButton(onClick = viewModel::onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                        }
                    }
                }
            )
        },
        bottomBar = {
            val isLast = uiState.currentStep == PublishStep.PREVIEW

            Surface(tonalElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = viewModel::onNext,
                        enabled = uiState.isNextEnabled && !uiState.isPublishing && !uiState.isPublished,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        if (uiState.isPublishing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Yuborilyapti…")
                        } else {
                            Text(if (isLast) "E’lon qilish" else "Davom etish")
                        }
                    }

                    uiState.publishError?.let {
                        Spacer(Modifier.height(10.dp))
                        AssistChip(
                            onClick = { /* dismiss qilmoqchi bo‘lsangiz state qo‘shamiz */ },
                            label = { Text(it) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }

                    if (uiState.isPublished) {
                        Spacer(Modifier.height(10.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text("✅ E’lon muvaffaqiyatli yaratildi") },
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = uiState.currentStep,
                label = "PublishWizard",
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (forward) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                }
            ) { step ->
                Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    when (step) {
                        PublishStep.FROM -> Step1From(
                            currentLocation = uiState.draft.fromLocation,
                            onLocationSelected = viewModel::onFromSelected,
                            suggestions = uiState.popularPoints
                        )
                        PublishStep.TO -> Step2To(
                            currentLocation = uiState.draft.toLocation,
                            onLocationSelected = viewModel::onToSelected,
                            suggestions = uiState.popularPoints
                        )
                        PublishStep.DATE -> Step3Date(uiState.draft.date, viewModel::onDateChange)
                        PublishStep.TIME -> Step4Time(uiState.draft.time, viewModel::onTimeChange)
                        PublishStep.PASSENGERS -> Step5Passengers(uiState.draft.passengers, viewModel::onPassengersChange)
                        PublishStep.PRICE -> Step6Price(
                            uiState = uiState,
                            onPriceChange = viewModel::onPriceChange,
                            onAdjustPrice = viewModel::adjustPrice
                        )
                        PublishStep.PREVIEW -> Step7Preview(uiState)
                    }
                }
            }
        }
    }
}
