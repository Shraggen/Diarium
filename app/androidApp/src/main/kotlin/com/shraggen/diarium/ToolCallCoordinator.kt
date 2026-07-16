package com.shraggen.diarium

import com.shraggen.diarium.speech.SpeechLanguage
import com.shraggen.diarium.tool.ToolCall
import com.shraggen.diarium.tool.ToolResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ToolCallCoordinator(
    private val runtime: AndroidDiariumRuntime,
    private val mutableUiState: MutableStateFlow<DiariumUiState>,
    private val coroutineScope: CoroutineScope,
) {

    private var plannedCall: ToolCall? = null

    fun updateUserInput(value: String) {
        plannedCall = null
        mutableUiState.update { state ->
            state.copy(
                userInput = value,
                pendingToolCall = null,
            )
        }
    }

    fun plan() {
        val state = mutableUiState.value
        if (state.isProcessing ||
            state.userInput.isBlank()
        ) {
            return
        }
        val userInput = state.userInput

        coroutineScope.launch {
            mutableUiState.update { current ->
                current.copy(
                    isProcessing = true,
                    output = "",
                    pendingToolCall = null,
                )
            }
            plannedCall = null

            val outcome = runCatching {
                runtime.plan(userInput)
            }
            plannedCall = outcome.getOrNull()

            mutableUiState.update { current ->
                current.copy(
                    isProcessing = false,
                    pendingToolCall = plannedCall?.let { call ->
                        pendingToolCall(userInput, call)
                    },
                    output = outcome.exceptionOrNull()?.failureMessage(
                        prefix = "Command planning failed: ",
                        fallback = "Unknown error.",
                    ).orEmpty(),
                )
            }
        }
    }

    fun confirm() {
        val pendingCall = confirmedPendingCall() ?: return
        val call = pendingCall.call

        coroutineScope.launch {
            mutableUiState.update { state ->
                state.copy(isProcessing = true, output = "")
            }

            val outcome = runCatching {
                runtime.execute(call)
            }
            val execution = outcome.getOrNull()
            if (execution != null) {
                plannedCall = null
            }

            mutableUiState.update { state ->
                state.copy(
                    isProcessing = false,
                    output = execution?.result?.displayText()
                        ?: outcome.exceptionOrNull()?.failureMessage(
                            prefix = "Tool execution failed: ",
                            fallback = "Unknown error.",
                        ).orEmpty(),
                    recentInspections = execution?.inspections
                        ?: state.recentInspections,
                    pendingToolCall = if (plannedCall == null) {
                        null
                    } else {
                        pendingCall
                    },
                )
            }
        }
    }

    private fun confirmedPendingCall(): PendingToolCall? {
        val state = mutableUiState.value
        return state.pendingToolCall?.takeIf { pending ->
            !state.isProcessing &&
                pending.confirmationAllowed &&
                pending.call == plannedCall
        }
    }

    fun cancel() {
        plannedCall = null
        mutableUiState.update { state ->
            state.copy(
                pendingToolCall = null,
                output = cancellationMessage(state.selectedLanguage),
            )
        }
    }
}

private fun cancellationMessage(language: SpeechLanguage): String =
    when (language) {
        SpeechLanguage.ENGLISH ->
            "Proposed action cancelled. Nothing was recorded."
        SpeechLanguage.GERMAN ->
            "Vorgeschlagene Aktion abgebrochen. Es wurde nichts gespeichert."
        SpeechLanguage.SERBIAN ->
            "Предложена радња је отказана. Ништа није сачувано."
    }

private fun ToolResult.displayText(): String =
    when (this) {
        is ToolResult.Success -> content.toString()
        is ToolResult.Failure -> "Tool execution failed: $message"
    }
