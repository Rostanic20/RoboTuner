package com.robo.robotuner.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.robo.robotuner.data.repository.SharedPrefsSettingsRepository
import com.robo.robotuner.domain.model.Tuning
import com.robo.robotuner.domain.model.TunerSettings
import com.robo.robotuner.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<TunerSettings> = repository.settings

    fun setTuning(tuning: Tuning) = repository.update { it.copy(tuning = tuning) }

    fun setReferenceA4(hz: Double) = repository.update { it.copy(referenceA4 = hz) }

    fun setTolerance(cents: Double) = repository.update { it.copy(toleranceCents = cents) }

    fun setVibration(enabled: Boolean) = repository.update { it.copy(vibration = enabled) }

    fun setUseFlats(useFlats: Boolean) = repository.update { it.copy(useFlats = useFlats) }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) { "Unknown ViewModel class" }
            return SettingsViewModel(SharedPrefsSettingsRepository.get(context)) as T
        }
    }
}
