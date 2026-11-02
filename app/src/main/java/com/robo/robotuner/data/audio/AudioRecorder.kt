package com.robo.robotuner.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AudioRecorder(private val context: Context) {

    val sampleRate: Int = SAMPLE_RATE

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<FloatArray> = flow {
        if (!hasPermission()) throw SecurityException("Audio recording permission not granted")

        val minBufferBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        check(minBufferBytes > 0) { "Audio format not supported: $minBufferBytes" }

        val record = AudioRecord(
            audioSource(),
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            maxOf(minBufferBytes, HOP_SIZE * BYTES_PER_SAMPLE * BUFFERED_HOPS)
        )
        try {
            check(record.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord initialization failed" }
            record.startRecording()
            check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone is in use by another app" }
            val buffer = ShortArray(HOP_SIZE)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = record.read(buffer, 0, HOP_SIZE)
                check(read >= 0) { "AudioRecord read failed: $read" }
                if (read > 0) emit(FloatArray(read) { buffer[it] / 32768f })
            }
        } finally {
            if (record.state == AudioRecord.STATE_INITIALIZED) record.stop()
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    private fun audioSource(): Int {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val unprocessed = audioManager?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        // VOICE_RECOGNITION faded a steady tone to the noise floor within 2 s on a Galaxy S23+; MIC did not.
        return if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.MIC
    }

    companion object {
        const val SAMPLE_RATE = 44100
        private const val HOP_SIZE = 2048
        private const val BYTES_PER_SAMPLE = 2
        private const val BUFFERED_HOPS = 4
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
}
