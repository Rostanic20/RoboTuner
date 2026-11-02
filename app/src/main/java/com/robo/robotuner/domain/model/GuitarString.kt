package com.robo.robotuner.domain.model

import kotlin.math.abs

data class GuitarString(val note: Note, val octave: Int) {

    fun frequency(referenceA4: Double = Note.DEFAULT_A4): Double = Note.getFrequency(note, octave, referenceA4)

    companion object {
        private const val MATCH_CENTS = 100.0

        fun closestIndex(strings: List<GuitarString>, frequency: Double, referenceA4: Double = Note.DEFAULT_A4): Int? =
            strings.indices
                .map { it to abs(Note.calculateCents(frequency, strings[it].frequency(referenceA4))) }
                .filter { (_, cents) -> cents < MATCH_CENTS }
                .minByOrNull { (_, cents) -> cents }
                ?.first
    }
}
