package com.robo.robotuner.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robo.robotuner.domain.model.DetectedPitch
import com.robo.robotuner.domain.model.Note
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.DarkCard
import com.robo.robotuner.ui.theme.TextMuted
import com.robo.robotuner.ui.theme.TextPrimary
import kotlin.math.abs

data class GuitarString(
    val name: String,
    val note: Note,
    val octave: Int
) {
    val frequency: Double = Note.getFrequency(note, octave)
}

val standardGuitarStrings = listOf(
    GuitarString("E", Note.E, 2),
    GuitarString("A", Note.A, 2),
    GuitarString("D", Note.D, 3),
    GuitarString("G", Note.G, 3),
    GuitarString("B", Note.B, 3),
    GuitarString("E", Note.E, 4)
)

@Composable
fun GuitarStringIndicator(
    currentPitch: DetectedPitch?,
    modifier: Modifier = Modifier
) {
    val detectedString = currentPitch?.let { pitch ->
        findClosestString(pitch.frequency)
    }

    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        standardGuitarStrings.forEachIndexed { index, guitarString ->
            val isActive = detectedString == guitarString
            val isInTune = isActive && currentPitch?.isInTune == true

            StringIndicator(
                stringNumber = 6 - index, // 6, 5, 4, 3, 2, 1
                stringName = guitarString.name,
                isActive = isActive,
                isInTune = isInTune
            )
        }
    }
}

@Composable
private fun StringIndicator(
    stringNumber: Int,
    stringName: String,
    isActive: Boolean,
    isInTune: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isInTune -> AccentGreen
            isActive -> AccentGreen.copy(alpha = 0.3f)
            else -> DarkCard
        },
        animationSpec = tween(150),
        label = "stringBg"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isInTune -> DarkCard
            isActive -> AccentGreen
            else -> TextMuted
        },
        animationSpec = tween(150),
        label = "stringText"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringNumber.toString(),
            fontSize = 10.sp,
            color = if (isActive) TextPrimary else TextMuted
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private fun findClosestString(frequency: Double): GuitarString? {
    if (frequency < 60 || frequency > 400) return null

    var closest: GuitarString? = null
    var minCentsDiff = Double.MAX_VALUE

    for (guitarString in standardGuitarStrings) {
        val cents = abs(1200 * kotlin.math.log2(frequency / guitarString.frequency))
        // Only match if within 100 cents (1 semitone)
        if (cents < minCentsDiff && cents < 100) {
            minCentsDiff = cents
            closest = guitarString
        }
    }

    return closest
}
