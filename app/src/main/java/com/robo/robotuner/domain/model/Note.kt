package com.robo.robotuner.domain.model

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

enum class Note(val displayName: String, val flatName: String, val semitoneOffset: Int) {
    C("C", "C", -9),
    C_SHARP("C#", "Db", -8),
    D("D", "D", -7),
    D_SHARP("D#", "Eb", -6),
    E("E", "E", -5),
    F("F", "F", -4),
    F_SHARP("F#", "Gb", -3),
    G("G", "G", -2),
    G_SHARP("G#", "Ab", -1),
    A("A", "A", 0),
    A_SHARP("A#", "Bb", 1),
    B("B", "B", 2);

    fun label(useFlats: Boolean): String = if (useFlats) flatName else displayName

    companion object {
        const val DEFAULT_A4 = 440.0
        private const val SEMITONES_PER_OCTAVE = 12

        fun getFrequency(note: Note, octave: Int, referenceA4: Double = DEFAULT_A4): Double {
            val semitonesFromA4 = (octave - 4) * SEMITONES_PER_OCTAVE + note.semitoneOffset
            return referenceA4 * 2.0.pow(semitonesFromA4 / 12.0)
        }

        fun fromFrequency(frequency: Double, referenceA4: Double = DEFAULT_A4): Pair<Note, Int> {
            if (frequency <= 0) return Pair(A, 4)

            val semitonesFromA4 = 12 * log2(frequency / referenceA4)
            val roundedSemitones = round(semitonesFromA4).toInt()

            val octaveOffset = if (roundedSemitones >= 0) {
                (roundedSemitones + 9) / 12
            } else {
                (roundedSemitones - 2) / 12
            }
            val octave = 4 + octaveOffset

            val noteIndex = ((roundedSemitones % 12) + 12 + 9) % 12

            return Pair(entries[noteIndex], octave)
        }

        fun calculateCents(detectedFrequency: Double, targetFrequency: Double): Double {
            if (detectedFrequency <= 0 || targetFrequency <= 0) return 0.0
            return 1200 * log2(detectedFrequency / targetFrequency)
        }
    }
}
