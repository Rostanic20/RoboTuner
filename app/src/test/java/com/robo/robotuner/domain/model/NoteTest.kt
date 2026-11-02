package com.robo.robotuner.domain.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class NoteTest {

    @Test
    fun `A4 frequency is 440Hz`() {
        val frequency = Note.getFrequency(Note.A, 4)
        assertEquals(440.0, frequency, 0.01)
    }

    @Test
    fun `C4 frequency is approximately 261Hz`() {
        val frequency = Note.getFrequency(Note.C, 4)
        assertEquals(261.63, frequency, 0.1)
    }

    @Test
    fun `octave doubles frequency`() {
        val a4 = Note.getFrequency(Note.A, 4)
        val a5 = Note.getFrequency(Note.A, 5)

        assertEquals(a4 * 2, a5, 0.01)
    }

    @Test
    fun `detect A4 from 440Hz`() {
        val (note, octave) = Note.fromFrequency(440.0)

        assertEquals(Note.A, note)
        assertEquals(4, octave)
    }

    @Test
    fun `detect low E from 82Hz`() {
        val (note, octave) = Note.fromFrequency(82.41)

        assertEquals(Note.E, note)
        assertEquals(2, octave)
    }

    @Test
    fun `detect all chromatic notes`() {
        for (noteEnum in Note.entries) {
            val frequency = Note.getFrequency(noteEnum, 4)
            val (detectedNote, detectedOctave) = Note.fromFrequency(frequency)

            assertEquals("${noteEnum.displayName} should be detected", noteEnum, detectedNote)
            assertEquals("Octave should be 4", 4, detectedOctave)
        }
    }

    @Test
    fun `cents calculation for in-tune note`() {
        val cents = Note.calculateCents(440.0, 440.0)
        assertEquals(0.0, cents, 0.01)
    }

    @Test
    fun `cents calculation for sharp note`() {
        // 450Hz is sharp of 440Hz
        val cents = Note.calculateCents(450.0, 440.0)
        assertTrue("Should be positive (sharp)", cents > 0)
        assertTrue("Should be reasonable cents value", cents < 50)
    }

    @Test
    fun `cents calculation for flat note`() {
        // 430Hz is flat of 440Hz
        val cents = Note.calculateCents(430.0, 440.0)
        assertTrue("Should be negative (flat)", cents < 0)
        assertTrue("Should be reasonable cents value", cents > -50)
    }

    @Test
    fun `semitone is 100 cents`() {
        val a4 = 440.0
        val aSharp4 = Note.getFrequency(Note.A_SHARP, 4)

        val cents = abs(Note.calculateCents(aSharp4, a4))
        assertEquals(100.0, cents, 1.0)
    }

    @Test
    fun `guitar strings frequencies`() {
        val guitarStrings = mapOf(
            Pair(Note.E, 2) to 82.41,
            Pair(Note.A, 2) to 110.0,
            Pair(Note.D, 3) to 146.83,
            Pair(Note.G, 3) to 196.0,
            Pair(Note.B, 3) to 246.94,
            Pair(Note.E, 4) to 329.63
        )

        for ((noteOctave, expectedFreq) in guitarStrings) {
            val (note, octave) = noteOctave
            val frequency = Note.getFrequency(note, octave)

            assertEquals(
                "${note.displayName}$octave should be ~$expectedFreq Hz",
                expectedFreq,
                frequency,
                1.0
            )
        }
    }
}
