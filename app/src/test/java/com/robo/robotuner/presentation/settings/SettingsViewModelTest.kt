package com.robo.robotuner.presentation.settings

import com.robo.robotuner.domain.model.Tuning
import com.robo.robotuner.domain.model.TunerSettings
import com.robo.robotuner.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    private val repository = object : SettingsRepository {
        private val state = MutableStateFlow(TunerSettings())
        override val settings: StateFlow<TunerSettings> = state
        override fun update(transform: (TunerSettings) -> TunerSettings) = state.update { transform(it).sanitized() }
    }

    private val viewModel = SettingsViewModel(repository)

    @Test
    fun `each setter changes only its own field`() {
        viewModel.setTuning(Tuning.OPEN_G)
        viewModel.setReferenceA4(442.0)
        viewModel.setTolerance(5.0)
        viewModel.setVibration(false)
        viewModel.setUseFlats(true)
        viewModel.setCapo(2)
        assertEquals(
            TunerSettings(Tuning.OPEN_G, referenceA4 = 442.0, toleranceCents = 5.0, vibration = false, useFlats = true, capo = 2),
            viewModel.settings.value
        )
    }
}
