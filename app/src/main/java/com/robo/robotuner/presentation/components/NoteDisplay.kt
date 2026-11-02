package com.robo.robotuner.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.robo.robotuner.domain.model.DetectedPitch
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.AccentOrange
import com.robo.robotuner.ui.theme.AccentRed
import com.robo.robotuner.ui.theme.TextMuted
import com.robo.robotuner.ui.theme.TextPrimary
import com.robo.robotuner.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun NoteDisplay(
    pitch: DetectedPitch?,
    modifier: Modifier = Modifier
) {
    val noteColor by animateColorAsState(
        targetValue = when {
            pitch == null -> TextMuted
            pitch.isInTune -> AccentGreen
            pitch.isFlat -> AccentRed
            pitch.isSharp -> AccentOrange
            else -> TextPrimary
        },
        animationSpec = tween(150),
        label = "noteColor"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = pitch?.note?.displayName ?: "-",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = noteColor
            )
            if (pitch != null) {
                Text(
                    text = pitch.octave.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }

        Text(
            text = if (pitch != null) String.format(Locale.getDefault(), "%.1f Hz", pitch.frequency) else "- Hz",
            fontSize = 16.sp,
            color = TextMuted
        )
    }
}
