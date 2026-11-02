package com.robo.robotuner.domain.model

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

enum class Note(val displayName: String, val semitoneOffset: Int) {
    C("C", -9),
    C_SHARP("C#", -8),
    D("D", -7),
    D_SHARP("D#", -6),
    E("E", -5),
    F("F", -4),
    F_SHARP("F#", -3),
    G("G", -2),
    G_SHARP("G#", -1),
    A("A", 0),
    A_SHARP("A#", 1),
    B("B", 2);

    companion object {
        private const val A4_FREQUENCY = 440.0
        private const val SEMITONES_PER_OCTAVE = 12

        fun getFrequency(note: Note, octave: Int): Double {
            val semitonesFromA4 = (octave - 4) * SEMITONES_PER_OCTAVE + note.semitoneOffset
            return A4_FREQUENCY * 2.0.pow(semitonesFromA4 / 12.0)
        }

        fun fromFrequency(frequency: Double): Pair<Note, Int> {
            if (frequency <= 0) return Pair(A, 4)

            val semitonesFromA4 = 12 * log2(frequency / A4_FREQUENCY)
            val roundedSemitones = round(semitonesFromA4).toInt()

            val octaveOffset = if (roundedSemitones >= 0) {
                (roundedSemitones + 9) / 12
            } else {
                (roundedSemitones - 2) / 12
            }
            val octave = 4 + octaveOffset

            var noteIndex = ((roundedSemitones % 12) + 12 + 9) % 12

            return Pair(entries[noteIndex], octave)
        }

        fun calculateCents(detectedFrequency: Double, targetFrequency: Double): Double {
            if (detectedFrequency <= 0 || targetFrequency <= 0) return 0.0
            return 1200 * log2(detectedFrequency / targetFrequency)
        }
    }
}
