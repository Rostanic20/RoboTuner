package com.robo.robotuner.domain.model

sealed class TunerState {
    data object Idle : TunerState()
    data object Listening : TunerState()
    data class Detected(val frequency: Double) : TunerState()
    data class Error(val message: String) : TunerState()
}
