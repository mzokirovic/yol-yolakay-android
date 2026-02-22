package com.example.yol_yolakay.feature.publish

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.feature.publish.components.PublishSuccessDialog
import com.example.yol_yolakay.feature.publish.steps.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    viewModel: PublishViewModel = viewModel(),
    onPublished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(350),
        label = "publish_progress"
    )

    BackHandler(enabled = uiState.currentStep != PublishStep.FROM) {
        viewModel.onBack()
    }

    val stepTitle = remember(uiState.currentStep) {
        when (uiState.currentStep) {
            PublishStep.FROM -> "Qayerdan?"
            PublishStep.TO -> "Qayerga?"
            PublishStep.DATE -> "Sana"
            PublishStep.TIME -> "Vaqt"
            PublishStep.PASSENGERS -> "O‘rinlar soni"
            PublishStep.PRICE -> "Narx"
            PublishStep.PREVIEW -> "Tekshirish"
        }
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "E’lon berish",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stepTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        if (uiState.currentStep != PublishStep.FROM) {
                            IconButton(onClick = viewModel::onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Orqaga"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
                // ✅ Nafis progress bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = cs.onSurface,
                    trackColor = cs.outlineVariant.copy(alpha = 0.4f)
                )
            }
        },
        bottomBar = {
            val isLast = uiState.currentStep == PublishStep.PREVIEW

            Surface(
                color = cs.surface,
                border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .navigationBarsPadding()
                ) {
                    // ✅ Validation hint (nafisroq qilingan)
                    val hint = if (uiState.currentStep == PublishStep.PREVIEW)
                        uiState.publishValidationMessage else uiState.validationMessage

                    if (!uiState.isPublishing && !uiState.isPublished && hint != null) {
                        Text(
                            text = hint,
                            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
                            color = cs.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    uiState.publishError?.let { err ->
                        Surface(
                            color = cs.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                err,
                                modifier = Modifier.padding(12.dp),
                                color = cs.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // ✅ Premium Black Button
                    Button(
                        onClick = viewModel::onNext,
                        enabled = uiState.isNextEnabled && !uiState.isPublishing && !uiState.isPublished,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.onSurface, // Qora tugma
                            contentColor = cs.surface      // Oq matn
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        if (uiState.isPublishing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp),
                                color = cs.surface
                            )
                        } else {
                            Text(
                                text = if (isLast) "E’lon qilish" else "Davom etish",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = uiState.currentStep,
                label = "PublishWizard",
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (forward) {
                        slideInHorizontally { it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 2 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 2 } + fadeOut()
                    }.using(SizeTransform(clip = false))
                }
            ) { step ->
                Box(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
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
                        PublishStep.PREVIEW -> Step7Preview(
                            uiState = uiState,
                            onEditStep = viewModel::goToStep
                        )
                    }
                }
            }

            if (uiState.isPublished) {
                PublishSuccessDialog(
                    onGoMyTrips = {
                        onPublished()
                        viewModel.resetAfterPublish()
                    },
                    onPublishAnother = {
                        viewModel.resetAfterPublish()
                    }
                )
            }
        }
    }
}