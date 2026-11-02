package com.robo.robotuner.data.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class PitchKalmanFilterTest {

    private lateinit var filter: PitchKalmanFilter

    @Before
    fun setup() {
        filter = PitchKalmanFilter()
    }

    @Test
    fun `first measurement is returned as is`() {
        val result = filter.filter(440.0)
        assertEquals(440.0, result, 0.001)
    }

    @Test
    fun `smooth noisy measurements`() {
        val truePitch = 440.0
        val noisyMeasurements = listOf(438.0, 442.0, 439.0, 441.0, 440.0, 438.5, 441.5)

        var lastResult = 0.0
        for (m in noisyMeasurements) {
            lastResult = filter.filter(m)
        }

        val error = abs(lastResult - truePitch)
        assertTrue("Filtered result should be close to 440Hz, was $lastResult", error < 2.0)
    }

    @Test
    fun `reject sudden outlier`() {
        // Feed consistent values
        repeat(5) { filter.filter(440.0) }

        // Feed an outlier
        val resultAfterOutlier = filter.filter(880.0)

        // Should not jump to 880, should be damped
        assertTrue("Should not jump to outlier", resultAfterOutlier < 600)
    }

    @Test
    fun `track gradual pitch change`() {
        // Simulate pitch bend
        val pitches = listOf(440.0, 442.0, 444.0, 446.0, 448.0, 450.0)

        var lastResult = 0.0
        for (p in pitches) {
            lastResult = filter.filter(p)
        }

        // Should track the change
        assertTrue("Should track upward trend", lastResult > 445)
    }

    @Test
    fun `reset clears state`() {
        filter.filter(440.0)
        filter.filter(441.0)
        filter.filter(442.0)

        filter.reset()

        val result = filter.filter(200.0)
        assertEquals("After reset, should return new measurement", 200.0, result, 0.001)
    }
}
