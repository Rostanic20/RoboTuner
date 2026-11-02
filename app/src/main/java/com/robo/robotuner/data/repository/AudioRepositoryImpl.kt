package com.robo.robotuner.data.repository

import com.robo.robotuner.data.audio.AudioRecorder
import com.robo.robotuner.domain.repository.AudioRepository
import kotlinx.coroutines.flow.Flow

class AudioRepositoryImpl(
    private val audioRecorder: AudioRecorder
) : AudioRepository {

    override fun startListening(): Flow<FloatArray> {
        return audioRecorder.startRecording()
    }

    override fun stopListening() {
        audioRecorder.stopRecording()
    }

    override fun isListening(): Boolean {
        return audioRecorder.isRecording()
    }
}
