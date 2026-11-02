package com.robo.robotuner.domain.usecase

import com.robo.robotuner.domain.model.TunerState
import com.robo.robotuner.domain.repository.AudioRepository
import com.robo.robotuner.domain.repository.PitchDetectorRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DetectPitchUseCase(
    private val audioRepository: AudioRepository,
    private val pitchDetectorRepository: PitchDetectorRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    fun execute(): Flow<TunerState> = flow {
        pitchDetectorRepository.reset()
        audioRepository.startListening().collect { samples ->
            val frequency = pitchDetectorRepository.detectPitch(samples)
            emit(if (frequency != null) TunerState.Detected(frequency) else TunerState.Listening)
        }
    }.flowOn(dispatcher).conflate()
}
