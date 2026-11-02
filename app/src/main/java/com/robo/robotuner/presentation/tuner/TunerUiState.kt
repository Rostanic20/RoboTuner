package com.robo.robotuner.presentation.tuner

import com.robo.robotuner.domain.model.DetectedPitch
import com.robo.robotuner.domain.model.GuitarString
import com.robo.robotuner.domain.model.Note
import kotlin.math.abs

data class TunerUiState(
    val isListening: Boolean = false,
    val reading: TunerReading? = null,
    val strings: List<GuitarString> = GuitarString.STANDARD,
    val lockedString: Int? = null,
    val tunedStrings: Set<Int> = emptySet(),
    val error: String? = null
)

data class TunerReading(
    val note: Note,
    val octave: Int,
    val cents: Double,
    val frequency: Double,
    val stringIndex: Int?
) {
    val isInTune: Boolean get() = abs(cents) <= DetectedPitch.IN_TUNE_THRESHOLD
    val isFlat: Boolean get() = cents < -DetectedPitch.IN_TUNE_THRESHOLD
    val isSharp: Boolean get() = cents > DetectedPitch.IN_TUNE_THRESHOLD

    companion object {
        fun from(pitch: DetectedPitch, strings: List<GuitarString>, lockedString: Int?): TunerReading {
            val target = lockedString?.takeIf { it in strings.indices }
                ?: GuitarString.closestIndex(strings, pitch.frequency)
            if (target == null) {
                return TunerReading(pitch.note, pitch.octave, pitch.cents, pitch.frequency, stringIndex = null)
            }
            val string = strings[target]
            return TunerReading(
                note = string.note,
                octave = string.octave,
                cents = Note.calculateCents(pitch.frequency, string.frequency),
                frequency = pitch.frequency,
                stringIndex = target
            )
        }
    }
}
