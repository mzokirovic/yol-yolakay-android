package com.example.yol_yolakay.feature.publish

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yol_yolakay.core.session.CurrentUser
import com.example.yol_yolakay.feature.publish.steps.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    viewModel: PublishViewModel = viewModel(),
    onPublished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // ✅ Seam ulash: 1 marta set qilib qo‘yamiz
    val ctx = LocalContext.current
    LaunchedEffect(ctx) {
        viewModel.setCurrentUser(CurrentUser.id(ctx))
    }

    LaunchedEffect(uiState.isPublished) {
        if (uiState.isPublished) {
            delay(1200)
            onPublished()
            viewModel.resetAfterPublish()
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 500),
        label = "ProgressBarAnimation"
    )

    BackHandler(enabled = uiState.currentStep != PublishStep.FROM) {
        viewModel.onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp).height(8.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                },
                navigationIcon = {
                    AnimatedVisibility(
                        visible = uiState.currentStep != PublishStep.FROM,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        IconButton(onClick = { viewModel.onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga")
                        }
                    }
                }
            )
        },
        bottomBar = {
            val isLast = uiState.currentStep == PublishStep.PREVIEW

            Button(
                onClick = { viewModel.onNext() },
                enabled = uiState.isNextEnabled && !uiState.isPublishing && !uiState.isPublished,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Yuborilyapti...")
                } else {
                    Text(if (isLast) "E'lon qilish" else "Davom etish")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
        ) {

            if (uiState.publishError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.publishError!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (uiState.isPublished) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "✅ E'lon yaratildi!",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedContent(
                targetState = uiState.currentStep,
                label = "WizardTransition",
                transitionSpec = {
                    val isMovingForward = targetState.ordinal > initialState.ordinal

                    if (isMovingForward) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(SizeTransform(clip = false))
                }
            ) { step ->
                when (step) {
                    PublishStep.FROM -> Step1From(uiState.fromLocation, viewModel::onFromChange)
                    PublishStep.TO -> Step2To(uiState.toLocation, viewModel::onToChange)
                    PublishStep.DATE -> Step3Date(uiState.date, viewModel::onDateChange)
                    PublishStep.TIME -> Step4Time(uiState.time, viewModel::onTimeChange)
                    PublishStep.PASSENGERS -> Step5Passengers(uiState.passengers, viewModel::onPassengersChange)
                    PublishStep.PRICE -> Step6Price(uiState.price, viewModel::onPriceChange)
                    PublishStep.PREVIEW -> Step7Preview(uiState)
                }
            }
        }
    }
}
