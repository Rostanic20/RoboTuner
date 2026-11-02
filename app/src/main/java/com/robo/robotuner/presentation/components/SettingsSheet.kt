package com.robo.robotuner.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.robo.robotuner.R
import com.robo.robotuner.domain.model.Note
import com.robo.robotuner.domain.model.Tuning
import com.robo.robotuner.domain.model.TunerSettings
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.DarkBackground
import com.robo.robotuner.ui.theme.DarkCard
import com.robo.robotuner.ui.theme.DarkSurface
import com.robo.robotuner.ui.theme.TextMuted
import com.robo.robotuner.ui.theme.TextPrimary
import com.robo.robotuner.ui.theme.TextSecondary
import kotlin.math.roundToInt

class SettingsActions(
    val onTuning: (Tuning) -> Unit,
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            SectionTitle(R.string.tuning)
            Column(Modifier.selectableGroup()) {
                Tuning.entries.forEach { tuning ->
                    TuningRow(tuning, selected = tuning == settings.tuning, useFlats = settings.useFlats) {
                        actions.onTuning(tuning)
                    }
                }
            }
            Divider()

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
            Slider(
                value = settings.referenceA4.toFloat(),
                onValueChange = { actions.onReferenceA4(it.roundToInt().toDouble()) },
                valueRange = TunerSettings.MIN_A4.toFloat()..TunerSettings.MAX_A4.toFloat(),
                steps = (TunerSettings.MAX_A4 - TunerSettings.MIN_A4).toInt() - 1,
                modifier = Modifier.semantics { stateDescription = referenceText },
                colors = SliderDefaults.colors(
                    thumbColor = AccentGreen,
                    activeTrackColor = AccentGreen,
                    inactiveTrackColor = DarkCard,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
            Divider()

            SectionTitle(R.string.tolerance)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TunerSettings.TOLERANCE_OPTIONS.forEach { cents ->
                    Choice(
                        label = stringResource(R.string.tolerance_value, cents.roundToInt()),
                        selected = settings.toleranceCents == cents
                    ) { actions.onTolerance(cents) }
                }
            }
            Divider()

            SectionTitle(R.string.note_names)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(stringResource(R.string.sharps), selected = !settings.useFlats) { actions.onUseFlats(false) }
                Choice(stringResource(R.string.flats), selected = settings.useFlats) { actions.onUseFlats(true) }
            }
            Divider()

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
}

@Composable
private fun SectionTitle(label: Int) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(color = DarkCard, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun TuningRow(tuning: Tuning, selected: Boolean, useFlats: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = AccentGreen, unselectedColor = TextMuted)
        )
        Spacer(Modifier.padding(start = 12.dp))
        Column {
            Text(stringResource(tuning.nameRes()), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(tuning.notesLabel(useFlats), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
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
