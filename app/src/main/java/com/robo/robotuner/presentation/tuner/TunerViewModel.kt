package com.robo.robotuner.presentation.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robo.robotuner.domain.model.TunerState
import com.robo.robotuner.domain.usecase.DetectPitchUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TunerViewModel(
    private val detectPitchUseCase: DetectPitchUseCase
) : ViewModel() {

    private val _tunerState = MutableStateFlow<TunerState>(TunerState.Idle)
    val tunerState: StateFlow<TunerState> = _tunerState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var listeningJob: Job? = null

    fun startListening() {
        if (_isListening.value) return

        _isListening.value = true
        _tunerState.value = TunerState.Listening

        listeningJob = viewModelScope.launch {
            detectPitchUseCase.execute()
                .catch { e ->
                    _tunerState.value = TunerState.Error(e.message ?: "Unknown error")
                    _isListening.value = false
                }
                .collect { state ->
                    _tunerState.value = state
                }
        }
    }

    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        detectPitchUseCase.stop()
        _isListening.value = false
        _tunerState.value = TunerState.Idle
    }

    fun toggleListening() {
        if (_isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
