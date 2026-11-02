package com.robo.robotuner.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class AudioRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false

    val sampleRate: Int = SAMPLE_RATE

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<FloatArray> = flow {
        if (!hasPermission()) {
            throw SecurityException("Audio recording permission not granted")
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ).coerceAtLeast(BUFFER_SIZE)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord initialization failed")
        }

        val buffer = ShortArray(BUFFER_SIZE)
        isRecording = true
        audioRecord?.startRecording()

        try {
            while (isRecording && coroutineContext.isActive) {
                val readCount = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                if (readCount > 0) {
                    val floatBuffer = FloatArray(readCount)
                    for (i in 0 until readCount) {
                        floatBuffer[i] = buffer[i] / Short.MAX_VALUE.toFloat()
                    }
                    emit(floatBuffer)
                }
            }
        } finally {
            stopRecordingInternal()
        }
    }.flowOn(Dispatchers.IO)

    fun stopRecording() {
        isRecording = false
    }

    private fun stopRecordingInternal() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun isRecording(): Boolean = isRecording

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 8192  // Increased for better low frequency detection
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
}
