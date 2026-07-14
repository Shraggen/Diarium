package com.shraggen.diarium

import android.app.Application
import android.net.Uri
import com.shraggen.diarium.speech.LlamatikSpeechTranscriber
import com.shraggen.diarium.speech.SpeechLanguage
import com.shraggen.diarium.speech.Transcript
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidSpeechRuntime(
    application: Application,
) {
    private val modelStore = AndroidModelStore(application)
    private val recorder = AndroidVoiceRecorder(application)
    private val transcriber = LlamatikSpeechTranscriber()

    fun newestModel(): File? = modelStore.newestModel(StoredModelType.WHISPER)

    suspend fun importModel(uri: Uri): File = withContext(Dispatchers.IO) {
        transcriber.release()
        modelStore.importModel(uri, StoredModelType.WHISPER)
    }

    suspend fun initialize(modelFile: File) = withContext(Dispatchers.IO) {
        check(transcriber.initialize(modelFile.absolutePath)) {
            "Llamatik could not initialize the selected Whisper model."
        }
    }

    suspend fun capture(): File = withContext(Dispatchers.IO) {
        recorder.capture()
    }

    suspend fun transcribe(
        wavFile: File,
        language: SpeechLanguage,
    ): Transcript = withContext(Dispatchers.IO) {
        try {
            transcriber.transcribe(wavFile.absolutePath, language)
        } finally {
            wavFile.delete()
        }
    }

    fun stopCapture() {
        recorder.stop()
    }

    fun release() {
        recorder.stop()
        transcriber.release()
    }
}
