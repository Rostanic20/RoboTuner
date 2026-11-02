package com.robo.robotuner.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.robo.robotuner.R
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.DarkBackground
import com.robo.robotuner.ui.theme.DarkCard
import com.robo.robotuner.ui.theme.TextMuted
import com.robo.robotuner.ui.theme.TextSecondary

@Composable
fun MicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background by animateColorAsState(
        targetValue = if (isListening) AccentGreen else DarkCard,
        animationSpec = tween(200),
        label = "micBackground"
    )
    val pulse = rememberInfiniteTransition(label = "micPulse")
    val pulseProgress by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "micPulseProgress"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .drawBehind {
                    if (isListening) {
                        val base = 36.dp.toPx()
                        drawCircle(
                            color = AccentGreen.copy(alpha = 0.35f * (1f - pulseProgress)),
                            radius = base + (size.minDimension / 2 - base) * pulseProgress
                        )
                    }
                }
        ) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = background,
                border = if (isListening) null else BorderStroke(2.dp, TextMuted),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = stringResource(if (isListening) R.string.stop_tuner else R.string.start_tuner),
                        tint = if (isListening) DarkBackground else TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(if (isListening) R.string.listening else R.string.tap_to_start),
            style = MaterialTheme.typography.labelSmall,
            color = if (isListening) AccentGreen else TextMuted
        )
    }
}
