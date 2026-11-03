package com.robo.robotuner.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.robo.robotuner.domain.repository.TonePlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AudioTrackTonePlayer(private val sampleRate: Int) : TonePlayer {

    private val synth = PluckSynth(sampleRate)

    override suspend fun play(frequency: Double) {
        val pcm = withContext(Dispatchers.Default) {
            synth.render(frequency).let { samples -> ShortArray(samples.size) { (samples[it] * Short.MAX_VALUE).toInt().toShort() } }
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * Short.SIZE_BYTES)
            .build()
        try {
            val written = track.write(pcm, 0, pcm.size)
            check(written == pcm.size) { "AudioTrack write failed: $written" }
            track.play()
            delay(pcm.size * 1000L / sampleRate)
        } finally {
            track.release()
        }
    }
}
