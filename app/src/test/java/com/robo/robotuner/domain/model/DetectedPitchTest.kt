package com.robo.robotuner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class DetectedPitchTest {

    private fun cents(from: Double, cents: Double) = from * 2.0.pow(cents / 1200)

    @Test
    fun `nearest note without history`() {
        val pitch = DetectedPitch.fromFrequency(cents(110.0, 55.0))
        assertEquals(Note.A_SHARP, pitch.note)
        assertEquals(-45.0, pitch.cents, 0.01)
    }

    @Test
    fun `note label holds across the semitone boundary while within 60 cents`() {
        val previous = DetectedPitch.fromFrequency(110.0)
        val pitch = DetectedPitch.fromFrequency(cents(110.0, 55.0), previous)
        assertEquals(Note.A, pitch.note)
        assertEquals(55.0, pitch.cents, 0.01)
    }

    @Test
    fun `note label switches beyond 60 cents`() {
        val previous = DetectedPitch.fromFrequency(110.0)
        val pitch = DetectedPitch.fromFrequency(cents(110.0, 65.0), previous)
        assertEquals(Note.A_SHARP, pitch.note)
        assertEquals(2, pitch.octave)
    }
}
