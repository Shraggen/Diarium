package com.shraggen.diarium

import android.net.Uri
import com.shraggen.diarium.speech.SpeechLanguage
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class VoiceInputCoordinator(
    private val runtime: AndroidSpeechRuntime,
    private val toolCallCoordinator: ToolCallCoordinator,
    private val mutableUiState: MutableStateFlow<DiariumUiState>,
    private val coroutineScope: CoroutineScope,
) {

    fun loadModel(uri: Uri) {
        if (mutableUiState.value.speechModelStatus is SpeechModelStatus.Loading ||
            mutableUiState.value.isBusy
        ) {
            return
        }

        coroutineScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    speechModelStatus = SpeechModelStatus.Loading(
                        "Copying Whisper model to app storage…",
                    ),
                    voiceStatus = VoiceStatus.Idle,
                )
            }
            runCatching {
                initializeModel(runtime.importModel(uri))
            }.exceptionOrNull()?.let(::reportModelError)
        }
    }

    fun restoreModel() {
        val modelFile = runtime.newestModel() ?: return
        coroutineScope.launch {
            runCatching {
                initializeModel(modelFile)
            }.exceptionOrNull()?.let(::reportModelError)
        }
    }

    fun updateLanguage(language: SpeechLanguage) {
        if (mutableUiState.value.isBusy) {
            return
        }
        mutableUiState.update { state ->
            state.copy(
                selectedLanguage = language,
                detectedSpeechLanguage = null,
            )
        }
    }

    fun start() {
        val state = mutableUiState.value
        if (state.isBusy ||
            state.modelStatus !is ModelStatus.Ready ||
            state.speechModelStatus !is SpeechModelStatus.Ready
        ) {
            return
        }
        captureAndTranscribe(state.selectedLanguage)
    }

    fun stop() {
        runtime.stopCapture()
    }

    fun reportPermissionDenied() {
        mutableUiState.update { state ->
            state.copy(
                voiceStatus = VoiceStatus.Error(
                    "Microphone permission is required for voice input.",
                ),
            )
        }
    }

    private fun captureAndTranscribe(language: SpeechLanguage) {
        coroutineScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    voiceStatus = VoiceStatus.Recording,
                    detectedSpeechLanguage = null,
                    output = "",
                    pendingToolCall = null,
                )
            }

            val outcome = runCatching {
                val wavFile = runtime.capture()
                mutableUiState.update { state ->
                    state.copy(voiceStatus = VoiceStatus.Transcribing)
                }
                runtime.transcribe(wavFile, language)
            }
            val transcript = outcome.getOrNull()

            if (transcript == null) {
                reportCaptureError(outcome.exceptionOrNull())
                return@launch
            }

            toolCallCoordinator.updateUserInput(transcript.text)
            mutableUiState.update { state ->
                state.copy(
                    voiceStatus = VoiceStatus.Idle,
                    detectedSpeechLanguage = transcript.detectedLanguage,
                )
            }
            toolCallCoordinator.plan()
        }
    }

    private suspend fun initializeModel(modelFile: File) {
        mutableUiState.update { state ->
            state.copy(
                speechModelStatus = SpeechModelStatus.Loading(
                    "Loading ${modelFile.name}…",
                ),
            )
        }
        runtime.initialize(modelFile)
        mutableUiState.update { state ->
            state.copy(
                speechModelStatus = SpeechModelStatus.Ready(modelFile.name),
                voiceStatus = VoiceStatus.Idle,
            )
        }
    }

    private fun reportModelError(exception: Throwable) {
        runtime.release()
        mutableUiState.update { state ->
            state.copy(
                speechModelStatus = SpeechModelStatus.Error(
                    exception.failureMessage(
                        prefix = "",
                        fallback = "Whisper model initialization failed.",
                    ),
                ),
            )
        }
    }

    private fun reportCaptureError(exception: Throwable?) {
        mutableUiState.update { state ->
            state.copy(
                voiceStatus = VoiceStatus.Error(
                    checkNotNull(exception).failureMessage(
                        prefix = "Voice input failed: ",
                        fallback = "Unknown audio error.",
                    ),
                ),
            )
        }
    }
}
