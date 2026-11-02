package com.robo.robotuner.domain.model

import kotlin.math.abs

data class DetectedPitch(
    val frequency: Double,
    val note: Note,
    val octave: Int,
    val cents: Double,
    val targetFrequency: Double
) {
    companion object {
        private const val NOTE_HOLD_CENTS = 60.0

        fun fromFrequency(
            frequency: Double,
            previous: DetectedPitch? = null,
            referenceA4: Double = Note.DEFAULT_A4
        ): DetectedPitch {
            if (previous != null) {
                val cents = Note.calculateCents(frequency, previous.targetFrequency)
                if (abs(cents) <= NOTE_HOLD_CENTS) return previous.copy(frequency = frequency, cents = cents)
            }
            val (note, octave) = Note.fromFrequency(frequency, referenceA4)
            val targetFrequency = Note.getFrequency(note, octave, referenceA4)
            return DetectedPitch(
                frequency = frequency,
                note = note,
                octave = octave,
                cents = Note.calculateCents(frequency, targetFrequency),
                targetFrequency = targetFrequency
            )
        }
    }
}
