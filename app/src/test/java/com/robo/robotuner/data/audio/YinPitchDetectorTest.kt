package com.robo.robotuner.data.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class YinPitchDetectorTest {

    private lateinit var detector: YinPitchDetector

    @Before
    fun setup() {
        detector = YinPitchDetector()
    }

    @Test
    fun `detect A4 440Hz pure sine`() {
        val samples = TestUtils.generateSineWave(440.0)
        val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNotNull("Should detect pitch", result)
        val centsError = abs(TestUtils.centsError(result!!.frequency, 440.0))
        assertTrue("Error should be < 10 cents, was $centsError", centsError < 10)
    }

    @Test
    fun `detect low E 82Hz`() {
        val samples = TestUtils.generateSineWave(82.41)
        val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNotNull("Should detect pitch", result)
        val centsError = abs(TestUtils.centsError(result!!.frequency, 82.41))
        assertTrue("Error should be < 15 cents, was $centsError", centsError < 15)
    }

    @Test
    fun `detect high E 329Hz`() {
        val samples = TestUtils.generateSineWave(329.63)
        val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNotNull("Should detect pitch", result)
        val centsError = abs(TestUtils.centsError(result!!.frequency, 329.63))
        assertTrue("Error should be < 10 cents, was $centsError", centsError < 10)
    }

    @Test
    fun `detect with harmonics`() {
        val samples = TestUtils.generateSineWaveWithHarmonics(110.0)
        val result = detector.detectPitch(samples, TestUtils.SAMPLE_RATE)

        assertNotNull("Should detect pitch", result)
        val centsError = abs(TestUtils.centsError(result!!.frequency, 110.0))
        assertTrue("Error should be < 20 cents, was $centsError", centsError < 20)
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
    fun `confidence higher for clean signal`() {
        val cleanSamples = TestUtils.generateSineWave(440.0, amplitude = 0.9f)
        val noisySamples = TestUtils.generateSineWave(440.0, amplitude = 0.3f)

        val cleanResult = detector.detectPitch(cleanSamples, TestUtils.SAMPLE_RATE)
        val noisyResult = detector.detectPitch(noisySamples, TestUtils.SAMPLE_RATE)

        assertNotNull(cleanResult)
        assertNotNull(noisyResult)
        assertTrue(
            "Clean signal should have higher confidence",
            cleanResult!!.confidence >= noisyResult!!.confidence
        )
    }

    @Test
    fun `detect within custom frequency range`() {
        val samples = TestUtils.generateSineWave(200.0)

        val resultInRange = detector.detectPitch(samples, TestUtils.SAMPLE_RATE, 150.0, 250.0)
        val resultOutOfRange = detector.detectPitch(samples, TestUtils.SAMPLE_RATE, 300.0, 400.0)

        assertNotNull("Should detect when in range", resultInRange)
        assertNull("Should not detect when out of range", resultOutOfRange)
    }
}
