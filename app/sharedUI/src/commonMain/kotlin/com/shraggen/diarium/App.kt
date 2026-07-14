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
import com.shraggen.diarium.speech.SpeechLanguage

data class DiariumUiState(
    val modelStatus: ModelStatus = ModelStatus.NotLoaded,
    val userInput: String = "",
    val output: String = "",
    val isProcessing: Boolean = false,
    val recentInspections: List<InspectionRecord> = emptyList(),
    val journalError: String? = null,
    val pendingToolCall: PendingToolCall? = null,
    val speechModelStatus: SpeechModelStatus = SpeechModelStatus.NotLoaded,
    val voiceStatus: VoiceStatus = VoiceStatus.Idle,
    val selectedLanguage: SpeechLanguage = SpeechLanguage.ENGLISH,
    val detectedSpeechLanguage: String? = null,
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

sealed interface SpeechModelStatus {
    data object NotLoaded : SpeechModelStatus

    data class Loading(val message: String) : SpeechModelStatus

    data class Ready(val modelName: String) : SpeechModelStatus

    data class Error(val message: String) : SpeechModelStatus
}

sealed interface VoiceStatus {
    data object Idle : VoiceStatus

    data object Recording : VoiceStatus

    data object Transcribing : VoiceStatus

    data class Error(val message: String) : VoiceStatus
}

@Composable
fun App(
    state: DiariumUiState = DiariumUiState(),
    onUserInputChanged: (String) -> Unit = {},
    onSelectModel: () -> Unit = {},
    onSelectWhisperModel: () -> Unit = {},
    onLanguageChanged: (SpeechLanguage) -> Unit = {},
    onVoiceInput: () -> Unit = {},
    onProcess: () -> Unit = {},
    onConfirmToolCall: () -> Unit = {},
    onCancelToolCall: () -> Unit = {},
) {
    val modelReady = state.modelStatus is ModelStatus.Ready
    val modelLoading = state.modelStatus is ModelStatus.Loading
    val speechModelReady = state.speechModelStatus is SpeechModelStatus.Ready
    val speechModelLoading = state.speechModelStatus is SpeechModelStatus.Loading
    val recording = state.voiceStatus is VoiceStatus.Recording
    val transcribing = state.voiceStatus is VoiceStatus.Transcribing
    val copy = copyFor(state.selectedLanguage)

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
                text = copy.title,
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = modelStatusText(state.modelStatus, copy),
                color = when (state.modelStatus) {
                    is ModelStatus.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onBackground
                },
            )

            Button(
                onClick = onSelectModel,
                enabled = !modelLoading && !state.isProcessing && !recording && !transcribing,
            ) {
                Text(if (modelReady) copy.changeLlm else copy.selectLlm)
            }

            Text(
                text = speechModelStatusText(state.speechModelStatus, copy),
                color = when (state.speechModelStatus) {
                    is SpeechModelStatus.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onBackground
                },
            )

            Button(
                onClick = onSelectWhisperModel,
                enabled = !speechModelLoading && !state.isProcessing && !recording && !transcribing,
            ) {
                Text(if (speechModelReady) copy.changeWhisper else copy.selectWhisper)
            }

            LanguageSelector(
                selected = state.selectedLanguage,
                label = copy.language,
                enabled = !state.isProcessing && !recording && !transcribing,
                onLanguageChanged = onLanguageChanged,
            )

            OutlinedTextField(
                value = state.userInput,
                onValueChange = onUserInputChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = modelReady && !state.isProcessing && !recording && !transcribing,
                label = { Text(copy.command) },
                placeholder = {
                    Text(copy.commandPlaceholder)
                },
                minLines = 3,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onProcess,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = modelReady &&
                        state.userInput.isNotBlank() &&
                        !state.isProcessing &&
                        !recording &&
                        !transcribing,
                ) {
                    Text(copy.process)
                }

                Button(
                    onClick = onVoiceInput,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = if (recording) {
                        true
                    } else {
                        modelReady && speechModelReady && !state.isProcessing && !transcribing
                    },
                ) {
                    Text(if (recording) copy.stopRecording else copy.recordVoice)
                }
            }

            if (modelLoading || speechModelLoading || state.isProcessing || transcribing) {
                CircularProgressIndicator()
            }

            VoiceStatusText(state.voiceStatus, copy)

            state.detectedSpeechLanguage?.let { language ->
                Text("${copy.detectedLanguage}: $language")
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
                            text = copy.result,
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
                    copy = copy,
                )
            }

            InspectionJournal(
                inspections = state.recentInspections,
                error = state.journalError,
                copy = copy,
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
    copy: AppCopy,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = copy.confirmWrite,
                style = MaterialTheme.typography.titleMedium,
            )
            Text("${copy.tool}: ${call.toolName}")
            Text(call.arguments)
            Text(copy.nothingSaved)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onConfirm,
                    enabled = !isProcessing,
                ) {
                    Text(copy.confirm)
                }
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isProcessing,
                ) {
                    Text(copy.cancel)
                }
            }
        }
    }
}

@Composable
private fun InspectionJournal(
    inspections: List<InspectionRecord>,
    error: String?,
    copy: AppCopy,
) {
    Text(
        text = copy.journal,
        style = MaterialTheme.typography.titleLarge,
    )

    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (inspections.isEmpty() && error == null) {
        Text(copy.emptyJournal)
    }

    inspections.forEach { inspection ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${copy.hive} ${inspection.hiveId}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (inspection.queenSeen) {
                        copy.queenSeen
                    } else {
                        copy.queenNotSeen
                    },
                )
                Text("${copy.record} #${inspection.id}")
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    selected: SpeechLanguage,
    label: String,
    enabled: Boolean,
    onLanguageChanged: (SpeechLanguage) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SpeechLanguage.entries.forEach { language ->
            OutlinedButton(
                onClick = { onLanguageChanged(language) },
                enabled = enabled && selected != language,
            ) {
                Text(language.whisperCode.uppercase())
            }
        }
    }
    Text(selected.displayName)
}

@Composable
private fun VoiceStatusText(status: VoiceStatus, copy: AppCopy) {
    when (status) {
        VoiceStatus.Idle -> Unit
        VoiceStatus.Recording -> Text(copy.listening)
        VoiceStatus.Transcribing -> Text(copy.transcribing)
        is VoiceStatus.Error -> Text(
            text = status.message,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun modelStatusText(status: ModelStatus, copy: AppCopy): String =
    when (status) {
        ModelStatus.NotLoaded -> copy.llmMissing
        is ModelStatus.Loading -> status.message
        is ModelStatus.Ready -> "${copy.ready}: ${status.modelName}"
        is ModelStatus.Error -> status.message
    }

private fun speechModelStatusText(
    status: SpeechModelStatus,
    copy: AppCopy,
): String =
    when (status) {
        SpeechModelStatus.NotLoaded -> copy.whisperMissing
        is SpeechModelStatus.Loading -> status.message
        is SpeechModelStatus.Ready -> "${copy.ready}: ${status.modelName}"
        is SpeechModelStatus.Error -> status.message
    }
