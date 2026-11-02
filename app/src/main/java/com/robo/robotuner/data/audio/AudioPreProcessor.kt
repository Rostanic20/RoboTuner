package com.robo.robotuner.data.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AudioPreProcessor(private val sampleRate: Int) {

    private var prevInput = 0f
    private var prevOutput = 0f

    fun process(samples: FloatArray): FloatArray {
        val result = FloatArray(samples.size)

        // Step 1: Remove DC offset
        var sum = 0f
        for (sample in samples) sum += sample
        val dcOffset = sum / samples.size

        // Step 2: High-pass filter (removes frequencies below ~70Hz)
        // Simple first-order IIR high-pass
        val rc = 1f / (2f * PI.toFloat() * HIGH_PASS_CUTOFF)
        val dt = 1f / sampleRate
        val alpha = rc / (rc + dt)

        for (i in samples.indices) {
            val input = samples[i] - dcOffset
            result[i] = alpha * (prevOutput + input - prevInput)
            prevInput = input
            prevOutput = result[i]
        }

        // Step 3: Normalize
        var maxAbs = 0f
        for (sample in result) {
            val abs = kotlin.math.abs(sample)
            if (abs > maxAbs) maxAbs = abs
        }

        if (maxAbs > 0.001f) {
            val scale = 0.9f / maxAbs
            for (i in result.indices) {
                result[i] *= scale
            }
        }

        return result
    }

    fun reset() {
        prevInput = 0f
        prevOutput = 0f
    }

    companion object {
        // Lowered from 70Hz to 30Hz - 70Hz was attenuating 82Hz (low E) by ~24%
        private const val HIGH_PASS_CUTOFF = 30f
    }
}
