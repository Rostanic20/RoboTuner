package com.robo.robotuner.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PitchTrackerTest {

    private val tracker = PitchTracker()

    private fun feed(vararg frequencies: Double?) = frequencies.map { tracker.update(it) }

    @Test
    fun `new pitch needs three consistent readings`() {
        assertEquals(listOf(null, null, 110.0), feed(110.0, 110.0, 110.0))
    }

    @Test
    fun `single octave glitch is ignored`() {
        feed(110.0, 110.0, 110.0)
        assertEquals(listOf(110.0, 110.0), feed(220.0, 110.0))
    }

    @Test
    fun `outlier inside the track is rejected by the median`() {
        feed(110.0, 110.0, 110.0, 110.0)
        assertEquals(110.0, tracker.update(111.0)!!, 0.0)
    }

    @Test
    fun `new string replaces the old one after confirmation`() {
        feed(82.41, 82.41, 82.41)
        assertEquals(listOf(82.41, 82.41, 110.0), feed(110.0, 110.0, 110.0))
    }

    @Test
    fun `reading is held through a short dropout and cleared after a long one`() {
        feed(110.0, 110.0, 110.0)
        val held = feed(*arrayOfNulls<Double>(22))
        assertEquals(110.0, held.last()!!, 0.0)
        assertNull(tracker.update(null))
    }

    @Test
    fun `two glitches in a row do not move the reading`() {
        feed(110.0, 110.0, 110.0)
        assertEquals(listOf(110.0, 110.0, 110.0, 110.0, 110.0), feed(220.0, 220.0, 110.0, 110.0, 110.0))
    }

    @Test
    fun `pluck after a pause restarts confirmation but keeps showing the held value`() {
        feed(110.0, 110.0, 110.0)
        feed(null, null, null)
        assertEquals(listOf(110.0, 110.0, 111.0), feed(111.0, 111.0, 111.0))
    }
}
