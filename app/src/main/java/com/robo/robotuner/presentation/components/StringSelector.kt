package com.robo.robotuner.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robo.robotuner.R
import com.robo.robotuner.domain.model.GuitarString
import com.robo.robotuner.presentation.tuner.TunerReading
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.DarkBackground
import com.robo.robotuner.ui.theme.DarkCard
import com.robo.robotuner.ui.theme.TextMuted
import com.robo.robotuner.ui.theme.TextPrimary
import com.robo.robotuner.ui.theme.TextSecondary

@Composable
fun StringSelector(
    strings: List<GuitarString>,
    reading: TunerReading?,
    lockedString: Int?,
    tunedStrings: Set<Int>,
    useFlats: Boolean,
    onStringClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            strings.forEachIndexed { index, string ->
                StringChip(
                    number = strings.size - index,
                    string = string,
                    isActive = reading?.stringIndex == index,
                    isInTune = reading?.stringIndex == index && reading.isInTune,
                    isLocked = lockedString == index,
                    isTuned = index in tunedStrings,
                    useFlats = useFlats,
                    onClick = { onStringClick(index) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (lockedString == null) {
                stringResource(R.string.mode_auto)
            } else {
                stringResource(R.string.mode_locked, strings.size - lockedString)
            },
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun StringChip(
    number: Int,
    string: GuitarString,
    isActive: Boolean,
    isInTune: Boolean,
    isLocked: Boolean,
    isTuned: Boolean,
    useFlats: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = when {
            isInTune -> AccentGreen
            isActive -> AccentGreen.copy(alpha = 0.25f)
            else -> DarkCard
        },
        animationSpec = tween(150),
        label = "chipBackground"
    )
    val content = when {
        isInTune -> DarkBackground
        isActive -> AccentGreen
        else -> TextSecondary
    }
    val states = listOfNotNull(
        stringResource(R.string.string_state_locked).takeIf { isLocked },
        stringResource(R.string.string_state_tuned).takeIf { isTuned },
        stringResource(R.string.string_state_active).takeIf { isActive }
    )
    val description = stringResource(
        R.string.string_chip_description,
        number,
        (listOf("${string.note.label(useFlats)}${string.octave}") + states).joinToString(", ")
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) TextPrimary else TextMuted
        )
        Spacer(Modifier.height(4.dp))
        Box {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = background,
                border = if (isLocked) BorderStroke(2.dp, AccentGreen) else null,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = description }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = string.note.label(useFlats),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                }
            }
            val badge = when {
                isLocked -> Icons.Filled.Lock
                isTuned -> Icons.Filled.Check
                else -> null
            }
            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = AccentGreen,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                ) {
                    Icon(badge, contentDescription = null, tint = DarkBackground, modifier = Modifier.padding(3.dp))
                }
            }
        }
    }
}
