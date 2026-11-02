package com.robo.robotuner.presentation.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robo.robotuner.domain.model.TunerState
import com.robo.robotuner.domain.usecase.DetectPitchUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
    private var wantsListening = false

    fun startListening() {
        wantsListening = true
        listen()
    }

    fun stopListening() {
        wantsListening = false
        halt()
    }

    fun onScreenVisible() {
        if (wantsListening) listen()
    }

    fun onScreenHidden() {
        halt()
    }

    private fun listen() {
        if (listeningJob?.isActive == true) return
        val previousJob = listeningJob
        _isListening.value = true
        _tunerState.value = TunerState.Listening
        listeningJob = viewModelScope.launch {
            previousJob?.cancelAndJoin()
            detectPitchUseCase.execute()
                .catch { e ->
                    wantsListening = false
                    _isListening.value = false
                    _tunerState.value = TunerState.Error(e.message ?: "Unknown error")
                }
                .collect { _tunerState.value = it }
        }
    }

    private fun halt() {
        val job = listeningJob ?: return
        if (!job.isActive) return
        job.cancel()
        _isListening.value = false
        _tunerState.value = TunerState.Idle
    }
}
