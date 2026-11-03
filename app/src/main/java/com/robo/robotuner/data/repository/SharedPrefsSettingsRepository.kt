package com.robo.robotuner.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.robo.robotuner.domain.model.Tuning
import com.robo.robotuner.domain.model.TunerSettings
import com.robo.robotuner.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

class SharedPrefsSettingsRepository private constructor(context: Context) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    override val settings: StateFlow<TunerSettings> = state.asStateFlow()

    override fun update(transform: (TunerSettings) -> TunerSettings) {
        var changed = false
        val updated = state.updateAndGet { old -> transform(old).sanitized().also { changed = it != old } }
        if (!changed) return
        prefs.edit {
            putString(KEY_TUNING, updated.tuning.name)
            putFloat(KEY_A4, updated.referenceA4.toFloat())
            putFloat(KEY_TOLERANCE, updated.toleranceCents.toFloat())
            putBoolean(KEY_VIBRATION, updated.vibration)
            putBoolean(KEY_FLATS, updated.useFlats)
            putInt(KEY_CAPO, updated.capo)
        }
    }

    private fun read(): TunerSettings {
        val defaults = TunerSettings()
        return TunerSettings(
            tuning = Tuning.entries.find { it.name == prefs.getString(KEY_TUNING, null) } ?: defaults.tuning,
            referenceA4 = prefs.getFloat(KEY_A4, defaults.referenceA4.toFloat()).toDouble(),
            toleranceCents = prefs.getFloat(KEY_TOLERANCE, defaults.toleranceCents.toFloat()).toDouble(),
            vibration = prefs.getBoolean(KEY_VIBRATION, defaults.vibration),
            useFlats = prefs.getBoolean(KEY_FLATS, defaults.useFlats),
            capo = prefs.getInt(KEY_CAPO, defaults.capo)
        ).sanitized()
    }

    companion object {
        @Volatile
        private var instance: SharedPrefsSettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SharedPrefsSettingsRepository(context.applicationContext).also { instance = it }
            }

        private const val FILE = "tuner_settings"
        private const val KEY_TUNING = "tuning"
        private const val KEY_A4 = "reference_a4"
        private const val KEY_TOLERANCE = "tolerance_cents"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_FLATS = "use_flats"
        private const val KEY_CAPO = "capo"
    }
}
