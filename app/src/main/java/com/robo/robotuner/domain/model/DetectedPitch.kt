package com.robo.robotuner.domain.model

import kotlin.math.abs

data class DetectedPitch(
    val frequency: Double,
    val note: Note,
    val octave: Int,
    val cents: Double,
    val targetFrequency: Double
) {
    val isInTune: Boolean
        get() = abs(cents) <= IN_TUNE_THRESHOLD

    val isFlat: Boolean
        get() = cents < -IN_TUNE_THRESHOLD

    val isSharp: Boolean
        get() = cents > IN_TUNE_THRESHOLD

    companion object {
        const val IN_TUNE_THRESHOLD = 5.0

        fun fromFrequency(frequency: Double): DetectedPitch {
            val (note, octave) = Note.fromFrequency(frequency)
            val targetFrequency = Note.getFrequency(note, octave)
            val cents = Note.calculateCents(frequency, targetFrequency)

            return DetectedPitch(
                frequency = frequency,
                note = note,
                octave = octave,
                cents = cents,
                targetFrequency = targetFrequency
            )
        }
    }
}
