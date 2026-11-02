package com.robo.robotuner.data.audio

import com.robo.robotuner.domain.model.Note
import kotlin.math.abs

class PitchTracker {

    private val track = ArrayDeque<Double>()
    private val candidate = ArrayDeque<Double>()
    private var misses = 0

    fun update(frequency: Double?): Double? {
        val current = median(track)
        if (frequency != null && current != null && misses <= GAP_FRAMES && isClose(frequency, current)) {
            candidate.clear()
            misses = 0
            track.addLast(frequency)
            if (track.size > MEDIAN_FRAMES) track.removeFirst()
            return median(track)
        }

        misses++
        if (frequency == null) {
            candidate.clear()
        } else {
            if (candidate.isNotEmpty() && !isClose(frequency, candidate.last())) candidate.clear()
            candidate.addLast(frequency)
            if (candidate.size >= CONFIRM_FRAMES) {
                track.clear()
                track.addAll(candidate)
                candidate.clear()
                misses = 0
            }
        }
        if (misses > HOLD_FRAMES) track.clear()
        return median(track)
    }

    fun reset() {
        track.clear()
        candidate.clear()
        misses = 0
    }

    private fun isClose(a: Double, b: Double) = abs(Note.calculateCents(a, b)) <= JUMP_CENTS

    private fun median(values: Collection<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    companion object {
        private const val JUMP_CENTS = 60.0
        private const val CONFIRM_FRAMES = 3
        private const val MEDIAN_FRAMES = 5
        private const val GAP_FRAMES = 1
        private const val HOLD_FRAMES = 22
    }
}
