package com.robo.robotuner.data.audio

import com.robo.robotuner.domain.repository.PitchDetectorRepository
import kotlin.math.sqrt

class PitchPipeline(private val sampleRate: Int) : PitchDetectorRepository {

    private val preProcessor = AudioPreProcessor(sampleRate)
    private val detector = YinPitchDetector()
    private val tracker = PitchTracker()
    private val window = FloatArray(WINDOW_SIZE)
    private var filled = 0

    override fun detectPitch(samples: FloatArray): Double? {
        append(preProcessor.process(samples))
        if (filled < WINDOW_SIZE) return null
        val reading = if (rms(window) >= MIN_RMS) detector.detectPitch(window, sampleRate) else null
        return tracker.update(reading)
    }

    override fun reset() {
        preProcessor.reset()
        tracker.reset()
        window.fill(0f)
        filled = 0
    }

    private fun append(samples: FloatArray) {
        val count = minOf(samples.size, WINDOW_SIZE)
        System.arraycopy(window, count, window, 0, WINDOW_SIZE - count)
        System.arraycopy(samples, samples.size - count, window, WINDOW_SIZE - count, count)
        filled = minOf(filled + count, WINDOW_SIZE)
    }

    private fun rms(samples: FloatArray): Float {
        var sum = 0.0
        for (sample in samples) sum += sample * sample
        return sqrt(sum / samples.size).toFloat()
    }

    companion object {
        const val WINDOW_SIZE = 6144
        private const val MIN_RMS = 0.0002f
    }
}
