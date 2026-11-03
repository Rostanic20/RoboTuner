package com.robo.robotuner.domain.model

import com.robo.robotuner.domain.model.Instrument.BASS
import com.robo.robotuner.domain.model.Instrument.GUITAR
import com.robo.robotuner.domain.model.Note.A
import com.robo.robotuner.domain.model.Note.A_SHARP
import com.robo.robotuner.domain.model.Note.B
import com.robo.robotuner.domain.model.Note.C
import com.robo.robotuner.domain.model.Note.C_SHARP
import com.robo.robotuner.domain.model.Note.D
import com.robo.robotuner.domain.model.Note.D_SHARP
import com.robo.robotuner.domain.model.Note.E
import com.robo.robotuner.domain.model.Note.F
import com.robo.robotuner.domain.model.Note.F_SHARP
import com.robo.robotuner.domain.model.Note.G
import com.robo.robotuner.domain.model.Note.G_SHARP

enum class Tuning(val instrument: Instrument, val strings: List<GuitarString>) {
    STANDARD(GUITAR, strings(E to 2, A to 2, D to 3, G to 3, B to 3, E to 4)),
    DROP_D(GUITAR, strings(D to 2, A to 2, D to 3, G to 3, B to 3, E to 4)),
    HALF_STEP_DOWN(GUITAR, strings(D_SHARP to 2, G_SHARP to 2, C_SHARP to 3, F_SHARP to 3, A_SHARP to 3, D_SHARP to 4)),
    D_STANDARD(GUITAR, strings(D to 2, G to 2, C to 3, F to 3, A to 3, D to 4)),
    DROP_C_SHARP(GUITAR, strings(C_SHARP to 2, G_SHARP to 2, C_SHARP to 3, F_SHARP to 3, A_SHARP to 3, D_SHARP to 4)),
    DROP_C(GUITAR, strings(C to 2, G to 2, C to 3, F to 3, A to 3, D to 4)),
    DADGAD(GUITAR, strings(D to 2, A to 2, D to 3, G to 3, A to 3, D to 4)),
    OPEN_G(GUITAR, strings(D to 2, G to 2, D to 3, G to 3, B to 3, D to 4)),
    OPEN_D(GUITAR, strings(D to 2, A to 2, D to 3, F_SHARP to 3, A to 3, D to 4)),
    OPEN_E(GUITAR, strings(E to 2, B to 2, E to 3, G_SHARP to 3, B to 3, E to 4)),
    OPEN_C(GUITAR, strings(C to 2, G to 2, C to 3, G to 3, C to 4, E to 4)),
    BASS_STANDARD(BASS, strings(E to 1, A to 1, D to 2, G to 2)),
    BASS_DROP_D(BASS, strings(D to 1, A to 1, D to 2, G to 2)),
    BASS_HALF_STEP_DOWN(BASS, strings(D_SHARP to 1, G_SHARP to 1, C_SHARP to 2, F_SHARP to 2)),
    BASS_D_STANDARD(BASS, strings(D to 1, G to 1, C to 2, F to 2)),
    BASS_DROP_C(BASS, strings(C to 1, G to 1, C to 2, F to 2))
}

private fun strings(vararg notes: Pair<Note, Int>) = notes.map { (note, octave) -> GuitarString(note, octave) }
