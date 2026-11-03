package com.robo.robotuner.data.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

class PluckSynth(private val sampleRate: Int) {

    fun render(frequency: Double): FloatArray {
        val samples = FloatArray((sampleRate * DURATION_SECONDS).toInt())
        var harmonic = 1
        while (harmonic <= MAX_HARMONICS && harmonic * frequency < MAX_HARMONIC_HZ) {
            addHarmonic(samples, frequency, harmonic)
            harmonic++
        }
        shapeEdges(samples)
        normalize(samples)
        return samples
    }

    private fun addHarmonic(samples: FloatArray, frequency: Double, harmonic: Int) {
        val step = 2 * PI * harmonic * frequency / sampleRate
        val decayPerSample = exp(-BASE_DECAY * (1 + HARMONIC_DECAY * (harmonic - 1)) / sampleRate)
        var envelope = 1.0 / harmonic
        for (i in samples.indices) {
            samples[i] += (envelope * sin(step * i)).toFloat()
            envelope *= decayPerSample
        }
    }

    private fun shapeEdges(samples: FloatArray) {
        val attack = (sampleRate * ATTACK_SECONDS).toInt()
        val release = (sampleRate * RELEASE_SECONDS).toInt()
        for (i in 0 until attack) samples[i] *= i.toFloat() / attack
        for (i in 0 until release) samples[samples.size - 1 - i] *= i.toFloat() / release
    }

    private fun normalize(samples: FloatArray) {
        val peak = samples.maxOf { abs(it) }
        if (peak == 0f) return
        val gain = PEAK / peak
        for (i in samples.indices) samples[i] *= gain
    }

    companion object {
        const val DURATION_SECONDS = 2.0
        private const val ATTACK_SECONDS = 0.005
        private const val RELEASE_SECONDS = 0.1
        private const val MAX_HARMONICS = 24
        private const val MAX_HARMONIC_HZ = 5000.0
        private const val BASE_DECAY = 1.5
        private const val HARMONIC_DECAY = 0.15
        private const val PEAK = 0.8f
    }
}
