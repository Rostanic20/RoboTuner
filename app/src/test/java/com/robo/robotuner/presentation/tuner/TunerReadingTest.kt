package com.robo.robotuner.presentation.tuner

import com.robo.robotuner.domain.model.DetectedPitch
import com.robo.robotuner.domain.model.GuitarString
import com.robo.robotuner.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.pow

class TunerReadingTest {

    private val strings = GuitarString.STANDARD

    private fun reading(frequency: Double, locked: Int? = null) =
        TunerReading.from(DetectedPitch.fromFrequency(frequency), strings, locked)

    @Test
    fun `flat A string reads against A2 even when nearer to G sharp`() {
        val r = reading(110.0 * 2.0.pow(-51 / 1200.0))
        assertEquals(Note.A, r.note)
        assertEquals(2, r.octave)
        assertEquals(-51.0, r.cents, 0.01)
        assertEquals(1, r.stringIndex)
        assertEquals(true, r.isFlat)
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
        assertEquals(2, r.octave)
        assertEquals(500.0, r.cents, 0.1)
        assertEquals(true, r.isSharp)
    }

    @Test
    fun `in tune within 3 cents`() {
        assertEquals(true, reading(110.0 * 2.0.pow(2.9 / 1200)).isInTune)
        assertEquals(true, reading(110.0 * 2.0.pow(3.1 / 1200)).isSharp)
        assertEquals(true, reading(110.0 * 2.0.pow(-3.1 / 1200)).isFlat)
    }

    @Test
    fun `out of range lock index is ignored`() {
        assertEquals(1, reading(110.0, locked = 9).stringIndex)
    }

    @Test
    fun `closest string picks the nearer of two within a semitone`() {
        assertEquals(4, GuitarString.closestIndex(strings, 246.94))
        assertNull(GuitarString.closestIndex(strings, 400.0))
    }
}
