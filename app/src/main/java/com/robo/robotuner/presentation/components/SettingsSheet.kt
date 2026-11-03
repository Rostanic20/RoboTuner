package com.robo.robotuner.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.robo.robotuner.R
import com.robo.robotuner.domain.model.Note
import com.robo.robotuner.domain.model.TunerSettings
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.DarkBackground
import com.robo.robotuner.ui.theme.DarkCard
import com.robo.robotuner.ui.theme.TextPrimary
import com.robo.robotuner.ui.theme.TextSecondary
import kotlin.math.roundToInt

class SettingsActions(
    val onReferenceA4: (Double) -> Unit,
    val onTolerance: (Double) -> Unit,
    val onVibration: (Boolean) -> Unit,
    val onUseFlats: (Boolean) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: TunerSettings,
    actions: SettingsActions,
    onDismiss: () -> Unit
) {
    SheetScaffold(rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismiss) {
        SectionTitle(R.string.reference_pitch)
        val referenceText = stringResource(R.string.reference_value, settings.referenceA4.roundToInt())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = referenceText,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (settings.referenceA4 != Note.DEFAULT_A4) {
                TextButton(onClick = { actions.onReferenceA4(Note.DEFAULT_A4) }) {
                    Text(stringResource(R.string.reset_to_440), color = AccentGreen)
                }
            }
        }
        StepSlider(
            value = settings.referenceA4.roundToInt(),
            range = TunerSettings.MIN_A4.toInt()..TunerSettings.MAX_A4.toInt(),
            valueText = referenceText,
            onValueChange = { actions.onReferenceA4(it.toDouble()) }
        )
        SheetDivider()

        SectionTitle(R.string.tolerance)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TunerSettings.TOLERANCE_OPTIONS.forEach { cents ->
                Choice(
                    label = stringResource(R.string.tolerance_value, cents.roundToInt()),
                    selected = settings.toleranceCents == cents
                ) { actions.onTolerance(cents) }
            }
        }
        SheetDivider()

        SectionTitle(R.string.note_names)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice(stringResource(R.string.sharps), selected = !settings.useFlats) { actions.onUseFlats(false) }
            Choice(stringResource(R.string.flats), selected = settings.useFlats) { actions.onUseFlats(true) }
        }
        SheetDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = settings.vibration,
                    role = Role.Switch,
                    onClick = { actions.onVibration(!settings.vibration) }
                )
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.vibrate_in_tune),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = settings.vibration,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkBackground,
                    checkedTrackColor = AccentGreen,
                    uncheckedTrackColor = DarkCard
                )
            )
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
            selectedLabelColor = AccentGreen,
            labelColor = TextSecondary
        )
    )
}
