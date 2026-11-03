package com.robo.robotuner.data.audio

import kotlin.math.ceil

class YinPitchDetector {

    fun detectPitch(samples: FloatArray, sampleRate: Int, lowestFrequency: Double): Double? {
        val minTau = (sampleRate / MAX_FREQUENCY).toInt().coerceAtLeast(2)
        val maxTau = ceil(sampleRate / lowestFrequency).toInt()
        val window = samples.size - maxTau - 1
        if (window < MIN_WINDOW) return null

        val difference = DoubleArray(maxTau + 2)
        for (tau in 1..maxTau + 1) {
            var sum = 0.0
            for (i in 0 until window) {
                val delta = samples[i] - samples[i + tau]
                sum += delta * delta
            }
            difference[tau] = sum
        }

        val normalized = DoubleArray(maxTau + 2)
        normalized[0] = 1.0
        var runningSum = 0.0
        for (tau in 1..maxTau + 1) {
            runningSum += difference[tau]
            normalized[tau] = if (runningSum > 0.0) difference[tau] * tau / runningSum else 1.0
        }

        val tau = firstDipBelowThreshold(normalized, minTau, maxTau) ?: return null
        return sampleRate / parabolicMinimum(difference, tau)
    }

    private fun firstDipBelowThreshold(normalized: DoubleArray, minTau: Int, maxTau: Int): Int? {
        var tau = minTau
        while (tau <= maxTau) {
            if (normalized[tau] < THRESHOLD) {
                while (tau < maxTau && normalized[tau + 1] < normalized[tau]) tau++
                return tau
            }
            tau++
        }
        return null
    }

    private fun parabolicMinimum(values: DoubleArray, tau: Int): Double {
        val s0 = values[tau - 1]
        val s1 = values[tau]
        val s2 = values[tau + 1]
        val denominator = s0 - 2 * s1 + s2
        if (denominator <= 0.0) return tau.toDouble()
        val shift = 0.5 * (s0 - s2) / denominator
        return if (shift > -1 && shift < 1) tau + shift else tau.toDouble()
    }

    companion object {
        private const val MAX_FREQUENCY = 1400.0
        private const val THRESHOLD = 0.15
        private const val MIN_WINDOW = 2048
    }
}
