package com.robo.robotuner.domain.usecase

import com.robo.robotuner.domain.model.DetectedPitch
import com.robo.robotuner.domain.model.TunerState
import com.robo.robotuner.domain.repository.AudioRepository
import com.robo.robotuner.domain.repository.PitchDetectorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DetectPitchUseCase(
    private val audioRepository: AudioRepository,
    private val pitchDetectorRepository: PitchDetectorRepository,
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE
) {
    fun execute(): Flow<TunerState> {
        return audioRepository.startListening()
            .map { audioSamples ->
                val frequency = pitchDetectorRepository.detectPitch(audioSamples, sampleRate)
                if (frequency != null && frequency > MIN_FREQUENCY && frequency < MAX_FREQUENCY) {
                    TunerState.Detected(DetectedPitch.fromFrequency(frequency))
                } else {
                    TunerState.Listening
                }
            }
    }

    fun stop() {
        audioRepository.stopListening()
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44100
        const val MIN_FREQUENCY = 20.0
        const val MAX_FREQUENCY = 5000.0
    }
}
