package com.robo.robotuner.presentation.tuner

import com.robo.robotuner.domain.model.Note
import com.robo.robotuner.domain.repository.AudioRepository
import com.robo.robotuner.domain.repository.PitchDetectorRepository
import com.robo.robotuner.domain.usecase.DetectPitchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    private val audio = object : AudioRepository {
        override fun startListening(): Flow<FloatArray> = flow {
            while (true) {
                emit(FloatArray(1))
                delay(HOP_MS)
            }
        }
    }

    private val detector = object : PitchDetectorRepository {
        override fun detectPitch(samples: FloatArray): Double? = frequency
        override fun reset() = Unit
    }

    private lateinit var viewModel: TunerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = TunerViewModel(DetectPitchUseCase(audio, detector, dispatcher))
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
            testScheduler.runCurrent()
        }
    }

    private fun TestScope.hear(hz: Double?, millis: Long = 200) {
        frequency = hz
        testScheduler.advanceTimeBy(millis)
        testScheduler.runCurrent()
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
        val events = mutableListOf<Unit>()
        backgroundScope.launch { viewModel.inTuneEvents.collect { events += it } }

        hear(110.0, millis = 300)
        assertTrue(viewModel.uiState.value.tunedStrings.isEmpty())
        assertTrue(events.isEmpty())

        hear(110.0, millis = 300)
        assertEquals(setOf(1), viewModel.uiState.value.tunedStrings)
        assertEquals(1, events.size)
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
    }

    private companion object {
        const val HOP_MS = 46L
    }
}
