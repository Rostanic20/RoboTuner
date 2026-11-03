package com.robo.robotuner.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.DarkCard
import com.robo.robotuner.ui.theme.DarkSurface
import com.robo.robotuner.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetScaffold(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            content = content
        )
    }
}

@Composable
internal fun SectionTitle(@StringRes label: Int) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
internal fun SheetDivider() {
    HorizontalDivider(color = DarkCard, modifier = Modifier.padding(top = 12.dp))
}

@Composable
internal fun StepSlider(value: Int, range: IntRange, valueText: String, onValueChange: (Int) -> Unit) {
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = range.last - range.first - 1,
        modifier = Modifier.semantics { stateDescription = valueText },
        colors = SliderDefaults.colors(
            thumbColor = AccentGreen,
            activeTrackColor = AccentGreen,
            inactiveTrackColor = DarkCard,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        )
    )
}
