package com.robo.robotuner.domain.repository

import com.robo.robotuner.domain.model.TunerSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<TunerSettings>
    fun update(transform: (TunerSettings) -> TunerSettings)
}
