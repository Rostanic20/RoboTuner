package com.robo.robotuner.presentation.components

import androidx.compose.ui.graphics.Color
import com.robo.robotuner.presentation.tuner.TunerReading
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.AccentOrange
import com.robo.robotuner.ui.theme.AccentRed

fun tuningColor(reading: TunerReading): Color = when {
    reading.isInTune -> AccentGreen
    reading.isFlat -> AccentRed
    else -> AccentOrange
}
