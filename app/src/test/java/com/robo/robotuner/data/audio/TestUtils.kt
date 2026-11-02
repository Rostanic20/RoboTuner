package com.robo.robotuner.data.audio

import kotlin.math.PI
import kotlin.math.sin

object TestUtils {

    const val SAMPLE_RATE = 44100

    fun generateSineWave(
        frequency: Double,
        durationMs: Int = 200,
        sampleRate: Int = SAMPLE_RATE,
        amplitude: Float = 0.8f
    ): FloatArray {
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = FloatArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            samples[i] = (amplitude * sin(2 * PI * frequency * t)).toFloat()
        }

        return samples
    }

    fun generateSineWaveWithHarmonics(
        fundamental: Double,
        harmonicAmplitudes: List<Float> = listOf(1f, 0.5f, 0.3f, 0.2f),
        durationMs: Int = 200,
        sampleRate: Int = SAMPLE_RATE
    ): FloatArray {
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = FloatArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0f
            for ((index, amp) in harmonicAmplitudes.withIndex()) {
                val harmonic = (index + 1) * fundamental
                sample += (amp * sin(2 * PI * harmonic * t)).toFloat()
            }
            samples[i] = sample * 0.5f
        }

        return samples
    }

    fun generateNoise(
        numSamples: Int,
        amplitude: Float = 0.1f
    ): FloatArray {
        val samples = FloatArray(numSamples)
        for (i in 0 until numSamples) {
            samples[i] = ((Math.random() * 2 - 1) * amplitude).toFloat()
        }
        return samples
    }

    fun centsError(detected: Double, expected: Double): Double {
        return 1200 * kotlin.math.log2(detected / expected)
    }
}
