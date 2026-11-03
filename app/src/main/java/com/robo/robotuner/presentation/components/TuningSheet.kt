package com.robo.robotuner.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.robo.robotuner.R
import com.robo.robotuner.domain.model.Instrument
import com.robo.robotuner.domain.model.Tuning
import com.robo.robotuner.domain.model.TunerSettings
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.TextMuted
import com.robo.robotuner.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningSheet(
    selected: Tuning,
    capo: Int,
    useFlats: Boolean,
    onSelect: (Tuning) -> Unit,
    onCapo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    SheetScaffold(sheetState, onDismiss) {
        SectionTitle(R.string.capo)
        val capoText = capoLabel(capo)
        Text(capoText, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        StepSlider(value = capo, range = 0..TunerSettings.MAX_CAPO, valueText = capoText, onValueChange = onCapo)

        Instrument.entries.forEach { instrument ->
            SheetDivider()
            SectionTitle(instrument.nameRes())
            Column(Modifier.selectableGroup()) {
                Tuning.entries.filter { it.instrument == instrument }.forEach { tuning ->
                    TuningRow(tuning, selected = tuning == selected, useFlats = useFlats) {
                        onSelect(tuning)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun capoLabel(capo: Int): String =
    if (capo == 0) stringResource(R.string.capo_off) else stringResource(R.string.capo_fret, capo)

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
