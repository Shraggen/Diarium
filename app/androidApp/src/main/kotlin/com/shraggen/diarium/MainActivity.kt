package com.shraggen.diarium

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

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<DiariumViewModel>()

    private val modelPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::loadModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsState()

            App(
                state = state,
                onUserInputChanged = viewModel::updateUserInput,
                onSelectModel = {
                    modelPicker.launch(arrayOf("*/*"))
                },
                onProcess = viewModel::processInput,
            )
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
        ),
    )
}
