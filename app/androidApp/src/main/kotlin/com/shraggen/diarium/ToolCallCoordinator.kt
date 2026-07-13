package com.shraggen.diarium

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
        mutableUiState.update { state ->
            state.copy(userInput = value)
        }
    }

    fun plan() {
        val state = mutableUiState.value
        if (state.isProcessing ||
            state.modelStatus !is ModelStatus.Ready ||
            state.userInput.isBlank()
        ) {
            return
        }

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
                runtime.plan(mutableUiState.value.userInput)
            }
            plannedCall = outcome.getOrNull()

            mutableUiState.update { current ->
                current.copy(
                    isProcessing = false,
                    pendingToolCall = plannedCall?.toPendingCall(),
                    output = outcome.exceptionOrNull()?.failureMessage(
                        prefix = "Inference failed: ",
                        fallback = "Unknown error.",
                    ).orEmpty(),
                )
            }
        }
    }

    fun confirm() {
        val call = plannedCall ?: return
        if (mutableUiState.value.isProcessing) {
            return
        }

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
                    pendingToolCall = plannedCall?.toPendingCall(),
                )
            }
        }
    }

    fun cancel() {
        plannedCall = null
        mutableUiState.update { state ->
            state.copy(
                pendingToolCall = null,
                output = "Proposed action cancelled. Nothing was recorded.",
            )
        }
    }
}

private fun ToolCall.toPendingCall() = PendingToolCall(
    toolName = toolName,
    arguments = arguments.toString(),
)

private fun ToolResult.displayText(): String =
    when (this) {
        is ToolResult.Success -> content.toString()
        is ToolResult.Failure -> "Tool execution failed: $message"
    }
