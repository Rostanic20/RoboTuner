package com.robo.robotuner.presentation.tuner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.robo.robotuner.data.audio.AudioRecorder
import com.robo.robotuner.data.audio.PitchPipeline
import com.robo.robotuner.data.repository.AudioRepositoryImpl
import com.robo.robotuner.data.repository.SharedPrefsSettingsRepository
import com.robo.robotuner.domain.usecase.DetectPitchUseCase

class TunerViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TunerViewModel::class.java)) {
            val appContext = context.applicationContext
            val audioRecorder = AudioRecorder(appContext)
            val detectPitchUseCase = DetectPitchUseCase(
                audioRepository = AudioRepositoryImpl(audioRecorder),
                pitchDetectorRepository = PitchPipeline(audioRecorder.sampleRate)
            )
            return TunerViewModel(detectPitchUseCase, SharedPrefsSettingsRepository.get(appContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
