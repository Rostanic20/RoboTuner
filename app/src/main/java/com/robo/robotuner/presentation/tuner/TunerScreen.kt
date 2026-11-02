package com.robo.robotuner.presentation.tuner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robo.robotuner.R
import com.robo.robotuner.presentation.components.MicButton
import com.robo.robotuner.presentation.components.ReadingDisplay
import com.robo.robotuner.presentation.components.StatusAction
import com.robo.robotuner.presentation.components.StatusMessage
import com.robo.robotuner.presentation.components.StringSelector
import com.robo.robotuner.presentation.components.TuneHint
import com.robo.robotuner.presentation.components.TunerGauge
import com.robo.robotuner.ui.theme.DarkBackground
import com.robo.robotuner.ui.theme.TextSecondary

private enum class MicPermission { Unknown, Denied, PermanentlyDenied }

private const val MAX_GAUGE_SIZE = 380

@Composable
fun TunerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: TunerViewModel = viewModel(factory = TunerViewModelFactory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var permission by rememberSaveable { mutableStateOf(MicPermission.Unknown) }
    var autoStartDone by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = when {
            granted -> MicPermission.Unknown
            context.canAskForMicAgain() -> MicPermission.Denied
            else -> MicPermission.PermanentlyDenied
        }
        if (granted) viewModel.startListening()
    }
    val requestMic = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    LaunchedEffect(Unit) {
        if (autoStartDone) return@LaunchedEffect
        autoStartDone = true
        if (context.hasMicPermission()) viewModel.startListening() else requestMic()
    }

    LifecycleStartEffect(viewModel) {
        if (permission != MicPermission.Unknown && context.hasMicPermission()) {
            permission = MicPermission.Unknown
            viewModel.startListening()
        } else {
            viewModel.onScreenVisible()
        }
        onStopOrDispose { viewModel.onScreenHidden() }
    }

    val view = LocalView.current
    DisposableEffect(state.isListening) {
        view.keepScreenOn = state.isListening
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(viewModel) {
        viewModel.inTuneEvents.collect { context.vibrateInTune() }
    }

    val onMicClick = {
        when {
            state.isListening -> viewModel.stopListening()
            context.hasMicPermission() -> viewModel.startListening()
            permission == MicPermission.PermanentlyDenied -> context.openAppSettings()
            else -> requestMic()
        }
    }
    val statusMessage = if (permission != MicPermission.Unknown) stringResource(R.string.mic_needed) else state.error
    val statusAction = when (permission) {
        MicPermission.Denied -> StatusAction(R.string.allow_mic, requestMic)
        MicPermission.PermanentlyDenied -> StatusAction(R.string.open_settings) { context.openAppSettings() }
        MicPermission.Unknown -> null
    }

    TunerContent(
        state = state,
        onMicClick = onMicClick,
        onStringClick = viewModel::toggleStringLock,
        status = { StatusMessage(statusMessage, statusAction) },
        modifier = modifier
    )
}

@Composable
private fun TunerContent(
    state: TunerUiState,
    onMicClick: () -> Unit,
    onStringClick: (Int) -> Unit,
    status: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val strings: @Composable (Modifier) -> Unit = { m ->
            StringSelector(
                strings = state.strings,
                reading = state.reading,
                lockedString = state.lockedString,
                tunedStrings = state.tunedStrings,
                onStringClick = onStringClick,
                modifier = m
            )
        }
        val controls: @Composable ColumnScope.() -> Unit = {
            status()
            MicButton(isListening = state.isListening, onClick = onMicClick)
        }

        if (maxWidth > maxHeight) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GaugeArea(state, Modifier.weight(1f).fillMaxHeight())
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    strings(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    controls()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Spacer(Modifier.height(16.dp))
                strings(Modifier.fillMaxWidth())
                GaugeArea(state, Modifier.weight(1f).fillMaxWidth())
                controls()
            }
        }
    }
}

@Composable
private fun GaugeArea(state: TunerUiState, modifier: Modifier) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side: Dp = minOf(maxWidth, maxHeight, MAX_GAUGE_SIZE.dp)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(side)) {
            TunerGauge(reading = state.reading, modifier = Modifier.fillMaxSize())
            ReadingDisplay(reading = state.reading, isListening = state.isListening, gaugeSize = side)
            TuneHint(
                reading = state.reading,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = side * 0.04f)
            )
        }
    }
}
