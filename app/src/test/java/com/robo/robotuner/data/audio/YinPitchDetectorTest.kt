package com.robo.robotuner.data.audio

import com.robo.robotuner.data.audio.TestUtils.SAMPLE_RATE
import com.robo.robotuner.data.audio.TestUtils.ToneStream
import com.robo.robotuner.data.audio.TestUtils.centsError
import com.robo.robotuner.domain.model.Instrument
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class YinPitchDetectorTest {

    private val detector = YinPitchDetector()
    private val frameSize = PitchPipeline.WINDOW_SIZE

    private fun detect(samples: FloatArray, instrument: Instrument = Instrument.GUITAR) =
        detector.detectPitch(samples, SAMPLE_RATE, instrument.lowestDetectableHz)

    @Test
    fun `pure tones across the bass range are within a twentieth of a cent`() {
        for (step in 0..19) {
            for (offset in listOf(-37.0, 0.0, 41.0)) {
                val frequency = C1 * 2.0.pow(step / 12.0) * 2.0.pow(offset / 1200)
                val result = detect(ToneStream(step).next(frameSize, frequency), Instrument.BASS)
                assertNotNull("no pitch for $frequency Hz", result)
                val error = abs(centsError(result!!, frequency))
                assertTrue("$frequency Hz off by $error cents", error < 0.05)
            }
        }
    }

    @Test
    fun `bass heard through a phone mic without its fundamental is not reported an octave up`() {
        val noFundamental = listOf(0.0, 1.0, 0.7, 0.5, 0.35, 0.2, 0.15, 0.1)
        for (step in 0..19) {
            val frequency = C1 * 2.0.pow(step / 12.0)
            val result = detect(ToneStream(step).next(frameSize, frequency, noFundamental, noise = 0.003), Instrument.BASS)
            assertNotNull("no pitch for $frequency Hz", result)
            val error = abs(centsError(result!!, frequency))
            assertTrue("$frequency Hz off by $error cents", error < 0.5)
        }
    }

    @Test
    fun `guitar floor does not reach bass notes`() {
        assertNull(detect(ToneStream().next(frameSize, 41.2), Instrument.GUITAR))
    }

    @Test
    fun `pure tones across the guitar range are within a twentieth of a cent`() {
        for (step in 0..36) {
            for (offset in listOf(-37.0, -13.0, 0.0, 23.0, 41.0)) {
                val frequency = 82.41 * 2.0.pow(step / 12.0) * 2.0.pow(offset / 1200)
                val result = detect(ToneStream(step).next(frameSize, frequency))
                assertNotNull("no pitch for $frequency Hz", result)
                val error = abs(centsError(result!!, frequency))
                assertTrue("$frequency Hz off by $error cents", error < 0.05)
            }
        }
    }

    @Test
    fun `fundamental weaker than its second harmonic is not reported an octave up`() {
        val weakFundamental = listOf(0.25, 1.0, 0.7, 0.5, 0.35, 0.2, 0.15, 0.1)
        for (step in 0..36) {
            val frequency = 82.41 * 2.0.pow(step / 12.0)
            val result = detect(ToneStream(step).next(frameSize, frequency, weakFundamental, noise = 0.003))
            assertNotNull("no pitch for $frequency Hz", result)
            val error = abs(centsError(result!!, frequency))
            assertTrue("$frequency Hz off by $error cents", error < 0.5)
        }
    }

    @Test
    fun `silence has no pitch`() {
        assertNull(detect(FloatArray(frameSize)))
    }

    @Test
    fun `noise has no pitch`() {
        for (seed in 1..20) {
            assertNull("seed $seed", detect(TestUtils.generateNoise(frameSize, 0.3f, seed)))
        }
    }

    @Test
    fun `frame too short for the lowest note has no pitch`() {
        assertNull(detect(ToneStream().next(2048, 110.0)))
    }

    private companion object {
        const val C1 = 32.70
    }
}
