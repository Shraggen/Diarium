package com.shraggen.diarium

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
import com.shraggen.diarium.speech.WHISPER_SAMPLE_RATE
import com.shraggen.diarium.speech.pcm16MonoWav
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal class AndroidVoiceRecorder(
    private val application: Application,
) {

    private val stopRequested = AtomicBoolean(false)

    fun capture(): File {
        checkMicrophonePermission()
        stopRequested.set(false)
        val capturedAudio = createVad().use(::recordWithVad)
        check(capturedAudio.heardSpeech) {
            "No speech was detected. Try again and speak after tapping the microphone."
        }
        return writeWav(capturedAudio.samples)
    }

    fun stop() {
        stopRequested.set(true)
    }

    private fun createVad() =
        VadSilero(
            context = application,
            sampleRate = SampleRate.SAMPLE_RATE_16K,
            frameSize = FrameSize.FRAME_SIZE_512,
            mode = Mode.NORMAL,
            speechDurationMs = MINIMUM_SPEECH_MILLIS,
            silenceDurationMs = AUTO_STOP_SILENCE_MILLIS,
        )

    private fun recordWithVad(vad: VadSilero): CapturedAudio {
        val recorder = createAudioRecord(FrameSize.FRAME_SIZE_512.value)
        return try {
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                "Android could not start microphone capture."
            }
            collectSamples(recorder, vad)
        } finally {
            releaseRecorder(recorder)
        }
    }

    private fun collectSamples(
        recorder: AudioRecord,
        vad: VadSilero,
    ): CapturedAudio {
        val samples = ShortSampleBuffer()
        val frame = ShortArray(FrameSize.FRAME_SIZE_512.value)
        var heardSpeech = false
        var frameCount = 0
        var captureComplete = false

        while (!stopRequested.get() &&
            frameCount < MAX_FRAME_COUNT &&
            !captureComplete
        ) {
            val read = readFrame(recorder, frame)
            if (read <= 0) {
                check(stopRequested.get()) {
                    "Microphone capture failed with Android error $read."
                }
                captureComplete = true
                continue
            }

            samples.append(frame, read)
            frameCount++
            if (read == frame.size) {
                val speechInFrame = vad.isSpeech(frame)
                if (speechInFrame) {
                    heardSpeech = true
                } else if (heardSpeech) {
                    captureComplete = true
                }
            }
        }
        return CapturedAudio(samples.toArray(), heardSpeech)
    }

    private fun releaseRecorder(recorder: AudioRecord) {
        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            recorder.stop()
        }
        recorder.release()
        stopRequested.set(false)
    }

    private fun checkMicrophonePermission() {
        check(
            application.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        ) {
            "Microphone permission is required for voice input."
        }
    }

    @Suppress("MissingPermission")
    private fun createAudioRecord(frameSize: Int): AudioRecord {
        val minimumBufferSize = AudioRecord.getMinBufferSize(
            WHISPER_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minimumBufferSize > 0) {
            "This device does not support 16 kHz mono PCM microphone capture."
        }
        val bufferSize = maxOf(minimumBufferSize, frameSize * BYTES_PER_SAMPLE * 2)
        return AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            WHISPER_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
    }

    private fun readFrame(recorder: AudioRecord, destination: ShortArray): Int {
        var offset = 0
        while (offset < destination.size && !stopRequested.get()) {
            val read = recorder.read(
                destination,
                offset,
                destination.size - offset,
                AudioRecord.READ_BLOCKING,
            )
            if (read <= 0) {
                return read
            }
            offset += read
        }
        return offset
    }

    private fun writeWav(samples: ShortArray): File {
        val directory = File(application.cacheDir, "voice-captures")
        check(directory.exists() || directory.mkdirs()) {
            "Could not create temporary voice storage."
        }
        directory.listFiles { file ->
            file.extension.equals("wav", ignoreCase = true)
        }?.forEach(File::delete)
        return File(directory, "capture-${System.currentTimeMillis()}.wav").apply {
            writeBytes(pcm16MonoWav(samples))
        }
    }

    private companion object {
        const val BYTES_PER_SAMPLE = 2
        const val MINIMUM_SPEECH_MILLIS = 64
        const val AUTO_STOP_SILENCE_MILLIS = 900
        const val MAX_RECORDING_SECONDS = 30
        const val SILERO_FRAME_SIZE = 512
        const val MAX_FRAME_COUNT =
            (WHISPER_SAMPLE_RATE * MAX_RECORDING_SECONDS) / SILERO_FRAME_SIZE
    }
}

private data class CapturedAudio(
    val samples: ShortArray,
    val heardSpeech: Boolean,
)

private class ShortSampleBuffer(
    initialCapacity: Int = 16_384,
) {
    private var values = ShortArray(initialCapacity)
    private var size = 0

    fun append(source: ShortArray, count: Int) {
        require(count in 0..source.size)
        ensureCapacity(size + count)
        source.copyInto(
            destination = values,
            destinationOffset = size,
            startIndex = 0,
            endIndex = count,
        )
        size += count
    }

    fun toArray(): ShortArray = values.copyOf(size)

    private fun ensureCapacity(requiredSize: Int) {
        if (requiredSize <= values.size) {
            return
        }
        var newSize = values.size
        while (newSize < requiredSize) {
            newSize *= 2
        }
        values = values.copyOf(newSize)
    }
}
