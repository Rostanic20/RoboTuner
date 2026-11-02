package com.robo.robotuner.presentation.components

import androidx.annotation.StringRes
import com.robo.robotuner.R
import com.robo.robotuner.domain.model.Tuning

@StringRes
fun Tuning.nameRes(): Int = when (this) {
    Tuning.STANDARD -> R.string.tuning_standard
    Tuning.DROP_D -> R.string.tuning_drop_d
    Tuning.HALF_STEP_DOWN -> R.string.tuning_half_step_down
    Tuning.DADGAD -> R.string.tuning_dadgad
    Tuning.OPEN_G -> R.string.tuning_open_g
    Tuning.OPEN_D -> R.string.tuning_open_d
}

fun Tuning.notesLabel(useFlats: Boolean): String = strings.joinToString(" ") { it.note.label(useFlats) }
