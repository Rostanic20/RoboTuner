package com.robo.robotuner.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.robo.robotuner.R
import com.robo.robotuner.presentation.tuner.TunerReading
import com.robo.robotuner.ui.theme.TextMuted
import com.robo.robotuner.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun ReadingDisplay(
    reading: TunerReading?,
    isListening: Boolean,
    gaugeSize: Dp,
    modifier: Modifier = Modifier
) {
    val noteSize = with(LocalDensity.current) { (gaugeSize * 0.26f).toSp() }
    val octaveSize = with(LocalDensity.current) { (gaugeSize * 0.09f).toSp() }
    val color by animateColorAsState(
        targetValue = reading?.let { tuningColor(it) } ?: TextMuted,
        animationSpec = tween(150),
        label = "readingColor"
    )

    if (reading == null) {
        Text(
            text = stringResource(if (isListening) R.string.play_a_string else R.string.tuner_off),
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
            modifier = modifier
        )
        return
    }

    val centsText = stringResource(R.string.cents_value, formatCents(reading.cents))
    val description = stringResource(
        R.string.reading_description,
        "${reading.note.displayName}${reading.octave}",
        centsText
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = reading.note.displayName,
                fontSize = noteSize,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = reading.octave.toString(),
                fontSize = octaveSize,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = gaugeSize * 0.04f)
            )
        }
        Text(
            text = centsText,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = stringResource(R.string.frequency_value, reading.frequency),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

fun formatCents(cents: Double): String {
    val rounded = cents.roundToInt()
    return if (rounded > 0) "+$rounded" else "$rounded"
}
