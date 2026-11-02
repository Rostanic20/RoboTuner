package com.robo.robotuner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TuningTest {

    @Test
    fun `every tuning has six strings from low to high`() {
        for (tuning in Tuning.entries) {
            val frequencies = tuning.strings.map { it.frequency() }
            assertEquals(tuning.name, 6, frequencies.size)
            assertEquals(tuning.name, frequencies.sorted(), frequencies)
        }
    }

    @Test
    fun `half step down is a semitone below standard on every string`() {
        Tuning.HALF_STEP_DOWN.strings.zip(Tuning.STANDARD.strings).forEach { (lowered, standard) ->
            assertEquals(-100.0, Note.calculateCents(lowered.frequency(), standard.frequency()), 1e-9)
        }
    }

    @Test
    fun `flat names read the way players write them`() {
        assertEquals("Eb", Note.D_SHARP.label(useFlats = true))
        assertEquals("D#", Note.D_SHARP.label(useFlats = false))
        assertEquals("E", Note.E.label(useFlats = true))
    }

    @Test
    fun `settings are clamped to supported values`() {
        val s = TunerSettings(referenceA4 = 500.0, toleranceCents = 4.0).sanitized()
        assertEquals(TunerSettings.MAX_A4, s.referenceA4, 0.0)
        assertEquals(TunerSettings.DEFAULT_TOLERANCE, s.toleranceCents, 0.0)
        assertEquals(TunerSettings.MIN_A4, TunerSettings(referenceA4 = 0.0).sanitized().referenceA4, 0.0)
    }

    @Test
    fun `reference pitch scales note frequencies`() {
        assertEquals(432.0, Note.getFrequency(Note.A, 4, 432.0), 1e-9)
        assertEquals(Note.A to 4, Note.fromFrequency(432.0, 432.0))
    }
}
