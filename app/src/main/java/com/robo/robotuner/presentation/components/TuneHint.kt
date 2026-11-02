package com.robo.robotuner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.robo.robotuner.R
import com.robo.robotuner.presentation.tuner.TunerReading

@Composable
fun TuneHint(reading: TunerReading?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
        if (reading == null) return@Box
        val color = tuningColor(reading)
        val (icon, label) = when {
            reading.isInTune -> Icons.Filled.Check to R.string.in_tune
            reading.isFlat -> Icons.Filled.KeyboardArrowUp to R.string.tune_up
            else -> Icons.Filled.KeyboardArrowDown to R.string.tune_down
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}
