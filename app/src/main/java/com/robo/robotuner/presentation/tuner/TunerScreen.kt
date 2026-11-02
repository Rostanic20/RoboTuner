package com.robo.robotuner.presentation.tuner

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robo.robotuner.domain.model.TunerState
import com.robo.robotuner.presentation.components.GuitarStringIndicator
import com.robo.robotuner.presentation.components.NoteDisplay
import com.robo.robotuner.presentation.components.TunerGauge
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.AccentOrange
import com.robo.robotuner.ui.theme.AccentRed
import com.robo.robotuner.ui.theme.DarkBackground
import com.robo.robotuner.ui.theme.DarkCard
import com.robo.robotuner.ui.theme.TextMuted
import com.robo.robotuner.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun TunerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: TunerViewModel = viewModel(factory = TunerViewModelFactory(context))

    val tunerState by viewModel.tunerState.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()

    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            showPermissionRationale = true
        }
    }

    LifecycleStartEffect(viewModel) {
        viewModel.onScreenVisible()
        onStopOrDispose { viewModel.onScreenHidden() }
    }

    val currentPitch = (tunerState as? TunerState.Detected)?.pitch
    val isInTune = currentPitch?.isInTune == true

    LaunchedEffect(isInTune) {
        if (isInTune) {
            delay(IN_TUNE_VIBRATION_DELAY_MS)
            vibrateInTune(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "RoboTuner",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // Guitar string indicator
            GuitarStringIndicator(
                currentPitch = currentPitch,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(contentAlignment = Alignment.Center) {
                TunerGauge(
                    cents = currentPitch?.cents ?: 0.0,
                    isInTune = isInTune,
                    isActive = isListening
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NoteDisplay(pitch = currentPitch)

                    Spacer(modifier = Modifier.height(8.dp))

                    val centsText = currentPitch?.let {
                        val rounded = it.cents.roundToInt()
                        if (rounded > 0) "+$rounded" else "$rounded"
                    } ?: "-"

                    val centsColor by animateColorAsState(
                        targetValue = when {
                            currentPitch == null -> TextMuted
                            isInTune -> AccentGreen
                            currentPitch.isFlat -> AccentRed
                            else -> AccentOrange
                        },
                        animationSpec = tween(150),
                        label = "centsColor"
                    )

                    Text(
                        text = "$centsText cents",
                        fontSize = 18.sp,
                        color = centsColor
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            val errorText = when {
                showPermissionRationale -> "Microphone access needed"
                tunerState is TunerState.Error -> (tunerState as TunerState.Error).message
                else -> null
            }
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = AccentRed,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            val buttonColor by animateColorAsState(
                targetValue = if (isListening) AccentGreen else DarkCard,
                animationSpec = tween(200),
                label = "buttonColor"
            )

            val buttonBorderColor by animateColorAsState(
                targetValue = if (isListening) AccentGreen else TextMuted,
                animationSpec = tween(200),
                label = "borderColor"
            )

            val buttonScale by animateFloatAsState(
                targetValue = if (isListening) 1.1f else 1f,
                animationSpec = tween(200),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size((64 * buttonScale).dp)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .border(2.dp, buttonBorderColor, CircleShape)
                    .clickable {
                        if (isListening) {
                            viewModel.stopListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isListening) "ON" else "OFF",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isListening) DarkBackground else TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private const val IN_TUNE_VIBRATION_DELAY_MS = 400L

@Suppress("DEPRECATION")
private fun vibrateInTune(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(50)
    }
}
