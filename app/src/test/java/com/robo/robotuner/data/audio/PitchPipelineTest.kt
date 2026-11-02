package com.robo.robotuner.data.audio

import com.robo.robotuner.data.audio.TestUtils.GUITAR_STRINGS
import com.robo.robotuner.data.audio.TestUtils.HOP_SIZE
import com.robo.robotuner.data.audio.TestUtils.SAMPLE_RATE
import com.robo.robotuner.data.audio.TestUtils.ToneStream
import com.robo.robotuner.data.audio.TestUtils.centsError
import com.robo.robotuner.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class PitchPipelineTest {

    private val pipeline = PitchPipeline(SAMPLE_RATE)
    private val stream = ToneStream()
    private val guitar = listOf(0.25, 1.0, 0.7, 0.5, 0.35, 0.2, 0.15, 0.1)

    private fun hops(count: Int, block: () -> FloatArray): List<Double?> =
        List(count) { pipeline.detectPitch(block()) }

    private fun tone(count: Int, frequency: Double, harmonics: List<Double> = listOf(1.0)) =
        hops(count) { stream.next(HOP_SIZE, frequency, harmonics, noise = 0.003) }

    private var noiseSeed = 0

    private fun noise(count: Int, amplitude: Float) =
        hops(count) { TestUtils.generateNoise(HOP_SIZE, amplitude, seed = ++noiseSeed) }

    @Test
    fun `every guitar string with a weak fundamental reads in tune within half a cent`() {
        for (frequency in GUITAR_STRINGS) {
            pipeline.reset()
            val result = tone(10, frequency, guitar).last()
            assertNotNull("no pitch for $frequency Hz", result)
            val error = abs(centsError(result!!, frequency))
            assertTrue("$frequency Hz off by $error cents", error < 0.5)
        }
    }

    @Test
    fun `first reading appears on the fifth hop`() {
        val outputs = tone(5, 110.0)
        assertTrue(outputs.dropLast(1).all { it == null })
        assertNotNull(outputs.last())
    }

    @Test
    fun `peg turn of 20 cents settles within 6 hops`() {
        tone(10, 110.0)
        val sharp = 110.0 * 2.0.pow(20 / 1200.0)
        val outputs = tone(6, sharp)
        assertEquals(20.0, centsError(outputs.last()!!, 110.0), 0.5)
    }

    @Test
    fun `changing string never shows a note in between`() {
        tone(10, 82.41)
        val outputs = tone(10, 110.0).filterNotNull()
        val notes = outputs.map { Note.fromFrequency(it).first }.toSet()
        assertEquals(setOf(Note.E, Note.A), notes)
        assertEquals(0.0, centsError(outputs.last(), 110.0), 0.5)
    }

    @Test
    fun `string near a semitone boundary still shows a reading`() {
        val outputs = tone(8, 110.0 * 2.0.pow(48 / 1200.0))
        assertNotNull(outputs.last())
    }

    @Test
    fun `room noise after a note clears the reading within about a second`() {
        tone(10, 110.0)
        val outputs = noise(30, 0.003f)
        assertNotNull("holds briefly", outputs.first())
        assertNull("clears after hold", outputs.last())
    }

    @Test
    fun `quiet unprocessed-level signal is still read`() {
        val outputs = hops(8) { stream.next(HOP_SIZE, 110.0, guitar, noise = 0.00005, gain = 0.0004) }
        assertEquals(0.0, centsError(outputs.last()!!, 110.0), 0.5)
    }

    @Test
    fun `digital silence never produces a reading`() {
        assertTrue(hops(20) { FloatArray(HOP_SIZE) }.all { it == null })
    }

    @Test
    fun `room noise alone never produces a reading`() {
        assertTrue(noise(40, 0.003f).all { it == null })
    }
}
