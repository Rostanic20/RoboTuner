package com.robo.robotuner.data.audio

import kotlin.math.PI

class AudioPreProcessor(sampleRate: Int) {

    private val alpha: Float
    private var prevInput = 0f
    private var prevOutput = 0f

    init {
        val rc = 1f / (2f * PI.toFloat() * HIGH_PASS_CUTOFF)
        val dt = 1f / sampleRate
        alpha = rc / (rc + dt)
    }

    fun process(samples: FloatArray): FloatArray {
        val result = FloatArray(samples.size)
        for (i in samples.indices) {
            val input = samples[i]
            result[i] = alpha * (prevOutput + input - prevInput)
            prevInput = input
            prevOutput = result[i]
        }
        return result
    }

    fun reset() {
        prevInput = 0f
        prevOutput = 0f
    }

    companion object {
        private const val HIGH_PASS_CUTOFF = 30f
    }
}
