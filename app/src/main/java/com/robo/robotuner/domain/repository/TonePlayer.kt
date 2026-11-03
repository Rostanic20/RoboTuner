package com.robo.robotuner.domain.repository

interface TonePlayer {
    suspend fun play(frequency: Double)
}
