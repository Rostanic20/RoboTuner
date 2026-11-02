package com.robo.robotuner.data.audio

import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object TestUtils {

    const val SAMPLE_RATE = 44100
    const val HOP_SIZE = 2048

    val GUITAR_STRINGS = listOf(82.41, 110.0, 146.83, 196.0, 246.94, 329.63)

    fun generateSineWave(
        frequency: Double,
        numSamples: Int = 8192,
        amplitude: Float = 0.8f
    ): FloatArray = ToneStream().next(numSamples, frequency, listOf(amplitude.toDouble()), gain = 1.0)

    fun generateNoise(numSamples: Int, amplitude: Float, seed: Int = 1): FloatArray {
        val random = Random(seed)
        return FloatArray(numSamples) { ((random.nextDouble() * 2 - 1) * amplitude).toFloat() }
    }

    fun centsError(detected: Double, expected: Double): Double = 1200 * log2(detected / expected)

    class ToneStream(seed: Int = 1) {
        private val random = Random(seed)
        private val phases = DoubleArray(16) { random.nextDouble() * 2 * PI }

        fun next(
            numSamples: Int,
            frequency: Double,
            harmonics: List<Double> = listOf(1.0),
            inharmonicity: Double = 0.0,
            noise: Double = 0.0,
            gain: Double = 0.3
        ): FloatArray = FloatArray(numSamples) {
            var sample = 0.0
            for ((index, amplitude) in harmonics.withIndex()) {
                val h = index + 1
                phases[index] += 2 * PI * h * frequency * sqrt(1 + inharmonicity * h * h) / SAMPLE_RATE
                sample += amplitude * sin(phases[index])
            }
            (gain * sample + noise * (random.nextDouble() * 2 - 1)).toFloat()
        }
    }
}
