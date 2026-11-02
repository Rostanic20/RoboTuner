package com.robo.robotuner.domain.model

import kotlin.math.abs

data class GuitarString(val note: Note, val octave: Int) {

    val frequency: Double = Note.getFrequency(note, octave)

    companion object {
        val STANDARD = listOf(
            GuitarString(Note.E, 2),
            GuitarString(Note.A, 2),
            GuitarString(Note.D, 3),
            GuitarString(Note.G, 3),
            GuitarString(Note.B, 3),
            GuitarString(Note.E, 4)
        )

        private const val MATCH_CENTS = 100.0

        fun closestIndex(strings: List<GuitarString>, frequency: Double): Int? =
            strings.indices
                .map { it to abs(Note.calculateCents(frequency, strings[it].frequency)) }
                .filter { (_, cents) -> cents < MATCH_CENTS }
                .minByOrNull { (_, cents) -> cents }
                ?.first
    }
}
