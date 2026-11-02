package com.robo.robotuner.data.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class HpsPitchDetectorTest {

    private lateinit var detector: HpsPitchDetector

    @Before
    fun setup() {
        detector = HpsPitchDetector()
    }

    @Test
    fun `detect A4 440Hz`() {
        val samples = TestUtils.generateSineWave(440.0)
        val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNotNull("Should detect pitch", result)
        val centsError = abs(TestUtils.centsError(result!!.frequency, 440.0))
        assertTrue("Error should be < 30 cents, was $centsError", centsError < 30)
    }

    @Test
    fun `detect low E 82Hz`() {
        val samples = TestUtils.generateSineWave(82.41, durationMs = 300)
        val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNotNull("Should detect pitch", result)
        val centsError = abs(TestUtils.centsError(result!!.frequency, 82.41))
        assertTrue("Error should be < 50 cents, was $centsError", centsError < 50)
    }

    @Test
    fun `find fundamental with strong harmonics`() {
        // Guitar-like signal with strong harmonics
        val harmonics = listOf(1f, 0.8f, 0.6f, 0.4f, 0.3f)
        val samples = TestUtils.generateSineWaveWithHarmonics(110.0, harmonics, durationMs = 300)
        val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNotNull("Should detect fundamental", result)
        // HPS should find fundamental, not harmonics
        val centsError = abs(TestUtils.centsError(result!!.frequency, 110.0))
        assertTrue("Should find fundamental (110Hz), error was $centsError cents", centsError < 50)
    }

    @Test
    fun `reject silence`() {
        val samples = FloatArray(8192) { 0f }
        val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNull("Should reject silence", result)
    }

    @Test
    fun `reject noise`() {
        val samples = TestUtils.generateNoise(8192, 0.005f)
        val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNull("Should reject low-level noise", result)
    }

    @Test
    fun `different frequencies detection`() {
        val testFrequencies = listOf(100.0, 200.0, 300.0, 440.0, 600.0, 800.0)

        for (freq in testFrequencies) {
            val samples = TestUtils.generateSineWave(freq)
            val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

            assertNotNull("Should detect $freq Hz", result)
            val centsError = abs(TestUtils.centsError(result!!.frequency, freq))
            assertTrue("Error for $freq Hz should be < 50 cents, was $centsError", centsError < 50)
        }
    }
}
