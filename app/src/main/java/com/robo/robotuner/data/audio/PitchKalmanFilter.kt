package com.robo.robotuner.data.audio

class PitchKalmanFilter {

    private var estimate = 0.0
    private var errorEstimate = 1.0
    private var isInitialized = false

    fun filter(measurement: Double): Double {
        if (!isInitialized) {
            estimate = measurement
            errorEstimate = 1.0
            isInitialized = true
            return measurement
        }

        // Prediction
        val errorPrediction = errorEstimate + PROCESS_NOISE

        // Update
        val kalmanGain = errorPrediction / (errorPrediction + MEASUREMENT_NOISE)
        estimate = estimate + kalmanGain * (measurement - estimate)
        errorEstimate = (1 - kalmanGain) * errorPrediction

        return estimate
    }

    fun reset() {
        estimate = 0.0
        errorEstimate = 1.0
        isInitialized = false
    }

    fun getEstimate(): Double = estimate

    companion object {
        private const val PROCESS_NOISE = 0.01
        private const val MEASUREMENT_NOISE = 0.1
    }
}
