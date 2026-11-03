package com.robo.robotuner.domain.model

enum class Instrument(val lowestDetectableHz: Double) {
    GUITAR(60.0),
    BASS(30.0)
}
