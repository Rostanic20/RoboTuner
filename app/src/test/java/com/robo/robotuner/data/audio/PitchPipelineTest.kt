package com.robo.robotuner.data.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class PitchPipelineTest {

    private lateinit var pipeline: PitchPipeline

    @Before
    fun setup() {
        pipeline = PitchPipeline(TestUtils.SAMPLE_RATE)
    }

    // Helper to feed multiple frames until we get a confirmed result
    private fun detectWithConfirmation(frequency: Double, frames: Int = 6): Double? {
        var result: Double? = null
        repeat(frames) {
            val samples = TestUtils.generateSineWave(frequency)
            result = pipeline.detectPitch(samples, TestUtils.SAMPLE_RATE)
        }
        return result
    }

    private fun detectWithConfirmationHarmonics(
        frequency: Double,
        harmonics: List<Float>,
        frames: Int = 6
    ): Double? {
        var result: Double? = null
        repeat(frames) {
            val samples = TestUtils.generateSineWaveWithHarmonics(frequency, harmonics, durationMs = 200)
            result = pipeline.detectPitch(samples, TestUtils.SAMPLE_RATE)
        }
        return result
    }

    @Test
    fun `detect A4 440Hz`() {
        val result = detectWithConfirmation(440.0)

        assertNotNull("Should detect 440Hz", result)
        val centsError = abs(TestUtils.centsError(result!!, 440.0))
        assertTrue("Error should be < 20 cents, was $centsError", centsError < 20)
    }

    @Test
    fun `detect all guitar strings`() {
        val guitarStrings = mapOf(
            "E2" to 82.41,
            "A2" to 110.0,
            "D3" to 146.83,
            "G3" to 196.0,
            "B3" to 246.94,
            "E4" to 329.63
        )

        for ((name, freq) in guitarStrings) {
            pipeline.reset()
            val result = detectWithConfirmation(freq)

            assertNotNull("Should detect $name ($freq Hz)", result)
            val centsError = abs(TestUtils.centsError(result!!, freq))
            assertTrue("$name error should be < 30 cents, was $centsError", centsError < 30)
        }
    }

    @Test
    fun `handle guitar-like harmonics`() {
        // Low E with harmonics
        val harmonics = listOf(1f, 0.7f, 0.5f, 0.3f, 0.2f)
        val result = detectWithConfirmationHarmonics(82.41, harmonics)

        assertNotNull("Should detect fundamental with harmonics", result)
        // Should find fundamental ~82Hz, not harmonic at 165Hz
        assertTrue("Should find low E fundamental, was $result", result!! < 100)
    }

    @Test
    fun `reject silence`() {
        repeat(10) {
            val samples = FloatArray(8192) { 0f }
            pipeline.detectPitch(samples, TestUtils.SAMPLE_RATE)
        }
        val samples = FloatArray(8192) { 0f }
        val result = pipeline.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNull("Should reject silence", result)
    }

    @Test
    fun `reject low noise`() {
        repeat(10) {
            val samples = TestUtils.generateNoise(8192, 0.01f)
            pipeline.detectPitch(samples, TestUtils.SAMPLE_RATE)
        }
        val samples = TestUtils.generateNoise(8192, 0.01f)
        val result = pipeline.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNull("Should reject noise", result)
    }

    @Test
    fun `smooth consecutive measurements`() {
        val results = mutableListOf<Double>()

        // Feed multiple frames of same frequency
        repeat(10) {
            val samples = TestUtils.generateSineWave(440.0)
            val result = pipeline.detectPitch(samples, TestUtils.SAMPLE_RATE)
            if (result != null) results.add(result)
        }

        assertTrue("Should have multiple results", results.size >= 3)

        // Check stability - variance should be low
        val mean = results.average()
        val variance = results.map { (it - mean) * (it - mean) }.average()

        assertTrue("Results should be stable, variance was $variance", variance < 5.0)
    }

    @Test
    fun `hysteresis prevents note flickering`() {
        // Feed A4 multiple times to establish baseline
        repeat(8) {
            pipeline.detectPitch(
                TestUtils.generateSineWave(440.0),
                TestUtils.SAMPLE_RATE
            )
        }

        // Feed one frame of slightly different pitch (still A4)
        val result = pipeline.detectPitch(
            TestUtils.generateSineWave(442.0),
            TestUtils.SAMPLE_RATE
        )

        assertNotNull(result)
        // Should still be close to A4 range
        val centsError = abs(TestUtils.centsError(result!!, 440.0))
        assertTrue("Should maintain stable note, cents error was $centsError", centsError < 20)
    }

    @Test
    fun `chromatic scale detection`() {
        // Test all 12 notes in octave 4
        val chromaticScale = listOf(
            261.63, // C4
            277.18, // C#4
            293.66, // D4
            311.13, // D#4
            329.63, // E4
            349.23, // F4
            369.99, // F#4
            392.00, // G4
            415.30, // G#4
            440.00, // A4
            466.16, // A#4
            493.88  // B4
        )

        for (freq in chromaticScale) {
            pipeline.reset()
            val result = detectWithConfirmation(freq)

            assertNotNull("Should detect $freq Hz", result)
            val centsError = abs(TestUtils.centsError(result!!, freq))
            assertTrue("Error for $freq Hz should be < 30 cents, was $centsError", centsError < 30)
        }
    }
}
