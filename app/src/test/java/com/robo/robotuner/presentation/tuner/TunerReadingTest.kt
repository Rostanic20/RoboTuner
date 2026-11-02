package com.robo.robotuner.presentation.tuner

import com.robo.robotuner.domain.model.DetectedPitch
import com.robo.robotuner.domain.model.GuitarString
import com.robo.robotuner.domain.model.Note
import com.robo.robotuner.domain.model.Tuning
import com.robo.robotuner.domain.model.TunerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class TunerReadingTest {

    private fun cents(hz: Double, c: Double) = hz * 2.0.pow(c / 1200)

    private fun reading(frequency: Double, locked: Int? = null, settings: TunerSettings = TunerSettings()) =
        TunerReading.from(DetectedPitch.fromFrequency(frequency, referenceA4 = settings.referenceA4), settings, locked)

    @Test
    fun `flat A string reads against A2 even when nearer to G sharp`() {
        val r = reading(cents(110.0, -51.0))
        assertEquals(Note.A, r.note)
        assertEquals(2, r.octave)
        assertEquals(-51.0, r.cents, 0.01)
        assertEquals(1, r.stringIndex)
        assertTrue(r.isFlat)
    }

    @Test
    fun `pitch far from every string falls back to the nearest note`() {
        val r = reading(92.5)
        assertEquals(Note.F_SHARP, r.note)
        assertNull(r.stringIndex)
    }

    @Test
    fun `locked string measures against that string whatever is played`() {
        val r = reading(110.0, locked = 0)
        assertEquals(Note.E, r.note)
        assertEquals(500.0, r.cents, 0.1)
        assertTrue(r.isSharp)
    }

    @Test
    fun `out of range lock index is ignored`() {
        assertEquals(1, reading(110.0, locked = 9).stringIndex)
    }

    @Test
    fun `tolerance comes from settings`() {
        assertTrue(reading(cents(110.0, 2.9)).isInTune)
        assertTrue(reading(cents(110.0, 3.1)).isSharp)
        assertTrue(reading(cents(110.0, 4.0), settings = TunerSettings(toleranceCents = 5.0)).isInTune)
        assertTrue(reading(cents(110.0, -1.5), settings = TunerSettings(toleranceCents = 1.0)).isFlat)
    }

    @Test
    fun `drop D low string matches D2 not E2`() {
        val r = reading(73.42, settings = TunerSettings(tuning = Tuning.DROP_D))
        assertEquals(Note.D, r.note)
        assertEquals(0, r.stringIndex)
        assertEquals(0.0, r.cents, 0.1)
    }

    @Test
    fun `reference pitch moves every target`() {
        val r = reading(108.0, settings = TunerSettings(referenceA4 = 432.0))
        assertEquals(Note.A, r.note)
        assertEquals(0.0, r.cents, 0.01)
    }

    @Test
    fun `closest string picks the nearer of two within a semitone`() {
        assertEquals(4, GuitarString.closestIndex(Tuning.STANDARD.strings, 246.94))
        assertNull(GuitarString.closestIndex(Tuning.STANDARD.strings, 400.0))
    }
}
