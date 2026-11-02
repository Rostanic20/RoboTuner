package com.robo.robotuner.domain.model

import com.robo.robotuner.domain.model.Note.A
import com.robo.robotuner.domain.model.Note.A_SHARP
import com.robo.robotuner.domain.model.Note.B
import com.robo.robotuner.domain.model.Note.C_SHARP
import com.robo.robotuner.domain.model.Note.D
import com.robo.robotuner.domain.model.Note.D_SHARP
import com.robo.robotuner.domain.model.Note.E
import com.robo.robotuner.domain.model.Note.F_SHARP
import com.robo.robotuner.domain.model.Note.G
import com.robo.robotuner.domain.model.Note.G_SHARP

enum class Tuning(val strings: List<GuitarString>) {
    STANDARD(strings(E to 2, A to 2, D to 3, G to 3, B to 3, E to 4)),
    DROP_D(strings(D to 2, A to 2, D to 3, G to 3, B to 3, E to 4)),
    HALF_STEP_DOWN(strings(D_SHARP to 2, G_SHARP to 2, C_SHARP to 3, F_SHARP to 3, A_SHARP to 3, D_SHARP to 4)),
    DADGAD(strings(D to 2, A to 2, D to 3, G to 3, A to 3, D to 4)),
    OPEN_G(strings(D to 2, G to 2, D to 3, G to 3, B to 3, D to 4)),
    OPEN_D(strings(D to 2, A to 2, D to 3, F_SHARP to 3, A to 3, D to 4))
}

private fun strings(vararg notes: Pair<Note, Int>) = notes.map { (note, octave) -> GuitarString(note, octave) }
