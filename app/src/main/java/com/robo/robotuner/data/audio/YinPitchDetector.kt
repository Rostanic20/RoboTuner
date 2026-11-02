package com.robo.robotuner.data.audio

import kotlin.math.abs
import kotlin.math.sqrt

class YinPitchDetector {

    data class Result(val frequency: Double, val confidence: Float)

    fun detectPitch(
        audioSamples: FloatArray,
        sampleRate: Int,
        minFreq: Double = MIN_FREQUENCY,
        maxFreq: Double = MAX_FREQUENCY
    ): Result? {
        if (audioSamples.isEmpty()) return null

        val bufferSize = audioSamples.size
        val yinBufferSize = bufferSize / 2

        if (yinBufferSize < MIN_BUFFER_SIZE) return null

        // Convert frequency range to tau range
        // tau = sampleRate / frequency, so higher freq = lower tau
        val minTau = (sampleRate / maxFreq).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate / minFreq).toInt().coerceAtMost(yinBufferSize - 1)

        if (minTau >= maxTau) return null

        val yinBuffer = FloatArray(yinBufferSize)

        computeDifferenceFunction(audioSamples, yinBuffer, yinBufferSize)
        cumulativeMeanNormalizedDifference(yinBuffer, yinBufferSize)

        val tauEstimate = absoluteThreshold(yinBuffer, minTau, maxTau)
        if (tauEstimate == -1) return null

        val betterTau = parabolicInterpolation(yinBuffer, tauEstimate, yinBufferSize)
        val frequency = sampleRate.toDouble() / betterTau

        if (frequency < minFreq || frequency > maxFreq) return null

        val confidence = 1f - yinBuffer[tauEstimate].coerceIn(0f, 1f)
        return Result(frequency, confidence)
    }

    private fun computeDifferenceFunction(
        audioSamples: FloatArray,
        yinBuffer: FloatArray,
        yinBufferSize: Int
    ) {
        for (tau in 0 until yinBufferSize) {
            var sum = 0f
            for (i in 0 until yinBufferSize) {
                val delta = audioSamples[i] - audioSamples[i + tau]
                sum += delta * delta
            }
            yinBuffer[tau] = sum
        }
    }

    private fun cumulativeMeanNormalizedDifference(yinBuffer: FloatArray, yinBufferSize: Int) {
        yinBuffer[0] = 1f
        var runningSum = 0f

        for (tau in 1 until yinBufferSize) {
            runningSum += yinBuffer[tau]
            // Use epsilon comparison instead of exact equality
            if (runningSum > 1e-10f) {
                yinBuffer[tau] = yinBuffer[tau] * tau / runningSum
            }
        }
    }

    private fun absoluteThreshold(yinBuffer: FloatArray, minTau: Int, maxTau: Int): Int {
        var tau = minTau
        while (tau <= maxTau) {
            if (yinBuffer[tau] < THRESHOLD) {
                while (tau + 1 <= maxTau && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++
                }
                if (yinBuffer[tau] < THRESHOLD) {
                    return tau
                }
            }
            tau++
        }

        // Fallback: find global minimum in range
        var minTauIdx = minTau
        var minVal = yinBuffer[minTau]
        for (t in minTau + 1..maxTau) {
            if (yinBuffer[t] < minVal) {
                minVal = yinBuffer[t]
                minTauIdx = t
            }
        }

        return if (minVal < THRESHOLD_FALLBACK) minTauIdx else -1
    }

    private fun parabolicInterpolation(yinBuffer: FloatArray, tau: Int, yinBufferSize: Int): Double {
        if (tau <= 0 || tau >= yinBufferSize - 1) {
            return tau.toDouble()
        }

        val s0 = yinBuffer[tau - 1]
        val s1 = yinBuffer[tau]
        val s2 = yinBuffer[tau + 1]

        val denominator = 2 * (2 * s1 - s2 - s0)
        if (abs(denominator) < 1e-9f) {
            return tau.toDouble()
        }

        val adjustment = (s2 - s0) / denominator

        return if (abs(adjustment) < 1) {
            tau.toDouble() + adjustment.toDouble()
        } else {
            tau.toDouble()
        }
    }

    companion object {
        private const val THRESHOLD = 0.10f
        private const val THRESHOLD_FALLBACK = 0.25f
        // Increased from 1024 - need more samples for 82Hz detection
        // 2048 samples at 44100Hz = ~46ms = ~4 cycles of 82Hz
        private const val MIN_BUFFER_SIZE = 2048
        private const val MIN_FREQUENCY = 65.0   // Lowered for low E string with margin
        private const val MAX_FREQUENCY = 1400.0
    }
}
