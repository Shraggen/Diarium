package com.shraggen.diarium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shraggen.diarium.beekeeping.InspectionRecord

data class DiariumUiState(
    val modelStatus: ModelStatus = ModelStatus.NotLoaded,
    val userInput: String = "",
    val output: String = "",
    val isProcessing: Boolean = false,
    val recentInspections: List<InspectionRecord> = emptyList(),
    val journalError: String? = null,
    val pendingToolCall: PendingToolCall? = null,
)

data class PendingToolCall(
    val toolName: String,
    val arguments: String,
)

sealed interface ModelStatus {
    data object NotLoaded : ModelStatus

    data class Loading(
        val message: String,
    ) : ModelStatus

    data class Ready(
        val modelName: String,
    ) : ModelStatus

    data class Error(
        val message: String,
    ) : ModelStatus
}

@Composable
fun App(
    state: DiariumUiState = DiariumUiState(),
    onUserInputChanged: (String) -> Unit = {},
    onSelectModel: () -> Unit = {},
    onProcess: () -> Unit = {},
    onConfirmToolCall: () -> Unit = {},
    onCancelToolCall: () -> Unit = {},
) {
    val modelReady = state.modelStatus is ModelStatus.Ready
    val modelLoading = state.modelStatus is ModelStatus.Loading

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Diarium local tool call",
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = modelStatusText(state.modelStatus),
                color = when (state.modelStatus) {
                    is ModelStatus.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onBackground
                },
            )

            Button(
                onClick = onSelectModel,
                enabled = !modelLoading && !state.isProcessing,
            ) {
                Text(if (modelReady) "Change GGUF model" else "Select GGUF model")
            }

            OutlinedTextField(
                value = state.userInput,
                onValueChange = onUserInputChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = modelReady && !state.isProcessing,
                label = { Text("Command") },
                placeholder = {
                    Text("I inspected hive 4 and saw the queen.")
                },
                minLines = 3,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onProcess,
                    enabled = modelReady &&
                        state.userInput.isNotBlank() &&
                        !state.isProcessing,
                ) {
                    Text("Process locally")
                }

                if (modelLoading || state.isProcessing) {
                    CircularProgressIndicator()
                }
            }

            if (state.output.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Result",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(state.output)
                    }
                }
            }

            state.pendingToolCall?.let { pendingCall ->
                PendingToolCallCard(
                    call = pendingCall,
                    isProcessing = state.isProcessing,
                    onConfirm = onConfirmToolCall,
                    onCancel = onCancelToolCall,
                )
            }

            InspectionJournal(
                inspections = state.recentInspections,
                error = state.journalError,
            )
        }
    }
}

@Composable
private fun PendingToolCallCard(
    call: PendingToolCall,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Confirm journal write",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("Tool: ${call.toolName}")
            Text(call.arguments)
            Text("Nothing is saved until you confirm.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onConfirm,
                    enabled = !isProcessing,
                ) {
                    Text("Confirm")
                }
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isProcessing,
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun InspectionJournal(
    inspections: List<InspectionRecord>,
    error: String?,
) {
    Text(
        text = "Inspection journal",
        style = MaterialTheme.typography.titleLarge,
    )

    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (inspections.isEmpty() && error == null) {
        Text("No inspections recorded yet.")
    }

    inspections.forEach { inspection ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Hive ${inspection.hiveId}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (inspection.queenSeen) {
                        "Queen seen"
                    } else {
                        "Queen not seen"
                    },
                )
                Text("Record #${inspection.id}")
            }
        }
    }
}

private fun modelStatusText(status: ModelStatus): String =
    when (status) {
        ModelStatus.NotLoaded -> "Select an instruction-tuned GGUF model to begin."
        is ModelStatus.Loading -> status.message
        is ModelStatus.Ready -> "Ready: ${status.modelName}"
        is ModelStatus.Error -> status.message
    }
