package com.robo.robotuner.data.audio

import com.robo.robotuner.data.audio.TestUtils.SAMPLE_RATE
import com.robo.robotuner.data.audio.TestUtils.centsError
import com.robo.robotuner.domain.model.Tuning
import com.robo.robotuner.domain.model.TunerSettings
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PluckSynthTest {

    private val synth = PluckSynth(SAMPLE_RATE)
    private val detector = YinPitchDetector()

    private val everyTarget = Tuning.entries
        .flatMap { tuning -> (0..TunerSettings.MAX_CAPO).flatMap { capo -> tuning.strings.map { it.transposed(capo) } } }
        .distinct()
        .map { it.frequency() }

    @Test
    fun `reference tone for every string and capo is within a tenth of a cent`() {
        for (frequency in everyTarget) {
            val tone = synth.render(frequency)
            val frame = tone.copyOfRange(SAMPLE_RATE / 4, SAMPLE_RATE / 4 + PitchPipeline.WINDOW_SIZE)
            val result = detector.detectPitch(frame, SAMPLE_RATE, 30.0)
            assertNotNull("no pitch for $frequency Hz", result)
            val error = abs(centsError(result!!, frequency))
            assertTrue("$frequency Hz off by $error cents", error < 0.1)
        }
    }

    @Test
    fun `tone never clips and starts and ends at silence`() {
        for (frequency in everyTarget) {
            val tone = synth.render(frequency)
            assertTrue("$frequency Hz clips", tone.all { abs(it) <= 1f })
            assertTrue("$frequency Hz clicks on start", abs(tone.first()) < 0.01f)
            assertTrue("$frequency Hz clicks on stop", abs(tone.last()) < 0.01f)
        }
    }

    @Test
    fun `tone decays like a plucked string`() {
        val tone = synth.render(110.0)
        val quarter = tone.size / 4
        val start = tone.copyOfRange(0, quarter).maxOf { abs(it) }
        val end = tone.copyOfRange(3 * quarter, tone.size).maxOf { abs(it) }
        assertTrue("start $start, end $end", end < start / 4)
    }
}
