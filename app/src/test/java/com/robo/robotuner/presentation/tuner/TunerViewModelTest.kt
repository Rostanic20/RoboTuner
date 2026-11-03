package com.robo.robotuner.presentation.tuner

import com.robo.robotuner.domain.model.Instrument
import com.robo.robotuner.domain.model.Note
import com.robo.robotuner.domain.model.Tuning
import com.robo.robotuner.domain.model.TunerSettings
import com.robo.robotuner.domain.repository.AudioRepository
import com.robo.robotuner.domain.repository.PitchDetectorRepository
import com.robo.robotuner.domain.repository.SettingsRepository
import com.robo.robotuner.domain.repository.TonePlayer
import com.robo.robotuner.domain.usecase.DetectPitchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TunerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var frequency: Double? = null
    private var openMics = 0
    private val detectorFloors = mutableListOf<Double>()
    private val tones = mutableListOf<Double>()
    private var toneFails = false

    private val audio = object : AudioRepository {
        override fun startListening(): Flow<FloatArray> = flow {
            openMics++
            try {
                while (true) {
                    emit(FloatArray(1))
                    delay(HOP_MS)
                }
            } finally {
                openMics--
            }
        }
    }

    private val detector = object : PitchDetectorRepository {
        override fun detectPitch(samples: FloatArray): Double? = frequency
        override fun reset(lowestFrequency: Double) {
            detectorFloors += lowestFrequency
        }
    }

    private val tonePlayer = object : TonePlayer {
        override suspend fun play(frequency: Double) {
            tones += frequency
            check(!toneFails) { "no audio output" }
            delay(TONE_MS)
        }
    }

    private val settings = object : SettingsRepository {
        private val state = MutableStateFlow(TunerSettings())
        override val settings: StateFlow<TunerSettings> = state
        override fun update(transform: (TunerSettings) -> TunerSettings) = state.update { transform(it).sanitized() }
    }

    private lateinit var viewModel: TunerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = TunerViewModel(DetectPitchUseCase(audio, detector, dispatcher), settings, tonePlayer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun listeningTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        viewModel.startListening()
        try {
            block()
        } finally {
            viewModel.stopListening()
            viewModel.onScreenHidden()
            testScheduler.runCurrent()
        }
    }

    private fun TestScope.hear(hz: Double?, millis: Long = 200) {
        frequency = hz
        testScheduler.advanceTimeBy(millis)
        testScheduler.runCurrent()
    }

    private fun TestScope.collectEvents(): List<TunerEvent> {
        val events = mutableListOf<TunerEvent>()
        backgroundScope.launch { viewModel.events.collect { events += it } }
        return events
    }

    @Test
    fun `reading follows the closest string`() = listeningTest {
        hear(106.8)
        val reading = viewModel.uiState.value.reading!!
        assertEquals(Note.A, reading.note)
        assertEquals(1, reading.stringIndex)
        assertTrue(reading.isFlat)
    }

    @Test
    fun `tapping a string locks it and tapping again unlocks`() = listeningTest {
        viewModel.toggleStringLock(0)
        hear(110.0)
        assertEquals(0, viewModel.uiState.value.lockedString)
        assertEquals(Note.E, viewModel.uiState.value.reading!!.note)

        viewModel.toggleStringLock(0)
        hear(110.0)
        assertNull(viewModel.uiState.value.lockedString)
        assertEquals(Note.A, viewModel.uiState.value.reading!!.note)
    }

    @Test
    fun `string is marked tuned and vibrates only after staying in tune`() = listeningTest {
        val events = collectEvents()

        hear(110.0, millis = 300)
        assertTrue(viewModel.uiState.value.tunedStrings.isEmpty())
        assertTrue(events.isEmpty())

        hear(110.0, millis = 300)
        assertEquals(setOf(1), viewModel.uiState.value.tunedStrings)
        assertEquals(listOf(TunerEvent.STRING_TUNED), events)
    }

    @Test
    fun `wobbling in and out of tune never counts as tuned`() = listeningTest {
        repeat(6) {
            hear(110.0, millis = 200)
            hear(112.0, millis = 200)
        }
        assertTrue(viewModel.uiState.value.tunedStrings.isEmpty())
    }

    @Test
    fun `stopping clears the reading and the listening flag`() = listeningTest {
        hear(110.0)
        viewModel.stopListening()
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isListening)
        assertNull(viewModel.uiState.value.reading)
        assertEquals(0, openMics)
    }

    @Test
    fun `vibration off still marks the string tuned but sends no event`() = listeningTest {
        settings.update { it.copy(vibration = false) }
        val events = collectEvents()
        hear(110.0, millis = 600)
        assertEquals(setOf(1), viewModel.uiState.value.tunedStrings)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `changing tuning clears the tuned checklist and retargets the strings`() = listeningTest {
        hear(110.0, millis = 600)
        assertEquals(setOf(1), viewModel.uiState.value.tunedStrings)

        settings.update { it.copy(tuning = Tuning.DROP_D) }
        hear(73.42)
        assertTrue(viewModel.uiState.value.tunedStrings.isEmpty())
        assertEquals(Note.D, viewModel.uiState.value.reading!!.note)
        assertEquals(0, viewModel.uiState.value.reading!!.stringIndex)
    }

    @Test
    fun `changing reference pitch applies to the current note immediately`() = listeningTest {
        hear(440.0)
        assertEquals(0.0, viewModel.uiState.value.reading!!.cents, 0.01)
        settings.update { it.copy(referenceA4 = 442.0) }
        hear(440.0)
        assertEquals(-7.85, viewModel.uiState.value.reading!!.cents, 0.01)
    }

    @Test
    fun `tuning the last string sends all tuned once`() = listeningTest {
        val events = collectEvents()
        for (hz in GUITAR) hear(hz, millis = 600)
        assertTrue(viewModel.uiState.value.allTuned)
        assertEquals(List(5) { TunerEvent.STRING_TUNED } + TunerEvent.ALL_TUNED, events)

        hear(null)
        hear(110.0, millis = 600)
        assertEquals(TunerEvent.STRING_TUNED, events.last())
        assertEquals(1, events.count { it == TunerEvent.ALL_TUNED })
    }

    @Test
    fun `capo retargets the strings to the fretted notes`() = listeningTest {
        settings.update { it.copy(capo = 2) }
        hear(92.50)
        val reading = viewModel.uiState.value.reading!!
        assertEquals(Note.F_SHARP, reading.note)
        assertEquals(0, reading.stringIndex)
        assertTrue(reading.isInTune)
    }

    @Test
    fun `capo change clears the tuned checklist`() = listeningTest {
        hear(110.0, millis = 600)
        settings.update { it.copy(capo = 1) }
        hear(null)
        assertTrue(viewModel.uiState.value.tunedStrings.isEmpty())
    }

    @Test
    fun `switching to bass restarts the detector with the bass floor`() = listeningTest {
        hear(null)
        assertEquals(listOf(Instrument.GUITAR.lowestDetectableHz), detectorFloors)

        settings.update { it.copy(tuning = Tuning.BASS_STANDARD) }
        hear(41.20)
        assertEquals(listOf(Instrument.GUITAR.lowestDetectableHz, Instrument.BASS.lowestDetectableHz), detectorFloors)
        assertEquals(1, openMics)
        assertEquals(Note.E, viewModel.uiState.value.reading!!.note)
        assertEquals(1, viewModel.uiState.value.reading!!.octave)

        settings.update { it.copy(tuning = Tuning.BASS_DROP_D) }
        hear(null)
        assertEquals(2, detectorFloors.size)
    }

    @Test
    fun `lock on a string the new tuning lacks is released`() = listeningTest {
        viewModel.toggleStringLock(1)
        settings.update { it.copy(tuning = Tuning.BASS_STANDARD) }
        hear(null)
        assertEquals(1, viewModel.uiState.value.lockedString)

        viewModel.toggleStringLock(3)
        settings.update { it.copy(tuning = Tuning.STANDARD) }
        hear(null)
        assertEquals(3, viewModel.uiState.value.lockedString)

        viewModel.toggleStringLock(5)
        settings.update { it.copy(tuning = Tuning.BASS_STANDARD) }
        hear(null)
        assertNull(viewModel.uiState.value.lockedString)
    }

    @Test
    fun `reference tone closes the mic while it plays and reopens it after`() = listeningTest {
        hear(110.0)
        assertEquals(1, openMics)

        viewModel.playReference(1)
        hear(110.0)
        assertEquals(0, openMics)
        assertEquals(1, viewModel.uiState.value.playingString)
        assertNull(viewModel.uiState.value.reading)
        assertTrue(viewModel.uiState.value.isListening)
        assertEquals(listOf(110.0), tones)

        hear(110.0, millis = TONE_MS)
        assertEquals(1, openMics)
        assertNull(viewModel.uiState.value.playingString)
        assertEquals(Note.A, viewModel.uiState.value.reading!!.note)
    }

    @Test
    fun `reference tone in tune does not mark the string tuned`() = listeningTest {
        viewModel.playReference(1)
        hear(110.0, millis = TONE_MS - 100)
        assertTrue(viewModel.uiState.value.tunedStrings.isEmpty())
    }

    @Test
    fun `reference tone follows capo and reference pitch`() = listeningTest {
        settings.update { it.copy(capo = 2, referenceA4 = 442.0) }
        viewModel.playReference(0)
        hear(null)
        assertEquals(Note.getFrequency(Note.F_SHARP, 2, 442.0), tones.single(), 1e-9)
    }

    @Test
    fun `holding another string replaces the playing tone`() = listeningTest {
        viewModel.playReference(0)
        hear(null, millis = 500)
        viewModel.playReference(5)
        hear(null)
        assertEquals(5, viewModel.uiState.value.playingString)
        assertEquals(0, openMics)
        hear(null, millis = TONE_MS)
        assertEquals(1, openMics)
    }

    @Test
    fun `starting the tuner during a tone stops the tone`() = runTest(dispatcher) {
        viewModel.playReference(0)
        testScheduler.runCurrent()
        assertEquals(0, viewModel.uiState.value.playingString)

        viewModel.startListening()
        hear(110.0)
        assertNull(viewModel.uiState.value.playingString)
        assertEquals(1, openMics)
        viewModel.stopListening()
        testScheduler.runCurrent()
    }

    @Test
    fun `tone with the tuner off leaves it off`() = runTest(dispatcher) {
        viewModel.playReference(0)
        hear(null, millis = TONE_MS + 200)
        assertEquals(0, openMics)
        assertFalse(viewModel.uiState.value.isListening)
    }

    @Test
    fun `hiding the screen during a tone stops it and the mic stays closed until shown`() = listeningTest {
        viewModel.playReference(0)
        hear(null)
        viewModel.onScreenHidden()
        hear(null, millis = TONE_MS + 200)
        assertNull(viewModel.uiState.value.playingString)
        assertEquals(0, openMics)

        viewModel.onScreenVisible()
        hear(null)
        assertEquals(1, openMics)
    }

    @Test
    fun `failed tone is reported and listening resumes`() = listeningTest {
        val events = collectEvents()
        toneFails = true
        viewModel.playReference(0)
        hear(null)
        assertEquals(listOf(TunerEvent.TONE_FAILED), events)
        assertNull(viewModel.uiState.value.playingString)
        assertEquals(1, openMics)
    }

    private companion object {
        const val HOP_MS = 46L
        const val TONE_MS = 2000L
        val GUITAR = listOf(82.41, 110.0, 146.83, 196.0, 246.94, 329.63)
    }
}
