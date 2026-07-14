package com.shraggen.diarium

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shraggen.diarium.speech.SpeechLanguage
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiariumViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = AndroidDiariumRuntime(application)
    private val speechRuntime = AndroidSpeechRuntime(application)
    private val mutableUiState = MutableStateFlow(
        DiariumUiState(
            selectedLanguage = SpeechLanguage.fromLanguageTag(
                Locale.getDefault().toLanguageTag(),
            ),
        ),
    )

    val uiState: StateFlow<DiariumUiState> = mutableUiState.asStateFlow()
    internal val toolCalls = ToolCallCoordinator(
        runtime = runtime,
        mutableUiState = mutableUiState,
        coroutineScope = viewModelScope,
    )
    internal val llmModels = LlmModelCoordinator(
        runtime = runtime,
        mutableUiState = mutableUiState,
        coroutineScope = viewModelScope,
    )
    internal val voiceInput = VoiceInputCoordinator(
        runtime = speechRuntime,
        toolCallCoordinator = toolCalls,
        mutableUiState = mutableUiState,
        coroutineScope = viewModelScope,
    )

    init {
        refreshRecentInspections()
        llmModels.restore()
        voiceInput.restoreModel()
    }

    override fun onCleared() {
        speechRuntime.release()
        runtime.shutdown()
        super.onCleared()
    }

    private fun refreshRecentInspections() {
        viewModelScope.launch {
            val outcome = runCatching {
                runtime.recentInspections()
            }

            mutableUiState.update { state ->
                state.copy(
                    recentInspections = outcome.getOrDefault(
                        state.recentInspections,
                    ),
                    journalError = outcome.exceptionOrNull()?.failureMessage(
                        prefix = "Could not load journal: ",
                        fallback = "Unknown database error.",
                    ),
                )
            }
        }
    }
}
