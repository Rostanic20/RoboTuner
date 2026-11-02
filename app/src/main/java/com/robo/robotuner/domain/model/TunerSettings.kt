package com.robo.robotuner.domain.model

data class TunerSettings(
    val tuning: Tuning = Tuning.STANDARD,
    val referenceA4: Double = Note.DEFAULT_A4,
    val toleranceCents: Double = DEFAULT_TOLERANCE,
    val vibration: Boolean = true,
    val useFlats: Boolean = false
) {
    fun sanitized(): TunerSettings = copy(
        referenceA4 = referenceA4.coerceIn(MIN_A4, MAX_A4),
        toleranceCents = toleranceCents.takeIf { it in TOLERANCE_OPTIONS } ?: DEFAULT_TOLERANCE
    )

    companion object {
        const val MIN_A4 = 432.0
        const val MAX_A4 = 446.0
        const val DEFAULT_TOLERANCE = 3.0
        val TOLERANCE_OPTIONS = listOf(1.0, 2.0, 3.0, 5.0)
    }
}
