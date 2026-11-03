package com.robo.robotuner.presentation.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.robo.robotuner.R
import com.robo.robotuner.domain.model.Instrument
import com.robo.robotuner.domain.model.Tuning

@StringRes
fun Tuning.nameRes(): Int = when (this) {
    Tuning.STANDARD, Tuning.BASS_STANDARD -> R.string.tuning_standard
    Tuning.DROP_D, Tuning.BASS_DROP_D -> R.string.tuning_drop_d
    Tuning.HALF_STEP_DOWN, Tuning.BASS_HALF_STEP_DOWN -> R.string.tuning_half_step_down
    Tuning.D_STANDARD, Tuning.BASS_D_STANDARD -> R.string.tuning_d_standard
    Tuning.DROP_C_SHARP -> R.string.tuning_drop_c_sharp
    Tuning.DROP_C, Tuning.BASS_DROP_C -> R.string.tuning_drop_c
    Tuning.DADGAD -> R.string.tuning_dadgad
    Tuning.OPEN_G -> R.string.tuning_open_g
    Tuning.OPEN_D -> R.string.tuning_open_d
    Tuning.OPEN_E -> R.string.tuning_open_e
    Tuning.OPEN_C -> R.string.tuning_open_c
}

@StringRes
fun Instrument.nameRes(): Int = when (this) {
    Instrument.GUITAR -> R.string.instrument_guitar
    Instrument.BASS -> R.string.instrument_bass
}

@Composable
fun tuningTitle(tuning: Tuning, capo: Int): String {
    val name = stringResource(tuning.nameRes())
    val withInstrument = if (tuning.instrument == Instrument.BASS) stringResource(R.string.bass_tuning, name) else name
    return if (capo == 0) withInstrument else stringResource(R.string.tuning_with_capo, withInstrument, capo)
}

fun Tuning.notesLabel(useFlats: Boolean): String = strings.joinToString(" ") { it.note.label(useFlats) }
