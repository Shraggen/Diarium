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
import com.shraggen.diarium.beekeeping.HiveIdentifierConsistency
import com.shraggen.diarium.beekeeping.InspectionRecord
import com.shraggen.diarium.beekeeping.QueenObservationConsistency
import com.shraggen.diarium.speech.SpeechLanguage

data class DiariumUiState(
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
    onSelectWhisperModel: () -> Unit = {},
    onLanguageChanged: (SpeechLanguage) -> Unit = {},
    onVoiceInput: () -> Unit = {},
    onProcess: () -> Unit = {},
    onConfirmToolCall: () -> Unit = {},
    onCancelToolCall: () -> Unit = {},
) {
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
                enabled = !state.isProcessing && !recording && !transcribing,
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
                    enabled = state.userInput.isNotBlank() &&
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
                        speechModelReady &&
                            !state.isProcessing &&
                            !transcribing
                    },
                ) {
                    Text(if (recording) copy.stopRecording else copy.recordVoice)
                }
            }

            if (speechModelLoading || state.isProcessing || transcribing) {
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
            Text("${copy.hive}: ${call.hiveId ?: copy.unknown}")
            Text(
                "${copy.queenSeen}: " + when (call.queenSeen) {
                    true -> copy.yes
                    false -> copy.no
                    null -> copy.unknown
                },
            )
            IdentifierConsistencyMessage(call.identifierConsistency, copy)
            QueenConsistencyMessage(call.queenConsistency, copy)
            Text(copy.nothingSaved)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onConfirm,
                    enabled = call.confirmationAllowed && !isProcessing,
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
private fun IdentifierConsistencyMessage(
    consistency: HiveIdentifierConsistency,
    copy: AppCopy,
) {
    when (consistency) {
        is HiveIdentifierConsistency.Verified -> Unit
        is HiveIdentifierConsistency.Mismatch -> {
            Text(
                text = copy.identifierMismatch,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "${copy.spokenHive}: ${consistency.transcriptIdentifier}",
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "${copy.proposedHive}: ${consistency.proposedIdentifier}",
                color = MaterialTheme.colorScheme.error,
            )
        }
        is HiveIdentifierConsistency.CannotVerify -> {
            Text(
                text = copy.identifierCannotVerify,
                color = MaterialTheme.colorScheme.error,
            )
            if (consistency.transcriptIdentifiers.isNotEmpty()) {
                Text(
                    text = "${copy.spokenHive}: " +
                        consistency.transcriptIdentifiers.joinToString(),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun QueenConsistencyMessage(
    consistency: QueenObservationConsistency,
    copy: AppCopy,
) {
    when (consistency) {
        is QueenObservationConsistency.Verified -> Unit
        is QueenObservationConsistency.Mismatch -> {
            Text(
                text = copy.queenMismatch,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "${copy.transcriptQueenSeen}: " +
                    booleanDisplayValue(consistency.transcriptQueenSeen, copy),
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "${copy.proposedQueenSeen}: " +
                    booleanDisplayValue(consistency.proposedQueenSeen, copy),
                color = MaterialTheme.colorScheme.error,
            )
        }
        is QueenObservationConsistency.CannotVerify -> {
            Text(
                text = copy.queenCannotVerify,
                color = MaterialTheme.colorScheme.error,
            )
            if (consistency.transcriptObservations.isNotEmpty()) {
                Text(
                    text = "${copy.transcriptQueenSeen}: " +
                        consistency.transcriptObservations.joinToString { value ->
                            booleanDisplayValue(value, copy)
                        },
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun booleanDisplayValue(
    value: Boolean?,
    copy: AppCopy,
): String = when (value) {
    true -> copy.yes
    false -> copy.no
    null -> copy.unknown
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
