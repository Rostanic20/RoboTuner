package com.robo.robotuner.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.robo.robotuner.R
import com.robo.robotuner.domain.model.Tuning
import com.robo.robotuner.ui.theme.TextPrimary
import com.robo.robotuner.ui.theme.TextSecondary

@Composable
fun TopBar(
    tuning: Tuning,
    onOpenTunings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuningName = stringResource(tuning.nameRes())
    val changeTuning = stringResource(R.string.change_tuning, tuningName)
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        AssistChip(
            onClick = onOpenTunings,
            label = { Text(tuningName) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            colors = AssistChipDefaults.assistChipColors(labelColor = TextPrimary, trailingIconContentColor = TextSecondary),
            modifier = Modifier.semantics { contentDescription = changeTuning }
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = TextSecondary)
        }
    }
}
