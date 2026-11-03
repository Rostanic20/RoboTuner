package com.robo.robotuner.presentation.tuner

import com.robo.robotuner.domain.model.DetectedPitch
import com.robo.robotuner.domain.model.GuitarString
import com.robo.robotuner.domain.model.Note
import com.robo.robotuner.domain.model.TunerSettings
import kotlin.math.abs

data class TunerUiState(
    val isListening: Boolean = false,
    val reading: TunerReading? = null,
    val settings: TunerSettings = TunerSettings(),
    val lockedString: Int? = null,
    val tunedStrings: Set<Int> = emptySet(),
    val playingString: Int? = null,
    val error: String? = null
) {
    val strings: List<GuitarString> get() = settings.strings
    val allTuned: Boolean get() = strings.indices.all { it in tunedStrings }
}

data class TunerReading(
    val note: Note,
    val octave: Int,
    val cents: Double,
    val frequency: Double,
    val stringIndex: Int?,
    val toleranceCents: Double = TunerSettings.DEFAULT_TOLERANCE
) {
    val isInTune: Boolean get() = abs(cents) <= toleranceCents
    val isFlat: Boolean get() = cents < -toleranceCents
    val isSharp: Boolean get() = cents > toleranceCents

    companion object {
        fun from(pitch: DetectedPitch, settings: TunerSettings, lockedString: Int?): TunerReading {
            val strings = settings.strings
            val target = lockedString?.takeIf { it in strings.indices }
                ?: GuitarString.closestIndex(strings, pitch.frequency, settings.referenceA4)
            if (target == null) {
                return TunerReading(
                    pitch.note, pitch.octave, pitch.cents, pitch.frequency,
                    stringIndex = null,
                    toleranceCents = settings.toleranceCents
                )
            }
            val string = strings[target]
            return TunerReading(
                note = string.note,
                octave = string.octave,
                cents = Note.calculateCents(pitch.frequency, string.frequency(settings.referenceA4)),
                frequency = pitch.frequency,
                stringIndex = target,
                toleranceCents = settings.toleranceCents
            )
        }
    }
}
