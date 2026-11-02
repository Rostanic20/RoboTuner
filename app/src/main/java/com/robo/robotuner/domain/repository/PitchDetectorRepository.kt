package com.robo.robotuner.domain.repository

interface PitchDetectorRepository {
    fun detectPitch(audioSamples: FloatArray, sampleRate: Int): Double?
}
