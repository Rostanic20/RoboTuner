package com.robo.robotuner.data.audio

import com.robo.robotuner.data.audio.TestUtils.SAMPLE_RATE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class AudioPreProcessorTest {

    private val processor = AudioPreProcessor(SAMPLE_RATE)

    private fun rms(samples: FloatArray) = sqrt(samples.map { it * it }.average())

    @Test
    fun `removes DC offset`() {
        val processed = processor.process(FloatArray(SAMPLE_RATE) { 0.5f })
        assertTrue(abs(processed.takeLast(1000).average()) < 0.001)
    }

    @Test
    fun `passes low E almost unchanged`() {
        val input = TestUtils.generateSineWave(82.41, numSamples = SAMPLE_RATE)
        val ratio = rms(processor.process(input).takeLast(8192).toFloatArray()) / rms(input)
        assertTrue("gain was $ratio", ratio > 0.9)
    }

    @Test
    fun `keeps the signal level instead of normalising it`() {
        val quiet = TestUtils.generateSineWave(440.0, amplitude = 0.01f)
        assertEquals(rms(quiet), rms(processor.process(quiet)), 0.001)
    }

    @Test
    fun `filter state carries across calls`() {
        val input = TestUtils.generateSineWave(110.0, numSamples = 4096)
        val whole = AudioPreProcessor(SAMPLE_RATE).process(input)
        val split = processor.process(input.copyOfRange(0, 2048)) + processor.process(input.copyOfRange(2048, 4096))
        assertTrue(whole.contentEquals(split))
    }
}
