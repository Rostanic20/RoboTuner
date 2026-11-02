package com.robo.robotuner.presentation.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robo.robotuner.domain.model.TunerState
import com.robo.robotuner.domain.repository.SettingsRepository
import com.robo.robotuner.domain.usecase.DetectPitchUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TunerViewModel(
    private val detectPitchUseCase: DetectPitchUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val tunerState = MutableStateFlow<TunerState>(TunerState.Idle)
    private val isListening = MutableStateFlow(false)
    private val lockedString = MutableStateFlow<Int?>(null)
    private val tunedStrings = MutableStateFlow<Set<Int>>(emptySet())
    private val readingMapper = ReadingMapper()

    val uiState: StateFlow<TunerUiState> =
        combine(tunerState, isListening, lockedString, tunedStrings, settingsRepository.settings) {
                state, listening, locked, tuned, settings ->
            TunerUiState(
                isListening = listening,
                reading = readingMapper.map((state as? TunerState.Detected)?.frequency, settings, locked),
                settings = settings,
                lockedString = locked,
                tunedStrings = tuned,
                error = (state as? TunerState.Error)?.message
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, TunerUiState(settings = settingsRepository.settings.value))

    private val inTuneChannel = Channel<Unit>(Channel.CONFLATED)
    val inTuneEvents: Flow<Unit> = inTuneChannel.receiveAsFlow()

    private var listeningJob: Job? = null
    private var wantsListening = false

    init {
        viewModelScope.launch {
            uiState
                .map { state -> state.reading?.takeIf { it.isInTune }?.let { Triple(it.note, it.octave, it.stringIndex) } }
                .distinctUntilChanged()
                .collectLatest { inTune ->
                    if (inTune == null) return@collectLatest
                    delay(IN_TUNE_HOLD_MS)
                    inTune.third?.let { index -> tunedStrings.update { it + index } }
                    if (settingsRepository.settings.value.vibration) inTuneChannel.send(Unit)
                }
        }
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.tuning to it.referenceA4 }
                .distinctUntilChanged()
                .drop(1)
                .collect { tunedStrings.value = emptySet() }
        }
    }

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

    fun toggleStringLock(index: Int) {
        lockedString.update { if (it == index) null else index }
    }

    private fun listen() {
        if (listeningJob?.isActive == true) return
        val previousJob = listeningJob
        isListening.value = true
        tunerState.value = TunerState.Listening
        listeningJob = viewModelScope.launch {
            previousJob?.cancelAndJoin()
            detectPitchUseCase.execute()
                .catch { e ->
                    wantsListening = false
                    isListening.value = false
                    tunerState.value = TunerState.Error(e.message ?: "Unknown error")
                }
                .collect { tunerState.value = it }
        }
    }

    private fun halt() {
        val job = listeningJob ?: return
        if (!job.isActive) return
        job.cancel()
        isListening.value = false
        tunerState.value = TunerState.Idle
    }

    companion object {
        const val IN_TUNE_HOLD_MS = 400L
    }
}
