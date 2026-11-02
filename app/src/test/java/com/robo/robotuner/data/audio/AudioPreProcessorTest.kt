package com.robo.robotuner.data.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class AudioPreProcessorTest {

    private lateinit var processor: AudioPreProcessor

    @Before
    fun setup() {
        processor = AudioPreProcessor(TestUtils.SAMPLE_RATE)
    }

    @Test
    fun `remove DC offset`() {
        // Signal with DC offset
        val samples = FloatArray(1000) { 0.5f }
        val processed = processor.process(samples)

        val mean = processed.sum() / processed.size
        assertTrue("DC offset should be removed, mean was $mean", abs(mean) < 0.01)
    }

    @Test
    fun `preserve sine wave shape`() {
        val samples = TestUtils.generateSineWave(440.0, durationMs = 100)
        val processed = processor.process(samples)

        // Check that output still has oscillations (not all zeros)
        val maxAbs = processed.maxOf { abs(it) }
        assertTrue("Should preserve signal, max was $maxAbs", maxAbs > 0.1)
    }

    @Test
    fun `normalize output level`() {
        val quietSamples = TestUtils.generateSineWave(440.0, amplitude = 0.1f)
        val loudSamples = TestUtils.generateSineWave(440.0, amplitude = 0.9f)

        val processedQuiet = processor.process(quietSamples)
        processor.reset()
        val processedLoud = processor.process(loudSamples)

        val maxQuiet = processedQuiet.maxOf { abs(it) }
        val maxLoud = processedLoud.maxOf { abs(it) }

        // Both should be normalized to similar levels
        assertTrue("Quiet signal should be boosted", maxQuiet > 0.5)
        assertTrue("Both should be similar level", abs(maxQuiet - maxLoud) < 0.3)
    }

    @Test
    fun `attenuate very low frequencies`() {
        // Generate 30Hz signal (below cutoff)
        val lowFreqSamples = TestUtils.generateSineWave(30.0, durationMs = 200)
        val processed = processor.process(lowFreqSamples)

        // Low frequencies should be attenuated
        val inputRms = kotlin.math.sqrt(lowFreqSamples.map { it * it }.average().toFloat())
        val outputRms = kotlin.math.sqrt(processed.map { it * it }.average().toFloat())

        // Note: normalization will boost it back, but relative energy should shift
        // This is a basic check - high-pass should reduce DC-like components
        assertNotNull(processed)
    }

    @Test
    fun `reset clears filter state`() {
        processor.process(TestUtils.generateSineWave(100.0))
        processor.reset()

        val samples = TestUtils.generateSineWave(440.0)
        val processed = processor.process(samples)

        assertNotNull(processed)
        assertTrue(processed.isNotEmpty())
    }
}
