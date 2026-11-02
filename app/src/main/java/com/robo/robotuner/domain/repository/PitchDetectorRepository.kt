package com.robo.robotuner.domain.repository

interface PitchDetectorRepository {
    fun detectPitch(samples: FloatArray): Double?
    fun reset()
}
