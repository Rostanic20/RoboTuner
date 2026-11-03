package com.robo.robotuner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class TuningTest {

    private val stringCounts = mapOf(Instrument.GUITAR to 6, Instrument.BASS to 4)

    private fun offsets(tuning: Tuning, from: Tuning) =
        tuning.strings.zip(from.strings).map { (a, b) -> Math.round(Note.calculateCents(a.frequency(), b.frequency()) / 100).toInt() }

    @Test
    fun `every tuning has its instrument's string count from low to high`() {
        for (tuning in Tuning.entries) {
            val frequencies = tuning.strings.map { it.frequency() }
            assertEquals(tuning.name, stringCounts.getValue(tuning.instrument), frequencies.size)
            assertEquals(tuning.name, frequencies.sorted(), frequencies)
        }
    }

    @Test
    fun `every tuning matches its name in semitones from standard`() {
        val expected = mapOf(
            Tuning.DROP_D to listOf(-2, 0, 0, 0, 0, 0),
            Tuning.HALF_STEP_DOWN to listOf(-1, -1, -1, -1, -1, -1),
            Tuning.D_STANDARD to listOf(-2, -2, -2, -2, -2, -2),
            Tuning.DROP_C_SHARP to listOf(-3, -1, -1, -1, -1, -1),
            Tuning.DROP_C to listOf(-4, -2, -2, -2, -2, -2),
            Tuning.DADGAD to listOf(-2, 0, 0, 0, -2, -2),
            Tuning.OPEN_G to listOf(-2, -2, 0, 0, 0, -2),
            Tuning.OPEN_D to listOf(-2, 0, 0, -1, -2, -2),
            Tuning.OPEN_E to listOf(0, 2, 2, 1, 0, 0),
            Tuning.OPEN_C to listOf(-4, -2, -2, 0, 1, 0),
            Tuning.BASS_DROP_D to listOf(-2, 0, 0, 0),
            Tuning.BASS_HALF_STEP_DOWN to listOf(-1, -1, -1, -1),
            Tuning.BASS_D_STANDARD to listOf(-2, -2, -2, -2),
            Tuning.BASS_DROP_C to listOf(-4, -2, -2, -2)
        )
        for ((tuning, semitones) in expected) {
            val standard = if (tuning.instrument == Instrument.BASS) Tuning.BASS_STANDARD else Tuning.STANDARD
            assertEquals(tuning.name, semitones, offsets(tuning, standard))
        }
        assertEquals(Tuning.entries.size - 2, expected.size)
    }

    @Test
    fun `bass standard is an octave below the guitar's four lowest strings`() {
        assertEquals(listOf(-12, -12, -12, -12), offsets(Tuning.BASS_STANDARD, Tuning.STANDARD))
    }

    @Test
    fun `a string a semitone flat at the lowest reference pitch is still above the detector floor`() {
        for (tuning in Tuning.entries) {
            val lowest = tuning.strings.first().frequency(TunerSettings.MIN_A4) * 2.0.pow(-1 / 12.0)
            assertTrue("${tuning.name} at $lowest Hz", lowest > tuning.instrument.lowestDetectableHz)
        }
    }

    @Test
    fun `transposing wraps into the next octave`() {
        assertEquals(GuitarString(Note.F_SHARP, 2), GuitarString(Note.E, 2).transposed(2))
        assertEquals(GuitarString(Note.C, 4), GuitarString(Note.B, 3).transposed(1))
        assertEquals(GuitarString(Note.B, 3), GuitarString(Note.C, 4).transposed(-1))
        assertEquals(GuitarString(Note.E, 5), GuitarString(Note.E, 4).transposed(12))
    }

    @Test
    fun `capo raises every target string by one semitone per fret`() {
        val settings = TunerSettings(tuning = Tuning.STANDARD, capo = 3)
        settings.strings.zip(Tuning.STANDARD.strings).forEach { (capoed, open) ->
            assertEquals(300.0, Note.calculateCents(capoed.frequency(), open.frequency()), 1e-9)
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
        val s = TunerSettings(referenceA4 = 500.0, toleranceCents = 4.0, capo = 12).sanitized()
        assertEquals(TunerSettings.MAX_A4, s.referenceA4, 0.0)
        assertEquals(TunerSettings.DEFAULT_TOLERANCE, s.toleranceCents, 0.0)
        assertEquals(TunerSettings.MAX_CAPO, s.capo)
        assertEquals(TunerSettings.MIN_A4, TunerSettings(referenceA4 = 0.0).sanitized().referenceA4, 0.0)
        assertEquals(0, TunerSettings(capo = -1).sanitized().capo)
    }

    @Test
    fun `reference pitch scales note frequencies`() {
        assertEquals(432.0, Note.getFrequency(Note.A, 4, 432.0), 1e-9)
        assertEquals(Note.A to 4, Note.fromFrequency(432.0, 432.0))
    }
}
