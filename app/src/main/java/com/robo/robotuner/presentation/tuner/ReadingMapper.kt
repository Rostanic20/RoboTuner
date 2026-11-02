package com.robo.robotuner.presentation.tuner

import com.robo.robotuner.domain.model.DetectedPitch
import com.robo.robotuner.domain.model.TunerSettings

class ReadingMapper {
    private var previous: DetectedPitch? = null
    private var previousA4: Double? = null

    fun map(frequency: Double?, settings: TunerSettings, lockedString: Int?): TunerReading? {
        if (frequency == null) {
            previous = null
            return null
        }
        val hysteresis = previous.takeIf { previousA4 == settings.referenceA4 }
        val pitch = DetectedPitch.fromFrequency(frequency, hysteresis, settings.referenceA4)
        previous = pitch
        previousA4 = settings.referenceA4
        return TunerReading.from(pitch, settings, lockedString)
    }
}
