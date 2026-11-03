package com.robo.robotuner.presentation.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robo.robotuner.domain.model.TunerState
import com.robo.robotuner.domain.repository.SettingsRepository
import com.robo.robotuner.domain.repository.TonePlayer
import com.robo.robotuner.domain.usecase.DetectPitchUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TunerViewModel(
    private val detectPitchUseCase: DetectPitchUseCase,
    private val settingsRepository: SettingsRepository,
    private val tonePlayer: TonePlayer
) : ViewModel() {

    private val tunerState = MutableStateFlow<TunerState>(TunerState.Idle)
    private val wantsListening = MutableStateFlow(false)
    private val marks = MutableStateFlow(StringMarks())
    private val readingMapper = ReadingMapper()

    val uiState: StateFlow<TunerUiState> =
        combine(tunerState, wantsListening, marks, settingsRepository.settings) { state, listening, marks, settings ->
            TunerUiState(
                isListening = listening,
                reading = readingMapper.map((state as? TunerState.Detected)?.frequency, settings, marks.locked),
                settings = settings,
                lockedString = marks.locked,
                tunedStrings = marks.tuned,
                playingString = marks.playing,
                error = (state as? TunerState.Error)?.message
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, TunerUiState(settings = settingsRepository.settings.value))

    private val eventChannel = Channel<TunerEvent>(Channel.CONFLATED)
    val events: Flow<TunerEvent> = eventChannel.receiveAsFlow()

    private var listeningJob: Job? = null
    private var toneJob: Job? = null

    init {
        viewModelScope.launch {
            uiState
                .map { state -> state.reading?.takeIf { it.isInTune }?.let { Triple(it.note, it.octave, it.stringIndex) } }
                .distinctUntilChanged()
                .collectLatest { inTune ->
                    if (inTune == null) return@collectLatest
                    delay(IN_TUNE_HOLD_MS)
                    val completed = inTune.third?.let(::markTuned) ?: false
                    if (settingsRepository.settings.value.vibration) {
                        eventChannel.send(if (completed) TunerEvent.ALL_TUNED else TunerEvent.STRING_TUNED)
                    }
                }
        }
        viewModelScope.launch {
            settingsRepository.settings
                .map { Triple(it.tuning, it.referenceA4, it.capo) }
                .distinctUntilChanged()
                .drop(1)
                .collect { (tuning) ->
                    marks.update { old ->
                        old.copy(tuned = emptySet(), locked = old.locked?.takeIf { it in tuning.strings.indices })
                    }
                }
        }
    }

    fun startListening() {
        toneJob?.cancel()
        wantsListening.value = true
        listen()
    }

    fun stopListening() {
        wantsListening.value = false
        halt()
    }

    fun onScreenVisible() {
        if (wantsListening.value && toneJob?.isActive != true) listen()
    }

    fun onScreenHidden() {
        toneJob?.cancel()
        halt()
    }

    fun toggleStringLock(index: Int) {
        marks.update { it.copy(locked = if (it.locked == index) null else index) }
    }

    fun playReference(index: Int) {
        val settings = settingsRepository.settings.value
        val frequency = settings.strings.getOrNull(index)?.frequency(settings.referenceA4) ?: return
        val previousTone = toneJob
        toneJob = viewModelScope.launch {
            previousTone?.cancelAndJoin()
            halt()
            marks.update { it.copy(playing = index) }
            try {
                tonePlayer.play(frequency)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                eventChannel.send(TunerEvent.TONE_FAILED)
            } finally {
                marks.update { it.copy(playing = null) }
            }
            if (wantsListening.value) listen()
        }
    }

    private fun markTuned(index: Int): Boolean {
        val stringCount = settingsRepository.settings.value.strings.size
        var completed = false
        marks.update { old ->
            old.copy(tuned = old.tuned + index).also {
                completed = it.tuned.size == stringCount && old.tuned.size < stringCount
            }
        }
        return completed
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun listen() {
        if (listeningJob?.isActive == true) return
        val previousJob = listeningJob
        tunerState.value = TunerState.Listening
        listeningJob = viewModelScope.launch {
            previousJob?.cancelAndJoin()
            settingsRepository.settings
                .map { it.tuning.instrument }
                .distinctUntilChanged()
                .flatMapLatest { detectPitchUseCase.execute(it.lowestDetectableHz) }
                .catch { e ->
                    wantsListening.value = false
                    tunerState.value = TunerState.Error(e.message ?: "Unknown error")
                }
                .collect { tunerState.value = it }
        }
    }

    private fun halt() {
        val job = listeningJob ?: return
        if (!job.isActive) return
        job.cancel()
        tunerState.value = TunerState.Idle
    }

    private data class StringMarks(
        val locked: Int? = null,
        val tuned: Set<Int> = emptySet(),
        val playing: Int? = null
    )

    companion object {
        const val IN_TUNE_HOLD_MS = 400L
    }
}

enum class TunerEvent { STRING_TUNED, ALL_TUNED, TONE_FAILED }
