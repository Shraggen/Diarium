package com.shraggen.diarium.speech

import com.llamatik.library.platform.WhisperBridge

class LlamatikSpeechTranscriber {

    private var initializedModelPath: String? = null

    fun initialize(modelLocation: String): Boolean {
        release()
        val modelPath = WhisperBridge.getModelPath(modelLocation)
        val initialized = WhisperBridge.initModel(modelPath)
        if (initialized) {
            initializedModelPath = modelPath
        }
        return initialized
    }

    fun transcribe(
        wavLocation: String,
        language: SpeechLanguage,
    ): Transcript {
        check(initializedModelPath != null) {
            "The Whisper model is not initialized."
        }
        val json = WhisperBridge.transcribeWavSegments(
            wavPath = wavLocation,
            language = language.whisperCode,
            initialPrompt = language.whisperPrompt,
            translate = false,
            diarize = false,
        )
        return WhisperTranscriptParser.parseSegments(json)
    }

    fun release() {
        if (initializedModelPath != null) {
            WhisperBridge.release()
            initializedModelPath = null
        }
    }
}
