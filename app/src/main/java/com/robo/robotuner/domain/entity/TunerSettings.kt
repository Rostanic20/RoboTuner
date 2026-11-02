package com.robo.robotuner.domain.entity

data class TunerSettings(
    val referenceFrequency: Double = DEFAULT_REFERENCE_FREQUENCY,
    val inTuneThreshold: Double = DEFAULT_IN_TUNE_THRESHOLD,
    val noiseThreshold: Double = DEFAULT_NOISE_THRESHOLD
) {
    companion object {
        const val DEFAULT_REFERENCE_FREQUENCY = 440.0
        const val DEFAULT_IN_TUNE_THRESHOLD = 5.0
        const val DEFAULT_NOISE_THRESHOLD = 0.02
    }
}
