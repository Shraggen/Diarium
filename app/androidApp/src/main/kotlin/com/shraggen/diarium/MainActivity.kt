package com.shraggen.diarium

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.shraggen.diarium.beekeeping.InspectionRecord

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<DiariumViewModel>()

    private val modelPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel.llmModels::load)
    }

    private val whisperModelPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel.voiceInput::loadModel)
    }

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.voiceInput.start()
        } else {
            viewModel.voiceInput.reportPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsState()

            App(
                state = state,
                onUserInputChanged = viewModel.toolCalls::updateUserInput,
                onSelectModel = {
                    modelPicker.launch(arrayOf("*/*"))
                },
                onSelectWhisperModel = {
                    whisperModelPicker.launch(arrayOf("*/*"))
                },
                onLanguageChanged = viewModel.voiceInput::updateLanguage,
                onVoiceInput = ::handleVoiceInput,
                onProcess = viewModel.toolCalls::plan,
                onConfirmToolCall = viewModel.toolCalls::confirm,
                onCancelToolCall = viewModel.toolCalls::cancel,
            )
        }
    }

    private fun handleVoiceInput() {
        if (viewModel.uiState.value.voiceStatus is VoiceStatus.Recording) {
            viewModel.voiceInput.stop()
            return
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.voiceInput.start()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Preview
@Composable
@Suppress("FunctionNaming")
fun AppAndroidPreview() {
    App(
        state = DiariumUiState(
            modelStatus = ModelStatus.Ready(
                "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            ),
            userInput = "I inspected hive 4 and saw the queen.",
            output = "{\"hive_id\":\"4\",\"queen_seen\":true,\"recorded\":true}",
            recentInspections = listOf(
                InspectionRecord(
                    id = 1,
                    hiveId = "4",
                    queenSeen = true,
                    recordedAtEpochMillis = 0,
                ),
            ),
        ),
    )
}
