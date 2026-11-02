package com.robo.robotuner.domain.repository

import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    fun startListening(): Flow<FloatArray>
}
