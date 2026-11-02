package com.robo.robotuner.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.AccentRed

data class StatusAction(val label: Int, val onClick: () -> Unit)

@Composable
fun StatusMessage(message: String?, action: StatusAction?, modifier: Modifier = Modifier) {
    if (message == null) return
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(bottom = 8.dp)) {
        Text(
            text = message,
            color = AccentRed,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            TextButton(onClick = action.onClick) {
                Text(stringResource(action.label), color = AccentGreen)
            }
        }
    }
}
