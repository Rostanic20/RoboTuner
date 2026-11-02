package com.robo.robotuner.data.audio

import com.robo.robotuner.domain.model.Note
import com.robo.robotuner.domain.repository.PitchDetectorRepository
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

class PitchPipeline(sampleRate: Int) : PitchDetectorRepository {

    private val preProcessor = AudioPreProcessor(sampleRate)
    private val hpsDetector = HpsPitchDetector()
    private val yinDetector = YinPitchDetector()
    private val kalmanFilter = PitchKalmanFilter()

    // Hysteresis state
    private var currentNote: Note? = null
    private var currentOctave: Int = 4
    private var noteConfirmCount = 0
    private var silenceCount = 0
    private var lastFrequency: Double? = null
    private var confirmedFrequency: Double? = null

    override fun detectPitch(audioSamples: FloatArray, sampleRate: Int): Double? {
        // Step 1: Pre-process audio
        val processed = preProcessor.process(audioSamples)

        // Check signal level - must be above noise floor
        val rms = calculateRMS(processed)
        if (rms < NOISE_THRESHOLD) {
            silenceCount++
            // Only reset after sustained silence
            if (silenceCount >= SILENCE_FRAMES_TO_RESET) {
                reset()
            }
            return null
        }
        silenceCount = 0

        // Step 2: HPS to find candidate fundamental
        val hpsResult = hpsDetector.detectPitch(processed, sampleRate)
        if (hpsResult == null || hpsResult.confidence < MIN_HPS_CONFIDENCE) {
            // Low confidence - don't update, but don't fully reset either
            noteConfirmCount = (noteConfirmCount - 1).coerceAtLeast(0)
            return confirmedFrequency
        }

        val candidateFreq = hpsResult.frequency

        // Step 3: YIN refinement around candidate (±1 semitone)
        val semitoneRatio = 2.0.pow(1.0 / 12.0)
        val minFreq = candidateFreq / semitoneRatio
        val maxFreq = candidateFreq * semitoneRatio

        val yinResult = yinDetector.detectPitch(processed, sampleRate, minFreq, maxFreq)

        // Use YIN if confident, otherwise fall back to HPS
        val rawFrequency = if (yinResult != null && yinResult.confidence > MIN_YIN_CONFIDENCE) {
            yinResult.frequency
        } else {
            candidateFreq
        }

        // Step 4: Kalman filter for smoothing
        val smoothedFrequency = kalmanFilter.filter(rawFrequency)

        // Step 5: Hysteresis - stabilize note display
        val (newNote, newOctave) = Note.fromFrequency(smoothedFrequency)

        if (newNote == currentNote && newOctave == currentOctave) {
            noteConfirmCount = (noteConfirmCount + 1).coerceAtMost(NOTE_CONFIRM_THRESHOLD + 5)
        } else {
            // Different note detected - decrease confidence
            noteConfirmCount = (noteConfirmCount - 2).coerceAtLeast(0)
            if (noteConfirmCount == 0) {
                currentNote = newNote
                currentOctave = newOctave
            }
        }

        lastFrequency = smoothedFrequency

        // Only return frequency after note is confirmed stable
        if (noteConfirmCount >= NOTE_CONFIRM_THRESHOLD) {
            confirmedFrequency = smoothedFrequency
            return smoothedFrequency
        }

        // Return last confirmed frequency if we have one, otherwise null
        return confirmedFrequency
    }

    fun reset() {
        preProcessor.reset()
        kalmanFilter.reset()
        noteConfirmCount = 0
        silenceCount = 0
        currentNote = null
        lastFrequency = null
        confirmedFrequency = null
    }

    private fun calculateRMS(samples: FloatArray): Float {
        var sum = 0f
        for (sample in samples) {
            sum += sample * sample
        }
        return kotlin.math.sqrt(sum / samples.size)
    }

    companion object {
        private const val NOISE_THRESHOLD = 0.04f          // Balance: reject noise but detect playing
        private const val MIN_HPS_CONFIDENCE = 0.20f       // Lowered for faster detection
        private const val MIN_YIN_CONFIDENCE = 0.70f       // Lowered for low frequencies
        private const val NOTE_CONFIRM_THRESHOLD = 2       // Faster response (was 4)
        private const val SILENCE_FRAMES_TO_RESET = 8      // Frames of silence before full reset
    }
}
