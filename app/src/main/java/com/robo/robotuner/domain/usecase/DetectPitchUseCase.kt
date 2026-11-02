package com.robo.robotuner.domain.usecase

import com.robo.robotuner.domain.model.DetectedPitch
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
        var previous: DetectedPitch? = null
        audioRepository.startListening().collect { samples ->
            val pitch = pitchDetectorRepository.detectPitch(samples)
                ?.let { DetectedPitch.fromFrequency(it, previous) }
            previous = pitch
            emit(if (pitch != null) TunerState.Detected(pitch) else TunerState.Listening)
        }
    }.flowOn(dispatcher).conflate()
}
