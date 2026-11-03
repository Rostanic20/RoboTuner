package com.robo.robotuner.presentation.tuner

import android.Manifest
import android.widget.Toast
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
import com.robo.robotuner.presentation.components.SettingsActions
import com.robo.robotuner.presentation.components.SettingsSheet
import com.robo.robotuner.presentation.components.TopBar
import com.robo.robotuner.presentation.components.ReadingDisplay
import com.robo.robotuner.presentation.components.StatusAction
import com.robo.robotuner.presentation.components.StatusMessage
import com.robo.robotuner.presentation.components.StringSelector
import com.robo.robotuner.presentation.components.TuneHint
import com.robo.robotuner.presentation.components.TunerGauge
import com.robo.robotuner.presentation.components.TuningSheet
import com.robo.robotuner.presentation.settings.SettingsViewModel
import com.robo.robotuner.ui.theme.DarkBackground

private enum class MicPermission { Unknown, Denied, PermanentlyDenied }

private enum class Sheet { None, Tunings, Settings }

private const val MAX_GAUGE_SIZE = 380

@Composable
fun TunerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: TunerViewModel = viewModel(factory = TunerViewModelFactory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var permission by rememberSaveable { mutableStateOf(MicPermission.Unknown) }
    var autoStartDone by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(Sheet.None) }

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
        viewModel.events.collect { event ->
            when (event) {
                TunerEvent.STRING_TUNED -> context.vibrateInTune()
                TunerEvent.ALL_TUNED -> context.vibrateAllTuned()
                TunerEvent.TONE_FAILED -> Toast.makeText(context, R.string.tone_failed, Toast.LENGTH_SHORT).show()
            }
        }
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
        onStringLongClick = viewModel::playReference,
        onOpenTunings = { sheet = Sheet.Tunings },
        onOpenSettings = { sheet = Sheet.Settings },
        status = { StatusMessage(statusMessage, statusAction) },
        modifier = modifier
    )

    if (sheet != Sheet.None) {
        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(context))
        val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
        val closeSheet = { sheet = Sheet.None }
        when (sheet) {
            Sheet.Tunings -> TuningSheet(
                selected = settings.tuning,
                capo = settings.capo,
                useFlats = settings.useFlats,
                onSelect = settingsViewModel::setTuning,
                onCapo = settingsViewModel::setCapo,
                onDismiss = closeSheet
            )
            Sheet.Settings -> SettingsSheet(
                settings = settings,
                actions = SettingsActions(
                    onReferenceA4 = settingsViewModel::setReferenceA4,
                    onTolerance = settingsViewModel::setTolerance,
                    onVibration = settingsViewModel::setVibration,
                    onUseFlats = settingsViewModel::setUseFlats
                ),
                onDismiss = closeSheet
            )
            Sheet.None -> Unit
        }
    }
}

@Composable
private fun TunerContent(
    state: TunerUiState,
    onMicClick: () -> Unit,
    onStringClick: (Int) -> Unit,
    onStringLongClick: (Int) -> Unit,
    onOpenTunings: () -> Unit,
    onOpenSettings: () -> Unit,
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
                playingString = state.playingString,
                allTuned = state.allTuned,
                useFlats = state.settings.useFlats,
                onStringClick = onStringClick,
                onStringLongClick = onStringLongClick,
                modifier = m
            )
        }
        val topBar: @Composable () -> Unit = {
            TopBar(
                tuning = state.settings.tuning,
                capo = state.settings.capo,
                onOpenTunings = onOpenTunings,
                onOpenSettings = onOpenSettings
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
                    topBar()
                    Spacer(Modifier.height(8.dp))
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
                topBar()
                Spacer(Modifier.height(8.dp))
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
            TunerGauge(
                reading = state.reading,
                toleranceCents = state.settings.toleranceCents,
                modifier = Modifier.fillMaxSize()
            )
            ReadingDisplay(
                reading = state.reading,
                playing = state.playingString?.let(state.strings::getOrNull),
                isListening = state.isListening,
                useFlats = state.settings.useFlats,
                gaugeSize = side
            )
            TuneHint(
                reading = state.reading,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = side * 0.04f)
            )
        }
    }
}
